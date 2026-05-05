package com.example.finanzas.ui;

import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
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
import com.example.finanzas.data.api.SettingsService;
import com.example.finanzas.data.api.SuggestionService;
import com.example.finanzas.data.api.TransService;
import com.example.finanzas.data.model.CategorySuggestion;
import com.example.finanzas.data.model.Categoria;
import com.example.finanzas.data.model.FinancialAccount;
import com.example.finanzas.data.model.Transaccion;
import com.example.finanzas.util.CategoryVisuals;
import com.example.finanzas.util.CurrencyConverter;
import com.example.finanzas.util.LabelColorUtils;
import com.example.finanzas.util.UiFormUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
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

    private EditText etMonto;
    private TextInputEditText etNota, etFecha, etHora;
    private TextInputLayout tilFecha, tilHora, tilCategoria, tilMoneda;
    private MaterialButtonToggleGroup toggleTipo, toggleAccountType;
    private MaterialButton btnTipoGasto, btnTipoIngreso;
    private TextInputEditText actCategoria;
    private MaterialAutoCompleteTextView actMoneda;
    private MaterialButton btnGuardar;
    private MaterialButton btnSugerir;
    private MaterialButton btnManageCategories;
    private MaterialButton btnMoreOptions;
    private Chip chipSugerencia;
    private View layoutMoreOptions;
    private TextView tvMontoCurrency, tvMontoError;
    private boolean moreOptionsExpanded = false;

    private Integer editingId = null;
    private List<Categoria> categorias;
    private List<Categoria> visibles;
    private CategorySuggestion currentSuggestion;
    private Integer pendingSuggestedCategoryId;
    private final Map<Integer, String> accountTypesByButtonId = new HashMap<>();
    private String selectedAccountType = "CARD";

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
        btnTipoGasto  = v.findViewById(R.id.btnTipoGasto);
        btnTipoIngreso = v.findViewById(R.id.btnTipoIngreso);
        actCategoria  = v.findViewById(R.id.actCategoria);
        actMoneda     = v.findViewById(R.id.actMoneda);
        btnGuardar    = v.findViewById(R.id.btnGuardar);
        btnSugerir    = v.findViewById(R.id.btnSugerir);
        btnManageCategories = v.findViewById(R.id.btnManageCategories);
        btnMoreOptions = v.findViewById(R.id.btnMoreOptions);
        chipSugerencia = v.findViewById(R.id.chipSugerencia);
        layoutMoreOptions = v.findViewById(R.id.layoutMoreOptions);

        setupTransactionTypeSelector();
        setupCurrencySelector();
        setupAccountTypeSelector();
        setMoreOptionsExpanded(false);
        updateCurrencyPrefix();

        if (etFecha != null) UiFormUtils.bindDatePicker(requireContext(), etFecha);
        if (etHora != null) UiFormUtils.bindTimePicker(requireContext(), etHora);
        bindMontoErrorCleaner();
        UiFormUtils.clearErrorOnTextChange(etFecha, etHora, actCategoria);

        actCategoria.setOnClickListener(view -> showCategoryPickerSheet());
        tilCategoria.setEndIconOnClickListener(view -> showCategoryPickerSheet());

        if (btnSugerir != null) btnSugerir.setVisibility(View.GONE);
        btnManageCategories.setOnClickListener(v14 -> showCategoryManagerDialog());
        btnMoreOptions.setOnClickListener(v15 -> setMoreOptionsExpanded(!moreOptionsExpanded));
        chipSugerencia.setOnClickListener(v12 -> aplicarSugerenciaActual());
        chipSugerencia.setOnCloseIconClickListener(v13 -> {
            chipSugerencia.setVisibility(View.GONE);
            currentSuggestion = null;
        });

        precargarDesdeArgs();
        cargarCategoriasYRefrescar();

        if (etFecha != null && (etFecha.getText() == null || TextUtils.isEmpty(etFecha.getText().toString()))) {
            etFecha.setText(UiFormUtils.formatUiDate(new Date()));
        }
        if (etHora != null && (etHora.getText() == null || TextUtils.isEmpty(etHora.getText().toString()))) {
            etHora.setText(formatTime(new Date()));
        }

        btnGuardar.setOnClickListener(this::onGuardar);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateCurrencyPrefix();
        populateAccountTypeButtons(selectedAccountType);
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
            aplicarFiltroYRefrescar(isIncomeSelected(), false, null);
        });
        setSelectedTransactionType(false);
    }

    private boolean isIncomeSelected() {
        return toggleTipo != null && toggleTipo.getCheckedButtonId() == R.id.btnTipoIngreso;
    }

    private void setSelectedTransactionType(boolean income) {
        if (toggleTipo != null) {
            toggleTipo.check(income ? R.id.btnTipoIngreso : R.id.btnTipoGasto);
        }
        updateTransactionTypeUi();
    }

    private void updateTransactionTypeUi() {
        styleToggleButton(btnTipoGasto, !isIncomeSelected());
        styleToggleButton(btnTipoIngreso, isIncomeSelected());
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
        });
        populateAccountTypeButtons("CARD");
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
        if (toggleAccountType == null || getContext() == null) return;
        String preferred = SettingsService.normalizeAccountType(preferredAccountType == null ? selectedAccountType : preferredAccountType);
        toggleAccountType.removeAllViews();
        accountTypesByButtonId.clear();
        addAccountTypeButton("CASH", getString(R.string.transaction_account_cash), R.drawable.ic_cash);
        addAccountTypeButton("CARD", getString(R.string.transaction_account_card), R.drawable.ic_card);
        for (FinancialAccount account : SettingsService.listFinancialAccounts(requireContext())) {
            if (account == null || account.getName() == null || account.getName().trim().isEmpty()) continue;
            addAccountTypeButton(account.getId(), account.getName(), R.drawable.ic_card);
        }
        setSelectedAccountType(preferred);
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
        if (btnMoreOptions != null) {
            btnMoreOptions.setText(expanded ? R.string.transaction_less_options : R.string.transaction_more_options);
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
                tryApplyPendingSuggestion();
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

    private void solicitarSugerencia() {
        final double montoActual = parseMontoActual();
        final boolean esIngreso = isIncomeSelected();
        final String nota = etNota.getText() == null ? "" : etNota.getText().toString().trim();

        btnSugerir.setEnabled(false);
        btnSugerir.setText(R.string.suggest_loading);
        chipSugerencia.setVisibility(View.GONE);

        SuggestionService.suggest(requireContext(), nota, esIngreso, Math.abs(montoActual), new SuggestionService.Callback() {
            @Override
            public void onSuccess(CategorySuggestion suggestion) {
                if (!isAdded()) return;
                btnSugerir.setEnabled(true);
                btnSugerir.setText(R.string.suggest_category);
                showSuggestionResult(completeSuggestion(suggestion, nota));
            }

            @Override
            public void onError() {
                if (!isAdded()) return;
                btnSugerir.setEnabled(true);
                btnSugerir.setText(R.string.suggest_category);
                showSuggestionResult(buildLocalSuggestion(nota));
            }
        });
    }

    private void aplicarSugerenciaActual() {
        if (currentSuggestion == null || !currentSuggestion.hasSuggestion()) {
            chipSugerencia.setVisibility(View.GONE);
            return;
        }
        pendingSuggestedCategoryId = currentSuggestion.getCategoriaId();
        tryApplyPendingSuggestion();
    }

    private void tryApplyPendingSuggestion() {
        if (pendingSuggestedCategoryId == null || categorias == null) return;
        Categoria sugerida = null;
        for (Categoria c : categorias) {
            if (c != null && c.id == pendingSuggestedCategoryId) {
                sugerida = c;
                break;
            }
        }
        if (sugerida == null) return;
        if (isIncomeSelected() != sugerida.esIngreso) setSelectedTransactionType(sugerida.esIngreso);
        aplicarFiltroYRefrescar(sugerida.esIngreso, true, sugerida.nombre);
        actCategoria.setText(sugerida.nombre);
        pendingSuggestedCategoryId = null;
    }

    private int calcularPorcentaje(double confidence) {
        if (confidence <= 0) return 0;
        if (confidence > 1) {
            return (int) Math.round(confidence);
        }
        return (int) Math.round(confidence * 100);
    }

    private void showSuggestionResult(@Nullable CategorySuggestion suggestion) {
        currentSuggestion = suggestion;
        if (suggestion != null && suggestion.hasSuggestion()) {
            Categoria categoria = findCategoriaById(suggestion.getCategoriaId());
            String nombre = suggestion.getCategoriaNombre();
            if (TextUtils.isEmpty(nombre) && categoria != null) nombre = categoria.nombre;
            if (TextUtils.isEmpty(nombre)) nombre = getString(R.string.hint_categoria);

            int porcentaje = calcularPorcentaje(suggestion.getConfidence());
            chipSugerencia.setText(getString(R.string.suggest_chip_with_confidence, nombre, porcentaje));
            chipSugerencia.setVisibility(View.VISIBLE);
            pendingSuggestedCategoryId = suggestion.getCategoriaId();
            tryApplyPendingSuggestion();
            UiFormUtils.showMessage(requireView(), getString(R.string.suggest_applied, nombre));
            return;
        }

        chipSugerencia.setVisibility(View.GONE);
        pendingSuggestedCategoryId = null;
        UiFormUtils.showMessage(requireView(), R.string.suggest_not_found);
    }

    @Nullable
    private CategorySuggestion completeSuggestion(@Nullable CategorySuggestion suggestion, @NonNull String nota) {
        if (suggestion != null && suggestion.hasSuggestion()) {
            Categoria categoria = findCategoriaById(suggestion.getCategoriaId());
            if (categoria != null) {
                suggestion.setCategoriaNombre(categoria.nombre);
                return suggestion;
            }
        }
        return buildLocalSuggestion(nota);
    }

    @Nullable
    private CategorySuggestion buildLocalSuggestion(@Nullable String nota) {
        if (categorias == null || categorias.isEmpty()) return null;
        String text = normalize(nota);
        if (text.isEmpty()) return null;

        Categoria match = null;
        if (containsAny(text, "comida", "restaurante", "mercado", "supermercado", "almuerzo", "cena", "desayuno")) {
            match = findCategoriaByKeywords(false, "alimentacion", "comida", "mercado", "restaurante");
        } else if (containsAny(text, "taxi", "bus", "gasolina", "combustible", "uber", "transporte", "pasaje")) {
            match = findCategoriaByKeywords(false, "transporte", "taxi", "bus", "gasolina", "combustible");
        } else if (containsAny(text, "netflix", "cine", "juego", "spotify", "entretenimiento", "ocio")) {
            match = findCategoriaByKeywords(false, "entretenimiento", "cine", "juego", "netflix", "ocio");
        } else if (containsAny(text, "luz", "agua", "internet", "telefono", "servicio", "servicios", "electricidad")) {
            match = findCategoriaByKeywords(false, "servicios", "luz", "agua", "internet", "electricidad", "telefono");
        } else if (containsAny(text, "salario", "sueldo", "pago", "nomina", "ingreso", "quincena")) {
            match = findCategoriaByKeywords(true, "salario", "sueldo", "ingreso", "pago");
        }

        if (match == null) return null;
        CategorySuggestion local = new CategorySuggestion();
        local.setCategoriaId(match.id);
        local.setCategoriaNombre(match.nombre);
        local.setConfidence(0.7);
        return local;
    }

    @Nullable
    private Categoria findCategoriaById(@Nullable Integer id) {
        if (id == null || categorias == null) return null;
        for (Categoria categoria : categorias) {
            if (categoria != null && categoria.id == id) return categoria;
        }
        return null;
    }

    private boolean isSpecialCategory(@NonNull Categoria categoria) {
        return categoria.nombre != null
                && categoria.nombre.equalsIgnoreCase(Transaccion.INITIAL_BALANCE_CATEGORY);
    }

    @Nullable
    private Categoria findCategoriaByKeywords(boolean esIngreso, @NonNull String... keywords) {
        if (categorias == null) return null;
        for (Categoria categoria : categorias) {
            if (categoria == null || categoria.esIngreso != esIngreso) continue;
            String name = normalize(categoria.nombre);
            for (String keyword : keywords) {
                if (name.contains(keyword)) return categoria;
            }
        }
        return null;
    }

    private boolean containsAny(@NonNull String text, @NonNull String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) return true;
        }
        return false;
    }

    @NonNull
    private String normalize(@Nullable String raw) {
        if (raw == null) return "";
        String clean = Normalizer.normalize(raw.toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        return clean.replaceAll("\\p{InCombiningDiacriticalMarks}+", "").trim();
    }

    private double parseMontoActual() {
        String sMonto = etMonto.getText() == null ? "" : etMonto.getText().toString();
        return parseMontoSeguro(sMonto);
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

            boolean esIngreso = args.getBoolean(EXTRA_ES_INGRESO, false);
            setSelectedTransactionType(esIngreso);

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

        MaterialButton create = new MaterialButton(requireContext());
        create.setText(R.string.pres_btn_agregar_categoria);
        create.setAllCaps(false);
        LinearLayout.LayoutParams createParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        createParams.topMargin = dp(12);
        root.addView(create, createParams);

        Runnable renderAll = () -> renderCategoryPickerRows(list, "", dialog);
        renderAll.run();
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                renderCategoryPickerRows(list, s == null ? "" : s.toString(), dialog);
            }
            @Override public void afterTextChanged(Editable s) { }
        });
        create.setOnClickListener(v -> {
            dialog.dismiss();
            showCategoryManagerDialog();
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

    private void showCategoryManagerDialog() {
        final boolean incomeFlow = isIncomeSelected();
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(12), dp(20), dp(8));

        TextInputEditText etName = new TextInputEditText(requireContext());
        etName.setHint(getString(incomeFlow ? R.string.category_new_income : R.string.category_new_expense));
        etName.setSingleLine(true);
        root.addView(etName, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        MaterialButton btnCreate = new MaterialButton(requireContext());
        btnCreate.setText(R.string.pres_btn_agregar_categoria);
        LinearLayout.LayoutParams createParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        createParams.topMargin = dp(10);
        root.addView(btnCreate, createParams);

        LinearLayout list = new LinearLayout(requireContext());
        list.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams listParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        listParams.topMargin = dp(12);
        root.addView(list, listParams);

        Runnable render = () -> renderCategoryRows(list, incomeFlow);
        render.run();

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(incomeFlow ? R.string.category_manage_income : R.string.category_manage_expense)
                .setView(root)
                .setNegativeButton(android.R.string.cancel, null)
                .create();

        btnCreate.setOnClickListener(v -> {
            String name = etName.getText() == null ? "" : etName.getText().toString().trim();
            if (name.isEmpty()) {
                etName.setError(getString(incomeFlow ? R.string.category_new_income : R.string.category_new_expense));
                return;
            }
            btnCreate.setEnabled(false);
            CategoryStore.createCategoria(requireContext(), name, incomeFlow, new CategoryStore.CreateCallback() {
                @Override
                public void onReady(@NonNull Categoria categoria) {
                    if (!isAdded()) return;
                    btnCreate.setEnabled(true);
                    etName.setText("");
                    CategoryStore.clearCache();
                    cargarCategoriasYRefrescar();
                    UiFormUtils.showMessage(requireView(), R.string.category_saved);
                    CategoryStore.loadOnce(requireContext(), new CategoryStore.Callback() {
                        @Override
                        public void onReady(List<? extends Categoria> cats) {
                            categorias = new ArrayList<>(cats);
                            aplicarFiltroYRefrescar(incomeFlow, true, categoria.nombre);
                            render.run();
                        }

                        @Override
                        public void onError() { }
                    });
                }

                @Override
                public void onError() {
                    if (!isAdded()) return;
                    btnCreate.setEnabled(true);
                    UiFormUtils.showMessage(requireView(), R.string.category_save_error);
                }
            });
        });

        dialog.show();
    }

    private void renderCategoryRows(@NonNull LinearLayout list, boolean incomeFlow) {
        list.removeAllViews();
        if (categorias == null) return;
        for (Categoria categoria : categorias) {
            if (categoria == null || categoria.esIngreso != incomeFlow || isSpecialCategory(categoria)) continue;

            LinearLayout row = new LinearLayout(requireContext());
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, dp(6), 0, dp(6));

            TextView name = new TextView(requireContext());
            name.setText(categoria.nombre);
            name.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurface));
            name.setTextSize(15f);
            row.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            if (categoria.editable) {
                MaterialButton edit = new MaterialButton(requireContext());
                edit.setText(R.string.category_edit);
                edit.setMinHeight(dp(40));
                edit.setOnClickListener(v -> showEditCategoryDialog(categoria, incomeFlow, () ->
                        reloadCategoriesForManager(list, incomeFlow, categoria.nombre)));
                row.addView(edit, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                ));

                MaterialButton delete = new MaterialButton(requireContext());
                delete.setText(R.string.btn_eliminar);
                delete.setMinHeight(dp(40));
                delete.setOnClickListener(v -> confirmDeleteCategory(categoria, incomeFlow, () ->
                        reloadCategoriesForManager(list, incomeFlow, null)));
                LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                deleteParams.leftMargin = dp(6);
                row.addView(delete, deleteParams);
            } else {
                TextView protectedLabel = new TextView(requireContext());
                protectedLabel.setText(R.string.category_protected);
                protectedLabel.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurfaceVariant));
                protectedLabel.setTextSize(12f);
                row.addView(protectedLabel);
            }

            list.addView(row, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
        }
    }

    private void reloadCategoriesForManager(@NonNull LinearLayout list, boolean incomeFlow, @Nullable String preferred) {
        CategoryStore.loadOnce(requireContext(), new CategoryStore.Callback() {
            @Override
            public void onReady(List<? extends Categoria> cats) {
                categorias = new ArrayList<>(cats);
                aplicarFiltroYRefrescar(incomeFlow, true, preferred);
                renderCategoryRows(list, incomeFlow);
            }

            @Override
            public void onError() { }
        });
    }

    private void showEditCategoryDialog(@NonNull Categoria categoria, boolean incomeFlow, @Nullable Runnable afterChange) {
        TextInputEditText input = new TextInputEditText(requireContext());
        input.setSingleLine(true);
        input.setText(categoria.nombre);
        input.setSelectAllOnFocus(true);
        input.setPadding(dp(20), dp(8), dp(20), dp(8));

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.category_edit)
                .setView(input)
                .setPositiveButton(R.string.btn_guardar, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = input.getText() == null ? "" : input.getText().toString().trim();
            if (name.isEmpty()) {
                input.setError(getString(R.string.category_save_error));
                return;
            }
            Categoria updated = new Categoria(categoria.id, categoria.userId, name, incomeFlow, categoria.editable);
            CategoryStore.updateCategoria(requireContext(), updated, new CategoryStore.SimpleCallback() {
                @Override
                public void onSuccess() {
                    if (!isAdded()) return;
                    dialog.dismiss();
                    UiFormUtils.showMessage(requireView(), R.string.category_saved);
                    CategoryStore.clearCache();
                    cargarCategoriasYRefrescar();
                    if (afterChange != null) afterChange.run();
                }

                @Override
                public void onError() {
                    if (isAdded()) UiFormUtils.showMessage(requireView(), R.string.category_save_error);
                }
            });
        }));
        dialog.show();
    }

    private void confirmDeleteCategory(@NonNull Categoria categoria, boolean incomeFlow, @Nullable Runnable afterChange) {
        new MaterialAlertDialogBuilder(requireContext())
                .setMessage(R.string.category_delete_confirm)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.btn_eliminar, (dialog, which) ->
                        CategoryStore.deleteCategoria(requireContext(), categoria.id, new CategoryStore.SimpleCallback() {
                            @Override
                            public void onSuccess() {
                                if (!isAdded()) return;
                                UiFormUtils.showMessage(requireView(), R.string.category_deleted);
                                CategoryStore.clearCache();
                                cargarCategoriasYRefrescar();
                                if (afterChange != null) afterChange.run();
                            }

                            @Override
                            public void onError() {
                                if (isAdded()) UiFormUtils.showMessage(requireView(), R.string.category_in_use_error);
                            }
                        }))
                .show();
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

        String nombreSel = actCategoria.getText() == null ? "" : actCategoria.getText().toString().trim();
        Categoria seleccionada = null;
        if (visibles != null) {
            for (Categoria c : visibles) {
                if (c != null && c.nombre != null && c.nombre.equals(nombreSel)) { seleccionada = c; break; }
            }
        }
        if (seleccionada == null) {
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
        final String notaLocal = etNota.getText() == null ? "" : etNota.getText().toString().trim();
        final Categoria catSel = seleccionada;
        final Integer editingIdLocal = editingId;

        UiFormUtils.setActionLoading(btnGuardar, true);

        if (editingIdLocal == null || editingIdLocal < 0) {
            TransService.create(requireContext(), catSel.id, esIngresoLocal, montoLocal, notaLocal, fechaMs, monedaLocal, accountTypeLocal,
                    new TransService.SimpleCb() {
                        @Override public void onOk(int newId) {
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
