package androidx.iot.log;

import android.util.Log;

import androidx.iot.aiot.Link;

/**
 * 阿里平台日志服务,上传本地日志到阿里物联网平台
 */
public class LogService {

    /**
     * 最大一次打印长度
     */
    public final static int MAX_LENGTH = 2000;

    /**
     * 打印致命日志
     *
     * @param report  是否上报
     * @param module  模块名称
     * @param code    状态码
     * @param content 内容
     */
    public static void f(boolean report, String module, int code, String content) {
        maxPrint(report, LogLevel.F, module, code, content);
    }

    /**
     * 打印致命日志
     *
     * @param report  是否上报
     * @param module  模块名称
     * @param content 内容
     */
    public static void f(boolean report, String module, String content) {
        maxPrint(report, LogLevel.F, module, LogCode.OK, content);
    }

    /**
     * 打印致命日志并上报
     *
     * @param module  模块名称
     * @param code    状态码
     * @param content 内容
     */
    public static void f(String module, int code, String content) {
        maxPrint(true, LogLevel.F, module, code, content);
    }

    /**
     * 打印致命日志并上报
     *
     * @param module  模块名称
     * @param content 内容
     */
    public static void f(String module, String content) {
        maxPrint(true, LogLevel.F, module, LogCode.OK, content);
    }

    /**
     * 打印错误日志
     *
     * @param report  是否上报
     * @param module  模块名称
     * @param code    状态码
     * @param content 内容
     */
    public static void e(boolean report, String module, int code, String content) {
        maxPrint(report, LogLevel.E, module, code, content);
    }

    /**
     * 打印错误日志
     *
     * @param report  是否上报
     * @param module  模块名称
     * @param content 内容
     */
    public static void e(boolean report, String module,String content) {
        maxPrint(report, LogLevel.E, module, LogCode.OK, content);
    }

    /**
     * 打印错误日志并上报
     *
     * @param module  模块名称
     * @param code    状态码
     * @param content 内容
     */
    public static void e(String module, int code, String content) {
        maxPrint(true, LogLevel.E, module, code, content);
    }

    /**
     * 打印错误日志并上报
     *
     * @param module  模块名称
     * @param content 内容
     */
    public static void e(String module, String content) {
        maxPrint(true, LogLevel.E, module, LogCode.OK, content);
    }

    /**
     * 打印警告日志
     *
     * @param report  是否上报
     * @param module  模块名称
     * @param code    状态码
     * @param content 内容
     */
    public static void w(boolean report, String module, int code, String content) {
        maxPrint(report, LogLevel.W, module, code, content);
    }

    /**
     * 打印警告日志
     *
     * @param report  是否上报
     * @param module  模块名称
     * @param content 内容
     */
    public static void w(boolean report, String module, String content) {
        maxPrint(report, LogLevel.W, module, LogCode.OK, content);
    }

    /**
     * 打印警告日志并上报
     *
     * @param module  模块名称
     * @param code    状态码
     * @param content 内容
     */
    public static void w(String module, int code, String content) {
        maxPrint(true, LogLevel.W, module, code, content);
    }

    /**
     * 打印警告日志并上报
     *
     * @param module  模块名称
     * @param content 内容
     */
    public static void w(String module, String content) {
        maxPrint(true, LogLevel.W, module, LogCode.OK, content);
    }

    /**
     * 打印信息日志
     *
     * @param report  是否上报
     * @param module  模块名称
     * @param code    状态码
     * @param content 内容
     */
    public static void i(boolean report, String module, int code, String content) {
        maxPrint(report, LogLevel.I, module, code, content);
    }

    /**
     * 打印信息日志
     *
     * @param report  是否上报
     * @param module  模块名称
     * @param content 内容
     */
    public static void i(boolean report, String module, String content) {
        maxPrint(report, LogLevel.I, module, LogCode.OK, content);
    }

    /**
     * 打印信息日志并上报
     *
     * @param module  模块名称
     * @param code    状态码
     * @param content 内容
     */
    public static void i(String module, int code, String content) {
        maxPrint(true, LogLevel.I, module, code, content);
    }

    /**
     * 打印信息日志并上报
     *
     * @param module  模块名称
     * @param content 内容
     */
    public static void i(String module, String content) {
        maxPrint(true, LogLevel.I, module, LogCode.OK, content);
    }

