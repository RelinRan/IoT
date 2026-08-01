package android.mqtt.iot.remote

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import com.google.gson.annotations.SerializedName
import okhttp3.Call
import okhttp3.ConnectionSpec
import okhttp3.EventListener
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong


interface SecureTunnelClient {
    fun acceptSession(sessionId: String, frameId: Long, serviceType: String, message: String = ""): Boolean
    fun rejectSession(sessionId: String, frameId: Long, serviceType: String, code: Int = 2, message: String): Boolean
    fun sendData(sessionId: String, serviceType: String, payload: ByteArray): Boolean
    fun releaseSession(sessionId: String, message: String = "", code: Int = ReleaseCode.ACCESS_SIDE_CLOSE): Boolean
    fun disconnect(code: Int = 1000, reason: String = "normal close")
}

class Remote(
    private val url: String,
    private val accessToken: String,
    private val listener: Listener,
    private val client: OkHttpClient = defaultClient(),
    private val subprotocol: String = SECURE_TUNNEL_SUBPROTOCOL,
) : SecureTunnelClient {

    interface Listener {
        fun onOpen(remote: Remote) = Unit
        fun onSessionCreated(frame: TunnelFrame, response: TunnelResult?) = Unit
        fun onData(frame: TunnelFrame) = Unit
        fun onSessionReleased(frame: TunnelFrame, reason: TunnelResult?) = Unit
        fun onResponse(frame: TunnelFrame, response: TunnelResult?) = Unit
        fun onClosing(code: Int, reason: String) = Unit
        fun onClosed(code: Int, reason: String) = Unit
        fun onFailure(t: Throwable, response: Response?) = Unit
    }

    @Volatile
    private var webSocket: WebSocket? = null
    private val frameId = AtomicLong(0L)

    fun connect() {
        webSocket = client.newWebSocket(buildRequest(url, accessToken, subprotocol), SocketListener())
    }

    fun isConnected(): Boolean = webSocket != null

    fun createSession(serviceType: String): Boolean {
        validateServiceType(serviceType)
        return sendFrame(
            TunnelFrame(
                header = TunnelHeader(
                    frameType = FrameType.SESSION_CREATE.value,
                    frameId = nextFrameId(),
                    serviceType = serviceType,
                )
            )
        )
    }

    override fun acceptSession(sessionId: String, frameId: Long, serviceType: String, message: String): Boolean {
        validateServiceType(serviceType)
        return sendResponse(sessionId, frameId, serviceType, code = 0, message = message)
    }

    override fun rejectSession(sessionId: String, frameId: Long, serviceType: String, code: Int, message: String): Boolean {
        validateServiceType(serviceType)
        return sendResponse(sessionId, frameId, serviceType, code, message)
    }

    override fun sendData(sessionId: String, serviceType: String, payload: ByteArray): Boolean {
        validateServiceType(serviceType)
        return sendFrame(
            TunnelFrame(
                header = TunnelHeader(
                    frameType = FrameType.DATA_TRANSPORT.value,
                    sessionId = sessionId,
                    frameId = nextFrameId(),
                    serviceType = serviceType,
                ),
                payload = payload,
            )
        )
    }

    fun sendText(sessionId: String, serviceType: String, text: String): Boolean {
        return sendData(sessionId, serviceType, text.toByteArray(StandardCharsets.UTF_8))
    }

    override fun releaseSession(
        sessionId: String,
        message: String,
        code: Int,
    ): Boolean {
        return sendFrame(
            TunnelFrame(
                header = TunnelHeader(
                    frameType = FrameType.SESSION_RELEASE.value,
                    sessionId = sessionId,
                    frameId = nextFrameId(),
                ),
                payload = RemoteProtocol.resultPayload(code, message),
            )
        )
    }

    override fun disconnect(code: Int, reason: String) {
        webSocket?.close(code, reason)
        webSocket = null
    }

    private fun sendResponse(
        sessionId: String,
        frameId: Long,
        serviceType: String,
        code: Int,
        message: String,
    ): Boolean {
        validateCode(code)
        return sendFrame(
            TunnelFrame(
                header = TunnelHeader(
                    frameType = FrameType.COMMON_RESPONSE.value,
                    sessionId = sessionId,
                    frameId = frameId,
                    serviceType = serviceType,
                ),
                payload = RemoteProtocol.resultPayload(code, message),
            )
        )
    }

    private fun sendFrame(frame: TunnelFrame): Boolean {
        val socket = webSocket ?: return false
        return socket.send(RemoteProtocol.encode(frame).toByteString())
    }

    private fun nextFrameId(): Long {
        val next = frameId.getAndIncrement()
        if (next == Long.MAX_VALUE) {
            frameId.set(0L)
        }
        return next
    }

    private inner class SocketListener : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            this@Remote.webSocket = webSocket
            listener.onOpen(this@Remote)
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            val frame = RemoteProtocol.decode(bytes.toByteArray())
            when (FrameType.fromValue(frame.header.frameType)) {
                FrameType.COMMON_RESPONSE -> listener.onResponse(frame, RemoteProtocol.parseResult(frame.payload))
                FrameType.SESSION_CREATE -> listener.onSessionCreated(frame, RemoteProtocol.parseResult(frame.payload))
                FrameType.SESSION_RELEASE -> listener.onSessionReleased(frame, RemoteProtocol.parseResult(frame.payload))
                FrameType.DATA_TRANSPORT -> listener.onData(frame)
                null -> listener.onData(frame)
            }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            listener.onClosing(code, reason)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            this@Remote.webSocket = null
            listener.onClosed(code, reason)
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            this@Remote.webSocket = null
            listener.onFailure(t, response)
        }
    }

    companion object {
        const val TOKEN_HEADER = "tunnel-access-token"
        const val SUBPROTOCOL_HEADER = "subprotocol"
        const val WEBSOCKET_PROTOCOL_HEADER = "Sec-WebSocket-Protocol"
        const val SECURE_TUNNEL_SUBPROTOCOL = "aliyun.iot.securetunnel-v1.1"

        private fun defaultClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectionSpecs(listOf(ConnectionSpec.MODERN_TLS, ConnectionSpec.COMPATIBLE_TLS))
                .eventListenerFactory { TunnelEventListener() }
                .pingInterval(30, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.SECONDS)
                .build()
        }

        internal fun buildRequest(url: String, accessToken: String, subprotocol: String): Request {
            require(accessToken.isNotBlank()) { "accessToken must not be blank." }
            require(subprotocol.isNotBlank()) { "subprotocol must not be blank." }
            return Request.Builder()
                .url(url)
                .header(TOKEN_HEADER, accessToken)
                .header(SUBPROTOCOL_HEADER, subprotocol)
                .header(WEBSOCKET_PROTOCOL_HEADER, subprotocol)
                .build()
        }

        private fun validateServiceType(serviceType: String) {
            require(serviceType.isNotBlank()) { "serviceType must not be blank." }
        }

        private fun validateCode(code: Int) {
            require(code in 0..255) { "code must be in 0..255." }
        }

        private class TunnelEventListener : EventListener() {
            override fun dnsEnd(call: Call, domainName: String, inetAddressList: List<InetAddress>) {
                Log.i(TAG, "secure tunnel dns:$domainName -> ${inetAddressList.joinToString { it.hostAddress ?: it.hostName }}")
            }

            override fun connectStart(call: Call, inetSocketAddress: InetSocketAddress, proxy: Proxy) {
                Log.i(TAG, "secure tunnel connect start:$inetSocketAddress proxy=$proxy")
            }

            override fun secureConnectStart(call: Call) {
                Log.i(TAG, "secure tunnel tls start")
            }

            override fun secureConnectEnd(call: Call, handshake: okhttp3.Handshake?) {
                Log.i(TAG, "secure tunnel tls end:${handshake?.tlsVersion} ${handshake?.cipherSuite}")
            }

            override fun connectFailed(
                call: Call,
                inetSocketAddress: InetSocketAddress,
                proxy: Proxy,
                protocol: okhttp3.Protocol?,
                ioe: IOException,
            ) {
                Log.i(TAG, "secure tunnel connect failed:$inetSocketAddress proxy=$proxy protocol=$protocol error=$ioe", ioe)
            }
        }

        private const val TAG = "Remote"
    }
}

