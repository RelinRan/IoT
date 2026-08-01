package android.mqtt.iot.utils

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.annotation.ChecksSdkIntAtLeast
import android.mqtt.iot.data.Register
import android.mqtt.iot.data.Regnwl
import com.google.gson.Gson
import java.io.File
import androidx.core.content.edit


object Store {


    private var project = "Alink"


    private var dir = "store"


    private val gson = Gson()


    fun initialize(project: String, dir: String) {
        this.project = project
        this.dir = dir
    }


    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
    fun isScopedStorage(): Boolean {
        return Build.VERSION.SDK_INT > Build.VERSION_CODES.S_V2
    }


    fun getExternalCompatibleDir(context: Context): File {
        if (isScopedStorage()) {
            return getExternalPackageDir(context, dir)
        }
        return getExternalCompatibleDir(context, project, dir)
    }


    fun getExternalCompatibleDir(context: Context, dir: String): File {
        if (isScopedStorage()) {
            return getExternalPackageDir(context, dir)
        }
        return getExternalStorageDir(project, dir)
    }


    fun getExternalCompatibleDir(context: Context, project: String, dir: String): File {
        if (isScopedStorage()) {
            return getExternalPackageDir(context, dir)
        }
        return getExternalStorageDir(project, dir)
    }


    fun getExternalPackageDir(context: Context, dir: String): File {
        val parent = File(context.getExternalFilesDir(null), dir)
        if (!parent.exists()) {
            parent.mkdirs()
        }
        return parent
    }


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


    private fun Context.sharePreferences(): SharedPreferences {
        return getSharedPreferences("${packageName}_store", Context.MODE_PRIVATE)
    }


    fun clear(context: Context) {
        context.sharePreferences().edit {
            clear()
        }
        val dir = getExternalStorageDir(project, dir)
        if (dir.exists()) {
            val files = dir.listFiles()
            files?.forEach { file ->
                file.delete()
            }
        }
    }


    fun clear(context: Context, key: String) {
        context.sharePreferences().edit {
            remove("REGISTER$key")
            remove("REGNWL$key")
        }
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


    fun register(context: Context, deviceId: String, register: Register) {
        val key = "REGISTER${deviceId}"
        val value = gson.toJson(register)
        if (isScopedStorage()) {
            save(context, key, gson.toJson(register))
        } else {
            backup(key, value)
        }
    }


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


    fun regnwl(context: Context, deviceId: String, regnwl: Regnwl) {
        val key = "REGISTER${deviceId}"
        val value = gson.toJson(regnwl)
        if (isScopedStorage()) {
            save(context, "REGNWL${deviceId}", gson.toJson(regnwl))
        } else {
            backup(key, value)
        }
    }


    fun backupFile(project: String, dir: String, key: String): File {
        return File(getExternalStorageDir(project, dir), "${key}.store")
    }


    fun backup(key: String, value: String) {
        backupFile(project, dir, key).writeText(value.toHex(), Charsets.UTF_8)
    }


    fun restore(key: String, def: String): String {
        val file = backupFile(project, dir, key)
        if (file.exists()) {
            return file.readText(Charsets.UTF_8).hexToString()
        }
        return def
    }

}
