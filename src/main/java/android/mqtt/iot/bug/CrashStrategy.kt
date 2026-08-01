package android.mqtt.iot.bug

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.mqtt.iot.utils.Store
import android.mqtt.iot.utils.TimeUtils
import com.tencent.bugly.crashreport.CrashReport
import java.io.File


class CrashStrategy(val context: Context,val project: String = "Alink", val dir: String = "crash", val limit:Int = 2000) : CrashReport.CrashHandleCallback() {

    private val TAG = "CrashStrategy"

    private fun crashDir(): File {
        return Store.getExternalCompatibleDir(context,project,dir)
    }

    override fun onCrashHandleStart(
        p0: Int,
        p1: String?,
        p2: String?,
        p3: String?
    ): MutableMap<String, String> {
        val crashType = p0
        val errorType = p1 ?: ""
        val errorMessage = p2 ?: ""
        val errorStack = p3 ?: ""
        val dir = crashDir()
        clearCrash(dir,limit)
        saveCrash(dir,crashType, errorType, errorMessage, errorStack)
        val result = super.onCrashHandleStart(p0, p1, p2, p3)
        return result?: mutableMapOf()
    }


    private fun clearCrash(dir:File,limit: Int) {
        if (dir.exists() && dir.isDirectory) {
            val txtFiles = dir.listFiles { file ->
                file.isFile && file.extension.equals(
                    "txt",
                    ignoreCase = true
                )
            }
            if (txtFiles != null) {
                if (txtFiles.size > limit) {
                    clearDir(dir)
                    Log.i(TAG, "crash txt文件超过${limit}个，已清空所有文件")
                }
            }
        } else {
            Log.e(TAG, "指定的目录不存在或不是一个有效的目录")
        }
    }


    private fun clearDir(directory: File) {
        if (directory.exists() && directory.isDirectory) {
            val files = directory.listFiles()
            if (files != null) {
                for (file in files) {
                    if (file.isFile) {
                        file.delete()
                    }
                }
            }
        }
    }


    private fun saveCrash(
        dir:File,
        crashType: Int,
        errorType: String,
        errorMessage: String,
        errorStack: String
    ) {
        val packageManager: PackageManager = context.packageManager
        val applicationInfo: ApplicationInfo = context.applicationInfo
        val appName = packageManager.getApplicationLabel(applicationInfo) as String
        val buffer = StringBuffer()
        val time = TimeUtils.formatMilliseconds(System.currentTimeMillis())
        buffer.append("Crash time:").append(time).append("\n")
        buffer.append("Application name:").append(appName).append("\n")
        try {
            val pi = packageManager.getPackageInfo(applicationInfo.packageName, 0)
            buffer.append("Version Code:").append(pi.versionCode).append("\n")
            buffer.append("Version Name:").append(pi.versionName).append("\n")
        } catch (e: PackageManager.NameNotFoundException) {
            throw RuntimeException(e)
        }
        buffer.append("Brand:").append(Build.BRAND).append("\n")
        buffer.append("Release:Android ").append(Build.VERSION.RELEASE).append("\n")
        buffer.append("Device:").append(Build.DEVICE).append("\n")
        buffer.append("Product:").append(Build.PRODUCT).append("\n")
        buffer.append("Manufacturer:").append(Build.MANUFACTURER).append("\n")
        buffer.append("Version Code:").append(Build.DISPLAY).append("\n")
        buffer.append("Fingerprint:").append(Build.FINGERPRINT).append("\n")

        buffer.append("Crash type:").append(crashType)
        buffer.append("Error type:").append(errorType).append("\n")
        buffer.append("Error message:").append(errorMessage).append("\n")
        buffer.append("Error stack:").append(errorStack).append("\n")

        val name = "crash${
            TimeUtils.formatMilliseconds(
                "yyyyMMddHHmmss",
                System.currentTimeMillis()
            )
        }.txt"
        val file = File(dir, name)
        Log.i(TAG,"[保存错误信息]")
        file.writeText(buffer.toString(), charset = Charsets.UTF_8)
    }

}
