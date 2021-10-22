package com.zfs.slidetoggleview;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class LightWaveFrameLayout extends FrameLayout {
    private final Paint mContentPaint = new Paint();
    private final LightWaveDrawable lightWaveDrawable = new LightWaveDrawable();

    public LightWaveFrameLayout(Context context) {
        super(context);
        init(context, null);
    }

    public LightWaveFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public LightWaveFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public LightWaveFrameLayout(
            Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    private void init(Context context, @Nullable AttributeSet attrs) {
        setWillNotDraw(false);
        lightWaveDrawable.setCallback(this);

        if (attrs == null) {
            setLightWave(new LightWave.AlphaHighlightBuilder().build());
            return;
        }

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.LightWaveFrameLayout, 0, 0);
        try {
            LightWave.Builder builder =
                    a.hasValue(R.styleable.LightWaveFrameLayout_wave_colored)
                            && a.getBoolean(R.styleable.LightWaveFrameLayout_wave_colored, false)
                            ? new LightWave.ColorHighlightBuilder()
                            : new LightWave.AlphaHighlightBuilder();
            setLightWave(builder.consumeAttributes(a).build());
        } finally {
            a.recycle();
        }
    }

    public  LightWaveFrameLayout setLightWave(@Nullable LightWave shimmer) {
        lightWaveDrawable.setLightWave(shimmer);
        if (shimmer != null && shimmer.clipToChildren) {
            setLayerType(LAYER_TYPE_HARDWARE, mContentPaint);
        } else {
            setLayerType(LAYER_TYPE_NONE, null);
        }

        return this;
    }

    /** Starts the shimmer animation. */
    public void startLightWave() {
        lightWaveDrawable.startLightWave();
    }

    /** Stops the shimmer animation. */
    public void stopLightWave() {
        lightWaveDrawable.stopLightWave();
    }

    /** Return whether the shimmer animation has been started. */
    public boolean isShimmerStarted() {
        return lightWaveDrawable.isWaveStarted();
    }

    @Override
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        final int width = getWidth();
        final int height = getHeight();
        lightWaveDrawable.setBounds(0, 0, width, height);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        lightWaveDrawable.maybeStartLightWave();
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopLightWave();
    }

    @Override
    public void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        lightWaveDrawable.draw(canvas);
    }

    @Override
    protected boolean verifyDrawable(@NonNull Drawable who) {
        return super.verifyDrawable(who) || who == lightWaveDrawable;
    }
}
