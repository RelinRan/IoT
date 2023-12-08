package androidx.iot.entity;

import android.os.BatteryManager;

public class Battery {

    /**
     * 当前电量
     */
    private int level;
    /**
     * 最大电量
     */
    private int scale;
    /**
     * 被充电状态<br/>
     * {@link BatteryManager#BATTERY_STATUS_UNKNOWN}：电池状态未知<br/>
     * {@link BatteryManager#BATTERY_STATUS_CHARGING}：电池正在充电<br/>
     * {@link BatteryManager#BATTERY_STATUS_DISCHARGING}：电池正在放电<br/>
     * {@link BatteryManager#BATTERY_STATUS_NOT_CHARGING}：电池未在充电<br/>
     * {@link BatteryManager#BATTERY_STATUS_FULL}：电池已充满<br/>
     */
    private int status;
    /**
     * 被充电状态<br/>
     * {@link BatteryManager#BATTERY_PLUGGED_AC}：表示已连接交流电充电器<br/>
     * {@link BatteryManager#BATTERY_PLUGGED_USB}：表示已连接USB充电<br/>
     * {@link BatteryManager#BATTERY_PLUGGED_WIRELESS}：表示已连接无线充电器<br/>
     */
    private int plugged;
    /**
     * 温度（单位：0.1°C）
     */
    private int temperature;
    /**
     * 电压（单位：mV）
     */
    private int voltage;

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getScale() {
        return scale;
    }

    public void setScale(int scale) {
        this.scale = scale;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getPlugged() {
        return plugged;
    }

    public void setPlugged(int plugged) {
        this.plugged = plugged;
    }

    public int getTemperature() {
        return temperature;
    }

    public void setTemperature(int temperature) {
        this.temperature = temperature;
    }

    public int getVoltage() {
        return voltage;
    }

    public void setVoltage(int voltage) {
        this.voltage = voltage;
    }


    @Override
    public String toString() {
        return "Battery{" +
                "level=" + level +
                ", scale=" + scale +
                ", status=" + status +
                ", plugged=" + plugged +
                ", temperature=" + temperature +
                ", voltage=" + voltage +
                '}';
    }
}
