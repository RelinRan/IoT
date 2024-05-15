package androidx.iot.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import androidx.iot.aiot.License;
import androidx.iot.aiot.OnMediaLicenseListener;
import androidx.iot.handler.LicenseHandler;
import androidx.iot.task.LicenseMedia;
import androidx.iot.utils.Device;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * U盘三元组信息监听
 */
public class LicenseReceiver extends BroadcastReceiver {

    /**
     * 日志标识
     */
    private String TAG = LicenseReceiver.class.getSimpleName();
    /**
     * 文件操作线程池
     */
    private ExecutorService service;
    /**
     * 文件操作线程池过程
     */
    private Future future;
    /**
     * 三元组信息传递者
     */
    private LicenseHandler handler;
    /**
     * 三元组监听
     */
    private ConcurrentHashMap<Long, OnMediaLicenseListener> map;
    /**
     * U盘授权读取
     */
    private LicenseMedia licenseMedia;

    /**
     * 注册
     *
     * @param context
     */
    public void register(Context context) {
        handler = new LicenseHandler();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_REMOVED);
        filter.addDataScheme("file");
        context.registerReceiver(this, filter);
        //原先U盘已插入,如果没有注册，进行注册流程.
        boolean isLicensed = License.acquire().isLicensed();
        Log.d(TAG, "register licensed:" + isLicensed);
        if (!isLicensed) {
            List<String> paths = Device.getRemovableStorageVolumePath(context);
            for (String path : paths) {
                Log.d(TAG, "path:" + path);
                readLicense(path);
            }
        }
    }

    /**
     * 解注
     *
     * @param context
     */
    public void unregister(Context context) {
        Log.i(TAG, "unregister");
        if (future != null) {
            future.cancel(true);
        }
        if (handler != null) {
            handler.remove();
            handler.removeCallbacksAndMessages(null);
            handler = null;
        }
        if (map != null) {
            map.clear();
            map = null;
        }
        context.unregisterReceiver(this);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.i(TAG, "action = " + action);
        if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
            onMediaMounted(context, intent);
        }
        if (action.equals(Intent.ACTION_MEDIA_REMOVED)) {
            onMediaRemoved(context, intent);
        }
    }

    /**
     * U盘挂载成功
     *
     * @param context 上下文
     * @param intent  意图
     */
    protected void onMediaMounted(Context context, Intent intent) {
        String path = intent.getData().getPath();
        Log.d(TAG, "USB mounted path = " + path);
        readLicense(path);
    }

    /**
     * U盘移除
     *
     * @param context 上下文
     * @param intent  意图
     */
    protected void onMediaRemoved(Context context, Intent intent) {
        Log.d(TAG, "USB removed");
    }

    /**
     * 拷贝三元组信息
     *
     * @param path
     */
    private void readLicense(String path) {
        if (service == null) {
            service = Executors.newCachedThreadPool();
        }
        if (future != null) {
            future.cancel(true);
        }
        if (licenseMedia == null) {
            licenseMedia = new LicenseMedia(path, handler, map);
        }
        future = service.submit(licenseMedia);
    }

    /**
     * 添加三元组信息监听
     *
     * @param listener
     * @return 监听Id
     */
    public long addMediaLicenseGrantedListener(OnMediaLicenseListener listener) {
        if (map == null) {
            map = new ConcurrentHashMap<>();
        }
        long tid = System.currentTimeMillis() + map.size() + 1;
        map.put(tid, listener);
        return tid;
    }

    /**
     * 删除三元组信息监听
     *
     * @param id 监听id
     */
    public void remove(long id) {
        if (map != null) {
            map.remove(id);
        }
    }

    public void clear() {
        if (map != null) {
            map.clear();
        }
    }

}
