package android.mqtt.iot.mqtt

import android.content.Context
import android.text.TextUtils
import android.mqtt.iot.link.AuthType
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.Random
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object OptionsBuilder {


    private const val ALGORITHM = "hmacsha256"


    private const val AREA = "cn-shanghai"


    private const val PORT = 1883


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
            id.append(clientId)
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
            id.append(productKey).append(".").append(deviceName)
            id.append("|securemode=").append(secureMode)
            id.append(",authType=").append(authType)
            id.append(",signmethod=").append(ALGORITHM)
            id.append(",random=").append(random)
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
