package androidx.iot.log;

import android.content.Context;
import android.util.DisplayMetrics;

import androidx.iot.aiot.License;
import androidx.iot.mqtt.Log;
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

    @Override
    protected void initialize(String project, String dir, String prefix) {
        super.initialize(project, dir, prefix);
        format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        setSupportScheduled(false);
    }

    /**
     * 写入日志
     *
     * @param context 上下文
     */
    public void save(Context context) {
        StringBuilder builder = new StringBuilder();
        builder.append("Application:" + Apk.getApplicationName(context) + "\n");
        builder.append("Date:" + format.format(new Date()) + "\n");
        builder.append("Version Name:" + Apk.getVersionName(context) + "\n");
        builder.append("Version Code:" + Apk.getVersionCode(context) + "\n");
        builder.append("License:" + License.with(context).isLicensed() + "\n");
        builder.append("SN:" + Device.getUniqueId(context) + "\n");
        builder.append("WIFI IP:" + Device.getWifiIpAddress(context) + "\n");
        //分辨率
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int screenWidth = displayMetrics.widthPixels;
        int screenHeight = displayMetrics.heightPixels;
        builder.append("Display:" + screenWidth + " * " + screenHeight + "\n");
        //屏幕密度
        float density = context.getResources().getDisplayMetrics().density;
        builder.append("Density:" + density + "\n");
        Log.i(TAG, builder.toString());
        write(builder.toString(), false);
    }

}
