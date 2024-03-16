package androidx.iot.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.iot.R;

/**
 * 横向进度条
 */
public class Progressbar extends View {

    private Paint paint;
    private float radius = 90;
    private int progressColor = Color.parseColor("#01D1F9");
    private int backgroundColor = Color.parseColor("#010B15");
    private int progress = 0;
    private int max = 100;
    private int width, height;

    public Progressbar(Context context) {
        super(context);
        initialize(context, null);
    }

    public Progressbar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initialize(context, attrs);
    }

    public Progressbar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context, attrs);
    }

    private void initialize(Context context, AttributeSet attrs) {
        paint = new Paint();
        paint.setAntiAlias(true);
        if (attrs!=null){
            TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.Progressbar);
            radius = array.getDimension(R.styleable.Progressbar_radius,radius);
            progressColor = array.getColor(R.styleable.Progressbar_progressColor,progressColor);
            backgroundColor = array.getColor(R.styleable.Progressbar_backgroundColor,backgroundColor);
            progress = array.getInt(R.styleable.Progressbar_progress,progress);
            max = array.getInt(R.styleable.Progressbar_max,max);
            array.recycle();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        width = getMeasuredWidth();
        height = getMeasuredHeight();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        paint.setColor(backgroundColor);
        paint.setStrokeCap(Paint.Cap.ROUND);
        RectF bounds = new RectF(0, 0, width, height);
        canvas.drawRoundRect(bounds, radius, radius, paint);

        paint.setColor(progressColor);
        float percent = 1.0f*progress/max;
        bounds = new RectF(0, 0, width*percent, height);
        canvas.drawRoundRect(bounds, radius, radius, paint);
    }

    public void setRadius(float radius) {
        this.radius = radius;
        invalidate();
    }

    public void setProgressColor(int progressColor) {
        this.progressColor = progressColor;
        invalidate();
    }

    @Override
    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
        invalidate();
    }

    public void setProgress(int progress) {
        this.progress = progress;
        invalidate();
    }

    public void setMax(int max) {
        this.max = max;
        invalidate();
    }

    public int getMax() {
        return max;
    }

    public int getProgress() {
        return progress;
    }
}
