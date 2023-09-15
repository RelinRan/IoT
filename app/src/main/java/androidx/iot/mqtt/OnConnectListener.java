package androidx.iot.mqtt;

import org.eclipse.paho.client.mqttv3.IMqttToken;

/**
 * 连接监听
 */
public interface OnConnectListener {

    /**
     * 连接丢失
     * @param cause 异常
     */
    void onConnectionLost(Throwable cause);

    /**
     * 连接成功
     *
     * @param token 异步令牌
     */
    void onConnectionSuccessful(IMqttToken token);

    /**
     * 连接失败
     *
     * @param token     异步令牌
     * @param exception 异常信息
     */
    void onConnectionFailed(IMqttToken token, Throwable exception);

}
