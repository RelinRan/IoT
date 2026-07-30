package androidx.iot.data

/**
 * 下行数据
 */
data class ServiceProperty<T>(
    val method: String,
    val id: String,
    val version: String,
    val params: T,
)
