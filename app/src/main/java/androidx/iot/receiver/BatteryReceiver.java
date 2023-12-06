package androidx.iot.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import androidx.iot.entity.Battery;

/**
 * 电池电量
 */
public class BatteryReceiver extends BroadcastReceiver {

    private boolean register;
    private Battery battery;
    private OnBatteryChangeListener onBatteryChangeListener;


    /**
     * 注册监听
     *
     * @param context
     */
    public void register(Context context) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        context.registerReceiver(this, filter);
        register = true;
    }

    /**
     * 取消监听
     *
     * @param context
     */
    public void unregister(Context context) {
        if (register) {
            context.unregisterReceiver(this);
        }
        register = false;
    }

    /**
     * @param onBatteryChangeListener
     */
    public void addBatteryChangeListener(OnBatteryChangeListener onBatteryChangeListener) {
        this.onBatteryChangeListener = onBatteryChangeListener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_BATTERY_CHANGED)) {
            if (battery == null) {
                battery = new Battery();
            }
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1); //当前电量
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1); //最大电量
            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1); //被充电状态
            int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1); //充电类型
            int temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1); //温度（单位：0.1°C）
            int voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1); //电压（单位：mV）
            battery.setLevel(level);
            battery.setScale(scale);
            battery.setStatus(status);
            battery.setPlugged(plugged);
            battery.setTemperature(temperature);
            battery.setVoltage(voltage);
            if (onBatteryChangeListener != null) {
                onBatteryChangeListener.onBatteryChanged(battery);
            }
        }
    }

}
