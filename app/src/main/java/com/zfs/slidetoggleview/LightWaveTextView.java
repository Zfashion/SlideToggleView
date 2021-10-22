package com.zfs.slidetoggleview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

public class LightWaveTextView extends AppCompatTextView {
    private final Paint mContentPaint = new Paint();
    private final LightWaveDrawable lightWaveDrawable = new LightWaveDrawable();

    public LightWaveTextView(Context context) {
        super(context);
        init(context, null);
    }

    public LightWaveTextView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public LightWaveTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, @Nullable AttributeSet attrs) {
        setWillNotDraw(false);
        lightWaveDrawable.setCallback(this);

        if (attrs == null) {
            setLightWave(new LightWave.AlphaHighlightBuilder().build());
            return;
        }

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.LightWaveTextView, 0, 0);
        try {
            LightWave.Builder shimmerBuilder =
                    a.hasValue(R.styleable.LightWaveTextView_wave_colored)
                            && a.getBoolean(R.styleable.LightWaveTextView_wave_colored, false)
                            ? new LightWave.ColorHighlightBuilder()
                            : new LightWave.AlphaHighlightBuilder();
            setLightWave(shimmerBuilder.consumeAttributes(a).build());
        } finally {
            a.recycle();
        }
    }

    public LightWaveTextView setLightWave(@Nullable LightWave shimmer) {
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
    public boolean isWaveStarted() {
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
