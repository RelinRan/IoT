package androidx.iot.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.iot.utils.Apk;

/**
 * 开关机
 * <p>
 * <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
 * <p>
 * <receiver
 *      android:name="androidx.lot.core.receiver.BootReceiver"
 *      android:enabled="true"
 *      android:exported="true"
 *      android:permission="android.permission.RECEIVE_BOOT_COMPLETED">
 *          <intent-filter>
 *          <action android:name="android.intent.action.BOOT_COMPLETED" />
 *          </intent-filter>
 * </receiver>
 */
public class BootReceiver extends BroadcastReceiver {

    private String TAG = BootReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            onBootCompleted(context);
        }
    }

    /**
     * 开机启动完成
     * @param context 上下文
     */
    protected void onBootCompleted(Context context){
        String packageName = context.getPackageName();
        Log.d(TAG, "开机启动完成 " + packageName);
        Apk.launch(context, packageName);
    }

}
