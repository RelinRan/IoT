package androidx.iot.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.util.Log;

public class Memory {

    private String TAG = Memory.class.getSimpleName();
    private ActivityManager activityManager;

    public Memory(Context context) {
        activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
    }

    /**
     * 获取内存信息
     *
     * @return
     */
    public ActivityManager.MemoryInfo getMemoryInfo() {
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        return memoryInfo;
    }

    /**
     * 使用内存占比
     *
     * @return
     */
    public int getUsagePercentage() {
        ActivityManager.MemoryInfo info = getMemoryInfo();
        long totalMemory = info.totalMem;
        long availableMemory = info.availMem;
        long useMemory = totalMemory - availableMemory;
        boolean lowMemory = info.lowMemory;
        int percent = (int) (availableMemory * 100 / totalMemory);
        Log.d(TAG, "total:" + formatMemorySize(totalMemory) + ",available:" + formatMemorySize(availableMemory) + ",use:" + formatMemorySize(useMemory) + ",percent:" + percent + "%,low:" + lowMemory);
        return percent;
    }

    /**
     * 格式化内存大小
     *
     * @param sizeInBytes
     * @return
     */
    public String formatMemorySize(long sizeInBytes) {
        if (sizeInBytes < 1024) {
            return sizeInBytes + "B";
        } else if (sizeInBytes < 1048576) {
            return String.format("%.2fKB", (float) sizeInBytes / 1024);
        } else if (sizeInBytes < 1073741824) {
            return String.format("%.2fMB", (float) sizeInBytes / 1048576);
        } else {
            return String.format("%.4fGB", (float) sizeInBytes / 1073741824);
        }
    }

}
