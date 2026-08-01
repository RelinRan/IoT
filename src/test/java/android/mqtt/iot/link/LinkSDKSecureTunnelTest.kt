package android.mqtt.iot.link

import org.junit.Assert.assertEquals
import org.junit.Test

class LinkSDKSecureTunnelTest {

    @Test
    fun secureTunnelRefreshDelayRefreshesAtMostEveryFiveDays() {
        assertEquals(5L * 24 * 60 * 60, LinkSDK.secureTunnelRefreshDelaySeconds(600522))
    }

    @Test
    fun secureTunnelRefreshDelayRefreshesBeforeTokenExpires() {
        assertEquals(300L, LinkSDK.secureTunnelRefreshDelaySeconds(600))
    }

    @Test
    fun secureTunnelRefreshDelayRefreshesImmediatelyWhenTokenIsNearlyExpired() {
        assertEquals(1L, LinkSDK.secureTunnelRefreshDelaySeconds(100))
        assertEquals(1L, LinkSDK.secureTunnelRefreshDelaySeconds(0))
    }
}
