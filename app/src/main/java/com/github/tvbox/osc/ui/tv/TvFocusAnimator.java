package com.github.tvbox.osc.ui.tv;

import android.view.View;
import android.view.animation.DecelerateInterpolator;

/** Shared, interruptible focus motion for remote navigation. */
public final class TvFocusAnimator {
    private static final DecelerateInterpolator INTERPOLATOR = new DecelerateInterpolator(1.5f);

    private TvFocusAnimator() {
    }

    public static void animate(View view, boolean focused) {
        if (view == null) return;
        view.animate().cancel();
        float scale = focused ? 1.04f : 1.0f;
        view.animate().scaleX(scale).scaleY(scale)
                .setDuration(180).setInterpolator(INTERPOLATOR).start();
    }
}
