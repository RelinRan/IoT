package androidx.iot.utils;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 属性值文件
 * 读取数据调用{@link #load()} ()}之后再调用{@link #getInt(String, int)}才能保证获取的是最新数据
 * 保存数据调用{@link #put(String, int)}之后需要调用{@link #store(String)}才能保存数据
 * 所有操作完毕必须调用{@link #close()}释放资源，防止内存泄露。
 */
public class Pairs {

    private String TAG = Pairs.class.getSimpleName();
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
    /**
     * 任务
     */
    private ExecutorService service;
    private PairsStore pairsStore;
    private PairsLoad pairsLoad;
    /**
     * 文件操作对象
     */
    private static Pairs instance;

    /**
     * 初始化
     *
     * @param context 上下文
     * @return
     */
    public static Pairs initialize(Context context) {
        if (instance == null) {
            synchronized (Pairs.class) {
                if (instance == null) {
                    instance = new Pairs(context);
                }
            }
        }
        return instance;
    }

    /**
     * 初始化
     *
     * @param context     上下文
     * @param projectName 项目名称
     * @return
     */
    public static Pairs initialize(Context context, String projectName) {
        if (instance == null) {
            synchronized (Pairs.class) {
                if (instance == null) {
                    instance = new Pairs(context, projectName);
                }
            }
        }
        return instance;
    }

    /**
     * 初始化
     *
     * @param context     上下文
     * @param projectName 项目名称
     * @param dirName     文件夹名称
     * @return
     */
    public static Pairs initialize(Context context, String projectName, String dirName) {
        if (instance == null) {
            synchronized (Pairs.class) {
                if (instance == null) {
                    instance = new Pairs(context, projectName, dirName);
                }
            }
        }
        return instance;
    }

    /**
     * 初始化
     *
     * @param context     上下文
     * @param projectName 项目名称
     * @param dirName     文件夹名称
     * @param fileName    文件名称
     * @return
     */
    public static Pairs initialize(Context context, String projectName, String dirName, String fileName) {
        if (instance == null) {
            synchronized (Pairs.class) {
                if (instance == null) {
                    instance = new Pairs(context, projectName, dirName, fileName);
                }
            }
        }
        return instance;
    }

    /**
     * 获取操作对象
     *
     * @return
     */
    public static Pairs acquire() {
        if (instance == null) {
            throw new RuntimeException(Pairs.class.getSimpleName() + " not initialize.");
        }
        return instance;
    }

    /**
     * 属性文件构造
     *
     * @param context 上下文
     */
    private Pairs(Context context) {
        this(context, "IoT", "Properties", "config.properties");
    }

    /**
     * 属性文件构造
     *
     * @param context     上下文
     * @param projectName 项目名称
     */
    private Pairs(Context context, String projectName) {
        this(context, projectName, "Properties", "config.properties");
    }

    /**
     * 属性文件构造
     *
     * @param context     上下文
     * @param projectName 项目名称
     * @param dirName     文件夹名称
     */
    private Pairs(Context context, String projectName, String dirName) {
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
    private Pairs(Context context, String projectName, String dirName, String fileName) {
        this.context = context;
        this.projectName = projectName;
        this.dirName = dirName;
        this.fileName = fileName;
        service = Executors.newFixedThreadPool(1);
        properties = new Properties();
        file = getFile();
        load();
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
        if (file != null && file.exists()) {
            return file;
        }
        File properties = new File(getDirectory(), fileName);
        if (!properties.exists()) {
            try {
                properties.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return properties;
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
        properties.setProperty(key, value);
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
     * 同步存储之前的操作
     *
     * @param comments 描述
     */
    public void store(String comments) {
        try {
            if (is != null) {
                is.close();
                is = null;
            }
            file = getFile();
            if (os == null && file != null && file.exists()) {
                os = new FileOutputStream(file);
            }
            if (properties != null && os != null) {
                properties.store(os, comments);
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 同步保存设置的信息
     */
    public void store() {
        store("Properties File");
    }


    /**
     * 异步保存数据
     *
     * @param comments 说明文字
     */
    public void commit(String comments) {
        if (pairsStore == null) {
            pairsStore = new PairsStore(this);
        }
        pairsStore.setComments(comments);
        service.submit(pairsStore);
    }

    /**
     * 异步保存数据
     */
    public void commit() {
        commit("Properties File");
    }

    /**
     * 同步读取文件
     *
     * @return
     */
    public void load() {
        try {
            if (os != null) {
                os.close();
                os = null;
            }
            file = getFile();
            if (is == null && file != null && file.exists()) {
                is = new FileInputStream(file);
            }
            if (properties != null && file != null && file.exists()) {
                properties.load(is);
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 异步获取配置文件内容
     */
    public void fetch() {
        if (pairsLoad == null) {
            pairsLoad = new PairsLoad(this);
        }
        service.submit(pairsLoad);
    }

    /**
     * 清空数据并异步更新文件
     */
    public Pairs clear() {
        if (properties != null) {
            properties.clear();
        }
        commit();
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
            throw new RuntimeException(e);
        }
    }

    /**
     * 释放资源
     */
    public void release() {
        close();
    }

}
