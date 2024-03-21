package androidx.iot.entity;

import androidx.iot.aiot.OnDynamicListener;

import org.eclipse.paho.client.mqttv3.MqttException;

public class DynamicBody {

    private String topic;
    private String payload;
    private MqttException exception;
    private OnDynamicListener listener;

    public DynamicBody(String topic, String payload, OnDynamicListener listener) {
        this.topic = topic;
        this.payload = payload;
        this.listener = listener;
    }

    public DynamicBody(MqttException exception, OnDynamicListener listener) {
        this.exception = exception;
        this.listener = listener;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public MqttException getException() {
        return exception;
    }

    public void setException(MqttException exception) {
        this.exception = exception;
    }

    public OnDynamicListener getListener() {
        return listener;
    }

    public void setListener(OnDynamicListener listener) {
        this.listener = listener;
    }
}
