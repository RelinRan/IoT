package android.mqtt.iot.link

import android.mqtt.iot.data.DeviceLogEntry
import android.mqtt.iot.data.DeviceLogLevel
import com.google.gson.Gson
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceLogOperationsTest {
    private val gson = Gson()
    private val published = mutableListOf<Pair<String, String>>()
    private val operations = PSub(
        topics = { Topics("product", "device") },
        publish = { topic, payload -> published += topic to payload },
        subscribe = { _, _ -> },
        unsubscribe = {},
        gson = gson,
    )

    @Test
    fun `requests device log configuration using official Alink protocol`() {
        operations.publishLogConfig()

        val (topic, payload) = published.single()
        val body = JsonParser.parseString(payload).asJsonObject
        assertEquals("/sys/product/device/thing/config/log/get", topic)
        assertEquals("thing.config.log.get", body["method"].asString)
        assertEquals("device", body["params"].asJsonObject["configScope"].asString)
        assertEquals("content", body["params"].asJsonObject["getType"].asString)
        assertEquals(0, body["sys"].asJsonObject["ack"].asInt)
    }

    @Test
    fun `posts device logs with official fields and no acknowledgement`() {
        operations.publishLogs(
            listOf(
                DeviceLogEntry(
                    utcTime = "2026-07-30T10:20:30.123+0800",
                    logLevel = DeviceLogLevel.ERROR,
                    module = "wash-flow",
                    code = "4103",
                    traceContext = "session-1",
                    logContent = "point erase failed",
                ),
            ),
        )

        val (topic, payload) = published.single()
        val body = JsonParser.parseString(payload).asJsonObject
        val entry = body["params"].asJsonArray.single().asJsonObject
        assertEquals("/sys/product/device/thing/log/post", topic)
        assertEquals("thing.log.post", body["method"].asString)
        assertEquals(0, body["sys"].asJsonObject["ack"].asInt)
        assertEquals("ERROR", entry["logLevel"].asString)
        assertEquals("wash-flow", entry["module"].asString)
        assertEquals("4103", entry["code"].asString)
        assertEquals("session-1", entry["traceContext"].asString)
        assertEquals("point erase failed", entry["logContent"].asString)
    }

    @Test
    fun `splits log batches at Aliyun limit of forty`() {
        val entries = List(41) { index ->
            DeviceLogEntry(logContent = "log-$index")
        }

        operations.publishLogs(entries)

        assertEquals(2, published.size)
        val sizes = published.map { (_, payload) ->
            JsonParser.parseString(payload).asJsonObject["params"].asJsonArray.size()
        }
        assertEquals(listOf(40, 1), sizes)
        assertTrue(published.all { it.first.endsWith("/thing/log/post") })
        assertFalse(published.any { it.second.isBlank() })
    }
}
