package com.example.finanzas.ui;

import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
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
import com.example.finanzas.ui.viewmodel.HomeViewModel;
import com.example.finanzas.util.CategoryVisuals;
import com.example.finanzas.util.Format;
import com.example.finanzas.util.FinancialAlertNotifier;
import com.example.finanzas.util.LabelColorUtils;
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
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

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
    private View cardAddAccount;
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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        swipe = view.findViewById(R.id.swipeHome);
        progress = view.findViewById(R.id.progressHome);
        tvPeriod = view.findViewById(R.id.tvHomePeriod);
        tvBalanceTotal = view.findViewById(R.id.tvHomeBalanceTotal);
        cardIncome = view.findViewById(R.id.cardIncome);
        cardExpense = view.findViewById(R.id.cardExpense);
        cardCash = view.findViewById(R.id.cardCash);
        cardCard = view.findViewById(R.id.cardCard);
        btnSettings = view.findViewById(R.id.btnHomeSettings);
        cardAddAccount = view.findViewById(R.id.cardAddAccount);
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

    private void setupNavigation(@NonNull View root) {
        btnSettings.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.nav_settings));
        cardAddAccount.setOnClickListener(v -> showCreateAccountDialog());
        btnSeeTransactions.setOnClickListener(v -> Navigation.findNavController(root).navigate(R.id.nav_list));
    }

    private void observeViewModel() {
        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            boolean isLoading = Boolean.TRUE.equals(loading);
            progress.setVisibility(isLoading && lastSummary == null ? View.VISIBLE : View.GONE);
            swipe.setRefreshing(isLoading && lastSummary != null);
        });
        viewModel.getCurrencyCode().observe(getViewLifecycleOwner(), code -> {
            if (code != null && !code.trim().isEmpty()) currencyCode = code;
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
        FinancialAlertNotifier.maybeNotifyImportantAlert(requireContext(), summary, currencyCode);
    }

    private void styleMetric(@NonNull View card, int titleRes, @NonNull String value, @DrawableRes int iconRes, @ColorInt int accent) {
        TextView title = card.findViewById(R.id.metricTitle);
        TextView amount = card.findViewById(R.id.metricValue);
        ImageView icon = card.findViewById(R.id.metricIcon);
        FrameLayout iconBg = card.findViewById(R.id.metricIconBg);
        title.setText(titleRes);
        amount.setText(value);
        amount.setTextColor(accent);
        icon.setImageResource(iconRes);
        icon.setColorFilter(accent);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(ColorUtils.setAlphaComponent(accent, 45));
        iconBg.setBackground(bg);
    }

    private void renderAccounts(@Nullable List<AccountBalance> balances) {
        listAccounts.removeAllViews();
        hasDynamicAccounts = false;
        listAccounts.setVisibility(View.GONE);
    }

    private void renderCardMetric(@NonNull HomeSummary summary) {
        if (!(cardCard instanceof MaterialCardView)) return;
        MaterialCardView card = (MaterialCardView) cardCard;
        card.removeAllViews();
        card.setCardBackgroundColor(color(R.color.md_theme_surface));
        card.setStrokeColor(color(R.color.md_theme_outlineVariant));
        card.setStrokeWidth(dp(1));
        card.setRadius(dp(8));

        Map<String, Double> balances = accountBalanceMap(summary.getAccountBalances());
        List<FinancialAccount> cards = SettingsService.listCardAccounts(requireContext());
        List<FinancialAccount> included = new java.util.ArrayList<>();
        for (FinancialAccount account : cards) {
            if (account.isIncludedInTotal()) included.add(account);
        }
        if (included.isEmpty() && !cards.isEmpty()) included.add(SettingsService.getVisibleCardAccount(requireContext()));
        boolean summaryMode = included.size() > 1;
        FinancialAccount visible = summaryMode ? null : (included.isEmpty() ? SettingsService.getVisibleCardAccount(requireContext()) : included.get(0));

        LinearLayout body = new LinearLayout(requireContext());
        body.setOrientation(LinearLayout.HORIZONTAL);
        body.setGravity(android.view.Gravity.CENTER_VERTICAL);
        body.setPadding(dp(16), dp(12), dp(12), dp(12));
        card.addView(body, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        ImageView icon = new ImageView(requireContext());
        icon.setImageResource(R.drawable.ic_card);
        icon.setColorFilter(color(R.color.chartBalance));
        GradientDrawable iconBg = new GradientDrawable();
        iconBg.setShape(GradientDrawable.OVAL);
        iconBg.setColor(ColorUtils.setAlphaComponent(color(R.color.chartBalance), 45));
        icon.setBackground(iconBg);
        icon.setPadding(dp(10), dp(10), dp(10), dp(10));
        body.addView(icon, new LinearLayout.LayoutParams(dp(52), dp(52)));

        LinearLayout text = new LinearLayout(requireContext());
        text.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        textParams.leftMargin = dp(14);
        body.addView(text, textParams);

        TextView title = new TextView(requireContext());
        title.setText(summaryMode ? "Tarjetas" : compactName(visible));
        title.setTextColor(color(R.color.md_theme_onSurface));
        title.setTextSize(summaryMode ? 16f : 14f);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        text.addView(title);

        TextView subtitle = new TextView(requireContext());
        subtitle.setText(summaryMode
                ? getString(R.string.home_cards_selected_count, included.size())
                : maskedLast4(visible));
        subtitle.setTextColor(color(R.color.md_theme_onSurfaceVariant));
        subtitle.setTextSize(13f);
        subtitle.setSingleLine(true);
        subtitle.setEllipsize(TextUtils.TruncateAt.END);
        text.addView(subtitle);

        double amountValue = summaryMode ? sumBalances(included, balances) : balanceFor(visible, balances);
        TextView amount = new TextView(requireContext());
        amount.setText(Format.money(amountValue, currencyCode));
        amount.setTextColor(color(R.color.chartBalance));
        amount.setTextSize(22f);
        amount.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        amount.setSingleLine(true);
        amount.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams amountParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        amountParams.topMargin = dp(3);
        text.addView(amount, amountParams);

        LinearLayout actions = new LinearLayout(requireContext());
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(android.view.Gravity.CENTER);
        body.addView(actions, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        if (cards.size() <= 1) {
            View add = circularIconButton(R.drawable.ic_add, color(R.color.income), color(R.color.md_theme_onPrimary), dp(42));
            add.setOnClickListener(v -> showManageCardsSheet());
            actions.addView(add);
        } else if (summaryMode) {
            View tune = plainIconButton(R.drawable.ic_tune, color(R.color.chartBalance), dp(38));
            tune.setOnClickListener(v -> showManageCardsSheet());
            actions.addView(tune);
        } else {
            View visibleBtn = plainIconButton(R.drawable.ic_visibility, color(R.color.md_theme_onSurfaceVariant), dp(36));
            visibleBtn.setOnClickListener(v -> showManageCardsSheet());
            actions.addView(visibleBtn);
            View swap = circularIconButton(R.drawable.ic_swap_horiz, color(R.color.risk_medium_text), color(R.color.md_theme_onPrimary), dp(38));
            swap.setOnClickListener(v -> showManageCardsSheet());
            LinearLayout.LayoutParams swapParams = new LinearLayout.LayoutParams(dp(38), dp(38));
            swapParams.leftMargin = dp(8);
            actions.addView(swap, swapParams);
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
        return name;
    }

    private String maskedLast4(@Nullable FinancialAccount account) {
        String last4 = account == null || TextUtils.isEmpty(account.getLast4()) ? "4242" : account.getLast4();
        return "\u2022\u2022\u2022\u2022 " + last4;
    }

    private void renderAnimatedBalance(double newBalance) {
        if (tvBalanceTotal == null) return;
        if (lastRenderedBalance == null || Math.abs(lastRenderedBalance - newBalance) < 0.005) {
            tvBalanceTotal.setText(Format.money(newBalance, currencyCode));
            lastRenderedBalance = newBalance;
            return;
        }
        double from = lastRenderedBalance;
        int defaultColor = color(R.color.md_theme_onSurface);
        int pulseColor = newBalance > from ? color(R.color.income) : color(R.color.expense);
        ValueAnimator animator = ValueAnimator.ofFloat((float) from, (float) newBalance);
        animator.setDuration(650L);
        animator.addUpdateListener(animation -> {
            double value = ((Float) animation.getAnimatedValue()).doubleValue();
            tvBalanceTotal.setText(Format.money(value, currencyCode));
            tvBalanceTotal.setTextColor(pulseColor);
        });
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                tvBalanceTotal.setText(Format.money(newBalance, currencyCode));
                tvBalanceTotal.setTextColor(defaultColor);
                lastRenderedBalance = newBalance;
            }
        });
        animator.start();
    }

    private void renderBudget(@NonNull HomeSummary summary) {
        boolean hasBudget = summary.getPresupuestoMonto() > 0;
        tvBudgetMissing.setVisibility(hasBudget ? View.GONE : View.VISIBLE);
        cardBudget.setVisibility(hasBudget ? View.VISIBLE : View.GONE);
        if (!hasBudget) return;
        int percent = (int) Math.max(0, Math.min(999, Math.round(summary.getPresupuestoPorcentaje())));
        tvBudgetPercent.setText(getString(R.string.home_budget_used_value, percent));
        String top = summary.getCategoriaMayorGasto() == null || summary.getCategoriaMayorGastoMonto() <= 0
                ? getString(R.string.home_top_category_empty)
                : getString(R.string.home_top_category_value, summary.getCategoriaMayorGasto(), Format.money(summary.getCategoriaMayorGastoMonto(), currencyCode));
        tvBudgetTop.setText(top);
        progressBudget.setProgressCompat(Math.min(100, percent), true);
    }

    private void renderLatest(@Nullable List<Transaccion> transactions) {
        listLatest.removeAllViews();
        boolean empty = transactions == null || transactions.isEmpty();
        tvLatestEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (empty) return;
        Map<Integer, TransactionLabelStore.Label> labels = TransactionLabelStore.assignedLabelDetails(requireContext());
        int count = 0;
        for (Transaccion tx : transactions) {
            if (tx == null || count >= 5) continue;
            listLatest.addView(transactionRow(tx, labels.get(tx.getId())));
            count++;
        }
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
        subtitle.setText("Selecciona las tarjetas que quieres incluir en el total y elige cuál mostrar en el inicio.");
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
        infoText.setText("Las tarjetas seleccionadas se incluirán en tu balance total.\nSolo una tarjeta puede mostrarse en inicio.");
        infoText.setTextColor(color(R.color.md_theme_onSurfaceVariant));
        infoText.setTextSize(14f);
        LinearLayout.LayoutParams infoTextParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        infoTextParams.leftMargin = dp(14);
        info.addView(infoText, infoTextParams);
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        infoParams.topMargin = dp(18);
        root.addView(info, infoParams);

        dialog.setContentView(root);
        dialog.show();
    }

    private View manageCardRow(@NonNull BottomSheetDialog dialog, @NonNull FinancialAccount account, @NonNull Map<String, Double> balances) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(12), dp(12), dp(12), dp(12));
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
        includeBox.addView(check, new LinearLayout.LayoutParams(dp(36), dp(36)));
        TextView includeText = smallLabel(account.isIncludedInTotal() ? "Incluida" : "Fuera", account.isIncludedInTotal() ? color(R.color.income) : color(R.color.md_theme_onSurfaceVariant));
        includeBox.addView(includeText);
        includeBox.setOnClickListener(v -> {
            SettingsService.setCardIncludedInTotal(requireContext(), account.getId(), !account.isIncludedInTotal());
            refreshCards(dialog);
        });
        row.addView(includeBox, new LinearLayout.LayoutParams(dp(70), ViewGroup.LayoutParams.WRAP_CONTENT));

        ImageView icon = new ImageView(requireContext());
        icon.setImageResource(R.drawable.ic_card);
        icon.setColorFilter(color(R.color.chartBalance));
        GradientDrawable iconBg = new GradientDrawable();
        iconBg.setShape(GradientDrawable.OVAL);
        iconBg.setColor(ColorUtils.setAlphaComponent(color(R.color.chartBalance), 45));
        icon.setBackground(iconBg);
        icon.setPadding(dp(10), dp(10), dp(10), dp(10));
        row.addView(icon, new LinearLayout.LayoutParams(dp(58), dp(58)));

        LinearLayout text = new LinearLayout(requireContext());
        text.setOrientation(LinearLayout.VERTICAL);
        TextView name = new TextView(requireContext());
        name.setText(account.getName());
        name.setTextColor(color(R.color.md_theme_onSurface));
        name.setTextSize(16f);
        name.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        text.addView(name);
        TextView last4 = new TextView(requireContext());
        last4.setText(maskedLast4(account));
        last4.setTextColor(color(R.color.md_theme_onSurfaceVariant));
        last4.setTextSize(14f);
        text.addView(last4);
        TextView amount = new TextView(requireContext());
        amount.setText(Format.money(balanceFor(account, balances), currencyCode));
        amount.setTextColor(color(R.color.chartBalance));
        amount.setTextSize(18f);
        amount.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        text.addView(amount);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        textParams.leftMargin = dp(14);
        row.addView(text, textParams);

        LinearLayout actions = new LinearLayout(requireContext());
        actions.setGravity(android.view.Gravity.CENTER);
        actions.setOrientation(LinearLayout.VERTICAL);
        if (account.isVisibleInHome()) {
            LinearLayout visible = iconTextAction(R.drawable.ic_visibility, "Visible", color(R.color.chartBalance));
            actions.addView(visible);
        } else {
            LinearLayout principal = iconTextAction(R.drawable.ic_swap_horiz, "Principal", color(R.color.risk_medium_text));
            principal.setOnClickListener(v -> {
                SettingsService.setVisibleCardAccount(requireContext(), account.getId());
                refreshCards(dialog);
            });
            actions.addView(principal);
        }
        if (account.isUserAdded()) {
            LinearLayout delete = iconTextAction(R.drawable.ic_delete, "Eliminar", color(R.color.expense));
            delete.setOnClickListener(v -> {
                SettingsService.deleteFinancialAccount(requireContext(), account.getId());
                refreshCards(dialog);
            });
            actions.addView(delete);
        }
        row.addView(actions, new LinearLayout.LayoutParams(dp(86), ViewGroup.LayoutParams.WRAP_CONTENT));
        return row;
    }

    private void refreshCards(@NonNull BottomSheetDialog dialog) {
        dialog.dismiss();
        viewModel.clearCache();
        loadSummary(true);
        cardCard.postDelayed(this::showManageCardsSheet, 120);
    }

    private void showCreateAccountDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(18), dp(20), dp(20));

        TextView title = new TextView(requireContext());
        title.setText("Agregar cuenta bancaria o tarjeta");
        title.setTextColor(color(R.color.md_theme_onSurface));
        title.setTextSize(20f);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(title);

        TextView subtitle = new TextView(requireContext());
        subtitle.setText("Se agregara como origen del dinero y tendra su propio saldo.");
        subtitle.setTextColor(color(R.color.md_theme_onSurfaceVariant));
        subtitle.setTextSize(14f);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subtitleParams.topMargin = dp(6);
        root.addView(subtitle, subtitleParams);

        TextInputLayout tilName = new TextInputLayout(requireContext());
        tilName.setHint("Nombre de la cuenta o tarjeta");
        TextInputEditText etName = new TextInputEditText(requireContext());
        etName.setSingleLine(true);
        tilName.addView(etName, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        nameParams.topMargin = dp(16);
        root.addView(tilName, nameParams);

        TextInputLayout tilBalance = new TextInputLayout(requireContext());
        tilBalance.setHint("Saldo inicial");
        TextInputEditText etBalance = new TextInputEditText(requireContext());
        etBalance.setHint("0.00");
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
        cancel.setText(android.R.string.cancel);
        LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48));
        cancelParams.topMargin = dp(8);
        root.addView(cancel, cancelParams);

        cancel.setOnClickListener(v -> dialog.dismiss());
        save.setOnClickListener(v -> {
            String name = etName.getText() == null ? "" : etName.getText().toString().trim();
            double initial = parseAmount(etBalance.getText() == null ? "" : etBalance.getText().toString());
            String currency = com.example.finanzas.util.CurrencyConverter.normalize(actCurrency.getText() == null ? currencyCode : actCurrency.getText().toString());
            if (name.isEmpty()) {
                etName.setError("Ingresa un nombre");
                return;
            }
            save.setEnabled(false);
            AccountService.create(requireContext(), name, initial, currency, new AccountService.CreateCb() {
                @Override
                public void onOk(@NonNull FinancialAccount account) {
                    if (!isAdded()) return;
                    dialog.dismiss();
                    CategoryStore.clearCache();
                    viewModel.clearCache();
                    loadSummary(true);
                    UiFormUtils.showMessage(requireView(), "Cuenta agregada");
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
                .setPositiveButton(R.string.tester_thanks_action, (d, which) -> {
                    if (isAdded()) Prefs.markTesterThanksSeen(requireContext());
                })
                .create();
        dialog.setOnCancelListener(d -> {
            if (isAdded()) Prefs.markTesterThanksSeen(requireContext());
        });
        dialog.setOnDismissListener(d -> testerThanksDialogShowing = false);
        dialog.show();
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
