package androidx.iot.text;

import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;

public class Channels extends Handler {

    public void read(OnReadListener listener,String content){
        Message message = obtainMessage();
        message.what = 1;
        ChannelsBody body = new ChannelsBody();
        body.setContent(content);
        body.setOnReadListener(listener);
        message.obj = body;
        sendMessage(message);
    }

    public void write(OnWriteListener listener,String content){
        Message message = obtainMessage();
        message.what = 2;
        ChannelsBody body = new ChannelsBody();
        body.setContent(content);
        body.setOnWriteListener(listener);
        message.obj = body;
        sendMessage(message);
    }

    @Override
    public void handleMessage(@NonNull Message msg) {
        super.handleMessage(msg);
        ChannelsBody body = (ChannelsBody) msg.obj;
        switch (msg.what){
            case 1:
                body.getOnReadListener().onRead(body.getContent());
                break;
            case 2:
                body.getOnWriteListener().onWrite(body.getContent());
                break;
        }
    }
}
