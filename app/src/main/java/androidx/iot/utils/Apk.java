package androidx.iot.utils;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * Apk
 */
public class Apk {

    private static final String TAG = Apk.class.getSimpleName();

    /**
     * Android7.0 FileProvider文件授权后缀
     */
    public static final String AUTHORITY_SUFFIX = ".fileProvider";

    /**
     * 获取应用名称
     *
     * @param context 上下文
     * @return
     */
    public static String getApplicationName(Context context) {
        PackageManager packageManager = context.getPackageManager();
        ApplicationInfo applicationInfo = context.getApplicationInfo();
        return packageManager.getApplicationLabel(applicationInfo).toString();
    }

    /**
     * @param context 上下文
     * @return 版本名字
     */
    public static String getVersionName(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getApplicationContext().getPackageName(), 0);
            return packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @param context 上下文
     * @return 版本代码
     */
    public static int getVersionCode(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getApplicationContext().getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * 比较版本号是否需要升级
     *
     * @param context    上下文
     * @param apiVersion 接口版本
     * @return 是否需要升级
     */
    public static boolean isNewVersion(Context context, String apiVersion) {
        String localVersion = getVersionName(context);
        String localItems[] = localVersion.split("\\.");
        String apiItems[] = apiVersion.split("\\.");
        int localIntItems[] = toIntArray(localItems);
        int apiIntItems[] = toIntArray(apiItems);
        if (localIntItems.length >= apiIntItems.length) {
            for (int i = 0; i < apiIntItems.length; i++) {
                if (localIntItems[i] < apiIntItems[i]) {
                    return true;
                }
                if (localIntItems[i] > apiIntItems[i]) {
                    return false;
                }
            }
        } else {
            for (int i = 0; i < localIntItems.length; i++) {
                if (localIntItems[i] < apiIntItems[i]) {
                    return true;
                }
                if (localIntItems[i] > apiIntItems[i]) {
                    return false;
                }
            }
            int lastIndex = localIntItems.length - 1;
            if (localIntItems[lastIndex] < apiIntItems[lastIndex]) {
                return true;
            }
            if (localIntItems[lastIndex] == apiIntItems[lastIndex]) {
                return true;
            }
        }
        return false;
    }

    /**
     * 字符数组转int数组
     *
     * @param array 字符数组
     * @return int数组
     */
    private static int[] toIntArray(String[] array) {
        int[] items = new int[array.length];
        for (int i = 0; i < items.length; i++) {
            String value = array[i];
            items[i] = filterHorizontalSymbol(value);
        }
        return items;
    }

    /**
     * 过滤横向符号
     *
     * @param value 数据
     * @return
     */
    private static int filterHorizontalSymbol(String value) {
        StringBuilder builder = new StringBuilder();
        char[] chars = value.toCharArray();
        for (int j = 0; j < chars.length; j++) {
            char charItem = chars[j];
            if (isNumeric(String.valueOf(charItem))) {
                builder.append(charItem);
            }
        }
        return builder.length() == 0 ? 0 : Integer.parseInt(builder.toString());
    }

    /**
     * 是否是数字
     *
     * @param value
     * @return
     */
    private static boolean isNumeric(String value) {
        return value.matches("-?\\d+(\\.\\d+)?");
    }

    /**
     * 文件路径获取Uri
     *
     * @param context   上下文
     * @param path      apk路径
     * @param authority Android 7.0以上FileProvider
     * @return 文件Uri
     */
    public static Uri getUriForPath(Context context, String path, String authority) {
        File file = new File(path);
        if (!file.exists()) {
            Log.e(TAG, "file is not exist path = " + file.getAbsolutePath());
        }
        Uri uri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            uri = FileProvider.getUriForFile(context, authority, file);
        } else {
            uri = Uri.fromFile(new File(path));
        }
        return uri;
    }

    /**
     * 文件路径获取Uri
     *
     * @param context 上下文
     * @param path    apk路径
     * @return 文件Uri
     */
    public static Uri getUriForPath(Context context, String path) {
        return getUriForPath(context, path, context.getApplicationContext().getPackageName() + AUTHORITY_SUFFIX);
    }

    /**
     * 普通安装apk
     *
     * @param context   上下文
     * @param path      apk路径
     * @param authority Android 7.0以上FileProvider
     */
    public static void install(Context context, String path, String authority) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri = getUriForPath(context, path, authority);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    /**
     * 普通安装apk
     *
     * @param context 上下文
     * @param path    apk路径
     */
    public static void install(Context context, String path) {
        install(context, path, context.getApplicationContext().getPackageName() + AUTHORITY_SUFFIX);
    }

