package com.example.finanzas.ui;

import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.finanzas.R;
import com.example.finanzas.data.api.SettingsService;
import com.example.finanzas.data.model.AnalysisCategoryPoint;
import com.example.finanzas.data.model.AnalysisChartState;
import com.example.finanzas.data.model.AnalysisMonthlyPoint;
import com.example.finanzas.data.model.FinancialAssistantGeneratedBy;
import com.example.finanzas.data.model.FinancialAssistantResult;
import com.example.finanzas.data.model.FinancialAssistantRiskLevel;
import com.example.finanzas.data.model.FinancialAssistantUiState;
import com.example.finanzas.data.model.HomeSummary;
import com.example.finanzas.ui.view.SpendlyDecorBackgroundDrawable;
import com.example.finanzas.ui.viewmodel.HomeViewModel;
import com.example.finanzas.util.Format;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.patrykandpatrick.vico.views.cartesian.CartesianChartView;

import java.util.Calendar;
import java.util.List;
import java.util.Objects;

public class AnalysisFragment extends Fragment {
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipe;
    private TextView tvPeriod;
    private TextView tvIncomeExpenseEmpty;
    private TextView tvCategoryBarsEmpty;
    private TextView tvExpenseTrendEmpty;
    private TextView tvBalanceEmpty;
    private TextView tvSavingsEmpty;
    private TextView tvIncomeExpenseSummary;
    private TextView tvCategoryBarsSummary;
    private TextView tvExpenseTrendSummary;
    private TextView tvBalanceSummary;
    private TextView tvSavingsSummary;
    private TextView tvIncomeExpenseBalance;
    private TextView tvExpenseTrendSingle;
    private TextView tvBalanceCurrent;
    private TextView tvBalanceDelta;
    private TextView tvSavingsPercent;
    private TextView tvSavingsHero;
    private TextView tvSavingsDetail;
    private TextView tvSavingsMessage;
    private TextView tvAssistantRisk;
    private TextView tvAssistantSource;
    private TextView tvAssistantSummary;
    private TextView tvAssistantSaving;
    private TextView tvAssistantConcern;
    private TextView tvAssistantAction;
    private TextView tvAssistantAlert;
    private MaterialButton btnAssistantRefresh;
    private CartesianChartView chartIncomeExpense;
    private CartesianChartView chartExpenseTrend;
    private CartesianChartView chartBalance;
    private View cardExpenseTrendSingle;
    private View chartSavings;
    private CircularProgressIndicator progressSavings;
    private LinearLayout listCategoryBars;
    private LinearLayout listIncomeExpenseAmounts;
    private LinearLayout listExpenseTrendMonths;
    private LinearLayout listBalanceMonths;
    private HomeViewModel viewModel;
    private String currencyCode = "PEN";
    private HomeSummary lastSummary;
    private String lastSummaryRenderKey;
    private AnalysisChartState lastRenderedChartState;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_analysis, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.setBackground(new SpendlyDecorBackgroundDrawable(requireContext()));
        swipe = view.findViewById(R.id.swipeAnalysis);
        tvPeriod = view.findViewById(R.id.tvAnalysisPeriod);
        tvPeriod.setText("");
        tvIncomeExpenseEmpty = view.findViewById(R.id.tvAnalysisIncomeExpenseEmpty);
        tvCategoryBarsEmpty = view.findViewById(R.id.tvAnalysisCategoryBarsEmpty);
        tvExpenseTrendEmpty = view.findViewById(R.id.tvAnalysisExpenseTrendEmpty);
        tvBalanceEmpty = view.findViewById(R.id.tvAnalysisBalanceEmpty);
        tvSavingsEmpty = view.findViewById(R.id.tvAnalysisSavingsEmpty);
        tvIncomeExpenseSummary = view.findViewById(R.id.tvAnalysisIncomeExpenseSummary);
        tvCategoryBarsSummary = view.findViewById(R.id.tvAnalysisCategoryBarsSummary);
        tvExpenseTrendSummary = view.findViewById(R.id.tvAnalysisExpenseTrendSummary);
        tvBalanceSummary = view.findViewById(R.id.tvAnalysisBalanceSummary);
        tvSavingsSummary = view.findViewById(R.id.tvAnalysisSavingsSummary);
        tvIncomeExpenseBalance = view.findViewById(R.id.tvAnalysisIncomeExpenseBalance);
        tvExpenseTrendSingle = view.findViewById(R.id.tvAnalysisExpenseTrendSingle);
        tvBalanceCurrent = view.findViewById(R.id.tvAnalysisBalanceCurrent);
        tvBalanceDelta = view.findViewById(R.id.tvAnalysisBalanceDelta);
        tvSavingsPercent = view.findViewById(R.id.tvAnalysisSavingsPercent);
        tvSavingsHero = view.findViewById(R.id.tvAnalysisSavingsHero);
        tvSavingsDetail = view.findViewById(R.id.tvAnalysisSavingsDetail);
        tvSavingsMessage = view.findViewById(R.id.tvAnalysisSavingsMessage);
        tvAssistantRisk = view.findViewById(R.id.tvAnalysisAssistantRisk);
        tvAssistantSource = view.findViewById(R.id.tvAnalysisAssistantSource);
        tvAssistantSummary = view.findViewById(R.id.tvAnalysisAssistantSummary);
        tvAssistantSaving = view.findViewById(R.id.tvAnalysisAssistantSaving);
        tvAssistantConcern = view.findViewById(R.id.tvAnalysisAssistantConcern);
        tvAssistantAction = view.findViewById(R.id.tvAnalysisAssistantAction);
        tvAssistantAlert = view.findViewById(R.id.tvAnalysisAssistantAlert);
        btnAssistantRefresh = view.findViewById(R.id.btnAnalysisAssistantRefresh);
        styleDetailButton(btnAssistantRefresh);
        btnAssistantRefresh.setIconResource(R.drawable.ic_refresh_24);
        chartIncomeExpense = view.findViewById(R.id.chartAnalysisIncomeExpense);
        chartExpenseTrend = view.findViewById(R.id.chartAnalysisExpenseTrend);
        chartBalance = view.findViewById(R.id.chartAnalysisBalance);
        chartSavings = view.findViewById(R.id.chartAnalysisSavings);
        cardExpenseTrendSingle = view.findViewById(R.id.cardAnalysisExpenseTrendSingle);
        progressSavings = view.findViewById(R.id.progressAnalysisSavings);
        listCategoryBars = view.findViewById(R.id.listAnalysisCategoryBars);
        listIncomeExpenseAmounts = view.findViewById(R.id.listAnalysisIncomeExpenseAmounts);
        listExpenseTrendMonths = view.findViewById(R.id.listAnalysisExpenseTrendMonths);
        listBalanceMonths = view.findViewById(R.id.listAnalysisBalanceMonths);
        viewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);
        resetRenderCache();
        chartIncomeExpense.setModelProducer(viewModel.getIncomeExpenseChartProducer());
        chartExpenseTrend.setModelProducer(viewModel.getExpenseTrendChartProducer());
        chartBalance.setModelProducer(viewModel.getBalanceChartProducer());
        currencyCode = SettingsService.getCurrencyCode(requireContext());

        swipe.setOnRefreshListener(() -> load(true));
        chartExpenseTrend.setOnClickListener(v -> showLatestExpensePoint());
        btnAssistantRefresh.setOnClickListener(v -> viewModel.refreshFinancialAssistant(true));
        viewModel.getLoading().observe(getViewLifecycleOwner(), loading ->
                swipe.setRefreshing(Boolean.TRUE.equals(loading)));
        viewModel.getCurrencyCode().observe(getViewLifecycleOwner(), code -> {
            String nextCode = code == null ? "" : code.trim();
            if (nextCode.isEmpty() || nextCode.equals(currencyCode)) return;
            currencyCode = nextCode;
            HomeSummary summary = viewModel.getSummary().getValue();
            if (summary != null && isAdded()) render(summary);
        });
        viewModel.getSummary().observe(getViewLifecycleOwner(), summary -> {
            if (summary != null && isAdded()) render(summary);
        });
        viewModel.getAnalysisCharts().observe(getViewLifecycleOwner(), state -> {
            if (state != null && isAdded()) renderAnalysisCharts(state);
        });
        viewModel.getFinancialAssistant().observe(getViewLifecycleOwner(), state -> {
            if (state != null && isAdded()) renderFinancialAssistant(state);
        });
        load(false);
    }

    @Override
    public void onDestroyView() {
        resetRenderCache();
        super.onDestroyView();
    }

    private void resetRenderCache() {
        lastSummaryRenderKey = null;
        lastRenderedChartState = null;
    }

    private void styleDetailButton(@NonNull MaterialButton button) {
        int background = ContextCompat.getColor(requireContext(), R.color.planning_dialog_button);
        button.setText("");
        button.setIconResource(R.drawable.ic_eye);
        button.setIconTint(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), android.R.color.white)));
        button.setBackgroundTintList(ColorStateList.valueOf(background));
        button.setRippleColor(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.planning_dialog_button_pressed)));
        button.setStrokeWidth(0);
        button.setCornerRadius(dp(16));
        button.setIconPadding(0);
        button.setMinWidth(dp(48));
        button.setMinHeight(dp(48));
        button.setInsetTop(0);
        button.setInsetBottom(0);
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
        String renderKey = analysisSummaryRenderKey(summary);
        if (Objects.equals(renderKey, lastSummaryRenderKey)) return;
        lastSummaryRenderKey = renderKey;
        lastSummary = summary;
        tvPeriod.setText(Format.monthYear(summary.getAnio(), summary.getMes()));
    }

    private void renderFinancialAssistant(@NonNull FinancialAssistantUiState state) {
        if (state instanceof FinancialAssistantUiState.Loading) {
            tvAssistantRisk.setText(R.string.financial_assistant_loading_chip);
            styleRiskChip(FinancialAssistantRiskLevel.MEDIUM);
            tvAssistantSource.setVisibility(View.GONE);
            tvAssistantSummary.setText(R.string.financial_assistant_loading);
            tvAssistantSaving.setText("...");
            tvAssistantConcern.setText(R.string.financial_assistant_pending);
            tvAssistantAction.setText(R.string.financial_assistant_wait_action);
            tvAssistantAlert.setVisibility(View.GONE);
            btnAssistantRefresh.setEnabled(false);
            return;
        }
        if (state instanceof FinancialAssistantUiState.Ready) {
            FinancialAssistantResult result = ((FinancialAssistantUiState.Ready) state).getResult();
            btnAssistantRefresh.setEnabled(true);
            tvAssistantRisk.setText(result.getRiskLabel());
            styleRiskChip(result.getRiskLevel());
            tvAssistantSource.setVisibility(result.getGeneratedBy() == FinancialAssistantGeneratedBy.local_fallback ? View.VISIBLE : View.GONE);
            tvAssistantSource.setText(R.string.financial_assistant_local_source);
            tvAssistantSummary.setText(result.getSummary());
            tvAssistantSaving.setText(result.getSuggestedSavingAmount() > 0.0
                    ? Format.money(result.getSuggestedSavingAmount(), currencyCode)
                    : getString(R.string.financial_assistant_no_saving));
            tvAssistantConcern.setText(result.getMainConcern());
            tvAssistantAction.setText(result.getRecommendedAction());
            List<String> alerts = result.getAlerts();
            tvAssistantAlert.setVisibility(alerts.isEmpty() ? View.GONE : View.VISIBLE);
            if (!alerts.isEmpty()) {
                tvAssistantAlert.setText(alerts.get(0));
            }
            return;
        }
        btnAssistantRefresh.setEnabled(true);
        tvAssistantRisk.setText(R.string.financial_assistant_pending);
        styleRiskChip(FinancialAssistantRiskLevel.LOW);
        tvAssistantSource.setVisibility(View.GONE);
        tvAssistantSummary.setText(R.string.financial_assistant_empty);
        tvAssistantSaving.setText("...");
        tvAssistantConcern.setText(R.string.financial_assistant_pending);
        tvAssistantAction.setText(R.string.financial_assistant_wait_action);
        tvAssistantAlert.setVisibility(View.GONE);
    }

    private void styleRiskChip(@NonNull FinancialAssistantRiskLevel riskLevel) {
        int bg;
        int text;
        if (riskLevel == FinancialAssistantRiskLevel.HIGH) {
            bg = R.color.risk_high_bg;
            text = R.color.risk_high_text;
        } else if (riskLevel == FinancialAssistantRiskLevel.MEDIUM) {
            bg = R.color.risk_medium_bg;
            text = R.color.risk_medium_text;
        } else {
            bg = R.color.risk_low_bg;
            text = R.color.risk_low_text;
        }
        GradientDrawable chip = new GradientDrawable();
        chip.setColor(ContextCompat.getColor(requireContext(), bg));
        chip.setStroke(dp(1), ContextCompat.getColor(requireContext(), text));
        chip.setCornerRadius(dp(12));
        tvAssistantRisk.setBackground(chip);
        tvAssistantRisk.setTextColor(ContextCompat.getColor(requireContext(), text));
    }

    private void renderAnalysisCharts(@NonNull AnalysisChartState state) {
        if (state.equals(lastRenderedChartState)) return;
        lastRenderedChartState = state;
        currencyCode = nonEmpty(state.getCurrencyCode(), currencyCode);
        setChartState(chartIncomeExpense, tvIncomeExpenseEmpty, hasLatestIncomeExpense(state.getMonths()));
        setExpenseTrendState(state.getMonths());
        setChartState(chartBalance, tvBalanceEmpty, state.hasBalance());
        tvBalanceCurrent.setVisibility(state.hasBalance() ? View.VISIBLE : View.GONE);
        tvBalanceDelta.setVisibility(state.hasBalance() ? View.VISIBLE : View.GONE);
        setSavingsState(state.getMonths());
        renderChartSummaries(state.getMonths());
        renderCategoryBars(state.getCategories());
        renderIncomeExpenseAmounts(state.getMonths());
        renderMonthLabels(listExpenseTrendMonths, state.getMonths(), countExpenseDataMonths(state.getMonths()) >= 2);
        renderMonthLabels(listBalanceMonths, state.getMonths(), state.hasBalance());
    }

    private String analysisSummaryRenderKey(@NonNull HomeSummary summary) {
        return currencyCode
                + "|" + summary.getAnio()
                + "|" + summary.getMes()
                + "|" + summary.getScoreFinanciero()
                + "|" + summary.getScoreEstado()
                + "|" + summary.getInsightPrincipal()
                + "|" + summary.getScoreExplicacion()
                + "|" + summary.getEstadoAhorro()
                + "|" + Math.round(summary.getAhorroSugerido() * 100.0)
                + "|" + summary.getRecomendacionAhorroMeta();
    }

    private void renderChartSummaries(@NonNull List<AnalysisMonthlyPoint> months) {
        if (months.isEmpty()) {
            tvIncomeExpenseSummary.setText(getString(
                    R.string.analysis_income_expense_subtitle,
                    Format.money(0.0, currencyCode),
                    Format.money(0.0, currencyCode)
            ));
            tvExpenseTrendSummary.setText(R.string.analysis_expense_trend_subtitle);
            tvBalanceSummary.setText(R.string.analysis_balance_subtitle);
            tvSavingsSummary.setText(getString(R.string.analysis_savings_subtitle, Format.money(0.0, currencyCode)));
            tvSavingsSummary.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurfaceVariant));
            return;
        }
        AnalysisMonthlyPoint latest = months.get(months.size() - 1);
        tvIncomeExpenseSummary.setText(getString(
                R.string.analysis_income_expense_subtitle,
                Format.money(latest.getIngresos(), currencyCode),
                Format.money(latest.getGastos(), currencyCode)
        ));
        tvIncomeExpenseBalance.setText(incomeExpenseBalanceText(latest));
        tvIncomeExpenseBalance.setTextColor(ContextCompat.getColor(
                requireContext(),
                latest.getAhorro() < 0 ? R.color.expense : R.color.md_theme_onSurface
        ));
        tvExpenseTrendSummary.setText(expenseTrendSummary(months));
        tvBalanceSummary.setText(R.string.analysis_balance_subtitle);
        tvBalanceCurrent.setText("Saldo actual: " + Format.money(latest.getBalance(), currencyCode));
        double previousBalance = months.size() >= 2 ? months.get(months.size() - 2).getBalance() : 0.0;
        double balanceDelta = latest.getBalance() - previousBalance;
        tvBalanceDelta.setText(balanceDeltaText(balanceDelta));
        tvBalanceDelta.setTextColor(ContextCompat.getColor(
                requireContext(),
                balanceDelta < -0.005 ? R.color.expense : R.color.income
        ));
        tvSavingsSummary.setText(getString(R.string.analysis_savings_subtitle, Format.money(latest.getAhorro(), currencyCode)));
        tvSavingsSummary.setTextColor(ContextCompat.getColor(
                requireContext(),
                latest.getAhorro() < 0 ? R.color.expense : R.color.md_theme_onSurfaceVariant
        ));
        renderSavingsSummary(latest);
    }

    private String expenseTrendSummary(@NonNull List<AnalysisMonthlyPoint> months) {
        AnalysisMonthlyPoint latest = latestExpensePoint(months);
        AnalysisMonthlyPoint previous = previousExpensePoint(months);
        if (latest == null) return getString(R.string.analysis_expense_trend_subtitle);
        if (previous == null) return "Agrega más transacciones para ver tu tendencia";
        double delta = latest.getGastos() - previous.getGastos();
        if (Math.abs(delta) < 0.005) {
            return "Tus gastos se mantuvieron estables";
        }
        String direction = delta > 0 ? "subieron" : "bajaron";
        return "Tus gastos " + direction + " " + Format.money(Math.abs(delta), currencyCode)
                + " respecto al periodo anterior";
    }

    private void setChartState(@NonNull CartesianChartView chart, @NonNull TextView empty, boolean hasData) {
        chart.setVisibility(hasData ? View.VISIBLE : View.GONE);
        empty.setVisibility(hasData ? View.GONE : View.VISIBLE);
    }

    private void setExpenseTrendState(@NonNull List<AnalysisMonthlyPoint> months) {
        int dataMonths = countExpenseDataMonths(months);
        boolean hasLine = dataMonths >= 2;
        chartExpenseTrend.setVisibility(hasLine ? View.VISIBLE : View.GONE);
        tvExpenseTrendEmpty.setVisibility(dataMonths == 0 ? View.VISIBLE : View.GONE);
        cardExpenseTrendSingle.setVisibility(dataMonths == 1 ? View.VISIBLE : View.GONE);
        if (dataMonths == 1) {
            AnalysisMonthlyPoint point = latestExpensePoint(months);
            if (point != null) {
                tvExpenseTrendSingle.setText(
                        "Gasto actual: " + Format.money(point.getGastos(), currencyCode)
                                + "\nAgrega más transacciones para ver tu tendencia"
                );
            }
        }
    }

    private void setSavingsState(@NonNull List<AnalysisMonthlyPoint> months) {
        AnalysisMonthlyPoint latest = latestPoint(months);
        boolean hasData = latest != null && (latest.getIngresos() > 0.0 || latest.getGastos() > 0.0);
        chartSavings.setVisibility(hasData ? View.VISIBLE : View.GONE);
        tvSavingsDetail.setVisibility(hasData ? View.VISIBLE : View.GONE);
        tvSavingsMessage.setVisibility(hasData ? View.VISIBLE : View.GONE);
        tvSavingsEmpty.setVisibility(hasData ? View.GONE : View.VISIBLE);
    }

    private boolean hasLatestIncomeExpense(@NonNull List<AnalysisMonthlyPoint> months) {
        AnalysisMonthlyPoint latest = latestPoint(months);
        return latest != null && (latest.getIngresos() > 0.0 || latest.getGastos() > 0.0);
    }

    @Nullable
    private AnalysisMonthlyPoint latestPoint(@NonNull List<AnalysisMonthlyPoint> months) {
        return months.isEmpty() ? null : months.get(months.size() - 1);
    }

    @Nullable
    private AnalysisMonthlyPoint latestExpensePoint(@NonNull List<AnalysisMonthlyPoint> months) {
        for (int i = months.size() - 1; i >= 0; i--) {
            AnalysisMonthlyPoint point = months.get(i);
            if (point.getGastos() > 0.0) return point;
        }
        return null;
    }

    @Nullable
    private AnalysisMonthlyPoint previousExpensePoint(@NonNull List<AnalysisMonthlyPoint> months) {
        boolean skippedLatest = false;
        for (int i = months.size() - 1; i >= 0; i--) {
            AnalysisMonthlyPoint point = months.get(i);
            if (point.getGastos() <= 0.0) continue;
            if (!skippedLatest) {
                skippedLatest = true;
                continue;
            }
            return point;
        }
        return null;
    }

    private int countExpenseDataMonths(@NonNull List<AnalysisMonthlyPoint> months) {
        int count = 0;
        for (AnalysisMonthlyPoint point : months) {
            if (point.getGastos() > 0.0) count++;
        }
        return count;
    }

    private String incomeExpenseBalanceText(@NonNull AnalysisMonthlyPoint latest) {
        double diff = latest.getIngresos() - latest.getGastos();
        if (diff > 0.005) {
            return "Balance positivo: te quedó " + Format.money(diff, currencyCode);
        }
        if (diff < -0.005) {
            return "Balance negativo: gastaste " + Format.money(Math.abs(diff), currencyCode)
                    + " más de lo que ingresaste";
        }
        return "Ingresos y gastos equilibrados";
    }

    private String balanceDeltaText(double delta) {
        if (delta < -0.005) {
            return "-" + Format.money(Math.abs(delta), currencyCode) + " este periodo";
        }
        return "+" + Format.money(Math.max(0.0, delta), currencyCode) + " este periodo";
    }

    private void renderSavingsSummary(@NonNull AnalysisMonthlyPoint latest) {
        double saving = latest.getAhorro();
        double income = latest.getIngresos();
        int percent = income <= 0.0 ? 0 : (int) Math.round((saving / income) * 100.0);
        int progress = Math.max(0, Math.min(100, percent));
        int accent = saving < -0.005 ? R.color.expense : R.color.income;
        progressSavings.setIndicatorColor(ContextCompat.getColor(requireContext(), accent));
        progressSavings.setProgressCompat(progress, true);
        tvSavingsPercent.setText(income <= 0.0 ? "0%" : percent + "%");
        tvSavingsPercent.setTextColor(ContextCompat.getColor(requireContext(), accent));
        tvSavingsHero.setText(R.string.analysis_savings_estimated);
        tvSavingsDetail.setText(Format.money(saving, currencyCode));
        tvSavingsDetail.setTextColor(ContextCompat.getColor(requireContext(), accent));
        tvSavingsMessage.setText(savingsMessage(saving, income, percent));
    }

    private String savingsMessage(double saving, double income, int percent) {
        String ratio = income > 0.0
                ? "Equivale al " + percent + "% de tus ingresos. "
                : "";
        if (saving > 0.005) {
            return ratio + "Buen mes, mantuviste un ahorro positivo";
        }
        if (Math.abs(saving) <= 0.005) {
            return ratio + "No generaste ahorro este mes";
        }
        return ratio + "Este mes gastaste más de lo que ingresaste";
    }

    private void renderIncomeExpenseAmounts(@NonNull List<AnalysisMonthlyPoint> months) {
        listIncomeExpenseAmounts.removeAllViews();
        AnalysisMonthlyPoint latest = latestPoint(months);
        boolean hasData = latest != null && (latest.getIngresos() > 0.0 || latest.getGastos() > 0.0);
        listIncomeExpenseAmounts.setVisibility(hasData ? View.VISIBLE : View.GONE);
        tvIncomeExpenseBalance.setVisibility(hasData ? View.VISIBLE : View.GONE);
        if (!hasData) return;
        listIncomeExpenseAmounts.addView(
                incomeExpenseAmountPill("Ingresos", Format.money(latest.getIngresos(), currencyCode), R.color.income),
                weightedPillParams(0)
        );
        listIncomeExpenseAmounts.addView(
                incomeExpenseAmountPill("Gastos", Format.money(latest.getGastos(), currencyCode), R.color.expense),
                weightedPillParams(10)
        );
    }

    private View incomeExpenseAmountPill(@NonNull String label, @NonNull String value, int colorRes) {
        int accent = ContextCompat.getColor(requireContext(), colorRes);
        LinearLayout pill = new LinearLayout(requireContext());
        pill.setOrientation(LinearLayout.VERTICAL);
        pill.setGravity(android.view.Gravity.CENTER);
        pill.setPadding(dp(10), dp(9), dp(10), dp(9));
        pill.setBackground(roundedDrawable(ColorUtils.setAlphaComponent(accent, 26), dp(12)));

        TextView labelView = new TextView(requireContext());
        labelView.setText(label);
        labelView.setTextColor(accent);
        labelView.setTextSize(12f);
        labelView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        pill.addView(labelView);

        TextView valueView = new TextView(requireContext());
        valueView.setText(value);
        valueView.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurface));
        valueView.setTextSize(14f);
        valueView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        valueView.setGravity(android.view.Gravity.CENTER);
        autosize(valueView, 11, 14);
        pill.addView(valueView, fullWidthParams(4));
        return pill;
    }

    private LinearLayout.LayoutParams weightedPillParams(int leftMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        params.leftMargin = dp(leftMargin);
        return params;
    }

    private void renderMonthLabels(
            @NonNull LinearLayout container,
            @NonNull List<AnalysisMonthlyPoint> months,
            boolean visible
    ) {
        container.removeAllViews();
        container.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (!visible || months.isEmpty()) return;
        for (AnalysisMonthlyPoint point : months) {
            TextView label = new TextView(requireContext());
            label.setText(shortMonthLabel(point));
            label.setGravity(android.view.Gravity.CENTER);
            label.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurfaceVariant));
            label.setTextSize(11f);
            label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            container.addView(label, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        }
    }

    private String shortMonthLabel(@NonNull AnalysisMonthlyPoint point) {
        String label = point.getLabel() == null ? "" : point.getLabel().trim();
        if (label.length() > 3) return label.substring(0, 3);
        return label.isEmpty() ? String.valueOf(point.getMes()) : label;
    }

    private void showLatestExpensePoint() {
        AnalysisChartState state = lastRenderedChartState;
        if (state == null) return;
        AnalysisMonthlyPoint point = latestExpensePoint(state.getMonths());
        if (point == null) return;
        Toast.makeText(
                requireContext(),
                shortMonthLabel(point) + ": " + Format.money(point.getGastos(), currencyCode),
                Toast.LENGTH_SHORT
        ).show();
    }

    private void renderCategoryBars(@NonNull List<AnalysisCategoryPoint> categories) {
        listCategoryBars.removeAllViews();
        boolean empty = categories.isEmpty();
        listCategoryBars.setVisibility(empty ? View.GONE : View.VISIBLE);
        tvCategoryBarsEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (empty) {
            tvCategoryBarsSummary.setText(R.string.analysis_categories_subtitle);
            return;
        }
        double total = 0.0;
        for (AnalysisCategoryPoint category : categories) total += category.getAmount();
        tvCategoryBarsSummary.setText(R.string.analysis_categories_subtitle);
        for (int i = 0; i < categories.size(); i++) {
            AnalysisCategoryPoint category = categories.get(i);
            double ratio = total <= 0.0 ? 0.0 : category.getAmount() / total;
            listCategoryBars.addView(categoryBarRow(category, ratio, categoryColor(i)));
        }
    }

    private View categoryBarRow(@NonNull AnalysisCategoryPoint category, double ratio, int color) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, dp(7), 0, dp(7));

        LinearLayout labels = new LinearLayout(requireContext());
        labels.setGravity(android.view.Gravity.CENTER_VERTICAL);
        labels.setOrientation(LinearLayout.HORIZONTAL);

        TextView name = new TextView(requireContext());
        name.setText(nonEmpty(category.getName(), getString(R.string.home_uncategorized)));
        name.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        name.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurface));
        name.setTextSize(14f);
        labels.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView amount = new TextView(requireContext());
        int percent = (int) Math.round(Math.max(0.0, ratio) * 100.0);
        amount.setText(Format.money(category.getAmount(), currencyCode) + " · " + percent + "%");
        amount.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurfaceVariant));
        amount.setTextSize(13f);
        labels.addView(amount);
        row.addView(labels);

        LinearLayout track = new LinearLayout(requireContext());
        track.setOrientation(LinearLayout.HORIZONTAL);
        track.setBackground(roundedDrawable(ColorUtils.setAlphaComponent(
                ContextCompat.getColor(requireContext(), R.color.md_theme_outlineVariant),
                80
        ), dp(4)));
        LinearLayout.LayoutParams trackParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(8)
        );
        trackParams.topMargin = dp(7);

        View fill = new View(requireContext());
        fill.setBackground(roundedDrawable(color, dp(4)));
        float fillWeight = Math.max(0.05f, Math.min(1f, (float) ratio));
        track.addView(fill, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, fillWeight));
        track.addView(new View(requireContext()), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f - fillWeight));
        row.addView(track, trackParams);
        return row;
    }

    private GradientDrawable roundedDrawable(int color, int radius) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(radius);
        return bg;
    }

    private int categoryColor(int index) {
        int[] palette = new int[]{
                R.color.chart_pie_1, R.color.chart_pie_3, R.color.chart_pie_4, R.color.chart_pie_6,
                R.color.chart_pie_5, R.color.chart_pie_8, R.color.chart_pie_10, R.color.chart_pie_12
        };
        return ContextCompat.getColor(requireContext(), palette[index % palette.length]);
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
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(10), dp(20), dp(18));
        root.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.planning_dialog_surface));
        root.setClipToPadding(false);

        FrameLayout sheetContent = new FrameLayout(requireContext());
        sheetContent.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.planning_dialog_surface));
        sheetContent.addView(root, new FrameLayout.LayoutParams(
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
        titleView.setTextSize(23f);
        titleView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(titleView, fullWidthParams(18));

        root.addView(heroCard(status, hero), fixedFullWidthParams(14, 106));

        LinearLayout metrics = new LinearLayout(requireContext());
        metrics.setOrientation(LinearLayout.HORIZONTAL);
        int firstIcon = showActionable ? resolveDrawable("ic_speed", R.drawable.ic_analytics) : R.drawable.ic_savings;
        int secondIcon = R.drawable.ic_account_balance_wallet;
        int thirdIcon = showActionable ? R.drawable.ic_bar_chart : R.drawable.ic_account_balance_wallet;
        metrics.addView(metricCard(metricOneLabel, metricOneValue, firstIcon, false), weightedMetricParams(0, 86));
        metrics.addView(metricCard(metricTwoLabel, metricTwoValue, secondIcon, false), weightedMetricParams(10, 86));
        root.addView(metrics, fullWidthParams(12));
        root.addView(metricCard(metricThreeLabel, metricThreeValue, thirdIcon, false), fixedFullWidthParams(10, 78));

        int recommendationIcon = showActionable ? R.drawable.ic_star : 0;
        root.addView(sectionCard(getString(R.string.detail_recommendation_main), recommendation, recommendationIcon, showActionable), fixedFullWidthParams(12, showActionable ? 88 : 80));
        root.addView(sectionCard(getString(R.string.detail_brief), brief, showActionable ? R.drawable.ic_article : R.drawable.ic_trending_up, false), fixedFullWidthParams(10, showActionable ? 78 : 70));
        if (showActionable) {
            root.addView(sectionCard(getString(R.string.detail_actionable_notice), recommendation, R.drawable.ic_notifications, false), fixedFullWidthParams(10, 78));
        }

        MaterialButton accept = new MaterialButton(requireContext());
        accept.setText(R.string.home_detail_accept);
        accept.setAllCaps(false);
        accept.setTextSize(17f);
        accept.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        accept.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorOnPrimary));
        accept.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.planning_dialog_button)));
        accept.setRippleColor(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.planning_dialog_button_pressed)));
        accept.setCornerRadius(dp(14));
        accept.setOnClickListener(v -> dialog.dismiss());
        LinearLayout.LayoutParams acceptParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56));
        acceptParams.topMargin = dp(12);
        root.addView(accept, acceptParams);

        dialog.setContentView(sheetContent);
        dialog.setOnShowListener(d -> {
            View sheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (sheet != null) {
                sheet.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.planning_dialog_surface));
                ViewGroup.LayoutParams params = sheet.getLayoutParams();
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                sheet.setLayoutParams(params);
                com.google.android.material.bottomsheet.BottomSheetBehavior<View> behavior =
                        com.google.android.material.bottomsheet.BottomSheetBehavior.from(sheet);
                behavior.setFitToContents(true);
                behavior.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
            }
        });
        dialog.show();
    }

    private View heroCard(@NonNull String status, @NonNull String hero) {
        LinearLayout card = cardContainer(true, dp(14), dp(12), dp(14), dp(12));
        TextView statusView = new TextView(requireContext());
        statusView.setText(status);
        statusView.setTextColor(ContextCompat.getColor(requireContext(), R.color.risk_medium_text));
        statusView.setTextSize(13f);
        statusView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        statusView.setPadding(dp(12), dp(5), dp(12), dp(5));
        GradientDrawable chipBg = new GradientDrawable();
        chipBg.setColor(ContextCompat.getColor(requireContext(), R.color.planning_dialog_field));
        chipBg.setStroke(dp(1), ContextCompat.getColor(requireContext(), R.color.risk_medium_text));
        chipBg.setCornerRadius(dp(11));
        statusView.setBackground(chipBg);
        card.addView(statusView, wrapParams(0));

        TextView heroView = new TextView(requireContext());
        heroView.setText(hero);
        heroView.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurface));
        heroView.setTextSize(18f);
        heroView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        autosize(heroView, 13, 18);
        LinearLayout.LayoutParams heroParams = fullWidthParams(12);
        card.addView(heroView, heroParams);
        return card;
    }

    private View metricCard(@NonNull String label, @NonNull String value, int iconRes, boolean highlighted) {
        LinearLayout card = cardContainer(highlighted, dp(12), dp(10), dp(12), dp(10));
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(android.view.Gravity.CENTER_VERTICAL);
        addIcon(card, iconRes, R.color.planning_dialog_icon);
        LinearLayout texts = new LinearLayout(requireContext());
        texts.setOrientation(LinearLayout.VERTICAL);
        TextView labelView = labelText(label, highlighted);
        TextView valueView = valueText(value);
        autosize(valueView, 13, 18);
        texts.addView(labelView);
        texts.addView(valueView, fullWidthParams(4));
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        textParams.leftMargin = dp(12);
        card.addView(texts, textParams);
        return card;
    }

    private View sectionCard(@NonNull String label, @NonNull String value, int iconRes, boolean highlighted) {
        LinearLayout card = cardContainer(highlighted, dp(12), dp(10), dp(12), dp(10));
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(android.view.Gravity.CENTER_VERTICAL);
        if (iconRes != 0) addIcon(card, iconRes, highlighted ? R.color.risk_medium_text : R.color.planning_dialog_icon);
        LinearLayout texts = new LinearLayout(requireContext());
        texts.setOrientation(LinearLayout.VERTICAL);
        TextView labelView = labelText(label, highlighted);
        TextView valueView = new TextView(requireContext());
        valueView.setText(value);
        valueView.setTextColor(ContextCompat.getColor(requireContext(), highlighted ? R.color.md_theme_onSurface : R.color.md_theme_onSurfaceVariant));
        valueView.setTextSize(highlighted ? 15f : 14f);
        valueView.setLineSpacing(0f, 1.0f);
        autosize(valueView, 11, highlighted ? 15 : 14);
        texts.addView(labelView);
        texts.addView(valueView, fullWidthParams(3));
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        textParams.leftMargin = iconRes == 0 ? 0 : dp(12);
        card.addView(texts, textParams);
        return card;
    }

    private TextView labelText(@NonNull String label, boolean highlighted) {
        TextView view = new TextView(requireContext());
        view.setText(label);
        view.setTextColor(ContextCompat.getColor(requireContext(), highlighted ? R.color.risk_medium_text : R.color.md_theme_onSurface));
        view.setTextSize(14f);
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private TextView valueText(@NonNull String value) {
        TextView view = new TextView(requireContext());
        view.setText(value);
        view.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurface));
        view.setTextSize(18f);
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private LinearLayout cardContainer(boolean highlighted, int left, int top, int right, int bottom) {
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(left, top, right, bottom);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(ContextCompat.getColor(requireContext(), highlighted ? R.color.risk_medium_bg : R.color.planning_dialog_field));
        bg.setStroke(dp(1), ContextCompat.getColor(requireContext(), highlighted ? R.color.risk_medium_text : R.color.planning_dialog_field_stroke));
        bg.setCornerRadius(dp(16));
        card.setBackground(bg);
        return card;
    }

    private void addIcon(@NonNull LinearLayout parent, int iconRes, int tintColorRes) {
        ImageView icon = new ImageView(requireContext());
        icon.setImageResource(iconRes);
        icon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), tintColorRes)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(26), dp(26));
        parent.addView(icon, params);
    }

    private LinearLayout.LayoutParams fullWidthParams(int topMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(topMargin);
        return params;
    }

    private LinearLayout.LayoutParams fixedFullWidthParams(int topMargin, int heightDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(heightDp));
        params.topMargin = dp(topMargin);
        return params;
    }

    private LinearLayout.LayoutParams weightedMetricParams(int leftMargin, int heightDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(heightDp), 1f);
        params.leftMargin = dp(leftMargin);
        return params;
    }

    private LinearLayout.LayoutParams wrapParams(int topMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(topMargin);
        return params;
    }

    private void autosize(@NonNull TextView view, int minSp, int maxSp) {
        view.setAutoSizeTextTypeUniformWithConfiguration(minSp, maxSp, 1, TypedValue.COMPLEX_UNIT_SP);
    }

    private int resolveDrawable(@NonNull String name, int fallback) {
        int id = getResources().getIdentifier(name, "drawable", requireContext().getPackageName());
        return id == 0 ? fallback : id;
    }

    private String nonEmpty(@Nullable String value, @NonNull String fallback) {
        String clean = value == null ? "" : value.trim();
        return clean.isEmpty() ? fallback : clean;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
