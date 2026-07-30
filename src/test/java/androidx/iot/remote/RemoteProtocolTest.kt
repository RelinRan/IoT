package androidx.iot.remote

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteProtocolTest {

    @Test
    fun encodeWritesBigEndianHeaderLengthAndPayload() {
        val payload = "hello".toByteArray()
        val frame = TunnelFrame(
            header = TunnelHeader(
                frameType = FrameType.DATA_TRANSPORT.value,
                sessionId = "session-1",
                frameId = 7L,
                serviceType = "shell",
            ),
            payload = payload,
        )

        val encoded = RemoteProtocol.encode(frame)
        val headerLength = ((encoded[0].toInt() and 0xFF) shl 8) or (encoded[1].toInt() and 0xFF)

        assertTrue(headerLength > 0)
        assertEquals(encoded.size, 2 + headerLength + payload.size)
        assertArrayEquals(payload, encoded.copyOfRange(2 + headerLength, encoded.size))
    }

    @Test
    fun buildRequestAddsAliyunTunnelHeaders() {
        val request = SecureTunnel.buildRequest(
            url = "wss://iot-secure-tunnel-cn-sh.aliyuncs.com/tunnel/test/source",
            accessToken = "token-123",
            subprotocol = SecureTunnel.SECURE_TUNNEL_SUBPROTOCOL,
        )

        assertEquals("token-123", request.header(SecureTunnel.TOKEN_HEADER))
        assertEquals(SecureTunnel.SECURE_TUNNEL_SUBPROTOCOL, request.header(SecureTunnel.SUBPROTOCOL_HEADER))
        assertEquals(SecureTunnel.SECURE_TUNNEL_SUBPROTOCOL, request.header(SecureTunnel.WEBSOCKET_PROTOCOL_HEADER))
    }

    @Test
    fun decodeRestoresHeaderAndPayload() {
        val original = TunnelFrame(
            header = TunnelHeader(
                frameType = FrameType.SESSION_CREATE.value,
                frameId = 12L,
                serviceType = "remote.sh",
            ),
            payload = RemoteProtocol.resultPayload(0, ""),
        )

        val decoded = RemoteProtocol.decode(RemoteProtocol.encode(original))

        assertEquals(original.header, decoded.header)
        assertArrayEquals(original.payload, decoded.payload)
    }

    @Test
    fun resultPayloadParsesCodeAndMessage() {
        val payload = RemoteProtocol.resultPayload(2, "reject")

        val result = RemoteProtocol.parseResult(payload)

        assertEquals(2, result?.code)
        assertEquals("reject", result?.msg)
    }

    @Test(expected = IllegalArgumentException::class)
    fun encodeRejectsPayloadLargerThanFourKb() {
        RemoteProtocol.encode(
            TunnelFrame(
                header = TunnelHeader(frameType = FrameType.DATA_TRANSPORT.value, sessionId = "s", frameId = 1L),
                payload = ByteArray(4097),
            )
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun decodeRejectsDeclaredHeaderLongerThanFrame() {
        RemoteProtocol.decode(byteArrayOf(0x00, 0x10, '{'.code.toByte()))
    }
}
