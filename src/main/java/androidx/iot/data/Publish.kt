package androidx.iot.data

data class Publish<T>(
    val id: String,
    val version: String? = null,
    val sys:Sys? = null,
    val params: T,
    val method: String? = null
)
