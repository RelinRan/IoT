package androidx.iot.utils

import android.os.Build
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

object TimeUtils {

    /**
     * 格式化日期
     * @param milliseconds 毫秒时间
     * @return yyyy-MM-dd HH:mm:ss
     */
    fun formatMilliseconds(milliseconds: Long): String {
        return formatMilliseconds("yyyy-MM-dd HH:mm:ss", milliseconds)
    }

    /**
     * 格式化日期
     * @param pattern 时间格式
     * @param milliseconds 毫秒时间
     * @return yyyy-MM-dd HH:mm:ss
     */
    fun formatMilliseconds(pattern: String, milliseconds: Long): String {
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        val date = Date(milliseconds)
        return sdf.format(date)
    }

}