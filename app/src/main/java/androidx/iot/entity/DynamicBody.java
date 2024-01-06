package androidx.iot.entity;

import androidx.iot.aiot.OnDynamicListener;

/**
 * 动态注册消息体
 */
public class DynamicBody {

    /**
     * 主题
     */
    private String topic;
    /**
     * 返回内容
     */
    private String payload;
    /**
     * 动态注册监听
     */
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
