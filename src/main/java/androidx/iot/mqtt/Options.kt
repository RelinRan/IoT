package androidx.iot.mqtt

import android.content.Context
import androidx.iot.link.AuthType

/**
 * MQTT参数
 * @param host 服务器地址
 * @param clientId 客户端id
 * @param username 用户名
 * @param password 密码
 * @param productKey 产品key
 * @param deviceName 设备名称
 * @param remote 远程登录
 */
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
