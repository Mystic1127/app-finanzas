package com.example.finanzas.ui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
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
import com.example.finanzas.data.api.SettingsService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TrendOverviewView extends View {

    public static final class TrendPoint {
        public final String label;
        public final float income;
        public final float expense;
        public final float balance;

        public TrendPoint(@NonNull String label, float income, float expense, float balance) {
            this.label = label;
            this.income = income;
            this.expense = expense;
            this.balance = balance;
        }
    }

    private final Paint surfacePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF surface = new RectF();
    private final Path linePath = new Path();
    private final List<TrendPoint> points = new ArrayList<>();

    private String currencyCode = "PEN";

    public TrendOverviewView(Context context) {
        super(context);
        init();
    }

    public TrendOverviewView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TrendOverviewView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void setData(@NonNull List<TrendPoint> newPoints, @NonNull String currencyCode) {
        if (sameData(newPoints, currencyCode)) return;
        points.clear();
        for (TrendPoint point : newPoints) {
            if (point != null) points.add(point);
        }
        this.currencyCode = currencyCode;
        invalidate();
    }

    private boolean sameData(@NonNull List<TrendPoint> newPoints, @NonNull String newCurrencyCode) {
        if (!TextUtils.equals(currencyCode, newCurrencyCode)) return false;
        int filteredIndex = 0;
        for (TrendPoint point : newPoints) {
            if (point == null) continue;
            if (filteredIndex >= points.size()) return false;
            TrendPoint current = points.get(filteredIndex);
            if (!TextUtils.equals(current.label, point.label)
                    || Float.compare(current.income, point.income) != 0
                    || Float.compare(current.expense, point.expense) != 0
                    || Float.compare(current.balance, point.balance) != 0) {
                return false;
            }
            filteredIndex++;
        }
        return filteredIndex == points.size();
    }

    private void init() {
        setWillNotDraw(false);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(dp(1));

        textPaint.setTextSize(sp(10));

        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(dp(2));
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);

        pointPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredHeight = dp(260) + getPaddingTop() + getPaddingBottom();
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
        surface.set(getPaddingStart(), getPaddingTop(), getWidth() - getPaddingEnd(), getHeight() - getPaddingBottom());
        float radius = dp(18);

        int surfaceColor = ContextCompat.getColor(getContext(), R.color.md_theme_surfaceContainerLow);
        int outline = ContextCompat.getColor(getContext(), R.color.md_theme_outlineVariant);
        surfacePaint.setStyle(Paint.Style.FILL);
        surfacePaint.setColor(surfaceColor);
        canvas.drawRoundRect(surface, radius, radius, surfacePaint);
        surfacePaint.setStyle(Paint.Style.STROKE);
        surfacePaint.setStrokeWidth(dp(1));
        surfacePaint.setColor(outline);
        canvas.drawRoundRect(surface, radius, radius, surfacePaint);

        if (points.isEmpty()) return;

        float left = surface.left + dp(52);
        float right = surface.right - dp(16);
        float top = surface.top + dp(24);
        float bottom = surface.bottom - dp(44);
        if (right <= left || bottom <= top) return;

        float min = 0f;
        float max = 0f;
        for (TrendPoint point : points) {
            max = Math.max(max, Math.max(point.income, Math.max(point.expense, point.balance)));
            min = Math.min(min, Math.min(point.income, Math.min(point.expense, point.balance)));
        }
        if (max == min) {
            max += 1f;
        } else {
            float padding = (max - min) * 0.16f;
            max += padding;
            min -= padding;
        }
        if (min > 0f) min = 0f;

        drawGrid(canvas, left, right, top, bottom, min, max);
        drawSeries(canvas, left, right, top, bottom, min, max, Series.INCOME,
                ContextCompat.getColor(getContext(), R.color.chart_line_income));
        drawSeries(canvas, left, right, top, bottom, min, max, Series.EXPENSE,
                ContextCompat.getColor(getContext(), R.color.chart_line_expense));
        drawSeries(canvas, left, right, top, bottom, min, max, Series.BALANCE,
                ContextCompat.getColor(getContext(), R.color.chart_line_balance));
        drawLabels(canvas, left, right, bottom);
    }

    private void drawGrid(Canvas canvas, float left, float right, float top, float bottom, float min, float max) {
        int text = ContextCompat.getColor(getContext(), R.color.md_theme_onSurfaceVariant);
        int grid = ColorUtils.setAlphaComponent(ContextCompat.getColor(getContext(), R.color.md_theme_outlineVariant), 150);
        gridPaint.setColor(grid);
        textPaint.setColor(text);
        textPaint.setTextAlign(Paint.Align.RIGHT);

        for (int i = 0; i <= 3; i++) {
            float ratio = i / 3f;
            float value = max - ((max - min) * ratio);
            float y = top + (bottom - top) * ratio;
            canvas.drawLine(left, y, right, y, gridPaint);
            canvas.drawText(shortMoney(value), left - dp(8), y + dp(4), textPaint);
        }

        if (min < 0f && max > 0f) {
            float zeroY = yFor(0f, top, bottom, min, max);
            gridPaint.setColor(ContextCompat.getColor(getContext(), R.color.md_theme_outline));
            canvas.drawLine(left, zeroY, right, zeroY, gridPaint);
        }
    }

    private void drawLabels(Canvas canvas, float left, float right, float bottom) {
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(ContextCompat.getColor(getContext(), R.color.md_theme_onSurfaceVariant));
        int count = points.size();
        int step = count <= 6 ? 1 : Math.max(1, (int) Math.ceil(count / 5f));
        for (int i = 0; i < count; i++) {
            boolean show = i == 0 || i == count - 1 || i % step == 0;
            if (!show) continue;
            canvas.drawText(points.get(i).label, xFor(i, count, left, right), bottom + dp(24), textPaint);
        }
    }

    private void drawSeries(
            Canvas canvas,
            float left,
            float right,
            float top,
            float bottom,
            float min,
            float max,
            Series series,
            @ColorInt int color
    ) {
        int count = points.size();
        if (count == 0) return;
        linePath.reset();
        for (int i = 0; i < count; i++) {
            float x = xFor(i, count, left, right);
            float y = yFor(valueFor(points.get(i), series), top, bottom, min, max);
            if (i == 0) linePath.moveTo(x, y);
            else linePath.lineTo(x, y);
        }
        linePaint.setColor(color);
        canvas.drawPath(linePath, linePaint);

        pointPaint.setColor(color);
        for (int i = 0; i < count; i++) {
            float x = xFor(i, count, left, right);
            float y = yFor(valueFor(points.get(i), series), top, bottom, min, max);
            float radius = i == count - 1 ? dp(4) : dp(2);
            canvas.drawCircle(x, y, radius, pointPaint);
        }
    }

    private float valueFor(@NonNull TrendPoint point, @NonNull Series series) {
        switch (series) {
            case INCOME:
                return point.income;
            case EXPENSE:
                return point.expense;
            default:
                return point.balance;
        }
    }

    private float xFor(int index, int count, float left, float right) {
        if (count <= 1) return (left + right) / 2f;
        return left + (right - left) * index / (count - 1f);
    }

    private float yFor(float value, float top, float bottom, float min, float max) {
        return top + (max - value) * (bottom - top) / (max - min);
    }

    private String shortMoney(float value) {
        String symbol = SettingsService.getCurrencySymbol(currencyCode);
        float abs = Math.abs(value);
        String sign = value < 0 ? "-" : "";
        if (abs >= 1000f) {
            return sign + symbol + String.format(Locale.US, "%.1fk", abs / 1000f);
        }
        return sign + symbol + Math.round(abs);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private float sp(int value) {
        return value * getResources().getDisplayMetrics().scaledDensity;
    }

    private enum Series {
        INCOME,
        EXPENSE,
        BALANCE
    }
}
