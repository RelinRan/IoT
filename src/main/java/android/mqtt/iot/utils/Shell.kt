package android.mqtt.iot.utils

import android.content.Context
import android.text.TextUtils
import android.util.Log
import android.mqtt.iot.data.Value
import android.mqtt.iot.utils.Apk.getLauncherComponentName
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStreamReader


object Shell {
    private val TAG: String = Shell::class.java.simpleName


    fun reboot(): Value {
        return batch("su", "reboot")
    }


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


    fun launch(packageName: String, className: String): Value {
        if (isRooted) {
            return batch("su", "am start -n $packageName/$className")
        }
        return Value(-1, "launch failed not root")
    }

    val isRooted: Boolean

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


    fun install(context: Context?, file: File?): Value {
        return install(context, if (file == null) "" else file.absolutePath)
    }


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


    fun builder(vararg command: String?): Value {
        var reader: BufferedReader? = null
        val value = Value(-1, "")
        try {
            Log.i(TAG, "builder command: " + command.joinToString(" "))
            val processBuilder = ProcessBuilder(*command)
            processBuilder.redirectErrorStream(true)
            val process = processBuilder.start()
            val exitCode = process.waitFor()
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

            val exitCode = process.waitFor()
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
