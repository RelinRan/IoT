package android.mqtt.iot.data

data class TunnelProxy(
    val schema:String,
    val path:String,
    val token_expire:Int,
    val tunnel_id:String,
    val payload_mode:String,
    val port:Int,
    val host:String,
    val operation:String,
    val uri:String,
    val token:String,
)
