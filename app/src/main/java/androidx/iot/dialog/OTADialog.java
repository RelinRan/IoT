package androidx.iot.dialog;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.iot.R;
import androidx.iot.aiot.Link;
import androidx.iot.io.Downloader;
import androidx.iot.io.OnDownloadListener;
import androidx.iot.utils.Apk;
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
    private TextView btnCancel;

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
        btnCancel = findViewById(R.id.btn_cancel);
        addClick(btnCancel);
        progressBar.setMax(100);
    }

    @Override
    public void onClick(View v) {
        dismiss();
    }

    /**
     * 设置来源国
     * @param url      文件下载地址
     * @param filename 保存的名称
     */
    public void setSource(String url, String filename) {
        downloader = new Downloader(getContext(), url);
        downloader.setOverride(true);
        downloader.setFileName(filename);
        downloader.setOnDownloadListener(this);
    }

    @Override
    public void show() {
        super.show();
        if (downloader != null) {
            downloader.start();
        }
    }

    @Override
    public void onDownloading(long total, long progress) {
        int value = (int) (progress * 100.0F / total);
        progressBar.setProgress(value);
        tvProgress.setText(value + "%");
    }

    @Override
    public void onDownloadCompleted(File file) {
        dismiss();
        Link.mqtt().api().publishProgress(100, "升级完成", null);
        Log.d(TAG, "download completed file = " + file.getAbsolutePath());
        Apk.install(getContext(), file.getAbsolutePath());
    }

    @Override
    public void onDownloadFailed(Exception e) {
        dismiss();
        Log.d(TAG, "download failed " + e.toString());
        Link.mqtt().api().publishProgress(-2, "下载失败", null);
    }

}
