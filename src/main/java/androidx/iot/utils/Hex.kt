package androidx.iot.utils

fun String.toHex(): String {
    val bytes = this.toByteArray()
    val sb = StringBuilder()
    for (byte in bytes) {
        sb.append(String.format("%02X", byte))
    }
    return sb.toString()
}

fun String.hexToByteArray(): ByteArray {
    val hex = replace(" ", "")
    // 检查十六进制字符串的长度是否为偶数
    require(hex.length % 2 == 0) { "Hex string must have an even length" }
    // 创建一个长度为十六进制字符串长度一半的 ByteArray
    return ByteArray(hex.length / 2) { i ->
        // 从十六进制字符串中取出每两个字符
        val startIndex = i * 2
        // 将这两个十六进制字符转换为一个字节
        hex.substring(startIndex, startIndex + 2).toInt(16).toByte()
    }
}

fun String.hexToString(): String {
    check(length % 2 == 0) { "Must have an even length" }
    return chunked(2)
        .map { it.toInt(16).toByte() }
        .toByteArray()
        .toString(Charsets.UTF_8)
}
