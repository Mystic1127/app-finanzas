package com.example.finanzas.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.finanzas.R;
import com.example.finanzas.data.model.CategoryBudgetInput;
import com.example.finanzas.ui.adapter.CategoryBudgetEditAdapter;
import com.example.finanzas.ui.viewmodel.BudgetViewModel;

import java.util.Calendar;
import java.util.List;

public class PresupuestoFragment extends Fragment {

    private EditText etPresupuesto;
    private EditText etNuevaCategoria;
    private CategoryBudgetEditAdapter categoryAdapter;
    private Button btnGuardarCategorias;
    private Button btnAgregarCategoria;
    private SwipeRefreshLayout swipeRefreshLayout;
    private int anio;
    private int mes;
    private BudgetViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_presupuesto, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        etPresupuesto = v.findViewById(R.id.etPresupuesto);
        etNuevaCategoria = v.findViewById(R.id.etNuevaCategoria);
        Button btnGuardarPresupuesto = v.findViewById(R.id.btnGuardarPresupuesto);
        btnGuardarCategorias = v.findViewById(R.id.btnGuardarCategorias);
        btnAgregarCategoria = v.findViewById(R.id.btnAgregarCategoria);
        androidx.recyclerview.widget.RecyclerView rvCategory = v.findViewById(R.id.rvCategoryBudgets);
        swipeRefreshLayout = v.findViewById(R.id.swipeBudget);

        rvCategory.setLayoutManager(new LinearLayoutManager(requireContext()));
        categoryAdapter = new CategoryBudgetEditAdapter();
        rvCategory.setAdapter(categoryAdapter);
        viewModel = new ViewModelProvider(this).get(BudgetViewModel.class);

        Calendar cal = Calendar.getInstance();
        anio = cal.get(Calendar.YEAR);
        mes = cal.get(Calendar.MONTH) + 1;

        btnGuardarPresupuesto.setOnClickListener(view -> guardarPresupuesto());
        btnGuardarCategorias.setOnClickListener(view -> guardarCategorias());
        btnAgregarCategoria.setOnClickListener(view -> crearCategoria());

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(this::recargarDatos);
        }
        observeViewModel();

        recargarDatos();
    }

    private void recargarDatos() {
        viewModel.load(anio, mes);
    }

    private void guardarPresupuesto() {
        String s = etPresupuesto.getText() == null ? "" : etPresupuesto.getText().toString().trim();
        double val;
        try {
            val = s.isEmpty() ? 0.0 : Math.max(0, Double.parseDouble(s));
        } catch (NumberFormatException ex) {
            Toast.makeText(requireContext(), "Monto inválido", Toast.LENGTH_SHORT).show();
            return;
        }

        viewModel.saveBudget(anio, mes, val);
    }

    private void guardarCategorias() {
        btnGuardarCategorias.setEnabled(false);
        List<CategoryBudgetInput> items = categoryAdapter.getItems();
        viewModel.saveCategoryBudgets(anio, mes, items);
    }

    private void crearCategoria() {
        String nombre = etNuevaCategoria.getText() == null ? "" : etNuevaCategoria.getText().toString().trim();
        if (nombre.isEmpty()) {
            Toast.makeText(requireContext(), R.string.pres_category_new_hint, Toast.LENGTH_SHORT).show();
            return;
        }

        btnAgregarCategoria.setEnabled(false);
        viewModel.createCategory(nombre, categoryAdapter.getItems());
    }

    private void stopRefreshing() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    private void observeViewModel() {
        viewModel.loading.observe(getViewLifecycleOwner(), loading -> {
            if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(Boolean.TRUE.equals(loading));
            if (!Boolean.TRUE.equals(loading)) {
                btnGuardarCategorias.setEnabled(true);
                btnAgregarCategoria.setEnabled(true);
            }
        });
        viewModel.budget.observe(getViewLifecycleOwner(), monto -> {
            if (monto != null) etPresupuesto.setText(String.valueOf(monto));
        });
        viewModel.categoryBudgets.observe(getViewLifecycleOwner(), items -> {
            if (items != null) {
                categoryAdapter.setItems(items);
                etNuevaCategoria.setText("");
            }
        });
        viewModel.message.observe(getViewLifecycleOwner(), msgRes -> {
            if (msgRes != null) Toast.makeText(requireContext(), msgRes, Toast.LENGTH_SHORT).show();
        });
    }
}
