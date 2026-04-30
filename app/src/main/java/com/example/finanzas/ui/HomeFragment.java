package com.example.finanzas.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finanzas.R;
import com.example.finanzas.data.api.SettingsService;
import com.example.finanzas.data.model.CategoryChartSlice;
import com.example.finanzas.data.model.ConversionSummary;
import com.example.finanzas.data.model.DashboardModulePref;
import com.example.finanzas.data.model.GamificationChallenge;
import com.example.finanzas.data.model.HomeSummary;
import com.example.finanzas.data.model.MonthlyTrendPoint;
import com.example.finanzas.data.model.TravelPreference;
import com.example.finanzas.ui.adapter.CategoryBudgetSummaryAdapter;
import com.example.finanzas.ui.adapter.DashboardModuleAdapter;
import com.example.finanzas.ui.adapter.GoalSummaryAdapter;
import com.example.finanzas.ui.adapter.ReminderSummaryAdapter;
import com.example.finanzas.ui.viewmodel.HomeViewModel;
import com.example.finanzas.util.Format;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.switchmaterial.SwitchMaterial;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HomeFragment extends Fragment {

    private static final String MODULE_BALANCE = "balance";
    private static final String MODULE_FORECAST = "forecast";
    private static final String MODULE_SIMULATION = "simulation";
    private static final String MODULE_AUTOMATION = "automation";
    private static final String MODULE_QUICK = "quick";
    private static final String MODULE_GAMIFICATION = "gamification";
    private static final String MODULE_ALERTS = "alerts";
    private static final String MODULE_BUDGETS = "budgets";
    private static final String MODULE_GOALS = "goals";
    private static final String MODULE_REMINDERS = "reminders";
    private static final String MODULE_CHART_BUDGET = "chart_budget";
    private static final String MODULE_CHART_TREND = "chart_trend";
    private static final String MODULE_CHART_BALANCE = "chart_balance";
    private static final String MODULE_CHART_GOALS = "chart_goals";
    private static final List<String> DEFAULT_MODULE_ORDER = Collections.unmodifiableList(Arrays.asList(
            MODULE_BALANCE,
            MODULE_FORECAST,
            MODULE_SIMULATION,
            MODULE_AUTOMATION,
            MODULE_QUICK,
            MODULE_GAMIFICATION,
            MODULE_ALERTS,
            MODULE_BUDGETS,
            MODULE_GOALS,
            MODULE_REMINDERS,
            MODULE_CHART_BALANCE,
            MODULE_CHART_GOALS,
            MODULE_CHART_BUDGET,
            MODULE_CHART_TREND
    ));

    private TextView tvSaldo;
    private TextView tvIngresos;
    private TextView tvGastos;
    private TextView tvAlertas;
    private TextView tvAlertasTitulo;
    private TextView tvPresupuestoResumen;
    private TextView tvInsightsTitulo;
    private ChipGroup chipInsights;
    private TextView tvConversionResumen;
    private TextView tvBudgetsEmpty;
    private TextView tvGoalsEmpty;
    private TextView tvRemindersEmpty;
    private TextView tvChartCategoriasEmpty;
    private TextView tvChartTrendEmpty;
    private TextView tvChartBalanceEmpty;
    private TextView tvChartGoalsEmpty;
    private TextView tvPredictProjected;
    private TextView tvPredictDaily;
    private TextView tvPredictDays;
    private TextView tvPredictAlerts;
    private TextView tvImportPend;
    private TextView tvSimResultado;
    private TextView tvSimDetalle;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipe;
    private CircularProgressIndicator progress;

    private CategoryBudgetSummaryAdapter budgetAdapter;
    private GoalSummaryAdapter goalAdapter;
    private ReminderSummaryAdapter reminderAdapter;
    private BarChart chartCategorias;
    private LineChart chartTrend;
    private BarChart chartBalance;
    private BarChart chartGoals;
    private Chip chipRiesgo;
    private ChipGroup chipGamificacion;
    private TextView tvGamificacionEmpty;
    private EditText etSimIngreso;
    private EditText etSimGasto;
    private LinearLayout moduleContainer;
    private final Map<String, View> moduleViews = new LinkedHashMap<>();
    private HomeSummary lastSummary;
    private HomeViewModel viewModel;
    private String smartAlertMessage;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        tvSaldo = v.findViewById(R.id.tvSaldo);
        tvIngresos = v.findViewById(R.id.tvIngresos);
        tvGastos = v.findViewById(R.id.tvGastos);
        tvAlertas = v.findViewById(R.id.tvAlertas);
        tvAlertasTitulo = v.findViewById(R.id.tvAlertasTitulo);
        tvBudgetsEmpty = v.findViewById(R.id.tvBudgetsEmpty);
        tvGoalsEmpty = v.findViewById(R.id.tvGoalsEmpty);
        tvRemindersEmpty = v.findViewById(R.id.tvRemindersEmpty);
        tvPresupuestoResumen = v.findViewById(R.id.tvPresupuestoResumen);
        tvInsightsTitulo = v.findViewById(R.id.tvInsightsTitulo);
        chipInsights = v.findViewById(R.id.chipInsights);
        tvConversionResumen = v.findViewById(R.id.tvConversionResumen);
        tvChartCategoriasEmpty = v.findViewById(R.id.tvChartCategoriasEmpty);
        tvChartTrendEmpty = v.findViewById(R.id.tvChartTrendEmpty);
        tvChartBalanceEmpty = v.findViewById(R.id.tvChartBalanceEmpty);
        tvChartGoalsEmpty = v.findViewById(R.id.tvChartGoalsEmpty);
        tvPredictProjected = v.findViewById(R.id.tvPredictProjected);
        tvPredictDaily = v.findViewById(R.id.tvPredictDaily);
        tvPredictDays = v.findViewById(R.id.tvPredictDays);
        tvPredictAlerts = v.findViewById(R.id.tvPredictAlerts);
        tvImportPend = v.findViewById(R.id.tvImportPend);
        chipRiesgo = v.findViewById(R.id.chipRiesgo);
        chipGamificacion = v.findViewById(R.id.chipGamificacion);
        tvGamificacionEmpty = v.findViewById(R.id.tvGamificacionEmpty);
        etSimIngreso = v.findViewById(R.id.etSimIngreso);
        etSimGasto = v.findViewById(R.id.etSimGasto);
        tvSimResultado = v.findViewById(R.id.tvSimResultado);
        tvSimDetalle = v.findViewById(R.id.tvSimDetalle);
        moduleContainer = v.findViewById(R.id.containerModules);
        swipe = v.findViewById(R.id.swipeHome);
        progress = v.findViewById(R.id.progressHome);
        chartCategorias = v.findViewById(R.id.chartCategorias);
        chartTrend = v.findViewById(R.id.chartTrend);
        chartBalance = v.findViewById(R.id.chartBalance);
        chartGoals = v.findViewById(R.id.chartGoals);
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        resetSimulation();
        setupModules(v);

        Button btnSimular = v.findViewById(R.id.btnSimular);
        btnSimular.setOnClickListener(view -> runSimulation());

        RecyclerView rvBudgets = v.findViewById(R.id.rvBudgets);
        RecyclerView rvGoals = v.findViewById(R.id.rvGoals);
        RecyclerView rvReminders = v.findViewById(R.id.rvReminders);

        rvBudgets.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvGoals.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvReminders.setLayoutManager(new LinearLayoutManager(requireContext()));

        budgetAdapter = new CategoryBudgetSummaryAdapter();
        goalAdapter = new GoalSummaryAdapter();
        reminderAdapter = new ReminderSummaryAdapter();

        rvBudgets.setAdapter(budgetAdapter);
        rvGoals.setAdapter(goalAdapter);
        rvReminders.setAdapter(reminderAdapter);

        setupChart(chartCategorias);
        setupChart(chartTrend);
        setupChart(chartBalance);
        setupChart(chartGoals);

        v.findViewById(R.id.btnLista).setOnClickListener(view -> Navigation.findNavController(view).navigate(R.id.nav_list));
        v.findViewById(R.id.btnPresupuesto).setOnClickListener(view -> Navigation.findNavController(view).navigate(R.id.nav_budget));
        v.findViewById(R.id.btnMetas).setOnClickListener(view -> Navigation.findNavController(view).navigate(R.id.nav_goals));
        v.findViewById(R.id.btnRecordatorios).setOnClickListener(view -> Navigation.findNavController(view).navigate(R.id.nav_reminders));
        v.findViewById(R.id.btnImportaciones).setOnClickListener(view -> Navigation.findNavController(view).navigate(R.id.nav_imports));

        FloatingActionButton fabNueva = v.findViewById(R.id.fabNueva);
        fabNueva.setOnClickListener(view -> Navigation.findNavController(view).navigate(R.id.nav_new));

        swipe.setOnRefreshListener(this::cargarResumen);
        observeViewModel();
    }

    @Override
    public void onResume() {
        super.onResume();
        cargarResumen();
    }

    private void cargarResumen() {
        Calendar cal = Calendar.getInstance();
        viewModel.loadSummary(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1);
    }

    private void observeViewModel() {
        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            boolean show = Boolean.TRUE.equals(loading);
            showLoading(show);
            swipe.setRefreshing(show);
        });

        viewModel.getSummary().observe(getViewLifecycleOwner(), summary -> {
            if (summary == null || !isAdded()) return;
            pintarResumen(summary);
        });

        viewModel.getSmartAlert().observe(getViewLifecycleOwner(), alert -> {
            smartAlertMessage = alert;
            if (lastSummary != null && isAdded()) pintarResumen(lastSummary);
        });

        viewModel.getInsights().observe(getViewLifecycleOwner(), items -> {
            if (items == null || items.isEmpty()) {
                tvInsightsTitulo.setVisibility(View.GONE);
                chipInsights.setVisibility(View.GONE);
                chipInsights.removeAllViews();
            } else {
                tvInsightsTitulo.setVisibility(View.VISIBLE);
                chipInsights.setVisibility(View.VISIBLE);
                chipInsights.removeAllViews();
                for (String insight : items) {
                    Chip chip = new Chip(requireContext());
                    chip.setText(insight);
                    chip.setChipIcon(ContextCompat.getDrawable(requireContext(), R.drawable.ic_arrow_trending_24));
                    chip.setChipIconVisible(true);
                    chip.setClickable(false);
                    chip.setCheckable(false);
                    chipInsights.addView(chip);
                }
            }
        });
    }

    private void pintarResumen(HomeSummary summary) {
        lastSummary = summary;
        applyModulePreferences(summary);
        resetSimulation();

        tvIngresos.setText(Format.money(summary.getIngresos()));
        tvGastos.setText(Format.money(summary.getGastos()));
        tvSaldo.setText(Format.money(summary.getSaldo()));

        renderConversion(summary.getConversion());

        if (summary.getPresupuestoMonto() > 0) {
            tvPresupuestoResumen.setVisibility(View.VISIBLE);
            tvPresupuestoResumen.setText(getString(
                    R.string.home_month_budget_resume,
                    Format.money(summary.getPresupuestoMonto()),
                    Format.money(summary.getPresupuestoRestante())
            ));
        } else {
            tvPresupuestoResumen.setVisibility(View.GONE);
        }

        tvPredictProjected.setText(Format.money(summary.getGastoProyectado()));
        tvPredictDaily.setText(Format.money(summary.getGastoPromedioDiario()));
        tvPredictDays.setText(String.valueOf(summary.getDiasRestantes()));
        aplicarRiesgo(summary.getRiesgoPresupuesto());

        List<String> alertasPredictivas = summary.getAlertasPredictivas();
        if (alertasPredictivas != null && !alertasPredictivas.isEmpty()) {
            tvPredictAlerts.setVisibility(View.VISIBLE);
            tvPredictAlerts.setText(joinAlertas(alertasPredictivas));
        } else {
            tvPredictAlerts.setVisibility(View.GONE);
        }

        List<String> alertas = new ArrayList<>(summary.getAlertas());
        if (smartAlertMessage != null && !smartAlertMessage.trim().isEmpty()) {
            alertas.add(0, smartAlertMessage);
        }
        if (!alertas.isEmpty()) {
            tvAlertasTitulo.setVisibility(View.VISIBLE);
            tvAlertas.setVisibility(View.VISIBLE);
            tvAlertas.setText(joinAlertas(alertas));
        } else {
            tvAlertasTitulo.setVisibility(View.GONE);
            tvAlertas.setVisibility(View.GONE);
        }

        if (summary.getImportacionesPendientes() > 0) {
            tvImportPend.setText(getString(R.string.home_automation_imports_pending, summary.getImportacionesPendientes()));
        } else {
            tvImportPend.setText(R.string.home_automation_imports_none);
        }

        budgetAdapter.setItems(summary.getPresupuestosCategoria());
        goalAdapter.setItems(summary.getMetas());
        reminderAdapter.setItems(summary.getRecordatorios());

        tvBudgetsEmpty.setVisibility(budgetAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
        tvGoalsEmpty.setVisibility(goalAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
        tvRemindersEmpty.setVisibility(reminderAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);

        renderGamification(summary);
        renderBudgetChart(summary.getChartCategorias());
        renderBalanceChart(summary.getIngresos(), summary.getGastos(), summary.getSaldo());
        renderGoalsChart(summary.getMetas());
        renderTrendChart(summary.getTendenciaMensual());
    }

    private String joinAlertas(List<String> alertas) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < alertas.size(); i++) {
            if (i > 0) sb.append('\n');
            sb.append(alertas.get(i));
        }
        return sb.toString();
    }

    private void aplicarRiesgo(@Nullable String riesgo) {
        if (chipRiesgo == null) return;
        String safe = riesgo == null ? "" : riesgo.toLowerCase();
        int bgRes;
        int textRes;
        int labelRes;
        switch (safe) {
            case "alto":
                bgRes = R.color.danger;
                textRes = R.color.white;
                labelRes = R.string.home_predict_risk_high;
                break;
            case "medio":
                bgRes = R.color.md_theme_secondary;
                textRes = R.color.md_theme_onSecondary;
                labelRes = R.string.home_predict_risk_medium;
                break;
            default:
                bgRes = R.color.chartBalance;
                textRes = R.color.white;
                labelRes = R.string.home_predict_risk_low;
                break;
        }
        chipRiesgo.setText(labelRes);
        chipRiesgo.setChipBackgroundColorResource(bgRes);
        chipRiesgo.setTextColor(ContextCompat.getColor(requireContext(), textRes));
    }

    private void showLoading(boolean show) {
        if (progress != null) progress.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void setupChart(@Nullable Chart<?> chart) {
        if (chart == null) return;
        chart.getDescription().setEnabled(false);
        chart.setNoDataText(getString(R.string.chart_no_data));
        chart.setNoDataTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurfaceVariant));
        Legend legend = chart.getLegend();
        legend.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurfaceVariant));
        legend.setXEntrySpace(12f);
    }

    private void renderBudgetChart(@Nullable List<CategoryChartSlice> slices) {
        if (chartCategorias == null) return;

        List<CategoryChartSlice> safeSlices = (slices == null || slices.isEmpty())
                ? Collections.singletonList(new CategoryChartSlice() {{
            setCategoriaNombre("Sin datos");
            setGastado(0.0);
            setPresupuesto(0.0);
        }})
                : slices;

        if (tvChartCategoriasEmpty != null) {
            tvChartCategoriasEmpty.setVisibility((slices == null || slices.isEmpty()) ? View.VISIBLE : View.GONE);
            tvChartCategoriasEmpty.setText(R.string.chart_no_data);
        }
        chartCategorias.setVisibility(View.VISIBLE);

        ArrayList<BarEntry> gastos = new ArrayList<>();
        ArrayList<BarEntry> presupuestos = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();
        for (int i = 0; i < safeSlices.size(); i++) {
            CategoryChartSlice slice = safeSlices.get(i);
            String catLabel = slice.getCategoriaNombre();
            labels.add((catLabel == null || catLabel.trim().isEmpty()) ? "Sin datos" : catLabel);
            gastos.add(new BarEntry(i, (float) slice.getGastado()));
            presupuestos.add(new BarEntry(i, (float) slice.getPresupuesto()));
        }

        BarDataSet gastoSet = new BarDataSet(gastos, getString(R.string.chart_label_spent));
        gastoSet.setColor(ContextCompat.getColor(requireContext(), R.color.expense));
        gastoSet.setValueTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurface));
        gastoSet.setValueTextSize(10f);

        BarDataSet presupuestoSet = new BarDataSet(presupuestos, getString(R.string.chart_label_budget));
        presupuestoSet.setColor(ContextCompat.getColor(requireContext(), R.color.chartBudget));
        presupuestoSet.setValueTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurface));
        presupuestoSet.setValueTextSize(10f);

        BarData data = new BarData(gastoSet, presupuestoSet);
        float groupSpace = 0.12f;
        float barSpace = 0.02f;
        float barWidth = 0.42f;
        data.setBarWidth(barWidth);

        chartCategorias.setData(data);
        chartCategorias.setScaleXEnabled(false);
        chartCategorias.setScaleYEnabled(false);
        chartCategorias.setDoubleTapToZoomEnabled(false);

        XAxis xAxis = chartCategorias.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);
        xAxis.setCenterAxisLabels(true);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurfaceVariant));
        xAxis.setDrawGridLines(false);
        xAxis.setLabelRotationAngle(-20f);

        YAxis left = chartCategorias.getAxisLeft();
        left.setAxisMinimum(0f);
        left.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurfaceVariant));
        left.setGridColor(ContextCompat.getColor(requireContext(), R.color.md_theme_outlineVariant));
        chartCategorias.getAxisRight().setEnabled(false);

        float groupWidth = data.getGroupWidth(groupSpace, barSpace);
        xAxis.setAxisMinimum(0f);
        xAxis.setAxisMaximum(groupWidth * labels.size());
        chartCategorias.groupBars(0f, groupSpace, barSpace);

        Legend legend = chartCategorias.getLegend();
        legend.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurfaceVariant));
        legend.setXEntrySpace(12f);

        chartCategorias.invalidate();
    }

    private void renderBalanceChart(double ingresos, double gastos, double saldo) {
        if (chartBalance == null) return;

        boolean hasData = ingresos != 0 || gastos != 0 || saldo != 0;
        if (tvChartBalanceEmpty != null) {
            tvChartBalanceEmpty.setVisibility(hasData ? View.GONE : View.VISIBLE);
            tvChartBalanceEmpty.setText(R.string.chart_no_data);
        }
        chartBalance.setVisibility(View.VISIBLE);

        List<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0, (float) ingresos));
        entries.add(new BarEntry(1, (float) gastos));
        entries.add(new BarEntry(2, (float) saldo));

        BarDataSet dataSet = new BarDataSet(entries, getString(R.string.home_chart_balance_label));
        dataSet.setColors(
                ContextCompat.getColor(requireContext(), R.color.income),
                ContextCompat.getColor(requireContext(), R.color.expense),
                ContextCompat.getColor(requireContext(), R.color.chartBalance)
        );
        dataSet.setValueTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurface));
        dataSet.setValueTextSize(12f);

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.5f);
        chartBalance.setData(data);
        chartBalance.setScaleXEnabled(false);
        chartBalance.setScaleYEnabled(false);
        chartBalance.setDoubleTapToZoomEnabled(false);

        XAxis xAxis = chartBalance.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(Arrays.asList(
                getString(R.string.chart_label_income_short),
                getString(R.string.chart_label_expense_short),
                getString(R.string.chart_label_balance)
        )));
        xAxis.setGranularity(1f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurfaceVariant));
        xAxis.setDrawGridLines(false);

        YAxis left = chartBalance.getAxisLeft();
        left.setAxisMinimum(Math.min(0f, (float) Math.min(Math.min(ingresos, gastos), saldo)) * 1.1f);
        left.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurfaceVariant));
        left.setGridColor(ContextCompat.getColor(requireContext(), R.color.md_theme_outlineVariant));
        chartBalance.getAxisRight().setEnabled(false);

        chartBalance.getLegend().setEnabled(false);
        chartBalance.invalidate();
    }

    private void renderGoalsChart(@Nullable List<com.example.finanzas.data.model.SavingsGoal> metas) {
        if (chartGoals == null) return;

        List<com.example.finanzas.data.model.SavingsGoal> safeMetas =
                (metas == null || metas.isEmpty())
                        ? Collections.singletonList(new com.example.finanzas.data.model.SavingsGoal())
                        : metas;

        if (tvChartGoalsEmpty != null) {
            tvChartGoalsEmpty.setVisibility((metas == null || metas.isEmpty()) ? View.VISIBLE : View.GONE);
            tvChartGoalsEmpty.setText(R.string.chart_no_data);
        }
        chartGoals.setVisibility(View.VISIBLE);

        List<BarEntry> progressEntries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < safeMetas.size(); i++) {
            com.example.finanzas.data.model.SavingsGoal goal = safeMetas.get(i);
            double progreso = goal.getProgreso();
            if (progreso <= 1) progreso *= 100.0;
            float clamped = (float) Math.min(100, Math.max(0, progreso));
            progressEntries.add(new BarEntry(i, clamped));
            String goalLabel = goal.getTitulo();
            labels.add((goalLabel == null || goalLabel.trim().isEmpty()) ? "Meta" : goalLabel);
        }

        BarDataSet dataSet = new BarDataSet(progressEntries, getString(R.string.home_chart_goals_label));
        dataSet.setColor(ContextCompat.getColor(requireContext(), R.color.chartBudget));
        dataSet.setValueTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurface));
        dataSet.setValueTextSize(12f);

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.6f);
        chartGoals.setData(data);
        chartGoals.setScaleXEnabled(false);
        chartGoals.setScaleYEnabled(false);
        chartGoals.setDoubleTapToZoomEnabled(false);

        XAxis xAxis = chartGoals.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setGranularity(1f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurfaceVariant));
        xAxis.setDrawGridLines(false);
        xAxis.setLabelRotationAngle(-25f);

        YAxis left = chartGoals.getAxisLeft();
        left.setAxisMinimum(0f);
        left.setAxisMaximum(110f);
        left.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurfaceVariant));
        left.setGridColor(ContextCompat.getColor(requireContext(), R.color.md_theme_outlineVariant));
        chartGoals.getAxisRight().setEnabled(false);

        chartGoals.getLegend().setEnabled(false);
        chartGoals.invalidate();
    }

    private void renderTrendChart(@Nullable List<MonthlyTrendPoint> points) {
        if (chartTrend == null) return;

        List<MonthlyTrendPoint> safePoints = (points == null || points.isEmpty())
                ? Collections.singletonList(new MonthlyTrendPoint() {{
            setEtiqueta("Actual");
            setIngresos(0.0);
            setGastos(0.0);
            setSaldo(0.0);
        }})
                : points;

        if (tvChartTrendEmpty != null) {
            tvChartTrendEmpty.setVisibility((points == null || points.isEmpty()) ? View.VISIBLE : View.GONE);
            tvChartTrendEmpty.setText(R.string.chart_no_data);
        }
        chartTrend.setVisibility(View.VISIBLE);

        ArrayList<Entry> ingresosEntries = new ArrayList<>();
        ArrayList<Entry> gastosEntries = new ArrayList<>();
        ArrayList<Entry> saldoEntries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();
        float minValue = 0f;

        for (int i = 0; i < safePoints.size(); i++) {
            MonthlyTrendPoint point = safePoints.get(i);
            String trendLabel = point.getEtiqueta();
            labels.add((trendLabel == null || trendLabel.trim().isEmpty()) ? "-" : trendLabel);
            ingresosEntries.add(new Entry(i, (float) point.getIngresos()));
            gastosEntries.add(new Entry(i, (float) point.getGastos()));
            float saldo = (float) point.getSaldo();
            saldoEntries.add(new Entry(i, saldo));
            minValue = Math.min(minValue, Math.min((float) point.getIngresos(), Math.min((float) point.getGastos(), saldo)));
        }

        LineDataSet ingresosSet = new LineDataSet(ingresosEntries, getString(R.string.chart_label_income));
        ingresosSet.setColor(ContextCompat.getColor(requireContext(), R.color.income));
        ingresosSet.setCircleColor(ContextCompat.getColor(requireContext(), R.color.income));
        ingresosSet.setLineWidth(2.8f);
        ingresosSet.setCircleRadius(4f);
        ingresosSet.setValueTextSize(10f);
        ingresosSet.setValueTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurface));

        LineDataSet gastosSet = new LineDataSet(gastosEntries, getString(R.string.chart_label_expense));
        gastosSet.setColor(ContextCompat.getColor(requireContext(), R.color.expense));
        gastosSet.setCircleColor(ContextCompat.getColor(requireContext(), R.color.expense));
        gastosSet.setLineWidth(2.8f);
        gastosSet.setCircleRadius(4f);
        gastosSet.setValueTextSize(10f);
        gastosSet.setValueTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurface));

        LineDataSet saldoSet = new LineDataSet(saldoEntries, getString(R.string.chart_label_balance));
        saldoSet.setColor(ContextCompat.getColor(requireContext(), R.color.chartBalance));
        saldoSet.setCircleColor(ContextCompat.getColor(requireContext(), R.color.chartBalance));
        saldoSet.setLineWidth(2.8f);
        saldoSet.setCircleRadius(4f);
        saldoSet.setValueTextSize(10f);
        saldoSet.setValueTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurface));
        saldoSet.enableDashedLine(10f, 4f, 0f);

        LineData data = new LineData(ingresosSet, gastosSet, saldoSet);
        chartTrend.setData(data);
        chartTrend.setDoubleTapToZoomEnabled(false);
        chartTrend.setScaleXEnabled(false);
        chartTrend.setScaleYEnabled(false);
        chartTrend.getAxisRight().setEnabled(false);

        XAxis xAxis = chartTrend.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setGranularity(1f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurfaceVariant));
        xAxis.setDrawGridLines(false);
        xAxis.setLabelRotationAngle(-20f);

        YAxis left = chartTrend.getAxisLeft();
        left.setAxisMinimum(minValue < 0f ? minValue * 1.1f : 0f);
        left.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurfaceVariant));
        left.setGridColor(ContextCompat.getColor(requireContext(), R.color.md_theme_outlineVariant));

        Legend legend = chartTrend.getLegend();
        legend.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurfaceVariant));
        legend.setXEntrySpace(12f);

        chartTrend.invalidate();
    }

    private void renderConversion(@Nullable ConversionSummary conversion) {
        if (tvConversionResumen == null) return;
        if (conversion == null || !conversion.isEnabled()) {
            tvConversionResumen.setVisibility(View.GONE);
            tvConversionResumen.setText("");
            return;
        }
        String saldo = Format.money(conversion.getSaldo(), conversion.getMonedaDestino());
        String ingresos = Format.money(conversion.getIngresos(), conversion.getMonedaDestino());
        String gastos = Format.money(conversion.getGastos(), conversion.getMonedaDestino());
        tvConversionResumen.setVisibility(View.VISIBLE);
        tvConversionResumen.setText(getString(R.string.home_conversion_summary, saldo, ingresos, gastos));
    }

    private void renderGamification(@Nullable HomeSummary summary) {
        if (chipGamificacion == null) return;
        chipGamificacion.removeAllViews();
        if (summary == null || summary.getGamificacionRetos() == null || summary.getGamificacionRetos().isEmpty()) {
            chipGamificacion.setVisibility(View.GONE);
            if (tvGamificacionEmpty != null) tvGamificacionEmpty.setVisibility(View.VISIBLE);
            return;
        }
        if (tvGamificacionEmpty != null) tvGamificacionEmpty.setVisibility(View.GONE);
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (GamificationChallenge challenge : summary.getGamificacionRetos()) {
            if (challenge == null) continue;
            Chip chip = (Chip) inflater.inflate(R.layout.chip_gamification, chipGamificacion, false);
            chip.setText(challenge.getTitulo());
            String descripcion = challenge.getDescripcion();
            if (!TextUtils.isEmpty(descripcion)) {
                chip.setOnClickListener(v -> Toast.makeText(requireContext(), descripcion, Toast.LENGTH_LONG).show());
            } else {
                chip.setOnClickListener(null);
            }
            chipGamificacion.addView(chip);
        }
        chipGamificacion.setVisibility(View.VISIBLE);
    }

    private void runSimulation() {
        if (lastSummary == null) return;
        double ingresoAdj = parseMontoSeguro(etSimIngreso);
        double gastoAdj = parseMontoSeguro(etSimGasto);
        double nuevosIngresos = lastSummary.getIngresos() + ingresoAdj;
        double nuevosGastos = lastSummary.getGastos() + gastoAdj;
        double nuevoSaldo = lastSummary.getSaldo() + ingresoAdj - gastoAdj;

        if (tvSimResultado != null) {
            tvSimResultado.setText(getString(R.string.home_simulation_result, Format.money(nuevoSaldo)));
            tvSimResultado.setVisibility(View.VISIBLE);
        }

        if (tvSimDetalle != null) {
            String detalle = getString(R.string.home_simulation_breakdown,
                    Format.money(nuevosIngresos), Format.money(nuevosGastos));
            int tipRes = nuevoSaldo >= lastSummary.getSaldo()
                    ? R.string.home_simulation_tip_positive
                    : R.string.home_simulation_tip_negative;
            tvSimDetalle.setText(detalle + "\n" + getString(tipRes));
            tvSimDetalle.setVisibility(View.VISIBLE);
        }
    }

    private void resetSimulation() {
        if (tvSimResultado != null) {
            tvSimResultado.setVisibility(View.GONE);
            tvSimResultado.setText("");
        }
        if (tvSimDetalle != null) {
            tvSimDetalle.setVisibility(View.GONE);
            tvSimDetalle.setText("");
        }
    }

    private void setupModules(View root) { /* sin cambios */ }
    private void applyModulePreferences(@Nullable HomeSummary summary) { /* sin cambios */ }
    private List<DashboardModuleAdapter.ModuleItem> buildModuleItems() { return new ArrayList<>(); }
    private String getModuleTitle(String id) { return id; }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_home, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_customize_dashboard) {
            showCustomizeDialog();
            return true;
        } else if (id == R.id.action_travel_settings) {
            showTravelDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showCustomizeDialog() { /* sin cambios */ }
    private void saveDashboardPreferences(List<DashboardModuleAdapter.ModuleItem> modules) { /* sin cambios */ }
    private void showTravelDialog() { /* sin cambios */ }

    private double parseMontoSeguro(EditText input) {
        if (input == null) return 0;
        CharSequence text = input.getText();
        return parseMontoSeguro(text == null ? "" : text.toString());
    }

    private double parseMontoSeguro(String raw) {
        if (raw == null) return 0;
        String limpio = raw.trim();
        if (limpio.isEmpty()) return 0;
        limpio = limpio.replaceAll("[^0-9,.-]", "");
        if (limpio.isEmpty()) return 0;

        int lastComma = limpio.lastIndexOf(',');
        int lastDot = limpio.lastIndexOf('.');
        if (lastComma >= 0 && lastDot >= 0) {
            if (lastComma > lastDot) {
                limpio = limpio.replace(".", "");
                limpio = limpio.replace(',', '.');
            } else {
                limpio = limpio.replace(",", "");
            }
        } else if (lastComma >= 0) {
            limpio = limpio.replace(',', '.');
        }

        if ("-".equals(limpio) || ".".equals(limpio) || "-.".equals(limpio) || ",".equals(limpio)) return 0;

        try {
            return Double.parseDouble(limpio);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}