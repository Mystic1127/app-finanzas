package com.example.finanzas.ui;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finanzas.R;
import com.example.finanzas.data.api.SettingsService;
import com.example.finanzas.data.model.CategoryChartSlice;
import com.example.finanzas.data.model.DashboardModulePref;
import com.example.finanzas.data.model.HomeSummary;
import com.example.finanzas.data.model.MonthlyTrendPoint;
import com.example.finanzas.data.model.PaymentReminder;
import com.example.finanzas.data.model.SavingsGoal;
import com.example.finanzas.ui.adapter.DashboardModuleAdapter;
import com.example.finanzas.ui.viewmodel.HomeViewModel;
import com.example.finanzas.util.Format;
import com.example.finanzas.util.FinancialAlertNotifier;
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
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HomeFragment extends Fragment {

    private static final String MODULE_SUMMARY = "summary";
    private static final String MODULE_CATEGORY = "category";
    private static final String MODULE_TREND = "trend";
    private static final String MODULE_ALERTS = "alerts";
    private static final String MODULE_INSIGHTS = "insights";
    private static final String MODULE_GOALS = "goals";
    private static final String MODULE_REMINDERS = "reminders";
    private static final String MODULE_QUICK = "quick";

    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipe;
    private CircularProgressIndicator progress;
    private View emptyState;
    private LinearLayout dashboardContainer;
    private View moduleSummary;
    private View moduleCategory;
    private View moduleTrend;
    private View moduleAlerts;
    private View moduleInsights;
    private View moduleGoals;
    private View moduleReminders;
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
    private TextView tvFinancialAlert;
    private TextView tvFinancialScore;
    private TextView tvSmartSavingSuggested;
    private TextView tvSmartSavingGoal;
    private TextView tvSmartSavingStatus;
    private TextView tvGoalTitle;
    private TextView tvGoalProgress;
    private TextView tvGoalMessage;
    private TextView tvReminderTitle;
    private TextView tvReminderDate;
    private TextView tvReminderStatus;
    private TextView tvRecommendationsPro;
    private MaterialButton btnFinancialDetail;
    private MaterialButton btnSmartSavingDetail;
    private ChipGroup chipRecommendations;
    private PieChart chartCategorias;
    private LineChart chartTrend;
    private LinearLayout chipAlerts;
    private ChipGroup chipInsights;
    private HomeViewModel viewModel;
    private String smartAlertMessage;
    private String currencyCode = "PEN";
    private final List<String> latestInsights = new ArrayList<>();
    private final Map<String, Boolean> moduleVisibility = new HashMap<>();
    private final List<String> moduleOrder = new ArrayList<>();
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
        dashboardContainer = v.findViewById(R.id.homeDashboardContainer);
        moduleSummary = v.findViewById(R.id.moduleHomeSummary);
        moduleCategory = v.findViewById(R.id.moduleHomeCategory);
        moduleTrend = v.findViewById(R.id.moduleHomeTrend);
        moduleAlerts = v.findViewById(R.id.moduleHomeAlerts);
        moduleInsights = v.findViewById(R.id.moduleHomeInsights);
        moduleGoals = v.findViewById(R.id.moduleHomeGoals);
        moduleReminders = v.findViewById(R.id.moduleHomeReminders);
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
        tvFinancialAlert = v.findViewById(R.id.tvFinancialAlert);
        tvFinancialScore = v.findViewById(R.id.tvFinancialScore);
        tvSmartSavingSuggested = v.findViewById(R.id.tvSmartSavingSuggested);
        tvSmartSavingGoal = v.findViewById(R.id.tvSmartSavingGoal);
        tvSmartSavingStatus = v.findViewById(R.id.tvSmartSavingStatus);
        tvGoalTitle = v.findViewById(R.id.tvHomeGoalTitle);
        tvGoalProgress = v.findViewById(R.id.tvHomeGoalProgress);
        tvGoalMessage = v.findViewById(R.id.tvHomeGoalMessage);
        tvReminderTitle = v.findViewById(R.id.tvHomeReminderTitle);
        tvReminderDate = v.findViewById(R.id.tvHomeReminderDate);
        tvReminderStatus = v.findViewById(R.id.tvHomeReminderStatus);
        btnFinancialDetail = v.findViewById(R.id.btnFinancialDetail);
        btnSmartSavingDetail = v.findViewById(R.id.btnSmartSavingDetail);
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
        btnFinancialDetail.setOnClickListener(view -> showFinancialDetail());
        btnSmartSavingDetail.setOnClickListener(view -> showSmartSavingDetail());
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
        renderGoalSummary(summary);
        renderReminderSummary(summary);
        FinancialAlertNotifier.maybeNotifyImportantAlert(requireContext(), summary, currencyCode);
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
        applyPieChartTheme();
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
        dataSet.setValueTextSize(10.5f);

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
        chartCategorias.setCenterTextSize(14f);
        chartCategorias.invalidate();
    }

    private void renderTrendChart(@Nullable List<MonthlyTrendPoint> points) {
        applyTrendChartTheme();
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
                lineSet(ingresos, getString(R.string.home_ingresos), R.color.chart_line_income),
                lineSet(gastos, getString(R.string.home_gastos), R.color.chart_line_expense),
                lineSet(balance, getString(R.string.home_balance), R.color.chart_line_balance)
        );
        data.setDrawValues(false);
        chartTrend.setData(data);

        XAxis xAxis = chartTrend.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setGranularity(1f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurfaceVariant));
        xAxis.setAxisLineColor(ContextCompat.getColor(requireContext(), R.color.md_theme_outlineVariant));

        YAxis left = chartTrend.getAxisLeft();
        left.setAxisMinimum(min < 0f ? min * 1.1f : 0f);
        left.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurfaceVariant));
        left.setGridColor(ContextCompat.getColor(requireContext(), R.color.md_theme_outlineVariant));
        left.setAxisLineColor(ContextCompat.getColor(requireContext(), R.color.md_theme_outlineVariant));
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
        LinkedHashSet<String> alertSet = new LinkedHashSet<>(summary.getAlertas());
        if (smartAlertMessage != null && !smartAlertMessage.trim().isEmpty()) {
            alertSet.add(smartAlertMessage.trim());
        }
        ArrayList<String> alerts = new ArrayList<>(alertSet);

        int count = 0;
        for (String alert : alerts) {
            if (!isActionableNotice(alert)) continue;
            addAlertRow(chipAlerts, alert, alertTone(alert));
            count++;
        }
        for (String predictive : summary.getAlertasPredictivas()) {
            if (!isActionableNotice(predictive)) continue;
            addAlertRow(chipAlerts, predictive, AlertTone.WARNING);
            count++;
        }

        boolean show = count > 0 && isModuleVisible(MODULE_ALERTS);
        tvAlertsEmpty.setVisibility(View.GONE);
        chipAlerts.setVisibility(show ? View.VISIBLE : View.GONE);
        setVisible(moduleAlerts, show);
    }

    private void renderInsights() {
        chipInsights.removeAllViews();
        chipInsights.setVisibility(View.GONE);

        if (lastSummary == null) {
            tvInsightsEmpty.setVisibility(View.VISIBLE);
            return;
        }

        tvInsightsEmpty.setVisibility(View.GONE);
        String financialState = financialStatusLabel(lastSummary);
        tvFinancialScore.setText(financialState);
        applyStatusStyle(tvFinancialScore, statusTone(financialState, lastSummary.isProyeccionPreliminar() && !"Estable".equals(financialState)));
        tvFinancialInsight.setText(shortText(
                nonEmpty(lastSummary.getInsightPrincipal(), getString(R.string.home_financial_missing)),
                120
        ));
        tvFinancialAlert.setText(shortText(financialSummaryAction(lastSummary), 110));
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
        tvSmartSavingStatus.setText(nonEmpty(
                summary.getEstadoAhorro(),
                getString(R.string.home_smart_saving_status_adjusted)
        ));
        applyStatusStyle(tvSmartSavingStatus, statusTone(summary.getEstadoAhorro(), summary.isProyeccionPreliminar()));
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
        tvSmartSavingGoal.setText(shortText(savingSummaryAction(summary), 110));
        renderRecommendations(summary);
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

    private String financialStatusLabel(@NonNull HomeSummary summary) {
        String state = nonEmpty(summary.getScoreEstado(), summary.isProyeccionPreliminar() ? "Preliminar" : getString(R.string.home_financial_score_risk));
        if (summary.isProyeccionPreliminar() && summary.getSaldoActualTotal() > 0.0 && !hasCriticalAlert(summary)) {
            state = "Estable";
        }
        return state;
    }

    private String financialSummaryAction(@NonNull HomeSummary summary) {
        if (summary.isProyeccionPreliminar()) {
            return "Registra mas movimientos para mejorar la precision.";
        }
        String alert = summary.getAlertaPrincipal();
        if (alert != null && !alert.trim().isEmpty() && !alert.equalsIgnoreCase(getString(R.string.home_alerts_empty))) {
            return alert.trim();
        }
        return "Revisa tus gastos principales una vez por semana.";
    }

    private String savingSummaryAction(@NonNull HomeSummary summary) {
        if (summary.isProyeccionPreliminar()) {
            return "Registra mas movimientos antes de apartar mas ahorro.";
        }
        if (summary.getAhorroSugerido() > 0.0) {
            return nonEmpty(summary.getRecomendacionAhorroMeta(), "Puedes avanzar con cautela este mes.");
        }
        return "Manten disponible tu saldo hasta tener mas datos.";
    }

    private boolean hasCriticalAlert(@NonNull HomeSummary summary) {
        for (String alert : summary.getAlertas()) {
            if (alertTone(alert) == AlertTone.CRITICAL) return true;
        }
        return false;
    }

    private boolean isActionableNotice(@Nullable String text) {
        String clean = text == null ? "" : text.toLowerCase(Locale.ROOT);
        if (clean.trim().isEmpty()) return false;
        if (clean.contains("preliminar") || clean.contains("saldo inicial") || clean.contains("registra mas")
                || clean.contains("registra más") || clean.contains("balance operativo")) {
            return false;
        }
        return clean.contains("presupuesto")
                || clean.contains("saldo bajo")
                || clean.contains("sin saldo suficiente")
                || clean.contains("vencid")
                || clean.contains("inusual")
                || clean.contains("subieron")
                || clean.contains("superad")
                || clean.contains("ritmo de gasto")
                || clean.contains("efectivo")
                || clean.contains("tarjeta")
                || clean.contains("cuenta");
    }

    private void showFinancialDetail() {
        if (lastSummary == null || !isAdded()) return;
        showDetailSheet(getString(R.string.home_financial_detail_title), buildFinancialDetailView(lastSummary));
    }

    private void showSmartSavingDetail() {
        if (lastSummary == null || !isAdded()) return;
        showDetailSheet(getString(R.string.home_smart_saving_detail_title), buildSmartSavingDetailView(lastSummary));
    }

    private void showDetailSheet(@NonNull String title, @NonNull View content) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(12), dp(22), dp(22));
        root.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.dialog_surface));

        View handle = new View(requireContext());
        GradientDrawable handleBg = new GradientDrawable();
        handleBg.setColor(ContextCompat.getColor(requireContext(), R.color.md_theme_outlineVariant));
        handleBg.setCornerRadius(dp(3));
        handle.setBackground(handleBg);
        LinearLayout.LayoutParams handleParams = new LinearLayout.LayoutParams(dp(42), dp(4));
        handleParams.gravity = android.view.Gravity.CENTER_HORIZONTAL;
        handleParams.bottomMargin = dp(18);
        root.addView(handle, handleParams);

        TextView titleView = new TextView(requireContext());
        titleView.setText(title);
        titleView.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurface));
        titleView.setTextSize(20);
        titleView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(titleView, matchWrapParams());

        ScrollView scroll = new ScrollView(requireContext());
        scroll.setFillViewport(false);
        scroll.setClipToPadding(false);
        scroll.setPadding(0, dp(10), 0, dp(4));
        scroll.addView(content);
        LinearLayout.LayoutParams scrollParams = matchWrapParams();
        scrollParams.topMargin = dp(6);
        root.addView(scroll, scrollParams);

        MaterialButton close = new MaterialButton(requireContext());
        close.setText(android.R.string.ok);
        close.setMinHeight(dp(44));
        close.setCornerRadius(dp(14));
        close.setAllCaps(false);
        close.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onPrimary));
        close.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.md_theme_primary)));
        close.setOnClickListener(v -> dialog.dismiss());
        LinearLayout.LayoutParams closeParams = matchWrapParams();
        closeParams.topMargin = dp(12);
        root.addView(close, closeParams);

        dialog.setContentView(root);
        dialog.setOnShowListener(d -> {
            FrameLayout bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                bottomSheet.setBackground(new ColorDrawable(Color.TRANSPARENT));
                bottomSheet.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
            }
        });
        dialog.show();
    }

    private View buildFinancialDetailView(@NonNull HomeSummary summary) {
        LinearLayout root = detailRoot();
        String status = financialStatusLabel(summary);
        String insight = nonEmpty(summary.getInsightPrincipal(), getString(R.string.home_financial_missing));
        AlertTone tone = statusTone(status, summary.isProyeccionPreliminar());
        addDetailHeader(root, status, shortText(insight, 145), tone);
        addMetricGrid(root,
                metric("Score", getString(
                R.string.home_financial_score_value,
                summary.getScoreFinanciero(),
                nonEmpty(summary.getScoreEstado(), getString(R.string.home_financial_score_risk))
        )),
                metric("Saldo estimado", Format.money(summary.getProyeccionFinMes(), currencyCode)),
                metric("Confianza", nonEmpty(summary.getConfianzaProyeccion(), getString(R.string.home_projection_confidence_low)))
        );
        addRecommendationBlock(root, "Recomendacion principal", shortText(financialSummaryAction(summary), 150), tone);
        String explanation = shortText(nonEmpty(summary.getScoreExplicacion(), ""), 150);
        if (shouldShowSecondaryText(explanation, insight, financialSummaryAction(summary))) {
            addQuietNote(root, "Lectura breve", explanation);
        }
        List<String> notices = actionableItems(summary.getAlertas());
        if (!notices.isEmpty()) {
            String notice = shortText(notices.get(0), 150);
            addRecommendationBlock(root, "Aviso accionable", notice, alertTone(notice));
        }
        return root;
    }

    private View buildSmartSavingDetailView(@NonNull HomeSummary summary) {
        LinearLayout root = detailRoot();
        String status = nonEmpty(summary.getEstadoAhorro(), getString(R.string.home_smart_saving_status_adjusted));
        String conclusion = savingSummaryAction(summary);
        AlertTone tone = statusTone(status, summary.isProyeccionPreliminar());
        addDetailHeader(root, status, shortText(conclusion, 145), tone);
        addMetricGrid(root,
                metric("Ahorro sugerido", Format.money(summary.getAhorroSugerido(), currencyCode)),
                metric("Saldo actual", Format.money(summary.getSaldoActualTotal(), currencyCode)),
                metric("Saldo final", Format.money(summary.getProyeccionFinMes(), currencyCode))
        );
        String recommendation = nonEmpty(summary.getAhorroSugeridoMensaje(), getString(R.string.home_smart_saving_not_recommended));
        if (shouldShowSecondaryText(recommendation, conclusion)) {
            addRecommendationBlock(root, "Recomendacion principal", shortText(recommendation, 150), tone);
        }
        String goal = summary.getRecomendacionAhorroMeta();
        if (goal != null && !goal.trim().isEmpty()) {
            addQuietNote(root, "Meta vinculada", shortText(goal.trim(), 160));
        }
        return root;
    }

    private List<String> actionableItems(@NonNull List<String> values) {
        ArrayList<String> out = new ArrayList<>();
        for (String value : values) {
            if (isActionableNotice(value)) out.add(value);
        }
        return out;
    }

    private LinearLayout detailRoot() {
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = 0;
        root.setPadding(pad, dp(8), pad, 0);
        return root;
    }

    private String[] metric(@NonNull String label, @NonNull String value) {
        return new String[]{ label, value };
    }

    private void addDetailHeader(@NonNull LinearLayout root, @NonNull String chip, @NonNull String message, @NonNull AlertTone tone) {
        LinearLayout hero = new LinearLayout(requireContext());
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(dp(16), dp(16), dp(16), dp(16));
        setRoundedBackground(hero, alertBackgroundColor(tone), R.color.md_theme_outlineVariant, 20);

        TextView status = new TextView(requireContext());
        status.setText(chip);
        status.setTextSize(12);
        status.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        status.setTextColor(ContextCompat.getColor(requireContext(), alertTextColor(tone)));
        status.setBackgroundResource(statusBackground(tone));
        status.setPadding(dp(12), dp(7), dp(12), dp(7));
        hero.addView(status, compactWrapParams());

        TextView body = new TextView(requireContext());
        body.setText(message);
        body.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurface));
        body.setTextSize(17);
        body.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        body.setPadding(0, dp(12), 0, dp(8));
        body.setSingleLine(false);
        hero.addView(body, matchWrapParams());

        TextView caption = new TextView(requireContext());
        String captionText;
        if (tone == AlertTone.POSITIVE) {
            captionText = "Estado positivo";
        } else if (tone == AlertTone.CRITICAL) {
            captionText = "Requiere atencion";
        } else if (tone == AlertTone.WARNING) {
            captionText = "Analisis preliminar";
        } else {
            captionText = "Lectura rapida";
        }
        caption.setText(captionText);
        caption.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurfaceVariant));
        caption.setTextSize(12);
        hero.addView(caption, matchWrapParams());

        root.addView(hero, matchWrapParams());
    }

    private void addMetricGrid(@NonNull LinearLayout root, String[]... metrics) {
        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(0, dp(6), 0, dp(2));
        for (int i = 0; i < metrics.length; i += 2) {
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setBaselineAligned(false);
            addMetricCard(row, metrics[i]);
            if (i + 1 < metrics.length) {
                addMetricCard(row, metrics[i + 1]);
            } else {
                View spacer = new View(requireContext());
                row.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1f));
            }
            LinearLayout.LayoutParams rowParams = matchWrapParams();
            rowParams.topMargin = dp(8);
            container.addView(row, rowParams);
        }
        root.addView(container, matchWrapParams());
    }

    private void addMetricCard(@NonNull LinearLayout row, @Nullable String[] item) {
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        setRoundedBackground(card, R.color.md_theme_surface, R.color.md_theme_outlineVariant, 16);

        String label = item == null || item.length < 1 ? "" : item[0];
        String value = item == null || item.length < 2 ? "" : item[1];

        TextView left = new TextView(requireContext());
        left.setText(label);
        left.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurfaceVariant));
        left.setTextSize(12);
        card.addView(left, matchWrapParams());

        TextView right = new TextView(requireContext());
        right.setText(value);
        right.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurface));
        right.setTextSize(15);
        right.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        right.setSingleLine(false);
        right.setPadding(0, dp(5), 0, 0);
        card.addView(right, matchWrapParams());

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        params.setMarginEnd(dp(8));
        row.addView(card, params);
    }

    private void addDetailSection(@NonNull LinearLayout root, @NonNull String title, @NonNull String body, @NonNull AlertTone tone) {
        TextView titleView = new TextView(requireContext());
        titleView.setText(title);
        titleView.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurface));
        titleView.setTextSize(14);
        titleView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams titleParams = matchWrapParams();
        titleParams.topMargin = dp(14);
        root.addView(titleView, titleParams);
        addAlertRow(root, body, tone);
    }

    private void addRecommendationBlock(@NonNull LinearLayout root, @NonNull String title, @NonNull String body, @NonNull AlertTone tone) {
        if (body == null || body.trim().isEmpty()) return;
        LinearLayout box = new LinearLayout(requireContext());
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(15), dp(13), dp(15), dp(14));
        setRoundedBackground(box, alertBackgroundColor(tone), R.color.md_theme_outlineVariant, 18);

        TextView titleView = new TextView(requireContext());
        titleView.setText(title);
        titleView.setTextColor(ContextCompat.getColor(requireContext(), alertTextColor(tone)));
        titleView.setTextSize(12);
        titleView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        box.addView(titleView, matchWrapParams());

        TextView bodyView = new TextView(requireContext());
        bodyView.setText(body.trim());
        bodyView.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurface));
        bodyView.setTextSize(15);
        bodyView.setSingleLine(false);
        bodyView.setMaxLines(Integer.MAX_VALUE);
        bodyView.setPadding(0, dp(6), 0, 0);
        box.addView(bodyView, matchWrapParams());

        LinearLayout.LayoutParams params = matchWrapParams();
        params.topMargin = dp(12);
        root.addView(box, params);
    }

    private void addQuietNote(@NonNull LinearLayout root, @NonNull String title, @NonNull String body) {
        if (body == null || body.trim().isEmpty()) return;
        LinearLayout note = new LinearLayout(requireContext());
        note.setOrientation(LinearLayout.VERTICAL);
        note.setPadding(dp(4), dp(4), dp(4), 0);

        TextView titleView = new TextView(requireContext());
        titleView.setText(title);
        titleView.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurface));
        titleView.setTextSize(13);
        titleView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        note.addView(titleView, matchWrapParams());

        TextView bodyView = new TextView(requireContext());
        bodyView.setText(body.trim());
        bodyView.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurfaceVariant));
        bodyView.setTextSize(14);
        bodyView.setSingleLine(false);
        bodyView.setMaxLines(Integer.MAX_VALUE);
        bodyView.setPadding(0, dp(5), 0, 0);
        note.addView(bodyView, matchWrapParams());

        LinearLayout.LayoutParams params = matchWrapParams();
        params.topMargin = dp(12);
        root.addView(note, params);
    }

    private boolean shouldShowSecondaryText(@Nullable String text, @Nullable String... existingValues) {
        String clean = normalizedDetailText(text);
        if (clean.isEmpty()) return false;
        for (String existing : existingValues) {
            String other = normalizedDetailText(existing);
            if (!other.isEmpty() && (clean.equals(other) || clean.contains(other) || other.contains(clean))) {
                return false;
            }
        }
        return true;
    }

    @NonNull
    private String normalizedDetailText(@Nullable String value) {
        if (value == null) return "";
        return value.trim().toLowerCase(Locale.ROOT).replace(".", "").replace(",", "");
    }

    private void addLimitedList(@NonNull LinearLayout root, @NonNull String title, @NonNull List<String> values, @NonNull AlertTone tone, int maxItems) {
        if (values == null || values.isEmpty()) return;
        int added = 0;
        TextView titleView = new TextView(requireContext());
        titleView.setText(title);
        titleView.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurface));
        titleView.setTextSize(14);
        titleView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams titleParams = matchWrapParams();
        titleParams.topMargin = dp(14);
        root.addView(titleView, titleParams);
        for (String value : values) {
            if (value == null || value.trim().isEmpty()) continue;
            addAlertRow(root, value, tone);
            added++;
            if (added >= maxItems) break;
        }
    }

    private LinearLayout.LayoutParams compactWrapParams() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams matchWrapParams() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private void addAlertRow(@NonNull LinearLayout group, @Nullable String text, @NonNull AlertTone tone) {
        if (text == null || text.trim().isEmpty()) return;
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.TOP);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dp(8);
        row.setLayoutParams(params);
        row.setPadding(0, 0, 0, 0);
        setRoundedBackground(row, alertBackgroundColor(tone), R.color.md_theme_outlineVariant, 16);

        View accent = new View(requireContext());
        GradientDrawable accentBg = new GradientDrawable();
        accentBg.setColor(ContextCompat.getColor(requireContext(), alertAccentColor(tone)));
        accentBg.setCornerRadii(new float[] { dp(16), dp(16), 0, 0, 0, 0, dp(16), dp(16) });
        accent.setBackground(accentBg);
        row.addView(accent, new LinearLayout.LayoutParams(dp(5), ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout content = new LinearLayout(requireContext());
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(12), dp(11), dp(12), dp(12));

        LinearLayout heading = new LinearLayout(requireContext());
        heading.setOrientation(LinearLayout.HORIZONTAL);
        heading.setGravity(android.view.Gravity.CENTER_VERTICAL);
        TextView icon = new TextView(requireContext());
        icon.setText(alertIcon(tone));
        icon.setGravity(android.view.Gravity.CENTER);
        icon.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        icon.setTextSize(13);
        icon.setTextColor(ContextCompat.getColor(requireContext(), alertTextColor(tone)));
        GradientDrawable iconBg = new GradientDrawable();
        iconBg.setShape(GradientDrawable.OVAL);
        iconBg.setColor(ContextCompat.getColor(requireContext(), statusBackgroundColor(tone)));
        icon.setBackground(iconBg);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(26), dp(26));
        iconParams.setMarginEnd(dp(10));
        heading.addView(icon, iconParams);

        TextView label = new TextView(requireContext());
        label.setText(alertToneLabel(tone));
        label.setTextColor(ContextCompat.getColor(requireContext(), alertTextColor(tone)));
        label.setTextSize(12);
        label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        heading.addView(label, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        content.addView(heading, matchWrapParams());

        TextView view = new TextView(requireContext());
        view.setText(text.trim());
        view.setSingleLine(false);
        view.setMaxLines(Integer.MAX_VALUE);
        view.setTextSize(14);
        view.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
        view.setTextColor(ContextCompat.getColor(requireContext(), alertTextColor(tone)));
        view.setPadding(0, dp(7), 0, 0);
        content.addView(view, matchWrapParams());
        row.addView(content, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        group.addView(row);
    }

    private String alertIcon(@NonNull AlertTone tone) {
        switch (tone) {
            case CRITICAL:
                return "!";
            case WARNING:
                return "!";
            case POSITIVE:
                return "✓";
            default:
                return "i";
        }
    }

    private String alertToneLabel(@NonNull AlertTone tone) {
        switch (tone) {
            case CRITICAL:
                return getString(R.string.alert_tone_critical);
            case WARNING:
                return getString(R.string.alert_tone_warning);
            case POSITIVE:
                return getString(R.string.alert_tone_positive);
            default:
                return getString(R.string.alert_tone_info);
        }
    }

    private void applyStatusStyle(@NonNull TextView view, @NonNull AlertTone tone) {
        view.setTextColor(ContextCompat.getColor(requireContext(), alertTextColor(tone)));
        view.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), alertBackgroundColor(tone))));
    }

    private AlertTone statusTone(@Nullable String value, boolean preliminary) {
        String clean = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (clean.contains("riesgo") || clean.contains("crit") || clean.contains("exced") || clean.contains("negativo")) {
            return AlertTone.CRITICAL;
        }
        if (clean.contains("estable") || clean.contains("bueno") || clean.contains("posit")
                || clean.contains("control") || clean.contains("saludable")) {
            return AlertTone.POSITIVE;
        }
        if (preliminary || clean.contains("preliminar") || clean.contains("atento") || clean.contains("ajust") || clean.contains("prudente")) {
            return AlertTone.WARNING;
        }
        return AlertTone.POSITIVE;
    }

    private AlertTone alertTone(@Nullable String text) {
        String clean = text == null ? "" : text.toLowerCase(Locale.ROOT);
        if (clean.contains("negativo") || clean.contains("superaron") || clean.contains("superado")
                || clean.contains("sin saldo suficiente") || clean.contains("vencido") || clean.contains("saldo bajo")) {
            return AlertTone.CRITICAL;
        }
        if (clean.contains("cerca") || clean.contains("inusualmente") || clean.contains("subieron")
                || clean.contains("ritmo de gasto") || clean.contains("preliminar") || clean.contains("revisa")) {
            return AlertTone.WARNING;
        }
        return AlertTone.INFO;
    }

    private int alertBackground(@NonNull AlertTone tone) {
        switch (tone) {
            case CRITICAL:
                return R.drawable.bg_alert_critical;
            case WARNING:
                return R.drawable.bg_alert_warning;
            case POSITIVE:
                return R.drawable.bg_alert_positive;
            default:
                return R.drawable.bg_alert_info;
        }
    }

    private int statusBackground(@NonNull AlertTone tone) {
        switch (tone) {
            case CRITICAL:
                return R.drawable.bg_alert_critical;
            case WARNING:
                return R.drawable.bg_alert_warning;
            case POSITIVE:
                return R.drawable.bg_alert_positive;
            default:
                return R.drawable.bg_alert_info;
        }
    }

    private int alertBackgroundColor(@NonNull AlertTone tone) {
        switch (tone) {
            case CRITICAL:
                return R.color.risk_high_bg;
            case WARNING:
                return R.color.risk_medium_bg;
            default:
            case INFO:
                return R.color.md_theme_secondaryContainer;
            case POSITIVE:
                return R.color.risk_low_bg;
        }
    }

    private int alertTextColor(@NonNull AlertTone tone) {
        switch (tone) {
            case CRITICAL:
                return R.color.risk_high_text;
            case WARNING:
                return R.color.risk_medium_text;
            case POSITIVE:
                return R.color.risk_low_text;
            default:
                return R.color.md_theme_onSecondaryContainer;
        }
    }

    private int alertAccentColor(@NonNull AlertTone tone) {
        switch (tone) {
            case CRITICAL:
                return R.color.risk_high_text;
            case WARNING:
                return R.color.risk_medium_text;
            case POSITIVE:
                return R.color.risk_low_text;
            default:
                return R.color.md_theme_primary;
        }
    }

    private int statusBackgroundColor(@NonNull AlertTone tone) {
        switch (tone) {
            case CRITICAL:
                return R.color.risk_high_bg;
            case WARNING:
                return R.color.risk_medium_bg;
            case POSITIVE:
                return R.color.risk_low_bg;
            default:
                return R.color.md_theme_secondaryContainer;
        }
    }

    private void setRoundedBackground(@NonNull View view, int fillColorRes, int strokeColorRes, int radiusDp) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(ContextCompat.getColor(requireContext(), fillColorRes));
        bg.setCornerRadius(dp(radiusDp));
        bg.setStroke(dp(1), ContextCompat.getColor(requireContext(), strokeColorRes));
        view.setBackground(bg);
    }

    private String shortText(@Nullable String value, int maxLength) {
        String clean = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        if (clean.length() <= maxLength) return clean;
        int cut = clean.lastIndexOf(' ', maxLength);
        if (cut < maxLength / 2) cut = maxLength;
        return clean.substring(0, cut).trim() + "...";
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
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

    private enum AlertTone {
        POSITIVE,
        INFO,
        WARNING,
        CRITICAL
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

    private void applyPieChartTheme() {
        int background = ContextCompat.getColor(requireContext(), R.color.md_theme_background);
        int text = ContextCompat.getColor(requireContext(), R.color.md_theme_onSurfaceVariant);
        chartCategorias.setBackgroundColor(Color.TRANSPARENT);
        chartCategorias.setHoleColor(background);
        chartCategorias.setTransparentCircleColor(background);
        chartCategorias.setTransparentCircleAlpha(0);
        chartCategorias.setEntryLabelColor(text);
        chartCategorias.setNoDataTextColor(text);
        Legend legend = chartCategorias.getLegend();
        legend.setTextColor(text);
        legend.setTextSize(12f);
        legend.setFormSize(10f);
        legend.setXEntrySpace(10f);
        legend.setYEntrySpace(6f);
    }

    private void applyTrendChartTheme() {
        int text = ContextCompat.getColor(requireContext(), R.color.md_theme_onSurfaceVariant);
        int outline = ContextCompat.getColor(requireContext(), R.color.md_theme_outlineVariant);
        chartTrend.setBackgroundColor(Color.TRANSPARENT);
        chartTrend.setDrawGridBackground(false);
        chartTrend.setNoDataTextColor(text);
        chartTrend.setBorderColor(outline);
        Legend legend = chartTrend.getLegend();
        legend.setTextColor(text);
        legend.setTextSize(12f);
        legend.setFormSize(10f);
        legend.setXEntrySpace(10f);
    }

    private LineDataSet lineSet(ArrayList<Entry> entries, String label, int colorRes) {
        int color = ContextCompat.getColor(requireContext(), colorRes);
        LineDataSet set = new LineDataSet(entries, label);
        set.setColor(color);
        set.setCircleColor(color);
        set.setCircleHoleColor(ContextCompat.getColor(requireContext(), R.color.md_theme_surface));
        set.setValueTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurface));
        set.setHighLightColor(color);
        set.setLineWidth(2.8f);
        set.setCircleRadius(3.8f);
        set.setCircleHoleRadius(1.6f);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setCubicIntensity(0.18f);
        return set;
    }

    private ArrayList<Integer> chartColors() {
        ArrayList<Integer> colors = new ArrayList<>();
        colors.add(ContextCompat.getColor(requireContext(), R.color.chart_pie_1));
        colors.add(ContextCompat.getColor(requireContext(), R.color.chart_pie_2));
        colors.add(ContextCompat.getColor(requireContext(), R.color.chart_pie_3));
        colors.add(ContextCompat.getColor(requireContext(), R.color.chart_pie_4));
        colors.add(ContextCompat.getColor(requireContext(), R.color.chart_pie_5));
        colors.add(ContextCompat.getColor(requireContext(), R.color.chart_pie_6));
        colors.add(ContextCompat.getColor(requireContext(), R.color.chart_pie_7));
        colors.add(ContextCompat.getColor(requireContext(), R.color.chart_pie_8));
        return colors;
    }

    private void renderGoalSummary(@NonNull HomeSummary summary) {
        SavingsGoal goal = nearestGoal(summary.getMetas());
        if (goal == null) {
            tvGoalTitle.setText(R.string.home_empty_goals);
            tvGoalProgress.setText(R.string.home_goal_progress_empty);
            tvGoalMessage.setText(R.string.home_goal_create_hint);
            return;
        }
        double progress = goal.getMontoObjetivo() <= 0.0
                ? goal.getProgreso()
                : (goal.getMontoActual() * 100.0 / goal.getMontoObjetivo());
        progress = Math.max(0.0, Math.min(100.0, progress));
        double missing = Math.max(0.0, goal.getMontoObjetivo() - goal.getMontoActual());
        tvGoalTitle.setText(nonEmpty(goal.getTitulo(), getString(R.string.home_goal_without_title)));
        tvGoalProgress.setText(getString(R.string.home_goal_progress_value, Math.round(progress)));
        tvGoalMessage.setText(getString(
                R.string.home_goal_missing_value,
                Format.money(missing, goal.getMoneda())
        ));
    }

    @Nullable
    private SavingsGoal nearestGoal(@NonNull List<SavingsGoal> goals) {
        ArrayList<SavingsGoal> active = new ArrayList<>();
        for (SavingsGoal goal : goals) {
            if (goal == null || goal.getMontoObjetivo() <= 0.0) continue;
            if (goal.getMontoActual() >= goal.getMontoObjetivo()) continue;
            active.add(goal);
        }
        active.sort(Comparator.comparing(
                SavingsGoal::getFechaObjetivo,
                Comparator.nullsLast(Date::compareTo)
        ));
        return active.isEmpty() ? null : active.get(0);
    }

    private void renderReminderSummary(@NonNull HomeSummary summary) {
        PaymentReminder reminder = nextReminder(summary.getRecordatorios());
        if (reminder == null) {
            tvReminderTitle.setText(R.string.home_empty_reminders);
            tvReminderDate.setText(R.string.home_reminder_create_hint);
            tvReminderStatus.setText(R.string.home_reminder_status_empty);
            applyStatusStyle(tvReminderStatus, AlertTone.INFO);
            return;
        }
        tvReminderTitle.setText(nonEmpty(reminder.getTitulo(), getString(R.string.home_reminder_without_title)));
        String dateText = reminder.getFechaVencimiento() == null
                ? getString(R.string.home_reminder_no_date)
                : Format.date(reminder.getFechaVencimiento());
        String time = reminder.getHoraRecordatorio();
        if (time != null && !time.trim().isEmpty()) dateText = dateText + " - " + time.trim();
        tvReminderDate.setText(getString(
                R.string.home_reminder_due_value,
                dateText,
                Format.money(reminder.getMonto(), reminder.getMoneda())
        ));
        int days = reminder.getDiasRestantes();
        String status;
        AlertTone tone;
        if (days < 0) {
            status = getString(R.string.home_reminder_overdue);
            tone = AlertTone.CRITICAL;
        } else if (days == 0) {
            status = getString(R.string.home_reminder_today);
            tone = AlertTone.WARNING;
        } else {
            status = getString(R.string.home_reminder_days_left, days);
            tone = AlertTone.INFO;
        }
        tvReminderStatus.setText(status);
        applyStatusStyle(tvReminderStatus, tone);
    }

    @Nullable
    private PaymentReminder nextReminder(@NonNull List<PaymentReminder> reminders) {
        ArrayList<PaymentReminder> pending = new ArrayList<>();
        for (PaymentReminder reminder : reminders) {
            if (reminder == null || reminder.isPagado()) continue;
            pending.add(reminder);
        }
        pending.sort(Comparator.comparing(
                PaymentReminder::getFechaVencimiento,
                Comparator.nullsLast(Date::compareTo)
        ));
        return pending.isEmpty() ? null : pending.get(0);
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
        moduleOrder.clear();
        Map<String, ModuleDef> defs = moduleDefsById();
        for (ModuleDef def : defaultModules()) {
            moduleVisibility.put(def.id, true);
        }
        LinkedHashSet<String> ordered = new LinkedHashSet<>();
        for (DashboardModulePref pref : summary.getDashboardPreferencias()) {
            if (pref == null || pref.getId() == null) continue;
            if (!defs.containsKey(pref.getId())) continue;
            moduleVisibility.put(pref.getId(), pref.isVisible());
            ordered.add(pref.getId());
        }
        for (ModuleDef def : defaultModules()) {
            ordered.add(def.id);
        }
        moduleOrder.addAll(ordered);
        applyDashboardOrder();
    }

    private void applyModuleVisibility(boolean hasData) {
        boolean summaryVisible = isModuleVisible(MODULE_SUMMARY);
        setVisible(moduleSummary, summaryVisible);
        setVisible(tvBudgetDetail, summaryVisible);
        setVisible(moduleCategory, hasData && isModuleVisible(MODULE_CATEGORY));
        setVisible(moduleTrend, hasData && isModuleVisible(MODULE_TREND));
        setVisible(moduleAlerts, hasData && isModuleVisible(MODULE_ALERTS));
        setVisible(moduleInsights, hasData && isModuleVisible(MODULE_INSIGHTS));
        setVisible(moduleGoals, isModuleVisible(MODULE_GOALS));
        setVisible(moduleReminders, isModuleVisible(MODULE_REMINDERS));
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
        MaterialButton btnCancel = content.findViewById(R.id.btnDashboardCancel);
        MaterialButton btnSave = content.findViewById(R.id.btnDashboardSave);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setNestedScrollingEnabled(true);

        DashboardModuleAdapter adapter = new DashboardModuleAdapter();
        adapter.setItems(buildModuleItems());
        rv.setAdapter(adapter);
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                adapter.moveItem(viewHolder.getAdapterPosition(), target.getAdapterPosition());
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            }
        }).attachToRecyclerView(rv);

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(content)
                .create();
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSave.setOnClickListener(v -> {
            saveModulePreferences(adapter.getItems());
            dialog.dismiss();
        });
        dialog.setOnShowListener(d -> {
            int maxHeight = Math.min(dp(420), (int) (getResources().getDisplayMetrics().heightPixels * 0.56f));
            int desiredHeight = Math.min(maxHeight, dp((adapter.getItemCount() * 58) + 8));
            ViewGroup.LayoutParams params = rv.getLayoutParams();
            params.height = desiredHeight;
            rv.setLayoutParams(params);
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialog.getWindow().setDimAmount(0.46f);
            }
        });
        dialog.show();
    }

    private List<DashboardModuleAdapter.ModuleItem> buildModuleItems() {
        List<DashboardModuleAdapter.ModuleItem> items = new ArrayList<>();
        Map<String, ModuleDef> defs = moduleDefsById();
        List<String> order = moduleOrder.isEmpty() ? defaultModuleIds() : moduleOrder;
        LinkedHashSet<String> safeOrder = new LinkedHashSet<>(order);
        safeOrder.addAll(defaultModuleIds());
        for (String id : safeOrder) {
            ModuleDef def = defs.get(id);
            if (def == null) continue;
            items.add(new DashboardModuleAdapter.ModuleItem(def.id, getString(def.titleRes), isModuleVisible(def.id)));
        }
        return items;
    }

    private void saveModulePreferences(List<DashboardModuleAdapter.ModuleItem> items) {
        try {
            JSONArray modules = new JSONArray();
            moduleOrder.clear();
            for (DashboardModuleAdapter.ModuleItem item : items) {
                JSONObject obj = new JSONObject();
                obj.put("id", item.getId());
                obj.put("visible", item.isVisible());
                modules.put(obj);
                moduleVisibility.put(item.getId(), item.isVisible());
                moduleOrder.add(item.getId());
            }
            JSONObject body = new JSONObject();
            body.put("modules", modules);
            SettingsService.saveDashboard(requireContext(), body, new SettingsService.SaveCb() {
                @Override
                public void onSuccess() {
                    if (!isAdded()) return;
                    applyDashboardOrder();
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
        modules.add(new ModuleDef(MODULE_INSIGHTS, R.string.home_financial_intelligence_title));
        modules.add(new ModuleDef(MODULE_CATEGORY, R.string.dashboard_module_chart_budget));
        modules.add(new ModuleDef(MODULE_TREND, R.string.dashboard_module_chart_trend));
        modules.add(new ModuleDef(MODULE_ALERTS, R.string.dashboard_module_alerts));
        modules.add(new ModuleDef(MODULE_GOALS, R.string.dashboard_module_goals));
        modules.add(new ModuleDef(MODULE_REMINDERS, R.string.dashboard_module_reminders));
        modules.add(new ModuleDef(MODULE_QUICK, R.string.dashboard_module_quick));
        return modules;
    }

    private Map<String, ModuleDef> moduleDefsById() {
        Map<String, ModuleDef> out = new HashMap<>();
        for (ModuleDef def : defaultModules()) {
            out.put(def.id, def);
        }
        return out;
    }

    private List<String> defaultModuleIds() {
        List<String> ids = new ArrayList<>();
        for (ModuleDef def : defaultModules()) {
            ids.add(def.id);
        }
        return ids;
    }

    private void applyDashboardOrder() {
        if (dashboardContainer == null || moduleSummary == null) return;
        Map<String, View> views = new HashMap<>();
        views.put(MODULE_SUMMARY, moduleSummary);
        views.put(MODULE_CATEGORY, moduleCategory);
        views.put(MODULE_TREND, moduleTrend);
        views.put(MODULE_ALERTS, moduleAlerts);
        views.put(MODULE_INSIGHTS, moduleInsights);
        views.put(MODULE_GOALS, moduleGoals);
        views.put(MODULE_REMINDERS, moduleReminders);
        views.put(MODULE_QUICK, moduleQuick);

        List<String> order = moduleOrder.isEmpty() ? defaultModuleIds() : moduleOrder;
        LinkedHashSet<String> safeOrder = new LinkedHashSet<>(order);
        safeOrder.addAll(defaultModuleIds());
        for (View view : views.values()) {
            if (view != null && view.getParent() == dashboardContainer) {
                dashboardContainer.removeView(view);
            }
        }
        for (String id : safeOrder) {
            View view = views.get(id);
            if (view != null) dashboardContainer.addView(view);
        }
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
