package android.mqtt.iot.link

import android.content.Context
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import android.mqtt.iot.data.OTAPackage
import android.mqtt.iot.data.DeviceJob
import android.mqtt.iot.data.DeviceJobNotifyMessage
import android.mqtt.iot.data.DeviceJobQueryData
import android.mqtt.iot.data.DeviceDistributionNotification
import android.mqtt.iot.data.DeviceLogConfigData
import android.mqtt.iot.data.DeviceLogEntry
import android.mqtt.iot.data.DeviceTag
import android.mqtt.iot.data.GatewayDevice
import android.mqtt.iot.data.SubDeviceSession
import android.mqtt.iot.data.RemoteConfigFile
import android.mqtt.iot.data.RemoteConfigPushMessage
import android.mqtt.iot.data.Received
import android.mqtt.iot.data.Register
import android.mqtt.iot.data.Regnwl
import android.mqtt.iot.data.ServiceProperty
import android.mqtt.iot.data.TunnelProxy
import android.mqtt.iot.mqtt.Mqtt
import android.mqtt.iot.mqtt.Options
import android.mqtt.iot.mqtt.OptionsBuilder
import android.mqtt.iot.utils.Store
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage


object LinkSDK : MqttCallbackExtended, IMqttActionListener {

    private const val TAG = "LinkSDK"


    private lateinit var mqtt: Mqtt


    private var gson: Gson = Gson()


    lateinit var options: Options

    private var scope = CoroutineScope(Dispatchers.IO)

    private var interval = 3


    var connectState: MutableState<Boolean> = mutableStateOf(value = false)


    var registerState: MutableState<Boolean> = mutableStateOf(value = false)


    var disableState: MutableState<Boolean> = mutableStateOf(value = false)


    var deleteState: MutableState<Boolean> = mutableStateOf(value = false)


    var upgradeState: MutableState<Received<OTAPackage>?> = mutableStateOf(value = null)


    var firmwareState: MutableState<Received<OTAPackage>?> = mutableStateOf(value = null)


    var servicePropertyState: MutableState<String?> = mutableStateOf(value = null)


    var secureTunnelState: MutableState<TunnelProxy?> = mutableStateOf(value = null)


    var logReportingEnabled: MutableState<Boolean> = mutableStateOf(value = false)


    var remoteConfigState: MutableState<RemoteConfigFile?> = mutableStateOf(value = null)


    var deviceJobState: MutableState<DeviceJob?> = mutableStateOf(value = null)


    var deviceDistributionState: MutableState<DeviceDistributionNotification?> = mutableStateOf(value = null)


    var managementMessageState: MutableState<String?> = mutableStateOf(value = null)
    private val secureTunnel = SecureTunnel(
        tunnelState = secureTunnelState,
        publishSecureTunnelProxy = { publishSecureTunnelProxy() },
        gson = gson,
    )
    private val operations = PSub(
        topics = { topics() },
        publish = { topic, payload -> publish(topic, payload) },
        subscribe = { topic, qos -> subscribe(topic, qos) },
        unsubscribe = { topic -> unsubscribe(topic) },
        gson = gson,
    )
    private var remote: Boolean = true


    private fun topics(): Topics {
        return Topics(options.productKey, options.deviceName)
    }

    private var time: Long = 0


