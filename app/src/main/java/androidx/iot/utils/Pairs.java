package androidx.iot.utils;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 属性值文件
 * 读取数据调用{@link #flush()}之后再调用{@link #getInt(String, int)}才能保证获取的是最新数据
 * 保存数据调用{@link #put(String, int)}之后需要调用{@link #apply(String)}才能保存数据
 * 所有操作完毕必须调用{@link #close()}释放资源，防止内存泄露。
 */
public class Pairs {

    /**
     * 项目名称
     */
    private String projectName;
    /**
     * 文件夹名称
     */
    private String dirName;
    /**
     * 文件名称
     */
    private String fileName;
    /**
     * 属性
     */
    private Properties properties;
    /**
     * 上下文
     */
    private Context context;
    /**
     * 文件
     */
    private File file;
    /**
     * 写入
     */
    private FileOutputStream os;
    /**
     * 读取
     */
    private FileInputStream is;

    private ExecutorService storeService;
    private Future storeFuture;
    private ExecutorService loadService;
    private Future loadFuture;

    /**
     * 属性文件构造
     *
     * @param context 上下文
     */
    public Pairs(Context context) {
        this(context, "IoT", "Properties", "config.properties");
    }

    /**
     * 属性文件构造
     *
     * @param context     上下文
     * @param projectName 项目名称
     */
    public Pairs(Context context, String projectName) {
        this(context, projectName, "Properties", "config.properties");
    }

    /**
     * 属性文件构造
     *
     * @param context     上下文
     * @param projectName 项目名称
     * @param dirName     文件夹名称
     */
    public Pairs(Context context, String projectName, String dirName) {
        this(context, projectName, dirName, "config.properties");
    }

    /**
     * 属性文件构造
     *
     * @param context     上下文
     * @param projectName 项目名称
     * @param dirName     文件夹名称
     * @param fileName    文件名称
     */
    public Pairs(Context context, String projectName, String dirName, String fileName) {
        this.context = context;
        this.projectName = projectName;
        this.dirName = dirName;
        this.fileName = fileName;
        properties = new Properties();
        file = getFile();
        storeService = Executors.newFixedThreadPool(5);
        loadService = Executors.newFixedThreadPool(5);
        flush();
    }

    /**
     * 获取文件夹
     *
     * @return
     */
    public File getDirectory() {
        return External.getStorageDir(context, projectName, dirName);
    }

    /**
     * 获取文件
     *
     * @return
     */
    public File getFile() {
        return new File(getDirectory(), fileName);
    }

    /**
     * 设置键值对
     *
     * @param key   键
     * @param value 值
     */
    public void put(String key, long value) {
        put(key, String.valueOf(value));
    }

    /**
     * 设置键值对
     *
     * @param key   键
     * @param value 值
     */
    public void put(String key, int value) {
        put(key, String.valueOf(value));
    }

    /**
     * 设置键值对
     *
     * @param key   键
     * @param value 值
     */
    public void put(String key, short value) {
        put(key, String.valueOf(value));
    }

    /**
     * 设置键值对
     *
     * @param key   键
     * @param value 值
     */
    public void put(String key, double value) {
        put(key, String.valueOf(value));
    }

    /**
     * 设置键值对
     *
     * @param key   键
     * @param value 值
     */
    public void put(String key, float value) {
        put(key, String.valueOf(value));
    }

    /**
     * 设置键值对
     *
     * @param key   键
     * @param value 值
     */
    public void put(String key, boolean value) {
        put(key, String.valueOf(value));
    }

    /**
     * 设置键值对
     *
     * @param key   键
     * @param value 值
     */
    public void put(String key, String value) {
        write().setProperty(key, value);
    }

    /**
     * 获取字符串
     *
     * @param key      键
     * @param defValue 默认值
     * @return
     */
    public String getString(String key, String defValue) {
        String value = properties.getProperty(key);
        return value == null || value.length() == 0 ? defValue : value;
    }

    /**
     * 获取int
     *
     * @param key      键
     * @param defValue 默认值
     * @return
     */
    public int getInt(String key, int defValue) {
        return Integer.parseInt(getString(key, String.valueOf(defValue)));
    }

    /**
     * 获取long
     *
     * @param key      键
     * @param defValue 默认值
     * @return
     */
    public long getLong(String key, long defValue) {
        return Long.parseLong(getString(key, String.valueOf(defValue)));
    }

    /**
     * 获取Float
     *
     * @param key      键
     * @param defValue 默认值
     * @return
     */
    public float getFloat(String key, float defValue) {
        return Float.parseFloat(getString(key, String.valueOf(defValue)));
    }

    /**
     * 获取Double
     *
     * @param key      键
     * @param defValue 默认值
     * @return
     */
    public double getDouble(String key, double defValue) {
        return Double.parseDouble(getString(key, String.valueOf(defValue)));
    }

    /**
     * 获取Short
     *
     * @param key      键
     * @param defValue 默认值
     * @return
     */
    public double getShort(String key, short defValue) {
        return Short.parseShort(getString(key, String.valueOf(defValue)));
    }

    /**
     * 获取Boolean
     *
     * @param key      键
     * @param defValue 默认值
     * @return
     */
    public boolean getBoolean(String key, boolean defValue) {
        return Boolean.parseBoolean(getString(key, String.valueOf(defValue)));
    }

    /**
     * 写入
     *
     * @return
     */
    private Properties write() {
        try {
            if (is != null) {
                is.close();
            }
            if (os == null) {
                if (!file.exists()) {
                    file = getFile();
                }
                os = new FileOutputStream(file);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return properties;
    }

    /**
     * 存储之前的操作
     *
     * @param comments 描述
     */
    private void store(String comments) {
        try {
            properties.store(os, comments);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 异步保存设置的信息
     *
     * @param comments 描述
     */
    public void apply(String comments) {
        storeFuture = storeService.submit(() -> {
            store(comments);
        });
    }

    /**
     * 异步保存设置的信息
     */
    public void apply() {
        apply("Properties File");
    }

    /**
     * 读取文件
     *
     * @return
     */
    private Properties load() {
        try {
            if (os != null) {
                os.close();
            }
            if (is == null) {
                if (!file.exists()) {
                    file = getFile();
                }
                is = new FileInputStream(file);
            }
            properties.load(is);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return properties;
    }

    /**
     * 异步加载刷新数据
     */
    public void flush() {
        loadFuture = loadService.submit(() -> {
            properties = load();
        });
    }

    /**
     * 清空数据
     */
    public Pairs clear() {
        if (file.exists()) {
            file.delete();
        }
        return this;
    }

    /**
     * 关闭文件操作
     */
    public void close() {
        try {
            if (os != null) {
                os.close();
                os = null;
            }
            if (is != null) {
                is.close();
                is = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 释放资源
     */
    public void release() {
        close();
        if (storeFuture != null) {
            storeFuture.cancel(true);
        }
        if (loadFuture != null) {
            loadFuture.cancel(true);
        }
    }

}
