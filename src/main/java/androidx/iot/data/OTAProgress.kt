package androidx.iot.data

/**
 * OTA模块版本信息
 * @param step OTA升级进度。取值范围：
 * 1~100的整数：升级进度百分比。
 * -1：升级失败。
 * -2：下载失败。
 * -3：校验失败。
 * -4：烧写失败。
 * @param desc 当前步骤的描述信息，长度不超过128个字符。如果发生异常，此字段可承载错误信息
 * @param module 升级包所属的模块名。模块的更多信息,上报默认（default）模块的OTA升级进度时，可以不上报module参数
 */
data class OTAProgress(val step:String, val desc:String, val module:String)
