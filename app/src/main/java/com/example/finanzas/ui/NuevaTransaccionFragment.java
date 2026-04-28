package com.example.finanzas.ui;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.finanzas.R;
import com.example.finanzas.data.api.CategoryStore;
import com.example.finanzas.data.api.SuggestionService;
import com.example.finanzas.data.api.TransService;
import com.example.finanzas.data.model.CategorySuggestion;
import com.example.finanzas.data.model.Categoria;
import com.example.finanzas.util.DateInputMask;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;

import java.text.ParseException;
import java.text.SimpleDateFormat;
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

    private TextInputEditText etMonto, etNota, etFecha;
    private MaterialSwitch swTipo;
    private MaterialAutoCompleteTextView actCategoria;
    private MaterialButton btnGuardar;
    private MaterialButton btnSugerir;
    private Chip chipSugerencia;

    private Integer editingId = null;
    private List<Categoria> categorias;
    private List<Categoria> visibles;
    private ArrayAdapter<String> catAdapter;
    private CategorySuggestion currentSuggestion;
    private Integer pendingSuggestedCategoryId;
    private final SimpleDateFormat inputDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
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
        etNota        = v.findViewById(R.id.etNota);
        etFecha       = v.findViewById(R.id.etFecha);
        swTipo        = v.findViewById(R.id.swTipo);
        actCategoria  = v.findViewById(R.id.actCategoria);
        btnGuardar    = v.findViewById(R.id.btnGuardar);
        btnSugerir    = v.findViewById(R.id.btnSugerir);
        chipSugerencia = v.findViewById(R.id.chipSugerencia);

        if (etFecha != null) {
            etFecha.setFilters(new InputFilter[]{new InputFilter.LengthFilter(10)});
            etFecha.addTextChangedListener(new DateInputMask(etFecha));
            setupDatePicker(etFecha);
        }

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
            etFecha.setText(inputDateFormat.format(new Date()));
        }

        btnGuardar.setOnClickListener(this::onGuardar);
    }

    private void setupDatePicker(@NonNull TextInputEditText input) {
        input.setFocusable(false);
        input.setOnClickListener(v -> showDatePicker(input));
        input.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) showDatePicker(input); });
    }

    private void showDatePicker(@NonNull TextInputEditText input) {
        Calendar calendar = Calendar.getInstance();
        String current = input.getText() == null ? "" : input.getText().toString();
        try {
            Date parsed = inputDateFormat.parse(current);
            if (parsed != null) calendar.setTime(parsed);
        } catch (ParseException ignored) { }

        DatePickerDialog dialog = new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            input.setText(inputDateFormat.format(calendar.getTime()));
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

        dialog.show();
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
                Toast.makeText(requireContext(), R.string.error_cargar_categorias, Toast.LENGTH_SHORT).show();
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
                Toast.makeText(requireContext(), R.string.suggest_error, Toast.LENGTH_SHORT).show();
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
                if (etFecha != null) etFecha.setText(inputDateFormat.format(new Date(fechaMs)));
            }

            btnGuardar.setText(R.string.btn_guardar);
        } else {
            btnGuardar.setText(R.string.btn_guardar);
            if (etFecha != null) etFecha.setText(inputDateFormat.format(new Date()));
        }
    }

    private void onGuardar(View view) {
        String sMonto = etMonto.getText() == null ? "" : etMonto.getText().toString().trim();
        if (sMonto.isEmpty()) {
            Toast.makeText(requireContext(),"Ingresa un monto",Toast.LENGTH_SHORT).show();
            return;
        }

        String sFecha = etFecha != null && etFecha.getText() != null
                ? etFecha.getText().toString().trim() : "";
        Date fechaSeleccionada = parseFechaSegura(sFecha);
        if (fechaSeleccionada == null) {
            Toast.makeText(requireContext(), R.string.error_formato_fecha, Toast.LENGTH_SHORT).show();
            return;
        }
        final long fechaMs = fechaSeleccionada.getTime();

        String nombreSel = actCategoria.getText() == null ? "" : actCategoria.getText().toString().trim();
        Categoria seleccionada = null;
        if (visibles != null) {
            for (Categoria c : visibles) {
                if (c != null && c.nombre != null && c.nombre.equals(nombreSel)) { seleccionada = c; break; }
            }
        }
        if (seleccionada == null) {
            Toast.makeText(requireContext(),"Selecciona una categoría",Toast.LENGTH_SHORT).show();
            actCategoria.requestFocus(); actCategoria.showDropDown();
            return;
        }

        final double montoLocal = Math.abs(parseMontoSeguro(sMonto));
        if (montoLocal <= 0) {
            Toast.makeText(requireContext(),"Monto inválido",Toast.LENGTH_SHORT).show();
            return;
        }

        final boolean esIngresoLocal = swTipo.isChecked();
        final String notaLocal = etNota.getText() == null ? "" : etNota.getText().toString().trim();
        final Categoria catSel = seleccionada;
        final Integer editingIdLocal = editingId;

        btnGuardar.setEnabled(false);

        if (editingIdLocal == null || editingIdLocal < 0) {
            TransService.create(requireContext(), catSel.id, esIngresoLocal, montoLocal, notaLocal, fechaMs,
                    new TransService.SimpleCb() {
                        @Override public void onOk(int newId) {
                            btnGuardar.setEnabled(true);
                            Toast.makeText(requireContext(), R.string.trans_saved, Toast.LENGTH_SHORT).show();
                            NavHostFragment.findNavController(NuevaTransaccionFragment.this).popBackStack();
                        }
                        @Override public void onError(@Nullable String message) {
                            btnGuardar.setEnabled(true);
                            if (message != null && !message.isEmpty()) {
                                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(requireContext(), R.string.error_guardar_transaccion, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

        } else {
            TransService.update(requireContext(), editingIdLocal, catSel.id, esIngresoLocal, montoLocal, notaLocal, fechaMs,
                    new TransService.VoidCb() {
                        @Override public void onOk() {
                            btnGuardar.setEnabled(true);
                            Toast.makeText(requireContext(), R.string.trans_updated, Toast.LENGTH_SHORT).show();
                            NavHostFragment.findNavController(NuevaTransaccionFragment.this).popBackStack();
                        }
                        @Override public void onError(@Nullable String message) {
                            btnGuardar.setEnabled(true);
                            if (message != null && !message.isEmpty()) {
                                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(requireContext(), R.string.error_guardar_transaccion, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }

    private Date parseFechaSegura(String raw) {
        if (raw == null || raw.length() != 10) return null;
        try {
            inputDateFormat.setLenient(false);
            return inputDateFormat.parse(raw);
        } catch (ParseException e) {
            return null;
        }
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

}
