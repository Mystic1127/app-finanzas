package com.example.finanzas.ui;

import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
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
import com.example.finanzas.util.CategoryVisuals;
import com.example.finanzas.util.Format;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

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
        String message = "Score: " + summary.getScoreFinanciero() + "/100"
                + "\nEstado: " + nonEmpty(summary.getScoreEstado(), getString(R.string.home_financial_missing))
                + "\n\n" + nonEmpty(summary.getInsightPrincipal(), getString(R.string.home_financial_missing))
                + "\n\n" + nonEmpty(summary.getScoreExplicacion(), "")
                + "\nTendencia: " + nonEmpty(summary.getScoreTendencia(), getString(R.string.home_comparison_no_previous))
                + "\nAlerta: " + nonEmpty(summary.getAlertaPrincipal(), getString(R.string.home_alerts_empty))
                + "\nProyeccion fin de mes: " + Format.money(summary.getProyeccionFinMes(), currencyCode);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.home_financial_detail_title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void showSavingDetail() {
        HomeSummary summary = lastSummary;
        if (summary == null) return;
        String suggested = summary.getAhorroSugerido() > 0
                ? Format.money(summary.getAhorroSugerido(), currencyCode)
                : getString(R.string.home_smart_saving_not_recommended);
        String message = "Estado: " + nonEmpty(summary.getEstadoAhorro(), getString(R.string.home_smart_saving_status_adjusted))
                + "\nAhorro sugerido: " + suggested
                + "\n\n" + nonEmpty(summary.getAhorroSugeridoMensaje(), getString(R.string.home_smart_saving_goal_empty))
                + "\n\n" + nonEmpty(summary.getRecomendacionAhorroMeta(), getString(R.string.home_smart_saving_goal_empty))
                + "\nSaldo actual: " + Format.money(summary.getSaldoActualTotal(), currencyCode)
                + "\nGasto proyectado: " + Format.money(summary.getGastoProyectado(), currencyCode)
                + "\nSaldo estimado fin de mes: " + Format.money(summary.getProyeccionFinMes(), currencyCode);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.home_smart_saving_detail_title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void renderCategoryChart(@Nullable List<CategoryChartSlice> slices) {
        List<CategoryChartSlice> safe = slices == null ? new ArrayList<>() : slices;
        ArrayList<CategorySpendingChartView.Slice> entries = new ArrayList<>();
        double total = 0.0;
        for (CategoryChartSlice slice : safe) {
            if (slice == null || slice.getGastado() <= 0) continue;
            String name = safeLabel(slice.getCategoriaNombre());
            entries.add(new CategorySpendingChartView.Slice(
                    name,
                    (float) slice.getGastado(),
                    CategoryVisuals.colorFor(requireContext(), name, false)
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
