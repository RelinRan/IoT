package androidx.iot.toast;

/**
 * 提示
 */
public interface Tip {

    /**
     * 显示
     *
     * @param backgroundResId 背景颜色
     * @param icon            图标
     * @param msg             消息
     */
    void show(int backgroundResId, int icon, String msg);

    /**
     * 成功
     *
     * @param icon 图标
     * @param msg  消息
     */
    void successful(int icon, String msg);

    /**
     * 成功
     *
     * @param msg 消息
     */
    void successful(String msg);

    /**
     * 失败
     *
     * @param msg 消息
     */
    void failure(String msg);


    /**
     * 信息
     * @param msg
     */
    void message(String msg);

}
