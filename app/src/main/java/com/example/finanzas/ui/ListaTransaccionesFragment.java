package com.example.finanzas.ui;

import android.os.Bundle;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.finanzas.R;
import com.example.finanzas.data.api.CategoryStore;
import com.example.finanzas.data.model.Categoria;
import com.example.finanzas.data.model.Transaccion;
import com.example.finanzas.data.model.TransaccionFiltro;
import com.example.finanzas.ui.adapter.TransaccionAdapter;
import com.example.finanzas.ui.viewmodel.TransactionsViewModel;
import com.example.finanzas.util.Format;
import com.example.finanzas.util.PerfLogger;
import com.example.finanzas.util.Prefs;
import com.example.finanzas.util.TransactionLabelStore;
import com.example.finanzas.util.UiFormUtils;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ListaTransaccionesFragment extends Fragment {

    private TransaccionAdapter adapter;
    private ProgressBar progress;
    private TextView tvPeriodo;
    private TextView tvEmpty;
    private SwipeRefreshLayout swipeRefreshLayout;
    private int selectedYear = 0;
    private int selectedMonth = 0;
    private boolean announcePeriod = false;
    private boolean pendingPrefClear = false;
    private boolean manualRefresh = false;
    private long perfStartMs;
    private long loadStartMs;
    private boolean firstRenderLogged;
    private MaterialButton btnFiltros;
    private MaterialButton btnAddTransactionColor;
    private View scrollTransactionLabels;
    private ChipGroup chipTransactionLabels;
    private TransaccionFiltro filtroActual;
    private List<Categoria> categorias;
    private final List<Transaccion> masterTransactions = new ArrayList<>();
    private List<TransactionLabelStore.Label> transactionLabels = new ArrayList<>();
    private Map<Integer, String> labelAssignments = new HashMap<>();
    private String selectedLabelId = null;
    private TransactionsViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_lista_transacciones, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        perfStartMs = PerfLogger.now();
        firstRenderLogged = false;

        ListView listView = v.findViewById(R.id.listView);
        tvPeriodo = v.findViewById(R.id.tvPeriodo);
        tvEmpty = v.findViewById(R.id.tvTransactionsEmpty);
        progress = v.findViewById(R.id.progressLista);
        swipeRefreshLayout = v.findViewById(R.id.swipeTransacciones);
        btnFiltros = v.findViewById(R.id.btnFiltros);
        btnAddTransactionColor = v.findViewById(R.id.btnAddTransactionColor);
        scrollTransactionLabels = v.findViewById(R.id.scrollTransactionLabels);
        chipTransactionLabels = v.findViewById(R.id.chipTransactionLabels);
        viewModel = new ViewModelProvider(requireActivity()).get(TransactionsViewModel.class);
        viewModel.clearCacheIfUserChanged();
        adapter = new TransaccionAdapter(requireContext(), new ArrayList<>());
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            Transaccion t = adapter.getItem(position);
            if (t == null) return;
            if (t.isInitialBalance()) {
                UiFormUtils.showMessage(requireView(), R.string.perfil_initial_balances_locked_help);
                return;
            }

            Bundle args = new Bundle();
            args.putInt(NuevaTransaccionFragment.EXTRA_ID, t.getId());
            args.putInt(NuevaTransaccionFragment.EXTRA_CAT_ID, t.getCategoriaId());
            args.putString(NuevaTransaccionFragment.EXTRA_CAT_NOMBRE, t.getCategoriaNombre());
            args.putBoolean(NuevaTransaccionFragment.EXTRA_ES_INGRESO, t.isEsIngreso());
            args.putDouble(NuevaTransaccionFragment.EXTRA_MONTO, t.getMonto());
            args.putString(NuevaTransaccionFragment.EXTRA_NOTA,
                    t.getNota() == null ? "" : t.getNota());
            args.putString(NuevaTransaccionFragment.EXTRA_MONEDA,
                    t.getMoneda() == null ? "PEN" : t.getMoneda());
            args.putString(NuevaTransaccionFragment.EXTRA_ACCOUNT_TYPE, t.getAccountType());
            if (t.getFecha() != null) {
                args.putLong(NuevaTransaccionFragment.EXTRA_FECHA, t.getFecha().getTime());
            }

            Navigation.findNavController(view).navigate(R.id.nav_new, args);
        });

        listView.setOnItemLongClickListener((p, view, pos, id) -> {
            Transaccion t = adapter.getItem(pos);
            if (t == null) return true;
            if (t.isInitialBalance()) {
                UiFormUtils.showMessage(requireView(), R.string.perfil_initial_balances_locked_help);
                return true;
            }

            new android.app.AlertDialog.Builder(requireContext())
                    .setTitle(R.string.btn_eliminar)
                    .setMessage(R.string.pres_confirm_delete)
                    .setPositiveButton(R.string.btn_eliminar, (d, w) -> eliminarRemotoYRefrescar(t.getId()))
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
            return true;
        });

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnChildScrollUpCallback((parent, child) ->
                    listView != null && listView.canScrollVertically(-1));
            swipeRefreshLayout.setOnRefreshListener(() -> {
                manualRefresh = true;
                cargarTransacciones();
            });
        }

        if (btnFiltros != null) {
            btnFiltros.setOnClickListener(v12 -> mostrarDialogoFiltros());
        }
        if (btnAddTransactionColor != null) {
            btnAddTransactionColor.setOnClickListener(v13 -> showLabelPickerDialog());
        }
        loadTransactionLabels();
        observeViewModel();
        PerfLogger.logSince("ListaTransaccionesFragment", "onViewCreated", perfStartMs);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTransactionLabels();
        cargarTransacciones();
    }

    private void cargarTransacciones() {
        int[] periodo = resolvePeriodo();
        final int anio = periodo[0];
        final int mes = periodo[1];
        actualizarPeriodoLabel(anio, mes);
        loadStartMs = PerfLogger.now();
        PerfLogger.log("ListaTransaccionesFragment", "loadStart");
        viewModel.load(anio, mes, filtroActual, manualRefresh);
    }

    private int[] resolvePeriodo() {
        if (selectedYear > 0 && selectedMonth > 0) {
            return new int[]{selectedYear, selectedMonth};
        }

        int prefYear = Prefs.getLastTransactionsYear(requireContext());
        int prefMonth = Prefs.getLastTransactionsMonth(requireContext());
        if (prefYear > 0 && prefMonth >= 1 && prefMonth <= 12) {
            selectedYear = prefYear;
            selectedMonth = prefMonth;
            announcePeriod = true;
            pendingPrefClear = true;
            return new int[]{selectedYear, selectedMonth};
        }

        Calendar cal = Calendar.getInstance();
        selectedYear = cal.get(Calendar.YEAR);
        selectedMonth = cal.get(Calendar.MONTH) + 1;
        announcePeriod = false;
        pendingPrefClear = false;
        return new int[]{selectedYear, selectedMonth};
    }

    private void actualizarPeriodoLabel(int anio, int mes) {
        if (tvPeriodo == null) return;
        tvPeriodo.setVisibility(View.VISIBLE);
        tvPeriodo.setText(getString(R.string.transactions_period_label, Format.monthYear(anio, mes)));

        Calendar cal = Calendar.getInstance();
        int actualYear = cal.get(Calendar.YEAR);
        int actualMonth = cal.get(Calendar.MONTH) + 1;
        boolean esActual = anio == actualYear && mes == actualMonth;

        tvPeriodo.setEnabled(!esActual);
        if (esActual) {
            tvPeriodo.setOnClickListener(null);
        } else {
            tvPeriodo.setOnClickListener(v -> {
                selectedYear = 0;
                selectedMonth = 0;
                Prefs.clearLastTransactionsPeriod(requireContext());
                announcePeriod = false;
                pendingPrefClear = false;
                UiFormUtils.showMessage(requireView(), R.string.transactions_period_reset);
                cargarTransacciones();
            });
        }
    }

    private void eliminarRemotoYRefrescar(int id) {
        viewModel.delete(id);
    }

    private void loadTransactionLabels() {
        if (!isAdded()) return;
        transactionLabels = TransactionLabelStore.listLabels(requireContext());
        labelAssignments = TransactionLabelStore.listAssignments(requireContext());
        adapter.setLabels(TransactionLabelStore.assignedLabelDetails(requireContext()));
        renderLabelChips();
        renderTransactionList();
    }

    private void renderLabelChips() {
        if (chipTransactionLabels == null || scrollTransactionLabels == null) return;
        chipTransactionLabels.removeAllViews();
        if (transactionLabels.isEmpty()) {
            scrollTransactionLabels.setVisibility(View.GONE);
            selectedLabelId = null;
            return;
        }
        scrollTransactionLabels.setVisibility(View.VISIBLE);

        Chip all = new Chip(requireContext());
        all.setText(R.string.transactions_labels_all);
        all.setCheckable(true);
        all.setChecked(selectedLabelId == null);
        all.setOnClickListener(v -> {
            selectedLabelId = null;
            renderLabelChips();
            renderTransactionList();
        });
        chipTransactionLabels.addView(all);

        boolean selectedStillExists = false;
        for (TransactionLabelStore.Label label : transactionLabels) {
            Chip chip = new Chip(requireContext());
            chip.setText(label.name);
            chip.setCheckable(true);
            chip.setChipIcon(new ColorDrawable(label.colorInt()));
            chip.setChipIconSize(dp(10));
            chip.setChipIconTint(null);
            chip.setChecked(label.id.equals(selectedLabelId));
            if (label.id.equals(selectedLabelId)) selectedStillExists = true;
            chip.setOnClickListener(v -> {
                selectedLabelId = label.id;
                renderLabelChips();
                renderTransactionList();
            });
            chipTransactionLabels.addView(chip);
        }
        if (selectedLabelId != null && !selectedStillExists) {
            selectedLabelId = null;
            renderLabelChips();
        }
    }

    private void renderTransactionList() {
        if (adapter == null) return;
        List<Transaccion> visible = new ArrayList<>();
        for (Transaccion tx : masterTransactions) {
            if (tx == null) continue;
            if (selectedLabelId == null || selectedLabelId.equals(labelAssignments.get(tx.getId()))) {
                visible.add(tx);
            }
        }
        adapter.clear();
        adapter.addAll(visible);
        adapter.notifyDataSetChanged();
        if (tvEmpty != null) tvEmpty.setVisibility(visible.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showLabelPickerDialog() {
        loadTransactionLabels();
        if (transactionLabels.isEmpty()) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.transactions_add_color)
                    .setMessage(R.string.transaction_label_empty)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(R.string.transaction_label_create, (dialog, which) ->
                            showCreateLabelDialog(() -> showLabelPickerDialog()))
                    .show();
            return;
        }

        String[] names = new String[transactionLabels.size()];
        for (int i = 0; i < transactionLabels.size(); i++) names[i] = transactionLabels.get(i).name;
        final int[] selected = {0};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.transactions_add_color)
                .setSingleChoiceItems(names, 0, (dialog, which) -> selected[0] = which)
                .setNeutralButton(R.string.transaction_label_create, (dialog, which) ->
                        showCreateLabelDialog(() -> showLabelPickerDialog()))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.transaction_label_next, (dialog, which) ->
                        showTransactionSelectionDialog(transactionLabels.get(selected[0])))
                .show();
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
        LinearLayout.LayoutParams hexParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        hexParams.topMargin = dp(10);
        root.addView(hex, hexParams);

        LinearLayout palette = new LinearLayout(requireContext());
        palette.setOrientation(LinearLayout.HORIZONTAL);
        int[] colors = new int[]{0xFFE53935, 0xFF43A047, 0xFF1E88E5, 0xFFF9A825, 0xFF8E24AA};
        for (int color : colors) {
            View swatch = new View(requireContext());
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(color);
            bg.setCornerRadius(dp(6));
            swatch.setBackground(bg);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(34), dp(34));
            params.setMargins(0, dp(12), dp(8), 0);
            palette.addView(swatch, params);
            swatch.setOnClickListener(v -> hex.setText(String.format("#%06X", (0xFFFFFF & color))));
        }
        root.addView(palette);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.transaction_label_create)
                .setView(root)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.btn_guardar, null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
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
            TransactionLabelStore.createLabel(requireContext(), name, color);
            dialog.dismiss();
            loadTransactionLabels();
            if (onSaved != null) onSaved.run();
        }));
        dialog.show();
    }

    private void showTransactionSelectionDialog(@NonNull TransactionLabelStore.Label label) {
        List<Transaccion> candidates = new ArrayList<>();
        for (Transaccion tx : masterTransactions) {
            if (tx != null && !tx.isInitialBalance()) candidates.add(tx);
        }
        if (candidates.isEmpty()) {
            UiFormUtils.showMessage(requireView(), R.string.transactions_empty_filtered);
            return;
        }
        String[] rows = new String[candidates.size()];
        boolean[] checked = new boolean[candidates.size()];
        for (int i = 0; i < candidates.size(); i++) {
            Transaccion tx = candidates.get(i);
            rows[i] = tx.getCategoriaNombre() + " - " + Format.money(tx.isEsIngreso() ? tx.getMonto() : -tx.getMonto(), tx.getMoneda())
                    + " - " + Format.date(tx.getFecha());
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.transaction_label_select_transaction)
                .setMultiChoiceItems(rows, checked, (dialog, which, isChecked) -> checked[which] = isChecked)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.transaction_label_confirm, (dialog, which) -> {
                    List<Integer> selected = new ArrayList<>();
                    for (int i = 0; i < checked.length; i++) {
                        if (checked[i]) selected.add(candidates.get(i).getId());
                    }
                    if (selected.isEmpty()) {
                        UiFormUtils.showMessage(requireView(), R.string.transaction_label_select_required);
                        return;
                    }
                    TransactionLabelStore.assignLabel(requireContext(), label.id, selected);
                    UiFormUtils.showMessage(requireView(), R.string.transaction_label_assigned);
                    loadTransactionLabels();
                })
                .show();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void mostrarDialogoFiltros() {
        View content = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_filtro_transacciones, null, false);
        TextInputEditText etTexto = content.findViewById(R.id.etFiltroTexto);
        TextInputEditText etInicio = content.findViewById(R.id.etFiltroInicio);
        TextInputEditText etFin = content.findViewById(R.id.etFiltroFin);
        MaterialAutoCompleteTextView actCategoria = content.findViewById(R.id.actFiltroCategoria);
        android.widget.RadioGroup rgOrden = content.findViewById(R.id.rgOrden);
        MaterialSwitch swAsc = content.findViewById(R.id.swAscendente);
        final String allCategoriesLabel = getString(R.string.transactions_filter_all_categories);

        UiFormUtils.bindDatePicker(requireContext(), etInicio);
        UiFormUtils.bindDatePicker(requireContext(), etFin);
        UiFormUtils.clearErrorOnTextChange(etTexto, etInicio, etFin);

        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(requireContext(), R.layout.item_dropdown, new ArrayList<>());
        actCategoria.setAdapter(catAdapter);
        actCategoria.setText(allCategoriesLabel, false);
        actCategoria.setOnFocusChangeListener((view, hasFocus) -> { if (hasFocus) actCategoria.showDropDown(); });
        actCategoria.setOnClickListener(view -> actCategoria.showDropDown());

        CategoryStore.loadOnce(requireContext(), new CategoryStore.Callback() {
            @Override
            public void onReady(List<? extends Categoria> cats) {
                categorias = new ArrayList<>(cats);
                List<String> nombres = new ArrayList<>();
                nombres.add(allCategoriesLabel);
                for (Categoria c : cats) nombres.add(c.nombre);
                catAdapter.clear();
                catAdapter.addAll(nombres);
                if (filtroActual != null && filtroActual.getCategoriaId() != null) {
                    for (Categoria c : cats) {
                        if (c != null && c.id == filtroActual.getCategoriaId()) {
                            actCategoria.setText(c.nombre, false);
                            break;
                        }
                    }
                }
            }

            @Override
            public void onError() { }
        });

        if (filtroActual != null) {
            if (!TextUtils.isEmpty(filtroActual.getTexto())) {
                etTexto.setText(filtroActual.getTexto());
            }
            if (filtroActual.getFechaInicio() != null) {
                etInicio.setText(UiFormUtils.formatUiDate(new Date(filtroActual.getFechaInicio())));
            }
            if (filtroActual.getFechaFin() != null) {
                etFin.setText(UiFormUtils.formatUiDate(new Date(filtroActual.getFechaFin() - 86_400_000L)));
            }
            if (filtroActual.getOrden() != null) {
                switch (filtroActual.getOrden()) {
                    case NOMBRE:
                        rgOrden.check(R.id.rbOrdenNombre);
                        break;
                    case CATEGORIA:
                        rgOrden.check(R.id.rbOrdenCategoria);
                        break;
                    default:
                        rgOrden.check(R.id.rbOrdenFecha);
                        break;
                }
            }
            swAsc.setChecked(filtroActual.isAscendente());
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setIcon(R.drawable.ic_filter)
                .setTitle(R.string.transactions_filter_title)
                .setView(content)
                .setPositiveButton(R.string.transactions_filter_apply, null)
                .setNegativeButton(R.string.transactions_filter_clear, (d, which) -> {
                    filtroActual = null;
                    cargarTransacciones();
                })
                .setNeutralButton(android.R.string.cancel, null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    UiFormUtils.clearErrors(etInicio, etFin);
                    TransaccionFiltro filtro = new TransaccionFiltro();
                    Long inicio = parseFiltroFecha(etInicio);
                    Long fin = parseFiltroFecha(etFin);
                    if (!isFechaFiltroValida(etInicio)) {
                        UiFormUtils.setError(etInicio, getString(R.string.error_formato_fecha));
                        return;
                    }
                    if (!isFechaFiltroValida(etFin)) {
                        UiFormUtils.setError(etFin, getString(R.string.error_formato_fecha));
                        return;
                    }
                    if (inicio != null) filtro.setFechaInicio(inicio);
                    if (fin != null) filtro.setFechaFin(fin + 86_400_000L);
                    String texto = etTexto.getText() == null ? "" : etTexto.getText().toString().trim();
                    if (!TextUtils.isEmpty(texto)) filtro.setTexto(texto);

                    String catNombre = actCategoria.getText() == null ? null : actCategoria.getText().toString().trim();
                    if (!TextUtils.isEmpty(catNombre) && !allCategoriesLabel.equalsIgnoreCase(catNombre) && categorias != null) {
                        for (Categoria c : categorias) {
                            if (c != null && catNombre.equalsIgnoreCase(c.nombre)) {
                                filtro.setCategoriaId(c.id);
                                break;
                            }
                        }
                    }

                    int checked = rgOrden.getCheckedRadioButtonId();
                    if (checked == R.id.rbOrdenNombre) filtro.setOrden(TransaccionFiltro.Orden.NOMBRE);
                    else if (checked == R.id.rbOrdenCategoria) filtro.setOrden(TransaccionFiltro.Orden.CATEGORIA);
                    else filtro.setOrden(TransaccionFiltro.Orden.FECHA);
                    filtro.setAscendente(swAsc.isChecked());

                    filtroActual = filtro;
                    cargarTransacciones();
                    dialog.dismiss();
                }));
        dialog.show();
    }

    @Nullable
    private Long parseFiltroFecha(TextInputEditText et) {
        if (et.getText() == null || TextUtils.isEmpty(et.getText().toString())) return null;
        Date date = UiFormUtils.parseUiDate(et.getText().toString().trim());
        return date == null ? null : date.getTime();
    }

    private boolean isFechaFiltroValida(TextInputEditText et) {
        return et.getText() == null
                || TextUtils.isEmpty(et.getText().toString().trim())
                || UiFormUtils.isValidUiDate(et.getText().toString().trim());
    }

    private void showLoading(boolean show) {
        if (progress != null) progress.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void stopRefreshing() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    private void observeViewModel() {
        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            boolean active = Boolean.TRUE.equals(loading);
            showLoading(active && adapter.getCount() == 0);
            if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(active && manualRefresh);
            if (!active) {
                PerfLogger.logSince("ListaTransaccionesFragment", "loadComplete", loadStartMs);
                manualRefresh = false;
            }
        });
        viewModel.getItems().observe(getViewLifecycleOwner(), items -> {
            if (!firstRenderLogged) {
                firstRenderLogged = true;
                PerfLogger.logSince("ListaTransaccionesFragment", "firstRender", perfStartMs);
            }
            List<Transaccion> safe = items == null ? new ArrayList<>() : items;
            masterTransactions.clear();
            masterTransactions.addAll(safe);
            renderTransactionList();
            int[] periodo = resolvePeriodo();
            if (pendingPrefClear) Prefs.clearLastTransactionsPeriod(requireContext());
            if (announcePeriod) {
                UiFormUtils.showMessage(requireView(),
                        getString(R.string.transactions_loaded_period, Format.monthYear(periodo[0], periodo[1])));
            }
            announcePeriod = false;
            pendingPrefClear = false;
            stopRefreshing();
        });
        viewModel.getDeleted().observe(getViewLifecycleOwner(), ok -> {
            if (Boolean.TRUE.equals(ok)) {
                UiFormUtils.showMessage(requireView(), R.string.trans_deleted);
                cargarTransacciones();
            } else if (ok != null) {
                UiFormUtils.showMessage(requireView(), R.string.error_eliminar_transaccion);
            }
        });
        viewModel.getError().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                UiFormUtils.showMessage(requireView(), message);
            } else {
                UiFormUtils.showMessage(requireView(), R.string.error_cargar_transacciones);
            }
            stopRefreshing();
        });
    }
}
