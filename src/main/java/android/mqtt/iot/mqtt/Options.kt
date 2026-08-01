package android.mqtt.iot.mqtt

import android.content.Context
import android.mqtt.iot.link.AuthType


data class Options(
    val context: Context,
    val authType: AuthType = AuthType.CONNECT,
    val host: String = "",
    val clientId: String = "",
    val username: String = "",
    val password: String = "",
    val productKey: String = "",
    val deviceName: String = "",
    val remote: Boolean = true,
)
