package androidx.iot.log;

/**
 * 日志级别
 */
public enum LogLevel {

    /**
     * 致命
     */
    F("FATAL"),
    /**
     * 错误
     */
    E("ERROR"),
    /**
     * 警告
     */
    W("WARN"),
    /**
     * 信息
     */
    I("INFO"),
    /**
     * 调试
     */
    D("DEBUG"),
    /**
     * 详细
     */
    V("VERBOSE");

    private String level;

    LogLevel(String level) {
        this.level = level;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

}
