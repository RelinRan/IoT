package android.mqtt.iot.mqtt

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import android.mqtt.iot.link.AuthType
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence


class Mqtt(context: Context, options: Options, callback: MqttCallback?) {

    private val TAG = "Mqtt"


    private var client: MqttAndroidClient? = null


    private var connOpts: MqttConnectOptions


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
                connOpts.isAutomaticReconnect = false
            }
        }
    }


    fun isConnected(): Boolean {
        return client?.isConnected == true
    }


    fun connect(context: Context,callback: IMqttActionListener?) {
        if (isNetworkAvailable(context)){
            client?.connect(connOpts, context, callback)
        }
    }


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


    fun public(topic: String, payload: ByteArray, qos: Int, retained: Boolean) {
        client?.publish(topic, payload, qos, retained)
    }


    fun publish(topic: String, payload: String) {
        client?.publish(topic, payload.toByteArray(), 0, false)
    }


    fun subscribe(
        topic: Array<String>,
        qos: IntArray,
        userContext: Any?,
        callback: IMqttActionListener?
    ) {
        client?.subscribe(topic, qos, userContext, callback)
    }


    fun subscribe(topic: Array<String>) {
        client?.subscribe(topic, IntArray(topic.size) { 0 })
    }


    fun subscribe(topic: String, qos: Int, userContext: Any?, callback: IMqttActionListener?) {
        client?.subscribe(topic, qos, userContext, callback)
    }


    fun subscribe(topic: String, qos: Int) {
        client?.subscribe(topic, qos)
    }


    fun unsubscribe(topic: Array<String>, userContext: Any?, callback: IMqttActionListener?) {
        client?.unsubscribe(topic, userContext, callback)
    }


    fun unsubscribe(topic: Array<String>) {
        client?.unsubscribe(topic)
    }


    fun unsubscribe(topic: String, userContext: Any?, callback: IMqttActionListener?) {
        client?.unsubscribe(topic, userContext, callback)
    }


    fun unsubscribe(topic: String) {
        client?.unsubscribe(topic)
    }


    fun disconnect() {
        Log.d(TAG, "disconnect")
        client?.let {
            it.unregisterResources()
            it.disconnect()
        }
    }


   private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
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
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val networks = connectivityManager.allNetworks
            for (net in networks) {
                val networkInfo = connectivityManager.getNetworkInfo(net)
                if (networkInfo != null && networkInfo.isConnected) {
                    return true
                }
            }
        }
        else {
            val networkInfo = connectivityManager.activeNetworkInfo
            return networkInfo != null && networkInfo.isConnected
        }
        return false
    }


}
