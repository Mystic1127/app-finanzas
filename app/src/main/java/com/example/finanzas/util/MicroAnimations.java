package com.example.finanzas.util;

import android.content.Context;
import android.provider.Settings;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class MicroAnimations {

    private static final DecelerateInterpolator DECELERATE = new DecelerateInterpolator();

    private MicroAnimations() {
    }

    public static boolean areAnimationsEnabled(@Nullable Context context) {
        if (context == null) return true;
        try {
            return Settings.Global.getFloat(
                    context.getContentResolver(),
                    Settings.Global.ANIMATOR_DURATION_SCALE,
                    1f
            ) > 0f;
        } catch (RuntimeException ignored) {
            return true;
        }
    }

    public static void pulseSelection(@Nullable View view) {
        if (view == null) return;
        view.animate().cancel();
        if (!areAnimationsEnabled(view.getContext())) {
            view.setScaleX(1f);
            view.setScaleY(1f);
            return;
        }
        view.setScaleX(1f);
        view.setScaleY(1f);
        view.animate()
                .scaleX(1.045f)
                .scaleY(1.045f)
                .setDuration(90L)
                .setInterpolator(DECELERATE)
                .withEndAction(() -> view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(130L)
                        .setInterpolator(DECELERATE)
                        .start())
                .start();
    }

    public static void pulseAction(@Nullable View view) {
        if (view == null) return;
        view.animate().cancel();
        if (!areAnimationsEnabled(view.getContext())) {
            view.setScaleX(1f);
            view.setScaleY(1f);
            return;
        }
        view.animate()
                .scaleX(0.985f)
                .scaleY(0.985f)
                .setDuration(70L)
                .setInterpolator(DECELERATE)
                .withEndAction(() -> view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(120L)
                        .setInterpolator(DECELERATE)
                        .start())
                .start();
    }

    public static void fadeVisibility(@Nullable View view, boolean visible) {
        if (view == null) return;
        int targetVisibility = visible ? View.VISIBLE : View.GONE;
        if (view.getVisibility() == targetVisibility) {
            view.setAlpha(1f);
            return;
        }

        view.animate().cancel();
        if (!areAnimationsEnabled(view.getContext())) {
            view.setVisibility(targetVisibility);
            view.setAlpha(1f);
            return;
        }

        if (visible) {
            view.setAlpha(0f);
            view.setVisibility(View.VISIBLE);
            view.animate()
                    .alpha(1f)
                    .setDuration(150L)
                    .setInterpolator(DECELERATE)
                    .start();
        } else {
            view.animate()
                    .alpha(0f)
                    .setDuration(120L)
                    .setInterpolator(DECELERATE)
                    .withEndAction(() -> {
                        view.setVisibility(View.GONE);
                        view.setAlpha(1f);
                    })
                    .start();
        }
    }

    public static void cancelAndReset(@NonNull View... views) {
        for (View view : views) {
            if (view == null) continue;
            view.animate().cancel();
            view.setScaleX(1f);
            view.setScaleY(1f);
            view.setAlpha(1f);
        }
    }
}
