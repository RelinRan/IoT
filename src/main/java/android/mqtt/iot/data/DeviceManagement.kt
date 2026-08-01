package android.mqtt.iot.data


enum class DeviceJobStatus { QUEUED, SENT, IN_PROGRESS, TIMED_OUT, FAILED, SUCCEEDED, CANCELLED, REJECTED, REMOVED }

data class DeviceJobFile(
    val signMethod: String? = null,
    val sign: String? = null,
    val fileUrl: String? = null,
)

data class DeviceJob(
    val taskId: String,
    val status: DeviceJobStatus? = null,
    val jobDocument: Map<String, Any?>? = null,
    val jobFile: DeviceJobFile? = null,
)

data class DeviceJobNotification(val task: DeviceJob)

data class DeviceJobQueryData(
    val statusDetails: Map<String, Any?>? = null,
    val taskId: String? = null,
    val task: DeviceJob? = null,
)

data class DeviceDistributionNotification(val cmd: Int = 0)

data class RemoteConfigFile(
    val configId: String? = null,
    val configSize: Long? = null,
    val sign: String? = null,
    val signMethod: String? = null,
    val url: String? = null,
    val getType: String? = null,
)

data class RemoteConfigPushMessage(
    val id: String,
    val version: String? = null,
    val params: RemoteConfigFile,
    val method: String? = null,
)

data class DeviceJobNotifyMessage(
    val id: String,
    val version: String? = null,
    val params: DeviceJobNotification,
)
