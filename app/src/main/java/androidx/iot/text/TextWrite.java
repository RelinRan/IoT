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

    public void setFile(File file) {
        this.file = file;
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
        if (writer != null) {
            writer.setFile(file);
            String value = writer.sync(content, append);
            if (onWriteListener != null) {
                channels.write(onWriteListener, value);
            }
        }
    }

}
