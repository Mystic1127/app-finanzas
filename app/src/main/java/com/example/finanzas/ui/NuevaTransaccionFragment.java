package com.example.finanzas.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
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
import com.example.finanzas.data.model.Transaccion;
import com.example.finanzas.util.CurrencyConverter;
import com.example.finanzas.util.UiFormUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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

    private TextInputEditText etMonto, etNota, etFecha, etHora;
    private TextInputLayout tilMonto, tilFecha, tilHora, tilCategoria, tilMoneda, tilAccountType;
    private MaterialSwitch swTipo;
    private MaterialAutoCompleteTextView actCategoria, actMoneda, actAccountType;
    private MaterialButton btnGuardar;
    private MaterialButton btnSugerir;
    private MaterialButton btnManageCategories;
    private Chip chipSugerencia;

    private Integer editingId = null;
    private List<Categoria> categorias;
    private List<Categoria> visibles;
    private ArrayAdapter<String> catAdapter;
    private CategorySuggestion currentSuggestion;
    private Integer pendingSuggestedCategoryId;
    private final CompoundButton.OnCheckedChangeListener switchListener = (buttonView, isChecked) ->
            aplicarFiltroYRefrescar(isChecked, false, null);

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
        tilMonto      = v.findViewById(R.id.tilMonto);
        tilFecha      = v.findViewById(R.id.tilFecha);
        tilHora       = v.findViewById(R.id.tilHora);
        tilCategoria  = v.findViewById(R.id.tilCategoria);
        tilMoneda     = v.findViewById(R.id.tilMoneda);
        tilAccountType = v.findViewById(R.id.tilAccountType);
        etNota        = v.findViewById(R.id.etNota);
        etFecha       = v.findViewById(R.id.etFecha);
        etHora        = v.findViewById(R.id.etHora);
        swTipo        = v.findViewById(R.id.swTipo);
        actCategoria  = v.findViewById(R.id.actCategoria);
        actMoneda     = v.findViewById(R.id.actMoneda);
        actAccountType = v.findViewById(R.id.actAccountType);
        btnGuardar    = v.findViewById(R.id.btnGuardar);
        btnSugerir    = v.findViewById(R.id.btnSugerir);
        btnManageCategories = v.findViewById(R.id.btnManageCategories);
        chipSugerencia = v.findViewById(R.id.chipSugerencia);
        setupCurrencySelector();
        setupAccountTypeSelector();
        updateCurrencyPrefix();

        if (etFecha != null) UiFormUtils.bindDatePicker(requireContext(), etFecha);
        if (etHora != null) UiFormUtils.bindTimePicker(requireContext(), etHora);
        UiFormUtils.clearErrorOnTextChange(etMonto, etFecha, etHora, actCategoria, actAccountType);

        actCategoria.setOnFocusChangeListener((view, hasFocus) -> { if (hasFocus) actCategoria.showDropDown(); });
        actCategoria.setOnClickListener(view -> actCategoria.showDropDown());

        catAdapter = new ArrayAdapter<>(requireContext(), R.layout.item_dropdown, new ArrayList<>());
        actCategoria.setAdapter(catAdapter);

        swTipo.setOnCheckedChangeListener(switchListener);

        btnSugerir.setOnClickListener(v1 -> solicitarSugerencia());
        btnManageCategories.setOnClickListener(v14 -> showCategoryManagerDialog());
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
    }

    private void updateCurrencyPrefix() {
        if (tilMonto != null && getContext() != null) {
            tilMonto.setPrefixText(SettingsService.getCurrencySymbol(resolveSelectedCurrency()) + " ");
        }
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

    private String resolveSelectedCurrency() {
        String raw = actMoneda == null || actMoneda.getText() == null
                ? SettingsService.getCurrencyCode(requireContext())
                : actMoneda.getText().toString();
        return CurrencyConverter.normalize(raw);
    }

    private void setupAccountTypeSelector() {
        ArrayAdapter<String> accountAdapter = new ArrayAdapter<>(
                requireContext(),
                R.layout.item_dropdown,
                new String[]{getString(R.string.transaction_account_cash), getString(R.string.transaction_account_card)}
        );
        actAccountType.setAdapter(accountAdapter);
        actAccountType.setText(getString(R.string.transaction_account_card), false);
        actAccountType.setOnFocusChangeListener((view, hasFocus) -> { if (hasFocus) actAccountType.showDropDown(); });
        actAccountType.setOnClickListener(view -> actAccountType.showDropDown());
    }

    private String resolveSelectedAccountType() {
        String selected = actAccountType == null || actAccountType.getText() == null ? "" : actAccountType.getText().toString();
        return getString(R.string.transaction_account_cash).equalsIgnoreCase(selected.trim()) ? "CASH" : "CARD";
    }

    private void setSelectedAccountType(@Nullable String accountType) {
        boolean cash = "CASH".equalsIgnoreCase(accountType);
        if (actAccountType != null) {
            actAccountType.setText(getString(cash ? R.string.transaction_account_cash : R.string.transaction_account_card), false);
        }
    }

    private void cargarCategoriasYRefrescar() {
        CategoryStore.loadOnce(requireContext(), new CategoryStore.Callback() {
            @Override
            public void onReady(List<? extends Categoria> cats) {
                categorias = new ArrayList<>(cats);
                boolean esIngreso = swTipo.isChecked();
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

        catAdapter.clear();
        catAdapter.addAll(nombres);
        catAdapter.notifyDataSetChanged();

        String actual = actCategoria.getText() == null ? "" : actCategoria.getText().toString();
        if (preservarSeleccion) {
            if (categoriaDeseada != null && nombres.contains(categoriaDeseada)) {
                actCategoria.setText(categoriaDeseada, false);
            } else if (!actual.isEmpty() && !nombres.contains(actual)) {
                actCategoria.setText("", false);
            }
        } else if (!actual.isEmpty() && !nombres.contains(actual)) {
            actCategoria.setText("", false);
        }
    }

    private void solicitarSugerencia() {
        final double montoActual = parseMontoActual();
        final boolean esIngreso = swTipo.isChecked();
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
        if (swTipo.isChecked() != sugerida.esIngreso) {
            swTipo.setOnCheckedChangeListener(null);
            swTipo.setChecked(sugerida.esIngreso);
            swTipo.setOnCheckedChangeListener(switchListener);
        }
        aplicarFiltroYRefrescar(sugerida.esIngreso, true, sugerida.nombre);
        actCategoria.setText(sugerida.nombre, false);
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
            swTipo.setChecked(esIngreso);

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

    private void showCategoryManagerDialog() {
        final boolean incomeFlow = swTipo.isChecked();
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
            tilMonto.setError(getString(R.string.error_ingresa_monto));
            return;
        }

        tilMonto.setError(null);
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
            actCategoria.requestFocus(); actCategoria.showDropDown();
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
            tilMonto.setError(getString(R.string.error_monto_invalido));
            return;
        }

        tilMonto.setError(null);
        final boolean esIngresoLocal = swTipo.isChecked();
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