    /**
     * 打印调试日志
     *
     * @param report  是否上报
     * @param module  模块名称
     * @param code    状态码
     * @param content 内容
     */
    public static void d(boolean report, String module, int code, String content) {
        maxPrint(report, LogLevel.D, module, code, content);
    }
    /**
     * 打印调试日志
     *
     * @param report  是否上报
     * @param module  模块名称
     * @param content 内容
     */
    public static void d(boolean report, String module, String content) {
        maxPrint(report, LogLevel.D, module, LogCode.OK, content);
    }

    /**
     * 打印调试日志并上报
     *
     * @param module  模块名称
     * @param code    状态码
     * @param content 内容
     */
    public static void d(String module, int code, String content) {
        maxPrint(true, LogLevel.D, module, code, content);
    }

    /**
     * 打印调试日志并上报
     *
     * @param module  模块名称
     * @param content 内容
     */
    public static void d(String module, String content) {
        maxPrint(true, LogLevel.D, module, LogCode.OK, content);
    }

    /**
     * 打印详细日志
     *
     * @param report  是否上报
     * @param module  模块名称
     * @param code    状态码
     * @param content 内容
     */
    public static void v(boolean report, String module, int code, String content) {
        maxPrint(report, LogLevel.V, module, code, content);
    }

    /**
     * 打印详细日志
     *
     * @param report  是否上报
     * @param module  模块名称
     * @param content 内容
     */
    public static void v(boolean report, String module,String content) {
        maxPrint(report, LogLevel.V, module, LogCode.OK, content);
    }

    /**
     * 打印详细日志并上报
     *
     * @param module  模块名称
     * @param code    状态码
     * @param content 内容
     */
    public static void v(String module, int code, String content) {
        maxPrint(true, LogLevel.V, module, code, content);
    }

    /**
     * 打印详细日志并上报
     *
     * @param module  模块名称
     * @param content 内容
     */
    public static void v(String module, String content) {
        maxPrint(true, LogLevel.V, module, LogCode.OK, content);
    }

    /**
     * 适应最大长度打印
     *
     * @param level  日志级别
     * @param module 模块名称
     * @param code   状态码
     * @param content    信息
     */
    private static void maxPrint(boolean report, LogLevel level, String module, int code, String content) {
        if (report) {
            report(level, module, code, content);
        }
        if (content.length() > MAX_LENGTH) {
            int length = MAX_LENGTH + 1;
            String remain = content;
            int index = 0;
            while (length > MAX_LENGTH) {
                index++;
                typePrint(level, module + "[" + index + "]", " \n" + remain.substring(0, MAX_LENGTH));
                remain = remain.substring(MAX_LENGTH);
                length = remain.length();
            }
            if (length <= MAX_LENGTH) {
                index++;
                typePrint(level, module + "[" + index + "]", " \n" + remain);
            }
        } else {
            typePrint(level, module, content);
        }
    }

    /**
     * 打印各种类型
     *
     * @param level   日志级别
     * @param module  模块名称
     * @param content 信息
     */
    private static void typePrint(LogLevel level, String module, String content) {
        if (level == LogLevel.F) {
            Log.println(Log.ASSERT, module, content);
        }
        if (level == LogLevel.E) {
            Log.println(Log.ERROR, module, content);
        }
        if (level == LogLevel.W) {
            Log.println(Log.WARN, module, content);
        }
        if (level == LogLevel.I) {
            Log.println(Log.INFO, module, content);
        }
        if (level == LogLevel.D) {
            Log.println(Log.DEBUG, module, content);
        }
        if (level == LogLevel.V) {
            Log.println(Log.VERBOSE, module, content);
        }
    }

    /**
     * 上报日志
     *
     * @param level   日志级别
     * @param module  模块名称
     * @param code    状态码
     * @param content 信息
     */
    private static void report(LogLevel level, String module, int code, String content) {
        if (Link.mqtt() == null) {
            return;
        }
        if (Link.mqtt().api() == null) {
            return;
        }
        Link.mqtt().api().publishLog(level, module, String.valueOf(code), String.valueOf(System.currentTimeMillis()), content);
    }

}
