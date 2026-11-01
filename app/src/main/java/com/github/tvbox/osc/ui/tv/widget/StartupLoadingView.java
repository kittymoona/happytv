package com.github.tvbox.osc.ui.tv.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;
import androidx.core.content.ContextCompat;
import com.github.tvbox.osc.R;

/** Branded startup overlay; its animation runs only while the overlay is visible. */
public final class StartupLoadingView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Drawable logo;
    private final String message;
    private final ValueAnimator animator;
    private float phase;

    public StartupLoadingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        logo = ContextCompat.getDrawable(context, R.drawable.app_icon);
        message = context.getString(R.string.startup_loading);
        setBackgroundResource(R.drawable.tv_background);
        setContentDescription(context.getString(R.string.app_name) + "，" + message);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        setClickable(true);
        animator = ValueAnimator.ofFloat(0f, (float) (Math.PI * 2));
        animator.setDuration(1600);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(value -> {
            phase = (float) value.getAnimatedValue();
            invalidate();
        });
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float scale = Math.min(getWidth() / 1280f, getHeight() / 720f);
        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;
        int halfLogo = Math.round(120 * scale);
        int logoY = Math.round(centerY - 45 * scale);
        logo.setBounds(Math.round(centerX) - halfLogo, logoY - halfLogo,
                Math.round(centerX) + halfLogo, logoY + halfLogo);
        logo.draw(canvas);
        paint.setColor(Color.WHITE);
        paint.setTextSize(32 * scale);
        float x = centerX - paint.measureText(message) / 2;
        float baseline = centerY + 140 * scale;
        for (int i = 0; i < message.length(); i++) {
            float wave = (float) Math.sin(phase - i * 0.48f);
            paint.setAlpha(Math.round(210 + 45 * wave));
            canvas.drawText(message, i, i + 1, x, baseline - wave * 3 * scale, paint);
            x += paint.measureText(message, i, i + 1);
        }
        paint.setAlpha(255);
    }

    private void updateAnimation() {
        if (animator == null) return;
        if (isAttachedToWindow() && isShown() && getWindowVisibility() == VISIBLE) {
            if (!animator.isStarted()) animator.start();
        } else {
            animator.cancel();
        }
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        updateAnimation();
    }

    @Override protected void onVisibilityChanged(View view, int visibility) {
        super.onVisibilityChanged(view, visibility);
        updateAnimation();
    }

    @Override protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        updateAnimation();
    }

    @Override protected void onDetachedFromWindow() {
        animator.cancel();
        super.onDetachedFromWindow();
    }
}
