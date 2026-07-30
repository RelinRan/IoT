package androidx.iot.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Paths
import java.util.TreeMap

class SftpPathAliasTest {
    private val shared = Paths.get("/storage/emulated/0")
    private val shadow = Paths.get("/data/user/0/app/cache/sftp-shadow")

    @Test
    fun mapsOnlySdcardAliasToSharedStorage() {
        assertEquals(shared, resolveSftpAlias(Paths.get("/sdcard"), shared))
        assertEquals(shared.resolve("Download/file.zip"), resolveSftpAlias(Paths.get("/sdcard/Download/file.zip"), shared))
        assertEquals(Paths.get("/storage"), resolveSftpAlias(Paths.get("/storage"), shared))
        assertEquals(Paths.get("/storage/emulated"), resolveSftpAlias(Paths.get("/storage/emulated"), shared))
    }

    @Test
    fun normalizesParentNavigationWithinSdcardAlias() {
        assertEquals(shared, resolveSftpAlias(Paths.get("/sdcard/Download/.."), shared))
        assertEquals(shared.resolve("Pictures"), resolveSftpAlias(Paths.get("/sdcard/Download/../Pictures"), shared))
        assertEquals(Paths.get("/"), resolveSftpAlias(Paths.get("/sdcard/.."), shared))
    }

    @Test
    fun resolvesSdcardFromRemotePathBeforeFollowingSystemSymlink() {
        assertEquals(shared, resolveRemoteSftpPath(Paths.get("/"), "/sdcard", shared, shadow))
        assertEquals(shared.resolve("Download"), resolveRemoteSftpPath(Paths.get("/"), "/sdcard/Download", shared, shadow))
    }

    @Test
    fun mapsRestrictedStorageParentsToAccessibleShadowDirectories() {
        assertEquals(shadow.resolve("storage"), resolveRemoteSftpPath(Paths.get("/"), "/storage", shared, shadow))
        assertEquals(shadow.resolve("storage/emulated"), resolveRemoteSftpPath(Paths.get("/"), "/storage/emulated", shared, shadow))
        assertEquals(shadow.resolve("storage/self"), resolveRemoteSftpPath(Paths.get("/"), "/storage/self", shared, shadow))
        assertEquals(shared, resolveRemoteSftpPath(Paths.get("/"), "/storage/emulated/0", shared, shadow))
        assertEquals(shared, resolveRemoteSftpPath(Paths.get("/"), "/storage/self/primary", shared, shadow))
    }

    @Test
    fun reportsSdcardAliasAsDirectoryInsteadOfSymbolicLink() {
        val attributes = sftpSdcardDirectoryAttributes(123L)

        assertTrue(attributes["isDirectory"] == true)
        assertFalse(attributes["isSymbolicLink"] == true)
        assertEquals(0L, attributes["size"])
    }

    @Test
    fun normalizesAccessibleSymbolicLinksToTheirTargetType() {
        val link = TreeMap<String, Any>().apply {
            putAll(mapOf(
            "isDirectory" to false,
            "isRegularFile" to false,
            "isSymbolicLink" to true,
            ))
        }
        val directory = mapOf<String, Any>("isDirectory" to true, "isRegularFile" to false, "size" to 0L)
        val file = mapOf<String, Any>("isDirectory" to false, "isRegularFile" to true, "size" to 128L)

        val directoryResult = normalizeSftpSymbolicLinkAttributes(link, directory)
        val fileResult = normalizeSftpSymbolicLinkAttributes(link, file)
        val brokenResult = normalizeSftpSymbolicLinkAttributes(link, null)

        assertTrue(directoryResult["isDirectory"] == true)
        assertFalse(directoryResult["isSymbolicLink"] == true)
        assertTrue(fileResult["isRegularFile"] == true)
        assertEquals(128L, fileResult["size"])
        assertTrue(brokenResult["isSymbolicLink"] == true)
    }

    @Test
    fun keepsRootAndCanonicalSharedStoragePathsUnchanged() {
        assertEquals(Paths.get("/"), resolveSftpAlias(Paths.get("/"), shared))
        assertEquals(shared, resolveSftpAlias(shared, shared))
        assertEquals(shared.resolve("Pictures"), resolveSftpAlias(shared.resolve("Pictures"), shared))
    }
}
