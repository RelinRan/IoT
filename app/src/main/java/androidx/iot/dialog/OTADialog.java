package androidx.iot.dialog;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.iot.R;
import androidx.iot.aiot.Alink;
import androidx.iot.aiot.Link;
import androidx.iot.io.Downloader;
import androidx.iot.io.OnDownloadListener;
import androidx.iot.utils.Apk;
import androidx.iot.utils.Shell;
import androidx.iot.widget.Progressbar;

import java.io.File;

/**
 * OTA升级Dialog
 */
public class OTADialog extends IoTDialog implements OnDownloadListener {

    private final String TAG = OTADialog.class.getSimpleName();
    private Downloader downloader;
    private Progressbar progressBar;
    private TextView tvProgress;
    private View divider;
    private TextView btnCancel;

    private Alink alink;
    private boolean aliot = true;
    private InstallHandler handler;

    public OTADialog(@NonNull Context context) {
        super(context);
    }

    @Override
    public int getContentLayoutResId() {
        return R.layout.iot_ota_dialog;
    }

    @Override
    public int getLayoutWidth() {
        return getHeightDisplayMetrics(0.75F);
    }

    @Override
    public int getLayoutHeight() {
        return WRAP_CONTENT;
    }

    @Override
    public void onViewCreated(View view) {
        progressBar = findViewById(R.id.progress_bar);
        tvProgress = findViewById(R.id.tv_progress);
        divider = findViewById(R.id.divider);
        btnCancel = findViewById(R.id.btn_cancel);
        addClick(btnCancel);
        progressBar.setMax(100);
        setCanceledOnTouchOutside(false);
        handler = new InstallHandler();
    }

    @Override
    public void onClick(View v) {
        dismiss();
    }

    /**
     * 设置是否时阿里物联网
     *
     * @param aliot
     */
    public void setAliot(boolean aliot) {
        this.aliot = aliot;
    }

    /**
     * 是否时阿里物联网
     *
     * @return
     */
    public boolean isAliot() {
        return aliot;
    }

    /**
     * 设置是否可点击取消
     *
     * @param enable
     */
    public void setCancel(boolean enable) {
        divider.setVisibility(enable ? View.VISIBLE : View.GONE);
        btnCancel.setVisibility(enable ? View.VISIBLE : View.GONE);
    }

    /**
     * 设置来源国
     *
     * @param url      文件下载地址
     * @param filename 保存的名称
     */
    public void setSource(String url, String filename) {
        if (url == null || TextUtils.isEmpty(url)) {
            return;
        }
        downloader = new Downloader(getContext(), url);
        downloader.setOverride(true);
        downloader.setFileName(filename);
        downloader.setOnDownloadListener(this);
    }


    /**
     * 设置资源
     *
     * @param url         地址
     * @param projectName 项目名称
     * @param dirName     文件夹名称，非沙盒不用设置。
     * @param filename    文件名称
     */
    public void setSource(String url, String projectName, String dirName, String filename) {
        if (url == null || TextUtils.isEmpty(url)) {
            return;
        }
        downloader = new Downloader(getContext(), url);
        downloader.setOverride(true);
        if (!TextUtils.isEmpty(projectName)) {
            downloader.setProjectName(projectName);
        }
        if (!TextUtils.isEmpty(dirName)) {
            downloader.setDirName(dirName);
        }
        downloader.setFileName(filename);
        downloader.setOnDownloadListener(this);
    }

    @Override
    public void show() {
        super.show();
        if (isAliot()) {
            alink = Link.api();
        }
        if (downloader != null) {
            downloader.start();
        }
    }

    @Override
    public void dismiss() {
        if (downloader != null) {
            downloader.cancel();
        }
        super.dismiss();
    }

    @Override
    public void onDownloading(long total, long progress) {
        int value = (int) (progress * 100.0F / total);
        progressBar.setProgress(value);
        tvProgress.setText(value + "%");
    }

    @Override
    public void onDownloadCompleted(File file) {
        progressBar.setProgress(100);
        tvProgress.setText("100%");
        handler.send(0, file);
    }

    @Override
    public void onDownloadFailed(Exception e) {
        dismiss();
        Log.d(TAG, "download failed " + e.toString());
        if (isAliot() && alink != null) {
            alink.publishProgress(-2, "下载失败", null);
        }
    }

    private class InstallHandler extends Handler {

        /**
         * 发送消息
         *
         * @param what 类型
         * @param file 文件
         */
        public void send(int what, File file) {
            Message msg = obtainMessage();
            msg.what = what;
            msg.obj = file.getAbsolutePath();
            sendMessageDelayed(msg, 300);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            File file = new File((String) msg.obj);
            switch (msg.what) {
                case 0:
                    Log.d(TAG, "download completed file = " + file.getAbsolutePath());
                    if (isAliot() && alink != null) {
                        alink.publishProgress(100, "升级完成", null);
                        Log.d(TAG, "upload upgrade finish status");
                    }
                    send(1, file);
                    break;
                case 1:
                    Log.d(TAG, "download progress " + progressBar.getProgress() + "/" + progressBar.getMax() + " install....");
                    Shell.install(getContext(), file);
                    break;
            }
        }
    }
}
