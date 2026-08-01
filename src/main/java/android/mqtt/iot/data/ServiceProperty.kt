package android.mqtt.iot.data


data class ServiceProperty<T>(
    val method: String,
    val id: String,
    val version: String,
    val params: T,
)
