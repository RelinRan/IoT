package androidx.iot.utils;

import android.content.ComponentName;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * adb shell指令执行
 */
public class Shell {

    private static final String TAG = Shell.class.getSimpleName();

    /**
     * 重启设备
     */
    public static Value reboot() {
        return batch("su", "reboot");
    }

    /**
     * 启动APP
     *
     * @param context 上下文
     * @return
     */
    public static Value launch(Context context) {
        ComponentName componentName = Apk.getLauncherComponentName(context);
        if (componentName == null) {
            return new Value(-1, "launch componentName is null.");
        }
        String packageName = componentName.getPackageName();
        String className = componentName.getClassName();
        return launch(packageName, className);
    }

    /**
     * 启动app
     *
     * @param packageName 包名
     * @param className   类名（包含路径）
     * @return
     */
    public static Value launch(String packageName, String className) {
        if (isRooted()) {
            return batch("su",  "am start -n " + packageName + "/" + className);
        }
        return new Value(-1, "launch failed not root");
    }

    /**
     * @return 是否拥有Root权限
     */
    public static boolean isRooted() {
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            os.writeBytes("exit\n");
            os.flush();
            process.waitFor();
            return process.exitValue() == 0;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 命令安装（Root权限）
     *
     * @param context 上下文
     * @param file    apk文件
     * @return
     */
    public static Value install(Context context, File file) {
        return install(context, file == null ? "" : file.getAbsolutePath());
    }

    /**
     * 命令安装（Root权限）
     *
     * @param context 上下文
     * @param path    安装路径
     */
    public static Value install(Context context, String path) {
        if (TextUtils.isEmpty(path)){
            return new Value(-1, "install file not exist.");
        }
        File file = new File(path);
        if (!file.exists()) {
            return new Value(-1, "install file not exist.");
        }
        if (!isRooted()) {
            return new Value(-2, "install device is not root.");
        }
        ComponentName componentName = Apk.getLauncherComponentName(context);
        if (componentName == null) {
            return new Value(-3, "install componentName is null.");
        }
        String packageName = componentName.getPackageName();
        String className = componentName.getClassName();
        String install = "pm install -r -i " + packageName + " " + path;
        String launch = "am start -n " + packageName + "/" + className;
        return batch("su", install, launch);
    }

    /**
     * 读取执行响应
     *
     * @param reader
     * @return
     */
    private static String read(BufferedReader reader) {
        try {
            String line;
            StringBuilder builder = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            return builder.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 直接执行命令
     *
     * @param command 命令
     * @return 0：正常，非0：异常
     */
    public static Value exec(String... command) {
        Value value = new Value(-1, "");
        BufferedReader reader = null;
        try {
            Log.i(TAG, "exec command: " + toString(command));
            Process process = Runtime.getRuntime().exec(command);
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            os.writeBytes("exit\n");
            os.flush();
            process.waitFor();
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String message = read(reader);
            Log.i(TAG, "exec message: " + message);
            value.setMessage(message);
            value.setCode(process.exitValue());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return value;
    }

    /**
     * builder方式执行命令
     *
     * @param command 指令
     * @return
     */
    public static Value builder(String... command) {
        BufferedReader reader = null;
        Value value = new Value(-1, "");
        try {
            Log.i(TAG, "builder command: " + toString(command));
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);//将错误输出和标准输出合并为一个流
            Process process = processBuilder.start();
            int exitCode = process.waitFor();//等待进程执行完成
            Log.i(TAG, "builder exit code: " + exitCode);
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String message = read(reader);
            Log.i(TAG, "builder message: " + message);
            value.setMessage(message);
            value.setCode(process.exitValue());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return value;
    }

    /**
     * 批量处理指令
     *
     * @param type     类型,超级用户：su; shell脚本：sh
     * @param commands 指令
     * @return
     */
    public static Value batch(String type, String... commands) {
        DataOutputStream dos = null;
        BufferedReader reader = null;
        Value value = new Value(-1, "");
        try {
            StringBuilder builder = new StringBuilder();
            int length = commands == null ? 0 : commands.length;
            for (int i = 0; i < length; i++) {
                builder.append(commands[i]);
                if (i != length - 1) {
                    builder.append(" && ");
                }
            }
            ProcessBuilder processBuilder = new ProcessBuilder(type, "-c", builder.toString());
            Log.i(TAG, "write command: " + type + " -c " + builder);
            Process process = processBuilder.start();

            int exitCode = process.waitFor();//等待进程执行完成
            Log.i(TAG, "write exit code: " + exitCode);
            value.setCode(process.exitValue());

            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String message = read(reader);
            Log.i(TAG, "write message: " + message);
            value.setMessage(message);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (dos != null) {
                    dos.close();
                }
                if (reader != null) {
                    reader.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return value;
    }

    private static String toString(String... command) {
        StringBuilder builder = new StringBuilder();
        int length = command == null ? 0 : command.length;
        for (int i = 0; i < length; i++) {
            builder.append(command[i]);
            if (i != length - 1) {
                builder.append(" ");
            }
        }
        return builder.toString();
    }

}