    fun initialize(
        context: Context,
        secureMode: Int = 2,
        authType: String = "register",
        instanceId: String = "iot-public",
        productKey: String,
        productSecret: String,
        deviceName: String,
        remote: Boolean = true,
    ) {
        this.remote = remote
        if (connectState.value) {
            return
        }
        if (System.currentTimeMillis() - time < 3000) {
            return
        }
        time = System.currentTimeMillis()
        when (secureMode) {
            2 -> {
                val register = Store.register(context, deviceName)
                val deviceSecret = register.deviceSecret
                if (deviceSecret.isNotEmpty()) {
                    registerState.value = true
                    Log.i(TAG, "device authorized,start connect...")
                    connect(context.applicationContext, productKey, deviceName, deviceSecret, remote)
                } else {
                    Log.i(TAG, "device not authorized,start register...")
                    register(
                        context = context.applicationContext,
                        secureMode = secureMode,
                        authType = authType,
                        instanceId = instanceId,
                        productKey = productKey,
                        productSecret = productSecret,
                        deviceName = deviceName,
                        remote = remote,
                    )
                }
            }

            -2 -> {
                val regnwl = Store.regnwl(context, deviceName)
                val clientId = regnwl.clientId
                val deviceToken = regnwl.deviceToken
                if (deviceToken.isNotEmpty()) {
                    registerState.value = true
                    Log.i(TAG, "device authorized,start connwl...")
                    connwl(
                        context.applicationContext,
                        clientId,
                        productKey,
                        deviceName,
                        deviceToken,
                        remote,
                    )
                } else {
                    Log.i(TAG, "device not authorized,start register...")
                    register(
                        context = context.applicationContext,
                        secureMode = secureMode,
                        authType = authType,
                        instanceId = instanceId,
                        productKey = productKey,
                        productSecret = productSecret,
                        deviceName = deviceName,
                        remote = remote,
                    )
                }
            }
        }
    }


    fun connect(
        context: Context,
        productKey: String,
        deviceName: String,
        deviceSecret: String,
        remote: Boolean = this.remote,
    ) {
        this.remote = remote
        disconnect()
        options = OptionsBuilder.connect(context.applicationContext, productKey, deviceName, deviceSecret, remote)
        mqtt = Mqtt(context.applicationContext, options, this)
        Log.i(TAG, "clientId:${options.clientId}")
        mqtt.connect(context, this)
    }


    fun connwl(
        context: Context,
        clientId: String,
        productKey: String,
        deviceName: String,
        deviceToken: String,
        remote: Boolean = this.remote,
    ) {
        this.remote = remote
        disconnect()
        options = OptionsBuilder.connwl(
            context.applicationContext,
            clientId,
            productKey,
            deviceName,
            deviceToken,
            remote,
        )
        mqtt = Mqtt(context.applicationContext, options, this)
        mqtt.connect(context, this)
    }


    fun register(
        context: Context,
        secureMode: Int = 2,
        authType: String = "register",
        instanceId: String,
        productKey: String,
        productSecret: String,
        deviceName: String,
        remote: Boolean = this.remote,
    ) {
        this.remote = remote
        registerState.value = false
        disconnect()
        options = OptionsBuilder.register(
            context.applicationContext,
            secureMode,
            authType,
            instanceId,
            productKey,
            productSecret,
            deviceName,
            remote,
        )
        mqtt = Mqtt(context.applicationContext, options, this)
        mqtt.connect(context, this)
    }


    override fun connectionLost(exception: Throwable?) {
        Log.e(TAG, "connection lost")
        connectState.value = false
    }


