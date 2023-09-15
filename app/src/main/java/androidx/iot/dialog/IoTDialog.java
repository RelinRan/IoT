package androidx.iot.dialog;

import android.app.Dialog;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.iot.R;

/**
 * 基础Dialog
 */
public abstract class IoTDialog extends Dialog implements View.OnClickListener{

    /**
     * 占比100%
     */
    public static final int MATCH_PARENT = ViewGroup.LayoutParams.MATCH_PARENT;
    /**
     * 自适应
     */
    public static final int WRAP_CONTENT = ViewGroup.LayoutParams.WRAP_CONTENT;
    /**
     * 半透明背景Style
     */
    public static final int THEME_TRANSLUCENT = R.style.IoT_Theme_Dialog_Translucent_Background;
    /**
     * 全透明背景的Style
     */
    public static final int THEME_TRANSPARENT = R.style.IoT_Theme_Dialog_Transparent_Background;
    /**
     * 底部动画
     */
    public static final int WINDOW_ANIM_BOTTOM = R.style.IoT_Window_Animation_Bottom;
    /**
     * 缩放动画
     */
    public static final int WINDOW_ANIM_SCALE = R.style.IoT_Window_Animation_Scale;
    /**
     * 内容视图
     */
    private View contentView;

    public IoTDialog(@NonNull Context context) {
        this(context,THEME_TRANSLUCENT);
    }

    public IoTDialog(@NonNull Context context, int themeResId) {
        super(context, themeResId);
        initialize(context);
    }

    protected IoTDialog(@NonNull Context context, boolean cancelable, @Nullable OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
        initialize(context);
    }

    /**
     * 初始化
     *
     * @param context    上下文
     */
    protected void initialize(Context context) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        contentView = LayoutInflater.from(context).inflate(getContentLayoutResId(), null);
        setContentView(contentView);
        onViewCreated(contentView);
        Window window = getWindow();
        window.setGravity(getGravity());
        window.setWindowAnimations(getWindowAnimationResId());
        window.setLayout(getLayoutWidth(), getLayoutHeight());
    }

    /**
     * 内容布局资源ID
     *
     * @return
     */
    public abstract int getContentLayoutResId();

    /**
     * 获取布局宽
     *
     * @return
     */
    public abstract int getLayoutWidth();

    /**
     * 获取布局高度
     *
     * @return
     */
    public abstract int getLayoutHeight();

    /**
     * 显示位置
     *
     * @return
     */
    public int getGravity() {
        return Gravity.CENTER;
    }

    /**
     * 获取主题资源
     *
     * @return
     */

    public int getThemeResId() {
        return THEME_TRANSPARENT;
    }

    /**
     * 获取样式资源
     *
     * @return
     */
    public int getWindowAnimationResId() {
        return WINDOW_ANIM_SCALE;
    }

    /**
     * 绑定控件
     *
     * @param contentView
     */
    public abstract void onViewCreated(View contentView);

    /**
     * 通过屏幕比例获取宽度
     *
     * @param scale 占比
     * @return
     */
    public int getWidthByDisplayMetrics(float scale) {
        return (int) (getContext().getResources().getDisplayMetrics().widthPixels * scale);
    }

    /**
     * 通过屏幕比例获取高度
     *
     * @param scale 占比
     * @return
     */
    public int getHeightDisplayMetrics(float scale) {
        return (int) (getContext().getResources().getDisplayMetrics().heightPixels * scale);
    }

    @Override
    public void onClick(View v) {

    }

    /**
     * 添加点击事件
     * @param ids view ids
     */
    protected void addClick(int... ids) {
        for (int i = 0; i < ids.length; i++) {
            findViewById(ids[i]).setOnClickListener(this);
        }
    }

    /**
     * 添加点击事件
     * @param views views
     */
    protected void addClick(View... views) {
        for (int i = 0; i < views.length; i++) {
            views[i].setOnClickListener(this);
        }
    }

    /**
     * 查找控件
     *
     * @param id  view id
     * @param <T>
     * @return
     */
    public final <T extends View> T findViewById(@IdRes int id) {
        if (id == View.NO_ID) {
            return null;
        }
        return contentView.findViewById(id);
    }

}
