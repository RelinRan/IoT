package androidx.iot.link

import androidx.iot.data.DeviceJobStatus
import androidx.iot.data.GatewayDevice
import com.google.gson.Gson
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceManagementOperationsTest {
    private val published = mutableListOf<Pair<String, String>>()
    private val subscriptions = mutableListOf<String>()
    private val operations = PSub(
        topics = { Topics("product", "device") },
        publish = { topic, payload -> published += topic to payload },
        subscribe = { topic, _ -> subscriptions += topic },
        unsubscribe = {},
        gson = Gson(),
    )

    @Test
    fun `requests product file configuration using official Alink payload`() {
        operations.publishRemoteConfig()

        val (topic, payload) = published.single()
        val body = JsonParser.parseString(payload).asJsonObject
        assertEquals("/sys/product/device/thing/config/get", topic)
        assertEquals("thing.config.get", body["method"].asString)
        assertEquals("product", body["params"].asJsonObject["configScope"].asString)
        assertEquals("file", body["params"].asJsonObject["getType"].asString)
        assertEquals(0, body["sys"].asJsonObject["ack"].asInt)
    }

    @Test
    fun `acknowledges a pushed configuration on its response topic`() {
        operations.replyRemoteConfig("config-push-1", 200)

        val (topic, payload) = published.single()
        val body = JsonParser.parseString(payload).asJsonObject
        assertEquals("/sys/product/device/thing/config/push_reply", topic)
        assertEquals("config-push-1", body["id"].asString)
        assertEquals(200, body["code"].asInt)
        assertEquals(0, body["data"].asJsonObject.size())
    }

    @Test
    fun `requests and updates a device job with official payloads`() {
        operations.publishJob("task-1")
        operations.publishJobStatus("task-1", DeviceJobStatus.IN_PROGRESS, mapOf("phase" to "download"), 50)

        val get = JsonParser.parseString(published[0].second).asJsonObject
        assertEquals("/sys/product/device/thing/job/get", published[0].first)
        assertEquals("task-1", get["params"].asJsonObject["taskId"].asString)

        val update = JsonParser.parseString(published[1].second).asJsonObject
        assertEquals("/sys/product/device/thing/job/update", published[1].first)
        assertEquals("task-1", update["params"].asJsonObject["taskId"].asString)
        assertEquals("IN_PROGRESS", update["params"].asJsonObject["status"].asString)
        assertEquals(50, update["params"].asJsonObject["progress"].asInt)
        assertEquals("download", update["params"].asJsonObject["statusDetails"].asJsonObject["phase"].asString)
    }

    @Test
    fun `subscribes to configuration job and distribution downstream messages`() {
        operations.subscribeRemoteConfigPush()
        operations.subscribeJobNotify()
        operations.subscribeJobReplies()
        operations.subscribeDeviceDistribution()

        assertEquals(
            listOf(
                "/sys/product/device/thing/config/push",
                "/sys/product/device/thing/job/notify",
                "/sys/product/device/thing/job/get_reply",
                "/sys/product/device/thing/job/update_reply",
                "/sys/product/device/thing/bootstrap/notify",
            ),
            subscriptions,
        )
    }

    @Test
    fun `acknowledges device distribution notification`() {
        operations.replyDeviceDistribution("bootstrap-1", 200)

        val (topic, payload) = published.single()
        val body = JsonParser.parseString(payload).asJsonObject
        assertEquals("/sys/product/device/thing/bootstrap/notify_reply", topic)
        assertEquals("bootstrap-1", body["id"].asString)
        assertEquals(200, body["code"].asInt)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects more than thirty topology changes`() {
        operations.publishTopologyAdd(List(31) { GatewayDevice("p", "d-$it") })
    }

    @Test
    fun `publishes file upload chunk with protocol block fields`() {
        operations.publishFileUploadChunk(
            fileToken = "token",
            streamId = 12L,
            fileId = 1,
            offset = 4L,
            bytes = byteArrayOf(1, 2, 3),
        )
        val body = JsonParser.parseString(published.single().second).asJsonObject
        val params = body["params"].asJsonObject
        assertEquals("token", params["fileToken"].asString)
        assertEquals(12L, params["fileInfo"].asJsonObject["streamId"].asLong)
        assertEquals(4L, params["fileBlock"].asJsonObject["offset"].asLong)
        assertEquals(3, params["fileBlock"].asJsonObject["size"].asInt)
    }
}