    override fun messageArrived(topic: String?, message: MqttMessage?) {
        val payload = String(message!!.payload)
        Log.d(TAG, "received ${topic} ${payload}")
        when (topic) {
            topics().SUB_REGISTER() -> {
                interval = 3
                val register = gson.fromJson(payload, Register::class.java)
                Store.register(options.context, options.deviceName, register)
                registerState.value = true
                connect(
                    options.context,
                    register.productKey,
                    register.deviceName,
                    register.deviceSecret,
                    options.remote,
                )
            }
            topics().SUB_REGNWL() -> {
                interval = 3
                val regnwl = gson.fromJson(payload, Regnwl::class.java)
                Store.regnwl(options.context, options.deviceName, regnwl)
                registerState.value = true
                connwl(
                    options.context,
                    regnwl.clientId,
                    regnwl.productKey,
                    regnwl.deviceName,
                    regnwl.deviceToken,
                    options.remote,
                )
            }
            topics().SUB_OTA_UPGRADE() -> {
                upgradeState.value =
                    gson.fromJson(payload, object : TypeToken<Received<OTAPackage>>() {}.type)
            }
            topics().SUB_OTA_FIRMWARE_GET() -> {
                firmwareState.value =
                    gson.fromJson(payload, object : TypeToken<Received<OTAPackage>>() {}.type)
            }
            topics().SUB_LOG_GET_REPLY() -> {
                val response: Received<DeviceLogConfigData> = gson.fromJson(
                    payload,
                    object : TypeToken<Received<DeviceLogConfigData>>() {}.type,
                )
                logReportingEnabled.value = response.code == "200" && response.data?.content?.mode == 1
                Log.i(TAG, "device log reporting enabled=${logReportingEnabled.value}")
            }
            topics().SUB_CONFIG_GET_REPLY() -> {
                val response: Received<RemoteConfigFile> = gson.fromJson(
                    payload,
                    object : TypeToken<Received<RemoteConfigFile>>() {}.type,
                )
                if (response.code == "200") remoteConfigState.value = response.data
            }
            topics().SUB_CONFIG_PUSH() -> {
                val push = gson.fromJson(payload, RemoteConfigPushMessage::class.java)
                remoteConfigState.value = push.params
                operations.replyRemoteConfig(push.id, 200)
            }
            topics().SUB_JOB_NOTIFY() -> {
                val notification = gson.fromJson(payload, DeviceJobNotifyMessage::class.java)
                deviceJobState.value = notification.params.task
                operations.replyJobNotification(notification.id, 200)
            }
            topics().SUB_JOB_GET_REPLY() -> {
                val response: Received<DeviceJobQueryData> = gson.fromJson(
                    payload,
                    object : TypeToken<Received<DeviceJobQueryData>>() {}.type,
                )
                deviceJobState.value = response.data?.task
            }
            topics().SUB_JOB_UPDATE_REPLY() -> {
                Log.i(TAG, "device job status update reply: $payload")
            }
            topics().SUB_BOOTSTRAP_NOTIFY() -> {
                val message: android.mqtt.iot.data.Publish<DeviceDistributionNotification> = gson.fromJson(
                    payload,
                    object : TypeToken<android.mqtt.iot.data.Publish<DeviceDistributionNotification>>() {}.type,
                )
                deviceDistributionState.value = message.params
                operations.replyDeviceDistribution(message.id, 200)
            }
            topics().SUB_SHADOW_GET(),
            topics().SUB_TOPO_ADD_REPLY(), topics().SUB_TOPO_DELETE_REPLY(), topics().SUB_TOPO_GET_REPLY(),
            topics().SUB_DEVICE_LIST_FOUND_REPLY(), topics().SUB_SUB_LOGIN_REPLY(), topics().SUB_SUB_BATCH_LOGIN_REPLY(),
            topics().SUB_SUB_LOGOUT_REPLY(), topics().SUB_SUB_BATCH_LOGOUT_REPLY(), topics().SUB_DESIRED_GET_REPLY(),
            topics().SUB_DESIRED_DELETE_REPLY(), topics().SUB_TAG_UPDATE_REPLY(), topics().SUB_TAG_DELETE_REPLY(),
            topics().SUB_NETWORK_DIAGNOSTIC_REPLY(), topics().SUB_FILE_UPLOAD_INIT_REPLY(),
            topics().SUB_FILE_UPLOAD_SEND_REPLY(), topics().SUB_FILE_UPLOAD_CANCEL_REPLY() -> {
                managementMessageState.value = payload
            }
            topics().PUB_DISABLE() -> {
                disableState.value = true
            }
            topics().PUB_ENABLE() -> {
                disableState.value = true
            }
            topics().PUB_DELETE() -> {
                Store.clear(options.context)
                registerState.value = false
                deleteState.value = true
            }
            topics().SUB_PROPERTY_SET() -> {
                servicePropertyState.value = payload
            }
            topics().SUB_SECURE_TUNNEL_NOTIFY() -> {
                if (!options.remote) {
                    Log.i(TAG, "remote login disabled, ignore secure tunnel notify")
                    return
                }
                secureTunnel.handlePayload(payload, "notify", options)
            }
            topics().SUB_SECURE_TUNNEL_PROXY() -> {
                if (!options.remote) {
                    Log.i(TAG, "remote login disabled, ignore secure tunnel proxy reply")
                    return
                }
                secureTunnel.handlePayload(payload, "proxy reply", options)
            }
        }
    }


