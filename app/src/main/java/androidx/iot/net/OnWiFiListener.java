package androidx.iot.net;

import android.net.wifi.WifiManager;

public interface OnWiFiListener {

    /**
     * WiFi信号改变
     *
     * @param manager  WiFi管理对象
     * @param strength 信号强度,Wi-Fi信号强度范围通常在-30到-100分贝之间。
     *                 信号强度值是负值，数值越接近零，表示信号越强。一般而言，信号强度在
     *                 -30到-50分贝之间被认为是强信号，
     *                 -50到-70分贝之间是中等信号，
     *                 -70到-90分贝之间是较弱信号，
     *                 而低于-90分贝的信号被认为是非常弱或无信号
     */

    void onRssiChanged(WifiManager manager, int strength);

    /**
     * 可用状态
     *
     * @param manager WiFi管理对象
     * @param enable  是否可用
     */
    void onAvailableStatus(WifiManager manager, boolean enable);

}
