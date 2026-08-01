package android.mqtt.iot.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.mqtt.iot.utils.Apk

class BootReceiver : BroadcastReceiver() {

    private val TAG = "BootReceiver"

    override fun onReceive(context: Context?, intent: Intent?) {
        intent?.let {
            val action = it.action
            Log.i(TAG, "action:${action}")
            if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
                val packageName = context!!.packageName
                onBootCompleted(context, packageName)
            }
        }
    }


    private fun onBootCompleted(context: Context?, packageName: String) {
        Log.i(TAG, "[开机启动完成] $packageName")
        context?.let {
            Apk.boot(context, packageName)
        }
    }

}
