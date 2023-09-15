package androidx.iot.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import androidx.iot.net.OnWiFiListener;

/**
 * WiFi监听
 * <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
 * <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
 */
public class WiFiReceiver extends BroadcastReceiver {

    private String TAG = WiFiReceiver.class.getSimpleName();
    private WifiManager wifiManager;
    private OnWiFiListener onWiFiListener;
    private boolean register = false;

    /**
     * 注册监听
     *
     * @param context
     */
    public void register(Context context) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.RSSI_CHANGED_ACTION);
        context.registerReceiver(this, filter);
        wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        register = true;
    }

    /**
     * 取消监听
     *
     * @param context
     */
    public void unregister(Context context) {
        if (register) {
            context.unregisterReceiver(this);
        }
        register = false;
    }

    /**
     * 添加WiFi监听
     *
     * @param listener
     */
    public void addWiFiListener(OnWiFiListener listener) {
        this.onWiFiListener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
            boolean enable = wifiManager.isWifiEnabled();
            Log.d(TAG, "enable = " + enable);
            if (onWiFiListener != null) {
                onWiFiListener.onAvailableStatus(wifiManager, enable);
            }
        }
        if (action.equals(WifiManager.RSSI_CHANGED_ACTION)) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            int strength = wifiInfo.getRssi();
            Log.d(TAG, "strength = " + strength);
            if (onWiFiListener != null) {
                onWiFiListener.onRssiChanged(wifiManager, strength);
            }
        }
    }

}
