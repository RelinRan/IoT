package androidx.iot.log;

import android.app.ActivityManager;
import android.content.Context;
import android.util.DisplayMetrics;

import androidx.iot.aiot.License;
import androidx.iot.mqtt.Console;
import androidx.iot.utils.Apk;
import androidx.iot.utils.Device;
import androidx.iot.utils.Memory;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 设备日志
 */
public class DeviceLog extends LogFile {

    private static DeviceLog instance;

    public static DeviceLog initialize(Context context, String projectDir) {
        if (instance == null) {
            synchronized (DeviceLog.class) {
                if (instance == null) {
                    instance = new DeviceLog(context.getApplicationContext());
                    instance.setFolder(projectDir, "Device");
                    instance.setPrefix("device");
                }
            }
        }
        return instance;
    }

    public static DeviceLog acquire() {
        return instance;
    }

    private DeviceLog(Context context) {
        super(context);
    }

    @Override
    protected void initializeParameters(Context context, String project, String dir, String prefix) {
        super.initializeParameters(context, project, dir, prefix);
        setSupportScheduled(false);
    }

    /**
     * 日志
     *
     * @param content 内容
     */
    public void log(String... content) {
        if (instance == null) {
            return;
        }
        StringBuilder builder = new StringBuilder();
        for (String item : content) {
            builder.append(item);
        }
        String value = builder.toString();
        write(value);
    }

}