    /**
     * 自动点击确认安装。
     * 此安装方法显示安装页面，自动点击安装。安装结果在onActivityResult处理。
     * 权限 - <uses-permission android:name="android.permission.INSTALL_PACKAGES" />
     *
     * @param activity    当前页面
     * @param path        apk路径
     * @param authority   Android 7.0以上FileProvider
     * @param requestCode 请求代码
     */
    public static void installPackage(Activity activity, String path, String authority, int requestCode) {
        Uri apkUri = getUriForPath(activity, path, authority);
        Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
        intent.setData(apkUri);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true);
        intent.putExtra(Intent.EXTRA_RETURN_RESULT, true);
        activity.startActivityForResult(intent, requestCode);
    }

    /**
     * 自动点击确认安装。
     * 此安装方法显示安装页面，自动点击安装。安装结果在onActivityResult处理。
     * 权限 - <uses-permission android:name="android.permission.INSTALL_PACKAGES" />
     *
     * @param activity    当前页面
     * @param path        apk路径
     * @param requestCode 请求代码
     */
    public static void installPackage(Activity activity, String path, int requestCode) {
        installPackage(activity, path, activity.getApplicationContext().getPackageName() + AUTHORITY_SUFFIX, requestCode);
    }

    /**
     * Session安装
     *
     * @param context 上下文
     * @param path    路径
     */
    public static void sessionInstall(Context context, String path) {
        File file = new File(path);
        String apkName = path.substring(path.lastIndexOf(File.separator) + 1, path.lastIndexOf(".apk"));
        PackageManager packageManager = context.getPackageManager();
        PackageInstaller packageInstaller = packageManager.getPackageInstaller();
        PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
        PackageInstaller.Session session = null;
        OutputStream outputStream = null;
        FileInputStream inputStream = null;
        try {
            //创建Session
            int sessionId = packageInstaller.createSession(params);
            //开启Session
            session = packageInstaller.openSession(sessionId);
            //获取输出流，用于将apk写入session
            outputStream = session.openWrite(apkName, 0, -1);
            inputStream = new FileInputStream(file);
            byte[] buffer = new byte[2048];
            int n;
            //读取apk文件写入session
            while ((n = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, n);
            }
            //写完需要关闭流，否则会抛异常“files still open”
            inputStream.close();
            inputStream = null;
            outputStream.flush();
            outputStream.close();
            outputStream = null;
            //配置安装完成后发起的intent，通常是打开activity（这里我做了修改，修改为广播，intent并未设置目标参数，后面有需求在这里修改补充）
            Intent intent = new Intent("iot.apk.install.completed");
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
            IntentSender intentSender = pendingIntent.getIntentSender();
            //提交启动安装
            session.commit(intentSender);
        } catch (Exception e) {
            e.printStackTrace();
            if (session != null) {
                session.abandon();
            }
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
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
    public static String getPackageName(Context context, String path) {
        PackageManager packageManager = context.getPackageManager();
        PackageInfo packageInfo = packageManager.getPackageArchiveInfo(path, PackageManager.GET_ACTIVITIES);
        if (packageInfo != null) {
            return packageInfo.packageName;
        }
        return null;
    }

    /**
     * 调用installPackage()方法之后,获取包名。
     *
     * @param context 上下文
     * @param data    onActivityResult中的data
     * @return
     */
    public static String getPackageName(Context context, Intent data) {
        Uri packageUri = data.getData();
        if (packageUri != null) {
            return getPackageName(context, packageUri.getPath());
        }
        return null;
    }

    /**
     * 调用installPackage()方法之后处理打开应用使用。
     *
     * @param context 上下文
     * @param data    onActivityResult中的data
     * @return
     */
    public static void launch(Context context, Intent data) {
        String packageName = getPackageName(context, data);
        if (packageName != null) {
            launch(context, packageName);
        }
    }

    /**
     * 启动应用
     *
     * @param context     上下文
     * @param packageName 包名
     */
    public static void launch(Context context, String packageName) {
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(packageName);
        if (intent != null) {
            intent.putExtra("launch",true);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }

    /**
     * 获取启动类组件名称
     *
     * @param context 上下文
     * @return 入口组件名称
     */
    public static ComponentName getLauncherComponentName(Context context) {
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setPackage(context.getPackageName());
        PackageManager packageManager = context.getPackageManager();
        ResolveInfo resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        if (resolveInfo != null) {
            return new ComponentName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name);
        }
        return null;
    }

    /**
     * 是否已打开
     *
     * @param context     上下文
     * @param packageName 包名
     * @return
     */
    public static boolean isOpen(Context context, String packageName) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = activityManager.getRunningAppProcesses();
        if (runningAppProcesses != null) {
            for (ActivityManager.RunningAppProcessInfo processInfo : runningAppProcesses) {
                try {
                    String processName = context.getPackageManager().getApplicationInfo(processInfo.processName, PackageManager.GET_META_DATA).packageName;
                    if (processName.equalsIgnoreCase(packageName)) {
                        return true;
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

}
