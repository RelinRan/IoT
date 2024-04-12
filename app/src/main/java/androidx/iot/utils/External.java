package androidx.iot.utils;

import android.content.Context;
import android.os.Build;
import android.os.Environment;

import java.io.File;

/**
 * 外置
 */
public class External {

    /**
     * 是否沙盒环境
     *
     * @return
     */
    public static boolean isSandbox() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;
    }

    /**
     * 获取存储文件夹
     *
     * @param context 上下文
     * @return
     */
    public static File getStorageDir(Context context) {
        if (isSandbox()) {
            return context.getExternalFilesDir(null);
        }
        return Environment.getExternalStorageDirectory();
    }

    /**
     * 获取存储文件夹
     *
     * @param context     上下文
     * @param projectName 项目名称
     * @return
     */
    public static File getStorageDir(Context context, String projectName) {
        File dir;
        if (isSandbox()) {
            dir = context.getExternalFilesDir(null);
        } else {
            dir = new File(Environment.getExternalStorageDirectory(), projectName);
        }
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    /**
     * 获取存储文件夹
     *
     * @param context     上下文
     * @param projectName 项目名称（Android10以下的外部路径）
     * @param dirName     文件夹名称
     * @return
     */
    public static File getStorageDir(Context context, String projectName, String dirName) {
        if (isSandbox()) {
            return context.getExternalFilesDir(dirName);
        }
        File project = new File(Environment.getExternalStorageDirectory(), projectName);
        if (!project.exists()) {
            project.mkdirs();
        }
        File dir = new File(project, dirName);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

}
