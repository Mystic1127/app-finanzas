package com.example.finanzas.util;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

import com.example.finanzas.R;

public final class LabelColorUtils {

    private LabelColorUtils() { }

    @ColorInt
    public static int cardBackground(@NonNull Context context, @ColorInt int labelColor) {
        int base = ContextCompat.getColor(context, R.color.md_theme_surface);
        int accent = accentOnSurface(context, labelColor);
        return ColorUtils.blendARGB(base, accent, isNight(context) ? 0.24f : 0.11f);
    }

    @ColorInt
    public static int cardStroke(@NonNull Context context, @ColorInt int labelColor) {
        int base = ContextCompat.getColor(context, R.color.md_theme_outlineVariant);
        int accent = accentOnSurface(context, labelColor);
        return ColorUtils.blendARGB(base, accent, isNight(context) ? 0.72f : 0.58f);
    }

    @ColorInt
    public static int chipBackground(@NonNull Context context, @ColorInt int labelColor, boolean selected) {
        int base = ContextCompat.getColor(context, selected ? R.color.md_theme_secondaryContainer : R.color.md_theme_surface);
        int accent = accentOnSurface(context, labelColor);
        float amount = isNight(context) ? (selected ? 0.36f : 0.26f) : (selected ? 0.22f : 0.14f);
        return ColorUtils.blendARGB(base, accent, amount);
    }

    @ColorInt
    public static int iconBackground(@NonNull Context context, @ColorInt int labelColor) {
        int base = ContextCompat.getColor(context, R.color.md_theme_surface);
        int accent = accentOnSurface(context, labelColor);
        return ColorUtils.blendARGB(base, accent, isNight(context) ? 0.34f : 0.18f);
    }

    @ColorInt
    public static int accentOnSurface(@NonNull Context context, @ColorInt int labelColor) {
        int surface = ContextCompat.getColor(context, R.color.md_theme_surface);
        return ensureContrast(opaque(labelColor), surface, 2.0);
    }

    @ColorInt
    public static int textOnTint(@NonNull Context context, @ColorInt int labelColor, @ColorInt int background) {
        int accent = accentOnSurface(context, labelColor);
        int readableAccent = ensureContrast(accent, background, 4.5);
        if (ColorUtils.calculateContrast(readableAccent, background) >= 4.5) {
            return readableAccent;
        }
        return ContextCompat.getColor(context, R.color.md_theme_onSurface);
    }

    @ColorInt
    public static int withAlpha(@ColorInt int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    public static boolean isNight(@NonNull Context context) {
        return (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
    }

    @ColorInt
    private static int ensureContrast(@ColorInt int color, @ColorInt int background, double minContrast) {
        color = opaque(color);
        background = opaque(background);
        if (ColorUtils.calculateContrast(color, background) >= minContrast) return color;

        int target = ColorUtils.calculateLuminance(background) < 0.5 ? Color.WHITE : Color.BLACK;
        for (float amount = 0.08f; amount <= 0.96f; amount += 0.08f) {
            int candidate = ColorUtils.blendARGB(color, target, amount);
            if (ColorUtils.calculateContrast(candidate, background) >= minContrast) return candidate;
        }
        return target;
    }

    @ColorInt
    private static int opaque(@ColorInt int color) {
        return Color.rgb(Color.red(color), Color.green(color), Color.blue(color));
    }
}
