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
import com.example.finanzas.data.model.HomeSummary;
import com.example.finanzas.ui.view.SpendlyDecorBackgroundDrawable;
import com.example.finanzas.ui.viewmodel.HomeViewModel;
import com.example.finanzas.util.Format;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.bottomsheet.BottomSheetDialog;
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
    private TextView tvScore;
    private TextView tvInsight;
    private TextView tvSavingStatus;
    private TextView tvSaving;
    private MaterialButton btnFinancialDetail;
    private MaterialButton btnSavingDetail;
    private CartesianChartView chartIncomeExpense;
    private CartesianChartView chartExpenseTrend;
    private CartesianChartView chartBalance;
    private CartesianChartView chartSavings;
    private LinearLayout listCategoryBars;
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
        tvScore = view.findViewById(R.id.tvAnalysisScore);
        tvInsight = view.findViewById(R.id.tvAnalysisInsight);
        tvSavingStatus = view.findViewById(R.id.tvAnalysisSavingStatus);
        tvSaving = view.findViewById(R.id.tvAnalysisSaving);
        btnFinancialDetail = view.findViewById(R.id.btnAnalysisFinancialDetail);
        btnSavingDetail = view.findViewById(R.id.btnAnalysisSavingDetail);
        styleDetailButton(btnFinancialDetail);
        styleDetailButton(btnSavingDetail);
        chartIncomeExpense = view.findViewById(R.id.chartAnalysisIncomeExpense);
        chartExpenseTrend = view.findViewById(R.id.chartAnalysisExpenseTrend);
        chartBalance = view.findViewById(R.id.chartAnalysisBalance);
        chartSavings = view.findViewById(R.id.chartAnalysisSavings);
        listCategoryBars = view.findViewById(R.id.listAnalysisCategoryBars);
        viewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);
        resetRenderCache();
        chartIncomeExpense.setModelProducer(viewModel.getIncomeExpenseChartProducer());
        chartExpenseTrend.setModelProducer(viewModel.getExpenseTrendChartProducer());
        chartBalance.setModelProducer(viewModel.getBalanceChartProducer());
        chartSavings.setModelProducer(viewModel.getSavingsChartProducer());
        currencyCode = SettingsService.getCurrencyCode(requireContext());

        swipe.setOnRefreshListener(() -> load(true));
        btnFinancialDetail.setOnClickListener(v -> showFinancialDetail());
        btnSavingDetail.setOnClickListener(v -> showSavingDetail());
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

    private void renderAnalysisCharts(@NonNull AnalysisChartState state) {
        if (state.equals(lastRenderedChartState)) return;
        lastRenderedChartState = state;
        currencyCode = nonEmpty(state.getCurrencyCode(), currencyCode);
        setChartState(chartIncomeExpense, tvIncomeExpenseEmpty, state.hasIncomeExpense());
        setChartState(chartExpenseTrend, tvExpenseTrendEmpty, state.hasExpenseTrend());
        setChartState(chartBalance, tvBalanceEmpty, state.hasBalance());
        setChartState(chartSavings, tvSavingsEmpty, state.hasSavings());
        renderChartSummaries(state.getMonths());
        renderCategoryBars(state.getCategories());
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
            tvIncomeExpenseSummary.setText(R.string.analysis_income_expense_subtitle);
            tvExpenseTrendSummary.setText(R.string.analysis_expense_trend_subtitle);
            tvBalanceSummary.setText(R.string.analysis_balance_subtitle);
            tvSavingsSummary.setText(R.string.analysis_savings_subtitle);
            tvSavingsSummary.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurfaceVariant));
            return;
        }
        AnalysisMonthlyPoint latest = months.get(months.size() - 1);
        tvIncomeExpenseSummary.setText(
                "Último mes: ingresos " + Format.money(latest.getIngresos(), currencyCode)
                        + " · gastos " + Format.money(latest.getGastos(), currencyCode)
        );
        tvExpenseTrendSummary.setText(expenseTrendSummary(months));
        tvBalanceSummary.setText("Balance acumulado: " + Format.money(latest.getBalance(), currencyCode));
        tvSavingsSummary.setText("Ahorro del mes: " + Format.money(latest.getAhorro(), currencyCode));
        tvSavingsSummary.setTextColor(ContextCompat.getColor(
                requireContext(),
                latest.getAhorro() < 0 ? R.color.expense : R.color.md_theme_onSurfaceVariant
        ));
    }

    private String expenseTrendSummary(@NonNull List<AnalysisMonthlyPoint> months) {
        AnalysisMonthlyPoint first = months.get(0);
        AnalysisMonthlyPoint latest = months.get(months.size() - 1);
        double delta = latest.getGastos() - first.getGastos();
        if (Math.abs(delta) < 0.005) {
            return "Gasto estable: " + Format.money(latest.getGastos(), currencyCode);
        }
        String direction = delta > 0 ? "subió" : "bajó";
        return "El gasto " + direction + " " + Format.money(Math.abs(delta), currencyCode)
                + " vs. el inicio del periodo.";
    }

    private void setChartState(@NonNull CartesianChartView chart, @NonNull TextView empty, boolean hasData) {
        chart.setVisibility(hasData ? View.VISIBLE : View.GONE);
        empty.setVisibility(hasData ? View.GONE : View.VISIBLE);
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
        double max = categories.get(0).getAmount();
        for (AnalysisCategoryPoint category : categories) total += category.getAmount();
        tvCategoryBarsSummary.setText("Total categorizado: " + Format.money(total, currencyCode));
        for (int i = 0; i < categories.size(); i++) {
            AnalysisCategoryPoint category = categories.get(i);
            double ratio = max <= 0.0 ? 0.0 : category.getAmount() / max;
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
        amount.setText(Format.money(category.getAmount(), currencyCode));
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
        float fillWeight = Math.max(0.05f, (float) ratio);
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
