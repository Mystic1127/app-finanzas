package com.example.finanzas.ui;

import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.finanzas.R;
import com.example.finanzas.data.api.SettingsService;
import com.example.finanzas.data.model.CategoryChartSlice;
import com.example.finanzas.data.model.HomeSummary;
import com.example.finanzas.data.model.MonthlyTrendPoint;
import com.example.finanzas.ui.view.CategorySpendingChartView;
import com.example.finanzas.ui.view.TrendOverviewView;
import com.example.finanzas.ui.viewmodel.HomeViewModel;
import com.example.finanzas.util.Format;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AnalysisFragment extends Fragment {
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipe;
    private TextView tvPeriod;
    private TextView tvCategoryEmpty;
    private TextView tvTrendEmpty;
    private TextView tvScore;
    private TextView tvInsight;
    private TextView tvSavingStatus;
    private TextView tvSaving;
    private MaterialButton btnFinancialDetail;
    private MaterialButton btnSavingDetail;
    private CategorySpendingChartView chartCategories;
    private TrendOverviewView chartTrend;
    private LinearLayout listCategories;
    private HomeViewModel viewModel;
    private String currencyCode = "PEN";
    private HomeSummary lastSummary;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_analysis, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        swipe = view.findViewById(R.id.swipeAnalysis);
        tvPeriod = view.findViewById(R.id.tvAnalysisPeriod);
        tvCategoryEmpty = view.findViewById(R.id.tvAnalysisCategoryEmpty);
        tvTrendEmpty = view.findViewById(R.id.tvAnalysisTrendEmpty);
        tvScore = view.findViewById(R.id.tvAnalysisScore);
        tvInsight = view.findViewById(R.id.tvAnalysisInsight);
        tvSavingStatus = view.findViewById(R.id.tvAnalysisSavingStatus);
        tvSaving = view.findViewById(R.id.tvAnalysisSaving);
        btnFinancialDetail = view.findViewById(R.id.btnAnalysisFinancialDetail);
        btnSavingDetail = view.findViewById(R.id.btnAnalysisSavingDetail);
        chartCategories = view.findViewById(R.id.chartAnalysisCategories);
        chartTrend = view.findViewById(R.id.chartAnalysisTrend);
        listCategories = view.findViewById(R.id.listAnalysisCategories);
        viewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);
        currencyCode = SettingsService.getCurrencyCode(requireContext());

        swipe.setOnRefreshListener(() -> load(true));
        btnFinancialDetail.setOnClickListener(v -> showFinancialDetail());
        btnSavingDetail.setOnClickListener(v -> showSavingDetail());
        viewModel.getLoading().observe(getViewLifecycleOwner(), loading ->
                swipe.setRefreshing(Boolean.TRUE.equals(loading)));
        viewModel.getCurrencyCode().observe(getViewLifecycleOwner(), code -> {
            if (code != null && !code.trim().isEmpty()) currencyCode = code;
            HomeSummary summary = viewModel.getSummary().getValue();
            if (summary != null && isAdded()) render(summary);
        });
        viewModel.getSummary().observe(getViewLifecycleOwner(), summary -> {
            if (summary != null && isAdded()) render(summary);
        });
        load(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        currencyCode = SettingsService.getCurrencyCode(requireContext());
        load(false);
    }

    private void load(boolean force) {
        Calendar cal = Calendar.getInstance();
        viewModel.loadSummary(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, force);
    }

    private void render(@NonNull HomeSummary summary) {
        lastSummary = summary;
        tvPeriod.setText(Format.monthYear(summary.getAnio(), summary.getMes()));
        renderCategoryChart(summary.getChartCategorias());
        renderTrendChart(summary.getTendenciaMensual());
        tvScore.setText(getString(
                R.string.home_financial_score_value,
                summary.getScoreFinanciero(),
                nonEmpty(summary.getScoreEstado(), getString(R.string.home_financial_missing))
        ));
        tvInsight.setText(nonEmpty(summary.getInsightPrincipal(), getString(R.string.home_financial_missing))
                + "\n" + nonEmpty(summary.getScoreExplicacion(), ""));
        tvSavingStatus.setText(nonEmpty(summary.getEstadoAhorro(), getString(R.string.home_smart_saving_status_adjusted)));
        String saving = summary.getAhorroSugerido() > 0
                ? getString(R.string.home_smart_saving_suggested_value, Format.money(summary.getAhorroSugerido(), currencyCode))
                : getString(R.string.home_smart_saving_not_recommended);
        tvSaving.setText(saving + "\n" + nonEmpty(summary.getRecomendacionAhorroMeta(), getString(R.string.home_smart_saving_goal_empty)));
    }

    private void showFinancialDetail() {
        HomeSummary summary = lastSummary;
        if (summary == null) return;
        showInsightDialog(
                getString(R.string.home_financial_detail_title),
                nonEmpty(summary.getScoreEstado(), getString(R.string.home_financial_missing)),
                nonEmpty(summary.getInsightPrincipal(), getString(R.string.home_financial_missing)),
                "Score",
                summary.getScoreFinanciero() + "/100",
                "Saldo estimado",
                Format.money(summary.getProyeccionFinMes(), currencyCode),
                "Tendencia",
                nonEmpty(summary.getScoreTendencia(), getString(R.string.home_comparison_no_previous)),
                nonEmpty(summary.getScoreExplicacion(), ""),
                nonEmpty(summary.getAlertaPrincipal(), getString(R.string.home_alerts_empty)),
                true
        );
    }

    private void showSavingDetail() {
        HomeSummary summary = lastSummary;
        if (summary == null) return;
        String suggested = summary.getAhorroSugerido() > 0
                ? Format.money(summary.getAhorroSugerido(), currencyCode)
                : getString(R.string.home_smart_saving_not_recommended);
        showInsightDialog(
                getString(R.string.home_smart_saving_detail_title),
                nonEmpty(summary.getEstadoAhorro(), getString(R.string.home_smart_saving_status_adjusted)),
                nonEmpty(summary.getAhorroSugeridoMensaje(), getString(R.string.home_smart_saving_goal_empty)),
                "Ahorro sugerido",
                suggested,
                "Saldo actual",
                Format.money(summary.getSaldoActualTotal(), currencyCode),
                "Saldo final",
                Format.money(summary.getProyeccionFinMes(), currencyCode),
                nonEmpty(summary.getRecomendacionAhorroMeta(), getString(R.string.home_smart_saving_goal_empty)),
                "Gasto proyectado: " + Format.money(summary.getGastoProyectado(), currencyCode),
                false
        );
    }

    private void showInsightDialog(
            @NonNull String title,
            @NonNull String status,
            @NonNull String hero,
            @NonNull String metricOneLabel,
            @NonNull String metricOneValue,
            @NonNull String metricTwoLabel,
            @NonNull String metricTwoValue,
            @NonNull String metricThreeLabel,
            @NonNull String metricThreeValue,
            @NonNull String recommendation,
            @NonNull String brief,
            boolean showActionable
    ) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        ScrollView scroll = new ScrollView(requireContext());
        scroll.setFillViewport(false);
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(10), dp(20), dp(24));
        scroll.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        View handle = new View(requireContext());
        GradientDrawable handleBg = new GradientDrawable();
        handleBg.setColor(ContextCompat.getColor(requireContext(), R.color.md_theme_outlineVariant));
        handleBg.setCornerRadius(dp(3));
        handle.setBackground(handleBg);
        LinearLayout.LayoutParams handleParams = new LinearLayout.LayoutParams(dp(52), dp(5));
        handleParams.gravity = android.view.Gravity.CENTER_HORIZONTAL;
        root.addView(handle, handleParams);

        TextView titleView = new TextView(requireContext());
        titleView.setText(title);
        titleView.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurface));
        titleView.setTextSize(24f);
        titleView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(titleView, fullWidthParams(28));

        root.addView(heroCard(status, hero), fullWidthParams(24));

        LinearLayout metrics = new LinearLayout(requireContext());
        metrics.setOrientation(LinearLayout.HORIZONTAL);
        metrics.addView(metricView(metricOneLabel, metricOneValue), weightedMetricParams(0));
        metrics.addView(metricView(metricTwoLabel, metricTwoValue), weightedMetricParams(12));
        root.addView(metrics, fullWidthParams(22));
        root.addView(metricView(metricThreeLabel, metricThreeValue), halfWidthParams(12));

        root.addView(sectionText("Recomendacion principal", recommendation, true), fullWidthParams(24));
        root.addView(sectionText("Lectura breve", brief, false), fullWidthParams(18));
        if (showActionable) {
            root.addView(sectionText("Aviso accionable", recommendation, true), fullWidthParams(18));
        }

        MaterialButton accept = new MaterialButton(requireContext());
        accept.setText(R.string.home_detail_accept);
        accept.setAllCaps(false);
        accept.setOnClickListener(v -> dialog.dismiss());
        LinearLayout.LayoutParams acceptParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56));
        acceptParams.topMargin = dp(28);
        root.addView(accept, acceptParams);

        dialog.setContentView(scroll);
        dialog.setOnShowListener(d -> {
            View sheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (sheet != null) {
                sheet.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.dialog_surface));
                ViewGroup.LayoutParams params = sheet.getLayoutParams();
                params.height = (int) (getResources().getDisplayMetrics().heightPixels * 0.92f);
                sheet.setLayoutParams(params);
                com.google.android.material.bottomsheet.BottomSheetBehavior.from(sheet).setState(
                        com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
                );
            }
        });
        dialog.show();
    }

    private View heroCard(@NonNull String status, @NonNull String hero) {
        LinearLayout card = cardContainer(true);
        TextView statusView = new TextView(requireContext());
        statusView.setText(status);
        statusView.setTextColor(ContextCompat.getColor(requireContext(), R.color.risk_medium_text));
        statusView.setTextSize(14f);
        statusView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        statusView.setPadding(dp(12), dp(7), dp(12), dp(7));
        GradientDrawable chipBg = new GradientDrawable();
        chipBg.setColor(ContextCompat.getColor(requireContext(), R.color.md_theme_surface));
        chipBg.setStroke(dp(1), ContextCompat.getColor(requireContext(), R.color.risk_medium_text));
        chipBg.setCornerRadius(dp(12));
        statusView.setBackground(chipBg);
        card.addView(statusView, wrapParams(0));

        TextView heroView = new TextView(requireContext());
        heroView.setText(hero);
        heroView.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurface));
        heroView.setTextSize(20f);
        heroView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams heroParams = fullWidthParams(20);
        card.addView(heroView, heroParams);
        return card;
    }

    private TextView metricView(@NonNull String label, @NonNull String value) {
        TextView view = new TextView(requireContext());
        view.setText(label + "\n" + value);
        view.setTextSize(14f);
        view.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurface));
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setPadding(dp(14), dp(14), dp(14), dp(14));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(ContextCompat.getColor(requireContext(), R.color.md_theme_surface));
        bg.setStroke(dp(1), ContextCompat.getColor(requireContext(), R.color.md_theme_outlineVariant));
        bg.setCornerRadius(dp(18));
        view.setBackground(bg);
        return view;
    }

    private TextView sectionText(@NonNull String label, @NonNull String value, boolean highlighted) {
        TextView view = new TextView(requireContext());
        view.setText(label + "\n" + value);
        view.setTextSize(highlighted ? 16f : 15f);
        view.setTextColor(ContextCompat.getColor(requireContext(), highlighted ? R.color.risk_medium_text : R.color.md_theme_onSurfaceVariant));
        if (highlighted) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setPadding(dp(16), dp(16), dp(16), dp(16));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(ContextCompat.getColor(requireContext(), highlighted ? R.color.risk_medium_bg : R.color.dialog_surface));
        bg.setStroke(highlighted ? 0 : dp(1), ContextCompat.getColor(requireContext(), R.color.md_theme_outlineVariant));
        bg.setCornerRadius(dp(20));
        view.setBackground(bg);
        return view;
    }

    private LinearLayout cardContainer(boolean highlighted) {
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(18), dp(16), dp(18));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(ContextCompat.getColor(requireContext(), highlighted ? R.color.risk_medium_bg : R.color.md_theme_surface));
        bg.setStroke(dp(1), ContextCompat.getColor(requireContext(), R.color.md_theme_outlineVariant));
        bg.setCornerRadius(dp(22));
        card.setBackground(bg);
        return card;
    }

    private LinearLayout.LayoutParams fullWidthParams(int topMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(topMargin);
        return params;
    }

    private LinearLayout.LayoutParams weightedMetricParams(int leftMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(102), 1f);
        params.leftMargin = dp(leftMargin);
        return params;
    }

    private LinearLayout.LayoutParams halfWidthParams(int topMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams((int) (getResources().getDisplayMetrics().widthPixels * 0.46f), dp(102));
        params.topMargin = dp(topMargin);
        return params;
    }

    private LinearLayout.LayoutParams wrapParams(int topMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(topMargin);
        return params;
    }

    private void renderCategoryChart(@Nullable List<CategoryChartSlice> slices) {
        List<CategoryChartSlice> safe = slices == null ? new ArrayList<>() : slices;
        ArrayList<CategorySpendingChartView.Slice> entries = new ArrayList<>();
        double total = 0.0;
        int colorIndex = 0;
        for (CategoryChartSlice slice : safe) {
            if (slice == null || slice.getGastado() <= 0) continue;
            String name = safeLabel(slice.getCategoriaNombre());
            entries.add(new CategorySpendingChartView.Slice(
                    name,
                    (float) slice.getGastado(),
                    colorForCategoryChart(colorIndex++, name)
            ));
            total += slice.getGastado();
        }
        boolean empty = entries.isEmpty();
        tvCategoryEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        chartCategories.setVisibility(empty ? View.GONE : View.VISIBLE);
        listCategories.setVisibility(empty ? View.GONE : View.VISIBLE);
        listCategories.removeAllViews();
        chartCategories.setData(entries, getString(R.string.home_category_chart_center), empty ? "" : Format.money(total, currencyCode));
        if (!empty && total > 0) {
            for (CategorySpendingChartView.Slice entry : entries) {
                addCategoryRow(entry, entry.value / (float) total);
            }
        }
    }

    private void renderTrendChart(@Nullable List<MonthlyTrendPoint> points) {
        List<MonthlyTrendPoint> safe = points == null ? new ArrayList<>() : points;
        boolean hasValues = false;
        ArrayList<TrendOverviewView.TrendPoint> chartPoints = new ArrayList<>();
        for (MonthlyTrendPoint point : safe) {
            if (point == null) continue;
            chartPoints.add(new TrendOverviewView.TrendPoint(
                    point.getEtiqueta() == null ? "" : point.getEtiqueta(),
                    (float) point.getIngresos(),
                    (float) point.getGastos(),
                    (float) point.getSaldo()
            ));
            if (point.getIngresos() != 0 || point.getGastos() != 0 || point.getSaldo() != 0) hasValues = true;
        }
        boolean empty = safe.isEmpty() || !hasValues;
        tvTrendEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        chartTrend.setVisibility(empty ? View.GONE : View.VISIBLE);
        chartTrend.setData(chartPoints, currencyCode);
    }

    private void addCategoryRow(@NonNull CategorySpendingChartView.Slice entry, float ratio) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(8), 0, dp(8));

        View dot = new View(requireContext());
        GradientDrawable marker = new GradientDrawable();
        marker.setShape(GradientDrawable.OVAL);
        marker.setColor(entry.color);
        dot.setBackground(marker);
        row.addView(dot, new LinearLayout.LayoutParams(dp(10), dp(10)));

        TextView name = new TextView(requireContext());
        name.setText(entry.label);
        name.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        name.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurface));
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        nameParams.leftMargin = dp(10);
        row.addView(name, nameParams);

        TextView value = new TextView(requireContext());
        value.setText(Math.round(ratio * 100f) + "%");
        value.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurfaceVariant));
        row.addView(value);
        listCategories.addView(row);
    }

    private int colorForCategoryChart(int index, @NonNull String name) {
        int[] palette = new int[]{
                R.color.chart_pie_1, R.color.chart_pie_2, R.color.chart_pie_3, R.color.chart_pie_4,
                R.color.chart_pie_5, R.color.chart_pie_6, R.color.chart_pie_7, R.color.chart_pie_8,
                R.color.chart_pie_9, R.color.chart_pie_10, R.color.chart_pie_11, R.color.chart_pie_12,
                R.color.chart_pie_13, R.color.chart_pie_14, R.color.chart_pie_15, R.color.chart_pie_16
        };
        if (index < palette.length) return ContextCompat.getColor(requireContext(), palette[index]);
        int stable = Math.abs(name.toLowerCase(Locale.ROOT).hashCode());
        return ContextCompat.getColor(requireContext(), palette[stable % palette.length]);
    }

    private String safeLabel(@Nullable String value) {
        String clean = value == null ? "" : value.trim();
        return clean.isEmpty() ? getString(R.string.home_uncategorized) : clean;
    }

    private String nonEmpty(@Nullable String value, @NonNull String fallback) {
        String clean = value == null ? "" : value.trim();
        return clean.isEmpty() ? fallback : clean;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
