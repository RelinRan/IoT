package androidx.iot.entity;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 上报的日志
 */
public class Log {

    /**
     * 日志的采集时间，为设备本地UTC时间，包含时区信息，以毫秒计，格式为"yyyy-MM-dd'T'HH:mm:ss.SSSZ"。
     * 可上报其它字符串格式，但不利于问题排查，不推荐使用。
     */
    private String utcTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(new Date());
    /**
     * 日志级别。可以使用默认日志级别，也可以自定义日志级别。默认日志级别从高到低为：
     * FATAL
     * ERROR
     * WARN
     * INFO
     * DEBUG
     */
    private String logLevel;
    /**
     * 模块名称：
     * 当设备端使用Android SDK时，模块名称为ALK-LK。
     * 当设备端使用C SDK时，需自定义模块名称。
     * 当设备端使用自行开发的SDK时，需自定义模块名称。
     */
    private String module = "ALK-LK";
    /**
     * 结果状态码，Sting类型的数字。
     * 错误码包含设备端SDK生成的错误码（Android SDK的错误码和C SDK的错误码）和用户自定义的状态码。
     * https://help.aliyun.com/zh/iot/developer-reference/error-codes-1?spm=a2c4g.11186623.0.0.6e2877e1rrOAit#reference4599
     */
    private String code;
    /**
     * 可选参数，上下文跟踪内容，设备端使用Alink协议消息的id，App端使用TraceId（追踪ID）。
     */
    private String traceContext = System.currentTimeMillis()+"";
    /**
     * 日志内容详情。
     */
    private String logContent;

    public String getUtcTime() {
        return utcTime;
    }

    public void setUtcTime(String utcTime) {
        this.utcTime = utcTime;
    }

    public String getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getTraceContext() {
        return traceContext;
    }

    public void setTraceContext(String traceContext) {
        this.traceContext = traceContext;
    }

    public String getLogContent() {
        return logContent;
    }

    public void setLogContent(String logContent) {
        this.logContent = logContent;
    }

    public JSONObject toJSONObject() {
        JSONObject object = new JSONObject();
        try {
            object.put("utcTime", utcTime);
            object.put("logLevel", logLevel);
            object.put("module", module);
            object.put("code", code);
            object.put("traceContext", traceContext);
            object.put("logContent", logContent);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return object;
    }

}
