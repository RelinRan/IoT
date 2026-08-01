package android.mqtt.iot.log

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.mqtt.iot.utils.Store
import android.mqtt.iot.utils.TimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit


object Orl {

    private lateinit var dir: File
    private var expiredDays: Int = 7
    private lateinit var scope: CoroutineScope
    private lateinit var appName: String
    private lateinit var company: String
    private var versionCode: Long = 0L
    private var versionName: String? = null


    @SuppressLint("SimpleDateFormat")
    fun initialize(
        context: Context,
        project: String = "Link",
        dirName: String = "orl",
        expiredDays: Int = 7,
        company: String = "深圳市安保医疗感控科技股份有限公司"
    ) {
        this.expiredDays = expiredDays
        this.company = company
        val packageManager: PackageManager = context.packageManager
        val applicationInfo: ApplicationInfo = context.applicationInfo
        appName = packageManager.getApplicationLabel(applicationInfo) as String
        try {
            val pi = packageManager.getPackageInfo(applicationInfo.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                versionCode = pi.longVersionCode
            }
            versionName = pi.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            throw RuntimeException(e)
        }
        dir = Store.getExternalCompatibleDir(context,project,dirName)
        scope = CoroutineScope(Dispatchers.IO)
    }


    fun log(
        date: Boolean = true,
        msg: String,
        append: Boolean = true,
        log: Boolean = false,
        priority: Int = Log.INFO,
        tag: String = "Orl",
    ) {
        if (!::dir.isInitialized) {
            Log.e(tag, "Operation dir is not initialize")
            return
        }
        if (log) {
            Log.println(priority, tag, msg)
        }
        scope.launch {
            val suffix = TimeUtils.formatMilliseconds("yyyyMMdd", System.currentTimeMillis())
            val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
            val now = System.currentTimeMillis()
            val expiredFiles = dir.listFiles()?.filter { file ->
                if (!file.name.startsWith("ORL")) return@filter false
                val dateStr = file.nameWithoutExtension.removePrefix("ORL")
                val fileDate = try {
                    dateFormat.parse(dateStr)?.time
                } catch (e: Exception) {
                    null
                }
                if (fileDate == null) return@filter false
                val diffDays = TimeUnit.MILLISECONDS.toDays(now - fileDate)
                diffDays > expiredDays
            }
            if (expiredFiles != null) {
                for (file in expiredFiles) {
                    file.delete()
                }
            }
            val text = File(dir, "ORL${suffix}.txt")
            if (!text.exists()) {
                val buffer = StringBuffer()
                buffer.append(company).append("\n")
                buffer.append("Application name:").append(appName).append("\n")
                buffer.append("Version Code:").append(versionCode).append("\n")
                buffer.append("Version Name:").append(versionName).append("\n")
                buffer.append("Brand:").append(Build.BRAND).append("\n")
                buffer.append("Release:Android ").append(Build.VERSION.RELEASE).append("\n")
                buffer.append("Device:").append(Build.DEVICE).append("\n")
                buffer.append("Product:").append(Build.PRODUCT).append("\n")
                buffer.append("Manufacturer:").append(Build.MANUFACTURER).append("\n")
                buffer.append("Version Code:").append(Build.DISPLAY).append("\n")
                text.writeText(buffer.toString())
            }
            val prefix =
                if (date) "${
                    TimeUtils.formatMilliseconds(
                        "yyyy-MM-dd HH:mm:ss:SSS",
                        System.currentTimeMillis()
                    )
                }  " else ""
            if (append) {
                text.appendText("${prefix}${msg}\n", charset = Charsets.UTF_8)
            } else {
                text.writeText("${prefix}${msg}\n", charset = Charsets.UTF_8)
            }
        }
    }

}
