package androidx.iot.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.util.Log;

import androidx.iot.net.NetworkState;
import androidx.iot.net.OnNetworkListener;

/**
 * 网络情况
 * <p>
 * <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
 */
public class NetworkReceiver extends BroadcastReceiver {

    private final String TAG = NetworkReceiver.class.getSimpleName();

    private OnNetworkListener onNetworkListener;
    private long time = 0;

    /**
     * 注册
     *
     * @param context
     */
    public void register(Context context) {
        Log.d(TAG, "register network state");
        IntentFilter filter = new IntentFilter();
        //android.net.conn.CONNECTIVITY_CHANGE
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        context.registerReceiver(this, filter);
    }

    /**
     * 解注
     *
     * @param context
     */
    public void unregister(Context context) {
        Log.d(TAG, "unregister network state");
        context.unregisterReceiver(this);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (System.currentTimeMillis() - time > 500) {
            boolean isAvailable = NetworkState.isAvailable(context);
            if (isAvailable) {
                if (onNetworkListener != null) {
                    onNetworkListener.onNetworkConnected(NetworkState.getType(context));
                }
            } else {
                if (onNetworkListener != null) {
                    onNetworkListener.onNetworkLost();
                }
            }
        }
        time = System.currentTimeMillis();
    }

    /**
     * 添加网络监听
     *
     * @param listener
     */
    public void addNetworkListener(OnNetworkListener listener) {
        this.onNetworkListener = listener;
    }

}
