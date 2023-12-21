package androidx.iot.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.iot.aiot.License;
import androidx.iot.aiot.OnLicenseListener;
import androidx.iot.text.Reader;
import androidx.iot.utils.Device;

import java.io.File;
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
    private TriplesMessage triplesMessage;
    /**
     * 三元组监听
     */
    private ConcurrentHashMap<Long, OnLicenseListener> triplesHashMap;
    private Context context;

    /**
     * 注册
     *
     * @param context
     */
    public void register(Context context) {
        this.context = context;
        triplesMessage = new TriplesMessage();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_REMOVED);
        filter.addDataScheme("file");
        context.registerReceiver(this, filter);
        //原先U盘已插入,如果没有注册，进行注册流程.
        boolean isLicensed = License.with(context).isLicensed();
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
        if (triplesMessage != null) {
            triplesMessage.removeCallbacksAndMessages(null);
            triplesMessage = null;
        }
        if (triplesHashMap != null) {
            triplesHashMap.clear();
            triplesHashMap = null;
        }
        context.unregisterReceiver(this);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;
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
        future = service.submit(() -> {
            File file = findLicense(new File(path));
            if (file == null || !file.exists()) {
                Log.e(TAG, License.with(context).getName() + " file does not exist");
            } else {
                Reader reader = new Reader(file);
                String content = reader.sync();
                License.with(context).fromJSON(content).granted();
                if (triplesHashMap != null) {
                    for (Long key : triplesHashMap.keySet()) {
                        triplesMessage.send(content, triplesHashMap.get(key));
                    }
                }
                reader.cancel();
            }
        });
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
                    if (name.equals(License.with(context).getName())) {
                        Log.d(TAG, child.getAbsolutePath());
                        return child;
                    }
                }
            }
        }
        return null;
    }

    private class TriplesMessage extends Handler {

        public void send(String triples, OnLicenseListener listener) {
            Message message = obtainMessage();
            message.what = 100;
            message.obj = listener;
            Bundle bundle = new Bundle();
            bundle.putString("triples", triples);
            message.setData(bundle);
            sendMessage(message);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 100:
                    OnLicenseListener listener = (OnLicenseListener) msg.obj;
                    String triples = msg.getData().getString("triples");
                    if (listener != null) {
                        listener.onLicense(triples);
                    }
                    break;
            }
        }
    }

    /**
     * 添加三元组信息监听
     *
     * @param onTriplesListener
     * @return 监听Id
     */
    public long addTriplesListener(OnLicenseListener onTriplesListener) {
        if (triplesHashMap == null) {
            triplesHashMap = new ConcurrentHashMap<>();
        }
        long tid = System.currentTimeMillis() + triplesHashMap.size() + 1;
        triplesHashMap.put(tid, onTriplesListener);
        return tid;
    }

    /**
     * 删除三元组信息监听
     *
     * @param id 监听id
     */
    public void remove(long id) {
        if (triplesHashMap != null) {
            triplesHashMap.remove(id);
        }
    }

    public void clear() {
        if (triplesHashMap != null) {
            triplesHashMap.clear();
        }
    }

}
