package androidx.iot.log

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.iot.utils.Store
import androidx.iot.utils.TimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * 操作运行日志
 */
object Orl {

    private lateinit var dir: File
    private var expiredDays: Int = 7
    private lateinit var scope: CoroutineScope
    private lateinit var appName: String
    private lateinit var company: String
    private var versionCode: Long = 0L
    private var versionName: String? = null


    /**
     * 初始化
     * @param context 上下文
     * @param dirName 文件夹
     * @param expiredDays 超期天数
     * @param company 公司
     */

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

    /**
     * 记录日志
     * @param date 日期
     * @param msg 内容
     * @param append 追加写入
     * @param log 是否log
     * @param priority log优先权
     * @param tag log标识
     */
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
            // 过期文件筛选：解析文件名中的日期（ORL20260701.txt → 20260701），按真实天数差判断
            val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
            val now = System.currentTimeMillis()
            val expiredFiles = dir.listFiles()?.filter { file ->
                // 仅处理 ORL 前缀的日志文件
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
                //项目名称
                buffer.append("Application name:").append(appName).append("\n")
                //项目版本号
                buffer.append("Version Code:").append(versionCode).append("\n")
                //项目版本名
                buffer.append("Version Name:").append(versionName).append("\n")
                //手机品牌
                buffer.append("Brand:").append(Build.BRAND).append("\n")
                //SDK版本
                buffer.append("Release:Android ").append(Build.VERSION.RELEASE).append("\n")
                //设备名
                buffer.append("Device:").append(Build.DEVICE).append("\n")
                //产品名
                buffer.append("Product:").append(Build.PRODUCT).append("\n")
                //制造商
                buffer.append("Manufacturer:").append(Build.MANUFACTURER).append("\n")
                //手机版本
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
