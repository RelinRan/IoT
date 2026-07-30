package androidx.iot.data

/**
 * 阿里物联网 - 设备证书一键复制内容
 * $deviceId.ini 文件，放入sdcard根目录下
 */
data class License(
    val ProductKey: String,
    val DeviceName: String,
    val DeviceSecret: String
)
