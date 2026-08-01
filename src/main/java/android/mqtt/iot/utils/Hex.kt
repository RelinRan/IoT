package android.mqtt.iot.utils

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
    require(hex.length % 2 == 0) { "Hex string must have an even length" }
    return ByteArray(hex.length / 2) { i ->
        val startIndex = i * 2
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
