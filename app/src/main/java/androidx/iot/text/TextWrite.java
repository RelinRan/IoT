package androidx.iot.text;

import java.io.File;

public class TextWrite implements Runnable {

    private Writer writer;
    private File file;
    private Channels channels;

    private String content;
    private boolean append;
    private OnWriteListener onWriteListener;
    private boolean cancel;

    public TextWrite(Writer writer, File file, Channels channels) {
        this.writer = writer;
        this.file = file;
        this.channels = channels;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setAppend(boolean append) {
        this.append = append;
    }

    public void setOnWriteListener(OnWriteListener onWriteListener) {
        this.onWriteListener = onWriteListener;
    }

    public void setCancel(boolean cancel) {
        this.cancel = cancel;
    }

    @Override
    public void run() {
        if (cancel) {
            return;
        }
        synchronized (file) {
            String value = writer.sync(content, append);
            if (onWriteListener != null) {
                if (channels == null) {
                    channels = new Channels();
                }
                channels.write(onWriteListener, value);
            }
        }
    }

}
