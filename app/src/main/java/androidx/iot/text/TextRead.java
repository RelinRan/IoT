package androidx.iot.text;

import java.io.File;

public class TextRead implements Runnable {

    private File file;
    private Reader reader;
    private Channels channels;

    private OnReadListener onReadListener;
    private boolean cancel;

    public TextRead(File file, Reader reader, Channels channels) {
        this.file = file;
        this.reader = reader;
        this.channels = channels;
    }

    public void setOnReadListener(OnReadListener onReadListener) {
        this.onReadListener = onReadListener;
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
            String content = reader.sync();
            if (onReadListener != null) {
                channels.read(onReadListener, content.toString());
            }
        }
    }


}
