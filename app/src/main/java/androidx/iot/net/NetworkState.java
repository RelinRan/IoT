package androidx.iot.net;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.Log;

/**
 * 网络状态
 * <p>
 * <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
 * <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
 */
public class NetworkState {

    /**
     * 网络是否可用
     *
     * @param context 上下文
     * @return
     */
    public static boolean isAvailable(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Network network = manager.getActiveNetwork();
                if (network != null) {
                    NetworkCapabilities capabilities = manager.getNetworkCapabilities(network);
                    if (capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                        boolean wifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
                        boolean cellular = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
                        Log.d("NetworkState", "wifi = " + wifi + ", cellular = " + cellular);
                        if (wifi || cellular) {
                            return true;
                        }
                    }
                }
            } else {
                NetworkInfo activeNetworkInfo = manager.getActiveNetworkInfo();
                if (activeNetworkInfo != null && activeNetworkInfo.isConnected()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 获取网络类型
     *
     * @param context
     * @return
     */
    public static NetworkType getType(Context context) {
        if (!isAvailable(context)) {
            return NetworkType.UNKNOWN;
        }
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = manager.getActiveNetworkInfo();
        if (info != null) {
            int type = info.getType();
            String subtypeName = info.getSubtypeName();
            if (type == ConnectivityManager.TYPE_WIFI) {
                return NetworkType.WIFI;
            }
            if (type == ConnectivityManager.TYPE_MOBILE) {
                return getSubType(info.getSubtype(), subtypeName);
            }
        }
        return NetworkType.UNKNOWN;
    }

    private static NetworkType getSubType(int subType, String subtypeName) {
        switch (subType) {
            case TelephonyManager.NETWORK_TYPE_GPRS://联通2G
            case TelephonyManager.NETWORK_TYPE_CDMA://电信2G
            case TelephonyManager.NETWORK_TYPE_EDGE://移动2G
            case TelephonyManager.NETWORK_TYPE_1xRTT:
            case TelephonyManager.NETWORK_TYPE_IDEN:
                return NetworkType.NET2G;
            case TelephonyManager.NETWORK_TYPE_EVDO_A://电信3G
            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
            case TelephonyManager.NETWORK_TYPE_EHRPD:
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                return NetworkType.NET3G;
            case TelephonyManager.NETWORK_TYPE_LTE:
                return NetworkType.NET4G;
            case TelephonyManager.NETWORK_TYPE_NR:
                return NetworkType.NET5G;
            default:
                if (subtypeName.equalsIgnoreCase("TD-SCDMA") || subtypeName.equalsIgnoreCase("WCDMA") || subtypeName.equalsIgnoreCase("CDMA2000")) {
                    return NetworkType.NET3G;
                }
                return NetworkType.UNKNOWN;
        }
    }

    /**
     * 获取网络类型
     *
     * @param capabilities
     * @return
     */
    public static String getType(NetworkCapabilities capabilities) {
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            return "WIFI";
        }
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            return "CELLULAR";
        }
        return "UNKNOWN";
    }

    /***
     * 计算网络丢包率
     * @param context
     * @return
     */
    public static float calculatePacketLossRate(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return -1.0f;
        }
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo == null || !networkInfo.isConnected()) {
            return -1.0f;
        }
        // 获取网络接口的数据流量信息
        long txPackets = TrafficStats.getUidTxPackets(android.os.Process.myUid());
        long rxPackets = TrafficStats.getUidRxPackets(android.os.Process.myUid());
        // 计算丢包率
        if (txPackets == TrafficStats.UNSUPPORTED || rxPackets == TrafficStats.UNSUPPORTED) {
            // 不支持获取流量统计信息
            return -1.0f;
        } else {
            // 丢包率 = (发送的数据包数 - 接收的数据包数) / 发送的数据包数
            if (txPackets > 0) {
                return (float) (txPackets - rxPackets) / txPackets;
            } else {
                return 0.0f; // 如果没有发送的数据包，则假设丢包率为0
            }
        }
    }

}
