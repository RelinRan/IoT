package androidx.iot.utils;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.storage.StorageManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * 设备
 */
public class Device {

    public static final String TAG = Device.class.getSimpleName();

    /**
     * 获取电池电量
     *
     * @param context
     * @return
     */
    public static int getBatteryLevel(Context context) {
        BatteryManager batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
        //Android5.0以上
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        } else {
            return 0;
        }
    }

    /**
     * 设备首次启动时生成的唯一标识符
     * 注意：如果刷机过程涉及清除设备的数据分区、重置设备或刷入全新的 ROM，那么设备首次启动时会生成新的 ANDROID_ID。这是因为刷机过程类似于设备的出厂复位，设备会重新生成 ANDROID_ID。
     *
     * @param context 上下文
     * @return 设备编号
     */
    public static String getAndroidId(Context context) {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID).toUpperCase();
    }

    /**
     * 获取IMEI号
     *
     * @param context
     * @return
     */
    public static String getIMEI(Context context) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            //Android10不再提供
            return null;
        } else {
            try {
                TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                String deviceId = telephonyManager.getDeviceId();
                return deviceId;
            } catch (Exception e) {
                return null;
            }
        }
    }

    /**
     * 获取以太网MAC地址
     *
     * @return
     */
    public static String getEth0Mac() {
        String macAddress = null;
        if (macAddress == null) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader("/sys/class/net/eth0/address"));
                macAddress = reader.readLine();
                reader.close();
                return macAddress;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return macAddress;
    }

    /**
     * 获取设备唯一ID
     *
     * @param context 上下文
     * @return
     */
    public static String getUniqueId(Context context) {
        String imei = getIMEI(context);
        if (imei != null) {
            Log.i(TAG, "imei = " + imei);
            return imei;
        }
        String mac = getEth0Mac();
        if (mac != null) {
            Log.i(TAG, "mac = " + mac);
            return mac.replace(":", "").toUpperCase();
        }
        String androidId = getAndroidId(context);
        Log.i(TAG, "androidId = " + androidId);
        return androidId;
    }

    /**
     * 获取可移动存储卷路径
     *
     * @param context 上下文
     * @return
     */
    public static List<String> getRemovableStorageVolumePath(Context context) {
        StorageManager storageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
        List<String> paths = new ArrayList<>();
        try {
            Class<?> storageVolumeClazz = Class.forName("android.os.storage.StorageVolume");
            Method getVolumeList = storageManager.getClass().getMethod("getVolumeList");
            Method getPath = storageVolumeClazz.getMethod("getPath");
            Method isRemovable = storageVolumeClazz.getMethod("isRemovable");
            Object[] storageVolumes = (Object[]) getVolumeList.invoke(storageManager);
            for (Object storageVolume : storageVolumes) {
                String path = (String) getPath.invoke(storageVolume);
                boolean removable = (Boolean) isRemovable.invoke(storageVolume);
                if (removable) {
                    paths.add(path);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return paths;
    }

    /**
     * 获取Wifi ip地址
     * <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
     *
     * @param context
     * @return
     */
    public static String getWifiIpAddress(Context context) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipAddress = wifiInfo.getIpAddress();
        return String.format("%d.%d.%d.%d",
                (ipAddress & 0xff),
                (ipAddress >> 8 & 0xff),
                (ipAddress >> 16 & 0xff),
                (ipAddress >> 24 & 0xff));
    }

    /**
     * 获取网口IP
     *
     * @return
     */
    public static String getInterfaceIpAddress() {
        try {
            for (Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces(); networkInterfaces.hasMoreElements(); ) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                if (networkInterface.isUp() && !networkInterface.isLoopback()) {
                    for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                        if (interfaceAddress.getAddress() instanceof Inet4Address) {
                            return interfaceAddress.getAddress().getHostAddress();
                        }
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return "";
    }

}
