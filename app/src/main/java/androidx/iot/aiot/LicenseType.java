package androidx.iot.aiot;

/**
 * 证书授权类型
 */
public enum LicenseType {

    /**
     * 一型一密免预注册认证方式,使用设备证书（ProductKey、DeviceName和DeviceSecret）连接
     */
    PRE_REGISTRATION,
    /**
     * 一型一密免预注册认证方式：使用ProductKey、DeviceName、ClientID、DeviceToken连接。
     */
    NO_PRE_REGISTRATION

}