    override fun deliveryComplete(token: IMqttDeliveryToken?) {
        Log.d(TAG, "delivery complete")
    }


    override fun connectComplete(reconnect: Boolean, serverURI: String?) {
        Log.d(TAG, "connect complete,reconnect:$reconnect")
        if (reconnect) {
            connectState.value = true
            registerState.value = true
        }
    }


    override fun onSuccess(token: IMqttToken?) {
        Log.d(TAG, "${options.authType} success")
        disableState.value = false
        deleteState.value = false
        when (options.authType) {
            AuthType.CONNECT, AuthType.CONNWL -> {
                connectState.value = true
                subscribeDisable()
                subscribeEnable()
                subscribeDelete()
                subscribeUpgrade()
                subscribeFirmware()
                subscribeProperty()
                subscribeLogConfig()
                publishLogConfig()
                subscribeRemoteConfigPush()
                subscribeRemoteConfigReply()
                publishRemoteConfig()
                subscribeJobNotify()
                subscribeJobReplies()
                subscribeDeviceDistribution()
                subscribeShadow()
                subscribeManagementReplies()
                if (options.remote) {
                    subscribeSecureTunnelNotify()
                    subscribeSecureTunnelProxy()
                    publishSecureTunnelProxy()
                } else {
                    secureTunnel.close()
                    Log.i(TAG, "remote login disabled, skip secure tunnel subscribe")
                }
            }
            AuthType.REGISTER -> {
                subscribe(topics().SUB_REGISTER(), 0)
            }

            AuthType.REGNWL -> {
                subscribe(topics().SUB_REGNWL(), 0)
            }
        }
    }


    override fun onFailure(token: IMqttToken?, exception: Throwable?) {
        Log.e(TAG, "${options.authType} failure $exception ${options.deviceName}")
        connectState.value = false
        exception?.let {
            if (exception is MqttException && exception.reasonCode==MqttException.REASON_CODE_NOT_AUTHORIZED.toInt()){
                Store.clear(options.context, options.deviceName)
            }
        }
        if (options.authType == AuthType.REGISTER || options.authType == AuthType.REGNWL || exception is java.net.UnknownHostException) {
            scope.launch {
                delay(interval * 1000L)
                mqtt.connect(context = options.context, this@LinkSDK)
                interval += 1
                if (interval > 600) {
                    interval = 3
                }
            }
            return
        }
    }


    internal fun secureTunnelRefreshDelaySeconds(tokenExpireSeconds: Int): Long {
        return SecureTunnel.secureTunnelRefreshDelaySeconds(tokenExpireSeconds)
    }


    fun <T> serviceProperty(typeToken: TypeToken<ServiceProperty<T>>): ServiceProperty<T>? {
        val json = servicePropertyState.value
        json ?: return null
        return gson.fromJson(json, typeToken.type)
    }


    fun publish(topic: String, payload: String) {
        if (::mqtt.isInitialized && connectState.value) {
            mqtt.publish(topic, payload)
        }
    }


    fun subscribe(topic: String, qos: Int) {
        if (::mqtt.isInitialized && connectState.value) {
            mqtt.subscribe(topic, qos)
        }
    }


    fun subscribe(topic: Array<String>) {
        if (::mqtt.isInitialized && connectState.value) {
            mqtt.subscribe(topic)
        }
    }


