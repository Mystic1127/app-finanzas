package com.example.finanzas.ui;

import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HomeFragment extends Fragment {
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipe;
    private CircularProgressIndicator progress;
    private TextView tvPeriod;
    private TextView tvBalanceTotal;
    private View cardIncome;
    private View cardExpense;
    private View cardCash;
    private View cardCard;
    private View btnCustomize;
    private View btnSettings;
    private View cardAddAccount;
    private GridLayout listAccounts;
    private TextView tvBudgetMissing;
    private MaterialCardView cardBudget;
    private TextView tvBudgetPercent;
    private TextView tvBudgetTop;
    private LinearProgressIndicator progressBudget;
    private TextView btnSeeTransactions;
    private LinearLayout listLatest;
    private TextView tvLatestEmpty;
    private HomeViewModel viewModel;
    private String currencyCode = "PEN";
    private HomeSummary lastSummary;
    private boolean showAccounts = true;
    private boolean showBudget = true;
    private boolean showLatest = true;
    private boolean hasDynamicAccounts;
    private boolean testerThanksDialogShowing;

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
        btnCustomize = view.findViewById(R.id.btnCustomizeHome);
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

        loadHomePrefs();
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
        btnCustomize.setOnClickListener(v -> showHomeCustomizeDialog());
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
        tvBalanceTotal.setText(Format.money(summary.getSaldoActualTotal(), currencyCode));
        styleMetric(cardIncome, R.string.home_ingresos, Format.money(summary.getIngresos(), currencyCode), R.drawable.ic_income, color(R.color.income));
        styleMetric(cardExpense, R.string.home_gastos, Format.money(summary.getGastos(), currencyCode), R.drawable.ic_expense, color(R.color.expense));
        styleMetric(cardCash, R.string.home_cash, Format.money(summary.getEfectivo(), currencyCode), R.drawable.ic_cash, color(R.color.income));
        styleMetric(cardCard, R.string.home_card_account, Format.money(summary.getTarjetaCuenta(), currencyCode), R.drawable.ic_card, color(R.color.chartBalance));
        renderAccounts(summary.getAccountBalances());
        renderBudget(summary);
        renderLatest(summary.getLatestTransactions());
        applyHomePrefs();
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
        if (balances == null || balances.isEmpty()) {
            listAccounts.setVisibility(View.GONE);
            return;
        }
        for (AccountBalance account : balances) {
            if (account == null) continue;
            if (account.isPrimary()) continue;
            hasDynamicAccounts = true;
            MaterialCardView card = new MaterialCardView(requireContext());
            card.setCardBackgroundColor(color(R.color.md_theme_surface));
            card.setStrokeColor(color(R.color.md_theme_outlineVariant));
            card.setStrokeWidth(dp(1));
            card.setRadius(dp(8));
            card.setUseCompatPadding(false);

            LinearLayout body = new LinearLayout(requireContext());
            body.setOrientation(LinearLayout.VERTICAL);
            body.setGravity(android.view.Gravity.CENTER_VERTICAL);
            body.setPadding(dp(14), dp(10), dp(14), dp(10));
            TextView name = new TextView(requireContext());
            name.setText(account.getName());
            name.setTextColor(color(R.color.md_theme_onSurfaceVariant));
            name.setTextSize(13f);
            name.setSingleLine(true);
            name.setEllipsize(TextUtils.TruncateAt.END);
            TextView amount = new TextView(requireContext());
            amount.setText(Format.money(account.getBalance(), currencyCode));
            amount.setTextColor(color(account.isPrimary() ? R.color.chartBalance : R.color.md_theme_primary));
            amount.setTextSize(18f);
            amount.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            amount.setMaxLines(2);
            TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(amount, 12, 18, 1, android.util.TypedValue.COMPLEX_UNIT_SP);
            body.addView(name);
            body.addView(amount);
            card.addView(body);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = dp(86);
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(dp(4), dp(4), dp(4), dp(4));
            listAccounts.addView(card, params);
        }
        listAccounts.setVisibility(showAccounts && hasDynamicAccounts ? View.VISIBLE : View.GONE);
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
        int accent = label != null ? label.colorInt() : CategoryVisuals.colorFor(requireContext(), tx.getCategoriaNombre(), tx.isEsIngreso());
        bg.setColor(label != null ? LabelColorUtils.cardBackground(requireContext(), accent) : color(R.color.md_theme_surface));
        bg.setStroke(dp(1), label != null ? LabelColorUtils.cardStroke(requireContext(), accent) : color(R.color.md_theme_outlineVariant));
        row.setBackground(bg);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowParams.bottomMargin = dp(10);
        row.setLayoutParams(rowParams);

        ImageView icon = new ImageView(requireContext());
        icon.setImageResource(CategoryVisuals.iconFor(tx.getCategoriaNombre(), tx.isEsIngreso()));
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
        title.setText(nonEmpty(tx.getCategoriaNombre(), getString(R.string.home_uncategorized)));
        title.setTextColor(color(R.color.md_theme_onSurface));
        title.setTextSize(16f);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        TextView subtitle = new TextView(requireContext());
        subtitle.setText(Format.date(tx.getFecha()) + " " + formatTime(tx.getFecha()) + " - " + SettingsService.getFinancialAccountName(requireContext(), tx.getAccountType()));
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
        double shown = tx.isEsIngreso() ? tx.getMonto() : -tx.getMonto();
        amount.setText(Format.money(shown, tx.getMoneda()));
        amount.setTextColor(color(tx.isEsIngreso() ? R.color.income : R.color.expense));
        amount.setTextSize(14f);
        amount.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        amount.setSingleLine(true);
        amount.setEllipsize(TextUtils.TruncateAt.END);
        row.addView(amount);
        return row;
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

    private void showHomeCustomizeDialog() {
        String[] labels = new String[] { "Cuentas y tarjetas", "Presupuesto", "Ultimas transacciones" };
        boolean[] checked = new boolean[] { showAccounts, showBudget, showLatest };
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dashboard_customize_title)
                .setMultiChoiceItems(labels, checked, (dialog, which, isChecked) -> checked[which] = isChecked)
                .setPositiveButton(R.string.dashboard_customize_save, (dialog, which) -> {
                    showAccounts = checked[0];
                    showBudget = checked[1];
                    showLatest = checked[2];
                    saveHomePrefs();
                    applyHomePrefs();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void loadHomePrefs() {
        try {
            JSONObject body = new JSONObject(SettingsService.getDashboardRaw(requireContext()));
            showAccounts = body.optBoolean("home_accounts", true);
            showBudget = body.optBoolean("home_budget", true);
            showLatest = body.optBoolean("home_latest", true);
        } catch (Exception ignored) {
            showAccounts = true;
            showBudget = true;
            showLatest = true;
        }
    }

    private void saveHomePrefs() {
        try {
            JSONObject body = new JSONObject(SettingsService.getDashboardRaw(requireContext()));
            body.put("home_accounts", showAccounts);
            body.put("home_budget", showBudget);
            body.put("home_latest", showLatest);
            SettingsService.saveDashboard(requireContext(), body, new SettingsService.SaveCb() {
                @Override public void onSuccess() { if (isAdded()) UiFormUtils.showMessage(requireView(), R.string.dashboard_customize_saved); }
                @Override public void onFail() { if (isAdded()) UiFormUtils.showMessage(requireView(), R.string.dashboard_customize_error); }
            });
        } catch (Exception e) {
            UiFormUtils.showMessage(requireView(), R.string.dashboard_customize_error);
        }
    }

    private void applyHomePrefs() {
        int accountVisibility = showAccounts ? View.VISIBLE : View.GONE;
        cardAddAccount.setVisibility(accountVisibility);
        listAccounts.setVisibility(showAccounts && hasDynamicAccounts ? View.VISIBLE : View.GONE);
        tvBudgetMissing.setVisibility(showBudget ? tvBudgetMissing.getVisibility() : View.GONE);
        cardBudget.setVisibility(showBudget ? cardBudget.getVisibility() : View.GONE);
        listLatest.setVisibility(showLatest ? View.VISIBLE : View.GONE);
        tvLatestEmpty.setVisibility(showLatest ? tvLatestEmpty.getVisibility() : View.GONE);
        btnSeeTransactions.setVisibility(showLatest ? View.VISIBLE : View.GONE);
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

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
