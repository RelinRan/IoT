package androidx.iot.data

/**
 * OTA升级包信息
 * @param size 升级包文件大小，单位：字节。OTA升级包中仅有一个升级包文件时，包含该参数。
 * @param version 设备升级包的版本信息
 * @param isDiff 仅当升级包类型为差分时，消息包含此参数,取值为1，表示仅包含新版本升级包与之前版本的差异部分，需要设备进行差分还原
 * @param url 升级包在对象存储（OSS）上的存储地址,OTA升级包中仅有一个升级包文件，且下载协议为HTTPS时，包含该参数
 * @param md5 当签名方法为MD5时，除了会给sign赋值外还会给md5赋值,OTA升级包中仅有一个升级包文件时，包含该参数
 * @param digestsign OTA升级包文件安全升级后的签名。仅当OTA升级包开启安全升级功能，才有此参数
 * @param sign OTA升级包文件的签名,OTA升级包中仅有一个升级包文件时，包含该参数
 * @param signMethod 签名方法。取值SHA256、MD5 对于Android差分升级包类型，仅支持MD5签名方法
 * @param module 升级包所属的模块名，模块名为default时，物联网平台不下发module参数
 */
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