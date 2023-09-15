package androidx.iot.net;

public interface OnNetworkListener {

    void onNetworkLost();

    void onNetworkConnected(NetworkType type);

}
