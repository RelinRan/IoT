package androidx.iot.link

import android.content.Context
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.iot.data.OTAPackage
import androidx.iot.data.DeviceJob
import androidx.iot.data.DeviceJobNotifyMessage
import androidx.iot.data.DeviceJobQueryData
import androidx.iot.data.DeviceDistributionNotification
import androidx.iot.data.DeviceLogConfigData
import androidx.iot.data.DeviceLogEntry
import androidx.iot.data.DeviceTag
import androidx.iot.data.GatewayDevice
import androidx.iot.data.SubDeviceSession
import androidx.iot.data.RemoteConfigFile
import androidx.iot.data.RemoteConfigPushMessage
import androidx.iot.data.Received
import androidx.iot.data.Register
import androidx.iot.data.Regnwl
import androidx.iot.data.ServiceProperty
import androidx.iot.data.TunnelProxy
import androidx.iot.mqtt.Mqtt
import androidx.iot.mqtt.Options
import androidx.iot.mqtt.OptionsBuilder
import androidx.iot.utils.Store
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

/**
 * 阿里云 IoT 客户端统一入口。
 *
 * 负责设备动态注册、MQTT 连接生命周期、OTA 消息、设备属性、设备启用/禁用/删除命令，
 * 以及可选的安全远程访问。
 */
object LinkSDK : MqttCallbackExtended, IMqttActionListener {

    private const val TAG = "LinkSDK"

    /** MQTT 客户端。 */
    private lateinit var mqtt: Mqtt

    /** JSON 序列化器。 */
    private var gson: Gson = Gson()

    /** 连接参数。 */
    lateinit var options: Options

    private var scope = CoroutineScope(Dispatchers.IO)

    private var interval = 3

    /** MQTT 客户端是否已连接。 */
    var connectState: MutableState<Boolean> = mutableStateOf(value = false)

    /** 动态注册是否已完成。 */
    var registerState: MutableState<Boolean> = mutableStateOf(value = false)

    /** 设备是否已禁用。 */
    var disableState: MutableState<Boolean> = mutableStateOf(value = false)

    /** 设备是否已删除。 */
    var deleteState: MutableState<Boolean> = mutableStateOf(value = false)

    /** IoT 平台推送的 OTA 升级信息。 */
    var upgradeState: MutableState<Received<OTAPackage>?> = mutableStateOf(value = null)

    /** 设备请求的 OTA 固件信息。 */
    var firmwareState: MutableState<Received<OTAPackage>?> = mutableStateOf(value = null)

    /** 服务端下发的属性。 */
    var servicePropertyState: MutableState<String?> = mutableStateOf(value = null)

    /** IoT 平台最近一次下发的安全隧道参数。 */
    var secureTunnelState: MutableState<TunnelProxy?> = mutableStateOf(value = null)

    /** 云端设备日志上报开关，只有云端返回 mode=1 时才允许发送日志。 */
    var logReportingEnabled: MutableState<Boolean> = mutableStateOf(value = false)

    /** 最近收到的远程配置文件描述。应用下载、校验并应用后调用 replyRemoteConfig。 */
    var remoteConfigState: MutableState<RemoteConfigFile?> = mutableStateOf(value = null)

    /** 最近收到的设备任务通知。 */
    var deviceJobState: MutableState<DeviceJob?> = mutableStateOf(value = null)

    /** 最近收到的设备分发通知。 */
    var deviceDistributionState: MutableState<DeviceDistributionNotification?> =
        mutableStateOf(value = null)

    /** 网关、标签、影子、诊断和文件上传等管理协议的最近一条响应原文。 */
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

    /** 创建当前设备对应的 Topic 构造器。 */
    private fun topics(): Topics {
        return Topics(options.productKey, options.deviceName)
    }

    private var time: Long = 0

