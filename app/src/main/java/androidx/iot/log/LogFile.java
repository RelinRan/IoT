package androidx.iot.log;

import android.content.Context;
import android.os.Build;
import android.os.Environment;

import androidx.iot.text.OnReadListener;
import androidx.iot.text.Reader;
import androidx.iot.text.Writer;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * 日志文件
 */
public class LogFile {

    /**
     * 根路径
     */
    private File root;
    /**
     * 项目名称
     */
    private String project;
    /**
     * 文件夹名称
     */
    private String dir;
    /**
     * 文件前缀名称
     */
    private String prefix;
    /**
     * 后缀
     */
    private String suffix;
    /**
     * 到期时间
     */
    private int exp;
    /**
     * 有效期单位
     */
    private TimeUnit timeUnit;
    /**
     * 时间格式
     */
    private SimpleDateFormat dateFormat;
    /**
     * 写入
     */
    private Writer writer;
    /**
     * 读取
     */
    private Reader reader;
    /**
     * 文件运维
     */
    private LogScheduled scheduled;
    /**
     * 检查第一次延时
     */
    private int initialDelay = 0;
    /**
     * 检查时段
     */
    private int period = 8;
    /**
     * 上下文
     */
    private Context context;

    /**
     * 上下文
     *
     * @return
     */
    public Context getContext() {
        return context;
    }

    /**
     * 日志文件
     */
    public LogFile(Context context) {
        initParams(context,"IoT", "Log", "log");
    }

    /**
     * 日志文件
     *
     * @param project 项目名称
     * @param dir     文件夹
     * @param prefix  文件前缀
     */
    public LogFile(Context context,String project, String dir, String prefix) {
        initParams(context,project, dir, prefix);
    }

    /**
     * 初始化操作
     *
     * @param project 项目名称
     * @param dir     文件夹
     * @param prefix  文件前缀
     */
    private void initParams(Context context,String project, String dir, String prefix) {
        this.context = context;
        dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            setRoot(context.getExternalFilesDir(null));
        } else {
            setRoot(Environment.getExternalStorageDirectory());
        }
        setFolder(project, dir);
        setPrefix(prefix);
        setSuffix(".txt");
        setTimeUnit(TimeUnit.HOURS);
        setExp(7 * 24);
        setInitialDelay(0);
        setPeriod(8);
    }

    /**
     * 设置初始第一次延迟时间
     *
     * @param initialDelay 延迟时间
     */
    public void setInitialDelay(int initialDelay) {
        this.initialDelay = initialDelay;
    }

    /**
     * 设置文件检查周期
     *
     * @param period
     */
    public void setPeriod(int period) {
        this.period = period;
    }

    /**
     * 设置根文件夹
     *
     * @param root
     */
    public void setRoot(File root) {
        this.root = root;
    }

    /**
     * 设置文件夹
     *
     * @param project 项目名称
     * @param dir     文件夹名称
     */
    public void setFolder(String project, String dir) {
        this.project = project;
        this.dir = dir;
    }

    /**
     * 获取文件夹
     */
    public File getFolder() {
        StringBuilder builder = new StringBuilder();
        builder.append(root);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            builder.append(File.separator);
            builder.append(project);
        }
        builder.append(File.separator);
        builder.append(dir);
        File folder = new File(builder.toString());
        if (!folder.exists()) {
            folder.mkdirs();
        }
        return folder;
    }

    /**
     * 清空日志文件
     */
    public void clear() {
        File folder = getFolder();
        File[] list = folder.listFiles();
        int size = list == null ? 0 : list.length;
        for (int i = 0; i < size; i++) {
            if (list[i].exists()) {
                list[i].delete();
            }
        }
    }

    /**
     * 文件前缀名
     *
     * @param prefix
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    /**
     * 设置后缀
     *
     * @param suffix
     */
    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    /**
     * 获取文件名
     *
     * @return
     */
    public String getFilename() {
        String data = getFormatDate("yyyy-MM-dd");
        StringBuilder builder = new StringBuilder();
        builder.append(prefix).append(data).append(suffix);
        return builder.toString();
    }

    /**
     * 获取操作文件
     *
     * @return
     */
    public File getFile() {
        return new File(getFolder(), getFilename());
    }

    /**
     * 设置时间单位
     *
     * @param timeUnit 时间单位
     */
    public void setTimeUnit(TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
    }

    /**
     * 设置有效期
     *
     * @param exp 到期时间
     */
    public void setExp(int exp) {
        this.exp = exp;
    }

    /**
     * 获取格式化时间
     *
     * @param pattern 模式
     * @return
     */
    public String getFormatDate(String pattern) {
        if (pattern != null) {
            dateFormat.applyPattern(pattern);
        }
        return dateFormat.format(new Date());
    }

    /**
     * 追加写入
     *
     * @param content 内容
     */
    public void write(String content) {
        write(content, true);
    }

    /**
     * 写入
     *
     * @param content 内容
     * @param append  是否追加写入
     */
    public void write(byte[] content, boolean append) {
        write(new String(content), append);
    }

    /**
     * 写入
     *
     * @param content 内容
     * @param append  是否追加写入
     */
    public void write(String content, boolean append) {
        StringBuilder builder = new StringBuilder();
        builder.append(getFormatDate("yyyy-MM-dd HH:mm:ss.SSS"));
        builder.append("  ");
        builder.append(content);
        builder.append("\n");
        if (writer == null) {
            writer = new Writer(getFile());
        }
        writer.async(builder.toString(), append, null);
    }

    /**
     * 同步读取
     *
     * @return
     */
    public String read() {
        if (reader == null) {
            reader = new Reader(getFile());
        }
        return reader.sync();
    }

    /**
     * 异步读取
     *
     * @param listener
     */
    public void read(OnReadListener listener) {
        if (reader == null) {
            reader = new Reader(getFile());
        }
        reader.async(listener);
    }

    /**
     * 开始文件维护
     */
    public void startScheduled() {
        if (scheduled != null) {
            return;
        }
        scheduled = new LogScheduled(getFolder(), prefix, suffix, exp, timeUnit);
        scheduled.setPeriod(period);
        scheduled.setInitialDelay(initialDelay);
        scheduled.start();
    }

    /**
     * 停止文件维护
     */
    public void stopScheduled() {
        if (scheduled != null) {
            scheduled.cancel();
            scheduled = null;
        }
    }

    /**
     * 取消
     */
    public void cancel() {
        if (writer != null) {
            writer.cancel();
        }
        if (reader != null) {
            reader.cancel();
        }
        stopScheduled();
    }

}
