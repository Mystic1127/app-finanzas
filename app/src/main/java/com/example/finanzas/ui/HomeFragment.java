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
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.appcompat.app.AlertDialog;

import com.example.finanzas.R;
import com.example.finanzas.data.api.SettingsService;
import com.example.finanzas.data.model.CategoryChartSlice;
import com.example.finanzas.data.model.HomeSummary;
import com.example.finanzas.data.model.MonthlyTrendPoint;
import com.example.finanzas.data.model.DashboardModulePref;
import com.example.finanzas.data.model.ConversionSummary;
import com.example.finanzas.data.model.TravelPreference;
import com.example.finanzas.data.model.GamificationChallenge;
import com.example.finanzas.ui.adapter.CategoryBudgetSummaryAdapter;
import com.example.finanzas.ui.adapter.DashboardModuleAdapter;
import com.example.finanzas.ui.adapter.GoalSummaryAdapter;
import com.example.finanzas.ui.adapter.ReminderSummaryAdapter;
import com.example.finanzas.ui.viewmodel.HomeViewModel;
import com.example.finanzas.util.Format;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.Chart;
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

        androidx.recyclerview.widget.RecyclerView rvBudgets = v.findViewById(R.id.rvBudgets);
        androidx.recyclerview.widget.RecyclerView rvGoals = v.findViewById(R.id.rvGoals);
        androidx.recyclerview.widget.RecyclerView rvReminders = v.findViewById(R.id.rvReminders);

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

        v.findViewById(R.id.btnLista)
                .setOnClickListener(view -> Navigation.findNavController(view).navigate(R.id.nav_list));
        v.findViewById(R.id.btnPresupuesto)
                .setOnClickListener(view -> Navigation.findNavController(view).navigate(R.id.nav_budget));
        v.findViewById(R.id.btnMetas)
                .setOnClickListener(view -> Navigation.findNavController(view).navigate(R.id.nav_goals));
        v.findViewById(R.id.btnRecordatorios)
                .setOnClickListener(view -> Navigation.findNavController(view).navigate(R.id.nav_reminders));
        v.findViewById(R.id.btnImportaciones)
                .setOnClickListener(view -> Navigation.findNavController(view).navigate(R.id.nav_imports));

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
        final int anio = cal.get(Calendar.YEAR);
        final int mes = cal.get(Calendar.MONTH) + 1;
        viewModel.loadSummary(anio, mes);
    }

    private void observeViewModel() {
        viewModel.loading.observe(getViewLifecycleOwner(), loading -> {
            boolean show = Boolean.TRUE.equals(loading);
            showLoading(show);
            swipe.setRefreshing(show);
        });
        viewModel.summary.observe(getViewLifecycleOwner(), summary -> {
            if (summary == null || !isAdded()) return;
            pintarResumen(summary);
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

        List<String> alertas = summary.getAlertas();
        if (alertas != null && !alertas.isEmpty()) {
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
        if (progress != null) {
            progress.setVisibility(show ? View.VISIBLE : View.GONE);
        }
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
        if (slices == null || slices.isEmpty()) {
            chartCategorias.clear();
            chartCategorias.invalidate();
            chartCategorias.setVisibility(View.GONE);
            if (tvChartCategoriasEmpty != null) {
                tvChartCategoriasEmpty.setVisibility(View.VISIBLE);
            }
            return;
        }

        if (tvChartCategoriasEmpty != null) {
            tvChartCategoriasEmpty.setVisibility(View.GONE);
        }
        chartCategorias.setVisibility(View.VISIBLE);

        ArrayList<BarEntry> gastos = new ArrayList<>();
        ArrayList<BarEntry> presupuestos = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();

        for (int i = 0; i < slices.size(); i++) {
            CategoryChartSlice slice = slices.get(i);
            labels.add(slice.getCategoriaNombre());
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
        xAxis.setAxisMaximum(0f + groupWidth * labels.size());
        chartCategorias.groupBars(0f, groupSpace, barSpace);

        Legend legend = chartCategorias.getLegend();
        legend.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurfaceVariant));
        legend.setXEntrySpace(12f);

        chartCategorias.invalidate();
    }

    private void renderBalanceChart(double ingresos, double gastos, double saldo) {
        if (chartBalance == null) return;
        boolean hasData = ingresos != 0 || gastos != 0 || saldo != 0;
        if (!hasData) {
            chartBalance.clear();
            chartBalance.invalidate();
            chartBalance.setVisibility(View.GONE);
            if (tvChartBalanceEmpty != null) tvChartBalanceEmpty.setVisibility(View.VISIBLE);
            return;
        }

        if (tvChartBalanceEmpty != null) tvChartBalanceEmpty.setVisibility(View.GONE);
        chartBalance.setVisibility(View.VISIBLE);

        List<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0, (float) ingresos));
        entries.add(new BarEntry(1, (float) gastos));
        entries.add(new BarEntry(2, (float) saldo));

        BarDataSet dataSet = new BarDataSet(entries, getString(R.string.home_chart_balance_label));
        int[] colors = new int[]{
                ContextCompat.getColor(requireContext(), R.color.income),
                ContextCompat.getColor(requireContext(), R.color.expense),
                ContextCompat.getColor(requireContext(), R.color.chartBalance)
        };
        dataSet.setColors(colors);
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

        Legend legend = chartBalance.getLegend();
        legend.setEnabled(false);

        chartBalance.invalidate();
    }

    private void renderGoalsChart(@Nullable List<com.example.finanzas.data.model.SavingsGoal> metas) {
        if (chartGoals == null) return;
        if (metas == null || metas.isEmpty()) {
            chartGoals.clear();
            chartGoals.invalidate();
            chartGoals.setVisibility(View.GONE);
            if (tvChartGoalsEmpty != null) tvChartGoalsEmpty.setVisibility(View.VISIBLE);
            return;
        }

        if (tvChartGoalsEmpty != null) tvChartGoalsEmpty.setVisibility(View.GONE);
        chartGoals.setVisibility(View.VISIBLE);

        List<BarEntry> progressEntries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < metas.size(); i++) {
            com.example.finanzas.data.model.SavingsGoal goal = metas.get(i);
            double progreso = goal.getProgreso();
            if (progreso <= 1) progreso = progreso * 100.0;
            float clamped = (float) Math.min(100, Math.max(0, progreso));
            progressEntries.add(new BarEntry(i, clamped));
            labels.add(goal.getTitulo());
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

        Legend legend = chartGoals.getLegend();
        legend.setEnabled(false);

        chartGoals.invalidate();
    }

    private void renderTrendChart(@Nullable List<MonthlyTrendPoint> points) {
        if (chartTrend == null) return;
        if (points == null || points.isEmpty()) {
            chartTrend.clear();
            chartTrend.invalidate();
            chartTrend.setVisibility(View.GONE);
            if (tvChartTrendEmpty != null) {
                tvChartTrendEmpty.setVisibility(View.VISIBLE);
            }
            return;
        }

        if (tvChartTrendEmpty != null) {
            tvChartTrendEmpty.setVisibility(View.GONE);
        }
        chartTrend.setVisibility(View.VISIBLE);

        ArrayList<Entry> ingresosEntries = new ArrayList<>();
        ArrayList<Entry> gastosEntries = new ArrayList<>();
        ArrayList<Entry> saldoEntries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();
        float minValue = 0f;

        for (int i = 0; i < points.size(); i++) {
            MonthlyTrendPoint point = points.get(i);
            labels.add(point.getEtiqueta());
            ingresosEntries.add(new Entry(i, (float) point.getIngresos()));
            gastosEntries.add(new Entry(i, (float) point.getGastos()));
            float saldo = (float) point.getSaldo();
            saldoEntries.add(new Entry(i, saldo));
            minValue = Math.min(minValue, Math.min((float) point.getIngresos(), Math.min((float) point.getGastos(), saldo)));
        }

        LineDataSet ingresosSet = new LineDataSet(ingresosEntries, getString(R.string.chart_label_income));
        ingresosSet.setColor(ContextCompat.getColor(requireContext(), R.color.income));
        ingresosSet.setCircleColor(ContextCompat.getColor(requireContext(), R.color.income));
        ingresosSet.setLineWidth(2f);
        ingresosSet.setCircleRadius(4f);
        ingresosSet.setValueTextSize(10f);
        ingresosSet.setValueTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurface));

        LineDataSet gastosSet = new LineDataSet(gastosEntries, getString(R.string.chart_label_expense));
        gastosSet.setColor(ContextCompat.getColor(requireContext(), R.color.expense));
        gastosSet.setCircleColor(ContextCompat.getColor(requireContext(), R.color.expense));
        gastosSet.setLineWidth(2f);
        gastosSet.setCircleRadius(4f);
        gastosSet.setValueTextSize(10f);
        gastosSet.setValueTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurface));

        LineDataSet saldoSet = new LineDataSet(saldoEntries, getString(R.string.chart_label_balance));
        saldoSet.setColor(ContextCompat.getColor(requireContext(), R.color.chartBalance));
        saldoSet.setCircleColor(ContextCompat.getColor(requireContext(), R.color.chartBalance));
        saldoSet.setLineWidth(2f);
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
        if (minValue < 0f) {
            left.setAxisMinimum(minValue * 1.1f);
        } else {
            left.setAxisMinimum(0f);
        }
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
        if (summary == null) {
            if (tvGamificacionEmpty != null) tvGamificacionEmpty.setVisibility(View.VISIBLE);
            chipGamificacion.setVisibility(View.GONE);
            return;
        }
        List<GamificationChallenge> retos = summary.getGamificacionRetos();
        if (retos == null || retos.isEmpty()) {
            chipGamificacion.setVisibility(View.GONE);
            if (tvGamificacionEmpty != null) tvGamificacionEmpty.setVisibility(View.VISIBLE);
            return;
        }
        if (tvGamificacionEmpty != null) tvGamificacionEmpty.setVisibility(View.GONE);
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (GamificationChallenge challenge : retos) {
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
                    Format.money(nuevosIngresos),
                    Format.money(nuevosGastos));
            int tipRes = nuevoSaldo >= lastSummary.getSaldo()
                    ? R.string.home_simulation_tip_positive
                    : R.string.home_simulation_tip_negative;
            detalle = detalle + "\n" + getString(tipRes);
            tvSimDetalle.setText(detalle);
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

    private void setupModules(View root) {
        if (root == null) return;
        moduleViews.clear();
        moduleViews.put(MODULE_BALANCE, root.findViewById(R.id.moduleBalance));
        moduleViews.put(MODULE_FORECAST, root.findViewById(R.id.moduleForecast));
        moduleViews.put(MODULE_SIMULATION, root.findViewById(R.id.moduleSimulation));
        moduleViews.put(MODULE_AUTOMATION, root.findViewById(R.id.moduleAutomation));
        moduleViews.put(MODULE_QUICK, root.findViewById(R.id.moduleQuick));
        moduleViews.put(MODULE_GAMIFICATION, root.findViewById(R.id.moduleGamification));
        moduleViews.put(MODULE_ALERTS, root.findViewById(R.id.moduleAlerts));
        moduleViews.put(MODULE_BUDGETS, root.findViewById(R.id.moduleBudgets));
        moduleViews.put(MODULE_GOALS, root.findViewById(R.id.moduleGoals));
        moduleViews.put(MODULE_REMINDERS, root.findViewById(R.id.moduleReminders));
        moduleViews.put(MODULE_CHART_BALANCE, root.findViewById(R.id.moduleChartBalance));
        moduleViews.put(MODULE_CHART_GOALS, root.findViewById(R.id.moduleChartGoals));
        moduleViews.put(MODULE_CHART_BUDGET, root.findViewById(R.id.moduleChartBudget));
        moduleViews.put(MODULE_CHART_TREND, root.findViewById(R.id.moduleChartTrend));
        if (moduleContainer == null) return;
        moduleContainer.removeAllViews();
        for (String id : DEFAULT_MODULE_ORDER) {
            View module = moduleViews.get(id);
            if (module != null) {
                module.setVisibility(View.VISIBLE);
                moduleContainer.addView(module);
            }
        }
    }

    private void applyModulePreferences(@Nullable HomeSummary summary) {
        if (moduleContainer == null || moduleViews.isEmpty()) return;
        List<String> order = new ArrayList<>(DEFAULT_MODULE_ORDER);
        Map<String, Boolean> visibility = new HashMap<>();
        if (summary != null) {
            List<DashboardModulePref> prefs = summary.getDashboardPreferencias();
            if (prefs != null && !prefs.isEmpty()) {
                order.clear();
                for (DashboardModulePref pref : prefs) {
                    if (pref == null) continue;
                    String id = pref.getId();
                    if (id == null || !moduleViews.containsKey(id)) continue;
                    order.add(id);
                    visibility.put(id, pref.isVisible());
                }
                for (String def : DEFAULT_MODULE_ORDER) {
                    if (!order.contains(def)) order.add(def);
                }
            }
        }
        moduleContainer.removeAllViews();
        for (String id : order) {
            View module = moduleViews.get(id);
            if (module == null) continue;
            boolean visible = !visibility.containsKey(id) || visibility.get(id);
            module.setVisibility(visible ? View.VISIBLE : View.GONE);
            moduleContainer.addView(module);
        }
    }

    private List<DashboardModuleAdapter.ModuleItem> buildModuleItems() {
        List<DashboardModuleAdapter.ModuleItem> items = new ArrayList<>();
        if (moduleViews.isEmpty()) return items;
        List<String> order = new ArrayList<>(DEFAULT_MODULE_ORDER);
        Map<String, Boolean> visibility = new HashMap<>();
        if (lastSummary != null) {
            List<DashboardModulePref> prefs = lastSummary.getDashboardPreferencias();
            if (prefs != null && !prefs.isEmpty()) {
                order.clear();
                for (DashboardModulePref pref : prefs) {
                    if (pref == null) continue;
                    String id = pref.getId();
                    if (id == null || !moduleViews.containsKey(id)) continue;
                    order.add(id);
                    visibility.put(id, pref.isVisible());
                }
                for (String def : DEFAULT_MODULE_ORDER) {
                    if (!order.contains(def)) order.add(def);
                }
            }
        }
        for (String id : order) {
            View module = moduleViews.get(id);
            if (module == null) continue;
            boolean visible = visibility.containsKey(id) ? visibility.get(id) : module.getVisibility() != View.GONE;
            items.add(new DashboardModuleAdapter.ModuleItem(id, getModuleTitle(id), visible));
        }
        return items;
    }

    private String getModuleTitle(String id) {
        if (!isAdded()) return id;
        switch (id) {
            case MODULE_BALANCE:
                return getString(R.string.dashboard_module_balance);
            case MODULE_FORECAST:
                return getString(R.string.dashboard_module_forecast);
            case MODULE_SIMULATION:
                return getString(R.string.dashboard_module_simulation);
            case MODULE_AUTOMATION:
                return getString(R.string.dashboard_module_automation);
            case MODULE_QUICK:
                return getString(R.string.dashboard_module_quick);
            case MODULE_GAMIFICATION:
                return getString(R.string.dashboard_module_gamification);
            case MODULE_ALERTS:
                return getString(R.string.dashboard_module_alerts);
            case MODULE_BUDGETS:
                return getString(R.string.dashboard_module_budgets);
            case MODULE_GOALS:
                return getString(R.string.dashboard_module_goals);
            case MODULE_REMINDERS:
                return getString(R.string.dashboard_module_reminders);
            case MODULE_CHART_BALANCE:
                return getString(R.string.dashboard_module_chart_balance);
            case MODULE_CHART_GOALS:
                return getString(R.string.dashboard_module_chart_goals);
            case MODULE_CHART_BUDGET:
                return getString(R.string.dashboard_module_chart_budget);
            case MODULE_CHART_TREND:
                return getString(R.string.dashboard_module_chart_trend);
            default:
                return id;
        }
    }

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

    private void showCustomizeDialog() {
        if (!isAdded() || moduleViews.isEmpty()) return;
        View content = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_dashboard_modules, null, false);
        RecyclerView rv = content.findViewById(R.id.rvModules);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        DashboardModuleAdapter adapter = new DashboardModuleAdapter();
        adapter.setItems(buildModuleItems());
        rv.setAdapter(adapter);

        ItemTouchHelper helper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {

                adapter.moveItem(
                        viewHolder.getAdapterPosition(),
                        target.getAdapterPosition()
                );

                return true;
            }


            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) { }

            @Override
            public boolean isLongPressDragEnabled() {
                return true;
            }
        });
        helper.attachToRecyclerView(rv);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.dashboard_customize_title)
                .setView(content)
                .setPositiveButton(R.string.dashboard_customize_save, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            List<DashboardModuleAdapter.ModuleItem> snapshot = new ArrayList<>(adapter.getItems());
            saveDashboardPreferences(snapshot);
            dialog.dismiss();
        }));
        dialog.show();
    }

    private void saveDashboardPreferences(List<DashboardModuleAdapter.ModuleItem> modules) {
        JSONArray arr = new JSONArray();
        for (DashboardModuleAdapter.ModuleItem item : modules) {
            if (item == null) continue;
            JSONObject o = new JSONObject();
            try {
                o.put("id", item.getId());
                o.put("visible", item.isVisible());
                arr.put(o);
            } catch (Exception ignore) { }
        }
        JSONObject body = new JSONObject();
        try {
            body.put("modules", arr);
        } catch (Exception ignore) { }

        SettingsService.saveDashboard(requireContext(), body, new SettingsService.SaveCb() {
            @Override
            public void onSuccess() {
                if (lastSummary != null) {
                    lastSummary.getDashboardPreferencias().clear();
                    for (DashboardModuleAdapter.ModuleItem item : modules) {
                        if (item == null) continue;
                        lastSummary.getDashboardPreferencias().add(new DashboardModulePref(item.getId(), item.isVisible()));
                    }
                }
                applyModulePreferences(lastSummary);
                Toast.makeText(requireContext(), R.string.dashboard_customize_saved, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFail() {
                Toast.makeText(requireContext(), R.string.dashboard_customize_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showTravelDialog() {
        if (!isAdded()) return;
        View content = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_travel_settings, null, false);
        SwitchMaterial swEnabled = content.findViewById(R.id.swTravelEnabled);
        EditText etBase = content.findViewById(R.id.etTravelBase);
        EditText etSecondary = content.findViewById(R.id.etTravelSecondary);
        EditText etRate = content.findViewById(R.id.etTravelRate);

        if (lastSummary != null && lastSummary.getTravelPreference() != null) {
            TravelPreference pref = lastSummary.getTravelPreference();
            swEnabled.setChecked(pref.isEnabled());
            if (pref.getBase() != null) etBase.setText(pref.getBase());
            if (pref.getCurrency() != null) etSecondary.setText(pref.getCurrency());
            if (pref.getRate() > 0) etRate.setText(String.valueOf(pref.getRate()));
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.travel_settings_title)
                .setView(content)
                .setPositiveButton(R.string.travel_settings_save, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            boolean enabled = swEnabled.isChecked();
            String base = etBase.getText() == null ? "" : etBase.getText().toString().trim().toUpperCase();
            String currency = etSecondary.getText() == null ? "" : etSecondary.getText().toString().trim().toUpperCase();
            String rateRaw = etRate.getText() == null ? "" : etRate.getText().toString().trim();
            double rate = 0;
            if (!rateRaw.isEmpty()) {
                rate = parseMontoSeguro(rateRaw);
                if (rate <= 0) {
                    Toast.makeText(requireContext(), R.string.travel_settings_error, Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            JSONObject body = new JSONObject();
            try {
                body.put("enabled", enabled);
                if (!TextUtils.isEmpty(base)) body.put("base", base);
                if (!TextUtils.isEmpty(currency)) body.put("currency", currency);
                if (rate > 0) body.put("rate", rate);
            } catch (Exception ignore) { }

            SettingsService.saveTravel(requireContext(), body, new SettingsService.SaveCb() {
                @Override
                public void onSuccess() {
                    Toast.makeText(requireContext(), R.string.travel_settings_saved, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    cargarResumen();
                }

                @Override
                public void onFail() {
                    Toast.makeText(requireContext(), R.string.travel_settings_error, Toast.LENGTH_SHORT).show();
                }
            });
        }));
        dialog.show();
    }

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

        if ("-".equals(limpio) || ".".equals(limpio) || "-.".equals(limpio) || ",".equals(limpio)) {
            return 0;
        }

        try {
            return Double.parseDouble(limpio);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
