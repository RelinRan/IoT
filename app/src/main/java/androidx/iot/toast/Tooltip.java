package androidx.iot.toast;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.iot.R;

/**
 * 提供简洁、即时的信息
 */
public class Tooltip implements Tip {

    private Context context;
    private Toast toast;
    private static Tooltip instance;

    private Tooltip(Context context) {
        this.context = context;
        View layout = LayoutInflater.from(context).inflate(R.layout.iot_tips, null, false);
        toast = new Toast(context);
        toast.setView(layout);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.BOTTOM, 0, context.getResources().getDimensionPixelOffset(R.dimen.iot_toast_radius_bottom));
    }

    public static Tooltip make(Context context) {
        if (instance == null) {
            synchronized (Tooltip.class) {
                if (instance == null) {
                    instance = new Tooltip(context);
                }
            }
        }
        return instance;
    }

    public Context getContext() {
        return context;
    }

    @Override
    public void show(int backgroundResId, int icon, String msg) {
        if (toast == null) {
            return;
        }
        View layout = toast.getView();
        LinearLayout ll_background = layout.findViewById(R.id.ll_background);
        ImageView iv_icon = layout.findViewById(R.id.iv_icon);
        TextView tv_msg = layout.findViewById(R.id.tv_msg);
        if (icon == 0) {
            iv_icon.setVisibility(View.GONE);
        } else {
            iv_icon.setImageResource(icon);
        }
        ll_background.setBackgroundResource(backgroundResId);
        tv_msg.setText(msg);
        toast.show();
    }

    @Override
    public void success(int icon, String msg) {
        show(R.drawable.iot_tips_msg_bg, icon, msg);
    }

    @Override
    public void success(String msg) {
        success(R.mipmap.ic_iot_icon, msg);
    }

    @Override
    public void failure(String msg) {
        show(R.drawable.iot_tips_failure_bg, 0, msg);
    }

    @Override
    public void message(String msg) {
        show(R.drawable.iot_tips_msg_bg, 0, msg);
    }

}
