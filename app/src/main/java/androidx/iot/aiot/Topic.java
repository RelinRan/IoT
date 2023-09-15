package androidx.iot.aiot;

/**
 * 阿里物联网平台主题
 */
public enum Topic {

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
     * @param topic      话题
     * @param productKey 产品key
     * @param deviceName 设备名称
     * @return
     */
    public boolean equals(String topic, String productKey, String deviceName) {
        return equals(topic, productKey, deviceName, null, null);
    }

    /**
     * 是否相等
     *
     * @param topic           主题
     * @param productKey      产品key
     * @param deviceName      设备名称
     * @param functionBlockId 模块id
     * @param identifier      属性标识符
     * @return
     */
    public boolean equals(String topic, String productKey, String deviceName, String functionBlockId, String identifier) {
        return real(productKey, deviceName, functionBlockId, identifier).equals(topic);
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
        String separator = "/";
        if (!value().contains(separator)) {
            return value();
        }
        String[] items = value().trim().split(separator);
        StringBuilder builder = new StringBuilder();
        StringBuffer group = new StringBuffer();
        if (functionBlockId != null && identifier != null) {
            group.append(functionBlockId).append(":").append(identifier);
        }
        int length = items.length;
        if (length > 0) {
            builder.append(separator);
        }
        for (int i = 0; i < length; i++) {
            String segment = items[i];
            if (segment.startsWith("$")) {
                if (productKey != null) {
                    segment = segment.replace("${productKey}", productKey);
                }
                if (deviceName != null) {
                    segment = segment.replace("${deviceName}", deviceName);
                }
                if (segment.contains("${tsl.")) {
                    if (functionBlockId != null && identifier != null) {
                        segment = segment.replace("${tsl.functionBlockId}:${tsl.service.identifier}", group);
                        segment = segment.replace("${tsl.functionBlockId}:${tsl.event.identifier}", group);
                    } else {
                        if (functionBlockId != null) {
                            segment = segment.replace("${tsl.functionBlockId}", functionBlockId);
                        }
                        if (identifier != null) {
                            segment = segment.replace("${tsl.service.identifier}", identifier);
                            segment = segment.replace("${tsl.event.identifier}", identifier);
                        }
                    }
                }
            }
            if (segment != null && segment.length() > 0) {
                builder.append(segment);
                if (i != length - 1) {
                    builder.append(separator);
                }
            }
        }
        return builder.toString();
    }

}
