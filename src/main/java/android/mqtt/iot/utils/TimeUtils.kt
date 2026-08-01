package android.mqtt.iot.utils

import android.os.Build
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

object TimeUtils {


    fun formatMilliseconds(milliseconds: Long): String {
        return formatMilliseconds("yyyy-MM-dd HH:mm:ss", milliseconds)
    }


    fun formatMilliseconds(pattern: String, milliseconds: Long): String {
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        val date = Date(milliseconds)
        return sdf.format(date)
    }

}
