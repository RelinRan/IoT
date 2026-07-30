package androidx.iot.data

data class Received<T>(
    val code: String,
    val data: T,
    val id: Long,
    val message: String,
    val method: String
)
