package androidx.iot.data

/**
 * OTA参数
 * @param version OTA模块版本
 * @param module  OTA模块名
 *               上报默认（default）模块的版本号时，可以不上报module参数。
 *               设备的默认（default）模块的版本号代表整个设备的固件版本号。
 */
data class OTAVersion(val version: String, val module: String = "default")
