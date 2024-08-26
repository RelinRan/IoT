package androidx.iot.text;

import android.os.Looper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 文本写入
 */
public class Writer {

    private File file;
    private ExecutorService service;
    private Future future;
    private Channels channels;
    private TextWrite textWrite;
    private BufferedWriter writer;

    public Writer(String path) {
        this.file = new File(path);
    }

    public Writer(File file) {
        this.file = file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    /**
     * 异步写入数据
     *
     * @param onWriteListener
     */
    public void async(String content, boolean append, OnWriteListener onWriteListener) {
        if (service == null) {
            service = Executors.newCachedThreadPool();
        }
        if (channels == null && onWriteListener != null) {
            channels = new Channels(Looper.getMainLooper());
        }
        if (textWrite == null) {
            textWrite = new TextWrite(this, file, channels);
        }
        textWrite.setFile(file);
        textWrite.setCancel(false);
        textWrite.setAppend(append);
        textWrite.setOnWriteListener(onWriteListener);
        textWrite.setContent(content);
        future = service.submit(textWrite);
    }

    /**
     * 取消操作
     */
    public void cancel() {
        if (textWrite != null) {
            textWrite.setCancel(true);
        }
        if (future != null) {
            future.cancel(true);
        }
        if (channels != null) {
            channels.removeWrite();
            channels.removeCallbacksAndMessages(null);
        }
    }

    /**
     * 同步写入数据
     *
     * @param content 内容
     * @param append  是否追加
     * @return
     */
    public synchronized String sync(String content, boolean append) {
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            if (writer == null) {
                writer = new BufferedWriter(new FileWriter(file, append));
            }
            writer.write(content);
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                    writer = null;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return content;
    }

}
