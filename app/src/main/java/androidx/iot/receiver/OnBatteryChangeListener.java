package androidx.iot.receiver;

import androidx.iot.entity.Battery;

/**
 * 电池电量改变监听
 */
public interface OnBatteryChangeListener {

    /**
     * 电池改变
     *
     * @param battery 电池
     */
    void onBatteryChanged(Battery battery);

}
