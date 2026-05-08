package com.example.finanzas.ui;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.animation.ValueAnimator;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.widget.TextViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.finanzas.R;
import com.example.finanzas.data.api.AccountService;
import com.example.finanzas.data.api.CategoryStore;
import com.example.finanzas.data.api.SettingsService;
import com.example.finanzas.data.model.AccountBalance;
import com.example.finanzas.data.model.FinancialAccount;
import com.example.finanzas.data.model.HomeSummary;
import com.example.finanzas.data.model.Transaccion;
import com.example.finanzas.ui.view.SpendlyDecorBackgroundDrawable;
import com.example.finanzas.ui.viewmodel.HomeViewModel;
import com.example.finanzas.util.CategoryVisuals;
import com.example.finanzas.util.Format;
import com.example.finanzas.util.FinancialAlertNotifier;
import com.example.finanzas.util.LabelColorUtils;
import com.example.finanzas.util.MicroAnimations;
import com.example.finanzas.util.NavigationAnimations;
import com.example.finanzas.util.Prefs;
import com.example.finanzas.util.TransactionLabelStore;
import com.example.finanzas.util.UiFormUtils;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;

public class HomeFragment extends Fragment {
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipe;
    private CircularProgressIndicator progress;
    private TextView tvPeriod;
    private TextView tvBalanceTotal;
    private View cardIncome;
    private View cardExpense;
    private View cardCash;
    private View cardCard;
    private View btnSettings;
    private GridLayout listAccounts;
    private TextView tvBudgetMissing;
    private MaterialCardView cardBudget;
    private TextView tvBudgetPercent;
    private TextView tvBudgetTop;
    private LinearProgressIndicator progressBudget;
    private View btnSeeTransactions;
    private LinearLayout listLatest;
    private TextView tvLatestEmpty;
    private HomeViewModel viewModel;
    private String currencyCode = "PEN";
    private HomeSummary lastSummary;
    private boolean hasDynamicAccounts;
    private boolean testerThanksDialogShowing;
    private Double lastRenderedBalance = null;
    private ValueAnimator balanceAnimator;
    private Double balanceAnimationTarget = null;
    private String lastCardMetricRenderKey;
    private String lastLatestRenderKey;
    private String lastNotificationRenderKey;
    private Integer lastBudgetProgress = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.setBackground(new SpendlyDecorBackgroundDrawable(requireContext()));
        swipe = view.findViewById(R.id.swipeHome);
        progress = view.findViewById(R.id.progressHome);
        tvPeriod = view.findViewById(R.id.tvHomePeriod);
        tvBalanceTotal = view.findViewById(R.id.tvHomeBalanceTotal);
        cardIncome = view.findViewById(R.id.cardIncome);
        cardExpense = view.findViewById(R.id.cardExpense);
        cardCash = view.findViewById(R.id.cardCash);
        cardCard = view.findViewById(R.id.cardCard);
        btnSettings = view.findViewById(R.id.btnHomeSettings);
        listAccounts = view.findViewById(R.id.listHomeAccounts);
        tvBudgetMissing = view.findViewById(R.id.tvHomeBudgetMissing);
        cardBudget = view.findViewById(R.id.cardHomeBudget);
        tvBudgetPercent = view.findViewById(R.id.tvHomeBudgetPercent);
        tvBudgetTop = view.findViewById(R.id.tvHomeBudgetTop);
        progressBudget = view.findViewById(R.id.progressHomeBudget);
        btnSeeTransactions = view.findViewById(R.id.btnHomeSeeTransactions);
        listLatest = view.findViewById(R.id.listHomeLatestTransactions);
        tvLatestEmpty = view.findViewById(R.id.tvHomeLatestEmpty);
        viewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);
        currencyCode = SettingsService.getCurrencyCode(requireContext());
        resetViewRenderCache();

        setupNavigation(view);
        swipe.setOnRefreshListener(() -> loadSummary(true));
        observeViewModel();
        maybeShowInitialCurrencyDialog();
    }

    @Override
    public void onResume() {
        super.onResume();
        currencyCode = SettingsService.getCurrencyCode(requireContext());
        loadSummary(false);
    }

    @Override
    public void onDestroyView() {
        if (balanceAnimator != null) {
            balanceAnimator.cancel();
            balanceAnimator = null;
        }
        balanceAnimationTarget = null;
        resetViewRenderCache();
        super.onDestroyView();
    }

    private void resetViewRenderCache() {
        lastCardMetricRenderKey = null;
        lastLatestRenderKey = null;
        lastBudgetProgress = null;
    }

    private void setupNavigation(@NonNull View root) {
        btnSettings.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.nav_settings, null, NavigationAnimations.detailSlide()));
        btnSeeTransactions.setOnClickListener(v ->
                Navigation.findNavController(root).navigate(R.id.nav_list, null, NavigationAnimations.detailSlide()));
    }

    private void observeViewModel() {
        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            boolean isLoading = Boolean.TRUE.equals(loading);
            progress.setVisibility(isLoading && lastSummary == null ? View.VISIBLE : View.GONE);
            swipe.setRefreshing(isLoading && lastSummary != null);
        });
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
        viewModel.getError().observe(getViewLifecycleOwner(), ignored ->
                UiFormUtils.showMessage(requireView(), R.string.error_cargar_transacciones));
    }

    private void loadSummary(boolean force) {
        Calendar cal = Calendar.getInstance();
        viewModel.loadSummary(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, force);
    }

    private void render(@NonNull HomeSummary summary) {
        lastSummary = summary;
        tvPeriod.setText(Format.monthYear(summary.getAnio(), summary.getMes()));
        renderAnimatedBalance(summary.getSaldoActualTotal());
        styleMetric(cardIncome, R.string.home_ingresos, Format.money(summary.getIngresos(), currencyCode), R.drawable.ic_income, color(R.color.income));
        styleMetric(cardExpense, R.string.home_gastos, Format.money(summary.getGastos(), currencyCode), R.drawable.ic_expense, color(R.color.expense));
        styleMetric(cardCash, R.string.home_cash, Format.money(summary.getEfectivo(), currencyCode), R.drawable.ic_cash, color(R.color.income));
        renderCardMetric(summary);
        renderAccounts(summary.getAccountBalances());
        renderBudget(summary);
        renderLatest(summary.getLatestTransactions());
        notifyImportantAlertIfNeeded(summary);
    }

    private void notifyImportantAlertIfNeeded(@NonNull HomeSummary summary) {
        String key = summary.getAnio()
                + "|" + summary.getMes()
                + "|" + Math.round(summary.getSaldoActualTotal() * 100.0)
                + "|" + Math.round(summary.getProyeccionFinMes() * 100.0)
                + "|" + summary.getAlertaPrincipal()
                + "|" + currencyCode;
        if (Objects.equals(key, lastNotificationRenderKey)) return;
        lastNotificationRenderKey = key;
        FinancialAlertNotifier.maybeNotifyImportantAlert(requireContext(), summary, currencyCode);
    }

    private void styleMetric(@NonNull View card, int titleRes, @NonNull String value, @DrawableRes int iconRes, @ColorInt int accent) {
        TextView title = card.findViewById(R.id.metricTitle);
        TextView amount = card.findViewById(R.id.metricValue);
        ImageView icon = card.findViewById(R.id.metricIcon);
        FrameLayout iconBg = card.findViewById(R.id.metricIconBg);
        title.setText(titleRes);
        title.setTextColor(color(R.color.md_theme_onSurface));
        title.setTextSize(15f);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        amount.setText(value);
        amount.setTextColor(accent);
        amount.setTextSize(18f);
        amount.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        icon.setImageResource(iconRes);
        icon.setColorFilter(accent);
        ViewGroup.LayoutParams iconBgParams = iconBg.getLayoutParams();
        iconBgParams.width = dp(40);
        iconBgParams.height = dp(40);
        iconBg.setLayoutParams(iconBgParams);
        ViewGroup.LayoutParams iconParams = icon.getLayoutParams();
        iconParams.width = dp(24);
        iconParams.height = dp(24);
        icon.setLayoutParams(iconParams);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(ColorUtils.setAlphaComponent(accent, 45));
        iconBg.setBackground(bg);
    }

    private void renderAccounts(@Nullable List<AccountBalance> balances) {
        if (listAccounts.getChildCount() > 0) listAccounts.removeAllViews();
        hasDynamicAccounts = false;
        if (listAccounts.getVisibility() != View.GONE) listAccounts.setVisibility(View.GONE);
    }

    private void renderCardMetric(@NonNull HomeSummary summary) {
        if (!(cardCard instanceof MaterialCardView)) return;
        Map<String, Double> balances = accountBalanceMap(summary.getAccountBalances());
        List<FinancialAccount> cards = SettingsService.listCardAccounts(requireContext());
        List<FinancialAccount> included = new java.util.ArrayList<>();
        for (FinancialAccount account : cards) {
            if (account.isIncludedInTotal()) included.add(account);
        }
        if (included.isEmpty() && !cards.isEmpty()) included.add(SettingsService.getVisibleCardAccount(requireContext()));
        boolean summaryMode = included.size() > 1;
        FinancialAccount visible = summaryMode ? null : (included.isEmpty() ? SettingsService.getVisibleCardAccount(requireContext()) : included.get(0));
        double amountValue = summaryMode ? sumBalances(included, balances) : balanceFor(visible, balances);
        String renderKey = cardMetricRenderKey(cards, included, visible, summaryMode, amountValue);
        MaterialCardView card = (MaterialCardView) cardCard;
        if (Objects.equals(renderKey, lastCardMetricRenderKey) && card.getChildCount() > 0) return;
        lastCardMetricRenderKey = renderKey;

        card.removeAllViews();
        card.setCardBackgroundColor(color(R.color.md_theme_surface));
        card.setStrokeColor(color(R.color.md_theme_outlineVariant));
        card.setStrokeWidth(dp(1));
        card.setRadius(dp(8));

        FrameLayout body = new FrameLayout(requireContext());
        body.setPadding(dp(16), dp(14), dp(9), dp(14));
        card.addView(body, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        ImageView icon = new ImageView(requireContext());
        icon.setImageResource(R.drawable.ic_card);
        icon.setColorFilter(color(R.color.chartBalance));
        GradientDrawable iconBg = new GradientDrawable();
        iconBg.setShape(GradientDrawable.OVAL);
        iconBg.setColor(ColorUtils.setAlphaComponent(color(R.color.chartBalance), 45));
        icon.setBackground(iconBg);
        icon.setPadding(dp(7), dp(7), dp(7), dp(7));
        FrameLayout.LayoutParams iconParams = new FrameLayout.LayoutParams(dp(40), dp(40));
        iconParams.gravity = android.view.Gravity.START | android.view.Gravity.CENTER_VERTICAL;
        body.addView(icon, iconParams);

        LinearLayout text = new LinearLayout(requireContext());
        text.setOrientation(LinearLayout.VERTICAL);
        text.setGravity(android.view.Gravity.CENTER_VERTICAL);
        text.setMinimumHeight(dp(58));
        FrameLayout.LayoutParams textParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        textParams.gravity = android.view.Gravity.CENTER_VERTICAL;
        textParams.leftMargin = dp(56);
        textParams.rightMargin = dp(4);
        body.addView(text, textParams);

        TextView title = new TextView(requireContext());
        title.setText(summaryMode ? "Tarjetas" : compactName(visible));
        title.setTextColor(color(R.color.md_theme_onSurface));
        title.setTextSize(15f);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        text.addView(title);

        TextView subtitle = new TextView(requireContext());
        String subtitleText = summaryMode ? getString(R.string.home_cards_selected_count, included.size()) : maskedLast4(visible);
        subtitle.setText(subtitleText);
        subtitle.setTextColor(color(R.color.md_theme_onSurfaceVariant));
        subtitle.setTextSize(11.5f);
        subtitle.setSingleLine(true);
        subtitle.setEllipsize(TextUtils.TruncateAt.END);
        subtitle.setVisibility(TextUtils.isEmpty(subtitleText) ? View.GONE : View.VISIBLE);
        text.addView(subtitle);

        TextView amount = new TextView(requireContext());
        amount.setText(Format.money(amountValue, currencyCode));
        amount.setTextColor(color(R.color.chartBalance));
        amount.setTextSize(18f);
        amount.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        amount.setSingleLine(true);
        amount.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams amountParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        amountParams.topMargin = TextUtils.isEmpty(subtitleText) ? dp(4) : dp(2);
        text.addView(amount, amountParams);

        LinearLayout actions = new LinearLayout(requireContext());
        actions.setOrientation(LinearLayout.VERTICAL);
        actions.setGravity(android.view.Gravity.CENTER);
        FrameLayout.LayoutParams actionsParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        actionsParams.gravity = android.view.Gravity.END | android.view.Gravity.TOP;
        body.addView(actions, actionsParams);

        if (cards.size() <= 1) {
            View add = circularIconButton(R.drawable.ic_add, color(R.color.planning_dialog_button), color(R.color.md_theme_onPrimary), dp(28));
            add.setOnClickListener(v -> showManageCardsSheet());
            actions.addView(add);
        } else if (summaryMode) {
            View tune = plainIconButton(R.drawable.ic_tune, color(R.color.chartBalance), dp(30));
            tune.setOnClickListener(v -> showManageCardsSheet());
            actions.addView(tune);
        } else {
            View swap = circularIconButton(R.drawable.ic_swap_horiz, color(R.color.chart_pie_4), color(R.color.black), dp(28));
            swap.setOnClickListener(v -> showManageCardsSheet());
            actions.addView(swap);
        }
    }

    private Map<String, Double> accountBalanceMap(@Nullable List<AccountBalance> balances) {
        Map<String, Double> map = new HashMap<>();
        if (balances == null || balances.isEmpty()) {
            return map;
        }
        for (AccountBalance account : balances) {
            if (account == null) continue;
            map.put(SettingsService.normalizeAccountType(account.getId()), account.getBalance());
        }
        return map;
    }

    private double balanceFor(@Nullable FinancialAccount account, @NonNull Map<String, Double> balances) {
        if (account == null) return 0.0;
        Double value = balances.get(SettingsService.normalizeAccountType(account.getId()));
        return value == null ? 0.0 : value;
    }

    private double sumBalances(@NonNull List<FinancialAccount> accounts, @NonNull Map<String, Double> balances) {
        double sum = 0.0;
        for (FinancialAccount account : accounts) sum += balanceFor(account, balances);
        return sum;
    }

    private String compactName(@Nullable FinancialAccount account) {
        if (account == null || TextUtils.isEmpty(account.getName())) return getString(R.string.transaction_account_card);
        String name = account.getName().trim();
        if (name.toLowerCase(Locale.ROOT).contains("predeterminada")) return getString(R.string.transaction_account_card);
        return name.length() > 7 ? name.substring(0, 7) : name;
    }

    private String maskedLast4(@Nullable FinancialAccount account) {
        String last4 = account == null ? "" : account.getLast4();
        if (TextUtils.isEmpty(last4)) return "";
        return "\u2022\u2022\u2022\u2022 " + last4;
    }

    private void renderAnimatedBalance(double newBalance) {
        if (tvBalanceTotal == null) return;

        if (balanceAnimator != null && balanceAnimator.isRunning() && balanceAnimationTarget != null
                && Math.abs(balanceAnimationTarget - newBalance) < 0.005) {
            return;
        }

        if (lastRenderedBalance == null || Math.abs(lastRenderedBalance - newBalance) < 0.005) {
            if (balanceAnimator != null) balanceAnimator.cancel();
            balanceAnimationTarget = null;
            tvBalanceTotal.setText(Format.money(newBalance, currencyCode));
            tvBalanceTotal.setTextColor(color(R.color.md_theme_onSurface));
            lastRenderedBalance = newBalance;
            return;
        }

        if (!MicroAnimations.areAnimationsEnabled(requireContext())) {
            if (balanceAnimator != null) balanceAnimator.cancel();
            balanceAnimationTarget = null;
            tvBalanceTotal.setText(Format.money(newBalance, currencyCode));
            tvBalanceTotal.setTextColor(color(R.color.md_theme_onSurface));
            lastRenderedBalance = newBalance;
            return;
        }

        if (balanceAnimator != null) balanceAnimator.cancel();
        double from = lastRenderedBalance;
        int defaultColor = color(R.color.md_theme_onSurface);
        int pulseColor = newBalance > from ? color(R.color.income) : color(R.color.expense);
        balanceAnimationTarget = newBalance;
        balanceAnimator = ValueAnimator.ofFloat((float) from, (float) newBalance);
        balanceAnimator.setDuration(420L);
        balanceAnimator.addUpdateListener(animation -> {
            double value = ((Float) animation.getAnimatedValue()).doubleValue();
            tvBalanceTotal.setText(Format.money(value, currencyCode));
            tvBalanceTotal.setTextColor(pulseColor);
            lastRenderedBalance = value;
        });
        balanceAnimator.addListener(new android.animation.AnimatorListenerAdapter() {
            private boolean cancelled;

            @Override
            public void onAnimationCancel(android.animation.Animator animation) {
                cancelled = true;
            }

            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                if (cancelled) return;
                tvBalanceTotal.setText(Format.money(newBalance, currencyCode));
                tvBalanceTotal.setTextColor(defaultColor);
                lastRenderedBalance = newBalance;
                balanceAnimationTarget = null;
                balanceAnimator = null;
            }
        });
        balanceAnimator.start();
    }

    private void renderBudget(@NonNull HomeSummary summary) {
        boolean hasBudget = summary.getPresupuestoMonto() > 0;
        tvBudgetMissing.setVisibility(hasBudget ? View.GONE : View.VISIBLE);
        cardBudget.setVisibility(hasBudget ? View.VISIBLE : View.GONE);
        if (!hasBudget) {
            lastBudgetProgress = null;
            return;
        }
        int percent = (int) Math.max(0, Math.min(999, Math.round(summary.getPresupuestoPorcentaje())));
        tvBudgetPercent.setText(getString(R.string.home_budget_used_value, percent));
        String top = summary.getCategoriaMayorGasto() == null || summary.getCategoriaMayorGastoMonto() <= 0
                ? getString(R.string.home_top_category_empty)
                : getString(R.string.home_top_category_value, summary.getCategoriaMayorGasto(), Format.money(summary.getCategoriaMayorGastoMonto(), currencyCode));
        tvBudgetTop.setText(top);
        int progress = Math.min(100, percent);
        boolean animateProgress = lastBudgetProgress != null
                && lastBudgetProgress != progress
                && MicroAnimations.areAnimationsEnabled(requireContext());
        progressBudget.setProgressCompat(progress, animateProgress);
        lastBudgetProgress = progress;
    }

    private void renderLatest(@Nullable List<Transaccion> transactions) {
        boolean empty = transactions == null || transactions.isEmpty();
        if (empty) {
            if (!Objects.equals(lastLatestRenderKey, "empty")
                    || listLatest.getChildCount() > 0
                    || tvLatestEmpty.getVisibility() != View.VISIBLE) {
                listLatest.removeAllViews();
                lastLatestRenderKey = "empty";
            }
            tvLatestEmpty.setVisibility(View.VISIBLE);
            return;
        }
        List<Transaccion> orderedTransactions = latestFirst(transactions);
        Map<Integer, TransactionLabelStore.Label> labels = TransactionLabelStore.assignedLabelDetails(requireContext());
        String renderKey = latestRenderKey(orderedTransactions, labels);
        if (Objects.equals(renderKey, lastLatestRenderKey)
                && listLatest.getChildCount() > 0
                && tvLatestEmpty.getVisibility() != View.VISIBLE) return;
        lastLatestRenderKey = renderKey;

        listLatest.removeAllViews();
        tvLatestEmpty.setVisibility(View.GONE);
        int count = 0;
        for (Transaccion tx : orderedTransactions) {
            if (tx == null || count >= 5) continue;
            listLatest.addView(transactionRow(tx, labels.get(tx.getId())));
            count++;
        }
    }

    @NonNull
    private List<Transaccion> latestFirst(@NonNull List<Transaccion> transactions) {
        List<Transaccion> ordered = new ArrayList<>(transactions);
        ordered.sort((left, right) -> {
            int byDate = Long.compare(transactionTime(right), transactionTime(left));
            if (byDate != 0) return byDate;
            int leftId = left == null ? 0 : left.getId();
            int rightId = right == null ? 0 : right.getId();
            return Integer.compare(rightId, leftId);
        });
        return ordered;
    }

    private long transactionTime(@Nullable Transaccion tx) {
        Date date = tx == null ? null : tx.getFecha();
        return date == null ? 0L : date.getTime();
    }

    private String cardMetricRenderKey(
            @NonNull List<FinancialAccount> cards,
            @NonNull List<FinancialAccount> included,
            @Nullable FinancialAccount visible,
            boolean summaryMode,
            double amountValue
    ) {
        StringBuilder key = new StringBuilder(currencyCode)
                .append('|')
                .append(summaryMode)
                .append('|')
                .append(Math.round(amountValue * 100.0));
        key.append("|cards=");
        for (FinancialAccount account : cards) {
            if (account == null) continue;
            key.append(account.getId())
                    .append(':')
                    .append(account.getName())
                    .append(':')
                    .append(account.getLast4())
                    .append(':')
                    .append(account.isIncludedInTotal())
                    .append(';');
        }
        key.append("|included=");
        for (FinancialAccount account : included) {
            if (account != null) key.append(account.getId()).append(';');
        }
        key.append("|visible=").append(visible == null ? "" : visible.getId());
        return key.toString();
    }

    private String latestRenderKey(
            @NonNull List<Transaccion> transactions,
            @NonNull Map<Integer, TransactionLabelStore.Label> labels
    ) {
        StringBuilder key = new StringBuilder(currencyCode);
        int count = 0;
        for (Transaccion tx : transactions) {
            if (tx == null || count >= 5) continue;
            TransactionLabelStore.Label label = labels.get(tx.getId());
            key.append('|')
                    .append(tx.getId())
                    .append(':')
                    .append(tx.getCategoriaId())
                    .append(':')
                    .append(tx.getCategoriaNombre())
                    .append(':')
                    .append(tx.isEsIngreso())
                    .append(':')
                    .append(tx.getMonto())
                    .append(':')
                    .append(tx.getMoneda())
                    .append(':')
                    .append(tx.getFecha() == null ? 0L : tx.getFecha().getTime())
                    .append(':')
                    .append(tx.getAccountType())
                    .append(':')
                    .append(tx.getNota())
                    .append(":label=")
                    .append(label == null ? "" : label.id + "," + label.name + "," + label.colorHex);
            count++;
        }
        return key.toString();
    }

    @NonNull
    private View transactionRow(@NonNull Transaccion tx, @Nullable TransactionLabelStore.Label label) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(14), dp(12), dp(12), dp(12));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(18));
        int accent = label != null ? label.colorInt() : (tx.isTransfer()
                ? color(R.color.chartAccent)
                : CategoryVisuals.colorFor(requireContext(), tx.getCategoriaNombre(), tx.isEsIngreso()));
        bg.setColor(label != null ? LabelColorUtils.cardBackground(requireContext(), accent) : color(R.color.md_theme_surface));
        bg.setStroke(dp(1), label != null ? LabelColorUtils.cardStroke(requireContext(), accent) : color(R.color.md_theme_outlineVariant));
        row.setBackground(bg);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowParams.bottomMargin = dp(10);
        row.setLayoutParams(rowParams);

        ImageView icon = new ImageView(requireContext());
        icon.setImageResource(tx.isTransfer() ? R.drawable.ic_transferencia : CategoryVisuals.iconFor(tx.getCategoriaNombre(), tx.isEsIngreso()));
        icon.setColorFilter(accent);
        GradientDrawable iconBg = new GradientDrawable();
        iconBg.setShape(GradientDrawable.OVAL);
        iconBg.setColor(LabelColorUtils.iconBackground(requireContext(), accent));
        icon.setBackground(iconBg);
        icon.setPadding(dp(9), dp(9), dp(9), dp(9));
        row.addView(icon, new LinearLayout.LayoutParams(dp(52), dp(52)));

        LinearLayout textColumn = new LinearLayout(requireContext());
        textColumn.setOrientation(LinearLayout.VERTICAL);
        TextView title = new TextView(requireContext());
        title.setText(tx.isTransfer() ? getString(R.string.tipo_transferencia) : nonEmpty(tx.getCategoriaNombre(), getString(R.string.home_uncategorized)));
        title.setTextColor(color(R.color.md_theme_onSurface));
        title.setTextSize(16f);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        TextView subtitle = new TextView(requireContext());
        if (tx.isTransfer()) {
            subtitle.setText(Format.date(tx.getFecha()) + " " + formatTime(tx.getFecha()) + " - "
                    + SettingsService.getFinancialAccountName(requireContext(), tx.getAccountType())
                    + " -> "
                    + SettingsService.getFinancialAccountName(requireContext(), tx.getTransferDestinationAccountType()));
        } else {
            subtitle.setText(Format.date(tx.getFecha()) + " " + formatTime(tx.getFecha()) + " - " + SettingsService.getFinancialAccountName(requireContext(), tx.getAccountType()));
        }
        subtitle.setTextColor(color(R.color.md_theme_onSurfaceVariant));
        subtitle.setTextSize(13f);
        textColumn.addView(title);
        textColumn.addView(subtitle);
        if (label != null) {
            TextView labelView = new TextView(requireContext());
            labelView.setText(label.name);
            labelView.setTextColor(accent);
            labelView.setTextSize(12f);
            labelView.setPadding(dp(8), dp(2), dp(8), dp(2));
            textColumn.addView(labelView);
        }
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        textParams.leftMargin = dp(12);
        row.addView(textColumn, textParams);

        TextView amount = new TextView(requireContext());
        double shown = tx.isTransfer() ? tx.getMonto() : (tx.isEsIngreso() ? tx.getMonto() : -tx.getMonto());
        amount.setText(Format.money(shown, tx.getMoneda()));
        amount.setTextColor(color(tx.isTransfer() ? R.color.chartAccent : (tx.isEsIngreso() ? R.color.income : R.color.expense)));
        amount.setTextSize(14f);
        amount.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        amount.setSingleLine(true);
        amount.setEllipsize(TextUtils.TruncateAt.END);
        row.addView(amount);
        return row;
    }

    private void showManageCardsSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(12), dp(20), dp(22));
        root.setBackgroundColor(color(R.color.dialog_surface));

        View handle = new View(requireContext());
        GradientDrawable handleBg = new GradientDrawable();
        handleBg.setColor(ColorUtils.setAlphaComponent(color(R.color.md_theme_onSurfaceVariant), 150));
        handleBg.setCornerRadius(dp(3));
        handle.setBackground(handleBg);
        LinearLayout.LayoutParams handleParams = new LinearLayout.LayoutParams(dp(64), dp(5));
        handleParams.gravity = android.view.Gravity.CENTER_HORIZONTAL;
        handleParams.bottomMargin = dp(20);
        root.addView(handle, handleParams);

        LinearLayout header = new LinearLayout(requireContext());
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(android.view.Gravity.CENTER_VERTICAL);
        TextView title = new TextView(requireContext());
        title.setText("Gestionar tarjetas");
        title.setTextColor(color(R.color.md_theme_onSurface));
        title.setTextSize(24f);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        header.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        View close = plainIconButton(R.drawable.ic_close, color(R.color.md_theme_onSurfaceVariant), dp(42));
        close.setOnClickListener(v -> dialog.dismiss());
        header.addView(close);
        root.addView(header);

        TextView subtitle = new TextView(requireContext());
        subtitle.setText("Elige que tarjetas cuentan en tu balance y cual se muestra en Inicio.");
        subtitle.setTextColor(color(R.color.md_theme_onSurfaceVariant));
        subtitle.setTextSize(15f);
        subtitle.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subtitleParams.topMargin = dp(8);
        subtitleParams.bottomMargin = dp(16);
        root.addView(subtitle, subtitleParams);

        Map<String, Double> balances = lastSummary == null ? new HashMap<>() : accountBalanceMap(lastSummary.getAccountBalances());
        for (FinancialAccount account : SettingsService.listCardAccounts(requireContext())) {
            root.addView(manageCardRow(dialog, account, balances));
        }

        MaterialButton add = new MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        add.setText("Agregar nueva tarjeta");
        add.setAllCaps(false);
        add.setTextColor(color(R.color.income));
        add.setIconResource(R.drawable.ic_add);
        add.setIconTint(ColorStateList.valueOf(color(R.color.income)));
        add.setStrokeColor(ColorStateList.valueOf(color(R.color.income)));
        add.setStrokeWidth(dp(1));
        add.setCornerRadius(dp(16));
        add.setOnClickListener(v -> {
            dialog.dismiss();
            showCreateAccountDialog();
        });
        LinearLayout.LayoutParams addParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(58));
        addParams.topMargin = dp(14);
        root.addView(add, addParams);

        LinearLayout info = new LinearLayout(requireContext());
        info.setGravity(android.view.Gravity.CENTER_VERTICAL);
        info.setOrientation(LinearLayout.HORIZONTAL);
        ImageView infoIcon = new ImageView(requireContext());
        infoIcon.setImageResource(R.drawable.ic_info);
        infoIcon.setColorFilter(color(R.color.md_theme_onSurfaceVariant));
        info.addView(infoIcon, new LinearLayout.LayoutParams(dp(26), dp(26)));
        TextView infoText = new TextView(requireContext());
        infoText.setText("Las tarjetas activas cuentan en tu balance y una se muestra en Inicio.");
        infoText.setTextColor(color(R.color.md_theme_onSurfaceVariant));
        infoText.setTextSize(13f);
        LinearLayout.LayoutParams infoTextParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        infoTextParams.leftMargin = dp(10);
        info.addView(infoText, infoTextParams);
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        infoParams.topMargin = dp(12);
        root.addView(info, infoParams);

        dialog.setContentView(root);
        dialog.show();
    }

    private View manageCardRow(@NonNull BottomSheetDialog dialog, @NonNull FinancialAccount account, @NonNull Map<String, Double> balances) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(10), dp(10), dp(10), dp(10));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color(R.color.md_theme_surface));
        bg.setCornerRadius(dp(14));
        bg.setStroke(dp(1), color(account.isVisibleInHome() ? R.color.chartBalance : R.color.md_theme_outlineVariant));
        row.setBackground(bg);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowParams.topMargin = dp(10);
        row.setLayoutParams(rowParams);

        LinearLayout includeBox = new LinearLayout(requireContext());
        includeBox.setGravity(android.view.Gravity.CENTER);
        includeBox.setOrientation(LinearLayout.VERTICAL);
        ImageView check = new ImageView(requireContext());
        check.setImageResource(R.drawable.ic_check_circle);
        check.setColorFilter(color(account.isIncludedInTotal() ? R.color.income : R.color.md_theme_onSurfaceVariant));
        includeBox.addView(check, new LinearLayout.LayoutParams(dp(30), dp(30)));
        TextView includeText = smallLabel(account.isIncludedInTotal() ? "Incluida" : "Fuera", account.isIncludedInTotal() ? color(R.color.income) : color(R.color.md_theme_onSurfaceVariant));
        includeBox.addView(includeText);
        includeBox.setOnClickListener(v -> {
            SettingsService.setCardIncludedInTotal(requireContext(), account.getId(), !account.isIncludedInTotal());
            refreshCards(dialog);
        });
        row.addView(includeBox, new LinearLayout.LayoutParams(dp(54), ViewGroup.LayoutParams.WRAP_CONTENT));

        ImageView icon = new ImageView(requireContext());
        icon.setImageResource(R.drawable.ic_card);
        icon.setColorFilter(color(R.color.chartBalance));
        GradientDrawable iconBg = new GradientDrawable();
        iconBg.setShape(GradientDrawable.OVAL);
        iconBg.setColor(ColorUtils.setAlphaComponent(color(R.color.chartBalance), 45));
        icon.setBackground(iconBg);
        icon.setPadding(dp(8), dp(8), dp(8), dp(8));
        row.addView(icon, new LinearLayout.LayoutParams(dp(48), dp(48)));

        LinearLayout text = new LinearLayout(requireContext());
        text.setOrientation(LinearLayout.VERTICAL);
        TextView name = new TextView(requireContext());
        name.setText(compactName(account));
        name.setTextColor(color(R.color.md_theme_onSurface));
        name.setTextSize(14f);
        name.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        name.setSingleLine(true);
        name.setEllipsize(TextUtils.TruncateAt.END);
        text.addView(name);
        TextView last4 = new TextView(requireContext());
        String last4Text = maskedLast4(account);
        last4.setText(last4Text);
        last4.setTextColor(color(R.color.md_theme_onSurfaceVariant));
        last4.setTextSize(12f);
        last4.setVisibility(TextUtils.isEmpty(last4Text) ? View.GONE : View.VISIBLE);
        text.addView(last4);
        TextView amount = new TextView(requireContext());
        amount.setText(Format.money(balanceFor(account, balances), currencyCode));
        amount.setTextColor(color(R.color.chartBalance));
        amount.setTextSize(16f);
        amount.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        amount.setSingleLine(true);
        amount.setEllipsize(TextUtils.TruncateAt.END);
        text.addView(amount);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        textParams.leftMargin = dp(10);
        row.addView(text, textParams);

        LinearLayout actions = new LinearLayout(requireContext());
        actions.setGravity(android.view.Gravity.CENTER);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        if (account.isVisibleInHome()) {
            View visible = plainIconButton(R.drawable.ic_visibility, color(R.color.chartBalance), dp(30));
            actions.addView(visible);
        } else {
            View principal = circularIconButton(R.drawable.ic_swap_horiz, color(R.color.chart_pie_4), color(R.color.black), dp(34));
            principal.setOnClickListener(v -> {
                SettingsService.setVisibleCardAccount(requireContext(), account.getId());
                refreshCards(dialog);
            });
            actions.addView(principal);
        }
        View edit = plainIconButton(R.drawable.ic_edit, color(R.color.md_theme_onSurfaceVariant), dp(30));
        edit.setOnClickListener(v -> {
            dialog.dismiss();
            showEditCardSheet(account);
        });
        LinearLayout.LayoutParams editParams = new LinearLayout.LayoutParams(dp(30), dp(30));
        editParams.leftMargin = dp(4);
        actions.addView(edit, editParams);
        if (account.isUserAdded()) {
            View delete = plainIconButton(R.drawable.ic_delete, color(R.color.expense), dp(30));
            delete.setOnClickListener(v -> {
                SettingsService.deleteFinancialAccount(requireContext(), account.getId());
                refreshCards(dialog);
            });
            LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(dp(30), dp(30));
            deleteParams.leftMargin = dp(4);
            actions.addView(delete, deleteParams);
        }
        row.addView(actions, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return row;
    }

    private void refreshCards(@NonNull BottomSheetDialog dialog) {
        dialog.dismiss();
        viewModel.clearCache();
        loadSummary(true);
        cardCard.postDelayed(this::showManageCardsSheet, 120);
    }

    private void showEditCardSheet(@NonNull FinancialAccount account) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(18), dp(20), dp(22));
        root.setBackgroundColor(color(R.color.dialog_surface));

        TextView title = new TextView(requireContext());
        title.setText("Editar tarjeta");
        title.setTextColor(color(R.color.md_theme_onSurface));
        title.setTextSize(20f);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(title);

        TextInputLayout tilName = new TextInputLayout(requireContext());
        tilName.setHint("Nombre (m\u00e1x. 7 letras)");
        TextInputEditText etName = new TextInputEditText(requireContext());
        etName.setSingleLine(true);
        etName.setFilters(new InputFilter[] { new InputFilter.LengthFilter(7) });
        etName.setText(account.getName() == null || account.getName().contains("predeterminada") ? "" : compactName(account));
        tilName.addView(etName, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        nameParams.topMargin = dp(16);
        root.addView(tilName, nameParams);

        TextInputLayout tilLast4 = new TextInputLayout(requireContext());
        tilLast4.setHint("\u00daltimos 4 d\u00edgitos (opcional)");
        TextInputEditText etLast4 = new TextInputEditText(requireContext());
        etLast4.setSingleLine(true);
        etLast4.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        etLast4.setFilters(new InputFilter[] { new InputFilter.LengthFilter(4) });
        etLast4.setText(account.getLast4());
        tilLast4.addView(etLast4, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams last4Params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        last4Params.topMargin = dp(12);
        root.addView(tilLast4, last4Params);

        MaterialButton save = new MaterialButton(requireContext());
        save.setText(R.string.btn_guardar);
        save.setAllCaps(false);
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52));
        saveParams.topMargin = dp(18);
        root.addView(save, saveParams);

        MaterialButton cancel = new MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        cancel.setText("Cancelar");
        cancel.setAllCaps(false);
        LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48));
        cancelParams.topMargin = dp(8);
        root.addView(cancel, cancelParams);

        cancel.setOnClickListener(v -> dialog.dismiss());
        save.setOnClickListener(v -> {
            String name = etName.getText() == null ? "" : etName.getText().toString().trim();
            String last4 = etLast4.getText() == null ? "" : etLast4.getText().toString().trim();
            if (account.isUserAdded() && name.isEmpty()) {
                tilName.setError("Ingresa un nombre");
                return;
            }
            SettingsService.updateCardDetails(requireContext(), account.getId(), name, last4);
            dialog.dismiss();
            viewModel.clearCache();
            loadSummary(true);
            cardCard.postDelayed(this::showManageCardsSheet, 120);
        });

        dialog.setContentView(root);
        dialog.show();
    }

    private void showCreateAccountDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(18), dp(20), dp(20));
        root.setBackgroundColor(color(R.color.dialog_surface));

        TextView title = new TextView(requireContext());
        title.setText("Agregar nueva tarjeta");
        title.setTextColor(color(R.color.md_theme_onSurface));
        title.setTextSize(20f);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(title);

        TextView subtitle = new TextView(requireContext());
        subtitle.setText("Se agregar\u00e1 como origen del dinero y tendr\u00e1 su propio saldo.");
        subtitle.setTextColor(color(R.color.md_theme_onSurfaceVariant));
        subtitle.setTextSize(14f);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subtitleParams.topMargin = dp(6);
        root.addView(subtitle, subtitleParams);

        TextInputLayout tilName = new TextInputLayout(requireContext());
        tilName.setHint("Nombre (m\u00e1x. 7 letras)");
        TextInputEditText etName = new TextInputEditText(requireContext());
        etName.setSingleLine(true);
        etName.setFilters(new InputFilter[] { new InputFilter.LengthFilter(7) });
        tilName.addView(etName, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        nameParams.topMargin = dp(16);
        root.addView(tilName, nameParams);

        TextInputLayout tilLast4 = new TextInputLayout(requireContext());
        tilLast4.setHint("\u00daltimos 4 d\u00edgitos (opcional)");
        TextInputEditText etLast4 = new TextInputEditText(requireContext());
        etLast4.setSingleLine(true);
        etLast4.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        etLast4.setFilters(new InputFilter[] { new InputFilter.LengthFilter(4) });
        tilLast4.addView(etLast4, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams last4Params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        last4Params.topMargin = dp(12);
        root.addView(tilLast4, last4Params);

        TextInputLayout tilBalance = new TextInputLayout(requireContext());
        tilBalance.setHintEnabled(false);
        TextInputEditText etBalance = new TextInputEditText(requireContext());
        etBalance.setHint("Saldo inicial");
        etBalance.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        tilBalance.addView(etBalance, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams balanceParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        balanceParams.topMargin = dp(12);
        root.addView(tilBalance, balanceParams);

        TextInputLayout tilCurrency = new TextInputLayout(requireContext());
        tilCurrency.setHint("Moneda");
        MaterialAutoCompleteTextView actCurrency = new MaterialAutoCompleteTextView(requireContext());
        actCurrency.setInputType(0);
        actCurrency.setText(currencyCode, false);
        actCurrency.setAdapter(new ArrayAdapter<>(requireContext(), R.layout.item_dropdown, com.example.finanzas.util.CurrencyConverter.supportedCurrencies()));
        actCurrency.setOnClickListener(v -> actCurrency.showDropDown());
        tilCurrency.addView(actCurrency, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams currencyParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        currencyParams.topMargin = dp(12);
        root.addView(tilCurrency, currencyParams);

        com.google.android.material.button.MaterialButton save = new com.google.android.material.button.MaterialButton(requireContext());
        save.setText(R.string.btn_guardar);
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52));
        saveParams.topMargin = dp(18);
        root.addView(save, saveParams);

        com.google.android.material.button.MaterialButton cancel = new com.google.android.material.button.MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        cancel.setText("Cancelar");
        LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48));
        cancelParams.topMargin = dp(8);
        root.addView(cancel, cancelParams);

        cancel.setOnClickListener(v -> dialog.dismiss());
        save.setOnClickListener(v -> {
            String name = etName.getText() == null ? "" : etName.getText().toString().trim();
            String last4 = etLast4.getText() == null ? "" : etLast4.getText().toString().trim();
            double initial = parseAmount(etBalance.getText() == null ? "" : etBalance.getText().toString());
            String currency = com.example.finanzas.util.CurrencyConverter.normalize(actCurrency.getText() == null ? currencyCode : actCurrency.getText().toString());
            if (name.isEmpty()) {
                etName.setError("Ingresa un nombre");
                return;
            }
            save.setEnabled(false);
            AccountService.create(requireContext(), name, initial, currency, last4, new AccountService.CreateCb() {
                @Override
                public void onOk(@NonNull FinancialAccount account) {
                    if (!isAdded()) return;
                    dialog.dismiss();
                    CategoryStore.clearCache();
                    viewModel.clearCache();
                    loadSummary(true);
                    UiFormUtils.showMessage(requireView(), "Tarjeta agregada");
                }

                @Override
                public void onError(@Nullable String message) {
                    if (!isAdded()) return;
                    save.setEnabled(true);
                    UiFormUtils.showMessage(requireView(), TextUtils.isEmpty(message) ? "No se pudo crear la cuenta" : message);
                }
            });
        });
        dialog.setContentView(root);
        dialog.show();
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
                "EUR - Euro",
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
                viewModel.clearCache();
                loadSummary(true);
                maybeShowTesterThanksDialog();
            }

            @Override
            public void onFail() {
                maybeShowTesterThanksDialog();
            }
        });
    }

    private void maybeShowTesterThanksDialog() {
        if (!isAdded() || testerThanksDialogShowing || Prefs.hasSeenTesterThanks(requireContext())) return;
        testerThanksDialogShowing = true;
        View content = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_tester_thanks, null, false);
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(content)
                .create();
        View close = content.findViewById(R.id.btnTesterThanksClose);
        View accept = content.findViewById(R.id.btnTesterThanksOk);
        View.OnClickListener dismissAndRemember = v -> {
            if (isAdded()) Prefs.markTesterThanksSeen(requireContext());
            dialog.dismiss();
        };
        if (close != null) close.setOnClickListener(dismissAndRemember);
        if (accept != null) accept.setOnClickListener(dismissAndRemember);
        dialog.setOnCancelListener(d -> {
            if (isAdded()) Prefs.markTesterThanksSeen(requireContext());
        });
        dialog.setOnDismissListener(d -> testerThanksDialogShowing = false);
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    private double parseAmount(@Nullable String raw) {
        if (raw == null) return 0.0;
        String clean = raw.trim().replaceAll("[^0-9,.-]", "");
        if (clean.isEmpty()) return 0.0;
        int lastComma = clean.lastIndexOf(',');
        int lastDot = clean.lastIndexOf('.');
        if (lastComma >= 0 && lastDot >= 0) {
            clean = lastComma > lastDot ? clean.replace(".", "").replace(',', '.') : clean.replace(",", "");
        } else if (lastComma >= 0) {
            clean = clean.replace(',', '.');
        }
        try {
            return Math.max(0.0, Double.parseDouble(clean));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private String formatTime(@Nullable Date date) {
        if (date == null) return "";
        return new SimpleDateFormat("HH:mm", Locale.US).format(date);
    }

    private String nonEmpty(@Nullable String value, @NonNull String fallback) {
        String clean = value == null ? "" : value.trim();
        return clean.isEmpty() ? fallback : clean;
    }

    private int color(int resId) {
        return ContextCompat.getColor(requireContext(), resId);
    }

    private View circularIconButton(@DrawableRes int iconRes, @ColorInt int background, @ColorInt int iconColor, int size) {
        FrameLayout frame = new FrameLayout(requireContext());
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(background);
        frame.setBackground(bg);
        ImageView icon = new ImageView(requireContext());
        icon.setImageResource(iconRes);
        icon.setColorFilter(iconColor);
        frame.addView(icon, new FrameLayout.LayoutParams(Math.max(dp(22), size / 2), Math.max(dp(22), size / 2), android.view.Gravity.CENTER));
        frame.setClickable(true);
        frame.setFocusable(true);
        frame.setLayoutParams(new LinearLayout.LayoutParams(size, size));
        return frame;
    }

    private View plainIconButton(@DrawableRes int iconRes, @ColorInt int iconColor, int size) {
        FrameLayout frame = new FrameLayout(requireContext());
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(ColorUtils.setAlphaComponent(iconColor, 32));
        frame.setBackground(bg);
        ImageView icon = new ImageView(requireContext());
        icon.setImageResource(iconRes);
        icon.setColorFilter(iconColor);
        frame.addView(icon, new FrameLayout.LayoutParams(Math.max(dp(20), size / 2), Math.max(dp(20), size / 2), android.view.Gravity.CENTER));
        frame.setClickable(true);
        frame.setFocusable(true);
        frame.setLayoutParams(new LinearLayout.LayoutParams(size, size));
        return frame;
    }

    private TextView smallLabel(@NonNull String value, @ColorInt int textColor) {
        TextView text = new TextView(requireContext());
        text.setText(value);
        text.setTextColor(textColor);
        text.setTextSize(11f);
        text.setGravity(android.view.Gravity.CENTER);
        text.setMaxLines(1);
        return text;
    }

    private LinearLayout iconTextAction(@DrawableRes int iconRes, @NonNull String label, @ColorInt int accent) {
        LinearLayout box = new LinearLayout(requireContext());
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(android.view.Gravity.CENTER);
        box.setPadding(dp(3), dp(4), dp(3), dp(4));
        ImageView icon = new ImageView(requireContext());
        icon.setImageResource(iconRes);
        icon.setColorFilter(accent);
        box.addView(icon, new LinearLayout.LayoutParams(dp(26), dp(26)));
        TextView text = smallLabel(label, accent);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        textParams.topMargin = dp(2);
        box.addView(text, textParams);
        box.setClickable(true);
        box.setFocusable(true);
        return box;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}



