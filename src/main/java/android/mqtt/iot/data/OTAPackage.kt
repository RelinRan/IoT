package android.mqtt.iot.data


data class OTAPackage (
    val size:Long,
    val version:String?,
    val isDiff:Int,
    val url:String,
    val md5:String,
    val digestsign:String,
    val sign:String,
    val signMethod:String,
    val module:String?,
    val extData:ExtData,
)
