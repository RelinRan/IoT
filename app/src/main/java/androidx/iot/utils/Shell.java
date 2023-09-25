package androidx.iot.utils;

import android.content.ComponentName;
import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * adb shell指令执行
 */
public class Shell {

    private static final String TAG = Shell.class.getSimpleName();
    /**
     * 类型 - 重启
     */
    public static final int REBOOT = 1;
    /**
     * 类型 - 安装
     */
    public static final int INSTALL = 2;

    /**
     * 重启设备
     *
     * @param listener 执行监听
     */
    public static Future reboot(OnExecuteListener listener) {
        return execute(REBOOT, new String[]{"reboot"}, listener);
    }

    /**
     * 重启设备
     */
    public static Future reboot() {
        return reboot(null);
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
     * @param context  上下文
     * @param path     安装路径
     * @param listener 安装监听
     */
    public static Future install(Context context, String path, OnExecuteListener listener) {
        int code;
        String msg;
        if (new File(path).exists()) {
            if (isRooted()) {
                ComponentName componentName = Apk.getLauncherComponentName(context);
                if (componentName != null) {
                    String packageName = componentName.getPackageName();
                    String className = componentName.getClassName();
                    String installCommand = "pm install -r " + path;
                    String launchCommand = "am start -n " + packageName + "/" + className;
                    String exitCommand = "exit";
                    return execute(INSTALL, new String[]{installCommand, launchCommand, exitCommand}, listener);
                } else {
                    code = -3;
                    msg = "component name not exist";
                }
            } else {
                code = -2;
                msg = "device is not rooted";
            }
        } else {
            code = -1;
            msg = "apk is not exist path = "+path;
        }
        Log.e(TAG, "code = " + code + ",msg = " + msg);
        if (listener != null) {
            listener.onExecuteResult(INSTALL, code, msg);
        }
        return null;
    }

    /**
     * 命令安装（Root权限）
     *
     * @param path 安装路径
     */
    public static void install(Context context, String path) {
        install(context, path, null);
    }

    /**
     * 指令执行监听
     */
    public interface OnExecuteListener {
        /**
         * 执行结果
         *
         * @param type 类型{@link #REBOOT}
         * @param code 0：成功 -100：失败
         * @param msg  信息
         */
        void onExecuteResult(int type, int code, String msg);
    }

    /**
     * 执行命令
     *
     * @param type     类型
     * @param commands 命令数组
     * @param listener 监听
     */
    public static Future execute(int type, String[] commands, OnExecuteListener listener) {
        return Executors.newCachedThreadPool().submit(() -> {
            Process process = null;
            DataOutputStream os = null;
            BufferedReader reader = null;
            try {
                process = Runtime.getRuntime().exec("su");
                Log.i(TAG, "su");
                os = new DataOutputStream(process.getOutputStream());
                for (String cmd : commands) {
                    Log.i(TAG, cmd);
                    os.writeBytes(cmd + "\n");
                }
                os.flush();
                process.waitFor();
                reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String outputLine;
                StringBuilder output = new StringBuilder();
                while ((outputLine = reader.readLine()) != null) {
                    output.append(outputLine);
                    output.append("\n");
                }
                String result = output.toString();
                boolean succeed = result.contains("Success");
                Log.i(TAG, "succeed = " + succeed + ", " + result);
                if (listener != null) {
                    listener.onExecuteResult(type, succeed ? 0 : -100, result);
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (os != null) {
                        os.close();
                    }
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (process != null) {
                    process.destroy();
                }
            }
        });
    }

}
