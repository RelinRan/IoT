package android.mqtt.iot.link

import org.junit.Assert.assertEquals
import org.junit.Test

class LinkSDKTopicsTest {
    private val topics = Topics("product", "device")

    @Test
    fun topicPathsRemainUnchanged() {
        assertEquals("/ota/device/inform/product/device", topics.PUB_OTA_INFORM())
        assertEquals("/ota/device/progress/product/device", topics.PUB_OTA_PROGRESS())
        assertEquals("/ota/device/upgrade/product/device", topics.SUB_OTA_UPGRADE())
        assertEquals("/ext/register", topics.SUB_REGISTER())
        assertEquals("/ext/regnwl", topics.SUB_REGNWL())
        assertEquals("/sys/product/device/thing/ota/firmware/get", topics.PUB_OTA_FIRMWARE_GET())
        assertEquals("/sys/product/device/thing/ota/firmware/get_reply", topics.SUB_OTA_FIRMWARE_GET())
        assertEquals("/sys/product/device/thing/disable", topics.PUB_DISABLE())
        assertEquals("/sys/product/device/thing/disable_reply", topics.SUB_DISABLE_REPLY())
        assertEquals("/sys/product/device/thing/enable", topics.PUB_ENABLE())
        assertEquals("/sys/product/device/thing/enable_reply", topics.SUB_ENABLE_REPLY())
        assertEquals("/sys/product/device/thing/delete", topics.PUB_DELETE())
        assertEquals("/sys/product/device/thing/delete_reply", topics.SUB_DELETE_REPLY())
        assertEquals("/sys/product/device/thing/event/property/post", topics.PUB_PROPERTY_POST())
        assertEquals("/sys/product/device/thing/model/post_reply", topics.SUB_PROPERTY_POST_REPLY())
        assertEquals("/sys/product/device/thing/service/property/set", topics.SUB_PROPERTY_SET())
        assertEquals("/sys/product/device/secure_tunnel/notify", topics.SUB_SECURE_TUNNEL_NOTIFY())
        assertEquals("/sys/product/device/secure_tunnel/proxy/request", topics.PUB_SECURE_TUNNEL_PROXY())
        assertEquals("/sys/product/device/secure_tunnel/proxy/request_reply", topics.SUB_SECURE_TUNNEL_PROXY())
    }
}
