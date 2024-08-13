package androidx.iot.net;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;

/**
 * Android 8.0（API 级别 26）及更高版本中使用
 * <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
 */
public class Networking {

    private String TAG = Networking.class.getSimpleName();
    private Context context;
    private ConnectivityManager connectivityManager;
    private NetworkCallback callback;
    private NetworkRequest request;
    private NetworkHandler networkHandler;
    private OnNetworkingListener onNetworkingListener;

    public Networking(Context context) {
        this.context = context;
        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        request = new NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR).addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build();
        callback = new NetworkCallback();
        networkHandler = new NetworkHandler();
    }

    /**
     * 注册网络监听
     *
     * @param listener
     */
    @RequiresPermission(value = "android.permission.ACCESS_NETWORK_STATE")
    public void registerNetworking(OnNetworkingListener listener) {
        onNetworkingListener = listener;
        if (context != null && !NetworkState.isAvailable(connectivityManager)) {
            Log.d(TAG, "onNetworkUnavailable");
            if (onNetworkingListener != null) {
                onNetworkingListener.onNetworkUnavailable();
            }
        }
        connectivityManager.registerNetworkCallback(request, callback);
    }

    /**
     * 销毁网络监听
     */
    public void unregisterNetworking() {
        connectivityManager.unregisterNetworkCallback(callback);
        networkHandler.removeCallbacksAndMessages(null);
        networkHandler = null;
        onNetworkingListener = null;
        context = null;
    }

    private class NetworkCallback extends ConnectivityManager.NetworkCallback {

        @Override
        public void onBlockedStatusChanged(@NonNull Network network, boolean blocked) {
            super.onBlockedStatusChanged(network, blocked);
            Log.d(TAG, "onBlockedStatusChanged");
        }

        @Override
        public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
            super.onCapabilitiesChanged(network, networkCapabilities);
            Log.d(TAG, "onCapabilitiesChanged");
        }

        @Override
        public void onLinkPropertiesChanged(@NonNull Network network, @NonNull LinkProperties linkProperties) {
            super.onLinkPropertiesChanged(network, linkProperties);
            Log.d(TAG, "onLinkPropertiesChanged");
        }

        @Override
        public void onAvailable(@NonNull Network network) {
            super.onAvailable(network);
            Log.d(TAG, "onAvailable");
            networkHandler.sendAvailable(network);
        }

        @Override
        public void onLosing(@NonNull Network network, int maxMsToLive) {
            super.onLosing(network, maxMsToLive);
            Log.d(TAG, "onLosing");
            networkHandler.sendUnavailable();
        }

        @Override
        public void onLost(@NonNull Network network) {
            super.onLost(network);
            Log.d(TAG, "onLost");
            networkHandler.sendUnavailable();
        }

        @Override
        public void onUnavailable() {
            super.onUnavailable();
            Log.d(TAG, "onUnavailable");
            networkHandler.sendUnavailable();
        }

    }

    private class NetworkHandler extends Handler{

        public void sendAvailable(Network network){
            Message message = obtainMessage();
            message.what = 0;
            message.obj = network;
            sendMessage(message);
        }

        public void sendUnavailable(){
            Message message = obtainMessage();
            message.what = 1;
            sendMessage(message);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 0:
                    Network network = (Network) msg.obj;
                    if (onNetworkingListener != null) {
                        onNetworkingListener.onNetworkAvailable(network);
                    }
                    break;
                case 1:
                    if (onNetworkingListener != null) {
                        onNetworkingListener.onNetworkUnavailable();
                    }
                    break;
            }
        }
    }

    public interface OnNetworkingListener {

        /**
         * 网络可用
         *
         * @param network
         */
        void onNetworkAvailable(@NonNull Network network);

        /**
         * 网络不可用
         */
        void onNetworkUnavailable();

    }

}
