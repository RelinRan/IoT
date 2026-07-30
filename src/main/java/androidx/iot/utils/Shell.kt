package androidx.iot.utils

import android.content.Context
import android.text.TextUtils
import android.util.Log
import androidx.iot.data.Value
import androidx.iot.utils.Apk.getLauncherComponentName
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStreamReader


/**
 * adb shell指令执行
 */
object Shell {
    private val TAG: String = Shell::class.java.simpleName

    /**
     * 重启设备
     */
    fun reboot(): Value {
        return batch("su", "reboot")
    }

    /**
     * 启动APP
     *
     * @param context 上下文
     * @return
     */
    fun launch(context: Context?): Value {
        val componentName = getLauncherComponentName(
            context!!
        )
        if (componentName == null) {
            return Value(-1, "launch componentName is null.")
        }
        val packageName = componentName.packageName
        val className = componentName.className
        return launch(packageName, className)
    }

    /**
     * 启动app
     *
     * @param packageName 包名
     * @param className   类名（包含路径）
     * @return
     */
    fun launch(packageName: String, className: String): Value {
        if (isRooted) {
            return batch("su", "am start -n $packageName/$className")
        }
        return Value(-1, "launch failed not root")
    }

    val isRooted: Boolean
        /**
         * @return 是否拥有Root权限
         */
        get() {
            try {
                val process = Runtime.getRuntime().exec("su")
                val os = DataOutputStream(process.outputStream)
                os.writeBytes("exit\n")
                os.flush()
                process.waitFor()
                return process.exitValue() == 0
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            return false
        }

    /**
     * 命令安装（Root权限）
     *
     * @param context 上下文
     * @param file    apk文件
     * @return
     */
    fun install(context: Context?, file: File?): Value {
        return install(context, if (file == null) "" else file.absolutePath)
    }

    /**
     * 命令安装（Root权限）
     *
     * @param context 上下文
     * @param path    安装路径
     */
    fun install(context: Context?, path: String): Value {
        if (TextUtils.isEmpty(path)) {
            return Value(-1, "install file not exist.")
        }
        val file = File(path)
        if (!file.exists()) {
            return Value(-1, "install file not exist.")
        }
        if (!isRooted) {
            return Value(-2, "install device is not root.")
        }
        val componentName = getLauncherComponentName(
            context!!
        )
        if (componentName == null) {
            return Value(-3, "install componentName is null.")
        }
        val packageName = componentName.packageName
        val className = componentName.className
        val install = "pm install -r -i $packageName $path"
        val launch = "am start -n $packageName/$className"
        return batch("su", install, launch)
    }

    /**
     * 卸载
     *
     * @param context 上下文
     * @return
     */
    fun uninstall(context: Context?): Value {
        val componentName = getLauncherComponentName(
            context!!
        )
        if (componentName == null) {
            return Value(-1, "uninstall componentName is null.")
        }
        val packageName = componentName.packageName
        val uninstall = "pm uninstall -k $packageName"
        return batch("su", uninstall)
    }

    /**
     * 读取执行响应
     *
     * @param reader
     * @return
     */
    private fun read(reader: BufferedReader): String {
        try {
            var line: String?
            val builder = StringBuilder()
            while ((reader.readLine().also { line = it }) != null) {
                builder.append(line)
            }
            return builder.toString()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""
    }

    /**
     * 直接执行命令
     *
     * @param command 命令
     * @return 0：正常，非0：异常
     */
    fun exec(vararg command: String?): Value {
        val value = Value(-1, "")
        var reader: BufferedReader? = null
        try {
            Log.i(TAG, "exec command: " + command.joinToString(" "))
            val process = Runtime.getRuntime().exec(command)
            val os = DataOutputStream(process.outputStream)
            os.writeBytes("exit\n")
            os.flush()
            process.waitFor()
            reader = BufferedReader(InputStreamReader(process.inputStream))
            val message = read(reader)
            Log.i(TAG, "exec message: $message")
            value.message = message
            value.code = process.exitValue()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        } finally {
            if (reader != null) {
                try {
                    reader.close()
                } catch (e: IOException) {
                    throw RuntimeException(e)
                }
            }
        }
        return value
    }

    /**
     * builder方式执行命令
     *
     * @param command 指令
     * @return
     */
    fun builder(vararg command: String?): Value {
        var reader: BufferedReader? = null
        val value = Value(-1, "")
        try {
            Log.i(TAG, "builder command: " + command.joinToString(" "))
            val processBuilder = ProcessBuilder(*command)
            processBuilder.redirectErrorStream(true) //将错误输出和标准输出合并为一个流
            val process = processBuilder.start()
            val exitCode = process.waitFor() //等待进程执行完成
            Log.i(TAG, "builder exit code: $exitCode")
            reader = BufferedReader(InputStreamReader(process.inputStream))
            val message = read(reader)
            Log.i(TAG, "builder message: $message")
            value.message = message
            value.code = process.exitValue()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                reader?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return value
    }

    /**
     * 批量处理指令
     *
     * @param type     类型,超级用户：su; shell脚本：sh
     * @param commands 指令
     * @return
     */
    fun batch(type: String, vararg commands: String?): Value {
        var dos: DataOutputStream? = null
        var reader: BufferedReader? = null
        val value = Value(-1, "")
        try {
            val builder = StringBuilder()
            val length = commands?.size ?: 0
            for (i in 0 until length) {
                builder.append(commands[i])
                if (i != length - 1) {
                    builder.append(" && ")
                }
            }
            val processBuilder = if (type == "su") {
                ProcessBuilder(type)
            } else {
                ProcessBuilder(type, "-c", builder.toString())
            }
            processBuilder.redirectErrorStream(true)
            Log.i(TAG, "write command: $type ${if (type == "su") "<stdin>" else "-c"} $builder")
            val process = processBuilder.start()
            if (type == "su") {
                dos = DataOutputStream(process.outputStream)
                dos.writeBytes(builder.toString())
                dos.writeBytes("\nexit\n")
                dos.flush()
            }

            val exitCode = process.waitFor() //等待进程执行完成
            Log.i(TAG, "write exit code: $exitCode")
            value.code = process.exitValue()

            reader = BufferedReader(InputStreamReader(process.inputStream))
            val message = read(reader)
            Log.i(TAG, "write message: $message")
            value.message = message
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                dos?.close()
                reader?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return value
    }

    private fun toString(vararg command: String): String {
        val builder = StringBuilder()
        val length = command?.size ?: 0
        for (i in 0 until length) {
            builder.append(command[i])
            if (i != length - 1) {
                builder.append(" ")
            }
        }
        return builder.toString()
    }
}
