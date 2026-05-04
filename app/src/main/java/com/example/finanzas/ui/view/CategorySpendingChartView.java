package com.example.finanzas.ui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

import com.example.finanzas.R;

import java.util.ArrayList;
import java.util.List;

public class CategorySpendingChartView extends View {

    public static final class Slice {
        public final String label;
        public final float value;
        public final int color;

        public Slice(@NonNull String label, float value, @ColorInt int color) {
            this.label = label;
            this.value = value;
            this.color = color;
        }
    }

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint centerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint totalPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arcBounds = new RectF();
    private final Rect textBounds = new Rect();
    private final List<Slice> slices = new ArrayList<>();

    private String centerLabel = "";
    private String totalText = "";

    public CategorySpendingChartView(Context context) {
        super(context);
        init();
    }

    public CategorySpendingChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CategorySpendingChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void setData(@NonNull List<Slice> newSlices, @NonNull String centerLabel, @NonNull String totalText) {
        slices.clear();
        for (Slice slice : newSlices) {
            if (slice != null && slice.value > 0f) slices.add(slice);
        }
        this.centerLabel = centerLabel;
        this.totalText = totalText;
        invalidate();
    }

    private void init() {
        setWillNotDraw(false);
        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeCap(Paint.Cap.ROUND);

        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeCap(Paint.Cap.BUTT);

        centerPaint.setTextAlign(Paint.Align.CENTER);
        centerPaint.setFakeBoldText(false);

        totalPaint.setTextAlign(Paint.Align.CENTER);
        totalPaint.setFakeBoldText(true);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredHeight = dp(220) + getPaddingTop() + getPaddingBottom();
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int height = heightMode == MeasureSpec.EXACTLY
                ? heightSize
                : Math.min(desiredHeight, heightMode == MeasureSpec.AT_MOST ? heightSize : desiredHeight);
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), height);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        float stroke = dp(22);
        float availableW = getWidth() - getPaddingStart() - getPaddingEnd() - stroke - dp(16);
        float availableH = getHeight() - getPaddingTop() - getPaddingBottom() - stroke - dp(16);
        float diameter = Math.max(0f, Math.min(availableW, availableH));
        float left = (getWidth() - diameter) / 2f;
        float top = getPaddingTop() + (availableH + stroke + dp(16) - diameter) / 2f;
        arcBounds.set(left, top, left + diameter, top + diameter);

        float centerX = arcBounds.centerX();
        float centerY = arcBounds.centerY();

        trackPaint.setStrokeWidth(stroke);
        int trackColor = ColorUtils.setAlphaComponent(
                ContextCompat.getColor(getContext(), R.color.md_theme_outlineVariant),
                170
        );
        trackPaint.setColor(trackColor);
        canvas.drawArc(arcBounds, 0f, 360f, false, trackPaint);

        float total = 0f;
        for (Slice slice : slices) total += slice.value;

        if (total > 0f) {
            arcPaint.setStrokeWidth(stroke);
            float start = -90f;
            for (int i = 0; i < slices.size(); i++) {
                Slice slice = slices.get(i);
                float sweep = 360f * slice.value / total;
                arcPaint.setColor(slice.color);
                arcPaint.setStrokeCap(slices.size() == 1 ? Paint.Cap.ROUND : Paint.Cap.BUTT);
                canvas.drawArc(arcBounds, start, sweep, false, arcPaint);
                start += sweep;
            }
        }

        centerPaint.setColor(ContextCompat.getColor(getContext(), R.color.md_theme_onSurfaceVariant));
        centerPaint.setTextSize(sp(13));
        String label = TextUtils.isEmpty(centerLabel) ? "" : centerLabel;
        centerPaint.getTextBounds(label, 0, label.length(), textBounds);
        canvas.drawText(label, centerX, centerY - dp(8), centerPaint);

        totalPaint.setColor(ContextCompat.getColor(getContext(), R.color.md_theme_onSurface));
        totalPaint.setTextSize(sp(18));
        String totalValue = TextUtils.isEmpty(totalText) ? "" : totalText;
        totalPaint.getTextBounds(totalValue, 0, totalValue.length(), textBounds);
        canvas.drawText(totalValue, centerX, centerY + dp(22), totalPaint);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private float sp(int value) {
        return value * getResources().getDisplayMetrics().scaledDensity;
    }
}
