package androidx.iot.task;

import android.util.Log;

import androidx.iot.aiot.License;
import androidx.iot.aiot.OnMediaLicenseListener;
import androidx.iot.handler.LicenseHandler;
import androidx.iot.receiver.LicenseReceiver;
import androidx.iot.text.Reader;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;

/**
 * U盘授权文件读取
 */
public class LicenseMedia implements Runnable {

    private String TAG = LicenseReceiver.class.getSimpleName();
    /**
     * 授权路径
     */
    private String path;
    /**
     * 三元组信息传递者
     */
    private LicenseHandler handler;
    /**
     * 三元组监听
     */
    private ConcurrentHashMap<Long, OnMediaLicenseListener> map;

    public LicenseMedia(String path, LicenseHandler handler, ConcurrentHashMap<Long, OnMediaLicenseListener> map) {
        this.path = path;
        this.handler = handler;
        this.map = map;
    }

    @Override
    public void run() {
        File file = findLicense(new File(path));
        if (file == null || !file.exists()) {
            Log.e(TAG, License.acquire().getName() + " file does not exist");
        } else {
            Reader reader = new Reader(file);
            String content = reader.sync();
            License.acquire().fromJSON(content).granted();
            if (map != null) {
                for (Long key : map.keySet()) {
                    handler.send(content, map.get(key));
                }
            }
            reader.cancel();
        }
    }

    /**
     * 查找三元组文件
     *
     * @param file 路径
     * @return
     */
    private File findLicense(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null && files.length > 0) {
                for (File child : files) {
                    if (child.isDirectory()) {
                        findLicense(child);
                    }
                    String name = child.getName();
                    Log.d(TAG, name);
                    if (name.equals(License.acquire().getName())) {
                        Log.d(TAG, child.getAbsolutePath());
                        return child;
                    }
                }
            }
        }
        return null;
    }

}
