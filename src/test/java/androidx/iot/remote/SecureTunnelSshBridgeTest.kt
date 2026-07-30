package androidx.iot.remote

import androidx.iot.data.TunnelProxy
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.ServerSocket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class SecureTunnelSshBridgeTest {

    @Test
    fun tunnelUrlUsesUriWhenProvided() {
        val proxy = tunnelProxy(uri = "wss://example.com/tunnel/1/dest")

        assertEquals("wss://example.com/tunnel/1/dest", SecureTunnelSshBridge.tunnelUrl(proxy))
    }

    @Test
    fun tunnelUrlBuildsFromPartsWhenUriIsBlank() {
        val proxy = tunnelProxy(
            uri = "",
            schema = "wss",
            host = "iot-secure-tunnel-cn-sh.aliyuncs.com",
            port = 443,
            path = "/tunnel/1/dest",
        )

        assertEquals(
            "wss://iot-secure-tunnel-cn-sh.aliyuncs.com:443/tunnel/1/dest",
            SecureTunnelSshBridge.tunnelUrl(proxy)
        )
    }

    @Test
    fun sessionCreateConnectsLocalServiceAndForwardsBothDirections() {
        val server = ServerSocket(0)
        val accepted = CountDownLatch(1)
        val receivedByServer = ByteArray(4)
        val serverThread = Thread {
            server.use {
                val socket = it.accept()
                socket.use { acceptedSocket ->
                    accepted.countDown()
                    acceptedSocket.getOutputStream().write(byteArrayOf(1, 2, 3))
                    acceptedSocket.getOutputStream().flush()
                    acceptedSocket.getInputStream().read(receivedByServer)
                }
            }
        }
        serverThread.start()

        val client = RecordingTunnelClient()
        val bridge = SecureTunnelSshBridge(localServicePort = server.localPort)
        bridge.onSessionCreated(
            client,
            TunnelFrame(
                header = TunnelHeader(
                    frameType = FrameType.SESSION_CREATE.value,
                    sessionId = "s1",
                    frameId = 10,
                    serviceType = SecureTunnelSshBridge.SERVICE_TYPE_SSH,
                )
            )
        )

        assertTrue(accepted.await(2, TimeUnit.SECONDS))
        assertEquals("s1", client.acceptedSessionId)
        assertEquals(10L, client.acceptedFrameId)

        bridge.onData(
            TunnelFrame(
                header = TunnelHeader(
                    frameType = FrameType.DATA_TRANSPORT.value,
                    sessionId = "s1",
                    serviceType = SecureTunnelSshBridge.SERVICE_TYPE_SSH,
                ),
                payload = byteArrayOf(4, 5, 6, 7),
            )
        )

        assertTrue(client.dataLatch.await(2, TimeUnit.SECONDS))
        assertArrayEquals(byteArrayOf(1, 2, 3), client.sentPayload)
        serverThread.join(2_000)
        assertArrayEquals(byteArrayOf(4, 5, 6, 7), receivedByServer)
        bridge.close()
    }

    @Test
    fun sessionCreateKeepsPlatformServiceTypeVerbatim() {
        val server = ServerSocket(0)
        val accepted = CountDownLatch(1)
        val serverThread = Thread {
            server.use {
                it.accept().use {
                    accepted.countDown()
                    Thread.sleep(200)
                }
            }
        }
        serverThread.start()

        val client = RecordingTunnelClient()
        val bridge = SecureTunnelSshBridge(localServicePort = server.localPort)
        val platformServiceType = "SSH_REMOTE_LOGIN_SERVICE_TYPE"
        bridge.onSessionCreated(
            client,
            TunnelFrame(
                header = TunnelHeader(
                    frameType = FrameType.SESSION_CREATE.value,
                    sessionId = "s1",
                    frameId = 10,
                    serviceType = platformServiceType,
                )
            )
        )

        assertTrue(accepted.await(2, TimeUnit.SECONDS))
        assertEquals(platformServiceType, client.acceptedServiceType)
        bridge.close()
        serverThread.join(2_000)
    }

    @Test
    fun sessionReleaseClosesSession() {
        val server = ServerSocket(0)
        val accepted = CountDownLatch(1)
        val closed = CountDownLatch(1)
        val serverThread = Thread {
            server.use {
                val socket = it.accept()
                accepted.countDown()
                socket.use { acceptedSocket ->
                    while (acceptedSocket.getInputStream().read() != -1) {
                        // 等待桥接关闭。
                    }
                    closed.countDown()
                }
            }
        }
        serverThread.start()

        val bridge = SecureTunnelSshBridge(localServicePort = server.localPort)
        bridge.onSessionCreated(
            RecordingTunnelClient(),
            TunnelFrame(
                header = TunnelHeader(
                    frameType = FrameType.SESSION_CREATE.value,
                    sessionId = "s1",
                    frameId = 11,
                    serviceType = SecureTunnelSshBridge.SERVICE_TYPE_SSH,
                )
            )
        )

        assertTrue(accepted.await(2, TimeUnit.SECONDS))
        bridge.onSessionReleased(
            TunnelFrame(
                header = TunnelHeader(
                    frameType = FrameType.SESSION_RELEASE.value,
                    sessionId = "s1",
                    serviceType = SecureTunnelSshBridge.SERVICE_TYPE_SSH,
                )
            )
        )

        assertTrue(closed.await(2, TimeUnit.SECONDS))
        serverThread.join(2_000)
        bridge.close()
    }

    @Test
    fun scheduleTokenExpiryClosesBridgeAfterTokenExpires() {
        val closed = CountDownLatch(1)
        val bridge = SecureTunnelSshBridge(
            listener = object : SecureTunnelSshBridge.Listener {
                override fun onClosed() {
                    closed.countDown()
                }
            }
        )
        bridge.markOpenForTest()

        bridge.scheduleTokenExpiry(1)

        assertTrue(closed.await(2, TimeUnit.SECONDS))
    }

    private class RecordingTunnelClient : SecureTunnelClient {
        var acceptedSessionId: String? = null
        var acceptedFrameId: Long? = null
        var acceptedServiceType: String? = null
        var sentPayload: ByteArray? = null
        val dataLatch = CountDownLatch(1)

        override fun acceptSession(sessionId: String, frameId: Long, serviceType: String, message: String): Boolean {
            acceptedSessionId = sessionId
            acceptedFrameId = frameId
            acceptedServiceType = serviceType
            return true
        }

        override fun rejectSession(
            sessionId: String,
            frameId: Long,
            serviceType: String,
            code: Int,
            message: String,
        ): Boolean = true

        override fun sendData(sessionId: String, serviceType: String, payload: ByteArray): Boolean {
            sentPayload = payload
            dataLatch.countDown()
            return true
        }

        override fun releaseSession(sessionId: String, message: String, code: Int): Boolean = true

        override fun disconnect(code: Int, reason: String) = Unit
    }

    private fun tunnelProxy(
        uri: String,
        schema: String = "wss",
        host: String = "example.com",
        port: Int = 443,
        path: String = "/tunnel/1/dest",
    ): TunnelProxy {
        return TunnelProxy(
            schema = schema,
            path = path,
            token_expire = 600,
            tunnel_id = "1",
            payload_mode = "app-proxy",
            port = port,
            host = host,
            operation = "connect",
            uri = uri,
            token = "token",
        )
    }

    private fun SecureTunnelSshBridge.markOpenForTest() {
        val field = SecureTunnelSshBridge::class.java.getDeclaredField("open")
        field.isAccessible = true
        (field.get(this) as AtomicBoolean).set(true)
    }
}
