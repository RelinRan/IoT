package androidx.iot.link

import androidx.iot.data.DeviceJobStatus
import androidx.iot.data.DeviceTag
import androidx.iot.data.GatewayDevice
import androidx.iot.data.SubDeviceSession
import androidx.iot.data.DeviceLogConfigParams
import androidx.iot.data.DeviceLogEntry
import androidx.iot.data.Module
import androidx.iot.data.OTAProgress
import androidx.iot.data.OTAVersion
import androidx.iot.data.Publish
import androidx.iot.data.Sys
import com.google.gson.Gson
import androidx.iot.utils.Base64

internal class PSub(
    private val topics: () -> Topics,
    private val publish: (topic: String, payload: String) -> Unit,
    private val subscribe: (topic: String, qos: Int) -> Unit,
    private val unsubscribe: (topic: String) -> Unit,
    private val gson: Gson,
) {
    fun publishSecureTunnelProxy() {
        val proxy = Publish(
            id = System.currentTimeMillis().toString(),
            params = emptyMap<String, Any>()
        )
        publish(topics().PUB_SECURE_TUNNEL_PROXY(), gson.toJson(proxy))
    }

    fun publishLogConfig() {
        val request = Publish(
            id = System.currentTimeMillis().toString(),
            version = "1.0",
            sys = Sys(ack = 0),
            params = DeviceLogConfigParams(),
            method = "thing.config.log.get",
        )
        publish(topics().PUB_LOG_GET(), gson.toJson(request))
    }

    fun publishLogs(entries: List<DeviceLogEntry>) {
        entries.chunked(40).forEach { batch ->
            if (batch.isEmpty()) return@forEach
            val request = Publish(
                id = System.currentTimeMillis().toString(),
                version = "1.0",
                sys = Sys(ack = 0),
                params = batch,
                method = "thing.log.post",
            )
            publish(topics().PUB_LOG_POST(), gson.toJson(request))
        }
    }

    fun subscribeLogConfig() {
        subscribe(topics().SUB_LOG_GET_REPLY(), 0)
    }

    fun unsubscribeLogConfig() {
        unsubscribe(topics().SUB_LOG_GET_REPLY())
    }

    fun publishRemoteConfig() {
        val request = Publish(
            id = System.currentTimeMillis().toString(),
            version = "1.0",
            sys = Sys(ack = 0),
            params = mapOf("configScope" to "product", "getType" to "file"),
            method = "thing.config.get",
        )
        publish(topics().PUB_CONFIG_GET(), gson.toJson(request))
    }

    fun replyRemoteConfig(id: String, code: Int = 200) {
        publish(topics().SUB_CONFIG_PUSH_REPLY(), gson.toJson(mapOf("id" to id, "code" to code, "data" to emptyMap<String, Any>())))
    }

    fun subscribeRemoteConfigPush() = subscribe(topics().SUB_CONFIG_PUSH(), 0)

    fun subscribeRemoteConfigReply() = subscribe(topics().SUB_CONFIG_GET_REPLY(), 0)

    fun unsubscribeRemoteConfigPush() = unsubscribe(topics().SUB_CONFIG_PUSH())

    fun unsubscribeRemoteConfigReply() = unsubscribe(topics().SUB_CONFIG_GET_REPLY())

    fun publishJob(taskId: String) {
        val request = Publish(
            id = System.currentTimeMillis().toString(),
            version = "1.0",
            params = mapOf("taskId" to taskId),
        )
        publish(topics().PUB_JOB_GET(), gson.toJson(request))
    }

    fun publishJobStatus(
        taskId: String,
        status: DeviceJobStatus,
        statusDetails: Map<String, Any?> = emptyMap(),
        progress: Int? = null,
    ) {
        require(progress == null || progress in 0..100) { "progress must be between 0 and 100" }
        val params = linkedMapOf<String, Any?>("taskId" to taskId, "status" to status.name, "statusDetails" to statusDetails)
        if (progress != null) params["progress"] = progress
        val request = Publish(
            id = System.currentTimeMillis().toString(),
            version = "1.0",
            params = params,
        )
        publish(topics().PUB_JOB_UPDATE(), gson.toJson(request))
    }

    fun subscribeJobNotify() = subscribe(topics().SUB_JOB_NOTIFY(), 0)

    fun subscribeJobReplies() {
        subscribe(topics().SUB_JOB_GET_REPLY(), 0)
        subscribe(topics().SUB_JOB_UPDATE_REPLY(), 0)
    }

    fun unsubscribeJobNotify() = unsubscribe(topics().SUB_JOB_NOTIFY())

    fun unsubscribeJobReplies() {
        unsubscribe(topics().SUB_JOB_GET_REPLY())
        unsubscribe(topics().SUB_JOB_UPDATE_REPLY())
    }

    fun replyJobNotification(id: String, code: Int = 200) {
        publish(topics().SUB_JOB_NOTIFY_REPLY(), gson.toJson(mapOf("id" to id, "code" to code, "data" to emptyMap<String, Any>())))
    }

    fun subscribeDeviceDistribution() = subscribe(topics().SUB_BOOTSTRAP_NOTIFY(), 0)

    fun unsubscribeDeviceDistribution() = unsubscribe(topics().SUB_BOOTSTRAP_NOTIFY())

    fun replyDeviceDistribution(id: String, code: Int = 200) {
        publish(topics().SUB_BOOTSTRAP_NOTIFY_REPLY(), gson.toJson(mapOf("id" to id, "code" to code, "data" to emptyMap<String, Any>())))
    }

    private fun alink(topic: String, method: String, params: Any, ack: Int = 0) {
        publish(topic, gson.toJson(Publish(System.currentTimeMillis().toString(), "1.0", Sys(ack), params, method)))
    }

    fun publishTopologyAdd(devices: List<GatewayDevice>) {
        require(devices.size <= 30) { "topology requests support at most 30 devices" }
        alink(topics().PUB_TOPO_ADD(), "thing.topo.add", devices)
    }

    fun publishTopologyDelete(devices: List<SubDeviceSession>) {
        require(devices.size <= 30) { "topology requests support at most 30 devices" }
        alink(topics().PUB_TOPO_DELETE(), "thing.topo.delete", devices)
    }

    fun publishTopologyGet() = alink(topics().PUB_TOPO_GET(), "thing.topo.get", emptyMap<String, Any>())

    fun publishDiscoveredDevices(devices: List<SubDeviceSession>) =
        alink(topics().PUB_DEVICE_LIST_FOUND(), "thing.list.found", devices)

    fun publishSubDeviceLogin(device: GatewayDevice) =
        publish(topics().PUB_SUB_LOGIN(), gson.toJson(mapOf("id" to System.currentTimeMillis().toString(), "params" to device)))

    fun publishSubDeviceBatchLogin(devices: List<GatewayDevice>) {
        require(devices.size <= 50) { "batch sub-device login supports at most 50 devices" }
        publish(topics().PUB_SUB_BATCH_LOGIN(), gson.toJson(mapOf("id" to System.currentTimeMillis().toString(), "params" to mapOf("deviceList" to devices))))
    }

    fun publishSubDeviceLogout(device: SubDeviceSession) =
        publish(topics().PUB_SUB_LOGOUT(), gson.toJson(mapOf("id" to System.currentTimeMillis().toString(), "params" to device)))

    fun publishSubDeviceBatchLogout(devices: List<SubDeviceSession>) {
        require(devices.size <= 50) { "batch sub-device logout supports at most 50 devices" }
        publish(topics().PUB_SUB_BATCH_LOGOUT(), gson.toJson(mapOf("id" to System.currentTimeMillis().toString(), "params" to mapOf("deviceList" to devices))))
    }

    fun publishDesiredProperties(properties: Map<String, Any?>) =
        alink(topics().PUB_DESIRED_GET(), "thing.property.desired.get", mapOf("attributes" to properties.keys))

    fun deleteDesiredProperties(properties: Collection<String>) =
        alink(topics().PUB_DESIRED_DELETE(), "thing.property.desired.delete", mapOf("attributes" to properties))

    fun updateTags(tags: List<DeviceTag>) = alink(topics().PUB_TAG_UPDATE(), "thing.deviceinfo.update", tags)
    fun deleteTags(tags: List<DeviceTag>) = alink(topics().PUB_TAG_DELETE(), "thing.deviceinfo.delete", tags)

    fun publishNetworkDiagnostic(params: Any) = alink(topics().PUB_NETWORK_DIAGNOSTIC(), "thing.diag.post", params)

    fun publishShadowUpdate(payload: Any) = publish(topics().PUB_SHADOW_UPDATE(), gson.toJson(payload))
    fun subscribeShadow() = subscribe(topics().SUB_SHADOW_GET(), 1)
    fun unsubscribeShadow() = unsubscribe(topics().SUB_SHADOW_GET())

    fun publishFileUploadInit(params: Any) = alink(topics().PUB_FILE_UPLOAD_INIT(), "thing.file.upload.mqtt.init", params)
    fun publishFileUploadChunk(params: Any) = alink(topics().PUB_FILE_UPLOAD_SEND(), "thing.file.upload.mqtt.send", params)
    fun publishFileUploadCancel(params: Any) = alink(topics().PUB_FILE_UPLOAD_CANCEL(), "thing.file.upload.mqtt.cancel", params)

    fun publishFileUploadChunk(
        fileToken: String,
        streamId: Long,
        fileId: Int,
        offset: Long,
        bytes: ByteArray,
    ) {
        require(offset >= 0) { "offset must not be negative" }
        publishFileUploadChunk(
            mapOf(
                "fileToken" to fileToken,
                "fileInfo" to mapOf("streamId" to streamId, "fileId" to fileId),
                "fileBlock" to mapOf(
                    "offset" to offset,
                    "size" to bytes.size,
                    "data" to Base64.encoder.encodeToString(bytes),
                ),
            ),
        )
    }

    fun subscribeManagementReplies() {
        listOf(
            topics().SUB_TOPO_ADD_REPLY(), topics().SUB_TOPO_DELETE_REPLY(), topics().SUB_TOPO_GET_REPLY(),
            topics().SUB_DEVICE_LIST_FOUND_REPLY(), topics().SUB_SUB_LOGIN_REPLY(), topics().SUB_SUB_BATCH_LOGIN_REPLY(),
            topics().SUB_SUB_LOGOUT_REPLY(), topics().SUB_SUB_BATCH_LOGOUT_REPLY(), topics().SUB_DESIRED_GET_REPLY(),
            topics().SUB_DESIRED_DELETE_REPLY(), topics().SUB_TAG_UPDATE_REPLY(), topics().SUB_TAG_DELETE_REPLY(),
            topics().SUB_NETWORK_DIAGNOSTIC_REPLY(), topics().SUB_FILE_UPLOAD_INIT_REPLY(),
            topics().SUB_FILE_UPLOAD_SEND_REPLY(), topics().SUB_FILE_UPLOAD_CANCEL_REPLY(),
        ).forEach { subscribe(it, 0) }
    }

    fun unsubscribeManagementReplies() {
        listOf(
            topics().SUB_TOPO_ADD_REPLY(), topics().SUB_TOPO_DELETE_REPLY(), topics().SUB_TOPO_GET_REPLY(),
            topics().SUB_DEVICE_LIST_FOUND_REPLY(), topics().SUB_SUB_LOGIN_REPLY(), topics().SUB_SUB_BATCH_LOGIN_REPLY(),
            topics().SUB_SUB_LOGOUT_REPLY(), topics().SUB_SUB_BATCH_LOGOUT_REPLY(), topics().SUB_DESIRED_GET_REPLY(),
            topics().SUB_DESIRED_DELETE_REPLY(), topics().SUB_TAG_UPDATE_REPLY(), topics().SUB_TAG_DELETE_REPLY(),
            topics().SUB_NETWORK_DIAGNOSTIC_REPLY(), topics().SUB_FILE_UPLOAD_INIT_REPLY(),
            topics().SUB_FILE_UPLOAD_SEND_REPLY(), topics().SUB_FILE_UPLOAD_CANCEL_REPLY(),
        ).forEach { unsubscribe(it) }
    }

    fun publishVersion(version: String, module: String = "default") {
        val ota = Publish(
            id = System.currentTimeMillis().toString(),
            params = OTAVersion(version, module)
        )
        publish(topics().PUB_OTA_INFORM(), gson.toJson(ota))
    }

    fun publishProgress(step: String, desc: String = "", module: String = "default") {
        val ota = Publish(
            id = System.currentTimeMillis().toString(),
            params = OTAProgress(step, desc, module)
        )
        publish(topics().PUB_OTA_PROGRESS(), gson.toJson(ota))
    }

    fun subscribeUpgrade() {
        subscribe(topics().SUB_OTA_UPGRADE(), 0)
    }

    fun unsubscribeUpgrade() {
        unsubscribe(topics().SUB_OTA_UPGRADE())
    }

    fun publishFirmware(module: String = "default") {
        val value = Publish(
            id = System.currentTimeMillis().toString(),
            version = "1.0",
            params = Module(module),
            method = "thing.ota.firmware.get"
        )
        publish(topics().PUB_OTA_FIRMWARE_GET(), gson.toJson(value))
    }

    fun <T> publishProperty(params: T) {
        val value = Publish(
            id = System.currentTimeMillis().toString(),
            version = "1.0",
            sys = Sys(ack = 1),
            params = params,
            method = "thing.event.property.post"
        )
        publish(topics().PUB_PROPERTY_POST(), gson.toJson(value))
    }

    fun subscribeProperty() {
        subscribe(topics().SUB_PROPERTY_POST_REPLY(), 0)
    }

    fun unsubscribeProperty() {
        unsubscribe(topics().SUB_PROPERTY_POST_REPLY())
    }

    fun subscribeSecureTunnelProxy() {
        subscribe(topics().SUB_SECURE_TUNNEL_PROXY(), 0)
    }

    fun unsubscribeSecureTunnelProxy() {
        unsubscribe(topics().SUB_SECURE_TUNNEL_PROXY())
    }

    fun subscribeSecureTunnelNotify() {
        subscribe(topics().SUB_SECURE_TUNNEL_NOTIFY(), 0)
    }

    fun unsubscribeSecureTunnelNotify() {
        unsubscribe(topics().SUB_SECURE_TUNNEL_NOTIFY())
    }

    fun subscribeFirmware() {
        subscribe(topics().SUB_OTA_FIRMWARE_GET(), 0)
    }

    fun unsubscribeFirmware() {
        unsubscribe(topics().SUB_OTA_FIRMWARE_GET())
    }

    fun subscribeDisable() {
        subscribe(topics().SUB_DISABLE_REPLY(), 0)
    }

    fun unsubscribeDisable() {
        unsubscribe(topics().SUB_DISABLE_REPLY())
    }

    fun subscribeDelete() {
        subscribe(topics().SUB_DELETE_REPLY(), 0)
    }

    fun unsubscribeDelete() {
        unsubscribe(topics().SUB_DELETE_REPLY())
    }

    fun subscribeEnable() {
        subscribe(topics().SUB_ENABLE_REPLY(), 0)
    }

    fun unsubscribeEnable() {
        unsubscribe(topics().SUB_ENABLE_REPLY())
    }

}
