package androidx.iot.text;

public class ChannelsBody {

    private String content;
    private OnReadListener onReadListener;
    private OnWriteListener onWriteListener;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public OnReadListener getOnReadListener() {
        return onReadListener;
    }

    public void setOnReadListener(OnReadListener onReadListener) {
        this.onReadListener = onReadListener;
    }

    public OnWriteListener getOnWriteListener() {
        return onWriteListener;
    }

    public void setOnWriteListener(OnWriteListener onWriteListener) {
        this.onWriteListener = onWriteListener;
    }
}
