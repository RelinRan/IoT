package androidx.iot.data

/** 阿里云设备端通用结果码。 */
object DeviceCommonCode {
    const val SUCCESS = 200
    const val PARAMETER_ERROR = 400
    const val UNAUTHORIZED = 401
    const val FORBIDDEN = 403
    const val NOT_FOUND = 404
    const val INTERNAL_ERROR = 500
}

data class GatewayDevice(
    val productKey: String,
    val deviceName: String,
    val clientId: String? = null,
    val sign: String? = null,
    val signMethod: String? = null,
    val timestamp: String? = null,
)

data class DeviceTag(val tagKey: String, val tagValue: String? = null)

data class SubDeviceSession(val productKey: String, val deviceName: String)
