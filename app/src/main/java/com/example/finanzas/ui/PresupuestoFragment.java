package com.example.finanzas.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.finanzas.R;
import com.example.finanzas.data.api.SettingsService;
import com.example.finanzas.data.model.CategoryBudgetInput;
import com.example.finanzas.ui.adapter.CategoryBudgetEditAdapter;
import com.example.finanzas.ui.viewmodel.BudgetViewModel;
import com.example.finanzas.util.CurrencyConverter;
import com.example.finanzas.util.PerfLogger;
import com.example.finanzas.util.UiFormUtils;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Calendar;
import java.util.List;

public class PresupuestoFragment extends Fragment {

    private EditText etPresupuesto;
    private EditText etNuevaCategoria;
    private MaterialAutoCompleteTextView actPresupuestoMoneda;
    private TextInputLayout tilPresupuesto;
    private TextInputLayout tilNuevaCategoria;
    private CategoryBudgetEditAdapter categoryAdapter;
    private Button btnGuardarPresupuesto;
    private Button btnGuardarCategorias;
    private Button btnAgregarCategoria;
    private SwipeRefreshLayout swipeRefreshLayout;
    private int anio;
    private int mes;
    private BudgetViewModel viewModel;
    private boolean manualRefresh;
    private long perfStartMs;
    private long loadStartMs;
    private boolean firstRenderLogged;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_presupuesto, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        perfStartMs = PerfLogger.now();
        firstRenderLogged = false;

        etPresupuesto = v.findViewById(R.id.etPresupuesto);
        etNuevaCategoria = v.findViewById(R.id.etNuevaCategoria);
        actPresupuestoMoneda = v.findViewById(R.id.actPresupuestoMoneda);
        tilPresupuesto = v.findViewById(R.id.tilPresupuesto);
        tilNuevaCategoria = v.findViewById(R.id.tilNuevaCategoria);
        btnGuardarPresupuesto = v.findViewById(R.id.btnGuardarPresupuesto);
        btnGuardarCategorias = v.findViewById(R.id.btnGuardarCategorias);
        btnAgregarCategoria = v.findViewById(R.id.btnAgregarCategoria);
        androidx.recyclerview.widget.RecyclerView rvCategory = v.findViewById(R.id.rvCategoryBudgets);
        swipeRefreshLayout = v.findViewById(R.id.swipeBudget);

        rvCategory.setLayoutManager(new LinearLayoutManager(requireContext()));
        categoryAdapter = new CategoryBudgetEditAdapter();
        rvCategory.setAdapter(categoryAdapter);
        viewModel = new ViewModelProvider(requireActivity()).get(BudgetViewModel.class);
        viewModel.clearCacheIfUserChanged();

        Calendar cal = Calendar.getInstance();
        anio = cal.get(Calendar.YEAR);
        mes = cal.get(Calendar.MONTH) + 1;

        btnGuardarPresupuesto.setOnClickListener(view -> guardarPresupuesto());
        btnGuardarCategorias.setOnClickListener(view -> guardarCategorias());
        btnAgregarCategoria.setOnClickListener(view -> crearCategoria());
        UiFormUtils.clearErrorOnTextChange(etPresupuesto, etNuevaCategoria);
        tilPresupuesto.setPrefixText(SettingsService.getCurrencySymbol(requireContext()) + " ");
        setupCurrencySelector();

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(() -> {
                manualRefresh = true;
                recargarDatos();
            });
        }
        observeViewModel();

        recargarDatos();
        PerfLogger.logSince("PresupuestoFragment", "onViewCreated", perfStartMs);
    }

    private void recargarDatos() {
        loadStartMs = PerfLogger.now();
        PerfLogger.log("PresupuestoFragment", "loadStart");
        viewModel.load(anio, mes, manualRefresh);
    }

    private void guardarPresupuesto() {
        String s = etPresupuesto.getText() == null ? "" : etPresupuesto.getText().toString().trim();
        double val;
        try {
            val = s.isEmpty() ? 0.0 : Math.max(0, parseMontoSeguro(s));
        } catch (NumberFormatException ex) {
            tilPresupuesto.setError(getString(R.string.error_monto_invalido));
            return;
        }

        tilPresupuesto.setError(null);
        viewModel.saveBudget(anio, mes, val, CurrencyConverter.normalize(
                actPresupuestoMoneda.getText() == null ? "" : actPresupuestoMoneda.getText().toString()
        ));
    }

    private void guardarCategorias() {
        List<CategoryBudgetInput> items = categoryAdapter.getItems();
        viewModel.saveCategoryBudgets(anio, mes, items);
    }

    private void crearCategoria() {
        String nombre = etNuevaCategoria.getText() == null ? "" : etNuevaCategoria.getText().toString().trim();
        if (nombre.isEmpty()) {
            tilNuevaCategoria.setError(getString(R.string.pres_category_new_hint));
            return;
        }

        tilNuevaCategoria.setError(null);
        viewModel.createCategory(nombre, categoryAdapter.getItems());
    }

    private void stopRefreshing() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    private void observeViewModel() {
        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            boolean isLoading = Boolean.TRUE.equals(loading);
            if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(isLoading && manualRefresh);
            UiFormUtils.setActionLoading(btnGuardarPresupuesto, isLoading);
            UiFormUtils.setActionLoading(btnGuardarCategorias, isLoading);
            UiFormUtils.setActionLoading(btnAgregarCategoria, isLoading);
            if (!isLoading) {
                PerfLogger.logSince("PresupuestoFragment", "loadComplete", loadStartMs);
                manualRefresh = false;
            }
        });
        viewModel.getBudget().observe(getViewLifecycleOwner(), monto -> {
            if (monto != null) etPresupuesto.setText(String.valueOf(monto));
        });
        viewModel.getCategoryBudgets().observe(getViewLifecycleOwner(), items -> {
            if (!firstRenderLogged) {
                firstRenderLogged = true;
                PerfLogger.logSince("PresupuestoFragment", "firstRender", perfStartMs);
            }
            if (items != null) {
                categoryAdapter.setItems(items);
                etNuevaCategoria.setText("");
            }
        });
        viewModel.getMessage().observe(getViewLifecycleOwner(), msgRes -> {
            if (msgRes != null) Snackbar.make(requireView(), msgRes, Snackbar.LENGTH_SHORT).show();
        });
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
        return Double.parseDouble(limpio);
    }

    private void setupCurrencySelector() {
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                CurrencyConverter.supportedCurrencies()
        );
        actPresupuestoMoneda.setAdapter(adapter);
        actPresupuestoMoneda.setText(SettingsService.getCurrencyCode(requireContext()), false);
        actPresupuestoMoneda.setOnFocusChangeListener((view, hasFocus) -> { if (hasFocus) actPresupuestoMoneda.showDropDown(); });
        actPresupuestoMoneda.setOnClickListener(view -> actPresupuestoMoneda.showDropDown());
        actPresupuestoMoneda.setOnItemClickListener((parent, view, position, id) ->
                tilPresupuesto.setPrefixText(SettingsService.getCurrencySymbol(CurrencyConverter.normalize(actPresupuestoMoneda.getText().toString())) + " ")
        );
    }
}
