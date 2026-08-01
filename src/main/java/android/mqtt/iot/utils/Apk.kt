package android.mqtt.iot.utils

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


    const val AUTHORITY_SUFFIX: String = ".fileProvider"


    fun getApplicationName(context: Context): String {
        val packageManager = context.packageManager
        val applicationInfo = context.applicationInfo
        return packageManager.getApplicationLabel(applicationInfo).toString()
    }


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


    private fun toIntArray(array: Array<String>): IntArray {
        val items = IntArray(array.size)
        for (i in items.indices) {
            val value = array[i]
            items[i] = filterHorizontalSymbol(value)
        }
        return items
    }


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


    private fun isNumeric(value: String): Boolean {
        return value.matches("-?\\d+(\\.\\d+)?".toRegex())
    }


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


    fun getUriForPath(context: Context, path: String?): Uri {
        return getUriForPath(
            context,
            path,
            context.applicationContext.packageName + AUTHORITY_SUFFIX
        )
    }


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
        intent.putExtra("IMPLUS_INSTALL", "SILENT_INSTALL")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }


    fun installPackage(activity: Activity, path: String?, authority: String?, requestCode: Int) {
        val apkUri = getUriForPath(activity, path, authority)
        val intent = Intent(Intent.ACTION_INSTALL_PACKAGE)
        intent.setData(apkUri)
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
        intent.putExtra(Intent.EXTRA_RETURN_RESULT, true)
        activity.startActivityForResult(intent, requestCode)
    }


    fun installPackage(activity: Activity, path: String?, requestCode: Int) {
        installPackage(
            activity,
            path,
            activity.applicationContext.packageName + AUTHORITY_SUFFIX,
            requestCode
        )
    }


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
            val sessionId = packageInstaller.createSession(params)
            session = packageInstaller.openSession(sessionId)
            outputStream = session.openWrite(apkName, 0, -1)
            inputStream = FileInputStream(file)
            val buffer = ByteArray(2048)
            var n: Int
            while ((inputStream.read(buffer).also { n = it }) > 0) {
                outputStream.write(buffer, 0, n)
            }
            inputStream.close()
            inputStream = null
            outputStream.flush()
            outputStream.close()
            outputStream = null
            val intent = Intent("iot.apk.install.completed")
            val pendingIntent =
                PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            val intentSender = pendingIntent.intentSender
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


    fun getPackageName(context: Context, path: String?): String? {
        val packageManager = context.packageManager
        val packageInfo =
            packageManager.getPackageArchiveInfo(path!!, PackageManager.GET_ACTIVITIES)
        if (packageInfo != null) {
            return packageInfo.packageName
        }
        return null
    }


    fun getPackageName(context: Context, data: Intent): String? {
        val packageUri = data.data
        if (packageUri != null) {
            return getPackageName(context, packageUri.path)
        }
        return null
    }


    fun launch(context: Context, data: Intent) {
        val packageName = getPackageName(context, data)
        if (packageName != null) {
            launch(context, packageName)
        }
    }


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


    fun launchUnknownAppSources(context: Context, launcher: ActivityResultLauncher<Intent>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
            intent.setData(Uri.parse("package:" + context.packageName))
            launcher.launch(intent)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            val intent = Intent()
            intent.setAction(Settings.ACTION_SECURITY_SETTINGS)
            launcher.launch(intent)
        } else {
            println("请在系统设置中手动开启未知来源安装权限")
        }
    }


    fun isExternalStorageManager(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return true
        }
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()
    }


    fun restartApp(context: Context) {
        val packageManager = context.packageManager
        val intent = packageManager.getLaunchIntentForPackage(context.packageName)
        intent?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(it)
            android.os.Process.killProcess(android.os.Process.myPid())
        }
    }

}
