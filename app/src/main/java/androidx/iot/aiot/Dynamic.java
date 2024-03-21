package androidx.iot.aiot;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.iot.entity.DynamicBody;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 动态注册
 */
public class Dynamic implements MqttCallback {

    private String TAG = Dynamic.class.getSimpleName();
    /**
     * Mqtt客户端
     */
    private MqttClient mqttClient;
    /**
     * Mqtt参数
     */
    private MqttConnectOptions mqttConnectOptions;
    /**
     * 动态注册监听
     */
    private ConcurrentHashMap<Long, OnDynamicListener> map;
    private DynamicHandler handler;
    private ExecutorService service;

    public Dynamic() {
        map = new ConcurrentHashMap<>();
        handler = new DynamicHandler();
        service = Executors.newCachedThreadPool();
    }

    /**
     * 添加动态注册监听
     *
     * @param id       监听id
     * @param listener 动态注册监听
     * @return
     */
    public Long addDynamicListener(Long id, OnDynamicListener listener) {
        map.put(id, listener);
        return id;
    }

    /**
     * 添加动态注册监听
     *
     * @param listener 动态注册监听
     * @return
     */
    public Long addDynamicListener(OnDynamicListener listener) {
        Long id = System.currentTimeMillis() + map.size();
        map.put(id, listener);
        return id;
    }

    /**
     * 移除监听
     *
     * @param ids 动态注册监听id
     */
    public void remove(Long... ids) {
        if (map.size() == 0) {
            return;
        }
        for (Long id : ids) {
            map.remove(id);
        }
    }

    /**
     * 一型一密预注册认证方式
     * 主题：/ext/register
     * 返回：{"deviceSecret":"xxx","productKey":"xxx","deviceName":"xxx"}
     *
     * @param productKey    产品key
     * @param productSecret 产品secret
     * @param deviceName    设备ID
     */
    public void register(String productKey, String productSecret, String deviceName) {
        register(ServerURL.value(true, productKey, "cn-shanghai", 1883), null, productKey, productSecret, deviceName);
    }

    /**
     * 一型一密预注册认证方式
     * 主题：/ext/register
     * 返回：{"deviceSecret":"xxx","productKey":"xxx","deviceName":"xxx"}
     *
     * @param instanceId    实列ID
     * @param productKey    产品key
     * @param productSecret 产品secret
     * @param deviceName    设备ID
     * @return 0：成功
     */
    public void register(String instanceId, String productKey, String productSecret, String deviceName) {
        register(ServerURL.value(true, productKey, "cn-shanghai", 1883), instanceId, productKey, productSecret, deviceName);
    }

    /**
     * 一型一密预注册认证方式
     * 主题：/ext/register
     * 返回：{"deviceSecret":"xxx","productKey":"xxx","deviceName":"xxx"}
     *
     * @param url           服务地址
     * @param instanceId    实列ID
     * @param productKey    产品key
     * @param productSecret 产品secret
     * @param deviceName    设备ID
     * @return 0：成功
     */
    public void register(String url, String instanceId, String productKey, String productSecret, String deviceName) {
        service.submit(() -> {
            try {
                Options options = new Options().register(instanceId, productKey, productSecret, deviceName);
                MemoryPersistence persistence = new MemoryPersistence();
                mqttClient = new MqttClient(url, options.getClientId(), persistence);
                mqttConnectOptions = new MqttConnectOptions();
                mqttConnectOptions.setMqttVersion(4);// MQTT 3.1.1
                mqttConnectOptions.setUserName(options.getUsername());// 用户名
                mqttConnectOptions.setPassword(options.getPassword().toCharArray());// 密码
                mqttConnectOptions.setAutomaticReconnect(false);//MQTT动态注册协议规定必须关闭自动重连。
                mqttClient.setCallback(this);
                mqttClient.connect(mqttConnectOptions);
            } catch (MqttException e) {
                Log.e(TAG, "reason " + e.getReasonCode() + " message " + e.getMessage());
                e.printStackTrace();
                for (Long key : map.keySet()) {
                    handler.send(2, new DynamicBody(e, map.get(key)));
                }
            }
        });
    }

