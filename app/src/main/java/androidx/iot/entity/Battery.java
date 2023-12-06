package androidx.iot.entity;

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
     * 被充电状态
     */
    private int status;
    /**
     * 被充电状态
     */
    private int plugged;
    /**
     * 温度（单位：0.1°C）
     */
    private int temperature ;
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
}
