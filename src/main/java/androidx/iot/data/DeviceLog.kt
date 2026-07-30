package androidx.iot.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 阿里云设备日志级别。 */
enum class DeviceLogLevel { FATAL, ERROR, WARN, INFO, DEBUG }

/** 待上报的设备日志。 */
data class DeviceLogEntry(
    val utcTime: String = now(),
    val logLevel: DeviceLogLevel = DeviceLogLevel.INFO,
    val module: String = "default",
    val code: String? = null,
    val traceContext: String? = null,
    val logContent: String,
) {
    companion object {
        private fun now(): String =
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US).format(Date())
    }
}

internal data class DeviceLogConfigParams(
    val configScope: String = "device",
    val getType: String = "content",
)

internal data class DeviceLogConfigContent(val mode: Int = 0)

internal data class DeviceLogConfigData(
    val getType: String? = null,
    val content: DeviceLogConfigContent? = null,
)
