package android.mqtt.iot.remote

import org.apache.sshd.sftp.server.SftpFileSystemAccessor
import org.apache.sshd.sftp.server.SftpSubsystemProxy
import java.nio.file.Path
import java.nio.file.LinkOption
import java.nio.file.Files
import java.nio.file.attribute.FileTime
import java.util.NavigableMap
import java.util.TreeMap

internal fun resolveSftpAlias(path: Path, sharedStorage: Path): Path {
    val normalized = path.normalize()
    val canonicalShared = sharedStorage.normalize()
    if (normalized.startsWith(canonicalShared)) return normalized
    val text = normalized.toString().replace('\\', '/')
    val alias = when {
        text == "/sdcard" || text == "sdcard" -> ""
        text.startsWith("/sdcard/") -> text.removePrefix("/sdcard/")
        text.startsWith("sdcard/") -> text.removePrefix("sdcard/")
        else -> return normalized
    }
    return if (alias.isEmpty()) canonicalShared else canonicalShared.resolve(alias).normalize()
}

internal fun resolveRemoteSftpPath(
    rootDir: Path,
    remotePath: String,
    sharedStorage: Path,
    shadowRoot: Path? = null,
): Path {
    val normalizedRemote = remotePath.replace('\\', '/')
    val remote = if (normalizedRemote.startsWith('/')) normalizedRemote else "/$normalizedRemote"
    if (remote == "/storage/emulated/0" || remote.startsWith("/storage/emulated/0/")) {
        return sharedStorage.resolve(remote.removePrefix("/storage/emulated/0").removePrefix("/")).normalize()
    }
    if (remote == "/storage/self/primary" || remote.startsWith("/storage/self/primary/")) {
        return sharedStorage.resolve(remote.removePrefix("/storage/self/primary").removePrefix("/")).normalize()
    }
    if (shadowRoot != null && (remote == "/storage" || remote == "/storage/emulated" || remote == "/storage/self")) {
        return shadowRoot.resolve(remote.removePrefix("/")).normalize()
    }
    val logical = rootDir.resolve(remote.removePrefix("/")).normalize()
    return resolveSftpAlias(logical, sharedStorage)
}

internal fun sftpSdcardDirectoryAttributes(timestamp: Long): Map<String, Any> {
    val time = FileTime.fromMillis(timestamp)
    return linkedMapOf(
        "lastModifiedTime" to time,
        "lastAccessTime" to time,
        "creationTime" to time,
        "size" to 0L,
        "isRegularFile" to false,
        "isDirectory" to true,
        "isSymbolicLink" to false,
        "isOther" to false,
        "fileKey" to "sdcard-alias",
    )
}

internal fun normalizeSftpSymbolicLinkAttributes(
    linkAttributes: NavigableMap<String, Any>,
    targetAttributes: Map<String, *>?,
): NavigableMap<String, Any> {
    if (linkAttributes["isSymbolicLink"] != true || targetAttributes == null) return TreeMap(linkAttributes)
    return TreeMap<String, Any>(linkAttributes).apply {
        targetAttributes.forEach { (key, value) -> if (value != null) put(key, value) }
        put("isSymbolicLink", false)
        put("isDirectory", targetAttributes["isDirectory"] == true)
        put("isRegularFile", targetAttributes["isRegularFile"] == true)
        put("isOther", targetAttributes["isOther"] == true)
    }
}

internal class SftpPathAliasAccessor(
    private val sharedStorage: Path,
    private val shadowRoot: Path,
) : SftpFileSystemAccessor {
    override fun resolveLocalFilePath(
        subsystem: SftpSubsystemProxy,
        rootDir: Path,
        remotePath: String,
    ): Path {
        return resolveRemoteSftpPath(rootDir, remotePath, sharedStorage, shadowRoot)
    }

    override fun readFileAttributes(
        subsystem: SftpSubsystemProxy,
        file: Path,
        view: String,
        vararg linkOptions: LinkOption,
    ): Map<String, *> {
        if (isSdcardAliasPath(file)) return sftpSdcardDirectoryAttributes(System.currentTimeMillis())
        return SftpFileSystemAccessor.DEFAULT.readFileAttributes(subsystem, file, view, *linkOptions)
    }

    override fun resolveReportedFileAttributes(
        subsystem: SftpSubsystemProxy,
        file: Path,
        flags: Int,
        attrs: NavigableMap<String, Any>,
        vararg linkOptions: LinkOption,
    ): NavigableMap<String, Any> {
        if (isSdcardAliasPath(file)) {
            return TreeMap<String, Any>(attrs).apply {
                putAll(sftpSdcardDirectoryAttributes(System.currentTimeMillis()))
            }
        }
        if (!Files.isSymbolicLink(file)) return attrs
        val targetAttributes = runCatching {
            SftpFileSystemAccessor.DEFAULT.readFileAttributes(subsystem, file, "basic:*")
        }.getOrNull()
        return normalizeSftpSymbolicLinkAttributes(attrs, targetAttributes)
    }

    private fun isSdcardAliasPath(file: Path): Boolean {
        val value = file.normalize().toString().replace('\\', '/')
        return value == "/sdcard" || value == "sdcard"
    }

}
