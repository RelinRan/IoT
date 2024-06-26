package androidx.iot.aiot;

public interface OnMediaLicenseListener {

    /**
     * 三元组许可读取完成
     *
     * @param content 三元组许可内容
     */
    void onMediaLicenseGranted(String content);

}
