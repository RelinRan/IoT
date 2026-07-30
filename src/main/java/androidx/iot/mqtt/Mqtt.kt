package androidx.iot.mqtt

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import androidx.iot.link.AuthType
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

/**
 * MQTT连接基础类
 */
class Mqtt(context: Context, options: Options, callback: MqttCallback?) {

    private val TAG = "Mqtt"

    /**
     * 客户端
     */
    private var client: MqttAndroidClient? = null

    /**
     * MQTT连接参数
     */
    private var connOpts: MqttConnectOptions

    /**
     * 初始化
     */
    init {
        val applicationContext = context.applicationContext
        val isNetworkAvailable = isNetworkAvailable(applicationContext)
        when (options.authType) {
            AuthType.CONNECT, AuthType.CONNWL -> {
                client = MqttAndroidClient(applicationContext, options.host, options.clientId)
                if (isNetworkAvailable){
                    client?.registerResources(applicationContext)
                }
                client?.setCallback(callback)
                connOpts = MqttConnectOptions()
                connOpts.connectionTimeout = 15
                connOpts.keepAliveInterval = 60
                connOpts.userName = options.username
                connOpts.password = options.password.toCharArray()
                connOpts.isAutomaticReconnect = true
                connOpts.isCleanSession = false
            }

            AuthType.REGISTER, AuthType.REGNWL -> {
                client = MqttAndroidClient(
                    applicationContext,
                    options.host,
                    options.clientId,
                    MemoryPersistence()
                )
                if (isNetworkAvailable){
                    client?.registerResources(applicationContext)
                }
                client?.setCallback(callback)
                connOpts = MqttConnectOptions()
                connOpts.connectionTimeout = 15
                connOpts.mqttVersion = 4// MQTT 3.1.1
                connOpts.userName = options.username
                connOpts.password = options.password.toCharArray()
                connOpts.isAutomaticReconnect = false//MQTT动态注册协议规定必须关闭自动重连
            }
        }
    }

    /**
     * 是否已连接
     * @return 是否连接
     */
    fun isConnected(): Boolean {
        return client?.isConnected == true
    }

    /**
     * 连接服务端
     * @param callback 连接监听
     */
    fun connect(context: Context,callback: IMqttActionListener?) {
        if (isNetworkAvailable(context)){
            client?.connect(connOpts, context, callback)
        }
    }

    /**
     * 向 MQTT 服务器发布消息
     * @param topic 指定消息要发布到的 MQTT 主题
     * @param payload 消息的实际内容，以字节数组的形式表示
     * @param qos 指定消息的服务质量等级
     *      0：最多一次（At most once）。消息可能会丢失，不会进行重传，消息只发送一次。这是最快但不可靠的服务质量等级。
     *      1：至少一次（At least once）。消息至少会被传递一次，如果发送失败会进行重传，确保消息最终到达接收方，但可能会有重复消息。
     *      2：恰好一次（Exactly once）。消息只会被传递一次，不会丢失也不会重复，通过复杂的握手协议来保证消息的准确性，但性能相对较低。
     * @param retained 指定消息是否为保留消息。如果设置为 true，MQTT 服务器会将该消息保留，并在新的客户端订阅该主题时立即将保留消息发送给客户端。如果设置为 false，则消息不会被保留。
     * @param userContext 用户自定义的上下文对象，可以是任何类型的对象
     * @param callback 一个实现了 IMqttActionListener 接口的对象，用于异步处理消息发布的结果
     */
    fun public(
        topic: String,
        payload: ByteArray,
        qos: Int,
        retained: Boolean,
        userContext: Any?,
        callback: IMqttActionListener?
    ) {
        client?.publish(topic, payload, qos, retained, userContext, callback)
    }

    /**
     * 向 MQTT 服务器发布消息
     * @param topic 指定消息要发布到的 MQTT 主题
     * @param payload 消息的实际内容，以字节数组的形式表示
     * @param qos 指定消息的服务质量等级
     *      0：最多一次（At most once）。消息可能会丢失，不会进行重传，消息只发送一次。这是最快但不可靠的服务质量等级。
     *      1：至少一次（At least once）。消息至少会被传递一次，如果发送失败会进行重传，确保消息最终到达接收方，但可能会有重复消息。
     *      2：恰好一次（Exactly once）。消息只会被传递一次，不会丢失也不会重复，通过复杂的握手协议来保证消息的准确性，但性能相对较低。
     * @param retained 指定消息是否为保留消息。如果设置为 true，MQTT 服务器会将该消息保留，并在新的客户端订阅该主题时立即将保留消息发送给客户端。如果设置为 false，则消息不会被保留。
     */
    fun public(topic: String, payload: ByteArray, qos: Int, retained: Boolean) {
        client?.publish(topic, payload, qos, retained)
    }

