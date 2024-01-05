package androidx.iot.log;

import android.content.Context;
import android.util.DisplayMetrics;

import androidx.iot.aiot.License;
import androidx.iot.mqtt.Console;
import androidx.iot.utils.Apk;
import androidx.iot.utils.Device;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 设备日志
 */
public class DeviceLog extends LogFile {

    private final String TAG = DeviceLog.class.getSimpleName();
    private SimpleDateFormat format;

    public DeviceLog(Context context) {
        super(context);
    }

    @Override
    protected void initializeParameters(Context context,String project, String dir, String prefix) {
        super.initializeParameters(context,project, dir, prefix);
        format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        setSupportScheduled(false);
    }

    /**
     * 保存
     */
    public void save() {
        StringBuilder builder = new StringBuilder();
        builder.append("Application:" + Apk.getApplicationName(getContext()) + "\n");
        builder.append("Date:" + format.format(new Date()) + "\n");
        builder.append("Version Name:" + Apk.getVersionName(getContext()) + "\n");
        builder.append("Version Code:" + Apk.getVersionCode(getContext()) + "\n");
        builder.append("License:" + License.acquire().isLicensed() + "\n");
        builder.append("SN:" + Device.getUniqueId(getContext()) + "\n");
        builder.append("WIFI IP:" + Device.getWifiIpAddress(getContext()) + "\n");
        //分辨率
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        int screenWidth = displayMetrics.widthPixels;
        int screenHeight = displayMetrics.heightPixels;
        builder.append("Display:" + screenWidth + " * " + screenHeight + "\n");
        //屏幕密度
        float density = getContext().getResources().getDisplayMetrics().density;
        builder.append("Density:" + density + "\n");
        Console.i(TAG, builder.toString());
        write(builder.toString(), false);
    }

}