    fun unsubscribe(topic: String) {
        if (::mqtt.isInitialized && connectState.value) {
            mqtt.unsubscribe(topic)
        }
    }


    fun unsubscribe(topic: Array<String>) {
        if (::mqtt.isInitialized && connectState.value) {
            mqtt.unsubscribe(topic)
        }
    }

    fun publishVersion(version: String, module: String = "default") {
        operations.publishVersion(version, module)
    }


    fun publishProgress(step: String, desc: String = "", module: String = "default") {
        operations.publishProgress(step, desc, module)
    }


    fun subscribeUpgrade() {
        operations.subscribeUpgrade()
    }


    fun unsubscribeUpgrade() {
        operations.unsubscribeUpgrade()
    }


    fun subscribeFirmware() {
        operations.subscribeFirmware()
    }


    fun unsubscribeFirmware() {
        operations.unsubscribeFirmware()
    }


    fun publishFirmware(module: String = "default") {
        operations.publishFirmware(module)
    }


    fun <T> publishProperty(params: T) {
        operations.publishProperty(params)
    }


    fun publishLogConfig() {
        operations.publishLogConfig()
    }


    fun publishLogs(entries: List<DeviceLogEntry>) {
        if (!connectState.value || !logReportingEnabled.value) {
            Log.d(TAG, "skip device logs connected=${connectState.value} enabled=${logReportingEnabled.value}")
            return
        }
        operations.publishLogs(entries)
    }


    fun publishLog(entry: DeviceLogEntry) {
        publishLogs(listOf(entry))
    }


    fun publishRemoteConfig() = operations.publishRemoteConfig()


    fun replyRemoteConfig(id: String, code: Int = 200) = operations.replyRemoteConfig(id, code)


    fun subscribeRemoteConfigPush() = operations.subscribeRemoteConfigPush()

    fun unsubscribeRemoteConfigPush() = operations.unsubscribeRemoteConfigPush()

    fun subscribeRemoteConfigReply() = operations.subscribeRemoteConfigReply()

    fun unsubscribeRemoteConfigReply() = operations.unsubscribeRemoteConfigReply()


    fun publishJob(taskId: String) = operations.publishJob(taskId)


    fun publishJobStatus(
        taskId: String,
        status: android.mqtt.iot.data.DeviceJobStatus,
        statusDetails: Map<String, Any?> = emptyMap(),
        progress: Int? = null,
    ) = operations.publishJobStatus(taskId, status, statusDetails, progress)


    fun subscribeJobNotify() = operations.subscribeJobNotify()

    fun unsubscribeJobNotify() = operations.unsubscribeJobNotify()

    fun subscribeJobReplies() = operations.subscribeJobReplies()

    fun unsubscribeJobReplies() = operations.unsubscribeJobReplies()


    fun subscribeDeviceDistribution() = operations.subscribeDeviceDistribution()

    fun unsubscribeDeviceDistribution() = operations.unsubscribeDeviceDistribution()

    fun replyDeviceDistribution(id: String, code: Int = 200) = operations.replyDeviceDistribution(id, code)


    fun publishTopologyAdd(devices: List<GatewayDevice>) = operations.publishTopologyAdd(devices)

    fun publishTopologyDelete(devices: List<SubDeviceSession>) = operations.publishTopologyDelete(devices)

    fun publishTopologyGet() = operations.publishTopologyGet()

    fun publishDiscoveredDevices(devices: List<SubDeviceSession>) = operations.publishDiscoveredDevices(devices)

    fun publishSubDeviceLogin(device: GatewayDevice) = operations.publishSubDeviceLogin(device)

    fun publishSubDeviceBatchLogin(devices: List<GatewayDevice>) = operations.publishSubDeviceBatchLogin(devices)

    fun publishSubDeviceLogout(device: SubDeviceSession) = operations.publishSubDeviceLogout(device)

    fun publishSubDeviceBatchLogout(devices: List<SubDeviceSession>) = operations.publishSubDeviceBatchLogout(devices)