    /**
     * 初始化客户端，并自动选择动态注册或直接连接。
     * 未注册设备会先执行动态注册再连接；已保存凭证的设备会直接连接。
     *
     * @param context 应用上下文
     * @param secureMode 认证安全模式：`2` 表示预注册，`-2` 表示免预注册
     * @param authType 动态注册类型：`register` 通过 `/ext/register` 返回设备密钥；
     * `regnwl` 通过 `/ext/regnwl` 返回客户端 ID 和设备令牌
     * @param instanceId IoT 实例 ID
     * @param productKey 产品密钥
     * @param productSecret 产品密钥对应的产品 Secret
     * @param deviceName 设备名称
     * @param remote 连接后是否订阅安全隧道 Topic 并启动本地 SSH 桥接
     */
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
                    connect(
                        context.applicationContext,
                        productKey,
                        deviceName,
                        deviceSecret,
                        remote
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

    /**
     * 使用一机一密或一型一密预注册凭证连接。
     *
     * @param context 应用上下文
     * @param productKey 产品密钥
     * @param deviceName 设备名称
     * @param deviceSecret 设备密钥
     * @param remote 本次连接是否启用安全远程访问
     */
    fun connect(
        context: Context,
        productKey: String,
        deviceName: String,
        deviceSecret: String,
        remote: Boolean = this.remote,
    ) {
        this.remote = remote
        disconnect()
        options = OptionsBuilder.connect(
            context.applicationContext,
            productKey,
            deviceName,
            deviceSecret,
            remote
        )
        mqtt = Mqtt(context.applicationContext, options, this)
        Log.i(TAG, "clientId:${options.clientId}")
        mqtt.connect(context, this)
    }

    /**
     * 使用一型一密免预注册认证返回的凭证连接。
     *
     * @param context 应用上下文
     * @param clientId MQTT 客户端 ID
     * @param productKey 产品密钥
     * @param deviceName 设备名称
     * @param deviceToken 设备令牌
     * @param remote 本次连接是否启用安全远程访问
     */
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

    /**
     * 启动设备动态注册。
     *
     * @param context 应用上下文
     * @param secureMode 认证安全模式：`2` 表示预注册，`-2` 表示免预注册
     * @param authType 注册类型：`register` 表示预注册，`regnwl` 表示免预注册
     * @param instanceId IoT 实例 ID
     * @param productKey 产品密钥
     * @param productSecret 产品 Secret
     * @param deviceName 设备名称
     * @param remote 注册完成后是否启用安全远程访问
     */
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

    /** MQTT 连接丢失时同步更新连接状态。 */
    override fun connectionLost(exception: Throwable?) {
        Log.e(TAG, "connection lost")
        connectState.value = false
    }

    /** 接收并按 Topic 分发云端下行消息。 */
    override fun messageArrived(topic: String?, message: MqttMessage?) {
        val payload = String(message!!.payload)
        Log.d(TAG, "received ${topic} ${payload}")
        when (topic) {
            // 一型一密预注册认证。
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
            // 一型一密免预注册认证。
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
            // IoT 平台推送的 OTA 升级信息。
            topics().SUB_OTA_UPGRADE() -> {
                upgradeState.value =
                    gson.fromJson(payload, object : TypeToken<Received<OTAPackage>>() {}.type)
            }
            // 设备请求的 OTA 固件信息。
            topics().SUB_OTA_FIRMWARE_GET() -> {
                firmwareState.value =
                    gson.fromJson(payload, object : TypeToken<Received<OTAPackage>>() {}.type)
            }

            topics().SUB_LOG_GET_REPLY() -> {
                val response: Received<DeviceLogConfigData> = gson.fromJson(
                    payload,
                    object : TypeToken<Received<DeviceLogConfigData>>() {}.type,
                )
                logReportingEnabled.value =
                    response.code == "200" && response.data?.content?.mode == 1
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
                val message: androidx.iot.data.Publish<DeviceDistributionNotification> =
                    gson.fromJson(
                        payload,
                        object :
                            TypeToken<androidx.iot.data.Publish<DeviceDistributionNotification>>() {}.type,
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
            // 禁用设备。
            topics().PUB_DISABLE() -> {
                disableState.value = true
            }
            // 启用设备。
            topics().PUB_ENABLE() -> {
                disableState.value = true
            }
            // 删除设备并清除已保存的凭证。
            topics().PUB_DELETE() -> {
                Store.clear(options.context)
                registerState.value = false
                deleteState.value = true
            }
            // 应用平台下发的属性。
            topics().SUB_PROPERTY_SET() -> {
                servicePropertyState.value = payload
            }
            // 处理安全隧道通知。
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

    /** MQTT 消息投递完成回调。 */
    override fun deliveryComplete(token: IMqttDeliveryToken?) {
        Log.d(TAG, "delivery complete")
    }

    /** MQTT 自动重连完成回调。 */
    override fun connectComplete(reconnect: Boolean, serverURI: String?) {
        Log.d(TAG, "connect complete,reconnect:$reconnect")
        if (reconnect) {
            connectState.value = true
            registerState.value = true
        }
    }

    /** MQTT 连接或注册请求成功后订阅设备能力 Topic。 */
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

    /** MQTT 连接或注册失败时更新状态，并按策略发起重试。 */
    override fun onFailure(token: IMqttToken?, exception: Throwable?) {
        Log.e(TAG, "${options.authType} failure $exception ${options.deviceName}")
        connectState.value = false
        exception?.let {
            if (exception is MqttException && exception.reasonCode == MqttException.REASON_CODE_NOT_AUTHORIZED.toInt()) {
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

    /** 计算安全隧道令牌到期前的刷新等待秒数。 */
    internal fun secureTunnelRefreshDelaySeconds(tokenExpireSeconds: Int): Long {
        return SecureTunnel.secureTunnelRefreshDelaySeconds(tokenExpireSeconds)
    }

    /**
     * 解析最近一次从 IoT 平台收到的属性设置负载。
     *
     * @param typeToken 期望属性负载的泛型类型
     * @return 解析后的服务属性；尚未收到属性负载时返回 `null`
     */
    fun <T> serviceProperty(typeToken: TypeToken<ServiceProperty<T>>): ServiceProperty<T>? {
        val json = servicePropertyState.value
        json ?: return null
        return gson.fromJson(json, typeToken.type)
    }

    /**
     * 向 MQTT Broker 发布消息。
     *
     * @param topic 目标 MQTT Topic
     * @param payload 消息负载
     */
    fun publish(topic: String, payload: String) {
        if (::mqtt.isInitialized && connectState.value) {
            mqtt.publish(topic, payload)
        }
    }

    /**
     * 订阅一个 MQTT Topic。
     *
     * @param topic 要订阅的 MQTT Topic
     * @param qos 服务质量等级：`0` 至多一次，`1` 至少一次，`2` 恰好一次
     */
    fun subscribe(topic: String, qos: Int) {
        if (::mqtt.isInitialized && connectState.value) {
            mqtt.subscribe(topic, qos)
        }
    }

    /**
     * 订阅多个 MQTT Topic。
     *
     * @param topic 要订阅的 MQTT Topic 集合
     */
    fun subscribe(topic: Array<String>) {
        if (::mqtt.isInitialized && connectState.value) {
            mqtt.subscribe(topic)
        }
    }

    /**
     * 取消订阅一个 MQTT Topic。
     *
     * @param topic 要取消订阅的 MQTT Topic
     */
    fun unsubscribe(topic: String) {
        if (::mqtt.isInitialized && connectState.value) {
            mqtt.unsubscribe(topic)
        }
    }

    /**
     * 取消订阅多个 MQTT Topic。
     *
     * @param topic 要取消订阅的 MQTT Topic 集合
     */
    fun unsubscribe(topic: Array<String>) {
        if (::mqtt.isInitialized && connectState.value) {
            mqtt.unsubscribe(topic)
        }
    }

    /**
     * 上报 OTA 模块版本。
     *
     * @param version 模块版本
     * @param module 模块名称
     */
    fun publishVersion(version: String, module: String = "default") {
        operations.publishVersion(version, module)
    }

    /**
     * 上报 OTA 升级进度。
     *
     * @param step `1` 到 `100` 的进度值，或错误码：`-1` 升级失败、`-2` 下载失败、
     * `-3` 校验失败、`-4` 烧写失败
     * @param desc 当前步骤说明或错误详情，最多 128 个字符
     * @param module 升级包所属模块
     */
    fun publishProgress(step: String, desc: String = "", module: String = "default") {
        operations.publishProgress(step, desc, module)
    }

    /** 订阅 IoT 平台的 OTA 升级通知。 */
    fun subscribeUpgrade() {
        operations.subscribeUpgrade()
    }

    /** 取消订阅 OTA 升级通知。 */
    fun unsubscribeUpgrade() {
        operations.unsubscribeUpgrade()
    }

    /** 订阅 OTA 固件响应。 */
    fun subscribeFirmware() {
        operations.subscribeFirmware()
    }

    /** 取消订阅 OTA 固件响应。 */
    fun unsubscribeFirmware() {
        operations.unsubscribeFirmware()
    }

    /** 请求指定模块的 OTA 固件信息。 */
    fun publishFirmware(module: String = "default") {
        operations.publishFirmware(module)
    }

    /**
     * 上报设备属性。
     *
     * @param params 属性负载
     */
    fun <T> publishProperty(params: T) {
        operations.publishProperty(params)
    }

    /** 请求云端设备日志上报配置。 */
    fun publishLogConfig() {
        operations.publishLogConfig()
    }

    /** 上报设备日志；未连接或云端关闭日志上报时自动忽略。 */
    fun publishLogs(entries: List<DeviceLogEntry>) {
        if (!connectState.value || !logReportingEnabled.value) {
            Log.d(
                TAG,
                "skip device logs connected=${connectState.value} enabled=${logReportingEnabled.value}"
            )
            return
        }
        operations.publishLogs(entries)
    }

    /** 上报一条设备日志。 */
    fun publishLog(entry: DeviceLogEntry) {
        publishLogs(listOf(entry))
    }

    /** 请求产品维度的远程配置文件描述。 */
    /** 主动请求产品级远程配置文件描述。 */
    fun publishRemoteConfig() = operations.publishRemoteConfig()

    /** 回复远程配置推送；应用应在下载、校验和落盘成功后传入 200。 */
    /** 回复远程配置推送处理结果。 */
    fun replyRemoteConfig(id: String, code: Int = 200) = operations.replyRemoteConfig(id, code)

    /** 订阅远程配置推送。 */
    fun subscribeRemoteConfigPush() = operations.subscribeRemoteConfigPush()

    /** 取消订阅远程配置推送。 */
    fun unsubscribeRemoteConfigPush() = operations.unsubscribeRemoteConfigPush()

    /** 订阅远程配置查询响应。 */
    fun subscribeRemoteConfigReply() = operations.subscribeRemoteConfigReply()

    /** 取消订阅远程配置查询响应。 */
    fun unsubscribeRemoteConfigReply() = operations.unsubscribeRemoteConfigReply()

    /** 请求任务详情；taskId 支持具体 ID、$next 和 $list。 */
    /** 查询指定任务、下一个任务或任务列表。 */
    fun publishJob(taskId: String) = operations.publishJob(taskId)

    /** 回报任务作业状态和可选进度。 */
    fun publishJobStatus(
        taskId: String,
        status: androidx.iot.data.DeviceJobStatus,
        statusDetails: Map<String, Any?> = emptyMap(),
        progress: Int? = null,
    ) = operations.publishJobStatus(taskId, status, statusDetails, progress)

    /** 订阅云端任务通知。 */
    fun subscribeJobNotify() = operations.subscribeJobNotify()

    /** 取消订阅云端任务通知。 */
    fun unsubscribeJobNotify() = operations.unsubscribeJobNotify()

    /** 订阅任务查询和状态更新响应。 */
    fun subscribeJobReplies() = operations.subscribeJobReplies()

    /** 取消订阅任务响应。 */
    fun unsubscribeJobReplies() = operations.unsubscribeJobReplies()

    /** 订阅设备分发通知。 */
    fun subscribeDeviceDistribution() = operations.subscribeDeviceDistribution()

    /** 取消订阅设备分发通知。 */
    fun unsubscribeDeviceDistribution() = operations.unsubscribeDeviceDistribution()

    /** 回复设备分发通知处理结果。 */
    fun replyDeviceDistribution(id: String, code: Int = 200) =
        operations.replyDeviceDistribution(id, code)

    /** 添加网关与子设备拓扑关系。 */
    fun publishTopologyAdd(devices: List<GatewayDevice>) = operations.publishTopologyAdd(devices)

    /** 删除网关与子设备拓扑关系。 */
    fun publishTopologyDelete(devices: List<SubDeviceSession>) =
        operations.publishTopologyDelete(devices)

    /** 查询当前网关的子设备拓扑关系。 */
    fun publishTopologyGet() = operations.publishTopologyGet()

    /** 上报网关发现的待接入子设备。 */
    fun publishDiscoveredDevices(devices: List<SubDeviceSession>) =
        operations.publishDiscoveredDevices(devices)

    /** 请求单个子设备经网关上线。 */
    fun publishSubDeviceLogin(device: GatewayDevice) = operations.publishSubDeviceLogin(device)

    /** 请求多个子设备经网关批量上线。 */
    fun publishSubDeviceBatchLogin(devices: List<GatewayDevice>) =
        operations.publishSubDeviceBatchLogin(devices)

    /** 请求单个子设备下线。 */
    fun publishSubDeviceLogout(device: SubDeviceSession) = operations.publishSubDeviceLogout(device)

    /** 请求多个子设备批量下线。 */
    fun publishSubDeviceBatchLogout(devices: List<SubDeviceSession>) =
        operations.publishSubDeviceBatchLogout(devices)

    /** 查询指定属性的期望值。 */
    fun publishDesiredProperties(properties: Map<String, Any?>) =
        operations.publishDesiredProperties(properties)

    /** 删除指定属性的期望值。 */
    fun deleteDesiredProperties(properties: Collection<String>) =
        operations.deleteDesiredProperties(properties)

    /** 更新设备标签。 */
    fun updateTags(tags: List<DeviceTag>) = operations.updateTags(tags)

    /** 删除设备标签。 */
    fun deleteTags(tags: List<DeviceTag>) = operations.deleteTags(tags)

    /** 上报设备网络诊断信息。 */
    fun publishNetworkDiagnostic(params: Any) = operations.publishNetworkDiagnostic(params)

    /** 更新设备影子状态或请求影子内容。 */
    fun publishShadowUpdate(payload: Any) = operations.publishShadowUpdate(payload)

    /** 订阅设备影子下行消息。 */
    fun subscribeShadow() = operations.subscribeShadow()

    /** 取消订阅设备影子下行消息。 */
    fun unsubscribeShadow() = operations.unsubscribeShadow()

    /** 初始化 MQTT 文件上传会话。 */
    fun publishFileUploadInit(params: Any) = operations.publishFileUploadInit(params)

    /** 使用完整协议参数发送文件分片。 */
    fun publishFileUploadChunk(params: Any) = operations.publishFileUploadChunk(params)

    /** 按文件令牌、文件标识和偏移量自动构造并发送 Base64 编码分片。 */
    fun publishFileUploadChunk(
        fileToken: String,
        streamId: Long,
        fileId: Int,
        offset: Long,
        bytes: ByteArray
    ) =
        operations.publishFileUploadChunk(fileToken, streamId, fileId, offset, bytes)

    /** 取消进行中的 MQTT 文件上传。 */
    fun publishFileUploadCancel(params: Any) = operations.publishFileUploadCancel(params)

    /** 订阅网关、标签、诊断和文件上传等管理协议响应。 */
    fun subscribeManagementReplies() = operations.subscribeManagementReplies()

    /** 取消订阅管理协议响应。 */
    fun unsubscribeManagementReplies() = operations.unsubscribeManagementReplies()

    /** 订阅云端设备日志配置响应。 */
    fun subscribeLogConfig() = operations.subscribeLogConfig()

    /** 取消订阅云端设备日志配置响应。 */
    fun unsubscribeLogConfig() = operations.unsubscribeLogConfig()

    /** 订阅设备属性消息。 */
    fun subscribeProperty() = operations.subscribeProperty()

    /** 取消订阅设备属性消息。 **/
    fun unsubscribeProperty() = operations.unsubscribeProperty()

    /** 订阅设备安全通道通知**/
    fun subscribeSecureTunnelNotify() = operations.subscribeSecureTunnelNotify()

    /** 取消订阅设备安全通道通知**/
    fun unsubscribeSecureTunnelNotify() = operations.unsubscribeSecureTunnelNotify()

    /** 订阅设备安全通道代理**/
    fun subscribeSecureTunnelProxy() = operations.subscribeSecureTunnelProxy()

    /** 取消订阅设备安全通道代理**/
    fun unnsubscribeSecureTunnelProxy() = operations.unsubscribeSecureTunnelProxy()

    /** 请求安全隧道代理信息。 */
    fun publishSecureTunnelProxy() = operations.publishSecureTunnelProxy()

    /**
     * 订阅设备禁用命令。
     * {"method":"thing.disable","id":"777291239","params":{},"version":"1.0.0"}
     */
    fun subscribeDisable() = operations.subscribeDisable()

    /** 取消订阅设备禁用命令。 */
    fun unsubscribeDisable() = operations.unsubscribeDisable()

    /** 订阅设备删除命令。 */
    fun subscribeDelete() = operations.subscribeDelete()

    /** 取消订阅设备删除命令。 */
    fun unsubscribeDelete() = operations.unsubscribeDelete()

    /** 订阅设备启用命令。 */
    fun subscribeEnable() = operations.subscribeEnable()

    /** 取消订阅设备启用命令。 */
    fun unsubscribeEnable() = operations.unsubscribeEnable()

    /** 断开 MQTT 客户端连接。 */
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
