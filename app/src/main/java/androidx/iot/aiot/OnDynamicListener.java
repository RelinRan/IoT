package androidx.iot.aiot;

public interface OnDynamicListener {

    /**
     * 动态注册接受到的消息
     *
     * @param topic 主题
     * @param payload  内容
     */
    void onDynamicRegisterReceived(String topic, String payload);

}
