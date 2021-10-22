package com.zfs.slidetoggleview;

import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class LightWaveDrawable extends Drawable {

    private final ValueAnimator.AnimatorUpdateListener mUpdateListener =
            animation -> invalidateSelf();

    private final Paint mWavePaint = new Paint();
    private final Rect mDrawRect = new Rect();
    private final Matrix mShaderMatrix = new Matrix();

    private @Nullable
    ValueAnimator mValueAnimator;

    private @Nullable LightWave lightWave;

    public LightWaveDrawable() {
        mWavePaint.setAntiAlias(true);
    }

    public void setLightWave(@Nullable LightWave lightWave) {
        this.lightWave = lightWave;
        if (this.lightWave != null) {
            mWavePaint.setXfermode(
                    new PorterDuffXfermode(
                            this.lightWave.alphaShimmer ? PorterDuff.Mode.DST_IN : PorterDuff.Mode.SRC_IN));
        }
        updateShader();
        updateValueAnimator();
        invalidateSelf();
    }

    /** Starts the shimmer animation. */
    public void startLightWave() {
        if (mValueAnimator != null && !isWaveStarted() && getCallback() != null) {
            mValueAnimator.start();
        }
    }

    /** Stops the shimmer animation. */
    public void stopLightWave() {
        if (mValueAnimator != null && isWaveStarted()) {
            mValueAnimator.cancel();
        }
    }

    /** Return whether the shimmer animation has been started. */
    public boolean isWaveStarted() {
        return mValueAnimator != null && mValueAnimator.isStarted();
    }

    @Override
    public void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        final int width = bounds.width();
        final int height = bounds.height();
        mDrawRect.set(0, 0, width, height);
        updateShader();
        maybeStartLightWave();
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        if (lightWave == null || mWavePaint.getShader() == null) {
            return;
        }

        final float tiltTan = (float) Math.tan(Math.toRadians(lightWave.tilt));
        final float translateHeight = mDrawRect.height() + tiltTan * mDrawRect.width();
        final float translateWidth = mDrawRect.width() + tiltTan * mDrawRect.height();
        final float dx;
        final float dy;
        final float animatedValue = mValueAnimator != null ? mValueAnimator.getAnimatedFraction() : 0f;
        switch (lightWave.direction) {
            default:
            case LightWave.Direction.LEFT_TO_RIGHT:
                dx = offset(-translateWidth, translateWidth, animatedValue);
                dy = 0;
                break;
            case LightWave.Direction.RIGHT_TO_LEFT:
                dx = offset(translateWidth, -translateWidth, animatedValue);
                dy = 0f;
                break;
            case LightWave.Direction.TOP_TO_BOTTOM:
                dx = 0f;
                dy = offset(-translateHeight, translateHeight, animatedValue);
                break;
            case LightWave.Direction.BOTTOM_TO_TOP:
                dx = 0f;
                dy = offset(translateHeight, -translateHeight, animatedValue);
                break;
        }

        mShaderMatrix.reset();
        mShaderMatrix.setRotate(lightWave.tilt, mDrawRect.width() / 2f, mDrawRect.height() / 2f);
        mShaderMatrix.postTranslate(dx, dy);
        mWavePaint.getShader().setLocalMatrix(mShaderMatrix);
        canvas.drawRect(mDrawRect, mWavePaint);
    }

    @Override
    public void setAlpha(int alpha) {
        // No-op, modify the Shimmer object you pass in instead
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        // No-op, modify the Shimmer object you pass in instead
    }

    @Override
    public int getOpacity() {
        return lightWave != null && (lightWave.clipToChildren || lightWave.alphaShimmer)
                ? PixelFormat.TRANSLUCENT
                : PixelFormat.OPAQUE;
    }

    private float offset(float start, float end, float percent) {
        return start + (end - start) * percent;
    }

    private void updateValueAnimator() {
        if (lightWave == null) {
            return;
        }

        final boolean started;
        if (mValueAnimator != null) {
            started = mValueAnimator.isStarted();
            mValueAnimator.cancel();
            mValueAnimator.removeAllUpdateListeners();
        } else {
            started = false;
        }

        mValueAnimator =
                ValueAnimator.ofFloat(0f, 1f + (float) (lightWave.repeatDelay / lightWave.animationDuration));
        mValueAnimator.setRepeatMode(lightWave.repeatMode);
        mValueAnimator.setRepeatCount(lightWave.repeatCount);
        mValueAnimator.setDuration(lightWave.animationDuration + lightWave.repeatDelay);
        mValueAnimator.addUpdateListener(mUpdateListener);
        if (started) {
            mValueAnimator.start();
        }
    }

    void maybeStartLightWave() {
        if (mValueAnimator != null
                && !mValueAnimator.isStarted()
                && lightWave != null
                && lightWave.autoStart
                && getCallback() != null) {
            mValueAnimator.start();
        }
    }

    private void updateShader() {
        final Rect bounds = getBounds();
        final int boundsWidth = bounds.width();
        final int boundsHeight = bounds.height();
        if (boundsWidth == 0 || boundsHeight == 0 || lightWave == null) {
            return;
        }
        final int width = lightWave.width(boundsWidth);
        final int height = lightWave.height(boundsHeight);

        final Shader shader;
        switch (lightWave.shape) {
            default:
            case LightWave.Shape.LINEAR:
                boolean vertical =
                        lightWave.direction == LightWave.Direction.TOP_TO_BOTTOM
                                || lightWave.direction == LightWave.Direction.BOTTOM_TO_TOP;
                int endX = vertical ? 0 : width;
                int endY = vertical ? height : 0;
                shader =
                        new LinearGradient(
                                0, 0, endX, endY, lightWave.colors, lightWave.positions, Shader.TileMode.CLAMP);
                break;
            case LightWave.Shape.RADIAL:
                shader =
                        new RadialGradient(
                                width / 2f,
                                height / 2f,
                                (float) (Math.max(width, height) / Math.sqrt(2)),
                                lightWave.colors,
                                lightWave.positions,
                                Shader.TileMode.CLAMP);
                break;
        }

        mWavePaint.setShader(shader);
    }
}
