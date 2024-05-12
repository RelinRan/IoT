package androidx.iot.aiot;

import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.iot.entity.DynamicBody;

public class DynamicHandler extends Handler {

    private final int WHAT_RECEIVED = 1;
    private final int WHAT_FAILURE = 2;

    /**
     * 接收
     *
     * @param body
     */
    public void received(DynamicBody body) {
        Message message = obtainMessage();
        message.what = WHAT_RECEIVED;
        message.obj = body;
        sendMessage(message);
    }

    /**
     * 移除接收消息
     */
    public void removeReceived() {
        removeMessages(WHAT_RECEIVED);
    }

    /**
     * 失败
     *
     * @param body
     */
    public void failure(DynamicBody body) {
        Message message = obtainMessage();
        message.what = WHAT_FAILURE;
        message.obj = body;
        sendMessage(message);
    }

    /**
     * 移除失败消息
     */
    public void removeFailure() {
        removeMessages(WHAT_FAILURE);
    }

    @Override
    public void handleMessage(@NonNull Message msg) {
        super.handleMessage(msg);
        DynamicBody body = (DynamicBody) msg.obj;
        switch (msg.what) {
            case WHAT_RECEIVED:
                if (body != null && body.getListener() != null) {
                    body.getListener().onDynamicRegisterReceived(body.getTopic(), body.getPayload());
                }
                break;
            case WHAT_FAILURE:
                if (body != null && body.getListener() != null) {
                    body.getListener().onDynamicRegisterFailure(body.getException());
                }
                break;
        }
    }

}
