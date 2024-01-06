package androidx.iot.entity;

import androidx.iot.aiot.OnDynamicListener;

public class DynamicBody {

    private String topic;
    private String payload;
    private OnDynamicListener listener;

    public DynamicBody(String topic, String payload, OnDynamicListener listener) {
        this.topic = topic;
        this.payload = payload;
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

    public OnDynamicListener getListener() {
        return listener;
    }

    public void setListener(OnDynamicListener listener) {
        this.listener = listener;
    }
}
