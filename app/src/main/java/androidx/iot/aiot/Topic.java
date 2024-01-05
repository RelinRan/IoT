package androidx.iot.aiot;

/**
 * 阿里物联网平台主题
 */
public enum Topic {

    /**
     * 设备注册 - 一型一密预注册认证方式：Topic为/ext/register，authType取值为register，返回DeviceSecret。
     */
    SUB_EXT_REGISTER("/ext/register"),
    /**
     * 设备注册 - 一型一密免预注册认证方式：Topic为/ext/regnwl，authType取值为regnwl，返回ClientID、DeviceToken。
     */
    SUB_EXT_REGNWL("/ext/regnwl"),
    /**
     * 设备上报OTA模块版本
     */
    PUB_OTA_INFORM("/ota/device/inform/${productKey}/${deviceName}"),

    /**
     * 物联网平台推送OTA升级包信息
     */
    SUB_OTA_UPGRADE("/ota/device/upgrade/${productKey}/${deviceName}"),

    /**
     * 设备请求OTA升级包信息
     */
    PUB_OTA_FIRMWARE_GET("/sys/${productKey}/${deviceName}/thing/ota/firmware/get"),

    /**
     * 设备请求OTA升级包信息 - 响应
     */
    SUB_OTA_FIRMWARE_GET("/sys/${productKey}/${deviceName}/thing/ota/firmware/get_reply"),

    /**
     * 设备上报升级进度
     */
    PUB_OTA_PROGRESS("/ota/device/progress/${productKey}/${deviceName}"),

    /**
     * 设备上报属性
     */
    PUB_PROPERTY("/sys/${productKey}/${deviceName}/thing/event/property/post"),

    /**
     * 设备上报属性 - 响应
     */
    SUB_PROPERTY("/sys/${productKey}/${deviceName}/thing/event/property/post_reply"),

    /**
     * 设备设置属性 - 响应
     */
    SUB_PROPERTY_SET("/sys/${productKey}/${deviceName}/thing/service/property/set"),

    /**
     * 设备上报日志
     */
    PUB_LOG("/sys/${productKey}/${deviceName}/thing/log/post"),

    /**
     * 设备上报日志 - 响应
     */
    SUB_LOG("/sys/${productKey}/${deviceName}/thing/log/post_reply"),

    /**
     * 设备主动上报网络状态
     */
    PUB_NETWORK("/sys/${productKey}/${deviceName}/_thing/diag/post"),

    /**
     * 设备主动上报网络状态 - 响应
     */
    SUB_NETWORK("/sys/${productKey}/${deviceName}/_thing/diag/post_reply"),

    /**
     * 设备上报事件
     */
    PUB_EVENT("/sys/${productKey}/${deviceName}/thing/event/${tsl.functionBlockId}:${tsl.event.identifier}/post"),

    /**
     * 设备上报事件 - 响应
     */
    SUB_EVENT("/sys/${productKey}/${deviceName}/thing/event/${tsl.functionBlockId}:${tsl.event.identifier}/post_reply"),

    /**
     * 设备服务调用
     */
    PUB_SERVICE("/sys/${productKey}/${deviceName}/thing/service/${tsl.functionBlockId}:${tsl.service.identifier}"),

    /**
     * 设备服务调用 - 响应
     */
    SUB_SERVICE("/sys/${productKey}/${deviceName}/thing/service/${tsl.functionBlockId}:${tsl.service.identifier}_reply"),

    /**
     * 设备请求远程通道认证信息
     */
    PUB_TUNNEL_PROXY("/sys/${productKey}/${deviceName}/secure_tunnel/proxy/request"),

    /**
     * 设备请求远程通道认证信息 - 响应
     */
    SUB_TUNNEL_PROXY("/sys/${productKey}/${deviceName}/secure_tunnel/proxy/request_reply"),

    /**
     * 安全隧道创建成功后下发相关信息，通知设备与物联网平台建立安全的WebSocket通道
     */
    SUB_TUNNEL_NOTIFY("/sys/${productKey}/${deviceName}/secure_tunnel/notify");


    private String value;

    Topic(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public void value(String value) {
        this.value = value;
    }

    /**
     * 是否相等
     *
     * @param topic 话题
     * @return
     */
    public boolean equals(String topic) {
        License license = License.acquire();
        if (license == null) {
            return false;
        }
        return topic.equals(real(license.getProductKey(), license.getDeviceName(), null, null));
    }

    /**
     * 是否相等
     *
     * @param topic           主题
     * @param functionBlockId 模块id
     * @param identifier      属性标识符
     * @return
     */
    public boolean equals(String topic, String functionBlockId, String identifier) {
        License license = License.acquire();
        if (license == null) {
            return false;
        }
        return topic.equals(real(license.getProductKey(), license.getDeviceName(), functionBlockId, identifier));
    }

    /**
     * 得到真实主题
     *
     * @param productKey 产品key
     * @param deviceName 设备名称
     * @return
     */
    public String real(String productKey, String deviceName) {
        return real(productKey, deviceName, null, null);
    }

    /**
     * 得到真实主题
     *
     * @param productKey      产品key
     * @param deviceName      设备名称
     * @param functionBlockId 模块id
     * @param identifier      属性标识符
     * @return
     */
    public String real(String productKey, String deviceName, String functionBlockId, String identifier) {
        if (value.contains("${productKey}")) {
            value = value.replace("${productKey}", productKey);
        }
        if (value.contains("${deviceName}")) {
            value = value.replace("${deviceName}", deviceName);
        }
        if (value.contains("${tsl.functionBlockId}")) {
            value = value.replace("${tsl.functionBlockId}", functionBlockId);
        }
        if (value.contains("${tsl.service.identifier}")) {
            value = value.replace("${tsl.service.identifier}", identifier);
        }
        if (value.contains("${tsl.event.identifier}")) {
            value = value.replace("${tsl.event.identifier}", identifier);
        }
        return value;
    }

}
