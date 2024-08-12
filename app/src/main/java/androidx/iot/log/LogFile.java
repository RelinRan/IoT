package androidx.iot.log;

import android.content.Context;

import androidx.iot.text.OnReadListener;
import androidx.iot.text.Reader;
import androidx.iot.text.Writer;
import androidx.iot.utils.Device;
import androidx.iot.utils.External;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * 日志文件
 */
public class LogFile {

    /**
     * 上下文
     */
    private Context context;
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
     * 日期是否可用
     */
    private boolean supportScheduled = true;
    private StringBuilder builder;


    /**
     * 获取上下文
     *
     * @return
     */
    public Context getContext() {
        return context;
    }

    /**
     * 日志文件
     *
     * @param context 上下文
     */
    public LogFile(Context context) {
        initializeParameters(context, "IoT", "Log", "log");
    }

    /**
     * 日志文件
     *
     * @param context 上下文
     * @param project 项目名称
     * @param dir     文件夹
     * @param prefix  文件前缀
     */
    public LogFile(Context context, String project, String dir, String prefix) {
        initializeParameters(context, project, dir, prefix);
    }

    /**
     * 初始化操作
     *
     * @param project 项目名称
     * @param dir     文件夹
     * @param prefix  文件前缀
     */
    protected void initializeParameters(Context context, String project, String dir, String prefix) {
        this.context = context;
        dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        setRoot(External.getStorageDir(context));
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
     * 设置是否支持定时任务
     *
     * @param supportScheduled
     */
    public void setSupportScheduled(boolean supportScheduled) {
        this.supportScheduled = supportScheduled;
    }

    /**
     * 是否支持定时任务
     *
     * @return
     */
    public boolean isSupportScheduled() {
        return supportScheduled;
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

    private StringBuilder getBuilder(){
        if (builder==null){
            builder = new StringBuilder();
        }else{
            builder.setLength(0);
        }
        return builder;
    }

    /**
     * 获取文件夹
     */
    public File getFolder() {
        StringBuilder builder = getBuilder();
        builder.append(root);
        builder.append(File.separator);
        builder.append(project);
        builder.append(File.separator);
        builder.append(dir);
        File folder = new File(builder.toString());
        if (!folder.exists()) {
            folder.mkdirs();
        }
        builder.setLength(0);
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
        StringBuilder builder = getBuilder();
        builder.append(prefix);
        if (isSupportScheduled()) {
            String data = getFormatDate("yyyy-MM-dd");
            builder.append(data);
        }
        builder.append(suffix);
        String name = builder.toString();
        builder.setLength(0);
        return name;
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
        File file = getFile();
        StringBuilder builder = getBuilder();
        if (!file.exists()) {
            builder.append(Device.getHeader(getContext()));
        }
        if (isSupportScheduled()) {
            builder.append(getFormatDate("yyyy-MM-dd HH:mm:ss.SSS"));
            builder.append("    ");
        }
        builder.append(content);
        builder.append("\n");
        if (writer == null) {
            writer = new Writer(file);
        }
        String path = builder.toString();
        builder.setLength(0);
        writer.async(path, append, null);
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
        //不支持定时任务检查
        if (supportScheduled == false) {
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
     * 计算文件夹大小
     *
     * @param folder 文件夹
     * @return
     */
    public long calculateFolderSize(File folder) {
        if (!folder.isDirectory()) {
            return folder.length();
        }
        long totalSize = 0;
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    totalSize += file.length();
                } else {
                    totalSize += calculateFolderSize(file);
                }
            }
        }
        return totalSize;
    }

    /**
     * 删除文件夹
     *
     * @param folder 文件夹
     */
    public void deleteFolder(File folder) {
        if (folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteFolder(file);
                }
            }
        }
        folder.delete();
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
