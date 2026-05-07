package com.example.finanzas.ui;

import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.finanzas.R;
import com.example.finanzas.data.api.CategoryStore;
import com.example.finanzas.data.api.AccountService;
import com.example.finanzas.data.api.SettingsService;
import com.example.finanzas.data.api.TransService;
import com.example.finanzas.data.model.Categoria;
import com.example.finanzas.data.model.FinancialAccount;
import com.example.finanzas.data.model.Transaccion;
import com.example.finanzas.data.local.room.RecurringTransactionEntity;
import com.example.finanzas.util.CategoryVisuals;
import com.example.finanzas.util.CurrencyConverter;
import com.example.finanzas.util.LabelColorUtils;
import com.example.finanzas.util.RecurringTransactionStore;
import com.example.finanzas.util.TransactionLabelStore;
import com.example.finanzas.util.UiFormUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NuevaTransaccionFragment extends Fragment {

    public static final String EXTRA_ID         = "EXTRA_ID";
    public static final String EXTRA_CAT_ID     = "EXTRA_CAT_ID";
    public static final String EXTRA_CAT_NOMBRE = "EXTRA_CAT_NOMBRE";
    public static final String EXTRA_ES_INGRESO = "EXTRA_ES_INGRESO";
    public static final String EXTRA_MONTO      = "EXTRA_MONTO";
    public static final String EXTRA_NOTA       = "EXTRA_NOTA";
    public static final String EXTRA_FECHA      = "EXTRA_FECHA";
    public static final String EXTRA_MONEDA     = "EXTRA_MONEDA";
    public static final String EXTRA_ACCOUNT_TYPE = "EXTRA_ACCOUNT_TYPE";
    public static final String EXTRA_IS_TRANSFER = "EXTRA_IS_TRANSFER";

    private EditText etMonto;
    private TextInputEditText etNota, etFecha, etHora;
    private TextInputLayout tilFecha, tilHora, tilCategoria, tilMoneda;
    private MaterialButtonToggleGroup toggleTipo, toggleAccountType;
    private MaterialButton btnTipoGasto, btnTipoTransferencia, btnTipoIngreso;
    private MaterialButton btnSwitchCard, btnSwitchDestinationCard;
    private TextInputEditText actCategoria;
    private MaterialAutoCompleteTextView actMoneda, actRecurrence;
    private MaterialButtonToggleGroup toggleDestinationAccount;
    private TextView tvTransferHint;
    private View scrollTransferDestination;
    private TextInputLayout tilEtiquetaVisual;
    private TextInputEditText etEtiquetaVisual;
    private View layoutCustomRecurrenceDays;
    private ChipGroup chipCustomRecurrenceDays;
    private MaterialButton btnClearRecurrence;
    private MaterialButton btnGuardar;
    private View layoutMoreOptions;
    private TextView tvMontoCurrency, tvMontoError;
    private boolean moreOptionsExpanded = false;
    private boolean saveButtonExpandTouch = false;

    private Integer editingId = null;
    private List<Categoria> categorias;
    private List<Categoria> visibles;
    private final Map<Integer, String> accountTypesByButtonId = new HashMap<>();
    private final Map<Integer, String> destinationTypesByButtonId = new HashMap<>();
    private String selectedAccountType = "CARD";
    private String selectedDestinationAccountType = "CASH";
    private String selectedLabelId = null;
    private List<TransactionLabelStore.Label> transactionLabels = new ArrayList<>();
    private final Map<Integer, Integer> recurrenceDayByChipId = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_nueva_transaccion, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        etMonto       = v.findViewById(R.id.etMonto);
        tvMontoCurrency = v.findViewById(R.id.tvMontoCurrency);
        tvMontoError = v.findViewById(R.id.tvMontoError);
        tilFecha      = v.findViewById(R.id.tilFecha);
        tilHora       = v.findViewById(R.id.tilHora);
        tilCategoria  = v.findViewById(R.id.tilCategoria);
        tilMoneda     = v.findViewById(R.id.tilMoneda);
        etNota        = v.findViewById(R.id.etNota);
        etFecha       = v.findViewById(R.id.etFecha);
        etHora        = v.findViewById(R.id.etHora);
        toggleTipo    = v.findViewById(R.id.toggleTipo);
        toggleAccountType = v.findViewById(R.id.toggleAccountType);
        toggleDestinationAccount = v.findViewById(R.id.toggleDestinationAccount);
        tvTransferHint = v.findViewById(R.id.tvTransferHint);
        scrollTransferDestination = v.findViewById(R.id.scrollTransferDestination);
        btnTipoGasto  = v.findViewById(R.id.btnTipoGasto);
        btnSwitchCard = v.findViewById(R.id.btnSwitchCard);
        btnSwitchDestinationCard = v.findViewById(R.id.btnSwitchDestinationCard);
        btnTipoTransferencia = v.findViewById(R.id.btnTipoTransferencia);
        btnTipoIngreso = v.findViewById(R.id.btnTipoIngreso);
        actCategoria  = v.findViewById(R.id.actCategoria);
        actMoneda     = v.findViewById(R.id.actMoneda);
        actRecurrence = v.findViewById(R.id.actRecurrence);
        tilEtiquetaVisual = v.findViewById(R.id.tilEtiquetaVisual);
        etEtiquetaVisual = v.findViewById(R.id.etEtiquetaVisual);
        layoutCustomRecurrenceDays = v.findViewById(R.id.layoutCustomRecurrenceDays);
        chipCustomRecurrenceDays = v.findViewById(R.id.chipCustomRecurrenceDays);
        btnClearRecurrence = v.findViewById(R.id.btnClearRecurrence);
        btnGuardar    = v.findViewById(R.id.btnGuardar);
        layoutMoreOptions = v.findViewById(R.id.layoutMoreOptions);

        setupTransactionTypeSelector();
        setupCurrencySelector();
        setupAccountTypeSelector();
        setupDestinationAccountSelector();
        setupRecurrenceSelector();
        setupLabelSelector();
        setMoreOptionsExpanded(false);
        updateCurrencyPrefix();

        if (etFecha != null) UiFormUtils.bindDatePicker(requireContext(), etFecha);
        if (etHora != null) UiFormUtils.bindTimePicker(requireContext(), etHora);
        bindMontoErrorCleaner();
        UiFormUtils.clearErrorOnTextChange(etFecha, etHora, actCategoria);

        actCategoria.setOnClickListener(view -> showCategoryPickerSheet());
        tilCategoria.setEndIconOnClickListener(view -> showCategoryPickerSheet());

        precargarDesdeArgs();
        cargarCategoriasYRefrescar();

        if (etFecha != null && (etFecha.getText() == null || TextUtils.isEmpty(etFecha.getText().toString()))) {
            etFecha.setText(UiFormUtils.formatUiDate(new Date()));
        }
        if (etHora != null && (etHora.getText() == null || TextUtils.isEmpty(etHora.getText().toString()))) {
            etHora.setText(formatTime(new Date()));
        }

        btnGuardar.setOnTouchListener((button, event) -> {
            int expandArea = dp(64);
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                saveButtonExpandTouch = event.getX() >= button.getWidth() - expandArea;
                return saveButtonExpandTouch;
            }
            if (saveButtonExpandTouch && event.getAction() == MotionEvent.ACTION_UP) {
                setMoreOptionsExpanded(!moreOptionsExpanded);
                saveButtonExpandTouch = false;
                return true;
            }
            if (event.getAction() == MotionEvent.ACTION_CANCEL) {
                saveButtonExpandTouch = false;
            }
            return false;
        });
        btnGuardar.setOnClickListener(this::onGuardar);
        if (btnSwitchCard != null) btnSwitchCard.setOnClickListener(v16 -> showSelectCardSheet());
        if (btnSwitchDestinationCard != null) btnSwitchDestinationCard.setOnClickListener(v16 -> showSelectDestinationCardSheet());
    }

    @Override
    public void onResume() {
        super.onResume();
        updateCurrencyPrefix();
        populateAccountTypeButtons(selectedAccountType);
        populateDestinationAccountButtons(selectedDestinationAccountType);
        loadLabels();
    }

    private void updateCurrencyPrefix() {
        if (tvMontoCurrency != null && getContext() != null) {
            String symbol = SettingsService.getCurrencySymbol(resolveSelectedCurrency());
            tvMontoCurrency.setText(TextUtils.isEmpty(symbol) ? "" : symbol.trim());
        }
    }

    private void bindMontoErrorCleaner() {
        if (etMonto == null) return;
        etMonto.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                setMontoError(null);
            }
            @Override public void afterTextChanged(Editable s) { }
        });
    }

    private void setMontoError(@Nullable String message) {
        if (tvMontoError == null) return;
        boolean hasError = !TextUtils.isEmpty(message);
        tvMontoError.setText(hasError ? message : "");
        tvMontoError.setVisibility(hasError ? View.VISIBLE : View.GONE);
        if (!hasError && etMonto != null) etMonto.setError(null);
    }

    private void setupCurrencySelector() {
        ArrayAdapter<String> currencyAdapter = new ArrayAdapter<>(
                requireContext(),
                R.layout.item_dropdown,
                CurrencyConverter.supportedCurrencies()
        );
        actMoneda.setAdapter(currencyAdapter);
        actMoneda.setText(SettingsService.getCurrencyCode(requireContext()), false);
        actMoneda.setOnFocusChangeListener((view, hasFocus) -> { if (hasFocus) actMoneda.showDropDown(); });
        actMoneda.setOnClickListener(view -> actMoneda.showDropDown());
        actMoneda.setOnItemClickListener((parent, view, position, id) -> updateCurrencyPrefix());
    }

    private void setupTransactionTypeSelector() {
        if (toggleTipo == null) return;
        toggleTipo.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            updateTransactionTypeUi();
            if (!isTransferSelected()) {
                aplicarFiltroYRefrescar(isIncomeSelected(), false, null);
            } else if (actCategoria != null) {
                actCategoria.setText(Transaccion.TRANSFER_CATEGORY);
                tilCategoria.setError(null);
            }
        });
        setSelectedTransactionType(false);
    }

    private boolean isIncomeSelected() {
        return toggleTipo != null && toggleTipo.getCheckedButtonId() == R.id.btnTipoIngreso;
    }

    private boolean isTransferSelected() {
        return toggleTipo != null && toggleTipo.getCheckedButtonId() == R.id.btnTipoTransferencia;
    }

    private void setSelectedTransactionType(boolean income) {
        if (toggleTipo != null) {
            toggleTipo.check(income ? R.id.btnTipoIngreso : R.id.btnTipoGasto);
        }
        updateTransactionTypeUi();
    }

    private void updateTransactionTypeUi() {
        styleToggleButton(btnTipoGasto, !isIncomeSelected() && !isTransferSelected());
        styleToggleButton(btnTipoTransferencia, isTransferSelected());
        styleToggleButton(btnTipoIngreso, isIncomeSelected());
        boolean transfer = isTransferSelected();
        if (tilCategoria != null) tilCategoria.setVisibility(transfer ? View.GONE : View.VISIBLE);
        if (tvTransferHint != null) tvTransferHint.setVisibility(transfer ? View.VISIBLE : View.GONE);
        if (scrollTransferDestination != null) scrollTransferDestination.setVisibility(transfer ? View.VISIBLE : View.GONE);
        if (btnSwitchDestinationCard != null) btnSwitchDestinationCard.setVisibility(transfer ? View.VISIBLE : View.GONE);
        updateDestinationAccountUi();
    }

    private String resolveSelectedCurrency() {
        String raw = actMoneda == null || actMoneda.getText() == null
                ? SettingsService.getCurrencyCode(requireContext())
                : actMoneda.getText().toString();
        return CurrencyConverter.normalize(raw);
    }

    private void setupAccountTypeSelector() {
        if (toggleAccountType == null) return;
        toggleAccountType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            String type = accountTypesByButtonId.get(checkedId);
            if (type != null) selectedAccountType = type;
            updateAccountTypeUi();
            if (SettingsService.normalizeAccountType(selectedDestinationAccountType).equals(resolveSelectedAccountType())) {
                selectedDestinationAccountType = nextAvailableDestinationCardId();
            }
            populateDestinationAccountButtons(selectedDestinationAccountType);
        });
        populateAccountTypeButtons("CARD");
    }

    private void setupDestinationAccountSelector() {
        if (toggleDestinationAccount == null) return;
        toggleDestinationAccount.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            String type = destinationTypesByButtonId.get(checkedId);
            if (type != null) selectedDestinationAccountType = type;
            updateDestinationAccountUi();
        });
        populateDestinationAccountButtons("CASH");
    }

    private void setupRecurrenceSelector() {
        if (actRecurrence == null) return;
        renderCustomRecurrenceDayChips(0);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                R.layout.item_dropdown,
                new String[]{
                        getString(R.string.transaction_recurrence_weekdays),
                        getString(R.string.transaction_recurrence_everyday),
                        getString(R.string.transaction_recurrence_custom)
                }
        );
        actRecurrence.setAdapter(adapter);
        actRecurrence.setText("", false);
        actRecurrence.setOnFocusChangeListener((view, hasFocus) -> { if (hasFocus) actRecurrence.showDropDown(); });
        actRecurrence.setOnClickListener(view -> actRecurrence.showDropDown());
        actRecurrence.setOnItemClickListener((parent, view, position, id) -> updateCustomRecurrenceVisibility());
        if (btnClearRecurrence != null) {
            btnClearRecurrence.setOnClickListener(v -> {
                actRecurrence.setText("", false);
                renderCustomRecurrenceDayChips(0);
                updateCustomRecurrenceVisibility();
            });
        }
        updateCustomRecurrenceVisibility();
    }

    private void renderCustomRecurrenceDayChips(int selectedMask) {
        if (chipCustomRecurrenceDays == null) return;
        chipCustomRecurrenceDays.removeAllViews();
        recurrenceDayByChipId.clear();
        int[] days = new int[]{
                Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
                Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY
        };
        String[] labels = new String[]{"L", "M", "X", "J", "V", "S", "D"};
        for (int i = 0; i < days.length; i++) {
            Chip chip = new Chip(requireContext());
            int id = View.generateViewId();
            chip.setId(id);
            chip.setText(labels[i]);
            chip.setCheckable(true);
            chip.setChecked((selectedMask & RecurringTransactionStore.bitForCalendarDay(days[i])) != 0);
            chip.setCheckedIconVisible(false);
            chip.setGravity(Gravity.CENTER);
            chip.setEnsureMinTouchTargetSize(true);
            chip.setMinWidth(dp(44));
            chip.setMinHeight(dp(40));
            chip.setChipStrokeWidth(dp(1));
            styleCustomRecurrenceChip(chip, chip.isChecked());
            chip.setOnCheckedChangeListener((buttonView, isChecked) -> styleCustomRecurrenceChip(chip, isChecked));
            recurrenceDayByChipId.put(id, days[i]);
            chipCustomRecurrenceDays.addView(chip);
        }
    }

    private void styleCustomRecurrenceChip(@NonNull Chip chip, boolean checked) {
        if (getContext() == null) return;
        int background = ContextCompat.getColor(requireContext(), checked ? R.color.planning_dialog_button : R.color.planning_dialog_field);
        int foreground = ContextCompat.getColor(requireContext(), checked ? android.R.color.white : R.color.md_theme_onSurface);
        int stroke = ContextCompat.getColor(requireContext(), checked ? R.color.planning_dialog_button : R.color.planning_dialog_field_stroke);
        chip.setChipBackgroundColor(ColorStateList.valueOf(background));
        chip.setTextColor(foreground);
        chip.setChipStrokeColor(ColorStateList.valueOf(stroke));
    }

    private void updateCustomRecurrenceVisibility() {
        if (layoutCustomRecurrenceDays == null || actRecurrence == null || actRecurrence.getText() == null) return;
        String recurrence = actRecurrence.getText().toString();
        boolean custom = recurrence.equals(getString(R.string.transaction_recurrence_custom));
        layoutCustomRecurrenceDays.setVisibility(custom ? View.VISIBLE : View.GONE);
        if (btnClearRecurrence != null) btnClearRecurrence.setVisibility(recurrence.trim().isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void setupLabelSelector() {
        loadLabels();
        if (etEtiquetaVisual != null) etEtiquetaVisual.setOnClickListener(v -> showLabelSheet());
        if (tilEtiquetaVisual != null) tilEtiquetaVisual.setEndIconOnClickListener(v -> showLabelSheet());
    }

    private String resolveSelectedAccountType() {
        return SettingsService.normalizeAccountType(selectedAccountType);
    }

    private void setSelectedAccountType(@Nullable String accountType) {
        String normalized = SettingsService.normalizeAccountType(accountType);
        selectedAccountType = normalized;
        if (toggleAccountType != null) {
            for (Map.Entry<Integer, String> entry : accountTypesByButtonId.entrySet()) {
                if (entry.getValue().equals(normalized)) {
                    toggleAccountType.check(entry.getKey());
                    updateAccountTypeUi();
                    return;
                }
            }
            Integer first = null;
            for (Integer id : accountTypesByButtonId.keySet()) {
                first = id;
                break;
            }
            if (first != null) toggleAccountType.check(first);
        }
        updateAccountTypeUi();
    }

    private void updateAccountTypeUi() {
        if (toggleAccountType == null) return;
        int checked = toggleAccountType.getCheckedButtonId();
        for (int i = 0; i < toggleAccountType.getChildCount(); i++) {
            View child = toggleAccountType.getChildAt(i);
            if (child instanceof MaterialButton) {
                styleToggleButton((MaterialButton) child, child.getId() == checked);
            }
        }
    }

    private void populateAccountTypeButtons(@Nullable String preferredAccountType) {
        populateAccountTypeButtons(preferredAccountType, true);
    }

    private void populateAccountTypeButtons(@Nullable String preferredAccountType, boolean useVisibleCardForDefault) {
        if (toggleAccountType == null || getContext() == null) return;
        String preferred = SettingsService.normalizeAccountType(preferredAccountType == null ? selectedAccountType : preferredAccountType);
        if ("CARD".equals(preferred) && useVisibleCardForDefault) {
            preferred = SettingsService.getVisibleCardAccount(requireContext()).getId();
        }
        FinancialAccount cardAccount = findCardAccount(preferred);
        if ("CASH".equals(preferred) || cardAccount == null) {
            cardAccount = SettingsService.getVisibleCardAccount(requireContext());
        }
        toggleAccountType.removeAllViews();
        accountTypesByButtonId.clear();
        addAccountTypeButton("CASH", getString(R.string.transaction_account_cash), R.drawable.ic_cash);
        addAccountTypeButton(cardAccount.getId(), cardPillLabel(cardAccount), R.drawable.ic_card);
        setSelectedAccountType(preferred);
    }

    @Nullable
    private FinancialAccount findCardAccount(@Nullable String accountType) {
        String normalized = SettingsService.normalizeAccountType(accountType);
        for (FinancialAccount account : SettingsService.listCardAccounts(requireContext())) {
            if (SettingsService.normalizeAccountType(account.getId()).equals(normalized)) return account;
        }
        return null;
    }

    private String cardPillLabel(@NonNull FinancialAccount account) {
        String name = account.getName() == null ? getString(R.string.transaction_account_card) : account.getName().trim();
        if (name.toLowerCase(Locale.ROOT).contains("predeterminada")) name = getString(R.string.transaction_account_card);
        if (name.length() > 7) name = name.substring(0, 7);
        String last4 = account.getLast4();
        return TextUtils.isEmpty(last4) ? name : name + " \u2022\u2022\u2022\u2022 " + last4;
    }

    private void populateDestinationAccountButtons(@Nullable String preferredAccountType) {
        if (toggleDestinationAccount == null || getContext() == null) return;
        String preferred = SettingsService.normalizeAccountType(preferredAccountType == null ? selectedDestinationAccountType : preferredAccountType);
        String origin = resolveSelectedAccountType();
        if (preferred.equals(origin)) preferred = nextAvailableDestinationCardId();
        FinancialAccount destinationCard = findCardAccount(preferred);
        if (destinationCard == null || "CASH".equals(preferred)) {
            destinationCard = firstDestinationCard(origin);
        }
        toggleDestinationAccount.removeAllViews();
        destinationTypesByButtonId.clear();
        addDestinationAccountButton("CASH", getString(R.string.transaction_account_cash), R.drawable.ic_cash);
        if (destinationCard != null) {
            addDestinationAccountButton(destinationCard.getId(), cardPillLabel(destinationCard), R.drawable.ic_card);
        }
        setSelectedDestinationAccountType(preferred);
    }

    private String nextAvailableDestinationCardId() {
        FinancialAccount account = firstDestinationCard(resolveSelectedAccountType());
        return account == null ? "CASH" : SettingsService.normalizeAccountType(account.getId());
    }

    @Nullable
    private FinancialAccount firstDestinationCard(@NonNull String origin) {
        for (FinancialAccount account : SettingsService.listCardAccounts(requireContext())) {
            String id = SettingsService.normalizeAccountType(account.getId());
            if (!id.equals(origin)) return account;
        }
        return null;
    }

    private void addAccountTypeButton(@NonNull String accountType, @NonNull String label, @DrawableRes int iconRes) {
        MaterialButton button = new MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        int id = View.generateViewId();
        button.setId(id);
        button.setText(label);
        button.setSingleLine(true);
        button.setAllCaps(false);
        button.setIconResource(iconRes);
        button.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
        button.setMinHeight(dp(48));
        button.setInsetTop(0);
        button.setInsetBottom(0);
        button.setStrokeWidth(dp(1));
        accountTypesByButtonId.put(id, SettingsService.normalizeAccountType(accountType));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(48));
        params.rightMargin = dp(8);
        toggleAccountType.addView(button, params);
    }

    private void addDestinationAccountButton(@NonNull String accountType, @NonNull String label, @DrawableRes int iconRes) {
        MaterialButton button = new MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        int id = View.generateViewId();
        button.setId(id);
        button.setText(label);
        button.setSingleLine(true);
        button.setAllCaps(false);
        button.setIconResource(iconRes);
        button.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
        button.setMinHeight(dp(48));
        button.setInsetTop(0);
        button.setInsetBottom(0);
        button.setStrokeWidth(dp(1));
        destinationTypesByButtonId.put(id, SettingsService.normalizeAccountType(accountType));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(48));
        params.rightMargin = dp(8);
        toggleDestinationAccount.addView(button, params);
    }

    private void showSelectCardSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(14), dp(20), dp(22));
        root.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.dialog_surface));

        LinearLayout header = new LinearLayout(requireContext());
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(android.view.Gravity.CENTER_VERTICAL);
        TextView title = new TextView(requireContext());
        title.setText("Seleccionar tarjeta");
        title.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurface));
        title.setTextSize(23f);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        header.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        ImageView close = new ImageView(requireContext());
        close.setImageResource(R.drawable.ic_close);
        close.setColorFilter(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurfaceVariant));
        close.setPadding(dp(9), dp(9), dp(9), dp(9));
        close.setOnClickListener(v -> dialog.dismiss());
        header.addView(close, new LinearLayout.LayoutParams(dp(44), dp(44)));
        root.addView(header);

        TextView subtitle = new TextView(requireContext());
        subtitle.setText("Elige la tarjeta para este gasto o ingreso.");
        subtitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurfaceVariant));
        subtitle.setTextSize(15f);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subtitleParams.topMargin = dp(6);
        subtitleParams.bottomMargin = dp(14);
        root.addView(subtitle, subtitleParams);

        for (FinancialAccount account : SettingsService.listCardAccounts(requireContext())) {
            root.addView(selectCardRow(dialog, account));
        }

        MaterialButton add = new MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        add.setText("Agregar nueva tarjeta");
        add.setAllCaps(false);
        add.setTextColor(ContextCompat.getColor(requireContext(), R.color.income));
        add.setIconResource(R.drawable.ic_add);
        add.setIconTint(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.income)));
        add.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.income)));
        add.setCornerRadius(dp(16));
        add.setOnClickListener(v -> {
            dialog.dismiss();
            showCreateCardSheet();
        });
        LinearLayout.LayoutParams addParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56));
        addParams.topMargin = dp(14);
        root.addView(add, addParams);

        dialog.setContentView(root);
        dialog.show();
    }

    private void showSelectDestinationCardSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(14), dp(20), dp(22));
        root.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.dialog_surface));

        LinearLayout header = new LinearLayout(requireContext());
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(android.view.Gravity.CENTER_VERTICAL);
        TextView title = new TextView(requireContext());
        title.setText("Destino de transferencia");
        title.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurface));
        title.setTextSize(22f);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        header.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        ImageView close = new ImageView(requireContext());
        close.setImageResource(R.drawable.ic_close);
        close.setColorFilter(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurfaceVariant));
        close.setPadding(dp(9), dp(9), dp(9), dp(9));
        close.setOnClickListener(v -> dialog.dismiss());
        header.addView(close, new LinearLayout.LayoutParams(dp(44), dp(44)));
        root.addView(header);

        TextView subtitle = new TextView(requireContext());
        subtitle.setText("Elige una tarjeta distinta al origen.");
        subtitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurfaceVariant));
        subtitle.setTextSize(15f);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subtitleParams.topMargin = dp(6);
        subtitleParams.bottomMargin = dp(14);
        root.addView(subtitle, subtitleParams);

        String origin = resolveSelectedAccountType();
        boolean any = false;
        for (FinancialAccount account : SettingsService.listCardAccounts(requireContext())) {
            String id = SettingsService.normalizeAccountType(account.getId());
            if (id.equals(origin)) continue;
            root.addView(selectDestinationCardRow(dialog, account));
            any = true;
        }
        if (!any) {
            TextView empty = new TextView(requireContext());
            empty.setText("No hay otra tarjeta disponible.");
            empty.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurfaceVariant));
            empty.setTextSize(15f);
            empty.setPadding(0, dp(10), 0, dp(10));
            root.addView(empty);
        }

        dialog.setContentView(root);
        dialog.show();
    }

    private View selectDestinationCardRow(@NonNull BottomSheetDialog dialog, @NonNull FinancialAccount account) {
        boolean selected = SettingsService.normalizeAccountType(account.getId()).equals(SettingsService.normalizeAccountType(selectedDestinationAccountType));
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(12), dp(14), dp(12));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(ContextCompat.getColor(requireContext(), R.color.md_theme_surface));
        bg.setCornerRadius(dp(14));
        bg.setStroke(dp(1), ContextCompat.getColor(requireContext(), selected ? R.color.income : R.color.md_theme_outlineVariant));
        row.setBackground(bg);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(10);
        row.setLayoutParams(params);

        ImageView icon = new ImageView(requireContext());
        icon.setImageResource(R.drawable.ic_card);
        icon.setColorFilter(ContextCompat.getColor(requireContext(), selected ? R.color.income : R.color.md_theme_onSurfaceVariant));
        row.addView(icon, new LinearLayout.LayoutParams(dp(34), dp(34)));

        TextView name = new TextView(requireContext());
        name.setText(cardPillLabel(account));
        name.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurface));
        name.setTextSize(16f);
        name.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        name.setSingleLine(true);
        name.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        nameParams.leftMargin = dp(14);
        row.addView(name, nameParams);

        ImageView check = new ImageView(requireContext());
        check.setImageResource(selected ? R.drawable.ic_check_circle : R.drawable.ic_card);
        check.setColorFilter(ContextCompat.getColor(requireContext(), selected ? R.color.income : R.color.md_theme_onSurfaceVariant));
        row.addView(check, new LinearLayout.LayoutParams(dp(28), dp(28)));

        row.setOnClickListener(v -> {
            selectedDestinationAccountType = SettingsService.normalizeAccountType(account.getId());
            populateDestinationAccountButtons(selectedDestinationAccountType);
            dialog.dismiss();
        });
        return row;
    }

    private View selectCardRow(@NonNull BottomSheetDialog dialog, @NonNull FinancialAccount account) {
        boolean selected = SettingsService.normalizeAccountType(account.getId()).equals(resolveSelectedAccountType());
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(12), dp(14), dp(12));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(ContextCompat.getColor(requireContext(), R.color.md_theme_surface));
        bg.setCornerRadius(dp(14));
        bg.setStroke(dp(1), ContextCompat.getColor(requireContext(), selected ? R.color.income : R.color.md_theme_outlineVariant));
        row.setBackground(bg);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(10);
        row.setLayoutParams(params);

        ImageView icon = new ImageView(requireContext());
        icon.setImageResource(R.drawable.ic_card);
        icon.setColorFilter(ContextCompat.getColor(requireContext(), selected ? R.color.income : R.color.md_theme_onSurfaceVariant));
        row.addView(icon, new LinearLayout.LayoutParams(dp(34), dp(34)));

        LinearLayout text = new LinearLayout(requireContext());
        text.setOrientation(LinearLayout.VERTICAL);
        TextView name = new TextView(requireContext());
        name.setText(account.getName());
        name.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurface));
        name.setTextSize(16f);
        name.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        text.addView(name);
        TextView last4 = new TextView(requireContext());
        String last4Text = TextUtils.isEmpty(account.getLast4()) ? "" : "\u2022\u2022\u2022\u2022 " + account.getLast4();
        last4.setText(last4Text);
        last4.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurfaceVariant));
        last4.setTextSize(14f);
        last4.setVisibility(TextUtils.isEmpty(last4Text) ? View.GONE : View.VISIBLE);
        text.addView(last4);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        textParams.leftMargin = dp(14);
        row.addView(text, textParams);

        ImageView check = new ImageView(requireContext());
        check.setImageResource(selected ? R.drawable.ic_check_circle : R.drawable.ic_visibility);
        check.setColorFilter(ContextCompat.getColor(requireContext(), selected ? R.color.income : R.color.md_theme_onSurfaceVariant));
        row.addView(check, new LinearLayout.LayoutParams(dp(30), dp(30)));

        row.setOnClickListener(v -> {
            selectedAccountType = SettingsService.normalizeAccountType(account.getId());
            populateAccountTypeButtons(selectedAccountType, false);
            dialog.dismiss();
        });
        return row;
    }

    private void showCreateCardSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(18), dp(20), dp(22));
        TextView title = new TextView(requireContext());
        title.setText("Agregar nueva tarjeta");
        title.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurface));
        title.setTextSize(22f);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(title);

        TextInputLayout tilName = new TextInputLayout(requireContext());
        tilName.setHint("Nombre (máx. 7 letras)");
        TextInputEditText etName = new TextInputEditText(requireContext());
        etName.setSingleLine(true);
        etName.setFilters(new InputFilter[] { new InputFilter.LengthFilter(7) });
        tilName.addView(etName, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        nameParams.topMargin = dp(16);
        root.addView(tilName, nameParams);

        TextInputLayout tilLast4 = new TextInputLayout(requireContext());
        tilLast4.setHint("Últimos 4 dígitos (opcional)");
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

        MaterialButton save = new MaterialButton(requireContext());
        save.setText(R.string.btn_guardar);
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52));
        saveParams.topMargin = dp(16);
        root.addView(save, saveParams);
        save.setOnClickListener(v -> {
            String name = etName.getText() == null ? "" : etName.getText().toString().trim();
            String last4 = etLast4.getText() == null ? "" : etLast4.getText().toString().trim();
            double balance = parseMontoSeguro(etBalance.getText() == null ? "" : etBalance.getText().toString());
            if (name.isEmpty()) {
                tilName.setError("Ingresa un nombre");
                return;
            }
            save.setEnabled(false);
            AccountService.create(requireContext(), name, balance, resolveSelectedCurrency(), last4, new AccountService.CreateCb() {
                @Override public void onOk(@NonNull FinancialAccount account) {
                    if (!isAdded()) return;
                    selectedAccountType = SettingsService.normalizeAccountType(account.getId());
                    populateAccountTypeButtons(selectedAccountType, false);
                    dialog.dismiss();
                }
                @Override public void onError(@Nullable String message) {
                    if (!isAdded()) return;
                    save.setEnabled(true);
                    UiFormUtils.showMessage(requireView(), TextUtils.isEmpty(message) ? "No se pudo crear la tarjeta" : message);
                }
            });
        });
        dialog.setContentView(root);
        dialog.show();
    }

    private void setSelectedDestinationAccountType(@Nullable String accountType) {
        String normalized = SettingsService.normalizeAccountType(accountType);
        selectedDestinationAccountType = normalized;
        if (toggleDestinationAccount != null) {
            for (Map.Entry<Integer, String> entry : destinationTypesByButtonId.entrySet()) {
                if (entry.getValue().equals(normalized)) {
                    toggleDestinationAccount.check(entry.getKey());
                    updateDestinationAccountUi();
                    return;
                }
            }
            for (Integer id : destinationTypesByButtonId.keySet()) {
                toggleDestinationAccount.check(id);
                break;
            }
        }
        updateDestinationAccountUi();
    }

    private void updateDestinationAccountUi() {
        if (toggleDestinationAccount == null) return;
        int checked = toggleDestinationAccount.getCheckedButtonId();
        String origin = resolveSelectedAccountType();
        for (int i = 0; i < toggleDestinationAccount.getChildCount(); i++) {
            View child = toggleDestinationAccount.getChildAt(i);
            if (child instanceof MaterialButton) {
                String type = destinationTypesByButtonId.get(child.getId());
                boolean sameAsOrigin = type != null && type.equals(origin);
                child.setEnabled(!sameAsOrigin || !isTransferSelected());
                styleToggleButton((MaterialButton) child, child.getId() == checked && child.isEnabled());
            }
        }
    }

    private void styleToggleButton(@Nullable MaterialButton button, boolean checked) {
        if (button == null || getContext() == null) return;
        int background = ContextCompat.getColor(requireContext(), checked ? R.color.md_theme_primary : R.color.md_theme_surface);
        int foreground = ContextCompat.getColor(requireContext(), checked ? R.color.md_theme_onPrimary : R.color.md_theme_onSurface);
        int stroke = ContextCompat.getColor(requireContext(), checked ? R.color.md_theme_primary : R.color.md_theme_outline);
        ColorStateList foregroundList = ColorStateList.valueOf(foreground);
        button.setBackgroundTintList(ColorStateList.valueOf(background));
        button.setTextColor(foreground);
        button.setIconTint(foregroundList);
        button.setStrokeColor(ColorStateList.valueOf(stroke));
    }

    private void setMoreOptionsExpanded(boolean expanded) {
        moreOptionsExpanded = expanded;
        if (layoutMoreOptions != null) {
            layoutMoreOptions.setVisibility(expanded ? View.VISIBLE : View.GONE);
        }
    }

    private void cargarCategoriasYRefrescar() {
        CategoryStore.loadOnce(requireContext(), new CategoryStore.Callback() {
            @Override
            public void onReady(List<? extends Categoria> cats) {
                categorias = new ArrayList<>(cats);
                boolean esIngreso = isIncomeSelected();
                String catDeseada = getArguments() != null ? getArguments().getString(EXTRA_CAT_NOMBRE) : null;
                aplicarFiltroYRefrescar(esIngreso, true, catDeseada);
            }

            @Override
            public void onError() {
                if (isAdded()) UiFormUtils.showMessage(requireView(), R.string.error_cargar_categorias);
            }
        });
    }

    private void aplicarFiltroYRefrescar(boolean esIngreso, boolean preservarSeleccion, @Nullable String categoriaDeseada) {
        if (categorias == null) return;

        visibles = new ArrayList<>();
        for (Categoria c : categorias) {
            if (c != null && c.esIngreso == esIngreso && !isSpecialCategory(c)) visibles.add(c);
        }

        List<String> nombres = new ArrayList<>();
        for (Categoria c : visibles) nombres.add(c.nombre);

        String actual = actCategoria.getText() == null ? "" : actCategoria.getText().toString();
        if (preservarSeleccion) {
            if (categoriaDeseada != null && nombres.contains(categoriaDeseada)) {
                actCategoria.setText(categoriaDeseada);
            } else if (!actual.isEmpty() && !nombres.contains(actual)) {
                actCategoria.setText("");
            }
        } else if (!actual.isEmpty() && !nombres.contains(actual)) {
            actCategoria.setText("");
        }
    }

    private boolean isSpecialCategory(@NonNull Categoria categoria) {
        return categoria.nombre != null
                && (categoria.nombre.equalsIgnoreCase(Transaccion.INITIAL_BALANCE_CATEGORY)
                || categoria.nombre.equalsIgnoreCase(Transaccion.TRANSFER_CATEGORY));
    }

    @NonNull
    private String normalize(@Nullable String raw) {
        if (raw == null) return "";
        String clean = Normalizer.normalize(raw.toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        return clean.replaceAll("\\p{InCombiningDiacriticalMarks}+", "").trim();
    }

    private void precargarDesdeArgs() {
        Bundle args = getArguments();
        if (args == null) return;

        if (args.containsKey(EXTRA_ID)) {
            editingId = args.getInt(EXTRA_ID, -1);

            double m = args.getDouble(EXTRA_MONTO, -1);
            if (m >= 0) etMonto.setText(String.valueOf(m));

            String nota = args.getString(EXTRA_NOTA);
            if (nota != null) etNota.setText(nota);

            boolean isTransfer = args.getBoolean(EXTRA_IS_TRANSFER, false);
            boolean esIngreso = args.getBoolean(EXTRA_ES_INGRESO, false);
            if (isTransfer && toggleTipo != null) {
                toggleTipo.check(R.id.btnTipoTransferencia);
                updateTransactionTypeUi();
            } else {
                setSelectedTransactionType(esIngreso);
            }

            long fechaMs = args.getLong(EXTRA_FECHA, -1L);
            if (fechaMs > 0) {
                Date fecha = new Date(fechaMs);
                if (etFecha != null) etFecha.setText(UiFormUtils.formatUiDate(fecha));
                if (etHora != null) etHora.setText(formatTime(fecha));
            }

            String moneda = args.getString(EXTRA_MONEDA);
            if (moneda != null && actMoneda != null) {
                actMoneda.setText(CurrencyConverter.normalize(moneda), false);
                updateCurrencyPrefix();
            }
            setSelectedAccountType(args.getString(EXTRA_ACCOUNT_TYPE, "CARD"));
            selectedLabelId = TransactionLabelStore.assignedLabelId(requireContext(), editingId);
            updateSelectedLabelText();
            loadExistingRecurrence(editingId);

            btnGuardar.setText(R.string.btn_guardar);
        } else {
            btnGuardar.setText(R.string.btn_guardar);
            if (etFecha != null) etFecha.setText(UiFormUtils.formatUiDate(new Date()));
            if (etHora != null) etHora.setText(formatTime(new Date()));
        }
    }

    private void showCategoryPickerSheet() {
        if (visibles == null) return;
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(12), dp(20), dp(20));

        TextView title = new TextView(requireContext());
        title.setText(R.string.hint_categoria);
        title.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurface));
        title.setTextSize(20f);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(title);

        TextInputEditText search = new TextInputEditText(requireContext());
        search.setHint("Buscar categoría");
        search.setSingleLine(true);
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        searchParams.topMargin = dp(12);
        root.addView(search, searchParams);

        ScrollView scroll = new ScrollView(requireContext());
        scroll.setFillViewport(false);
        LinearLayout list = new LinearLayout(requireContext());
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        LinearLayout.LayoutParams listParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(360)
        );
        listParams.topMargin = dp(10);
        root.addView(scroll, listParams);

        Runnable renderAll = () -> renderCategoryPickerRows(list, "", dialog);
        renderAll.run();
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                renderCategoryPickerRows(list, s == null ? "" : s.toString(), dialog);
            }
            @Override public void afterTextChanged(Editable s) { }
        });
        dialog.setContentView(root);
        dialog.show();
    }

    private void renderCategoryPickerRows(@NonNull LinearLayout list, @Nullable String filter, @NonNull BottomSheetDialog dialog) {
        list.removeAllViews();
        String normalizedFilter = normalize(filter);
        for (Categoria categoria : visibles) {
            if (categoria == null || isSpecialCategory(categoria)) continue;
            if (!normalizedFilter.isEmpty() && !normalize(categoria.nombre).contains(normalizedFilter)) continue;

            LinearLayout row = new LinearLayout(requireContext());
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, dp(10), 0, dp(10));
            row.setClickable(true);
            row.setFocusable(true);

            int accent = CategoryVisuals.colorFor(requireContext(), categoria.nombre, categoria.esIngreso);
            ImageView icon = new ImageView(requireContext());
            icon.setImageResource(CategoryVisuals.iconFor(categoria.nombre, categoria.esIngreso));
            icon.setColorFilter(accent);
            GradientDrawable iconBg = new GradientDrawable();
            iconBg.setShape(GradientDrawable.OVAL);
            iconBg.setColor(LabelColorUtils.iconBackground(requireContext(), accent));
            icon.setBackground(iconBg);
            icon.setPadding(dp(10), dp(10), dp(10), dp(10));
            row.addView(icon, new LinearLayout.LayoutParams(dp(50), dp(50)));

            TextView name = new TextView(requireContext());
            name.setText(categoria.nombre);
            name.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurface));
            name.setTextSize(16f);
            name.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            nameParams.leftMargin = dp(14);
            row.addView(name, nameParams);

            row.setOnClickListener(v -> {
                actCategoria.setText(categoria.nombre);
                tilCategoria.setError(null);
                dialog.dismiss();
            });

            list.addView(row, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
        }
    }

    private void loadLabels() {
        if (!isAdded()) return;
        transactionLabels = TransactionLabelStore.listLabels(requireContext());
        updateSelectedLabelText();
    }

    private void updateSelectedLabelText() {
        if (etEtiquetaVisual == null) return;
        TransactionLabelStore.Label selected = TransactionLabelStore.findLabel(requireContext(), selectedLabelId);
        etEtiquetaVisual.setText(selected == null ? getString(R.string.transaction_label_none) : selected.name);
        if (tilEtiquetaVisual != null && getContext() != null) {
            int tint = selected == null
                    ? ContextCompat.getColor(requireContext(), R.color.md_theme_onSurfaceVariant)
                    : selected.colorInt();
            tilEtiquetaVisual.setStartIconTintList(ColorStateList.valueOf(tint));
        }
    }

    private void loadExistingRecurrence(@Nullable Integer transactionId) {
        if (transactionId == null || transactionId <= 0 || actRecurrence == null) return;
        final android.content.Context appContext = requireContext().getApplicationContext();
        new Thread(() -> {
            RecurringTransactionEntity template = RecurringTransactionStore.findTemplateForSource(appContext, transactionId);
            if (!isAdded() || template == null) return;
            requireActivity().runOnUiThread(() -> applyExistingRecurrence(template));
        }).start();
    }

    private void applyExistingRecurrence(@NonNull RecurringTransactionEntity template) {
        if (template == null) return;
        if (RecurringTransactionStore.FREQUENCY_WEEKDAYS.equals(template.getFrequency())) {
            actRecurrence.setText(getString(R.string.transaction_recurrence_weekdays), false);
        } else if (RecurringTransactionStore.FREQUENCY_EVERYDAY.equals(template.getFrequency())) {
            actRecurrence.setText(getString(R.string.transaction_recurrence_everyday), false);
        } else if (RecurringTransactionStore.FREQUENCY_CUSTOM.equals(template.getFrequency())) {
            actRecurrence.setText(getString(R.string.transaction_recurrence_custom), false);
            renderCustomRecurrenceDayChips(template.getDaysMask());
        }
        updateCustomRecurrenceVisibility();
    }

    private void showLabelSheet() {
        loadLabels();
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(16), dp(20), dp(20));

        TextView title = new TextView(requireContext());
        title.setText(R.string.transaction_visual_label);
        title.setTextSize(18f);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurface));
        root.addView(title);

        MaterialButton none = new MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        none.setText(R.string.transaction_label_none);
        none.setAllCaps(false);
        none.setIconResource(R.drawable.ic_etiqueta);
        styleLabelOption(none, null, selectedLabelId == null);
        none.setOnClickListener(v -> {
            selectedLabelId = null;
            updateSelectedLabelText();
            dialog.dismiss();
        });
        LinearLayout.LayoutParams noneParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48));
        noneParams.topMargin = dp(12);
        root.addView(none, noneParams);

        for (TransactionLabelStore.Label label : transactionLabels) {
            MaterialButton row = new MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
            row.setText(label.name);
            row.setAllCaps(false);
            row.setIconResource(R.drawable.ic_etiqueta);
            styleLabelOption(row, label, label.id.equals(selectedLabelId));
            row.setOnClickListener(v -> {
                selectedLabelId = label.id;
                updateSelectedLabelText();
                dialog.dismiss();
            });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48));
            params.topMargin = dp(8);
            root.addView(row, params);
        }

        MaterialButton manage = new MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        manage.setText(R.string.transaction_label_manage);
        manage.setAllCaps(false);
        manage.setIconResource(R.drawable.ic_etiqueta);
        manage.setOnClickListener(v -> {
            dialog.dismiss();
            showManageLabelsDialog();
        });
        LinearLayout.LayoutParams manageParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48));
        manageParams.topMargin = dp(12);
        root.addView(manage, manageParams);

        MaterialButton create = new MaterialButton(requireContext());
        create.setText(R.string.transaction_label_create);
        create.setAllCaps(false);
        create.setIconResource(R.drawable.ic_etiqueta);
        create.setOnClickListener(v -> showCreateLabelDialog(() -> {
            loadLabels();
            dialog.dismiss();
            showLabelSheet();
        }));
        LinearLayout.LayoutParams createParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(50));
        createParams.topMargin = dp(12);
        root.addView(create, createParams);

        dialog.setContentView(root);
        dialog.show();
    }

    private void showManageLabelsDialog() {
        loadLabels();
        String[] names = new String[transactionLabels.size() + 1];
        names[0] = getString(R.string.transaction_label_create);
        for (int i = 0; i < transactionLabels.size(); i++) names[i + 1] = transactionLabels.get(i).name;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.transaction_label_manage)
                .setItems(names, (dialog, which) -> {
                    if (which == 0) {
                        showCreateLabelDialog(this::showManageLabelsDialog);
                    } else {
                        showEditLabelDialog(transactionLabels.get(which - 1));
                    }
                })
                .show();
    }

    private void showEditLabelDialog(@NonNull TransactionLabelStore.Label label) {
        View content = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_simple_text, null, false);
        TextInputEditText input = content.findViewById(R.id.etSimple);
        input.setHint(R.string.transaction_label_name);
        input.setText(label.name);

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(8), dp(20), dp(8));
        root.addView(content);

        TextInputEditText hex = new TextInputEditText(requireContext());
        hex.setHint(R.string.transaction_label_hex);
        hex.setSingleLine(true);
        hex.setText(label.colorHex);
        LinearLayout.LayoutParams hexParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hexParams.topMargin = dp(10);
        root.addView(hex, hexParams);
        root.addView(buildColorPalette(hex));

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.transaction_label_manage)
                .setView(root)
                .setNeutralButton(R.string.btn_eliminar, null)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.btn_guardar, null)
                .create();
        dialog.setOnShowListener(d -> {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                TransactionLabelStore.deleteLabel(requireContext(), label.id);
                if (label.id.equals(selectedLabelId)) selectedLabelId = null;
                dialog.dismiss();
                loadLabels();
            });
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String name = input.getText() == null ? "" : input.getText().toString().trim();
                String color = hex.getText() == null ? "" : hex.getText().toString().trim();
                if (name.isEmpty()) {
                    input.setError(getString(R.string.transaction_label_name_error));
                    return;
                }
                if (!TransactionLabelStore.isValidHex(TransactionLabelStore.normalizeHex(color))) {
                    hex.setError(getString(R.string.transaction_label_hex_error));
                    return;
                }
                TransactionLabelStore.updateLabel(requireContext(), label.id, name, color);
                dialog.dismiss();
                loadLabels();
            });
        });
        dialog.show();
    }

    private void styleLabelOption(@NonNull MaterialButton button, @Nullable TransactionLabelStore.Label label, boolean selected) {
        int accent = label == null
                ? ContextCompat.getColor(requireContext(), R.color.md_theme_outline)
                : label.colorInt();
        int background = selected
                ? LabelColorUtils.chipBackground(requireContext(), accent, true)
                : ContextCompat.getColor(requireContext(), R.color.md_theme_surface);
        int text = selected
                ? LabelColorUtils.textOnTint(requireContext(), accent, background)
                : ContextCompat.getColor(requireContext(), R.color.md_theme_onSurface);
        button.setBackgroundTintList(ColorStateList.valueOf(background));
        button.setTextColor(text);
        button.setStrokeColor(ColorStateList.valueOf(selected ? accent : ContextCompat.getColor(requireContext(), R.color.md_theme_outline)));
        button.setIconTint(ColorStateList.valueOf(label == null ? text : accent));
    }

    private void showCreateLabelDialog(@Nullable Runnable onSaved) {
        View content = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_simple_text, null, false);
        TextInputEditText input = content.findViewById(R.id.etSimple);
        input.setHint(R.string.transaction_label_name);

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(8), dp(20), dp(8));
        root.addView(content);

        TextInputEditText hex = new TextInputEditText(requireContext());
        hex.setHint(R.string.transaction_label_hex);
        hex.setSingleLine(true);
        hex.setText("#4FA37A");
        LinearLayout.LayoutParams hexParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hexParams.topMargin = dp(10);
        root.addView(hex, hexParams);

        root.addView(buildColorPalette(hex));

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.transaction_label_create)
                .setView(root)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.btn_guardar, null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = input.getText() == null ? "" : input.getText().toString().trim();
            String color = hex.getText() == null ? "" : hex.getText().toString().trim();
            if (name.isEmpty()) {
                input.setError(getString(R.string.transaction_label_name_error));
                return;
            }
            if (!TransactionLabelStore.isValidHex(TransactionLabelStore.normalizeHex(color))) {
                hex.setError(getString(R.string.transaction_label_hex_error));
                return;
            }
            TransactionLabelStore.Label label = TransactionLabelStore.createLabel(requireContext(), name, color);
            selectedLabelId = label.id;
            dialog.dismiss();
            loadLabels();
            if (onSaved != null) onSaved.run();
        }));
        dialog.show();
    }

    private View buildColorPalette(@NonNull TextInputEditText hex) {
        LinearLayout wrapper = new LinearLayout(requireContext());
        wrapper.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams wrapperParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        wrapperParams.topMargin = dp(12);
        wrapper.setLayoutParams(wrapperParams);

        TextView label = new TextView(requireContext());
        label.setText("Paleta de colores");
        label.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurface));
        label.setTextSize(13f);
        label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        wrapper.addView(label);

        GridLayout palette = new GridLayout(requireContext());
        palette.setColumnCount(6);
        LinearLayout.LayoutParams gridParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        gridParams.topMargin = dp(8);
        wrapper.addView(palette, gridParams);

        String selectedHex = TransactionLabelStore.normalizeHex(hex.getText() == null ? "#4FA37A" : hex.getText().toString());
        for (String colorHex : TransactionLabelStore.paletteColors()) {
            int color = android.graphics.Color.parseColor(colorHex);
            View swatch = new View(requireContext());
            swatch.setTag(colorHex);
            applyColorSwatchBackground(swatch, color, colorHex.equals(selectedHex));
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = dp(34);
            params.height = dp(34);
            params.setMargins(0, 0, dp(12), dp(12));
            palette.addView(swatch, params);
            swatch.setOnClickListener(v -> {
                hex.setText(colorHex);
                for (int i = 0; i < palette.getChildCount(); i++) {
                    View child = palette.getChildAt(i);
                    String childHex = child.getTag() == null ? "" : child.getTag().toString();
                    applyColorSwatchBackground(child, android.graphics.Color.parseColor(childHex), colorHex.equals(childHex));
                }
            });
        }
        return wrapper;
    }

    private void applyColorSwatchBackground(@NonNull View view, int color, boolean selected) {
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setColor(color);
        bg.setCornerRadius(dp(9));
        bg.setStroke(dp(selected ? 3 : 1), ContextCompat.getColor(requireContext(), selected ? R.color.md_theme_onSurface : R.color.md_theme_outline));
        view.setBackground(bg);
    }

    private void onGuardar(View view) {
        String sMonto = etMonto.getText() == null ? "" : etMonto.getText().toString().trim();
        if (sMonto.isEmpty()) {
            setMontoError(getString(R.string.error_ingresa_monto));
            return;
        }

        setMontoError(null);
        String sFecha = etFecha != null && etFecha.getText() != null
                ? etFecha.getText().toString().trim() : "";
        Date fechaSeleccionada = parseFechaSegura(sFecha);
        if (fechaSeleccionada == null) {
            tilFecha.setError(getString(R.string.error_formato_fecha));
            return;
        }
        tilFecha.setError(null);
        String sHora = etHora != null && etHora.getText() != null
                ? etHora.getText().toString().trim() : "";
        if (!UiFormUtils.isValidTime(sHora)) {
            tilHora.setError(getString(R.string.transaction_time_error));
            return;
        }
        tilHora.setError(null);
        final long fechaMs = combineDateAndTime(fechaSeleccionada, sHora);

        final boolean transferLocal = isTransferSelected();
        String nombreSel = actCategoria.getText() == null ? "" : actCategoria.getText().toString().trim();
        Categoria seleccionada = null;
        if (!transferLocal && visibles != null) {
            for (Categoria c : visibles) {
                if (c != null && c.nombre != null && c.nombre.equals(nombreSel)) { seleccionada = c; break; }
            }
        }
        if (!transferLocal && seleccionada == null) {
            tilCategoria.setError(getString(R.string.error_selecciona_categoria));
            actCategoria.requestFocus();
            showCategoryPickerSheet();
            return;
        }

        tilCategoria.setError(null);
        final String monedaLocal = resolveSelectedCurrency();
        if (!CurrencyConverter.supportedCurrencies().contains(monedaLocal)) {
            tilMoneda.setError(getString(R.string.transaction_currency_error));
            return;
        }
        tilMoneda.setError(null);
        final double montoLocal = Math.abs(parseMontoSeguro(sMonto));
        if (montoLocal <= 0) {
            setMontoError(getString(R.string.error_monto_invalido));
            return;
        }

        setMontoError(null);
        final boolean esIngresoLocal = isIncomeSelected();
        final String accountTypeLocal = resolveSelectedAccountType();
        final String destinationTypeLocal = SettingsService.normalizeAccountType(selectedDestinationAccountType);
        if (transferLocal && accountTypeLocal.equals(destinationTypeLocal)) {
            UiFormUtils.showMessage(requireView(), R.string.transfer_same_account_error);
            return;
        }
        final String notaLocal = etNota.getText() == null ? "" : etNota.getText().toString().trim();
        final String recurrenceLocal = resolveRecurrenceCode();
        final int recurrenceDaysMaskLocal = selectedCustomRecurrenceDaysMask();
        if (RecurringTransactionStore.FREQUENCY_CUSTOM.equals(recurrenceLocal) && recurrenceDaysMaskLocal == 0) {
            UiFormUtils.showMessage(requireView(), R.string.transaction_recurrence_custom_error);
            setMoreOptionsExpanded(true);
            return;
        }
        final Categoria catSel = seleccionada;
        final Integer editingIdLocal = editingId;

        UiFormUtils.setActionLoading(btnGuardar, true);

        if (transferLocal) {
            if (editingIdLocal != null && editingIdLocal > 0) {
                UiFormUtils.showMessage(requireView(), R.string.error_guardar_transaccion);
                UiFormUtils.setActionLoading(btnGuardar, false);
                return;
            }
            TransService.createTransfer(requireContext(), accountTypeLocal, destinationTypeLocal, montoLocal, notaLocal, fechaMs, monedaLocal,
                    new TransService.SimpleCb() {
                        @Override public void onOk(int newId) {
                            persistOptionalLabel(newId);
                            persistOptionalRecurrence(newId, true, accountTypeLocal, destinationTypeLocal, montoLocal, notaLocal, fechaMs, monedaLocal, recurrenceLocal, recurrenceDaysMaskLocal);
                            UiFormUtils.setActionLoading(btnGuardar, false);
                            UiFormUtils.showMessage(requireView(), R.string.trans_saved);
                            finishAfterCreate();
                        }
                        @Override public void onError(@Nullable String message) {
                            UiFormUtils.setActionLoading(btnGuardar, false);
                            UiFormUtils.showMessage(requireView(), !TextUtils.isEmpty(message) ? message : getString(R.string.error_guardar_transaccion));
                        }
                    });

        } else if (editingIdLocal == null || editingIdLocal < 0) {
            TransService.create(requireContext(), catSel.id, esIngresoLocal, montoLocal, notaLocal, fechaMs, monedaLocal, accountTypeLocal,
                    new TransService.SimpleCb() {
                        @Override public void onOk(int newId) {
                            persistOptionalLabel(newId);
                            persistOptionalRecurrence(newId, false, accountTypeLocal, null, montoLocal, notaLocal, fechaMs, monedaLocal, recurrenceLocal, recurrenceDaysMaskLocal);
                            UiFormUtils.setActionLoading(btnGuardar, false);
                            UiFormUtils.showMessage(requireView(), R.string.trans_saved);
                            finishAfterCreate();
                        }
                        @Override public void onError(@Nullable String message) {
                            UiFormUtils.setActionLoading(btnGuardar, false);
                            if (message != null && !message.isEmpty()) {
                                UiFormUtils.showMessage(requireView(), message);
                            } else {
                                UiFormUtils.showMessage(requireView(), R.string.error_guardar_transaccion);
                            }
                        }
                    });

        } else {
            TransService.update(requireContext(), editingIdLocal, catSel.id, esIngresoLocal, montoLocal, notaLocal, fechaMs, monedaLocal, accountTypeLocal,
                    new TransService.VoidCb() {
                        @Override public void onOk() {
                            persistOptionalLabel(editingIdLocal);
                            persistOptionalRecurrence(editingIdLocal, false, accountTypeLocal, null, montoLocal, notaLocal, fechaMs, monedaLocal, recurrenceLocal, recurrenceDaysMaskLocal);
                            UiFormUtils.setActionLoading(btnGuardar, false);
                            UiFormUtils.showMessage(requireView(), R.string.trans_updated);
                            NavHostFragment.findNavController(NuevaTransaccionFragment.this).popBackStack();
                        }
                        @Override public void onError(@Nullable String message) {
                            UiFormUtils.setActionLoading(btnGuardar, false);
                            if (message != null && !message.isEmpty()) {
                                UiFormUtils.showMessage(requireView(), message);
                            } else {
                                UiFormUtils.showMessage(requireView(), R.string.error_guardar_transaccion);
                            }
                        }
                    });
        }
    }

    private Date parseFechaSegura(String raw) {
        return UiFormUtils.parseUiDate(raw);
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

    private void finishAfterCreate() {
        NavController controller = NavHostFragment.findNavController(this);
        if (!controller.popBackStack(R.id.nav_home, false)) {
            controller.navigate(R.id.nav_home);
        }
    }

    private void persistOptionalLabel(int transactionId) {
        if (transactionId <= 0) return;
        TransactionLabelStore.setLabel(requireContext(), transactionId, selectedLabelId);
    }

    private void persistOptionalRecurrence(
            int transactionId,
            boolean isTransfer,
            @NonNull String accountType,
            @Nullable String destinationType,
            double amount,
            @NonNull String note,
            long firstDate,
            @NonNull String currency,
            @Nullable String recurrence,
            int recurrenceDaysMask
    ) {
        if (TextUtils.isEmpty(recurrence)) {
            RecurringTransactionStore.deleteTemplateForSource(requireContext(), transactionId);
            return;
        }
        int categoryId = 0;
        boolean income = isIncomeSelected();
        if (!isTransfer && visibles != null) {
            String selectedName = actCategoria.getText() == null ? "" : actCategoria.getText().toString().trim();
            for (Categoria c : visibles) {
                if (c != null && selectedName.equals(c.nombre)) {
                    categoryId = c.id;
                    break;
                }
            }
        }
        RecurringTransactionStore.saveTemplate(
                requireContext(),
                transactionId,
                recurrence,
                recurrenceDaysMask,
                isTransfer,
                categoryId,
                income,
                amount,
                note,
                firstDate,
                currency,
                accountType,
                destinationType,
                selectedLabelId
        );
    }

    @Nullable
    private String resolveRecurrenceCode() {
        if (actRecurrence == null || actRecurrence.getText() == null) return null;
        String value = actRecurrence.getText().toString();
        if (value.equals(getString(R.string.transaction_recurrence_weekdays))) return RecurringTransactionStore.FREQUENCY_WEEKDAYS;
        if (value.equals(getString(R.string.transaction_recurrence_everyday))) return RecurringTransactionStore.FREQUENCY_EVERYDAY;
        if (value.equals(getString(R.string.transaction_recurrence_custom))) return RecurringTransactionStore.FREQUENCY_CUSTOM;
        return null;
    }

    private int selectedCustomRecurrenceDaysMask() {
        if (chipCustomRecurrenceDays == null) return 0;
        int mask = 0;
        for (int i = 0; i < chipCustomRecurrenceDays.getChildCount(); i++) {
            View child = chipCustomRecurrenceDays.getChildAt(i);
            if (child instanceof Chip && ((Chip) child).isChecked()) {
                Integer day = recurrenceDayByChipId.get(child.getId());
                if (day != null) mask |= RecurringTransactionStore.bitForCalendarDay(day);
            }
        }
        return mask;
    }

    private long combineDateAndTime(@NonNull Date date, @NonNull String time) {
        String[] parts = time.split(":");
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(parts[0]));
        cal.set(Calendar.MINUTE, Integer.parseInt(parts[1]));
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private String formatTime(@NonNull Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return String.format(Locale.US, "%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

}


