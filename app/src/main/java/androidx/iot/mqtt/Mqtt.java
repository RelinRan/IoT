package androidx.iot.mqtt;

import android.content.Context;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.concurrent.ConcurrentHashMap;

/**
 * MQTT
 */
public class Mqtt implements Imqtt, MqttCallback, IMqttActionListener {

    public final static String TAG = Mqtt.class.getSimpleName();
    /**
     * MQTT客户端
     */
    private MqttAndroidClient client;
    /**
     * MQTT参数
     */
    private MqttConnectOptions mqttConnectOptions;
    /**
     * MQTT初始化参数
     */
    private MqttOption mqttOption;
    /**
     * 是否调试
     */
    private boolean debug;
    /**
     * MQTT
     */
    private static Mqtt mqtt;


    public Mqtt() {
        android.util.Log.i(TAG, "instantiation mqtt");
    }

    /**
     * 是否调试
     *
     * @return
     */
    public boolean isDebug() {
        return debug;
    }

    /**
     * 设置调试模式
     *
     * @param debug
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    /**
     * 初始化物联网操作对象
     *
     * @param context 上下文
     * @param options 参数
     * @return
     */
    public static Mqtt initialize(Context context, MqttOption options) {
        if (mqtt == null) {
            synchronized (Mqtt.class) {
                if (mqtt == null) {
                    mqtt = new Mqtt();
                }
            }
        }
        mqtt.initializeClient(context, options);
        return mqtt;
    }

    /**
     * 获取客户端对象（必须初始化才可以调用）
     *
     * @return
     */
    public static Mqtt client() {
        return mqtt;
    }

    /**
     * 初始化客户端
     *
     * @param context 上下文
     * @param options 参数
     */
    protected void initializeClient(Context context, MqttOption options) {
        this.mqttOption = options;
        client = new MqttAndroidClient(context.getApplicationContext(), options.getHost(), options.getClientId());
        client.setCallback(this);
        mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setUserName(options.getUserName());
        mqttConnectOptions.setPassword(options.getPassword().toCharArray());
        print("service url", options.getHost());
    }


    @Override
    public Imqtt connect() {
        try {
            if (client != null) {
                client.connect(mqttConnectOptions, null, this);
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
        return this;
    }

    @Override
    public void disconnect() {
        try {
            if (client != null) {
                client.disconnect();
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean isConnected() {
        if (client == null) {
            return false;
        }
        return client.isConnected();
    }

    //*******************状态**********************
    @Override
    public void connectionLost(Throwable cause) {
        android.util.Log.i(TAG, "connection lost");
        try {
            if (client != null) {
                client.connect();
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
        if (connectHashMap != null) {
            for (Long key : connectHashMap.keySet()) {
                connectHashMap.get(key).onConnectionLost(cause);
            }
        }
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        String payload = new String(message.getPayload());
        if (debug) {
            print("received", topic, payload);
        }
        if (messageHashMap != null) {
            for (Long key : messageHashMap.keySet()) {
                messageHashMap.get(key).onMessageReceived(topic, message);
            }
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        if (messageHashMap != null) {
            for (Long key : messageHashMap.keySet()) {
                messageHashMap.get(key).onMessageDelivered(token);
            }
        }
    }

    //*********************[连接成功]**********************

    @Override
    public void onSuccess(IMqttToken token) {
        android.util.Log.i(TAG, "connection successful");
        if (connectHashMap != null) {
            for (Long key : connectHashMap.keySet()) {
                connectHashMap.get(key).onConnectionSuccessful(token);
            }
        }
    }

    @Override
    public void onFailure(IMqttToken token, Throwable exception) {
        android.util.Log.i(TAG, "connection failed ");
        if (connectHashMap != null) {
            for (Long key : connectHashMap.keySet()) {
                connectHashMap.get(key).onConnectionFailed(token, exception);
            }
        }
    }

    //***********************[END]*****************************

    @Override
    public Imqtt publish(String topic, String payload, IMqttActionListener listener) {
        try {
            if (client != null) {
                if (client.isConnected() == false) {
                    client.connect();
                }
                MqttMessage message = new MqttMessage();
                int id = (int) System.currentTimeMillis() + 200;
                message.setPayload(payload.getBytes());
                message.setId(id);
                message.setQos(0);
                client.publish(topic, message, null, listener);
                if (debug) {
                    print("publish id:" + id, topic, payload);
                }
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
        return this;
    }

    @Override
    public Imqtt publish(String topic, String payload) {
        return publish(topic, payload, null);
    }

    @Override
    public Imqtt subscribe(String topic, IMqttActionListener listener) {
        try {
            if (debug) {
                print("subscribe", topic);
            }
            client.subscribe(topic, 0, null, listener);
        } catch (MqttException e) {
            e.printStackTrace();
        }
        return this;
    }

    @Override
    public Imqtt subscribe(String topic) {
        return subscribe(topic, null);
    }

    /**
     * 信息监听Map
     */
    private ConcurrentHashMap<Long, OnMessageListener> messageHashMap;

    @Override
    public long addMessageListener(OnMessageListener listener) {
        if (messageHashMap == null) {
            messageHashMap = new ConcurrentHashMap<>();
        }
        long mid = System.currentTimeMillis() + messageHashMap.size() + 1;
        messageHashMap.put(mid, listener);
        return mid;
    }

    /**
     * 连接监听Map
     */
    private ConcurrentHashMap<Long, OnConnectListener> connectHashMap;

    @Override
    public long addConnectListener(OnConnectListener listener) {
        long cid = System.currentTimeMillis();
        if (connectHashMap == null) {
            connectHashMap = new ConcurrentHashMap<>();
        }
        connectHashMap.put(cid, listener);
        return cid;
    }

    @Override
    public Imqtt remove(long... ids) {
        for (long id : ids) {
            if (messageHashMap != null) {
                messageHashMap.remove(id);
            }
            if (connectHashMap != null) {
                connectHashMap.remove(id);
            }
        }
        return this;
    }

    @Override
    public Imqtt clear() {
        if (messageHashMap != null) {
            messageHashMap.clear();
        }
        if (connectHashMap != null) {
            connectHashMap.clear();
        }
        return this;
    }

    @Override
    public Imqtt reset() {
        if (mqtt != null) {
            mqtt = null;
            client = null;
        }
        return this;
    }

    /**
     * 打印日志
     *
     * @param contents
     */
    public void print(String... contents) {
        int length = contents.length;
        if (length == 0) {
            return;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            builder.append(contents[i]).append(" ");
        }
        Log.i(TAG, builder.toString());
    }

}
