package android.mqtt.iot.remote

import android.util.Log
import android.mqtt.iot.data.TunnelProxy
import okhttp3.Response
import java.io.Closeable
import java.io.IOException
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 将阿里云安全隧道会话桥接到本地 SSH 服务器。
 */
class SecureTunnelSshBridge(
    private val localServiceHost: String = LOCALHOST,
    private val localServicePort: Int = SSH.DEFAULT_PORT,
    private val sshUsername: String = DEFAULT_USERNAME,
    private val sshPassword: String = DEFAULT_PASSWORD,
    private val appNameResolver: (String) -> String? = { null },
    private val appInfoResolver: (String) -> AndroidInteractiveShellFactory.AppInfo? = { packageName ->
        appNameResolver(packageName)?.let { appName ->
            AndroidInteractiveShellFactory.AppInfo(packageName = packageName, appName = appName)
        }
    },
    private val appListResolver: () -> List<AndroidInteractiveShellFactory.AppInfo> = { emptyList() },
    private val appLaunchActivityResolver: (String) -> String? = { null },
    private val appStartResolver: (String, String?) -> AndroidInteractiveShellFactory.AppStartResult? = { _, _ -> null },
    private val runningAppResolver: () -> List<AndroidInteractiveShellFactory.RunningAppInfo> = { emptyList() },
    private val cameraResolver: () -> List<AndroidInteractiveShellFactory.CameraInfo> = { emptyList() },
    private val volumeResolver: () -> List<AndroidInteractiveShellFactory.VolumeInfo> = { emptyList() },
    private val executor: ExecutorService = Executors.newCachedThreadPool(),
    private val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(),
    private val listener: Listener = Listener.EMPTY,
) : Closeable {

    interface Listener {
        fun onOpen() = Unit
        fun onClosed() = Unit
        fun onFailure(t: Throwable) = Unit

        object EMPTY : Listener
    }

    private data class Session(
        val id: String,
        val serviceType: String,
        val socket: Socket,
    )

    private val open = AtomicBoolean(false)
    private val sessions = ConcurrentHashMap<String, Session>()
    private var remote: Remote? = null
    private var ssh: SSH? = null
    private var tokenExpiryFuture: ScheduledFuture<*>? = null

    fun open(proxy: TunnelProxy) {
        if (!open.compareAndSet(false, true)) return

        if (proxy.operation.equals(OPERATION_CLOSE, ignoreCase = true)) {
            close()
            return
        }

        try {
            SSH.removePrivilegedPortRedirect(PRIVILEGED_HTTPS_PORT, localServicePort)
            if (!startSshServer()) {
                logInfo("local ssh server did not start, skip secure tunnel websocket")
                close()
                return
            }
            scheduleTokenExpiry(proxy.token_expire)
            val tunnelUrl = tunnelUrl(proxy)
            Log.i(TAG, "secure tunnel websocket connect:$tunnelUrl")
            val remoteClient = Remote(
                url = tunnelUrl,
                accessToken = proxy.token,
                listener = object : Remote.Listener {
                    override fun onOpen(remote: Remote) {
                        this@SecureTunnelSshBridge.remote = remote
                        listener.onOpen()
                    }

                    override fun onSessionCreated(frame: TunnelFrame, response: TunnelResult?) {
                        onSessionCreated(remoteClient(), frame)
                    }

                    override fun onData(frame: TunnelFrame) {
                        this@SecureTunnelSshBridge.onData(frame)
                    }

                    override fun onSessionReleased(frame: TunnelFrame, reason: TunnelResult?) {
                        this@SecureTunnelSshBridge.onSessionReleased(frame)
                    }

                    override fun onClosing(code: Int, reason: String) {
                        closeSessions()
                    }

                    override fun onClosed(code: Int, reason: String) {
                        close()
                    }

                    override fun onFailure(t: Throwable, response: Response?) {
                        listener.onFailure(
                            IOException("secure tunnel websocket failed url=$tunnelUrl response=${response?.code}", t)
                        )
                        close()
                    }
                }
            )
            remote = remoteClient
            remoteClient.connect()
        } catch (t: Throwable) {
            listener.onFailure(t)
            close()
        }
    }

    fun scheduleTokenExpiry(tokenExpireSeconds: Int) {
        tokenExpiryFuture?.cancel(false)
        if (tokenExpireSeconds <= 0) {
            close()
            return
        }
        tokenExpiryFuture = scheduler.schedule(
            {
                logInfo("secure tunnel token expired, closing bridge")
                close()
            },
            tokenExpireSeconds.toLong(),
            TimeUnit.SECONDS,
        )
    }

    internal fun onSessionCreated(client: SecureTunnelClient, frame: TunnelFrame) {
        val sessionId = frame.header.sessionId
        val frameId = frame.header.frameId
        val serviceType = frame.header.serviceType ?: SERVICE_TYPE_SSH
        if (sessionId.isNullOrBlank() || frameId == null) {
            return
        }

        try {
            val socket = Socket(localServiceHost, localServicePort)
            val session = Session(sessionId, serviceType, socket)
            sessions[sessionId] = session
            client.acceptSession(sessionId, frameId, serviceType)
            startLocalToTunnelPump(client, session)
        } catch (t: Throwable) {
            logInfo("session create failed:$sessionId serviceType=$serviceType error=$t", t)
            client.rejectSession(sessionId, frameId, serviceType, code = 1, message = t.message ?: "connect local ssh failed")
        }
    }

    internal fun onData(frame: TunnelFrame) {
        val sessionId = frame.header.sessionId ?: return
        val session = sessions[sessionId] ?: return
        try {
            session.socket.getOutputStream().write(frame.payload)
            session.socket.getOutputStream().flush()
        } catch (e: IOException) {
            closeSession(sessionId)
            remote?.releaseSession(sessionId, e.message ?: "local ssh write failed", ReleaseCode.DEVICE_SIDE_CLOSE)
        }
    }

    internal fun onSessionReleased(frame: TunnelFrame) {
        frame.header.sessionId?.let(::closeSession)
    }

    internal fun isOpen(): Boolean = open.get()

    private fun startSshServer(): Boolean {
        if (ssh?.isOpen() == true) return true

        ssh = SSH(
            host = SSH.DEFAULT_HOST,
            port = localServicePort,
            username = sshUsername,
            password = sshPassword,
            appInfoResolver = appInfoResolver,
            appListResolver = appListResolver,
            appLaunchActivityResolver = appLaunchActivityResolver,
            appStartResolver = appStartResolver,
            runningAppResolver = runningAppResolver,
            cameraResolver = cameraResolver,
            volumeResolver = volumeResolver,
            listener = object : SSH.Listener {
                override fun onFailure(t: Throwable) {
                    Log.i(TAG, "SSH failure:$t")
                    listener.onFailure(t)
                }
            }
        )
        ssh?.open()
        return ssh?.isOpen() == true
    }

    private fun startLocalToTunnelPump(client: SecureTunnelClient, session: Session) {
        executor.execute {
            val buffer = ByteArray(MAX_PAYLOAD_SIZE)
            try {
                while (!session.socket.isClosed) {
                    val count = session.socket.getInputStream().read(buffer)
                    if (count == -1) break
                    if (count > 0) {
                        client.sendData(session.id, session.serviceType, buffer.copyOf(count))
                    }
                }
            } catch (_: IOException) {
                // 下面的会话清理会通知隧道本地连接已经断开。
            } finally {
                closeSession(session.id)
                client.releaseSession(session.id, "local ssh closed", ReleaseCode.DEVICE_SIDE_CLOSE)
            }
        }
    }

    private fun remoteClient(): SecureTunnelClient {
        return remote ?: throw IllegalStateException("remote is not open")
    }

    private fun closeSession(sessionId: String) {
        sessions.remove(sessionId)?.socket?.closeQuietly()
    }

    private fun closeSessions() {
        sessions.keys.toList().forEach(::closeSession)
    }

    override fun close() {
        if (!open.getAndSet(false)) return
        closeSessions()
        tokenExpiryFuture?.cancel(false)
        tokenExpiryFuture = null
        remote?.disconnect()
        remote = null
        ssh?.close()
        ssh = null
        listener.onClosed()
    }

    private fun Socket.closeQuietly() {
        try {
            close()
        } catch (_: IOException) {
        }
    }

    private fun logInfo(message: String, t: Throwable? = null) {
        try {
            if (t == null) {
                Log.i(TAG, message)
            } else {
                Log.i(TAG, message, t)
            }
        } catch (_: RuntimeException) {
            // 本地 JVM 单元测试无法使用 android.util.Log。
        }
    }

    companion object {
        private const val TAG = "SecureTunnelSshBridge"
        private const val LOCALHOST = "127.0.0.1"
        private const val MAX_PAYLOAD_SIZE = 4 * 1024
        private const val PRIVILEGED_HTTPS_PORT = 443
        const val SERVICE_TYPE_SSH = "ssh"
        const val OPERATION_CLOSE = "close"
        const val OPERATION_CONNECT = "connect"
        const val DEFAULT_USERNAME = ""
        const val DEFAULT_PASSWORD = ""

        internal fun tunnelUrl(proxy: TunnelProxy): String {
            if (proxy.uri.isNotBlank()) return proxy.uri

            val schema = proxy.schema.ifBlank { "wss" }
            val port = if (proxy.port > 0) ":${proxy.port}" else ""
            val path = if (proxy.path.startsWith("/")) proxy.path else "/${proxy.path}"
            return "$schema://${proxy.host}$port$path"
        }

    }
}
