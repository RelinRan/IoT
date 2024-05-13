package androidx.iot.text;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 文本读取
 */
public class Reader {

    private final String TAG = Reader.class.getSimpleName();
    private File file;
    private ExecutorService service;
    private Future future;
    private Channels channels;
    private TextRead textRead;

    public Reader(String path) {
        this.file = new File(path);
        service = Executors.newCachedThreadPool();
    }

    public Reader(File file) {
        this.file = file;

    }

    /**
     * 异步读取数据
     *
     * @param onReadListener
     */
    public void async(OnReadListener onReadListener) {
        if (service==null){
            service = Executors.newCachedThreadPool();
        }
        if (channels == null) {
            channels = new Channels();
        }
        if (textRead == null) {
            textRead = new TextRead(file, this, channels);
        }
        textRead.setCancel(false);
        textRead.setOnReadListener(onReadListener);
        future = service.submit(textRead);
    }

    /**
     * 取消操作
     */
    public void cancel() {
        if (textRead != null) {
            textRead.setCancel(true);
        }
        if (future != null) {
            future.cancel(true);
        }
        if (channels != null) {
            channels.removeRead();
            channels.removeCallbacksAndMessages(null);
        }
    }

    /**
     * 同步读取数据
     *
     * @return
     */
    public synchronized String sync() {
        if (!file.exists()) {
            Log.e(TAG, "file is not exist " + file.getAbsolutePath());
            return null;
        }
        StringBuffer content = new StringBuffer();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return content.toString();
    }

}
