package androidx.iot.utils

import android.app.Activity
import android.app.ActivityManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.OutputStream


/**
 * Apk
 */
object Apk {
    private val TAG: String = Apk::class.java.simpleName

    /**
     * Android7.0 FileProvider文件授权后缀
     */
    const val AUTHORITY_SUFFIX: String = ".fileProvider"

    /**
     * 获取应用名称
     *
     * @param context 上下文
     * @return
     */
    fun getApplicationName(context: Context): String {
        val packageManager = context.packageManager
        val applicationInfo = context.applicationInfo
        return packageManager.getApplicationLabel(applicationInfo).toString()
    }

    /**
     * @param context 上下文
     * @return 版本名字
     */
    fun getVersionName(context: Context): String? {
        try {
            val packageInfo =
                context.packageManager.getPackageInfo(context.applicationContext.packageName, 0)
            return packageInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * @param context 上下文
     * @return 版本代码
     */
    fun getVersionCode(context: Context): Int {
        try {
            val packageInfo =
                context.packageManager.getPackageInfo(context.applicationContext.packageName, 0)
            return packageInfo.versionCode
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return 0
    }

    /**
     * 比较版本号是否需要升级
     *
     * @param context    上下文
     * @param apiVersion 接口版本
     * @return 是否需要升级
     */
    fun isNewVersion(context: Context, apiVersion: String): Boolean {
        val localVersion = getVersionName(context)
        val localItems =
            localVersion!!.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val apiItems =
            apiVersion.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val localIntItems = toIntArray(localItems)
        val apiIntItems = toIntArray(apiItems)
        if (localIntItems.size >= apiIntItems.size) {
            for (i in apiIntItems.indices) {
                if (localIntItems[i] < apiIntItems[i]) {
                    return true
                }
                if (localIntItems[i] > apiIntItems[i]) {
                    return false
                }
            }
        } else {
            for (i in localIntItems.indices) {
                if (localIntItems[i] < apiIntItems[i]) {
                    return true
                }
                if (localIntItems[i] > apiIntItems[i]) {
                    return false
                }
            }
            val lastIndex = localIntItems.size - 1
            if (localIntItems[lastIndex] < apiIntItems[lastIndex]) {
                return true
            }
            if (localIntItems[lastIndex] == apiIntItems[lastIndex]) {
                return true
            }
        }
        return false
    }

    /**
     * 比较版本号是否需要升级
     *
     * @param localVersion 本地版本
     * @param apiVersion 接口版本
     * @return 是否需要升级
     */
    fun isNewVersion(localVersion: String, apiVersion: String): Boolean {
        val localItems =
            localVersion!!.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val apiItems =
            apiVersion.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val localIntItems = toIntArray(localItems)
        val apiIntItems = toIntArray(apiItems)
        if (localIntItems.size >= apiIntItems.size) {
            for (i in apiIntItems.indices) {
                if (localIntItems[i] < apiIntItems[i]) {
                    return true
                }
                if (localIntItems[i] > apiIntItems[i]) {
                    return false
                }
            }
        } else {
            for (i in localIntItems.indices) {
                if (localIntItems[i] < apiIntItems[i]) {
                    return true
                }
                if (localIntItems[i] > apiIntItems[i]) {
                    return false
                }
            }
            val lastIndex = localIntItems.size - 1
            if (localIntItems[lastIndex] < apiIntItems[lastIndex]) {
                return true
            }
            if (localIntItems[lastIndex] == apiIntItems[lastIndex]) {
                return true
            }
        }
        return false
    }

    /**
     * 字符数组转int数组
     *
     * @param array 字符数组
     * @return int数组
     */
    private fun toIntArray(array: Array<String>): IntArray {
        val items = IntArray(array.size)
        for (i in items.indices) {
            val value = array[i]
            items[i] = filterHorizontalSymbol(value)
        }
        return items
    }

    /**
     * 过滤横向符号
     *
     * @param value 数据
     * @return
     */
    private fun filterHorizontalSymbol(value: String): Int {
        val builder = StringBuilder()
        val chars = value.toCharArray()
        for (j in chars.indices) {
            val charItem = chars[j]
            if (isNumeric(charItem.toString())) {
                builder.append(charItem)
            }
        }
        return if (builder.length == 0) 0 else builder.toString().toInt()
    }

    /**
     * 是否是数字
     *
     * @param value
     * @return
     */
    private fun isNumeric(value: String): Boolean {
        return value.matches("-?\\d+(\\.\\d+)?".toRegex())
    }

    /**
     * 文件路径获取Uri
     *
     * @param context   上下文
     * @param path      apk路径
     * @param authority Android 7.0以上FileProvider
     * @return 文件Uri
     */
    fun getUriForPath(context: Context?, path: String?, authority: String?): Uri {
        val file = File(path)
        if (!file.exists()) {
            Log.e(TAG, "file is not exist path = " + file.absolutePath)
        }
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(context!!, authority!!, file)
        } else {
            Uri.fromFile(File(path))
        }
        return uri
    }

    /**
     * 文件路径获取Uri
     *
     * @param context 上下文
     * @param path    apk路径
     * @return 文件Uri
     */
    fun getUriForPath(context: Context, path: String?): Uri {
        return getUriForPath(
            context,
            path,
            context.applicationContext.packageName + AUTHORITY_SUFFIX
        )
    }

    /**
     * 普通安装apk
     *
     * @param context   上下文
     * @param path      apk路径
     * @param authority Android 7.0以上FileProvider
     */
    /**
     * 普通安装apk
     *
     * @param context 上下文
     * @param path    apk路径
     */
    @JvmOverloads
    fun install(
        context: Context,
        path: String?,
        authority: String? = context.applicationContext.packageName + AUTHORITY_SUFFIX
    ) {
        val intent = Intent(Intent.ACTION_VIEW)
        val uri = getUriForPath(context, path, authority)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        intent.setDataAndType(uri, "application/vnd.android.package-archive")
        intent.putExtra("IMPLUS_INSTALL", "SILENT_INSTALL") //RK3568R板子
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /**
     * 自动点击确认安装。
     * 此安装方法显示安装页面，自动点击安装。安装结果在onActivityResult处理。
     * 权限 - <uses-permission android:name="android.permission.INSTALL_PACKAGES"></uses-permission>
     *
     * @param activity    当前页面
     * @param path        apk路径
     * @param authority   Android 7.0以上FileProvider
     * @param requestCode 请求代码
     */
    fun installPackage(activity: Activity, path: String?, authority: String?, requestCode: Int) {
        val apkUri = getUriForPath(activity, path, authority)
        val intent = Intent(Intent.ACTION_INSTALL_PACKAGE)
        intent.setData(apkUri)
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
        intent.putExtra(Intent.EXTRA_RETURN_RESULT, true)
        activity.startActivityForResult(intent, requestCode)
    }

    /**
     * 自动点击确认安装。
     * 此安装方法显示安装页面，自动点击安装。安装结果在onActivityResult处理。
     * 权限 - <uses-permission android:name="android.permission.INSTALL_PACKAGES"></uses-permission>
     *
     * @param activity    当前页面
     * @param path        apk路径
     * @param requestCode 请求代码
     */
    fun installPackage(activity: Activity, path: String?, requestCode: Int) {
        installPackage(
            activity,
            path,
            activity.applicationContext.packageName + AUTHORITY_SUFFIX,
            requestCode
        )
    }

    /**
     * Session安装
     *
     * @param context 上下文
     * @param path    路径
     */
    fun sessionInstall(context: Context, path: String) {
        val file = File(path)
        val apkName = path.substring(path.lastIndexOf(File.separator) + 1, path.lastIndexOf(".apk"))
        val packageManager = context.packageManager
        val packageInstaller = packageManager.packageInstaller
        val params =
            PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        var session: PackageInstaller.Session? = null
        var outputStream: OutputStream? = null
        var inputStream: FileInputStream? = null
        try {
            //创建Session
            val sessionId = packageInstaller.createSession(params)
            //开启Session
            session = packageInstaller.openSession(sessionId)
            //获取输出流，用于将apk写入session
            outputStream = session.openWrite(apkName, 0, -1)
            inputStream = FileInputStream(file)
            val buffer = ByteArray(2048)
            var n: Int
            //读取apk文件写入session
            while ((inputStream.read(buffer).also { n = it }) > 0) {
                outputStream.write(buffer, 0, n)
            }
            //写完需要关闭流，否则会抛异常“files still open”
            inputStream.close()
            inputStream = null
            outputStream.flush()
            outputStream.close()
            outputStream = null
            //配置安装完成后发起的intent，通常是打开activity（这里我做了修改，修改为广播，intent并未设置目标参数，后面有需求在这里修改补充）
            val intent = Intent("iot.apk.install.completed")
            val pendingIntent =
                PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            val intentSender = pendingIntent.intentSender
            //提交启动安装
            session.commit(intentSender)
        } catch (e: Exception) {
            e.printStackTrace()
            session?.abandon()
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            if (inputStream != null) {
                try {
                    inputStream.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * 获取apk包名
     *
     * @param context 上下文
     * @param path    apk路径
     * @return apk包名
     */
    fun getPackageName(context: Context, path: String?): String? {
        val packageManager = context.packageManager
        val packageInfo =
            packageManager.getPackageArchiveInfo(path!!, PackageManager.GET_ACTIVITIES)
        if (packageInfo != null) {
            return packageInfo.packageName
        }
        return null
    }

    /**
     * 调用installPackage()方法之后,获取包名。
     *
     * @param context 上下文
     * @param data    onActivityResult中的data
     * @return
     */
    fun getPackageName(context: Context, data: Intent): String? {
        val packageUri = data.data
        if (packageUri != null) {
            return getPackageName(context, packageUri.path)
        }
        return null
    }

    /**
     * 调用installPackage()方法之后处理打开应用使用。
     *
     * @param context 上下文
     * @param data    onActivityResult中的data
     * @return
     */
    fun launch(context: Context, data: Intent) {
        val packageName = getPackageName(context, data)
        if (packageName != null) {
            launch(context, packageName)
        }
    }

    /**
     * 启动应用
     *
     * @param context     上下文
     * @param packageName 包名
     */
    fun launch(context: Context, packageName: String?) {
        val intent = context.packageManager.getLaunchIntentForPackage(
            packageName!!
        )
        Log.i(TAG, "launch intent:${intent}")
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    /**
     * 启动应用
     *
     * @param context     上下文
     * @param packageName 包名
     */
    fun boot(context: Context, packageName: String?) {
        val intent = context.packageManager.getLaunchIntentForPackage(
            packageName!!
        )
        Log.i(TAG, "launch intent:${intent}")
        if (intent != null) {
            intent.putExtra("boot", true)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    /**
     * 获取启动类组件名称
     *
     * @param context 上下文
     * @return 入口组件名称
     */
    fun getLauncherComponentName(context: Context): ComponentName? {
        val intent = Intent(Intent.ACTION_MAIN, null)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        intent.setPackage(context.packageName)
        val packageManager = context.packageManager
        val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_ALL)
        if (resolveInfo != null) {
            return ComponentName(
                resolveInfo.activityInfo.packageName,
                resolveInfo.activityInfo.name
            )
        }
        return null
    }

    /**
     * 是否已打开
     *
     * @param context     上下文
     * @param packageName 包名
     * @return
     */
    fun isOpen(context: Context, packageName: String?): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningAppProcesses = activityManager.runningAppProcesses
        if (runningAppProcesses != null) {
            for (processInfo in runningAppProcesses) {
                try {
                    val processName = context.packageManager.getApplicationInfo(
                        processInfo.processName,
                        PackageManager.GET_META_DATA
                    ).packageName
                    if (processName.equals(packageName, ignoreCase = true)) {
                        return true
                    }
                } catch (e: PackageManager.NameNotFoundException) {
                    e.printStackTrace()
                }
            }
        }
        return false
    }

    /**
     * 管理应用所有文件访问权限
     * Android 10 及以上版本 30
     * @param context 上下文
     * @param launcher 结果启动器，val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult(), callback = {})
     *
     */
    fun launchAppAllFilesAccessPermission(
        context: Context,
        launcher: ActivityResultLauncher<Intent>
    ) {
        try {
            val intent = Intent()
            intent.action = Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
            val uri = Uri.fromParts("package", context.packageName, null)
            intent.data = uri
            launcher.launch(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 未知来源是否可用
     * @param context 上下文
     */
    fun isUnknownSourcesEnabled(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return context.packageManager.canRequestPackageInstalls()
        } else {
            try {
                return Settings.Secure.getInt(
                    context.contentResolver,
                    Settings.Secure.INSTALL_NON_MARKET_APPS
                ) == 1
            } catch (e: Settings.SettingNotFoundException) {
                e.printStackTrace()
                return false
            }
        }
    }

    /**
     * 启动未知来源应用页面
     * @param context 上下文
     * @param launcher 结果启动器，val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult(), callback = {})
     */
    fun launchUnknownAppSources(context: Context, launcher: ActivityResultLauncher<Intent>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Android 8.0 及以上版本，跳转到应用的未知来源安装权限设置页
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
            intent.setData(Uri.parse("package:" + context.packageName))
            launcher.launch(intent)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            // Android 4.2 到 Android 7.1 版本，跳转到系统的未知来源安装权限设置页
            val intent = Intent()
            intent.setAction(Settings.ACTION_SECURITY_SETTINGS)
            launcher.launch(intent)
        } else {
            // Android 4.1 及以下版本，通常在系统设置中手动开启未知来源选项，这里简单提示用户
            println("请在系统设置中手动开启未知来源安装权限")
        }
    }

    /**
     * 是否拥有管理应用所有文件访问权限
     * @return
     */
    fun isExternalStorageManager(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return true
        }
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()
    }

    /**
     * 重启应用
     * @param context 上下文
     */
    fun restartApp(context: Context) {
        // 获取应用启动的 Intent
        val packageManager = context.packageManager
        val intent = packageManager.getLaunchIntentForPackage(context.packageName)
        // 清空原有 Intent 的标志位
        intent?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            // 重启应用
            context.startActivity(it)
            // 结束当前进程
            android.os.Process.killProcess(android.os.Process.myPid())
        }
    }

}
