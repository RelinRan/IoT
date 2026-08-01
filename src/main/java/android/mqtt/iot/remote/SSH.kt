package android.mqtt.iot.remote

import android.mqtt.iot.utils.Shell
import org.apache.sshd.scp.server.ScpCommandFactory
import org.apache.sshd.server.SshServer
import org.apache.sshd.server.auth.password.PasswordAuthenticator
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider
import org.apache.sshd.server.shell.ShellFactory
import org.apache.sshd.server.shell.ProcessShellFactory
import org.apache.sshd.sftp.server.SftpSubsystemFactory
import java.io.Closeable
import java.io.File
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean


class SSH(
    private val host: String,
    private val port: Int,
    private val username: String,
    private val password: String,
    private val fallbackPort: Int = DEFAULT_PORT,
    private val hostKeyPath: Path = defaultHostKeyPath(),
    private val shellCommand: List<String> = defaultShellCommand(),
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
    private val listener: Listener = Listener.EMPTY,
) : Closeable {

    interface Listener {
        fun onOpen(ssh: SSH) = Unit
        fun onClosed() = Unit
        fun onFailure(t: Throwable) = Unit

        object EMPTY : Listener
    }

    private val open = AtomicBoolean(false)
    private var server: SshServer? = null
    private var redirectedPort: Int? = null
    private var actualPort: Int = port

    fun open() {
        if (!open.compareAndSet(false, true)) return

        require(host.isNotBlank()) { "host must not be blank." }
        require(port in 1..65535) { "port must be in 1..65535." }
        require(fallbackPort in 1024..65535) { "fallbackPort must be in 1024..65535." }
        require(username.isNotBlank()) { "username must not be blank." }
        require(password.isNotEmpty()) { "password must not be empty." }
        require(shellCommand.isNotEmpty()) { "shellCommand must not be empty." }

        try {
            ensureUserHome(hostKeyPath)

            val sshd = SshServer.setUpDefaultServer()
            sshd.host = host
            actualPort = bindPort(port, fallbackPort)
            sshd.port = actualPort
            sshd.keyPairProvider = SimpleGeneratorHostKeyProvider(hostKeyPath)
            sshd.passwordAuthenticator = PasswordAuthenticator { user, pass, _ ->
                user == username && pass == password
            }
            val shellFactory = if (shellCommand == defaultShellCommand()) {
                AndroidInteractiveShellFactory(
                    promptUser = username,
                    appInfoResolver = appInfoResolver,
                    appListResolver = appListResolver,
                    appLaunchActivityResolver = appLaunchActivityResolver,
                    appStartResolver = appStartResolver,
                    runningAppResolver = runningAppResolver,
                    cameraResolver = cameraResolver,
                    volumeResolver = volumeResolver,
                )
            } else {
                ProcessShellFactory(shellCommand.first(), shellCommand)
            }
            sshd.shellFactory = scpAwareShellFactory(shellFactory)
            sshd.commandFactory = sshd.shellFactory as ScpCommandFactory
            val sharedStorage = selectAccessibleSharedStorage(sharedStorageCandidates())
            val sftpShadowRoot = File(System.getProperty("java.io.tmpdir") ?: ".", "iot-sftp-shadow")
            File(sftpShadowRoot, "storage/emulated/0").mkdirs()
            File(sftpShadowRoot, "storage/self/primary").mkdirs()
            val sftpAccessor = SftpPathAliasAccessor(
                sharedStorage = sharedStorage,
                shadowRoot = sftpShadowRoot.toPath(),
            )
            sshd.subsystemFactories = listOf(
                SftpSubsystemFactory.Builder()
                    .withFileSystemAccessor(sftpAccessor)
                    .build()
            )
            sshd.start()

            server = sshd
            if (port != actualPort) {
                redirectPrivilegedPort(port, actualPort)
                redirectedPort = port
            }
            listener.onOpen(this)
        } catch (t: Throwable) {
            removePrivilegedPortRedirect()
            server?.stop(true)
            server = null
            open.set(false)
            listener.onFailure(t)
        }
    }

    fun isOpen(): Boolean {
        return open.get() && server?.isOpen == true
    }

    fun boundPort(): Int {
        return server?.port ?: actualPort
    }

    override fun close() {
        if (!open.getAndSet(false)) return

        try {
            server?.stop(true)
        } finally {
            removePrivilegedPortRedirect()
            server = null
            listener.onClosed()
        }
    }

    companion object {
        const val DEFAULT_HOST = "0.0.0.0"
        const val DEFAULT_PORT = 2222

        private fun defaultHostKeyPath(): Path {
            val tmpDir = System.getProperty("java.io.tmpdir") ?: "."
            return File(tmpDir, "iot-ssh-hostkey.ser").toPath()
        }

        private fun defaultShellCommand(): List<String> {
            return listOf("/system/bin/sh")
        }

        internal fun bindPort(requestedPort: Int, fallbackPort: Int): Int {
            return if (requestedPort < 1024) fallbackPort else requestedPort
        }

        internal fun selectAccessibleSharedStorage(
            candidates: List<Path>,
            canList: (Path) -> Boolean = { path ->
                runCatching { java.nio.file.Files.newDirectoryStream(path).use { it.iterator().hasNext() || path.toFile().isDirectory } }
                    .getOrDefault(false)
            },
        ): Path = candidates.firstOrNull(canList) ?: candidates.first()

        private fun sharedStorageCandidates(): List<Path> = listOf(
            "/storage/emulated/0",
            "/mnt/user/0/emulated/0",
            "/mnt/runtime/write/emulated/0",
            "/mnt/runtime/default/emulated/0",
        ).map { File(it).toPath() }

        internal fun privilegedPortRedirectScript(sourcePort: Int, targetPort: Int): String {
            return """
                IPTABLES="${'$'}(command -v iptables 2>/dev/null || echo /system/bin/iptables)"
                if [ ! -x "${'$'}IPTABLES" ]; then
                  echo "iptables not found. id=${'$'}(id 2>&1)" >&2
                  exit 127
                fi
                echo "redirect $sourcePort->$targetPort id=${'$'}(id 2>&1) iptables=${'$'}IPTABLES version=${'$'}("${'$'}IPTABLES" --version 2>&1)"
                "${'$'}IPTABLES" -t nat -D PREROUTING -p tcp --dport $sourcePort -j REDIRECT --to-ports $targetPort 2>/dev/null || true
                "${'$'}IPTABLES" -t nat -D OUTPUT -p tcp --dport $sourcePort -j REDIRECT --to-ports $targetPort 2>/dev/null || true
                "${'$'}IPTABLES" -t nat -A PREROUTING -p tcp --dport $sourcePort -j REDIRECT --to-ports $targetPort
            """.trimIndent()
        }

        internal fun privilegedPortRemoveRedirectScript(sourcePort: Int, targetPort: Int): String {
            return """
                IPTABLES="${'$'}(command -v iptables 2>/dev/null || echo /system/bin/iptables)"
                if [ -x "${'$'}IPTABLES" ]; then
                  "${'$'}IPTABLES" -t nat -D PREROUTING -p tcp --dport $sourcePort -j REDIRECT --to-ports $targetPort 2>/dev/null || true
                  "${'$'}IPTABLES" -t nat -D OUTPUT -p tcp --dport $sourcePort -j REDIRECT --to-ports $targetPort 2>/dev/null || true
                fi
            """.trimIndent()
        }

        internal fun removePrivilegedPortRedirect(sourcePort: Int, targetPort: Int) {
            Shell.batch("su", privilegedPortRemoveRedirectScript(sourcePort, targetPort))
        }

        internal fun ensureUserHome(hostKeyPath: Path) {
            val userHome = System.getProperty("user.home")
            if (!userHome.isNullOrBlank()) return

            val fallback = hostKeyPath.parent?.toFile()
                ?: File(System.getProperty("java.io.tmpdir") ?: ".")
            fallback.mkdirs()
            System.setProperty("user.home", fallback.absolutePath)
        }

        private fun scpAwareShellFactory(delegate: ShellFactory): ScpCommandFactory {
            return ScpCommandFactory().apply {
                delegateShellFactory = delegate
            }
        }
    }

    private fun redirectPrivilegedPort(sourcePort: Int, targetPort: Int) {
        if (!Shell.isRooted) {
            throw SecurityException("Binding port $sourcePort requires system/root permission. Root is required to redirect $sourcePort to $targetPort.")
        }

        val result = Shell.batch("su", privilegedPortRedirectScript(sourcePort, targetPort))
        if (result.code != 0) {
            val reason = result.message.ifBlank { "no command output; su or iptables may be blocked by the device." }
            throw SecurityException("Failed to redirect port $sourcePort to $targetPort: $reason")
        }
    }

    private fun removePrivilegedPortRedirect() {
        val sourcePort = redirectedPort ?: return
        removePrivilegedPortRedirect(sourcePort, actualPort)
        redirectedPort = null
    }
}
