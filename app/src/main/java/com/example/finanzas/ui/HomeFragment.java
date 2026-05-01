package com.example.finanzas.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.finanzas.R;
import com.example.finanzas.data.api.SettingsService;
import com.example.finanzas.data.model.CategoryChartSlice;
import com.example.finanzas.data.model.DashboardModulePref;
import com.example.finanzas.data.model.HomeSummary;
import com.example.finanzas.data.model.MonthlyTrendPoint;
import com.example.finanzas.data.model.WeeklyPlanCategory;
import com.example.finanzas.data.model.WeeklyPlanSummary;
import com.example.finanzas.ui.adapter.DashboardModuleAdapter;
import com.example.finanzas.ui.viewmodel.HomeViewModel;
import com.example.finanzas.util.Format;
import com.example.finanzas.util.PerfLogger;
import com.example.finanzas.util.Prefs;
import com.example.finanzas.util.UiFormUtils;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HomeFragment extends Fragment {

    private static final String MODULE_SUMMARY = "summary";
    private static final String MODULE_CATEGORY = "category";
    private static final String MODULE_TREND = "trend";
    private static final String MODULE_ALERTS = "alerts";
    private static final String MODULE_INSIGHTS = "insights";
    private static final String MODULE_QUICK = "quick";

    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipe;
    private CircularProgressIndicator progress;
    private View emptyState;
    private View moduleSummary;
    private View moduleCategory;
    private View moduleTrend;
    private View moduleAlerts;
    private View moduleInsights;
    private View moduleQuick;
    private TextView tvPeriod;
    private TextView tvIngresos;
    private TextView tvGastos;
    private TextView tvBalance;
    private TextView tvCash;
    private TextView tvCard;
    private TextView tvBudgetUsed;
    private TextView tvBudgetDetail;
    private TextView tvCategoryEmpty;
    private TextView tvTrendEmpty;
    private TextView tvAlertsEmpty;
    private TextView tvInsightsEmpty;
    private TextView tvFinancialInsight;
    private TextView tvFinancialProjection;
    private TextView tvFinancialAlert;
    private TextView tvFinancialScore;
    private TextView tvFinancialScoreExplanation;
    private TextView tvFinancialScoreTrend;
    private TextView tvSmartSavingSuggested;
    private TextView tvSmartSavingGoal;
    private TextView tvSmartSavingProjection;
    private TextView tvSmartSavingStatus;
    private TextView tvWeeklyPlanTitle;
    private View cardWeeklyPlan;
    private TextView tvWeeklyPlanStatus;
    private TextView tvWeeklyPlanAmount;
    private TextView tvWeeklyPlanMessage;
    private MaterialButton btnWeeklyPlanDetail;
    private TextView tvRecommendationsPro;
    private ChipGroup chipRecommendations;
    private PieChart chartCategorias;
    private LineChart chartTrend;
    private ChipGroup chipAlerts;
    private ChipGroup chipInsights;
    private HomeViewModel viewModel;
    private String smartAlertMessage;
    private String currencyCode = "PEN";
    private final List<String> latestInsights = new ArrayList<>();
    private final Map<String, Boolean> moduleVisibility = new HashMap<>();
    private HomeSummary lastSummary;
    private boolean manualRefresh;
    private long perfStartMs;
    private boolean firstRenderLogged;
    private boolean testerThanksDialogShowing;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        perfStartMs = PerfLogger.now();
        firstRenderLogged = false;

        swipe = v.findViewById(R.id.swipeHome);
        progress = v.findViewById(R.id.progressHome);
        emptyState = v.findViewById(R.id.homeEmptyState);
        moduleSummary = v.findViewById(R.id.moduleHomeSummary);
        moduleCategory = v.findViewById(R.id.moduleHomeCategory);
        moduleTrend = v.findViewById(R.id.moduleHomeTrend);
        moduleAlerts = v.findViewById(R.id.moduleHomeAlerts);
        moduleInsights = v.findViewById(R.id.moduleHomeInsights);
        moduleQuick = v.findViewById(R.id.moduleHomeQuick);
        tvPeriod = v.findViewById(R.id.tvHomePeriod);
        tvIngresos = v.findViewById(R.id.tvHomeIngresos);
        tvGastos = v.findViewById(R.id.tvHomeGastos);
        tvBalance = v.findViewById(R.id.tvHomeBalance);
        tvCash = v.findViewById(R.id.tvHomeCash);
        tvCard = v.findViewById(R.id.tvHomeCard);
        tvBudgetUsed = v.findViewById(R.id.tvHomeBudgetUsed);
        tvBudgetDetail = v.findViewById(R.id.tvHomeBudgetDetail);
        tvCategoryEmpty = v.findViewById(R.id.tvHomeCategoryEmpty);
        tvTrendEmpty = v.findViewById(R.id.tvHomeTrendEmpty);
        tvAlertsEmpty = v.findViewById(R.id.tvHomeAlertsEmpty);
        tvInsightsEmpty = v.findViewById(R.id.tvHomeInsightsEmpty);
        tvFinancialInsight = v.findViewById(R.id.tvFinancialInsight);
        tvFinancialProjection = v.findViewById(R.id.tvFinancialProjection);
        tvFinancialAlert = v.findViewById(R.id.tvFinancialAlert);
        tvFinancialScore = v.findViewById(R.id.tvFinancialScore);
        tvFinancialScoreExplanation = v.findViewById(R.id.tvFinancialScoreExplanation);
        tvFinancialScoreTrend = v.findViewById(R.id.tvFinancialScoreTrend);
        tvSmartSavingSuggested = v.findViewById(R.id.tvSmartSavingSuggested);
        tvSmartSavingGoal = v.findViewById(R.id.tvSmartSavingGoal);
        tvSmartSavingProjection = v.findViewById(R.id.tvSmartSavingProjection);
        tvSmartSavingStatus = v.findViewById(R.id.tvSmartSavingStatus);
        tvWeeklyPlanTitle = v.findViewById(R.id.tvWeeklyPlanTitle);
        cardWeeklyPlan = v.findViewById(R.id.cardWeeklyPlan);
        tvWeeklyPlanStatus = v.findViewById(R.id.tvWeeklyPlanStatus);
        tvWeeklyPlanAmount = v.findViewById(R.id.tvWeeklyPlanAmount);
        tvWeeklyPlanMessage = v.findViewById(R.id.tvWeeklyPlanMessage);
        btnWeeklyPlanDetail = v.findViewById(R.id.btnWeeklyPlanDetail);
        tvRecommendationsPro = v.findViewById(R.id.tvRecommendationsPro);
        chipRecommendations = v.findViewById(R.id.chipRecommendations);
        chartCategorias = v.findViewById(R.id.chartHomeCategorias);
        chartTrend = v.findViewById(R.id.chartHomeTrend);
        chipAlerts = v.findViewById(R.id.chipHomeAlerts);
        chipInsights = v.findViewById(R.id.chipHomeInsights);

        viewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);
        viewModel.clearCacheIfUserChanged();
        currencyCode = SettingsService.getCurrencyCode(requireContext());

        setupMenu();
        setupCharts();
        setupNavigation(v);
        btnWeeklyPlanDetail.setOnClickListener(view -> showWeeklyPlanDialog());
        swipe.setOnRefreshListener(() -> {
            manualRefresh = true;
            cargarResumen(true);
        });
        HomeSummary cachedSummary = viewModel.getSummary().getValue();
        Calendar cal = Calendar.getInstance();
        if (cachedSummary != null && viewModel.hasFreshSummary(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)) {
            render(cachedSummary);
        }
        observeViewModel();
        maybeShowInitialCurrencyDialog();
        PerfLogger.logSince("HomeFragment", "onViewCreated", perfStartMs);
    }

    @Override
    public void onResume() {
        super.onResume();
        currencyCode = SettingsService.getCurrencyCode(requireContext());
        cargarResumen(false);
    }

    private void setupMenu() {
        MenuHost menuHost = requireActivity();
        menuHost.addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menu.clear();
                menuInflater.inflate(R.menu.menu_home, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.action_customize_dashboard) {
                    showDashboardModulesDialog();
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }

    private void cargarResumen() {
        cargarResumen(false);
    }

    private void cargarResumen(boolean force) {
        Calendar cal = Calendar.getInstance();
        viewModel.loadSummary(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, force);
    }

    private void observeViewModel() {
        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            boolean isLoading = Boolean.TRUE.equals(loading);
            boolean hasContent = lastSummary != null;
            if (progress != null) progress.setVisibility(isLoading && !hasContent ? View.VISIBLE : View.GONE);
            if (swipe != null) swipe.setRefreshing(isLoading && manualRefresh);
            if (!isLoading) {
                manualRefresh = false;
            }
        });

        viewModel.getCurrencyCode().observe(getViewLifecycleOwner(), code -> {
            if (code != null && !code.trim().isEmpty()) {
                currencyCode = code;
                HomeSummary summary = viewModel.getSummary().getValue();
                if (summary != null && isAdded()) render(summary);
            }
        });

        viewModel.getSummary().observe(getViewLifecycleOwner(), summary -> {
            if (summary != null && isAdded()) render(summary);
        });

        viewModel.getSmartAlert().observe(getViewLifecycleOwner(), alert -> {
            smartAlertMessage = alert;
            HomeSummary summary = viewModel.getSummary().getValue();
            if (summary != null && isAdded()) renderAlerts(summary);
        });

        viewModel.getInsights().observe(getViewLifecycleOwner(), insights -> {
            latestInsights.clear();
            if (insights != null) latestInsights.addAll(insights);
            renderInsights();
        });

        viewModel.getError().observe(getViewLifecycleOwner(), ignored ->
                UiFormUtils.showMessage(requireView(), R.string.error_cargar_transacciones)
        );
    }

    private void render(@NonNull HomeSummary summary) {
        lastSummary = summary;
        if (!firstRenderLogged) {
            firstRenderLogged = true;
            PerfLogger.logSince("HomeFragment", "firstRender", perfStartMs);
        }
        applyModulePreferences(summary);
        tvPeriod.setText(Format.monthYear(summary.getAnio(), summary.getMes()));
        tvIngresos.setText(Format.money(summary.getIngresos(), currencyCode));
        tvGastos.setText(Format.money(summary.getGastos(), currencyCode));
        tvBalance.setText(Format.money(summary.getSaldoActualTotal(), currencyCode));
        tvCash.setText(Format.money(summary.getEfectivo(), currencyCode));
        tvCard.setText(Format.money(summary.getTarjetaCuenta(), currencyCode));

        double budgetPercent = summary.getPresupuestoPorcentaje();
        tvBudgetUsed.setText(summary.getPresupuestoMonto() > 0
                ? getString(R.string.home_budget_used_value, Math.min(999, Math.round(budgetPercent)))
                : getString(R.string.home_budget_no_budget));
        tvBudgetDetail.setText(summary.getPresupuestoMonto() > 0
                ? getString(
                        R.string.home_budget_detail,
                        Format.money(summary.getGastos(), currencyCode),
                        Format.money(summary.getPresupuestoMonto(), currencyCode),
                        Format.money(summary.getPresupuestoRestante(), currencyCode)
                )
                : getString(R.string.home_budget_missing_hint));

        boolean hasData = summary.getIngresos() > 0
                || summary.getGastos() > 0
                || summary.getSaldoActualTotal() > 0
                || !summary.getChartCategorias().isEmpty();
        emptyState.setVisibility(hasData ? View.GONE : View.VISIBLE);

        applyModuleVisibility(hasData);
        renderCategoryChart(summary.getChartCategorias());
        renderTrendChart(summary.getTendenciaMensual());
        renderAlerts(summary);
        renderInsights();
    }

    private String buildComparisonText(@NonNull HomeSummary summary) {
        if (summary.getGastosMesAnterior() <= 0) {
            return getString(R.string.home_comparison_no_previous);
        }
        double change = summary.getVariacionGastosPorcentaje();
        String direction = change > 0
                ? getString(R.string.home_comparison_more)
                : getString(R.string.home_comparison_less);
        return getString(R.string.home_comparison_value, Math.abs(Math.round(change)), direction);
    }

    private String buildTopCategoryText(@NonNull HomeSummary summary) {
        String name = summary.getCategoriaMayorGasto();
        if (name == null || name.trim().isEmpty() || summary.getCategoriaMayorGastoMonto() <= 0) {
            return getString(R.string.home_top_category_empty);
        }
        return getString(
                R.string.home_top_category_value,
                safeLabel(name),
                Format.money(summary.getCategoriaMayorGastoMonto(), currencyCode)
        );
    }

    private String financialStateText(@Nullable String state) {
        if ("EXCEDIDO".equals(state)) return getString(R.string.home_financial_state_exceeded);
        if ("RIESGO".equals(state)) return getString(R.string.home_financial_state_risk);
        return getString(R.string.home_financial_state_controlled);
    }

    private void renderCategoryChart(@Nullable List<CategoryChartSlice> slices) {
        List<CategoryChartSlice> safe = slices == null ? new ArrayList<>() : slices;
        boolean empty = safe.isEmpty();
        tvCategoryEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        chartCategorias.setVisibility(empty ? View.GONE : View.VISIBLE);
        if (empty) {
            chartCategorias.clear();
            return;
        }

        ArrayList<PieEntry> entries = new ArrayList<>();
        for (CategoryChartSlice slice : safe) {
            if (slice == null || slice.getGastado() <= 0) continue;
            entries.add(new PieEntry((float) slice.getGastado(), safeLabel(slice.getCategoriaNombre())));
        }
        if (entries.isEmpty()) {
            tvCategoryEmpty.setVisibility(View.VISIBLE);
            chartCategorias.setVisibility(View.GONE);
            chartCategorias.clear();
            return;
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(chartColors());
        dataSet.setSliceSpace(2f);
        dataSet.setValueTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurface));
        dataSet.setValueTextSize(11f);

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return Format.money(value, currencyCode);
            }
        });
        chartCategorias.setData(data);
        chartCategorias.setUsePercentValues(false);
        chartCategorias.setDrawEntryLabels(false);
        chartCategorias.setHoleRadius(58f);
        chartCategorias.setTransparentCircleRadius(62f);
        chartCategorias.setCenterText(getString(R.string.home_category_chart_center));
        chartCategorias.setCenterTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurface));
        chartCategorias.invalidate();
    }

    private void renderTrendChart(@Nullable List<MonthlyTrendPoint> points) {
        List<MonthlyTrendPoint> safe = points == null ? new ArrayList<>() : points;
        boolean empty = safe.isEmpty() || !hasTrendValues(safe);
        tvTrendEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        chartTrend.setVisibility(empty ? View.GONE : View.VISIBLE);
        if (empty) {
            chartTrend.clear();
            return;
        }

        ArrayList<Entry> ingresos = new ArrayList<>();
        ArrayList<Entry> gastos = new ArrayList<>();
        ArrayList<Entry> balance = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();
        float min = 0f;

        for (int i = 0; i < safe.size(); i++) {
            MonthlyTrendPoint point = safe.get(i);
            if (point == null) continue;
            ingresos.add(new Entry(i, (float) point.getIngresos()));
            gastos.add(new Entry(i, (float) point.getGastos()));
            balance.add(new Entry(i, (float) point.getSaldo()));
            labels.add(point.getEtiqueta() == null ? "" : point.getEtiqueta());
            min = Math.min(min, (float) point.getSaldo());
        }

        LineData data = new LineData(
                lineSet(ingresos, getString(R.string.home_ingresos), R.color.income),
                lineSet(gastos, getString(R.string.home_gastos), R.color.expense),
                lineSet(balance, getString(R.string.home_balance), R.color.chartBalance)
        );
        data.setDrawValues(false);
        chartTrend.setData(data);

        XAxis xAxis = chartTrend.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setGranularity(1f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurfaceVariant));

        YAxis left = chartTrend.getAxisLeft();
        left.setAxisMinimum(min < 0f ? min * 1.1f : 0f);
        left.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurfaceVariant));
        left.setGridColor(ContextCompat.getColor(requireContext(), R.color.md_theme_outlineVariant));
        left.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return shortMoney(value);
            }
        });
        chartTrend.getAxisRight().setEnabled(false);
        chartTrend.invalidate();
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

    private void renderAlerts(@NonNull HomeSummary summary) {
        chipAlerts.removeAllViews();
        ArrayList<String> alerts = new ArrayList<>(summary.getAlertas());
        if (smartAlertMessage != null && !smartAlertMessage.trim().isEmpty()) {
            alerts.add(0, smartAlertMessage.trim());
        }

        tvAlertsEmpty.setVisibility(alerts.isEmpty() ? View.VISIBLE : View.GONE);
        chipAlerts.setVisibility(alerts.isEmpty() ? View.GONE : View.VISIBLE);
        for (String alert : alerts) addChip(chipAlerts, alert, true);
    }

    private void renderInsights() {
        chipInsights.removeAllViews();
        chipInsights.setVisibility(View.GONE);

        if (lastSummary == null) {
            tvInsightsEmpty.setVisibility(View.VISIBLE);
            return;
        }

        tvInsightsEmpty.setVisibility(View.GONE);
        tvFinancialInsight.setText(nonEmpty(lastSummary.getInsightPrincipal(), getString(R.string.home_financial_missing)));
        tvFinancialProjection.setText(getString(
                R.string.home_financial_projection_value,
                Format.money(lastSummary.getProyeccionFinMes(), currencyCode),
                Format.money(lastSummary.getGastoPromedioDiario(), currencyCode),
                nonEmpty(lastSummary.getConfianzaProyeccion(), getString(R.string.home_projection_confidence_low))
        ));
        tvFinancialAlert.setText(nonEmpty(lastSummary.getAlertaPrincipal(), getString(R.string.home_alerts_empty)));
        tvFinancialScore.setText(getString(
                R.string.home_financial_score_value,
                lastSummary.getScoreFinanciero(),
                nonEmpty(lastSummary.getScoreEstado(), getString(R.string.home_financial_score_risk))
        ));
        tvFinancialScoreExplanation.setText(nonEmpty(
                lastSummary.getScoreExplicacion(),
                getString(R.string.home_financial_missing)
        ));
        tvFinancialScoreTrend.setText(nonEmpty(
                lastSummary.getScoreTendencia(),
                getString(R.string.home_comparison_no_previous)
        ));
        renderWeeklyPlan(lastSummary.getWeeklyPlan());
        renderSmartSaving(lastSummary);
    }

    private boolean hasTrendValues(@NonNull List<MonthlyTrendPoint> points) {
        for (MonthlyTrendPoint point : points) {
            if (point == null) continue;
            if (point.getIngresos() != 0.0 || point.getGastos() != 0.0 || point.getSaldo() != 0.0) {
                return true;
            }
        }
        return false;
    }

    private void renderSmartSaving(@NonNull HomeSummary summary) {
        if (summary.getAhorroSugerido() > 0) {
            tvSmartSavingSuggested.setText(getString(
                    R.string.home_smart_saving_suggested_value,
                    Format.money(summary.getAhorroSugerido(), currencyCode)
            ));
        } else {
            tvSmartSavingSuggested.setText(nonEmpty(
                    summary.getAhorroSugeridoMensaje(),
                    getString(R.string.home_smart_saving_not_recommended)
            ));
        }
        tvSmartSavingGoal.setText(nonEmpty(
                summary.getRecomendacionAhorroMeta(),
                getString(R.string.home_smart_saving_goal_empty)
        ));
        tvSmartSavingProjection.setText(getString(
                R.string.home_smart_saving_projection_value,
                Format.money(summary.getSaldoActualTotal(), currencyCode),
                Format.money(summary.getGastoProyectado(), currencyCode),
                Format.money(summary.getProyeccionFinMes(), currencyCode)
        ));
        tvSmartSavingStatus.setText(nonEmpty(
                summary.getEstadoAhorro(),
                getString(R.string.home_smart_saving_status_adjusted)
        ));
        renderRecommendations(summary);
    }

    private void renderWeeklyPlan(@Nullable WeeklyPlanSummary plan) {
        if (cardWeeklyPlan == null) return;
        if (plan == null) {
            cardWeeklyPlan.setVisibility(View.GONE);
            if (tvWeeklyPlanTitle != null) tvWeeklyPlanTitle.setVisibility(View.GONE);
            return;
        }
        if (tvWeeklyPlanTitle != null) tvWeeklyPlanTitle.setVisibility(View.VISIBLE);
        cardWeeklyPlan.setVisibility(View.VISIBLE);
        tvWeeklyPlanStatus.setText(nonEmpty(plan.getEstado(), getString(R.string.home_weekly_plan_state_caution)));
        tvWeeklyPlanStatus.setTextColor(weeklyPlanStateColor(plan.getEstado()));
        if (plan.getMontoSemanal() > 0.0) {
            tvWeeklyPlanAmount.setText(getString(
                    R.string.home_weekly_plan_amount,
                    Format.money(plan.getMontoSemanal(), currencyCode)
            ));
        } else {
            tvWeeklyPlanAmount.setText(R.string.home_weekly_plan_minimum);
        }
        tvWeeklyPlanMessage.setText(nonEmpty(plan.getMensaje(), getString(R.string.home_weekly_plan_empty_hint)));
    }

    private int weeklyPlanStateColor(@Nullable String state) {
        if ("Ajustado".equalsIgnoreCase(state)) {
            return ContextCompat.getColor(requireContext(), R.color.risk_high_text);
        }
        if ("Cuidado".equalsIgnoreCase(state)) {
            return ContextCompat.getColor(requireContext(), R.color.risk_medium_text);
        }
        return ContextCompat.getColor(requireContext(), R.color.income);
    }

    private void showWeeklyPlanDialog() {
        if (lastSummary == null || lastSummary.getWeeklyPlan() == null || !isAdded()) return;
        WeeklyPlanSummary plan = lastSummary.getWeeklyPlan();
        View content = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_weekly_plan, null, false);
        TextView tvAvailable = content.findViewById(R.id.tvWeeklyPlanDetailAvailable);
        TextView tvBasics = content.findViewById(R.id.tvWeeklyPlanDetailBasics);
        TextView tvDaily = content.findViewById(R.id.tvWeeklyPlanDetailDaily);
        TextView tvPreliminary = content.findViewById(R.id.tvWeeklyPlanPreliminary);
        ChipGroup chipCategories = content.findViewById(R.id.chipWeeklyPlanCategories);
        ChipGroup chipWarnings = content.findViewById(R.id.chipWeeklyPlanWarnings);

        tvAvailable.setText(plan.getMontoSemanal() > 0.0
                ? getString(R.string.home_weekly_plan_detail_available, Format.money(plan.getMontoSemanal(), currencyCode))
                : getString(R.string.home_weekly_plan_minimum));
        tvBasics.setText(getString(
                R.string.home_weekly_plan_detail_basics,
                Format.money(plan.getSaldoActual(), currencyCode),
                Format.money(plan.getBalanceMensual(), currencyCode),
                nonEmpty(plan.getEstado(), getString(R.string.home_weekly_plan_state_caution))
        ));
        tvDaily.setText(getString(
                R.string.home_weekly_plan_daily,
                Format.money(plan.getGastoDiario(), currencyCode)
        ));
        tvPreliminary.setText(nonEmpty(plan.getDetalleConfianza(), getString(R.string.home_weekly_plan_confidence_default)));

        chipCategories.removeAllViews();
        for (WeeklyPlanCategory category : plan.getCategorias()) {
            if (category == null) continue;
            String name = safeLabel(category.getNombre());
            String label = getString(R.string.home_weekly_plan_category_value, name, Format.money(category.getMonto(), currencyCode));
            addChip(chipCategories, label, category.isLimitadoPorPresupuesto());
        }

        chipWarnings.removeAllViews();
        for (String warning : plan.getAdvertencias()) {
            addChip(chipWarnings, warning, true);
        }
        chipWarnings.setVisibility(plan.getAdvertencias().isEmpty() ? View.GONE : View.VISIBLE);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.home_weekly_plan_title)
                .setView(content)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void renderRecommendations(@NonNull HomeSummary summary) {
        chipRecommendations.removeAllViews();
        if (!summary.isProUser()) {
            tvRecommendationsPro.setVisibility(View.VISIBLE);
            tvRecommendationsPro.setText(R.string.home_pro_locked_recommendations);
            chipRecommendations.setVisibility(View.GONE);
            return;
        }

        ArrayList<String> recommendations = new ArrayList<>(summary.getRecomendacionesInteligentes());
        tvRecommendationsPro.setVisibility(recommendations.isEmpty() ? View.VISIBLE : View.GONE);
        tvRecommendationsPro.setText(R.string.home_recommendations_empty);
        chipRecommendations.setVisibility(recommendations.isEmpty() ? View.GONE : View.VISIBLE);
        for (String recommendation : recommendations) addChip(chipRecommendations, recommendation, false);
    }

    private void addChip(@NonNull ChipGroup group, @Nullable String text, boolean alert) {
        if (text == null || text.trim().isEmpty()) return;
        Chip chip = new Chip(requireContext());
        chip.setText(text.trim());
        chip.setCheckable(false);
        chip.setClickable(false);
        chip.setChipBackgroundColorResource(alert ? R.color.md_theme_errorContainer : R.color.md_theme_secondaryContainer);
        chip.setTextColor(ContextCompat.getColor(requireContext(), alert ? R.color.md_theme_onErrorContainer : R.color.md_theme_onSurface));
        group.addView(chip);
    }

    private void setupCharts() {
        chartCategorias.getDescription().setEnabled(false);
        chartCategorias.setNoDataText(getString(R.string.chart_no_data));
        chartCategorias.setNoDataTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurfaceVariant));
        Legend pieLegend = chartCategorias.getLegend();
        pieLegend.setWordWrapEnabled(true);
        pieLegend.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurfaceVariant));

        chartTrend.getDescription().setEnabled(false);
        chartTrend.setNoDataText(getString(R.string.chart_no_data));
        chartTrend.setNoDataTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurfaceVariant));
        chartTrend.setScaleXEnabled(false);
        chartTrend.setScaleYEnabled(false);
        chartTrend.setDoubleTapToZoomEnabled(false);
        Legend trendLegend = chartTrend.getLegend();
        trendLegend.setWordWrapEnabled(true);
        trendLegend.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurfaceVariant));
    }

    private LineDataSet lineSet(ArrayList<Entry> entries, String label, int colorRes) {
        int color = ContextCompat.getColor(requireContext(), colorRes);
        LineDataSet set = new LineDataSet(entries, label);
        set.setColor(color);
        set.setCircleColor(color);
        set.setLineWidth(2.4f);
        set.setCircleRadius(3.6f);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        return set;
    }

    private ArrayList<Integer> chartColors() {
        ArrayList<Integer> colors = new ArrayList<>();
        colors.add(ContextCompat.getColor(requireContext(), R.color.expense));
        colors.add(ContextCompat.getColor(requireContext(), R.color.md_theme_primary));
        colors.add(ContextCompat.getColor(requireContext(), R.color.chartBudget));
        colors.add(ContextCompat.getColor(requireContext(), R.color.chartBalance));
        colors.add(ContextCompat.getColor(requireContext(), R.color.md_theme_secondary));
        colors.add(ContextCompat.getColor(requireContext(), R.color.risk_medium_text));
        colors.add(ContextCompat.getColor(requireContext(), R.color.risk_high_text));
        colors.add(ContextCompat.getColor(requireContext(), R.color.chartAccent));
        return colors;
    }

    private void setupNavigation(@NonNull View root) {
        root.findViewById(R.id.btnLista).setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.nav_list));
        root.findViewById(R.id.btnPresupuesto).setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.nav_budget));
        root.findViewById(R.id.btnMetas).setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.nav_goals));
        root.findViewById(R.id.btnRecordatorios).setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.nav_reminders));
        root.findViewById(R.id.btnImportaciones).setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.nav_imports));
        root.findViewById(R.id.btnReceiptScan).setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.nav_receipt_scan));
        FloatingActionButton fabNueva = root.findViewById(R.id.fabNueva);
        fabNueva.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.nav_new));
    }

    private void applyModulePreferences(@NonNull HomeSummary summary) {
        moduleVisibility.clear();
        for (ModuleDef def : defaultModules()) {
            moduleVisibility.put(def.id, true);
        }
        for (DashboardModulePref pref : summary.getDashboardPreferencias()) {
            if (pref == null || pref.getId() == null) continue;
            moduleVisibility.put(pref.getId(), pref.isVisible());
        }
    }

    private void applyModuleVisibility(boolean hasData) {
        boolean summaryVisible = isModuleVisible(MODULE_SUMMARY);
        setVisible(moduleSummary, summaryVisible);
        setVisible(tvBudgetDetail, summaryVisible);
        setVisible(moduleCategory, hasData && isModuleVisible(MODULE_CATEGORY));
        setVisible(moduleTrend, hasData && isModuleVisible(MODULE_TREND));
        setVisible(moduleAlerts, hasData && isModuleVisible(MODULE_ALERTS));
        setVisible(moduleInsights, isModuleVisible(MODULE_INSIGHTS));
        setVisible(moduleQuick, isModuleVisible(MODULE_QUICK));
    }

    private boolean isModuleVisible(String id) {
        Boolean visible = moduleVisibility.get(id);
        return visible == null || visible;
    }

    private void setVisible(@Nullable View view, boolean visible) {
        if (view != null) view.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void showDashboardModulesDialog() {
        View content = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_dashboard_modules, null, false);
        androidx.recyclerview.widget.RecyclerView rv = content.findViewById(R.id.rvModules);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));

        DashboardModuleAdapter adapter = new DashboardModuleAdapter();
        adapter.setItems(buildModuleItems());
        rv.setAdapter(adapter);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dashboard_customize_title)
                .setView(content)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.dashboard_customize_save, (dialog, which) -> saveModulePreferences(adapter.getItems()))
                .show();
    }

    private List<DashboardModuleAdapter.ModuleItem> buildModuleItems() {
        List<DashboardModuleAdapter.ModuleItem> items = new ArrayList<>();
        for (ModuleDef def : defaultModules()) {
            items.add(new DashboardModuleAdapter.ModuleItem(def.id, getString(def.titleRes), isModuleVisible(def.id)));
        }
        return items;
    }

    private void saveModulePreferences(List<DashboardModuleAdapter.ModuleItem> items) {
        try {
            JSONArray modules = new JSONArray();
            for (DashboardModuleAdapter.ModuleItem item : items) {
                JSONObject obj = new JSONObject();
                obj.put("id", item.getId());
                obj.put("visible", item.isVisible());
                modules.put(obj);
                moduleVisibility.put(item.getId(), item.isVisible());
            }
            JSONObject body = new JSONObject();
            body.put("modules", modules);
            SettingsService.saveDashboard(requireContext(), body, new SettingsService.SaveCb() {
                @Override
                public void onSuccess() {
                    if (!isAdded()) return;
                    applyModuleVisibility(lastSummary == null || lastSummary.getIngresos() > 0 || lastSummary.getGastos() > 0 || lastSummary.getSaldoActualTotal() > 0);
                    UiFormUtils.showMessage(requireView(), R.string.dashboard_customize_saved);
                }

                @Override
                public void onFail() {
                    if (isAdded()) UiFormUtils.showMessage(requireView(), R.string.dashboard_customize_error);
                }
            });
        } catch (Exception e) {
            UiFormUtils.showMessage(requireView(), R.string.dashboard_customize_error);
        }
    }

    private List<ModuleDef> defaultModules() {
        List<ModuleDef> modules = new ArrayList<>();
        modules.add(new ModuleDef(MODULE_SUMMARY, R.string.dashboard_module_balance));
        modules.add(new ModuleDef(MODULE_CATEGORY, R.string.dashboard_module_chart_budget));
        modules.add(new ModuleDef(MODULE_TREND, R.string.dashboard_module_chart_trend));
        modules.add(new ModuleDef(MODULE_ALERTS, R.string.dashboard_module_alerts));
        modules.add(new ModuleDef(MODULE_INSIGHTS, R.string.home_insights_title));
        modules.add(new ModuleDef(MODULE_QUICK, R.string.dashboard_module_quick));
        return modules;
    }

    private String safeLabel(@Nullable String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? getString(R.string.home_uncategorized) : trimmed;
    }

    private void maybeShowInitialCurrencyDialog() {
        if (!isAdded() || SettingsService.hasCurrencyConfigured(requireContext())) {
            maybeShowTesterThanksDialog();
            return;
        }

        final boolean[] saved = { false };
        final String[] codes = new String[] { "PEN", "USD", "EUR", "CLP" };
        final String[] labels = new String[] {
                "PEN - Sol peruano - S/",
                "USD - Dolar estadounidense - $",
                "EUR - Euro - €",
                "CLP - Peso chileno - CLP$"
        };

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.initial_currency_title)
                .setItems(labels, (d, which) -> {
                    saved[0] = true;
                    saveInitialCurrencyChoice(codes[Math.max(0, Math.min(which, codes.length - 1))]);
                })
                .setNegativeButton(R.string.initial_currency_default, (d, which) -> {
                    saved[0] = true;
                    saveInitialCurrencyChoice("PEN");
                })
                .create();
        dialog.setOnCancelListener(d -> {
            if (!saved[0]) {
                saved[0] = true;
                saveInitialCurrencyChoice("PEN");
            }
        });
        dialog.show();
    }

    private void saveInitialCurrencyChoice(@NonNull String code) {
        SettingsService.saveCurrency(requireContext(), code, 0.0, new SettingsService.SaveCb() {
            @Override
            public void onSuccess() {
                if (!isAdded()) return;
                currencyCode = SettingsService.getCurrencyCode(requireContext());
                if (viewModel != null) {
                    viewModel.clearCache();
                    cargarResumen(true);
                }
                maybeShowTesterThanksDialog();
            }

            @Override
            public void onFail() {
                if (!isAdded()) return;
                SettingsService.saveCurrency(requireContext(), "PEN", 0.0, new SettingsService.SaveCb() {
                    @Override public void onSuccess() { }
                    @Override public void onFail() { }
                });
                maybeShowTesterThanksDialog();
            }
        });
    }

    private void maybeShowTesterThanksDialog() {
        if (!isAdded() || testerThanksDialogShowing || Prefs.hasSeenTesterThanks(requireContext())) {
            return;
        }
        testerThanksDialogShowing = true;
        View content = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_tester_thanks, null, false);
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(content)
                .setPositiveButton(R.string.tester_thanks_action, (d, which) -> {
                    if (isAdded()) {
                        Prefs.markTesterThanksSeen(requireContext());
                    }
                })
                .create();
        dialog.setOnCancelListener(d -> {
            if (isAdded()) {
                Prefs.markTesterThanksSeen(requireContext());
            }
        });
        dialog.setOnDismissListener(d -> testerThanksDialogShowing = false);
        dialog.show();
    }

    private String nonEmpty(@Nullable String value, @NonNull String fallback) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    private static class ModuleDef {
        final String id;
        final int titleRes;

        ModuleDef(String id, int titleRes) {
            this.id = id;
            this.titleRes = titleRes;
        }
    }
}
