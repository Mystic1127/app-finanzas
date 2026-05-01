package com.example.finanzas.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.finanzas.R;
import com.example.finanzas.data.api.CategoryStore;
import com.example.finanzas.data.api.SettingsService;
import com.example.finanzas.data.api.SuggestionService;
import com.example.finanzas.data.api.TransService;
import com.example.finanzas.data.model.CategorySuggestion;
import com.example.finanzas.data.model.Categoria;
import com.example.finanzas.util.CurrencyConverter;
import com.example.finanzas.util.UiFormUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

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
        chipSugerencia = v.findViewById(R.id.chipSugerencia);
        setupCurrencySelector();
        setupAccountTypeSelector();
        updateCurrencyPrefix();

        if (etFecha != null) UiFormUtils.bindDatePicker(requireContext(), etFecha);
        if (etHora != null) UiFormUtils.bindTimePicker(requireContext(), etHora);
        UiFormUtils.clearErrorOnTextChange(etMonto, etFecha, etHora, actCategoria, actAccountType);

        actCategoria.setOnFocusChangeListener((view, hasFocus) -> { if (hasFocus) actCategoria.showDropDown(); });
        actCategoria.setOnClickListener(view -> actCategoria.showDropDown());

        catAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, new ArrayList<>());
        actCategoria.setAdapter(catAdapter);

        swTipo.setOnCheckedChangeListener(switchListener);

        btnSugerir.setOnClickListener(v1 -> solicitarSugerencia());
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
                android.R.layout.simple_list_item_1,
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
                android.R.layout.simple_list_item_1,
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
        for (Categoria c : categorias) if (c != null && c.esIngreso == esIngreso) visibles.add(c);

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
                currentSuggestion = suggestion;
                if (suggestion != null && suggestion.hasSuggestion()) {
                    int porcentaje = calcularPorcentaje(suggestion.getConfidence());
                    String nombre = suggestion.getCategoriaNombre() == null ? "" : suggestion.getCategoriaNombre();
                    chipSugerencia.setText(getString(R.string.suggest_chip_with_confidence, nombre, porcentaje));
                    chipSugerencia.setVisibility(View.VISIBLE);
                    pendingSuggestedCategoryId = suggestion.getCategoriaId();
                    tryApplyPendingSuggestion();
                } else if (suggestion != null && !TextUtils.isEmpty(suggestion.getMotivo())) {
                    chipSugerencia.setText(getString(R.string.suggest_chip_message, suggestion.getMotivo()));
                    chipSugerencia.setVisibility(View.VISIBLE);
                    pendingSuggestedCategoryId = null;
                } else {
                    chipSugerencia.setVisibility(View.GONE);
                    pendingSuggestedCategoryId = null;
                }
            }

            @Override
            public void onError() {
                if (!isAdded()) return;
                btnSugerir.setEnabled(true);
                btnSugerir.setText(R.string.suggest_category);
                UiFormUtils.showMessage(requireView(), R.string.suggest_error);
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

}
