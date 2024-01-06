package androidx.iot.mqtt;

import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.iot.entity.MqttBody;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MqttHandler extends Handler {

    public void connectionSuccessful(IMqttToken token, OnConnectListener listener) {
        Message msg = obtainMessage();
        msg.what = 1;
        MqttBody body = new MqttBody();
        body.setToken(token);
        body.setConnectListener(listener);
        msg.obj = body;
        sendMessage(msg);
    }

    public void connectFailed(IMqttToken token, Throwable exception, OnConnectListener listener) {
        Message msg = obtainMessage();
        msg.what = 2;
        MqttBody body = new MqttBody();
        body.setToken(token);
        body.setThrowable(exception);
        body.setConnectListener(listener);
        msg.obj = body;
        sendMessage(msg);
    }

    public void connectionLost(Throwable exception, OnConnectListener listener) {
        Message msg = obtainMessage();
        msg.what = 3;
        MqttBody body = new MqttBody();
        body.setThrowable(exception);
        body.setConnectListener(listener);
        msg.obj = body;
        sendMessage(msg);
    }

    public void messageArrived(String topic, MqttMessage message, OnMessageListener listener) {
        Message msg = obtainMessage();
        msg.what = 4;
        MqttBody body = new MqttBody();
        body.setTopic(topic);
        body.setMessage(message);
        body.setMessageListener(listener);
        msg.obj = body;
        sendMessage(msg);
    }

    public void deliveryComplete(IMqttDeliveryToken token, OnMessageListener listener) {
        Message msg = obtainMessage();
        msg.what = 5;
        MqttBody body = new MqttBody();
        body.setDeliveryToken(token);
        body.setMessageListener(listener);
        msg.obj = body;
        sendMessage(msg);
    }

    @Override
    public void handleMessage(@NonNull Message msg) {
        super.handleMessage(msg);
        MqttBody body = (MqttBody) msg.obj;
        switch (msg.what) {
            case 1:
                if (body.getConnectListener() != null) {
                    body.getConnectListener().onConnectionSuccessful(body.getToken());
                }
                break;
            case 2:
                if (body.getConnectListener() != null) {
                    body.getConnectListener().onConnectionFailed(body.getToken(), body.getThrowable());
                }
                break;
            case 3:
                if (body.getConnectListener() != null) {
                    body.getConnectListener().onConnectionLost(body.getThrowable());
                }
                break;
            case 4:
                if (body.getMessageListener() != null) {
                    body.getMessageListener().onMessageReceived(body.getTopic(), body.getMessage());
                }
                break;
            case 5:
                if (body.getMessageListener() != null) {
                    body.getMessageListener().onMessageDelivered(body.getDeliveryToken());
                }
                break;
        }
    }

    /**
     * 释放资源
     */
    public void destroy() {
        removeMessages(1);
        removeMessages(2);
        removeMessages(3);
        removeMessages(4);
        removeMessages(5);
        removeCallbacksAndMessages(null);
    }

}