package androidx.iot.aiot;

import android.util.Log;

import androidx.iot.entity.DynamicBody;

import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.concurrent.ConcurrentHashMap;

public class Regnwl implements Runnable{

    private final String TAG = Dynamic.class.getSimpleName();
    private String url;
    private String instanceId;
    private String productKey;
    private String productSecret;
    private String deviceName;
    private MqttCallback mqttCallback;
    private DynamicHandler handler;
    private ConcurrentHashMap<Long, OnDynamicListener> map;

    private MqttClient mqttClient;
    private MqttConnectOptions mqttConnectOptions;

    public Regnwl(String url, String instanceId, String productKey, String productSecret, String deviceName, MqttCallback mqttCallback, DynamicHandler handler, ConcurrentHashMap<Long, OnDynamicListener> map) {
        this.url = url;
        this.instanceId = instanceId;
        this.productKey = productKey;
        this.productSecret = productSecret;
        this.deviceName = deviceName;
        this.mqttCallback = mqttCallback;
        this.handler = handler;
        this.map = map;
    }

    @Override
    public void run() {
        try {
            Options options = new Options().regnwl(instanceId, productKey, productSecret, deviceName);
            MemoryPersistence persistence = new MemoryPersistence();
            mqttClient = new MqttClient(url, options.getClientId(), persistence);
            mqttConnectOptions = new MqttConnectOptions();
            mqttConnectOptions.setMqttVersion(4);// MQTT 3.1.1
            mqttConnectOptions.setUserName(options.getUsername());// 用户名
            mqttConnectOptions.setPassword(options.getPassword().toCharArray());// 密码
            mqttConnectOptions.setAutomaticReconnect(false);//MQTT动态注册协议规定必须关闭自动重连。
            mqttClient.setCallback(mqttCallback);
            Log.i(TAG, url + "\n" + options.getClientId() + "\n" + options.getUsername() + "\n" + options.getPassword());
            mqttClient.connect(mqttConnectOptions);
        } catch (MqttException e) {
            Log.e(TAG, "reason " + e.getReasonCode() + " message " + e.getMessage());
            e.printStackTrace();
            for (Long key : map.keySet()) {
                handler.failure(new DynamicBody(e, map.get(key)));
            }
        }
    }

    /**
     * 释放资源
     */
    public void release(){
        mqttClient = null;
        mqttConnectOptions = null;
    }

}
