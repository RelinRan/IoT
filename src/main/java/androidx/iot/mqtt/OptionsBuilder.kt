package androidx.iot.mqtt

import android.content.Context
import android.text.TextUtils
import androidx.iot.link.AuthType
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.Random
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object OptionsBuilder {

    /**
     * 签名算法。目前支持hmacmd5、hmacsha1、hmacsha256。
     */
    private const val ALGORITHM = "hmacsha256"

    /**
     * 服务区域
     */
    private const val AREA = "cn-shanghai"

    /**
     * 服务端口
     */
    private const val PORT = 1883


    /**
     * 一机一密、一型一密预注册认证方式：使用设备证书（ProductKey、DeviceName和DeviceSecret）连接
     *
     * @param context      上下文
     * @param productKey   产品秘钥
     * @param deviceName   设备名称
     * @param deviceSecret 设备机密
     * @return
     */
    fun connect(
        context:Context,
        productKey: String,
        deviceName: String,
        deviceSecret: String,
        enableRemoteLogin: Boolean = true,
    ): Options {
        try {
            val timestamp = System.currentTimeMillis().toString()
            val id = StringBuilder()
            //客户端ID，可自定义，长度在64个字符内。建议使用设备的MAC地址或SN码，方便您识别区分不同的客户端
            id.append(productKey).append(".").append(deviceName)
            id.append("|timestamp=").append(timestamp)
            id.append(",_v=paho-android-1.0.0")
            id.append(",securemode=2")
            id.append(",signmethod=").append(ALGORITHM) //HmacSHA256
            id.append("|")
            val clientId = id.toString()
            val username = deviceName + "&" + productKey
            val content = StringBuilder()
            content.append("clientId").append(productKey).append(".").append(deviceName)
            content.append("deviceName").append(deviceName)
            content.append("productKey").append(productKey)
            content.append("timestamp").append(timestamp)
            val mac = Mac.getInstance(ALGORITHM)
            val secretKeySpec = SecretKeySpec(deviceSecret.toByteArray(), ALGORITHM)
            mac.init(secretKeySpec)
            val macRes = mac.doFinal(content.toString().toByteArray())
            val password = String.format("%064x", BigInteger(1, macRes))
            return Options(
                context,
                AuthType.CONNECT,
                host(false, productKey, AREA, PORT),
                clientId,
                username,
                password,
                productKey,
                deviceName,
                enableRemoteLogin,
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return Options(context,AuthType.CONNECT, host(false, productKey, AREA, PORT), "", "", "")
    }

    /**
     * 一型一密免预注册认证方式：使用ProductKey、DeviceName、ClientID、DeviceToken连接
     *
     * @param context     上下文
     * @param clientId    客户端id
     * @param productKey  产品key
     * @param deviceName  设备名称
     * @param deviceToken 设备令牌
     * @return
     */
    fun connwl(
        context:Context,
        clientId: String,
        productKey: String,
        deviceName: String,
        deviceToken: String,
        enableRemoteLogin: Boolean = true,
    ): Options {
        try {
            val timestamp = System.currentTimeMillis().toString()
            val id = java.lang.StringBuilder()
            id.append(clientId) //客户端ID，可自定义，长度在64个字符内。建议使用设备的MAC地址或SN码，方便您识别区分不同的客户端
            id.append("|timestamp=").append(timestamp)
            id.append(",_v=paho-android-1.0.0")
            id.append(",securemode=-2")
            id.append(",authType=connwl|")
            val mClientId = id.toString()
            val username = "$deviceName&$productKey"
            val password = deviceToken
            return Options(
                context,
                AuthType.CONNWL,
                host(false, productKey, AREA, PORT),
                mClientId,
                username,
                password,
                productKey,
                deviceName,
                enableRemoteLogin,
            )
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        return Options(context,AuthType.CONNWL)
    }

    /**
     * 动态注册
     *
     * @param context       上下文
     * @param secureMode    安全模式。
     *                      一型一密预注册认证方式：固定取值为2。
     *                      一型一密免预注册认证方式：固定取值为-2。
     * @param authType      一型一密认证方式，不同类型将返回不同的认证参数：
     *                      register：一型一密预注册认证方式,主题：/ext/register,返回 deviceSecret、productKey、deviceName
     *                      regnwl：一型一密免预注册认证方式,主题：/ext/regnwl,返回 clientId、productKey、deviceName、deviceToken
     * @param instanceId    实例ID
     * @param productKey    产品key
     * @param productSecret 产品secret
     * @param deviceName    设备名称
     * @return
     */
    fun register(
        context: Context,
        secureMode: Int = 2,
        authType: String = "register",
        instanceId: String,
        productKey: String,
        productSecret: String,
        deviceName: String,
        enableRemoteLogin: Boolean = true,
    ): Options {
        val optionsType = if (authType == "register") AuthType.REGISTER else AuthType.REGNWL
        try {
            val random = Random().nextInt(1000000)
            val id = java.lang.StringBuilder()
            //客户端ID，可自定义，长度在64个字符内。建议使用设备的MAC地址或SN码，方便您识别区分不同的客户端
            id.append(productKey).append(".").append(deviceName)
            id.append("|securemode=").append(secureMode)
            id.append(",authType=").append(authType)
            id.append(",signmethod=").append(ALGORITHM)
            id.append(",random=").append(random)
            //实例ID。请登录物联网平台控制台，在实例概览页面查看。
            if (!TextUtils.isEmpty(instanceId)) {
                id.append(",instanceId=").append(instanceId)
            }
            id.append("|")
            val clientId = id.toString()
            val username = "$deviceName&$productKey"
            val content = java.lang.StringBuilder()
            content.append("deviceName").append(deviceName)
            content.append("productKey").append(productKey)
            content.append("random").append(random)
            val password = encrypt(content.toString(), productSecret)

            return Options(
                context,
                optionsType,
                host(true, productKey, AREA, PORT),
                clientId,
                username,
                password,
                productKey,
                deviceName,
                enableRemoteLogin,
            )
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        return Options(context,optionsType)
    }


    /**
     * Mqtt服务器连接地址
     *
     * @param register   是否注册
     * @param productKey 产品key
     * @param area       服务器区域（例如：cn-shanghai）
     * @param port       端口（例如：443）
     * @return
     */
    fun host(
        register: Boolean,
        productKey: String?,
        area: String?,
        port: Int
    ): String {
        val sb = StringBuffer(if (register) "ssl://" else "tcp://")
        sb.append(productKey)
        sb.append(".iot-as-mqtt.")
        sb.append(area)
        sb.append(".aliyuncs.com:")
        sb.append(port)
        return sb.toString()
    }

    /**
     * 使用HMAC_ALGORITHM加密。
     *
     * @param content 明文
     * @param secret  密钥
     * @return 密文
     */
    private fun encrypt(content: String, secret: String): String {
        try {
            val text = content.toByteArray(StandardCharsets.UTF_8)
            val key = secret.toByteArray(StandardCharsets.UTF_8)
            val secretKey = SecretKeySpec(key, ALGORITHM)
            val mac = Mac.getInstance(secretKey.algorithm)
            mac.init(secretKey)
            return byte2hex(mac.doFinal(text))
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            return ""
        }
    }

    /**
     * 二进制转十六进制字符串。
     *
     * @param b 二进制数组
     * @return 十六进制字符串
     */
    private fun byte2hex(b: ByteArray?): String {
        val sb = StringBuffer()
        var n = 0
        while (b != null && n < b.size) {
            val stmp = Integer.toHexString(b[n].toInt() and 0XFF)
            if (stmp.length == 1) {
                sb.append('0')
            }
            sb.append(stmp)
            n++
        }
        return sb.toString().uppercase(Locale.getDefault())
    }

}
