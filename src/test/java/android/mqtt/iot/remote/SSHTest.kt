package android.mqtt.iot.remote

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Paths

class SSHTest {

    @Test
    fun selectsFirstSharedStoragePathThatCanActuallyBeListed() {
        val first = Paths.get("/storage/emulated/0")
        val second = Paths.get("/mnt/user/0/emulated/0")

        assertTrue(SSH.selectAccessibleSharedStorage(listOf(first, second)) { it == second } == second)
    }

    @Test
    fun openStartsSshServerAndCloseStopsIt() {
        val keyFile = File.createTempFile("iot-ssh-hostkey", ".ser")
        keyFile.delete()

        val ssh = SSH(
            host = "127.0.0.1",
            port = 22222,
            username = "admin",
            password = "123456",
            hostKeyPath = keyFile.toPath(),
            shellCommand = listOf(shellPath()),
        )

        ssh.open()
        assertTrue(ssh.isOpen())

        ssh.close()
        assertFalse(ssh.isOpen())
        keyFile.delete()
    }

    @Test
    fun openStartsWhenUserHomeIsBlank() {
        val originalUserHome = System.getProperty("user.home")
        val keyFile = File.createTempFile("iot-ssh-hostkey", ".ser")
        keyFile.delete()

        try {
            System.setProperty("user.home", "")
            val ssh = SSH(
                host = "127.0.0.1",
                port = 22223,
                username = "admin",
                password = "123456",
                hostKeyPath = keyFile.toPath(),
                shellCommand = listOf(shellPath()),
            )

            ssh.open()
            assertTrue(ssh.isOpen())
            assertTrue(System.getProperty("user.home")?.isNotBlank() == true)
            ssh.close()
        } finally {
            if (originalUserHome == null) {
                System.clearProperty("user.home")
            } else {
                System.setProperty("user.home", originalUserHome)
            }
            keyFile.delete()
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun openRejectsBlankUsername() {
        SSH(
            host = "127.0.0.1",
            port = 22222,
            username = "",
            password = "123456",
            shellCommand = listOf(shellPath()),
        ).open()
    }

    @Test
    fun privilegedPortUsesFallbackBindPort() {
        assertTrue(SSH.bindPort(443, 2222) == 2222)
        assertTrue(SSH.bindPort(2222, 2223) == 2222)
    }

    @Test
    fun privilegedPortRedirectScriptIncludesDiagnosticsAndFallbackIptablesPath() {
        val script = SSH.privilegedPortRedirectScript(443, 2222)

        assertTrue(script.contains("/system/bin/iptables"))
        assertTrue(script.contains("id=$"))
        assertTrue(script.contains("--dport 443"))
        assertTrue(script.contains("--to-ports 2222"))
        assertTrue(script.contains("-D OUTPUT"))
        assertFalse(script.contains("-A OUTPUT"))
    }

    private fun shellPath(): String {
        return if (File("/bin/sh").exists()) "/bin/sh" else "cmd.exe"
    }
}
