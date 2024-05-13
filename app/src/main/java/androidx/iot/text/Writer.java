package androidx.iot.text;

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

    public Writer(String path) {
        this.file = new File(path);
    }

    public Writer(File file) {
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
        if (textWrite == null) {
            textWrite = new TextWrite(this, file, channels);
        }
        textWrite.setCancel(false);
        textWrite.setAppend(append);
        textWrite.setContent(content);
        textWrite.setOnWriteListener(onWriteListener);
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
    public String sync(String content, boolean append) {
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            BufferedWriter writer = new BufferedWriter(new FileWriter(file, append));
            writer.write(content);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return content;
    }

}