    fun publishDesiredProperties(properties: Map<String, Any?>) = operations.publishDesiredProperties(properties)

    fun deleteDesiredProperties(properties: Collection<String>) = operations.deleteDesiredProperties(properties)

    fun updateTags(tags: List<DeviceTag>) = operations.updateTags(tags)

    fun deleteTags(tags: List<DeviceTag>) = operations.deleteTags(tags)

    fun publishNetworkDiagnostic(params: Any) = operations.publishNetworkDiagnostic(params)

    fun publishShadowUpdate(payload: Any) = operations.publishShadowUpdate(payload)

    fun subscribeShadow() = operations.subscribeShadow()

    fun unsubscribeShadow() = operations.unsubscribeShadow()

    fun publishFileUploadInit(params: Any) = operations.publishFileUploadInit(params)

    fun publishFileUploadChunk(params: Any) = operations.publishFileUploadChunk(params)

    fun publishFileUploadChunk(fileToken: String, streamId: Long, fileId: Int, offset: Long, bytes: ByteArray) =
        operations.publishFileUploadChunk(fileToken, streamId, fileId, offset, bytes)

    fun publishFileUploadCancel(params: Any) = operations.publishFileUploadCancel(params)

    fun subscribeManagementReplies() = operations.subscribeManagementReplies()

    fun unsubscribeManagementReplies() = operations.unsubscribeManagementReplies()


    fun subscribeLogConfig() {
        operations.subscribeLogConfig()
    }


    fun unsubscribeLogConfig() {
        operations.unsubscribeLogConfig()
    }


    fun subscribeProperty() {
        operations.subscribeProperty()
    }


    fun unsubscribeProperty() {
        operations.unsubscribeProperty()
    }


    fun subscribeSecureTunnelNotify() {
        operations.subscribeSecureTunnelNotify()
    }


    fun unsubscribeSecureTunnelNotify() {
        operations.unsubscribeSecureTunnelNotify()
    }


    fun subscribeSecureTunnelProxy() {
        operations.subscribeSecureTunnelProxy()
    }


    fun unnsubscribeSecureTunnelProxy() {
        operations.unsubscribeSecureTunnelProxy()
    }


    fun publishSecureTunnelProxy() {
        operations.publishSecureTunnelProxy()
    }


    fun subscribeDisable() {
        operations.subscribeDisable()
    }


    fun unsubscribeDisable() {
        operations.unsubscribeDisable()
    }


    fun subscribeDelete() {
        operations.subscribeDelete()
    }


    fun unsubscribeDelete() {
        operations.unsubscribeDelete()
    }


    fun subscribeEnable() {
        operations.subscribeEnable()
    }


    fun unsubscribeEnable() {
        operations.unsubscribeEnable()
    }


    fun disconnect() {
        if (::mqtt.isInitialized && connectState.value) {
            secureTunnel.close()
            if (::options.isInitialized) {
                unsubscribeUpgrade()
                unsubscribeFirmware()
                unsubscribeDisable()
                unsubscribeDelete()
                unsubscribeEnable()
                unsubscribeLogConfig()
                unsubscribeRemoteConfigPush()
                unsubscribeRemoteConfigReply()
                unsubscribeJobNotify()
                unsubscribeJobReplies()
                unsubscribeDeviceDistribution()
                unsubscribeShadow()
                unsubscribeManagementReplies()
                if (options.remote) {
                    unnsubscribeSecureTunnelProxy()
                    unsubscribeSecureTunnelNotify()
                }
            }
            mqtt.disconnect()
            upgradeState.value = null
            firmwareState.value = null
            connectState.value = false
            registerState.value = false
            disableState.value = false
            deleteState.value = false
            logReportingEnabled.value = false
            remoteConfigState.value = null
            deviceJobState.value = null
            deviceDistributionState.value = null
            managementMessageState.value = null
        }
    }

}
