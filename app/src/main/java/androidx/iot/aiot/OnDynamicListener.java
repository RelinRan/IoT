package androidx.iot.aiot;

import org.eclipse.paho.client.mqttv3.MqttException;

public interface OnDynamicListener {

    /**
     * 动态注册接受到的消息
     *
     * @param topic 主题
     * @param payload  内容
     */
    void onDynamicRegisterReceived(String topic, String payload);

    /**
     * 动态注册失败
     *
     * @param e
     */
    void onDynamicRegisterFailure(MqttException e);

}
