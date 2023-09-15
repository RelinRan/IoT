package androidx.iot.mqtt;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * MQTT发布订阅消息监听
 */
public interface OnMessageListener {

    /**
     * 消息到达
     * @param topic 主题
     * @param message 消息内容
     */
    void onMessageReceived(String topic, MqttMessage message);

    /**
     * 消息递送完成
     * @param token
     */
    void onMessageDelivered(IMqttDeliveryToken token);

}
