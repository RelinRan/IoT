package androidx.iot.aiot;

public class ServerURL {

    /**
     * Mqtt服务器连接地址
     *
     * @param register   是否注册
     * @param productKey 产品key
     * @param area       服务器区域（例如：cn-shanghai）
     * @param port       端口（例如：443）
     * @return
     */
    public static String value(boolean register, String productKey, String area, int port) {
        StringBuffer sb = new StringBuffer(register ? "ssl://" : "tcp://");
        sb.append(productKey);
        sb.append(".iot-as-mqtt.");
        sb.append(area);
        sb.append(".aliyuncs.com:");
        sb.append(port);
        return sb.toString();
    }

}
