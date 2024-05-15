package androidx.iot.handler;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.iot.aiot.OnMediaLicenseListener;

/**
 * 授权Handler
 */
public class LicenseHandler extends Handler {

    public final int WHAT_LICENSE = 100;

    /**
     * 发送
     *
     * @param license  授权信息
     * @param listener 监听
     */
    public void send(String license, OnMediaLicenseListener listener) {
        Message message = obtainMessage();
        message.what = WHAT_LICENSE;
        message.obj = listener;
        Bundle bundle = new Bundle();
        bundle.putString("LICENSE", license);
        message.setData(bundle);
        sendMessage(message);
    }

    /**
     * 移除消息
     */
    public void remove() {
        removeMessages(WHAT_LICENSE);
    }

    @Override
    public void handleMessage(@NonNull Message msg) {
        super.handleMessage(msg);
        switch (msg.what) {
            case WHAT_LICENSE:
                OnMediaLicenseListener listener = (OnMediaLicenseListener) msg.obj;
                String triples = msg.getData().getString("LICENSE");
                if (listener != null) {
                    listener.onMediaLicenseGranted(triples);
                }
                break;
        }
    }

}