typealias SecureTunnel = Remote

data class TunnelFrame(
    val header: TunnelHeader,
    val payload: ByteArray = ByteArray(0),
) {
    override fun equals(other: Any?): Boolean {
        return other is TunnelFrame &&
            header == other.header &&
            payload.contentEquals(other.payload)
    }

    override fun hashCode(): Int {
        return 31 * header.hashCode() + payload.contentHashCode()
    }
}

data class TunnelHeader(
    @SerializedName("frame_type")
    val frameType: Int,
    @SerializedName("session_id")
    val sessionId: String? = null,
    @SerializedName("frame_id")
    val frameId: Long? = null,
    @SerializedName("service_type")
    val serviceType: String? = null,
)

data class TunnelResult(
    val code: Int,
    val msg: String = "",
)

enum class FrameType(val value: Int) {
    COMMON_RESPONSE(1),
    SESSION_CREATE(2),
    SESSION_RELEASE(3),
    DATA_TRANSPORT(4);

    companion object {
        fun fromValue(value: Int): FrameType? = values().firstOrNull { it.value == value }
    }
}

object ReleaseCode {
    const val ACCESS_SIDE_CLOSE = 0
    const val DEVICE_SIDE_CLOSE = 1
    const val ACCESS_SIDE_DISCONNECTED = 2
    const val DEVICE_SIDE_DISCONNECTED = 3
    const val PLATFORM_UPDATE = 4
}

