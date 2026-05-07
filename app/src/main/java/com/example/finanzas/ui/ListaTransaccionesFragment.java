package com.example.finanzas.ui;

import android.os.Bundle;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.text.TextUtils;
import android.text.Editable;
import android.text.TextWatcher;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
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
import com.example.finanzas.util.LabelColorUtils;
import com.example.finanzas.util.CurrencyConverter;
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
    private MaterialButton btnOrdenar;
    private MaterialButton btnChangeMonth;
    private MaterialButton btnAddTransactionColor;
    private TextInputEditText etTransactionSearch;
    private ChipGroup chipQuickFilters;
    private TextView tvVisibleCount;
    private TextView tvVisibleTotal;
    private View cardVisibleTotal;
    private TransaccionFiltro filtroActual;
    private List<Categoria> categorias;
    private final List<Transaccion> masterTransactions = new ArrayList<>();
    private List<TransactionLabelStore.Label> transactionLabels = new ArrayList<>();
    private Map<Integer, String> labelAssignments = new HashMap<>();
    private String selectedLabelId = null;
    private TransaccionFiltro.Tipo quickType = TransaccionFiltro.Tipo.TODAS;
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
        btnOrdenar = v.findViewById(R.id.btnOrdenar);
        btnChangeMonth = v.findViewById(R.id.btnChangeMonth);
        btnAddTransactionColor = v.findViewById(R.id.btnAddTransactionColor);
        etTransactionSearch = v.findViewById(R.id.etTransactionSearch);
        chipQuickFilters = v.findViewById(R.id.chipQuickFilters);
        tvVisibleCount = v.findViewById(R.id.tvVisibleCount);
        tvVisibleTotal = v.findViewById(R.id.tvVisibleTotal);
        cardVisibleTotal = v.findViewById(R.id.cardVisibleTotal);
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
            args.putBoolean(NuevaTransaccionFragment.EXTRA_IS_TRANSFER, t.isTransfer());
            args.putDouble(NuevaTransaccionFragment.EXTRA_MONTO, t.getMonto());
            args.putString(NuevaTransaccionFragment.EXTRA_NOTA,
                    t.getDisplayNote() == null ? "" : t.getDisplayNote());
            args.putString(NuevaTransaccionFragment.EXTRA_MONEDA,
                    t.getMoneda() == null ? "PEN" : t.getMoneda());
            args.putString(NuevaTransaccionFragment.EXTRA_ACCOUNT_TYPE, t.getAccountType());
            if (t.getFecha() != null) {
                args.putLong(NuevaTransaccionFragment.EXTRA_FECHA, t.getFecha().getTime());
            }

            viewModel.clearTransientEvents();
            Navigation.findNavController(view).navigate(R.id.nav_new, args);
        });

        listView.setOnItemLongClickListener((p, view, pos, id) -> {
            Transaccion t = adapter.getItem(pos);
            if (t == null) return true;
            if (t.isInitialBalance()) {
                UiFormUtils.showMessage(requireView(), R.string.perfil_initial_balances_locked_help);
                return true;
            }

            new MaterialAlertDialogBuilder(requireContext())
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
        if (btnOrdenar != null) {
            btnOrdenar.setOnClickListener(v12 -> showSortDialog());
        }
        if (btnChangeMonth != null) {
            btnChangeMonth.setOnClickListener(v12 -> showMonthDialog());
        }
        if (btnAddTransactionColor != null) {
            btnAddTransactionColor.setOnClickListener(v13 -> showLabelPickerDialog());
        }
        if (etTransactionSearch != null) {
            etTransactionSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    applySearchText(s == null ? "" : s.toString());
                }
                @Override public void afterTextChanged(Editable s) { }
            });
        }
        renderQuickFilters();
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
        tvPeriodo.setText(Format.monthYear(anio, mes));

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
        if (selectedLabelId != null) {
            boolean selectedStillExists = false;
            for (TransactionLabelStore.Label label : transactionLabels) {
                if (selectedLabelId.equals(label.id)) {
                    selectedStillExists = true;
                    break;
                }
            }
            if (!selectedStillExists) selectedLabelId = null;
        }
        renderTransactionList();
    }

    private void renderFilterLabelChips(@NonNull ChipGroup group, @NonNull String[] pendingSelection) {
        group.removeAllViews();
        Chip all = new Chip(requireContext());
        all.setText(R.string.transactions_labels_all);
        all.setCheckable(true);
        all.setChecked(pendingSelection[0] == null);
        styleNeutralLabelChip(all, pendingSelection[0] == null);
        all.setOnClickListener(v -> {
            pendingSelection[0] = null;
            renderFilterLabelChips(group, pendingSelection);
        });
        group.addView(all);

        for (TransactionLabelStore.Label label : transactionLabels) {
            Chip chip = new Chip(requireContext());
            chip.setText(label.name);
            chip.setCheckable(true);
            boolean selected = label.id.equals(pendingSelection[0]);
            chip.setChecked(selected);
            styleColoredLabelChip(chip, label, selected);
            chip.setOnClickListener(v -> {
                pendingSelection[0] = label.id;
                renderFilterLabelChips(group, pendingSelection);
            });
            group.addView(chip);
        }
    }

    private void styleNeutralLabelChip(@NonNull Chip chip, boolean selected) {
        int background = ContextCompat.getColor(requireContext(),
                selected ? R.color.md_theme_primaryContainer : R.color.md_theme_surface);
        int text = ContextCompat.getColor(requireContext(),
                selected ? R.color.md_theme_onPrimaryContainer : R.color.md_theme_onSurface);
        int stroke = ContextCompat.getColor(requireContext(),
                selected ? R.color.md_theme_primary : R.color.md_theme_outline);
        chip.setChipIconVisible(false);
        chip.setCheckedIconVisible(false);
        chip.setEnsureMinTouchTargetSize(false);
        chip.setMinHeight(dp(38));
        chip.setChipBackgroundColor(ColorStateList.valueOf(background));
        chip.setTextColor(text);
        chip.setChipStrokeColor(ColorStateList.valueOf(stroke));
        chip.setChipStrokeWidth(dp(1));
    }

    private void styleColoredLabelChip(@NonNull Chip chip, @NonNull TransactionLabelStore.Label label, boolean selected) {
        int labelColor = label.colorInt();
        int background = LabelColorUtils.chipBackground(requireContext(), labelColor, selected);
        int accent = LabelColorUtils.accentOnSurface(requireContext(), labelColor);
        GradientDrawable marker = new GradientDrawable();
        marker.setColor(accent);
        marker.setCornerRadius(dp(2));
        marker.setSize(dp(12), dp(4));

        chip.setChipIcon(marker);
        chip.setChipIconVisible(true);
        chip.setChipIconSize(dp(12));
        chip.setChipIconTint(null);
        chip.setCheckedIconVisible(false);
        chip.setEnsureMinTouchTargetSize(false);
        chip.setMinHeight(dp(38));
        chip.setChipBackgroundColor(ColorStateList.valueOf(background));
        chip.setTextColor(LabelColorUtils.textOnTint(requireContext(), labelColor, background));
        chip.setChipStrokeColor(ColorStateList.valueOf(LabelColorUtils.cardStroke(requireContext(), labelColor)));
        chip.setChipStrokeWidth(dp(1));
    }

    private void renderTransactionList() {
        if (adapter == null) return;
        List<Transaccion> visible = new ArrayList<>();
        for (Transaccion tx : masterTransactions) {
            if (tx == null) continue;
            if (!matchesQuickType(tx)) continue;
            if (selectedLabelId == null || selectedLabelId.equals(labelAssignments.get(tx.getId()))) {
                visible.add(tx);
            }
        }
        adapter.clear();
        adapter.addAll(visible);
        adapter.notifyDataSetChanged();
        if (tvEmpty != null) tvEmpty.setVisibility(visible.isEmpty() ? View.VISIBLE : View.GONE);
        updateVisibleTotal(visible);
    }

    private boolean matchesQuickType(@NonNull Transaccion tx) {
        if (quickType == TransaccionFiltro.Tipo.INGRESOS) return tx.isEsIngreso() && !tx.isTransfer() && !tx.isInitialBalance();
        if (quickType == TransaccionFiltro.Tipo.GASTOS) return !tx.isEsIngreso() && !tx.isTransfer();
        if (quickType == TransaccionFiltro.Tipo.TRANSFERENCIAS) return tx.isTransfer();
        return true;
    }

    private void updateVisibleTotal(@NonNull List<Transaccion> visible) {
        double total = 0.0;
        String currency = com.example.finanzas.data.api.SettingsService.getCurrencyCode(requireContext());
        double manualRate = com.example.finanzas.data.api.SettingsService.getManualRate(requireContext());
        for (Transaccion tx : visible) {
            if (tx == null || tx.isTransfer()) continue;
            double converted = CurrencyConverter.convert(tx.getMonto(), tx.getMoneda(), currency, currency, manualRate);
            total += tx.isEsIngreso() ? converted : -converted;
        }
        boolean showTotal = hasActiveTransactionFilter();
        if (cardVisibleTotal != null) {
            cardVisibleTotal.setVisibility(showTotal ? View.VISIBLE : View.GONE);
        }
        if (tvVisibleCount != null) tvVisibleCount.setText(getString(R.string.transactions_total_label, visible.size()));
        if (tvVisibleTotal != null) {
            tvVisibleTotal.setText(Format.money(total, currency));
            tvVisibleTotal.setTextColor(ContextCompat.getColor(requireContext(), total >= 0 ? R.color.income : R.color.expense));
        }
    }

    private boolean hasActiveTransactionFilter() {
        if (selectedLabelId != null) return true;
        if (quickType != TransaccionFiltro.Tipo.TODAS) return true;
        if (filtroActual == null) return false;
        if (!TextUtils.isEmpty(filtroActual.getTexto())) return true;
        if (filtroActual.getFechaInicio() != null || filtroActual.getFechaFin() != null) return true;
        if (filtroActual.getCategoriaId() != null) return true;
        if (filtroActual.getTipo() != null && filtroActual.getTipo() != TransaccionFiltro.Tipo.TODAS) return true;
        if (!TextUtils.isEmpty(filtroActual.getAccountType())) return true;
        return filtroActual.getMontoMin() != null || filtroActual.getMontoMax() != null;
    }

    private void renderQuickFilters() {
        if (chipQuickFilters == null) return;
        chipQuickFilters.removeAllViews();
        addQuickChip(R.string.transactions_quick_all, TransaccionFiltro.Tipo.TODAS);
        addQuickChip(R.string.transactions_quick_income, TransaccionFiltro.Tipo.INGRESOS);
        addQuickChip(R.string.transactions_quick_expense, TransaccionFiltro.Tipo.GASTOS);
        addQuickChip(R.string.transactions_quick_transfer, TransaccionFiltro.Tipo.TRANSFERENCIAS);
    }

    private void addQuickChip(int labelRes, TransaccionFiltro.Tipo type) {
        Chip chip = new Chip(requireContext());
        chip.setText(labelRes);
        chip.setCheckable(true);
        chip.setChecked(quickType == type);
        chip.setOnClickListener(v -> {
            quickType = type;
            renderQuickFilters();
            renderTransactionList();
        });
        styleQuickFilterChip(chip, type, quickType == type);
        chipQuickFilters.addView(chip);
    }

    private void styleQuickFilterChip(@NonNull Chip chip, @NonNull TransaccionFiltro.Tipo type, boolean selected) {
        int stroke = ContextCompat.getColor(requireContext(),
                type == TransaccionFiltro.Tipo.TODAS ? R.color.quick_filter_all_stroke : R.color.quick_filter_other_stroke);
        int background = ContextCompat.getColor(requireContext(), R.color.md_theme_surface);
        chip.setChipIconVisible(false);
        chip.setCheckedIconVisible(false);
        chip.setEnsureMinTouchTargetSize(false);
        chip.setChipBackgroundColor(ColorStateList.valueOf(background));
        chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.transaction_secondary_button_text));
        chip.setChipStrokeColor(ColorStateList.valueOf(stroke));
        chip.setChipStrokeWidth(dp(selected ? 2 : 1));
        chip.setTextSize(12f);
        chip.setSingleLine(true);
        chip.setMinWidth(0);
        chip.setMinHeight(dp(34));
        chip.setChipMinHeight(dp(34));
        chip.setTextStartPadding(dp(8));
        chip.setTextEndPadding(dp(8));
        chip.setChipStartPadding(dp(2));
        chip.setChipEndPadding(dp(2));
        ChipGroup.LayoutParams params = new ChipGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, dp(4), 0);
        chip.setLayoutParams(params);
    }

    private void applySearchText(@NonNull String raw) {
        if (filtroActual == null) filtroActual = new TransaccionFiltro();
        String clean = raw.trim();
        filtroActual.setTexto(clean.isEmpty() ? null : clean);
        cargarTransacciones();
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
                .setNeutralButton(R.string.transaction_label_manage, (dialog, which) -> showManageLabelsDialog())
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.transaction_label_next, (dialog, which) ->
                        showTransactionSelectionDialog(transactionLabels.get(selected[0])))
                .show();
    }

    private void showManageLabelsDialog() {
        loadTransactionLabels();
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

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.transaction_label_manage)
                .setView(root)
                .setNeutralButton(R.string.btn_eliminar, null)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.btn_guardar, null)
                .create();
        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                TransactionLabelStore.deleteLabel(requireContext(), label.id);
                dialog.dismiss();
                loadTransactionLabels();
            });
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
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
                loadTransactionLabels();
            });
        });
        dialog.show();
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

        root.addView(buildColorPalette(hex));

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
        label.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
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

    private void showSortDialog() {
        String[] options = new String[]{
                getString(R.string.transactions_sort_recent),
                getString(R.string.transactions_sort_oldest),
                getString(R.string.transactions_sort_amount_desc),
                getString(R.string.transactions_sort_amount_asc)
        };
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.transactions_sort)
                .setItems(options, (dialog, which) -> {
                    if (filtroActual == null) filtroActual = new TransaccionFiltro();
                    if (which == 0) {
                        filtroActual.setOrden(TransaccionFiltro.Orden.FECHA);
                        filtroActual.setAscendente(false);
                    } else if (which == 1) {
                        filtroActual.setOrden(TransaccionFiltro.Orden.FECHA);
                        filtroActual.setAscendente(true);
                    } else if (which == 2) {
                        filtroActual.setOrden(TransaccionFiltro.Orden.MONTO);
                        filtroActual.setAscendente(false);
                    } else {
                        filtroActual.setOrden(TransaccionFiltro.Orden.MONTO);
                        filtroActual.setAscendente(true);
                    }
                    cargarTransacciones();
                })
                .show();
    }

    private void showMonthDialog() {
        Calendar current = Calendar.getInstance();
        String[] months = new String[12];
        for (int i = 0; i < 12; i++) {
            months[i] = Format.monthYear(current.get(Calendar.YEAR), i + 1);
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.transactions_change_month)
                .setItems(months, (dialog, which) -> {
                    selectedYear = current.get(Calendar.YEAR);
                    selectedMonth = which + 1;
                    cargarTransacciones();
                })
                .show();
    }

    private void mostrarDialogoFiltros() {
        View content = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_filtro_transacciones, null, false);
        TextInputEditText etTexto = content.findViewById(R.id.etFiltroTexto);
        TextInputEditText etInicio = content.findViewById(R.id.etFiltroInicio);
        TextInputEditText etFin = content.findViewById(R.id.etFiltroFin);
        MaterialAutoCompleteTextView actCategoria = content.findViewById(R.id.actFiltroCategoria);
        android.widget.RadioGroup rgOrden = content.findViewById(R.id.rgOrden);
        MaterialSwitch swAsc = content.findViewById(R.id.swAscendente);
        MaterialButton btnCancel = content.findViewById(R.id.btnFilterCancel);
        MaterialButton btnClear = content.findViewById(R.id.btnFilterClear);
        MaterialButton btnApply = content.findViewById(R.id.btnFilterApply);
        ChipGroup chipFiltroEtiquetas = content.findViewById(R.id.chipFiltroEtiquetas);
        View scrollFiltroEtiquetas = content.findViewById(R.id.scrollFiltroEtiquetas);
        TextView tvFiltroEtiqueta = content.findViewById(R.id.tvFiltroEtiqueta);
        final String allCategoriesLabel = getString(R.string.transactions_filter_all_categories);
        final String[] pendingLabelSelection = new String[]{selectedLabelId};

        UiFormUtils.bindDatePicker(requireContext(), etInicio);
        UiFormUtils.bindDatePicker(requireContext(), etFin);
        UiFormUtils.clearErrorOnTextChange(etTexto, etInicio, etFin);

        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(requireContext(), R.layout.item_dropdown, new ArrayList<>());
        actCategoria.setAdapter(catAdapter);
        actCategoria.setText(allCategoriesLabel, false);
        actCategoria.setOnFocusChangeListener((view, hasFocus) -> { if (hasFocus) actCategoria.showDropDown(); });
        actCategoria.setOnClickListener(view -> actCategoria.showDropDown());

        if (chipFiltroEtiquetas != null) {
            if (transactionLabels.isEmpty()) {
                if (scrollFiltroEtiquetas != null) scrollFiltroEtiquetas.setVisibility(View.GONE);
                if (tvFiltroEtiqueta != null) tvFiltroEtiqueta.setVisibility(View.GONE);
                pendingLabelSelection[0] = null;
            } else {
                renderFilterLabelChips(chipFiltroEtiquetas, pendingLabelSelection);
            }
        }

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
                .setView(content)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnClear.setOnClickListener(v -> {
            filtroActual = null;
            selectedLabelId = null;
            cargarTransacciones();
            renderTransactionList();
            dialog.dismiss();
        });
        btnApply.setOnClickListener(v -> {
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
            selectedLabelId = pendingLabelSelection[0];
            cargarTransacciones();
            renderTransactionList();
            dialog.dismiss();
        });
        dialog.show();
        configureFilterDialogWindow(dialog, content);
    }

    private void configureFilterDialogWindow(@NonNull AlertDialog dialog, @NonNull View content) {
        View actions = content.findViewById(R.id.filterDialogActions);
        if (actions != null) {
            int bottomPadding = actions.getPaddingBottom();
            int leftPadding = actions.getPaddingLeft();
            int topPadding = actions.getPaddingTop();
            int rightPadding = actions.getPaddingRight();
            ViewCompat.setOnApplyWindowInsetsListener(actions, (view, insets) -> {
                int bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
                view.setPadding(
                        leftPadding,
                        topPadding,
                        rightPadding,
                        bottomPadding + Math.min(bottomInset, dp(24))
                );
                return insets;
            });
            ViewCompat.requestApplyInsets(actions);
        }

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            int width = Math.min(screenWidth - dp(40), dp(420));
            window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        content.post(() -> {
            ViewParent parent = content.getParent();
            if (parent instanceof View) {
                ((View) parent).setBackgroundResource(R.drawable.bg_dialog_surface);
            }

            int screenHeight = getResources().getDisplayMetrics().heightPixels;
            int availableHeight = Math.max(dp(320), screenHeight - dp(96));
            int maxHeight = Math.min(availableHeight, (int) (screenHeight * 0.86f));
            if (content.getMeasuredHeight() > maxHeight) {
                View scroll = content.findViewById(R.id.filterDialogScroll);
                if (scroll != null) {
                    int overflow = content.getMeasuredHeight() - maxHeight;
                    ViewGroup.LayoutParams scrollParams = scroll.getLayoutParams();
                    scrollParams.height = Math.max(dp(220), scroll.getMeasuredHeight() - overflow);
                    scroll.setLayoutParams(scrollParams);
                } else {
                    ViewGroup.LayoutParams params = content.getLayoutParams();
                    if (params != null) {
                        params.height = maxHeight;
                        content.setLayoutParams(params);
                    }
                }
                content.post(() -> {
                    Window resizedWindow = dialog.getWindow();
                    if (resizedWindow != null) {
                        int screenWidth = getResources().getDisplayMetrics().widthPixels;
                        int width = Math.min(screenWidth - dp(40), dp(420));
                        resizedWindow.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
                    }
                });
            }
        });
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
            if (ok != null) viewModel.clearTransientEvents();
        });
        viewModel.getError().observe(getViewLifecycleOwner(), message -> {
            if (message == null) {
                return;
            }
            if (!message.isEmpty()) {
                UiFormUtils.showMessage(requireView(), message);
            } else {
                UiFormUtils.showMessage(requireView(), R.string.error_cargar_transacciones);
            }
            stopRefreshing();
        });
    }
}
