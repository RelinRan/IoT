package androidx.iot.mqtt;

import android.content.Context;
import android.util.Log;

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
    private MqttAndroidClient mqttAndroidClient;
    /**
     * MQTT参数
     */
    private MqttConnectOptions mqttConnectOptions;
    /**
     * 是否调试
     */
    private boolean debug;
    private MqttHandler handler;
    /**
     * MQTT
     */
    private static Mqtt instance;


    public Mqtt() {
        Log.i(TAG, "instantiation mqtt");
        handler = new MqttHandler();
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
    public Mqtt debug(boolean debug) {
        this.debug = debug;
        return this;
    }

    /**
     * 初始化物联网操作对象
     *
     * @param context 上下文
     * @param options 参数
     * @return
     */
    public static Mqtt initialize(Context context, MqttOptions options) {
        if (instance == null) {
            synchronized (Mqtt.class) {
                if (instance == null) {
                    instance = new Mqtt();
                }
            }
        }
        instance.setMqttAndroidClientOptions(context, options);
        return instance;
    }

    /**
     * 获取客户端对象（必须初始化才可以调用）
     *
     * @return
     */
    public static Mqtt acquire() {
        return instance;
    }

    /**
     * 设置连接参数
     *
     * @param context 上下文
     * @param options 参数
     */
    public void setMqttAndroidClientOptions(Context context, MqttOptions options) {
        mqttAndroidClient = new MqttAndroidClient(context.getApplicationContext(), options.getHost(), options.getClientId());
        mqttAndroidClient.registerResources(context.getApplicationContext());
        mqttAndroidClient.setCallback(this);
        mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setUserName(options.getUserName());
        mqttConnectOptions.setPassword(options.getPassword().toCharArray());
        Log.i(TAG, "service url " + options.getHost());
        if (debug) {
            Log.i(TAG, options.getClientId() + " " + options.getUserName() + " " + options.getPassword());
        }
    }

    /**
     * 设置Android Mqtt客户端
     *
     * @param mqttAndroidClient
     */
    public void setMqttAndroidClient(MqttAndroidClient mqttAndroidClient) {
        if (mqttAndroidClient != null) {
            mqttAndroidClient.setCallback(this);
        }
        this.mqttAndroidClient = mqttAndroidClient;
    }

    /**
     * Android Mqtt客户端
     *
     * @return
     */
    public MqttAndroidClient getMqttAndroidClient() {
        return mqttAndroidClient;
    }


    /**
     * 设置Mqtt参数
     *
     * @param mqttConnectOptions
     */
    public void setMqttConnectOptions(MqttConnectOptions mqttConnectOptions) {
        this.mqttConnectOptions = mqttConnectOptions;
    }

    /**
     * Mqtt连接参数
     *
     * @return
     */
    public MqttConnectOptions getMqttConnectOptions() {
        return mqttConnectOptions;
    }

    @Override
    public Imqtt connect() {
        try {
            if (mqttAndroidClient != null) {
                mqttAndroidClient.connect(mqttConnectOptions, null, this);
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
        return this;
    }

    @Override
    public void disconnect() {
        try {
            if (mqttAndroidClient != null) {
                mqttAndroidClient.unregisterResources();
                Thread.sleep(50);
                mqttAndroidClient.disconnect();
            }
        } catch (MqttException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean isConnected() {
        if (mqttAndroidClient == null) {
            return false;
        }
        return mqttAndroidClient.isConnected();
    }

    //*******************状态**********************
    @Override
    public void connectionLost(Throwable cause) {
        Log.i(TAG, "connection lost");
        try {
            if (mqttAndroidClient != null) {
                mqttAndroidClient.connect();
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
        if (connectHashMap != null) {
            for (Long key : connectHashMap.keySet()) {
                handler.connectionLost(cause, connectHashMap.get(key));
            }
        }
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        String payload = new String(message.getPayload());
        if (debug) {
            Log.i(TAG, "received " + topic + " " + payload);
        }
        if (messageHashMap != null) {
            for (Long key : messageHashMap.keySet()) {
                handler.messageArrived(topic, message, messageHashMap.get(key));
            }
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        if (messageHashMap != null) {
            for (Long key : messageHashMap.keySet()) {
                handler.deliveryComplete(token, messageHashMap.get(key));
            }
        }
    }

    //*********************[连接成功]**********************

    @Override
    public void onSuccess(IMqttToken token) {
        Log.i(TAG, "connection successful");
        if (connectHashMap != null) {
            for (Long key : connectHashMap.keySet()) {
                handler.connectionSuccessful(token, connectHashMap.get(key));
            }
        }
    }

    @Override
    public void onFailure(IMqttToken token, Throwable exception) {
        Log.i(TAG, "connection failed");
        if (connectHashMap != null) {
            for (Long key : connectHashMap.keySet()) {
                handler.connectFailed(token, exception, connectHashMap.get(key));
            }
        }
    }

    //***********************[END]*****************************

    @Override
    public Imqtt publish(String topic, String payload, IMqttActionListener listener) {
        try {
            if (mqttAndroidClient != null) {
                if (mqttAndroidClient.isConnected() == false) {
                    mqttAndroidClient.connect();
                }
                MqttMessage message = new MqttMessage();
                int id = (int) System.currentTimeMillis() + 200;
                message.setPayload(payload.getBytes());
                message.setId(id);
                message.setQos(0);
                mqttAndroidClient.publish(topic, message, null, listener);
                if (debug) {
                    Log.i(TAG, "publish id:" + id + " " + topic + " " + payload);
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
                Log.i(TAG, "subscribe " + topic);
            }
            mqttAndroidClient.subscribe(topic, 0, null, listener);
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
        if (connectHashMap == null) {
            connectHashMap = new ConcurrentHashMap<>();
        }
        long cid = System.currentTimeMillis() + connectHashMap.size() + 1;
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
    public void destroy() {
        disconnect();
        if (messageHashMap != null) {
            messageHashMap.clear();
            connectHashMap = null;
        }
        if (connectHashMap != null) {
            connectHashMap.clear();
            connectHashMap = null;
        }
        if (handler != null) {
            handler.destroy();
            handler = null;
        }
        instance = null;
    }

}