    /**
     * 一型一密免预注册认证方式
     * 主题：/ext/regnwl
     * 返回：{"clientId":"xxx","productKey":"xxx","deviceName":"xxx","deviceToken":"xxx"}
     *
     * @param productKey    产品key
     * @param productSecret 产品secret
     * @param deviceName    设备ID
     * @return 0：成功
     */
    public void regnwl(String productKey, String productSecret, String deviceName) {
        register(ServerURL.value(true, productKey, "cn-shanghai", 1883), null, productKey, productSecret, deviceName);
    }

    /**
     * 一型一密免预注册认证方式
     * 主题：/ext/regnwl
     * 返回：{"clientId":"xxx","productKey":"xxx","deviceName":"xxx","deviceToken":"xxx"}
     *
     * @param instanceId    实列ID
     * @param productKey    产品key
     * @param productSecret 产品secret
     * @param deviceName    设备ID
     * @return 0：成功
     */
    public void regnwl(String instanceId, String productKey, String productSecret, String deviceName) {
        regnwl(ServerURL.value(true, productKey, "cn-shanghai", 1883), instanceId, productKey, productSecret, deviceName);
    }

    /**
     * 一型一密免预注册认证方式
     * 主题：/ext/regnwl
     * 返回：{"clientId":"xxx","productKey":"xxx","deviceName":"xxx","deviceToken":"xxx"}
     *
     * @param url           服务地址
     * @param instanceId    实列ID
     * @param productKey    产品key
     * @param productSecret 产品secret
     * @param deviceName    设备ID
     * @return 0：成功
     */
    public void regnwl(String url, String instanceId, String productKey, String productSecret, String deviceName) {
        service.submit(() -> {
            try {
                Options options = new Options().regnwl(instanceId, productKey, productSecret, deviceName);
                MemoryPersistence persistence = new MemoryPersistence();
                mqttClient = new MqttClient(url, options.getClientId(), persistence);
                mqttConnectOptions = new MqttConnectOptions();
                mqttConnectOptions.setMqttVersion(4);// MQTT 3.1.1
                mqttConnectOptions.setUserName(options.getUsername());// 用户名
                mqttConnectOptions.setPassword(options.getPassword().toCharArray());// 密码
                mqttConnectOptions.setAutomaticReconnect(false);//MQTT动态注册协议规定必须关闭自动重连。
                mqttClient.setCallback(this);
                Log.i(TAG, url + "\n" + options.getClientId() + "\n" + options.getUsername() + "\n" + options.getPassword());
                mqttClient.connect(mqttConnectOptions);
            } catch (MqttException e) {
                Log.e(TAG, "reason " + e.getReasonCode() + " message " + e.getMessage());
                e.printStackTrace();
                for (Long key : map.keySet()) {
                    handler.send(2, new DynamicBody(e, map.get(key)));
                }
            }
        });
    }

    @Override
    public void connectionLost(Throwable throwable) {
        Log.i(TAG, "connection lost");
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        String payload = new String(message.getPayload());
        Log.i(TAG, "received " + topic + " " + payload);
        for (Long key : map.keySet()) {
            handler.send(1, new DynamicBody(topic, payload, map.get(key)));
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {

    }

    /**
     * 断开连接
     */
    public void disconnect() {
        if (mqttClient != null && mqttClient.isConnected()) {
            try {
                mqttClient.disconnect(400);
            } catch (MqttException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public class DynamicHandler extends Handler {

        public void send(int what, DynamicBody body) {
            Message message = obtainMessage();
            message.what = what;
            message.obj = body;
            sendMessage(message);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            DynamicBody body = (DynamicBody) msg.obj;
            switch (msg.what) {
                case 1:
                    if (body != null && body.getListener() != null) {
                        body.getListener().onDynamicRegisterReceived(body.getTopic(), body.getPayload());
                    }
                    break;
                case 2:
                    if (body != null && body.getListener() != null) {
                        if (body != null && body.getListener() != null) {
                            body.getListener().onDynamicRegisterFailure(body.getException());
                        }
                    }
                    break;
            }
        }
    }

    /**
     * 释放资源
     */
    public void destroy() {
        if (handler != null) {
            handler.removeMessages(1);
            handler.removeCallbacksAndMessages(null);
            handler = null;
        }
        if (service != null) {
        }
    }

}
