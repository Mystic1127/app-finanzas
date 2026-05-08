package com.example.finanzas.ui;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONException;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import com.example.finanzas.util.ExcelImportParser;
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
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.text.SimpleDateFormat;

public class ImportFragment extends Fragment implements ImportJobAdapter.Listener, ImportRuleAdapter.Listener {

    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipe;
    private TextView tvImportsEmpty;
    private TextView tvRulesEmpty;
    private ImportJobAdapter jobAdapter;
    private ImportRuleAdapter ruleAdapter;
    private final List<Categoria> categorias = new ArrayList<>();
    private final List<ImportRule> importRules = new ArrayList<>();
    private final Map<Integer, String> categoriaNombres = new HashMap<>();
    private final Map<Boolean, List<Categoria>> categoriasPorTipo = new HashMap<>();
    private TextInputEditText activeImportLines;
    private TextView activeImportPreview;

    private final ActivityResultLauncher<String> importFilePicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), this::onImportFileSelected);

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
        maybeShowImportOnboarding(0);
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
                        if (isSpecialCategory(c)) continue;
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
                importRules.clear();
                if (rules != null) importRules.addAll(rules);
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
        TextView tvPreview = content.findViewById(R.id.tvImportPreview);
        MaterialAutoCompleteTextView actTipo = content.findViewById(R.id.actImportType);
        MaterialButton btnSelectFile = content.findViewById(R.id.btnSelectImportFile);
        MaterialButton btnCancel = content.findViewById(R.id.btnImportCancel);
        MaterialButton btnCreate = content.findViewById(R.id.btnImportCreate);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), R.layout.item_dropdown,
                getResources().getStringArray(R.array.import_type_entries));
        actTipo.setAdapter(adapter);
        actTipo.setText(getString(R.string.import_type_csv), false);
        activeImportLines = etLineas;
        activeImportPreview = tvPreview;
        etLineas.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateImportPreview(s == null ? "" : s.toString(), tvPreview);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        btnSelectFile.setOnClickListener(v -> importFilePicker.launch("*/*"));

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
                    lineas = parseLineasV2(lineasRaw);
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

        dialog.setOnDismissListener(d -> {
            activeImportLines = null;
            activeImportPreview = null;
        });
        dialog.show();
    }

    private void updateImportPreview(@Nullable String raw, @NonNull TextView preview) {
        if (raw == null || raw.trim().isEmpty()) {
            preview.setText(R.string.import_preview_empty);
            return;
        }
        try {
            JSONArray rows = parseLineasV2(raw);
            StringBuilder out = new StringBuilder(getString(R.string.import_preview_title, rows.length()));
            int max = Math.min(3, rows.length());
            for (int i = 0; i < max; i++) {
                JSONObject row = rows.getJSONObject(i);
                out.append("\n")
                        .append(row.optString("fecha"))
                        .append(" · ")
                        .append(row.optString("descripcion"))
                        .append(" · ")
                        .append(row.optDouble("monto"))
                        .append(row.optInt("es_ingreso", 0) == 1 ? " · I" : " · G");
            }
            preview.setText(out.toString());
        } catch (Exception e) {
            preview.setText(e.getMessage() == null ? getString(R.string.import_line_format_error) : e.getMessage());
        }
    }

    private void onImportFileSelected(@Nullable Uri uri) {
        if (uri == null || activeImportLines == null) return;
        String rawUri = uri.toString().toLowerCase(Locale.ROOT);
        String mime = requireContext().getContentResolver().getType(uri);
        String cleanMime = mime == null ? "" : mime.toLowerCase(Locale.ROOT);
        boolean excel = rawUri.endsWith(".xlsx") || rawUri.contains("xlsx") || cleanMime.contains("spreadsheet");
        boolean legacyExcel = (rawUri.endsWith(".xls") || rawUri.contains(".xls")) && !rawUri.contains(".xlsx");
        try (InputStream input = requireContext().getContentResolver().openInputStream(uri)) {
            if (input == null) throw new IllegalStateException("empty");
            if (excel && !legacyExcel) {
                JSONArray rows = parseExcelInput(input);
                activeImportLines.setText(rows.toString());
                if (activeImportPreview != null) updateImportPreview(rows.toString(), activeImportPreview);
                Toast.makeText(requireContext(), R.string.import_file_loaded, Toast.LENGTH_SHORT).show();
                return;
            }
            if (legacyExcel) {
                Toast.makeText(requireContext(), R.string.import_file_excel_pending, Toast.LENGTH_LONG).show();
                return;
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (out.length() > 0) out.append('\n');
                out.append(line);
            }
            activeImportLines.setText(out.toString());
            if (activeImportPreview != null) updateImportPreview(out.toString(), activeImportPreview);
            Toast.makeText(requireContext(), R.string.import_file_loaded, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            if (activeImportPreview != null && e.getMessage() != null) {
                activeImportPreview.setText(e.getMessage());
            }
            Toast.makeText(requireContext(), R.string.import_file_error, Toast.LENGTH_SHORT).show();
        }
    }

    private void maybeShowImportOnboarding(int step) {
        if (!isAdded()) return;
        long userId = Prefs.getCurrentUserId(requireContext());
        if (userId <= 0) return;
        String key = "imports_onboarding_seen_" + userId;
        if (requireContext().getSharedPreferences("finanzas_settings", Context.MODE_PRIVATE).getBoolean(key, false)) {
            return;
        }
        int[] titles = {
                R.string.import_onboarding_title_1,
                R.string.import_onboarding_title_2,
                R.string.import_onboarding_title_3
        };
        int[] bodies = {
                R.string.import_onboarding_body_1,
                R.string.import_onboarding_body_2,
                R.string.import_onboarding_body_3
        };
        int current = Math.max(0, Math.min(step, titles.length - 1));
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(titles[current])
                .setMessage(bodies[current])
                .setNegativeButton(R.string.import_onboarding_skip, (dialog, which) -> markImportOnboardingSeen(key));
        if (current < titles.length - 1) {
            builder.setPositiveButton(R.string.import_onboarding_next, (dialog, which) -> maybeShowImportOnboarding(current + 1));
        } else {
            builder.setPositiveButton(R.string.import_onboarding_done, (dialog, which) -> markImportOnboardingSeen(key));
        }
        builder.show();
    }

    private void markImportOnboardingSeen(@NonNull String key) {
        if (!isAdded()) return;
        requireContext().getSharedPreferences("finanzas_settings", Context.MODE_PRIVATE)
                .edit()
                .putBoolean(key, true)
                .apply();
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

    private JSONArray parseLineasV2(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new IllegalArgumentException(getString(R.string.import_lines_required));
        }
        String cleanRaw = raw.trim();
        if (cleanRaw.startsWith("[")) {
            try {
                JSONArray parsed = new JSONArray(cleanRaw);
                if (parsed.length() == 0) throw new IllegalArgumentException(getString(R.string.import_lines_required));
                return parsed;
            } catch (JSONException e) {
                throw new IllegalArgumentException(getString(R.string.import_line_format_error));
            }
        }

        String[] filas = raw.split("\n");
        JSONArray array = new JSONArray();
        List<String> errores = new ArrayList<>();
        boolean hasHeader = filas.length > 0 && looksLikeHeader(filas[0]);
        Map<String, Integer> header = hasHeader ? parseHeader(filas[0]) : new HashMap<>();
        for (int index = hasHeader ? 1 : 0; index < filas.length; index++) {
            String limpia = filas[index].trim();
            if (limpia.isEmpty()) continue;
            String[] partes = splitDelimited(limpia);
            if (partes.length < 3) {
                errores.add("Fila " + (index + 1) + ": formato incompleto");
                continue;
            }
            String fecha = hasHeader ? valueFor(partes, header, "fecha") : partes[0].trim();
            String descripcion = hasHeader ? valueFor(partes, header, "descripcion") : partes[1].trim();
            String montoStr = hasHeader ? valueFor(partes, header, "monto") : partes[2].trim();
            String tipoRaw = hasHeader ? valueFor(partes, header, "tipo") : (partes.length >= 4 ? partes[3].trim() : "");
            String moneda = hasHeader ? valueFor(partes, header, "moneda") : (partes.length >= 5 ? partes[4].trim() : "");
            String categoria = hasHeader ? valueFor(partes, header, "categoria") : (partes.length >= 6 ? partes[5].trim() : "");
            double monto;
            try {
                monto = parseAmount(montoStr);
            } catch (NumberFormatException e) {
                errores.add("Fila " + (index + 1) + ": monto invalido");
                continue;
            }
            boolean ingreso = monto >= 0;
            if (!TextUtils.isEmpty(tipoRaw)) ingreso = parseIncomeType(tipoRaw, ingreso);

            try {
                JSONObject linea = buildImportJson(fecha, descripcion, Math.abs(monto), ingreso, moneda, categoria);
                if (linea == null) {
                    errores.add("Fila " + (index + 1) + ": faltan fecha, descripcion o monto");
                } else {
                    array.put(linea);
                }
            } catch (JSONException e) {
                errores.add("Fila " + (index + 1) + ": formato invalido");
            }
        }
        if (array.length() == 0) {
            throw new IllegalArgumentException(errores.isEmpty()
                    ? getString(R.string.import_lines_required)
                    : TextUtils.join("\n", errores.subList(0, Math.min(3, errores.size()))));
        }
        return array;
    }

    private JSONArray parseExcelInput(@NonNull InputStream input) throws Exception {
        List<Map<String, String>> rows = ExcelImportParser.parseFirstSheet(input);
        if (rows.isEmpty()) throw new IllegalArgumentException(getString(R.string.import_lines_required));

        JSONArray out = new JSONArray();
        List<String> errors = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            Map<String, String> row = normalizeKeys(rows.get(i));
            String fecha = first(row, "fecha", "date", "dia", "vencimiento");
            String descripcion = first(row, "descripcion", "description", "detalle", "concepto", "nota", "comercio");
            String montoRaw = first(row, "monto", "importe", "amount", "valor", "total");
            String tipoRaw = first(row, "tipo", "type", "movimiento", "ingresogasto", "ingresoegreso");
            String moneda = first(row, "moneda", "currency", "divisa");
            String categoria = first(row, "categoria", "category", "rubro");
            if (TextUtils.isEmpty(fecha) || TextUtils.isEmpty(descripcion) || TextUtils.isEmpty(montoRaw)) {
                errors.add("Fila " + (i + 2) + ": faltan fecha, descripcion o monto");
                continue;
            }
            double amount;
            try {
                amount = parseAmount(montoRaw);
            } catch (NumberFormatException e) {
                errors.add("Fila " + (i + 2) + ": monto invalido");
                continue;
            }
            boolean ingreso = amount >= 0;
            if (!TextUtils.isEmpty(tipoRaw)) ingreso = parseIncomeType(tipoRaw, ingreso);

            JSONObject json = buildImportJson(fecha, descripcion, Math.abs(amount), ingreso, moneda, categoria);
            if (json == null) {
                errors.add("Fila " + (i + 2) + ": datos invalidos");
            } else {
                out.put(json);
            }
        }
        if (out.length() == 0) {
            throw new IllegalArgumentException(errors.isEmpty()
                    ? getString(R.string.import_line_format_error)
                    : TextUtils.join("\n", errors.subList(0, Math.min(3, errors.size()))));
        }
        return out;
    }

    @Nullable
    private JSONObject buildImportJson(@Nullable String fecha, @Nullable String descripcion, double monto,
                                       boolean ingreso, @Nullable String moneda, @Nullable String categoria) throws JSONException {
        String cleanFecha = normalizeDate(fecha);
        String cleanDesc = descripcion == null ? "" : descripcion.trim();
        if (TextUtils.isEmpty(cleanFecha) || TextUtils.isEmpty(cleanDesc) || monto <= 0.0) return null;

        ImportRule rule = findMatchingRule(cleanDesc);
        boolean finalIngreso = rule != null ? rule.isIngreso() : ingreso;
        Integer categoriaId = rule != null && rule.getCategoriaId() != null
                ? rule.getCategoriaId()
                : resolveCategoriaId(categoria == null ? "" : categoria.trim(), finalIngreso);

        JSONObject linea = new JSONObject();
        linea.put("fecha", cleanFecha);
        linea.put("descripcion", cleanDesc);
        linea.put("monto", monto);
        linea.put("es_ingreso", finalIngreso ? 1 : 0);
        if (!TextUtils.isEmpty(moneda)) linea.put("moneda", moneda.trim().toUpperCase(Locale.ROOT));
        if (categoriaId != null) linea.put("categoria_id", categoriaId);
        if (!TextUtils.isEmpty(categoria)) linea.put("categoria", categoria.trim());
        if (rule != null && !TextUtils.isEmpty(rule.getDescripcion())) linea.put("nota", rule.getDescripcion());
        return linea;
    }

    @Nullable
    private ImportRule findMatchingRule(@NonNull String descripcion) {
        for (ImportRule rule : importRules) {
            if (rule == null || TextUtils.isEmpty(rule.getPatron())) continue;
            if (descripcion.toLowerCase(Locale.ROOT).contains(rule.getPatron().toLowerCase(Locale.ROOT))) return rule;
        }
        return null;
    }

    private boolean looksLikeHeader(@NonNull String firstLine) {
        String normalized = normalize(firstLine);
        return normalized.contains("fecha") && (normalized.contains("monto") || normalized.contains("importe"));
    }

    private Map<String, Integer> parseHeader(@NonNull String line) {
        String[] parts = splitDelimited(line);
        Map<String, Integer> out = new HashMap<>();
        for (int i = 0; i < parts.length; i++) {
            String key = normalize(parts[i]);
            if (key.contains("fecha") || key.equals("date")) out.put("fecha", i);
            else if (key.contains("descripcion") || key.contains("detalle") || key.contains("concepto") || key.contains("nota")) out.put("descripcion", i);
            else if (key.contains("monto") || key.contains("importe") || key.contains("amount") || key.contains("total")) out.put("monto", i);
            else if (key.contains("tipo") || key.contains("movimiento") || key.contains("ingreso")) out.put("tipo", i);
            else if (key.contains("moneda") || key.contains("currency") || key.contains("divisa")) out.put("moneda", i);
            else if (key.contains("categoria") || key.contains("category") || key.contains("rubro")) out.put("categoria", i);
        }
        return out;
    }

    private String valueFor(String[] parts, Map<String, Integer> header, String key) {
        Integer index = header.get(key);
        return index != null && index >= 0 && index < parts.length ? parts[index].trim() : "";
    }

    private String[] splitDelimited(@NonNull String line) {
        String separator = line.contains(";") ? ";" : ",";
        return line.split(separator, -1);
    }

    private Map<String, String> normalizeKeys(@NonNull Map<String, String> row) {
        Map<String, String> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : row.entrySet()) {
            out.put(normalize(entry.getKey()), entry.getValue() == null ? "" : entry.getValue().trim());
        }
        return out;
    }

    private String first(@NonNull Map<String, String> row, @NonNull String... keys) {
        for (String key : keys) {
            String value = row.get(key);
            if (!TextUtils.isEmpty(value)) return value;
        }
        return "";
    }

    private String normalize(@Nullable String raw) {
        if (raw == null) return "";
        String clean = Normalizer.normalize(raw.toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        return clean.replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replaceAll("[^a-z0-9]", "")
                .trim();
    }

    private double parseAmount(@Nullable String raw) {
        if (raw == null) throw new NumberFormatException("empty");
        String clean = raw.trim().replaceAll("[^0-9,.-]", "");
        if (clean.isEmpty()) throw new NumberFormatException("empty");
        int lastComma = clean.lastIndexOf(',');
        int lastDot = clean.lastIndexOf('.');
        if (lastComma >= 0 && lastDot >= 0 && lastComma > lastDot) {
            clean = clean.replace(".", "").replace(',', '.');
        } else if (lastComma >= 0 && lastDot >= 0) {
            clean = clean.replace(",", "");
        } else if (lastComma >= 0) {
            clean = clean.replace(',', '.');
        }
        return Double.parseDouble(clean);
    }

    private boolean parseIncomeType(@Nullable String raw, boolean fallback) {
        if (raw == null) return fallback;
        String clean = raw.trim();
        if ("+".equals(clean) || "1".equals(clean)) return true;
        if ("-".equals(clean) || "0".equals(clean)) return false;
        String tipo = normalize(raw);
        if (tipo.startsWith("i") || tipo.contains("ingreso") || tipo.contains("abono") || tipo.equals("1")) return true;
        if (tipo.startsWith("g") || tipo.contains("gasto") || tipo.contains("egreso") || tipo.contains("cargo") || tipo.equals("0")) return false;
        return fallback;
    }

    private String normalizeDate(@Nullable String raw) {
        if (raw == null) return "";
        String clean = raw.trim();
        if (clean.isEmpty()) return "";
        if (clean.matches("\\d+(\\.\\d+)?")) {
            try {
                double serial = Double.parseDouble(clean);
                long millis = Math.round((serial - 25569.0) * 86400000.0);
                return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date(millis));
            } catch (Exception ignored) { }
        }
        String[] patterns = {"yyyy-MM-dd", "dd/MM/yyyy", "dd-MM-yyyy", "MM/dd/yyyy"};
        for (String pattern : patterns) {
            try {
                Date date = new SimpleDateFormat(pattern, Locale.US).parse(clean);
                if (date != null) return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date);
            } catch (Exception ignored) { }
        }
        return clean;
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

    private boolean isSpecialCategory(@Nullable Categoria categoria) {
        return categoria != null
                && categoria.nombre != null
                && categoria.nombre.equalsIgnoreCase(com.example.finanzas.data.model.Transaccion.INITIAL_BALANCE_CATEGORY);
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
                    int duplicados = response.optInt("duplicados", 0);
                    if (duplicados > 0) {
                        Toast.makeText(requireContext(),
                                getString(R.string.import_process_duplicates, duplicados),
                                Toast.LENGTH_LONG).show();
                    }
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
