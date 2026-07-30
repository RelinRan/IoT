package androidx.iot.utils

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.iot.data.Register
import androidx.iot.data.Regnwl
import com.google.gson.Gson
import java.io.File
import androidx.core.content.edit

/**
 * 数据存储
 */
object Store {

    /**
     * 项目名称
     */
    private var project = "Alink"

    /**
     * 存储文件夹
     */
    private var dir = "store"

    /**
     * JSON工具
     */
    private val gson = Gson()

    /**
     * 初始化
     */
    fun initialize(project: String, dir: String) {
        this.project = project
        this.dir = dir
    }

    /**
     * 分区储存限制（开发板系统项目要求卸载项目后，特殊数据保留）
     * 【注意】：此处限制对应开发板系统版本存储限制，并非大众手机系统的SDK_INT控制
     */
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
    fun isScopedStorage(): Boolean {
        return Build.VERSION.SDK_INT > Build.VERSION_CODES.S_V2
    }

    /**
     * 外部存储兼容文件夹
     * @param context 上下文
     */
    fun getExternalCompatibleDir(context: Context): File {
        if (isScopedStorage()) {
            return getExternalPackageDir(context, dir)
        }
        return getExternalCompatibleDir(context, project, dir)
    }

    /**
     * 外部存储兼容文件夹
     * @param context 上下文
     * @param dir 文件夹名称
     */
    fun getExternalCompatibleDir(context: Context, dir: String): File {
        if (isScopedStorage()) {
            return getExternalPackageDir(context, dir)
        }
        return getExternalStorageDir(project, dir)
    }

    /**
     * 外部存储兼容文件夹
     * @param context 上下文
     * @param project 项目名称
     * @param dir 文件夹名称
     */
    fun getExternalCompatibleDir(context: Context, project: String, dir: String): File {
        if (isScopedStorage()) {
            return getExternalPackageDir(context, dir)
        }
        return getExternalStorageDir(project, dir)
    }

    /**
     * 外部存储包名文件夹
     * @param context 上下文
     * @param dir 文件夹名称
     */
    fun getExternalPackageDir(context: Context, dir: String): File {
        val parent = File(context.getExternalFilesDir(null), dir)
        if (!parent.exists()) {
            parent.mkdirs()
        }
        return parent
    }

    /**
     * 外部存储文件夹
     * @param project 项目名称
     * @param dir 文件夹名称
     */
    fun getExternalStorageDir(project: String, dir: String): File {
        val project = File(Environment.getExternalStorageDirectory(), project)
        if (!project.exists()) {
            project.mkdirs()
        }
        val parent = File(project, dir)
        if (!parent.exists()) {
            parent.mkdirs()
        }
        return parent
    }

    /**
     *share 首选项
     */
    private fun Context.sharePreferences(): SharedPreferences {
        return getSharedPreferences("${packageName}_store", Context.MODE_PRIVATE)
    }

    /**
     * 清除所有数据
     * @param context 上下文
     */
    fun clear(context: Context) {
        context.sharePreferences().edit {
            clear()
        }
        //清空备份文件夹
        val dir = getExternalStorageDir(project, dir)
        if (dir.exists()) {
            val files = dir.listFiles()
            files?.forEach { file ->
                file.delete()
            }
        }
    }

    /**
     * 清除key数据
     * @param context 上下文
     * @param key 键
     */
    fun clear(context: Context, key: String) {
        context.sharePreferences().edit {
            remove("REGISTER$key")
            remove("REGNWL$key")
        }
        //清空备份文件夹
        val dir = getExternalStorageDir(project, dir)
        if (dir.exists()) {
            val files = dir.listFiles()
            files?.forEach { file ->
                if (file.name == "REGISTER$key" || file.name == "REGNWL$key") {
                    file.delete()
                }
            }
        }
    }


    /**
     * 保存
     * @param key 键名
     * @param value 键值
     */
    fun <T> save(context: Context, key: String, value: T) {
        val editor = context.sharePreferences().edit()
        when (value) {
            is String -> editor.putString(key, value).apply()
            is Float -> editor.putFloat(key, value).apply()
            is Int -> editor.putInt(key, value).apply()
            is Long -> editor.putLong(key, value).apply()
            is Boolean -> editor.putBoolean(key, value).apply()
            is Double -> editor.putString(key, value.toString()).apply()
            else -> throw IllegalArgumentException("Unsupported data type: ${value}")
        }
    }

    /**
     * 读取
     * @param context
     * @param key 键名
     * @param defValue 默认值
     */
    fun <T> read(context: Context, key: String, defValue: T): T {
        val preferences = context.sharePreferences();
        val value = when (defValue) {
            is String -> preferences.getString(key, defValue)
            is Int -> preferences.getInt(key, defValue)
            is Boolean -> preferences.getBoolean(key, defValue)
            is Float -> preferences.getFloat(key, defValue)
            is Long -> preferences.getLong(key, defValue)
            is Double -> preferences.getString(key, defValue.toString())?.toDouble()
            else -> throw IllegalArgumentException("Unsupported data type: ${defValue}")
        }
        return value as T
    }

    /**
     * 读取一型一密预注册认证信息
     * @param context
     * @param deviceId
     */
    fun register(context: Context, deviceId: String): Register {
        val key = "REGISTER${deviceId}"
        if (isScopedStorage()) {
            val json = read(context, key, "{}")
            return gson.fromJson(json, Register::class.java)
        }
        val content = restore(key, "{}")
        save(context, key, content)
        return gson.fromJson(content, Register::class.java)
    }

    /**
     * 保存一型一密预注册认证信息
     */
    fun register(context: Context, deviceId: String, register: Register) {
        val key = "REGISTER${deviceId}"
        val value = gson.toJson(register)
        if (isScopedStorage()) {
            save(context, key, gson.toJson(register))
        } else {
            backup(key, value)
        }
    }

    /**
     * 读取一型一密免预注册认证信息
     * @param context
     * @param deviceId
     */
    fun regnwl(context: Context, deviceId: String): Regnwl {
        val key = "REGNWL${deviceId}"
        if (isScopedStorage()) {
            val json = read(context, key, "{}")
            return gson.fromJson(json, Regnwl::class.java)
        }
        val content = restore(key, "{}")
        save(context, key, content)
        return gson.fromJson(content, Regnwl::class.java)
    }

    /**
     * 保存一型一密免预注册认证信息
     */
    fun regnwl(context: Context, deviceId: String, regnwl: Regnwl) {
        val key = "REGISTER${deviceId}"
        val value = gson.toJson(regnwl)
        if (isScopedStorage()) {
            save(context, "REGNWL${deviceId}", gson.toJson(regnwl))
        } else {
            backup(key, value)
        }
    }

    /**
     * 获取备份文件
     * @param project 项目名称
     * @param dir 文件夹名称
     * @param key key值
     */
    fun backupFile(project: String, dir: String, key: String): File {
        return File(getExternalStorageDir(project, dir), "${key}.store")
    }

    /**
     * 设置备份
     * @param key
     * @param value
     */
    fun backup(key: String, value: String) {
        backupFile(project, dir, key).writeText(value.toHex(), Charsets.UTF_8)
    }

    /**
     * 恢复备份
     * @param key
     */
    fun restore(key: String, def: String): String {
        val file = backupFile(project, dir, key)
        if (file.exists()) {
            return file.readText(Charsets.UTF_8).hexToString()
        }
        return def
    }

}