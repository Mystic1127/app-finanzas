package com.example.finanzas.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.finanzas.R;
import com.example.finanzas.data.api.CategoryStore;
import com.example.finanzas.data.api.ImportService;
import com.example.finanzas.data.model.Categoria;
import com.example.finanzas.data.model.ImportJob;
import com.example.finanzas.data.model.ImportRule;
import com.example.finanzas.ui.adapter.ImportJobAdapter;
import com.example.finanzas.ui.adapter.ImportRuleAdapter;
import com.example.finanzas.util.Prefs;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ImportFragment extends Fragment implements ImportJobAdapter.Listener, ImportRuleAdapter.Listener {

    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipe;
    private TextView tvImportsEmpty;
    private TextView tvRulesEmpty;
    private ImportJobAdapter jobAdapter;
    private ImportRuleAdapter ruleAdapter;
    private final List<Categoria> categorias = new ArrayList<>();
    private final Map<Integer, String> categoriaNombres = new HashMap<>();
    private final Map<Boolean, List<Categoria>> categoriasPorTipo = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_imports, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        swipe = view.findViewById(R.id.swipeImports);
        tvImportsEmpty = view.findViewById(R.id.tvImportsEmpty);
        tvRulesEmpty = view.findViewById(R.id.tvRulesEmpty);

        androidx.recyclerview.widget.RecyclerView rvImports = view.findViewById(R.id.rvImports);
        androidx.recyclerview.widget.RecyclerView rvRules = view.findViewById(R.id.rvRules);
        rvImports.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvRules.setLayoutManager(new LinearLayoutManager(requireContext()));

        jobAdapter = new ImportJobAdapter(this);
        ruleAdapter = new ImportRuleAdapter(this);
        rvImports.setAdapter(jobAdapter);
        rvRules.setAdapter(ruleAdapter);

        swipe.setOnRefreshListener(this::refreshAll);
        view.findViewById(R.id.btnNewImport).setOnClickListener(v -> showCreateDialog());
        view.findViewById(R.id.btnAddRule).setOnClickListener(v -> showRuleDialog(null));

        cargarCategorias();
        refreshAll();
    }

    private void cargarCategorias() {
        CategoryStore.loadOnce(requireContext(), new CategoryStore.Callback() {
            @Override
            public void onReady(List<? extends Categoria> cats) {
                categorias.clear();
                categoriaNombres.clear();
                categoriasPorTipo.clear();
                if (cats != null) {
                    categorias.addAll(cats);
                    for (Categoria c : cats) {
                        categoriaNombres.put(c.id, c.nombre);
                        boolean key = c.esIngreso;
                        if (!categoriasPorTipo.containsKey(key)) {
                            categoriasPorTipo.put(key, new ArrayList<>());
                        }
                        categoriasPorTipo.get(key).add(c);
                    }
                }
                ruleAdapter.setCategoryNames(categoriaNombres);
            }

            @Override
            public void onError() {
                Toast.makeText(requireContext(), R.string.error_cargar_categorias, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void refreshAll() {
        swipe.setRefreshing(true);
        ImportService.list(requireContext(), new ImportService.ListCallback() {
            @Override
            public void onSuccess(List<? extends ImportJob> items) {
                if (!isAdded()) return;
                swipe.setRefreshing(false);
                jobAdapter.setItems(new ArrayList<>(items));
                tvImportsEmpty.setVisibility(items == null || items.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError() {
                if (!isAdded()) return;
                swipe.setRefreshing(false);
                Toast.makeText(requireContext(), R.string.import_list_error, Toast.LENGTH_SHORT).show();
            }
        });

        ImportService.listRules(requireContext(), new ImportService.RulesCallback() {
            @Override
            public void onSuccess(List<? extends ImportRule> rules) {
                if (!isAdded()) return;
                ruleAdapter.setItems(new ArrayList<>(rules));
                tvRulesEmpty.setVisibility(rules == null || rules.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError() {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), R.string.import_rules_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showCreateDialog() {
        View content = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_import, null, false);
        TextInputLayout tilNombre = content.findViewById(R.id.tilImportName);
        TextInputLayout tilLineas = content.findViewById(R.id.tilImportLines);
        TextInputEditText etNombre = content.findViewById(R.id.etImportName);
        TextInputEditText etLineas = content.findViewById(R.id.etImportLines);
        MaterialAutoCompleteTextView actTipo = content.findViewById(R.id.actImportType);
        MaterialButton btnCancel = content.findViewById(R.id.btnImportCancel);
        MaterialButton btnCreate = content.findViewById(R.id.btnImportCreate);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), R.layout.item_dropdown,
                getResources().getStringArray(R.array.import_type_entries));
        actTipo.setAdapter(adapter);
        actTipo.setText(getString(R.string.import_type_csv), false);

        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        dialog.setContentView(content);
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnCreate.setOnClickListener(v -> {
                tilNombre.setError(null);
                tilLineas.setError(null);

                String nombre = etNombre.getText() == null ? "" : etNombre.getText().toString().trim();
                String tipo = actTipo.getText() == null ? "" : actTipo.getText().toString().trim();
                String lineasRaw = etLineas.getText() == null ? "" : etLineas.getText().toString().trim();

                if (TextUtils.isEmpty(nombre)) {
                    tilNombre.setError(getString(R.string.import_name_error));
                    return;
                }
                JSONArray lineas;
                try {
                    lineas = parseLineas(lineasRaw);
                } catch (IllegalArgumentException e) {
                    tilLineas.setError(e.getMessage());
                    return;
                }

                btnCreate.setEnabled(false);
                ImportService.create(requireContext(), mapTipo(tipo), nombre, lineas, new ImportService.CreateCallback() {
                    @Override
                    public void onSuccess(int importId) {
                        if (!isAdded()) return;
                        btnCreate.setEnabled(true);
                        dialog.dismiss();
                        Toast.makeText(requireContext(), getString(R.string.import_created_ok, importId), Toast.LENGTH_SHORT).show();
                        refreshAll();
                    }

                    @Override
                    public void onError() {
                        if (!isAdded()) return;
                        btnCreate.setEnabled(true);
                        Toast.makeText(requireContext(), R.string.import_create_error, Toast.LENGTH_SHORT).show();
                    }
                });
        });

        dialog.show();
    }

    private JSONArray parseLineas(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new IllegalArgumentException(getString(R.string.import_lines_required));
        }
        String[] filas = raw.split("\n");
        JSONArray array = new JSONArray();
        for (String fila : filas) {
            String limpia = fila.trim();
            if (limpia.isEmpty()) continue;
            String[] partes = limpia.split(";");
            if (partes.length < 3) {
                throw new IllegalArgumentException(getString(R.string.import_line_format_error));
            }
            String fecha = partes[0].trim();
            String descripcion = partes[1].trim();
            String montoStr = partes[2].trim().replace(",", ".");
            double monto;
            try {
                monto = Double.parseDouble(montoStr);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(getString(R.string.import_line_amount_error));
            }
            boolean ingreso = monto >= 0;
            if (partes.length >= 4) {
                String tipo = partes[3].trim().toUpperCase(Locale.ROOT);
                ingreso = tipo.startsWith("I") || "1".equals(tipo) || "+".equals(tipo);
            }

            try {
                JSONObject linea = new JSONObject();
                linea.put("fecha", fecha);
                linea.put("descripcion", descripcion);
                linea.put("monto", Math.abs(monto));
                linea.put("es_ingreso", ingreso ? 1 : 0);
                array.put(linea);
            } catch (JSONException e) {
                // Algo raro al construir el JSON → lo tratamos como error de formato
                throw new IllegalArgumentException(getString(R.string.import_line_format_error));
            }
        }
        if (array.length() == 0) {
            throw new IllegalArgumentException(getString(R.string.import_lines_required));
        }
        return array;
    }

    private String mapTipo(String texto) {
        if (texto == null) return "csv";
        if (texto.toLowerCase(Locale.ROOT).contains("excel")) {
            return "excel";
        }
        return "csv";
    }

    private void showRuleDialog(@Nullable ImportRule existing) {
        View content = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_rule, null, false);
        TextView tvTitle = content.findViewById(R.id.tvRuleSheetTitle);
        TextInputLayout tilPattern = content.findViewById(R.id.tilRulePattern);
        TextInputLayout tilDescripcion = content.findViewById(R.id.tilRuleDesc);
        TextInputLayout tilCategoria = content.findViewById(R.id.tilRuleCategory);
        TextInputEditText etPattern = content.findViewById(R.id.etRulePattern);
        TextInputEditText etDesc = content.findViewById(R.id.etRuleDesc);
        MaterialAutoCompleteTextView actCategoria = content.findViewById(R.id.actRuleCategory);
        SwitchMaterial swTipo = content.findViewById(R.id.swRuleIngreso);
        MaterialButton btnCancel = content.findViewById(R.id.btnRuleCancel);
        MaterialButton btnSave = content.findViewById(R.id.btnRuleSave);
        tvTitle.setText(existing == null ? R.string.import_rule_new_title : R.string.import_rule_edit_title);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), R.layout.item_dropdown,
                buildCategoriaLabels(false));
        actCategoria.setAdapter(adapter);
        actCategoria.setOnClickListener(v -> actCategoria.showDropDown());
        actCategoria.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) actCategoria.showDropDown(); });

        swTipo.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ArrayAdapter<String> nuevo = new ArrayAdapter<>(requireContext(), R.layout.item_dropdown,
                    buildCategoriaLabels(isChecked));
            actCategoria.setAdapter(nuevo);
            actCategoria.setText("", false);
        });

        if (existing != null) {
            etPattern.setText(existing.getPatron());
            if (!TextUtils.isEmpty(existing.getDescripcion())) {
                etDesc.setText(existing.getDescripcion());
            }
            swTipo.setChecked(existing.isIngreso());
            if (existing.getCategoriaId() != null) {
                String nombre = categoriaNombres.get(existing.getCategoriaId());
                if (nombre != null) {
                    actCategoria.setText(nombre, false);
                } else {
                    actCategoria.setText(String.valueOf(existing.getCategoriaId()), false);
                }
            }
        }

        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        dialog.setContentView(content);
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSave.setOnClickListener(v -> {
                tilPattern.setError(null);
                tilCategoria.setError(null);

                String patron = etPattern.getText() == null ? "" : etPattern.getText().toString().trim();
                if (patron.isEmpty()) {
                    tilPattern.setError(getString(R.string.import_rule_pattern_error));
                    return;
                }

                ImportRule rule = existing == null ? new ImportRule() : existing;
                rule.setPatron(patron);
                rule.setDescripcion(etDesc.getText() == null ? null : etDesc.getText().toString().trim());
                rule.setIngreso(swTipo.isChecked());

                String categoriaSel = actCategoria.getText() == null ? "" : actCategoria.getText().toString().trim();
                Integer categoriaId = resolveCategoriaId(categoriaSel, swTipo.isChecked());
                rule.setCategoriaId(categoriaId);

                btnSave.setEnabled(false);
                ImportService.saveRule(requireContext(), rule, new ImportService.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        if (!isAdded()) return;
                        btnSave.setEnabled(true);
                        dialog.dismiss();
                        Toast.makeText(requireContext(), R.string.import_rule_saved, Toast.LENGTH_SHORT).show();
                        refreshAll();
                    }

                    @Override
                    public void onError() {
                        if (!isAdded()) return;
                        btnSave.setEnabled(true);
                        Toast.makeText(requireContext(), R.string.import_rule_save_error, Toast.LENGTH_SHORT).show();
                    }
                });
        });

        dialog.show();
    }

    private List<String> buildCategoriaLabels(boolean ingreso) {
        List<Categoria> lista = categoriasPorTipo.getOrDefault(ingreso, new ArrayList<>());
        List<String> nombres = new ArrayList<>();
        nombres.add(getString(R.string.import_rule_any_category));
        for (Categoria c : lista) {
            nombres.add(c.nombre);
        }
        return nombres;
    }

    private Integer resolveCategoriaId(String label, boolean ingreso) {
        if (TextUtils.isEmpty(label) || label.equals(getString(R.string.import_rule_any_category))) {
            return null;
        }
        List<Categoria> lista = categoriasPorTipo.getOrDefault(ingreso, new ArrayList<>());
        for (Categoria c : lista) {
            if (label.equalsIgnoreCase(c.nombre)) {
                return c.id;
            }
        }
        for (Map.Entry<Integer, String> entry : categoriaNombres.entrySet()) {
            if (label.equalsIgnoreCase(entry.getValue())) {
                return entry.getKey();
            }
        }
        try {
            return Integer.parseInt(label);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public void onProcess(ImportJob job) {
        ImportService.process(requireContext(), job.getId(), new ImportService.ProcessCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                if (!isAdded()) return;
                refreshAll();

                int procesados = response != null ? response.optInt("procesados", 0) : 0;
                int errores = response != null ? response.optInt("errores", 0) : 0;
                Toast.makeText(requireContext(),
                        getString(R.string.import_process_completed, procesados, errores),
                        Toast.LENGTH_LONG).show();

                if (response != null) {
                    int omitidosSaldo = response.optInt("omitidos_saldo", 0);
                    if (omitidosSaldo > 0) {
                        Toast.makeText(requireContext(),
                                getResources().getQuantityString(R.plurals.import_process_skipped_balance,
                                        omitidosSaldo, omitidosSaldo),
                                Toast.LENGTH_LONG).show();
                    }

                    String mensaje = response.optString("mensaje", "");
                    if (!mensaje.isEmpty()) {
                        Toast.makeText(requireContext(), mensaje, Toast.LENGTH_LONG).show();
                    }

                    int anio = response.optInt("anio_sugerido", 0);
                    int mes = response.optInt("mes_sugerido", 0);
                    if (anio > 0 && mes > 0) {
                        Prefs.setLastTransactionsPeriod(requireContext(), anio, mes);
                    }
                }
            }

            @Override
            public void onError() {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), R.string.import_process_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onRuleSelected(ImportRule rule) {
        showRuleDialog(rule);
    }

    @Override
    public void onRuleLongPress(ImportRule rule) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.import_rule_delete_title)
                .setMessage(R.string.import_rule_delete_confirm)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.import_rule_delete, (dialog, which) ->
                        ImportService.deleteRule(requireContext(), rule.getId(), new ImportService.SimpleCallback() {
                            @Override
                            public void onSuccess() {
                                if (!isAdded()) return;
                                Toast.makeText(requireContext(), R.string.import_rule_deleted, Toast.LENGTH_SHORT).show();
                                refreshAll();
                            }

                            @Override
                            public void onError() {
                                if (!isAdded()) return;
                                Toast.makeText(requireContext(), R.string.import_rule_delete_error, Toast.LENGTH_SHORT).show();
                            }
                        }))
                .show();
    }
}
