package androidx.iot.mqtt;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;

/**
 * MQTT操作
 */
public interface Imqtt {

    /**
     * 连接
     */
    Imqtt connect();

    /**
     * 断开连接
     */
    void disconnect();

    /**
     * 是否已连接
     * @return
     */
    boolean isConnected();

    /**
     * 发布消息。封装publish方法，用于向Topic /${prodcutKey}/${deviceName}/user/update发布指定payload的消息
     *
     * @param topic    主题
     * @param payload  内容
     * @param listener 动作监听
     */
    Imqtt publish(String topic, String payload, IMqttActionListener listener);

    /**
     * 发布消息
     *
     * @param topic   主题
     * @param payload 内容
     * @return
     */
    Imqtt publish(String topic, String payload);

    /**
     * 订阅服务端主题
     *
     * @param topic    主题
     * @param listener 动作监听
     */
    Imqtt subscribe(String topic, IMqttActionListener listener);

    /**
     * 订阅服务端主题
     *
     * @param topic
     */
    Imqtt subscribe(String topic);

    /**
     * 添加信息监听
     *
     * @param listener
     */
    long addMessageListener(OnMessageListener listener);

    /**
     * 添加连接监听
     *
     * @param listener
     */
    long addConnectListener(OnConnectListener listener);

    /**
     * 通过id删除监听
     *
     * @param ids 信息监听Id
     * @return
     */
    Imqtt remove(long... ids);

    /**
     * 清除所有监听
     *
     * @return
     */
    Imqtt clear();

    /**
     * 重置
     * @return
     */
    Imqtt reset();
}
