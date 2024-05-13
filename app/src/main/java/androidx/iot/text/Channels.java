package androidx.iot.text;

import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;

public class Channels extends Handler {

    public final int WHAT_READ = 1;
    public final int WHAT_WRITE = 2;

    /**
     * 读取
     * @param listener
     * @param content
     */
    public void read(OnReadListener listener, String content) {
        Message message = obtainMessage();
        message.what = WHAT_READ;
        ChannelsBody body = new ChannelsBody();
        body.setContent(content);
        body.setOnReadListener(listener);
        message.obj = body;
        sendMessage(message);
    }

    /**
     * 移除读取
     */
    public void removeRead() {
        removeMessages(WHAT_READ);
    }

    /**
     * 写入
     * @param listener 监听
     * @param content
     */
    public void write(OnWriteListener listener, String content) {
        Message message = obtainMessage();
        message.what = WHAT_WRITE;
        ChannelsBody body = new ChannelsBody();
        body.setContent(content);
        body.setOnWriteListener(listener);
        message.obj = body;
        sendMessage(message);
    }

    /**
     * 移除写入
     */
    public void removeWrite() {
        removeMessages(WHAT_WRITE);
    }

    @Override
    public void handleMessage(@NonNull Message msg) {
        super.handleMessage(msg);
        ChannelsBody body = (ChannelsBody) msg.obj;
        switch (msg.what) {
            case WHAT_READ:
                if (body.getOnReadListener() != null) {
                    body.getOnReadListener().onRead(body.getContent());
                }
                break;
            case WHAT_WRITE:
                if (body.getOnWriteListener() != null) {
                    body.getOnWriteListener().onWrite(body.getContent());
                }
                break;
        }
    }
}
