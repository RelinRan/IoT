package android.mqtt.iot.utils

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import android.os.storage.StorageManager
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.SocketException
import java.nio.charset.StandardCharsets
import java.util.Locale


object Device {
    val TAG: String = Device::class.java.simpleName


    fun getAndroidId(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            .uppercase(
                Locale.getDefault()
            )
    }


    fun getIMEI(context: Context): String? {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            return null
        } else {
            try {
                val telephonyManager =
                    context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                val deviceId = telephonyManager.deviceId
                return deviceId
            } catch (e: Exception) {
                return null
            }
        }
    }

    val eth0Mac: String?

        get() {
            var macAddress: String? = null
            if (macAddress == null) {
                try {
                    // /sys/class/net/wlan0/address
                    // /sys/class/net/eth0/address
                    // /sys/class/net/eth1/address
                    val reader = BufferedReader(FileReader("/sys/class/net/eth0/address"))
                    macAddress = reader.readLine()
                    reader.close()
                    return macAddress
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            return macAddress
        }

    val wlanMac: String?

        get() {
            var macAddress: String? = null
            try {
                val reader = BufferedReader(FileReader("/sys/class/net/wlan0/address"))
                macAddress = reader.readLine()
                reader.close()
                return macAddress
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return macAddress
        }


    fun getUniqueId(context: Context): String {
        val eth0Mac: String? = eth0Mac
        if (eth0Mac != null && eth0Mac != "00:00:00:00:00:00") {
            Log.i(TAG, "eth0Mac = $eth0Mac")
            return eth0Mac.replace(":", "").uppercase(Locale.getDefault())
        }
        val wlanMac: String? = wlanMac
        if (wlanMac != null && wlanMac != "00:00:00:00:00:00") {
            Log.i(TAG, "wlanMac = $eth0Mac")
            return wlanMac.replace(":", "").uppercase(Locale.getDefault())
        }
        val imei: String? = getIMEI(context)
        if (imei != null) {
            Log.i(TAG, "imei = $imei")
            return imei
        }
        val androidId: String = getAndroidId(context)
        Log.i(TAG, "androidId = $androidId")
        return androidId
    }


    fun getRemovableStorageVolumePath(context: Context): List<String> {
        val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
        val paths: MutableList<String> = ArrayList()
        try {
            val storageVolumeClazz = Class.forName("android.os.storage.StorageVolume")
            val getVolumeList = storageManager.javaClass.getMethod("getVolumeList")
            val getPath = storageVolumeClazz.getMethod("getPath")
            val isRemovable = storageVolumeClazz.getMethod("isRemovable")
            val storageVolumes = getVolumeList.invoke(storageManager) as Array<Any>
            for (storageVolume in storageVolumes) {
                val path = getPath.invoke(storageVolume) as String
                val removable = isRemovable.invoke(storageVolume) as Boolean
                if (removable) {
                    paths.add(path)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return paths
    }


    fun getWifiIpAddress(context: Context): String {
        val wifiManager =
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiManager.connectionInfo
        val ipAddress = wifiInfo.ipAddress
        return String.format(
            "%d.%d.%d.%d",
            (ipAddress and 0xff),
            (ipAddress shr 8 and 0xff),
            (ipAddress shr 16 and 0xff),
            (ipAddress shr 24 and 0xff)
        )
    }

    val interfaceIpAddress: String

        get() {
            try {
                val networkInterfaces = NetworkInterface.getNetworkInterfaces()
                while (networkInterfaces.hasMoreElements()) {
                    val networkInterface = networkInterfaces.nextElement()
                    if (networkInterface.isUp && !networkInterface.isLoopback) {
                        for (interfaceAddress in networkInterface.interfaceAddresses) {
                            if (interfaceAddress.address is Inet4Address) {
                                return interfaceAddress.address.hostAddress
                            }
                        }
                    }
                }
            } catch (e: SocketException) {
                e.printStackTrace()
            }
            return ""
        }


    fun getHeader(context: Context): StringBuilder {
        val builder = StringBuilder()
        val sn: String = getUniqueId(context)
        val eSign: String =
            Base64.encoder.encodeToString(sn.toByteArray(StandardCharsets.UTF_8))
        builder.append("Company:深圳市安保医疗感控科技股份有限公司").append("\n")
        builder.append("eSign:").append(eSign).append("\n")
        builder.append("Application:" + Apk.getApplicationName(context)).append("\n")
        builder.append("Version Name:" + Apk.getVersionName(context)).append("\n")
        builder.append("Version Code:" + Apk.getVersionCode(context)).append("\n")
//        builder.append("License:" + License.acquire().isLicensed()).append("\n")
        builder.append("SN:$sn").append("\n")
        builder.append("WIFI IP:" + getWifiIpAddress(context))
            .append("\n")
        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        builder.append("Display:$screenWidth * $screenHeight").append("\n")
        val density = context.resources.displayMetrics.density
        builder.append("Density:$density").append("\n")
        return builder
    }
}
