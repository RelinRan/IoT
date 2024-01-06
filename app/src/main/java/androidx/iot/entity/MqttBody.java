package androidx.iot.entity;

import androidx.iot.mqtt.OnConnectListener;
import androidx.iot.mqtt.OnMessageListener;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * Mqtt消息体
 */
public class MqttBody {

    private IMqttToken token;
    private Throwable throwable;
    private OnConnectListener connectListener;
    private OnMessageListener messageListener;
    private String topic;
    private MqttMessage message;
    private IMqttDeliveryToken deliveryToken;

    public IMqttToken getToken() {
        return token;
    }

    public void setToken(IMqttToken token) {
        this.token = token;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public void setThrowable(Throwable throwable) {
        this.throwable = throwable;
    }

    public OnConnectListener getConnectListener() {
        return connectListener;
    }

    public void setConnectListener(OnConnectListener connectListener) {
        this.connectListener = connectListener;
    }

    public OnMessageListener getMessageListener() {
        return messageListener;
    }

    public void setMessageListener(OnMessageListener messageListener) {
        this.messageListener = messageListener;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public MqttMessage getMessage() {
        return message;
    }

    public void setMessage(MqttMessage message) {
        this.message = message;
    }

    public IMqttDeliveryToken getDeliveryToken() {
        return deliveryToken;
    }

    public void setDeliveryToken(IMqttDeliveryToken deliveryToken) {
        this.deliveryToken = deliveryToken;
    }
}
