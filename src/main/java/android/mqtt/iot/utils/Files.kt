package android.mqtt.iot.utils

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipFile

object Files {

    private val TAG = "Files"


    private fun getUriForPath(context: Context, path: String): Uri {
        val file = File(path)
        if (!file.exists()) {
            Log.e(
                TAG,
                "file is not exist path = " + file.absolutePath
            )
        }
        val uri: Uri
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val authority = context.applicationContext.packageName + ".fileProvider"
            Log.i(TAG, "authority = $authority")
            uri = FileProvider.getUriForFile(context, authority, file)
        } else {
            uri = Uri.fromFile(File(path))
        }
        return uri
    }


    fun copy(context: Context, src: File, dest: File) {
        if (src.exists() && !dest.exists()) {
            try {
                val inputUri: Uri = getUriForPath(context, src.getAbsolutePath())
                val fis: InputStream =
                    BufferedInputStream(context.getContentResolver().openInputStream(inputUri))
                val fos = FileOutputStream(dest)
                val buffer = ByteArray(4096)
                var bytesRead: Int
                while ((fis.read(buffer).also { bytesRead = it }) != -1) {
                    fos.write(buffer, 0, bytesRead)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }


    fun File.copyTo(destDir: File) {
        val destFile = File(destDir,this.name)
        this.copyTo(destFile, overwrite = true)
    }


    fun File.moveTo(destDir: File) {
        val destFile = File(destDir,this.name)
        this.copyTo(destFile, overwrite = true)
        this.delete()
    }


    fun File.unzipTo(destDir: File) {
        if (!destDir.exists()) {
            destDir.mkdirs()
        }
        ZipFile(this).use { zipFile ->
            zipFile.entries().asSequence().forEach { entry ->
                val destFile = File(destDir, entry.name)
                if (entry.isDirectory) {
                    destFile.mkdirs()
                } else {
                    destFile.parentFile?.mkdirs()
                    zipFile.getInputStream(entry).use { input ->
                        FileOutputStream(destFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
        }
    }

}
