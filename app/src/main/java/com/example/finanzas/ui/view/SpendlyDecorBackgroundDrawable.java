package com.example.finanzas.ui.view;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class SpendlyDecorBackgroundDrawable extends Drawable {
    private final boolean dark;
    private final float density;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int alpha = 255;

    public SpendlyDecorBackgroundDrawable(@NonNull Context context) {
        dark = (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        density = context.getResources().getDisplayMetrics().density;
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeCap(Paint.Cap.ROUND);
        glowPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        int width = getBounds().width();
        int height = getBounds().height();
        if (width <= 0 || height <= 0) return;

        int background = dark ? Color.rgb(2, 8, 18) : Color.rgb(253, 254, 254);
        int backgroundDeep = dark ? Color.rgb(0, 4, 10) : Color.rgb(245, 250, 247);
        int accent = dark ? Color.rgb(76, 154, 103) : Color.rgb(47, 107, 72);
        int cyan = dark ? Color.rgb(54, 200, 214) : Color.rgb(50, 197, 210);
        int line = dark ? Color.rgb(18, 51, 72) : Color.rgb(215, 236, 217);

        paint.setShader(new LinearGradient(
                0f, 0f, 0f, height,
                withAlpha(background, alpha),
                withAlpha(backgroundDeep, alpha),
                Shader.TileMode.CLAMP
        ));
        canvas.drawRect(0f, 0f, width, height, paint);

        paint.setShader(new RadialGradient(
                width * 0.5f, height * 0.08f, Math.max(width, height) * 0.72f,
                new int[]{
                        withAlpha(dark ? Color.rgb(7, 27, 35) : Color.WHITE, scaleAlpha(190)),
                        withAlpha(accent, scaleAlpha(dark ? 36 : 20)),
                        withAlpha(background, 0)
                },
                new float[]{0f, 0.48f, 1f},
                Shader.TileMode.CLAMP
        ));
        canvas.drawRect(0f, 0f, width, height, paint);
        paint.setShader(null);

        drawSoftArc(canvas, width, height, line, 0.42f, 0.23f, 0.96f, 0.62f, 205f, 140f, dark ? 76 : 92);
        drawSoftArc(canvas, width, height, accent, -0.24f, 0.14f, 1.28f, 0.60f, 198f, 152f, dark ? 82 : 72);
        drawSoftArc(canvas, width, height, cyan, 0.10f, -0.03f, 1.26f, 0.52f, 202f, 108f, dark ? 40 : 34);

        drawGlowDot(canvas, width * 0.18f, height * 0.28f, 2.6f, accent, dark ? 130 : 105);
        drawGlowDot(canvas, width * 0.78f, height * 0.19f, 4.4f, accent, dark ? 140 : 115);
        drawGlowDot(canvas, width * 0.86f, height * 0.33f, 5.5f, accent, dark ? 140 : 120);
        drawGlowDot(canvas, width * 0.52f, height * 0.74f, 3.8f, accent, dark ? 120 : 100);
        drawGlowDot(canvas, width * 0.60f, height * 0.49f, 3.2f, cyan, dark ? 110 : 86);
    }

    private void drawSoftArc(
            @NonNull Canvas canvas,
            int width,
            int height,
            int color,
            float left,
            float top,
            float right,
            float bottom,
            float start,
            float sweep,
            int arcAlpha
    ) {
        strokePaint.setShader(null);
        strokePaint.setStrokeWidth(0.9f * density);
        strokePaint.setColor(withAlpha(color, scaleAlpha(arcAlpha)));
        RectF rect = new RectF(left * width, top * height, right * width, bottom * height);
        canvas.drawArc(rect, start, sweep, false, strokePaint);
    }

    private void drawGlowDot(@NonNull Canvas canvas, float cx, float cy, float radiusDp, int color, int dotAlpha) {
        float radius = radiusDp * density;
        glowPaint.setShader(new RadialGradient(
                cx, cy, radius * 5.5f,
                withAlpha(color, scaleAlpha(Math.min(120, dotAlpha))),
                withAlpha(color, 0),
                Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(cx, cy, radius * 5.5f, glowPaint);
        glowPaint.setShader(null);
        glowPaint.setColor(withAlpha(color, scaleAlpha(dotAlpha)));
        canvas.drawCircle(cx, cy, radius, glowPaint);
    }

    private int scaleAlpha(int value) {
        return Math.max(0, Math.min(255, value * alpha / 255));
    }

    private static int withAlpha(int color, int value) {
        return Color.argb(
                Math.max(0, Math.min(255, value)),
                Color.red(color),
                Color.green(color),
                Color.blue(color)
        );
    }

    @Override
    public void setAlpha(int alpha) {
        this.alpha = Math.max(0, Math.min(255, alpha));
        invalidateSelf();
    }

    @Override
    public void setColorFilter(@Nullable android.graphics.ColorFilter colorFilter) {
        paint.setColorFilter(colorFilter);
        strokePaint.setColorFilter(colorFilter);
        glowPaint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }
}
