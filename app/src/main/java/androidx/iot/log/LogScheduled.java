package androidx.iot.log;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 日志定时任务
 */
public class LogScheduled {

    private File folder;
    /**
     * 文件前缀名称
     */
    private String prefix;
    /**
     * 后缀
     */
    private String suffix;
    /**
     * 有效期
     */
    private int exp;
    /**
     * 有效期单位
     */
    private TimeUnit unit;
    /**
     * 检查定时任务
     */
    private ScheduledExecutorService service;
    private Future future;
    /**
     * 检查第一次延时
     */
    private int initialDelay = 0;
    /**
     * 检查时段
     */
    private int period = 7;
    /**
     * 日期格式
     */
    private SimpleDateFormat dateFormat;

    /**
     * 构造
     *
     * @param folder 文件夹
     * @param prefix 前缀
     * @param suffix 后缀
     * @param exp    有效期
     * @param unit   时间单位
     */
    public LogScheduled(File folder, String prefix, String suffix, int exp, TimeUnit unit) {
        this.folder = folder;
        this.prefix = prefix;
        this.suffix = suffix;
        this.exp = exp;
        this.unit = unit;
        dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        service = Executors.newScheduledThreadPool(1);
    }

    /**
     * 设置日期格式
     *
     * @param dateFormat
     */
    public void setDateFormat(SimpleDateFormat dateFormat) {
        this.dateFormat = dateFormat;
    }

    /**
     * 设置初始化的时候延迟
     *
     * @param initialDelay
     */
    public void setInitialDelay(int initialDelay) {
        this.initialDelay = initialDelay;
    }

    /**
     * 设置间隔时间
     *
     * @param period
     */
    public void setPeriod(int period) {
        this.period = period;
    }

    /**
     * 是否过期
     *
     * @param diff
     * @return
     */
    protected boolean isExpired(long diff) {
        if (unit == TimeUnit.MILLISECONDS) {
            return diff > exp;
        } else if (unit == TimeUnit.SECONDS) {
            return diff / 1000 > exp;
        } else if (unit == TimeUnit.MINUTES) {
            return diff / 1000 / 60 > exp;
        } else if (unit == TimeUnit.HOURS) {
            return diff / 1000 / 60 / 60 > exp;
        } else if (unit == TimeUnit.DAYS) {
            return diff / 1000 / 60 / 60 / 24 > exp;
        } else {
            System.err.println("Time Unit is not support.");
        }
        return false;
    }

    /**
     * 扫描文件
     *
     * @param file
     */
    private void scanFiles(File file) {
        if (file.isDirectory()) {
            for (File item : file.listFiles()) {
                if (item.isDirectory()) {
                    scanFiles(item);
                } else {
                    deleteExpiredFile(item);
                }
            }
        } else {
            deleteExpiredFile(file);
        }
    }

    /**
     * 删除过期文件
     *
     * @param file
     */
    private void deleteExpiredFile(File file) {
        String filename = file.getName();
        if (filename.startsWith(prefix) && filename.endsWith(suffix)) {
            String date = filename.replace(prefix, "").replace(suffix, "");
            try {
                long time = dateFormat.parse(date).getTime();
                long now = System.currentTimeMillis();
                long diff = now - time;
                if (isExpired(diff)) {
                    file.deleteOnExit();
                }
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * 开始
     */
    public void start() {
        cancel();
        future = service.scheduleAtFixedRate(() -> {
            scanFiles(folder);
        }, initialDelay, period, unit);
    }

    /**
     * 取消
     */
    public void cancel() {
        if (future != null) {
            future.cancel(true);
        }
    }

}
