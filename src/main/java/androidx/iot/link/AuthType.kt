package androidx.iot.link

/**
 * 认证方式
 */
enum class AuthType {
    /**
     * 一机一密或一型一密预注册认证：使用 ProductKey、DeviceName 和 DeviceSecret 连接。
     */
    CONNECT,

    /**
     * 一型一密免预注册认证：使用 ProductKey、DeviceName、ClientID 和 DeviceToken 连接。
     */
    CONNWL,

    /**
     * 一型一密预注册认证接口。
     * /ext/register
     * {"deviceSecret":"xxx","productKey":"xxx","deviceName":"xxx"}
     */
    REGISTER,
    /**
     * 一型一密免预注册认证接口。
     * /ext/regnwl
     * {"clientId":"xxx","productKey":"xxx","deviceName":"xxx","deviceToken":"xxx"}
     */
    REGNWL

}