    /**
     * 向 MQTT 服务器发布消息
     * @param topic 指定消息要发布到的 MQTT 主题
     * @param payload 消息的实际内容，以字节数组的形式表示
     */
    fun publish(topic: String, payload: String) {
        client?.publish(topic, payload.toByteArray(), 0, false)
    }

    /**
     * 订阅 MQTT 主题
     * @param topic 数组中的每个元素代表一个要订阅的 MQTT 主题
     * @param qos 服务质量等级数组，与 topic 数组一一对应
     * @param userContext 用户自定义的上下文对象，可以是任意类型的对象
     * @param callback 用于异步处理订阅操作的结果
     */
    fun subscribe(
        topic: Array<String>,
        qos: IntArray,
        userContext: Any?,
        callback: IMqttActionListener?
    ) {
        client?.subscribe(topic, qos, userContext, callback)
    }

    /**
     * 订阅 MQTT 主题
     * @param topic 数组中的每个元素代表一个要订阅的 MQTT 主题
     */
    fun subscribe(topic: Array<String>) {
        client?.subscribe(topic, IntArray(topic.size) { 0 })
    }

    /**
     * 订阅 MQTT 主题
     * @param topic 要订阅的 MQTT 主题名称
     * @param qos 指定订阅该主题的服务质量等级
     *      0：最多一次（At most once），消息可能会丢失，不保证消息一定能到达。
     *      1：至少一次（At least once），消息至少会被传递一次，如果发送失败会进行重传，但可能会有重复消息。
     *      2：恰好一次（Exactly once），消息只会被传递一次，不会丢失也不会重复，通过复杂的握手协议来保证消息的准确性。
     * @param userContext 用户自定义的上下文对象，可以是任意类型的对象
     * @param callback 用于异步处理订阅操作的结果
     */
    fun subscribe(topic: String, qos: Int, userContext: Any?, callback: IMqttActionListener?) {
        client?.subscribe(topic, qos, userContext, callback)
    }

    /**
     * 订阅 MQTT 主题
     * @param topic 要订阅的 MQTT 主题名称
     * @param qos 指定订阅该主题的服务质量等级
     *      0：最多一次（At most once），消息可能会丢失，不保证消息一定能到达。
     *      1：至少一次（At least once），消息至少会被传递一次，如果发送失败会进行重传，但可能会有重复消息。
     *      2：恰好一次（Exactly once），消息只会被传递一次，不会丢失也不会重复，通过复杂的握手协议来保证消息的准确性。
     */
    fun subscribe(topic: String, qos: Int) {
        client?.subscribe(topic, qos)
    }

    /**
     * 取消订阅指定主题
     * @param topic 要订阅的 MQTT 主题名称数组
     * @param userContext 用户自定义的上下文对象，可以是任意类型的对象
     * @param callback 用于异步处理订阅操作的结果
     */
    fun unsubscribe(topic: Array<String>, userContext: Any?, callback: IMqttActionListener?) {
        client?.unsubscribe(topic, userContext, callback)
    }

    /**
     * 取消订阅指定主题
     * @param topic 要订阅的 MQTT 主题名称数组
     */
    fun unsubscribe(topic: Array<String>) {
        client?.unsubscribe(topic)
    }

    /**
     * 取消订阅指定主题
     * @param topic 要订阅的 MQTT 主题名称
     * @param userContext 用户自定义的上下文对象，可以是任意类型的对象
     * @param callback 用于异步处理订阅操作的结果
     */
    fun unsubscribe(topic: String, userContext: Any?, callback: IMqttActionListener?) {
        client?.unsubscribe(topic, userContext, callback)
    }

    /**
     * 取消订阅指定主题
     * @param topic 要订阅的 MQTT 主题名称
     */
    fun unsubscribe(topic: String) {
        client?.unsubscribe(topic)
    }

    /**
     * 断开连接
     */
    fun disconnect() {
        Log.d(TAG, "disconnect")
        client?.let {
            it.unregisterResources()
            it.disconnect()
        }
    }

    /**
     * 当前网络是否可用
     */
   private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        // 对于 Android 10（API 级别 29）及以上版本
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val network = connectivityManager.activeNetwork
            if (network != null) {
                val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
                return networkCapabilities != null && (
                        networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                        )
            }
        }
        // 对于 Android 5.0（API 级别 21）到 Android 9（API 级别 28）
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val networks = connectivityManager.allNetworks
            for (net in networks) {
                val networkInfo = connectivityManager.getNetworkInfo(net)
                if (networkInfo != null && networkInfo.isConnected) {
                    return true
                }
            }
        }
        // 对于 Android 5.0 以下版本
        else {
            val networkInfo = connectivityManager.activeNetworkInfo
            return networkInfo != null && networkInfo.isConnected
        }
        return false
    }


}