internal object RemoteProtocol {
    private const val HEADER_LENGTH_BYTES = 2
    private const val MAX_HEADER_LENGTH = 2048
    private const val MAX_PAYLOAD_LENGTH = 4 * 1024
    private val gson = Gson()

    fun encode(frame: TunnelFrame): ByteArray {
        val headerBytes = gson.toJson(frame.header).toByteArray(StandardCharsets.UTF_8)
        require(headerBytes.size <= MAX_HEADER_LENGTH) { "Tunnel header cannot exceed $MAX_HEADER_LENGTH bytes." }
        require(frame.payload.size <= MAX_PAYLOAD_LENGTH) { "Tunnel payload cannot exceed $MAX_PAYLOAD_LENGTH bytes." }

        return ByteArray(HEADER_LENGTH_BYTES + headerBytes.size + frame.payload.size).also { target ->
            target[0] = ((headerBytes.size ushr 8) and 0xFF).toByte()
            target[1] = (headerBytes.size and 0xFF).toByte()
            headerBytes.copyInto(target, destinationOffset = HEADER_LENGTH_BYTES)
            frame.payload.copyInto(target, destinationOffset = HEADER_LENGTH_BYTES + headerBytes.size)
        }
    }

    fun decode(bytes: ByteArray): TunnelFrame {
        require(bytes.size >= HEADER_LENGTH_BYTES) { "Tunnel frame must contain a 2-byte header length." }

        val headerLength = ((bytes[0].toInt() and 0xFF) shl 8) or (bytes[1].toInt() and 0xFF)
        require(headerLength in 1..MAX_HEADER_LENGTH) {
            "Tunnel header length must be between 1 and $MAX_HEADER_LENGTH bytes."
        }
        require(bytes.size >= HEADER_LENGTH_BYTES + headerLength) {
            "Tunnel frame is shorter than the declared header length."
        }

        val headerJson = String(bytes, HEADER_LENGTH_BYTES, headerLength, StandardCharsets.UTF_8)
        val header = try {
            gson.fromJson(headerJson, TunnelHeader::class.java)
        } catch (e: JsonSyntaxException) {
            throw IllegalArgumentException("Tunnel header is not valid JSON.", e)
        }
        require(header != null) { "Tunnel header is empty." }

        val payloadOffset = HEADER_LENGTH_BYTES + headerLength
        val payload = bytes.copyOfRange(payloadOffset, bytes.size)
        require(payload.size <= MAX_PAYLOAD_LENGTH) {
            "Tunnel payload cannot exceed $MAX_PAYLOAD_LENGTH bytes."
        }
        return TunnelFrame(header, payload)
    }

    fun resultPayload(code: Int, message: String): ByteArray {
        require(code in 0..255) { "code must be in 0..255." }
        val payload = JsonObject().apply {
            addProperty("code", code)
            addProperty("msg", message)
        }
        return gson.toJson(payload).toByteArray(StandardCharsets.UTF_8)
    }

    fun parseResult(payload: ByteArray): TunnelResult? {
        if (payload.isEmpty()) return null
        val json = String(payload, StandardCharsets.UTF_8)
        return try {
            gson.fromJson(json, TunnelResult::class.java)
        } catch (e: JsonSyntaxException) {
            null
        }
    }
}
