package androidx.iot.aiot;

import android.util.Log;

import androidx.iot.entity.DynamicBody;
import androidx.iot.handler.DynamicHandler;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 动态注册
 */
public class Dynamic implements MqttCallback {

    private String TAG = Dynamic.class.getSimpleName();
    /**
     * 动态注册监听
     */
    private Regnwl regnwl;
    private Register register;
    private DynamicHandler handler;
    private ExecutorService service;
    private ConcurrentHashMap<Long, OnDynamicListener> map;

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
        if (register == null) {
            register = new Register(url, instanceId, productKey, productSecret, deviceName, this, handler, map);
        }
        service.submit(register);
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
        if (regnwl == null) {
            regnwl = new Regnwl(url, instanceId, productKey, productSecret, deviceName, this, handler, map);
        }
        service.submit(regnwl);
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
            handler.received(new DynamicBody(topic, payload, map.get(key)));
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {

    }

    /**
     * 释放资源
     */
    public void destroy() {
        if (handler != null) {
            handler.removeReceived();
            handler.removeFailure();
            handler.removeCallbacksAndMessages(null);
            handler = null;
        }
        if (regnwl!=null){
            regnwl.release();
            regnwl = null;
        }
        if (register!=null){
            register.release();
            register = null;
        }
        service.shutdownNow();
        service = null;
        map.clear();
        map = null;
    }

}
