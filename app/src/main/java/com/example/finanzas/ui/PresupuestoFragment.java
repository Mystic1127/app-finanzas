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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.finanzas.R;
import com.example.finanzas.data.api.BudgetService;
import com.example.finanzas.data.api.CategoryBudgetService;
import com.example.finanzas.data.api.CategoryStore;
import com.example.finanzas.data.model.CategoryBudgetInput;
import com.example.finanzas.data.model.CategoryBudgetSummary;
import com.example.finanzas.data.model.Categoria;
import com.example.finanzas.ui.adapter.CategoryBudgetEditAdapter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
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

        Calendar cal = Calendar.getInstance();
        anio = cal.get(Calendar.YEAR);
        mes = cal.get(Calendar.MONTH) + 1;

        btnGuardarPresupuesto.setOnClickListener(view -> guardarPresupuesto());
        btnGuardarCategorias.setOnClickListener(view -> guardarCategorias());
        btnAgregarCategoria.setOnClickListener(view -> crearCategoria());

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(this::recargarDatos);
        }

        recargarDatos();
    }

    private void recargarDatos() {
        if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(true);
        cargarPresupuestoMensual();
        cargarCategorias();
    }

    private void cargarPresupuestoMensual() {
        BudgetService.get(requireContext(), anio, mes, new BudgetService.GetCb() {
            @Override public void onOk(double monto) {
                etPresupuesto.setText(String.valueOf(monto));
                stopRefreshing();
            }
            @Override public void onFail() {
                Toast.makeText(requireContext(), R.string.error_cargar_presupuesto, Toast.LENGTH_SHORT).show();
                stopRefreshing();
            }
        });
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

        BudgetService.set(requireContext(), anio, mes, val, new BudgetService.SimpleCb() {
            @Override public void onOk() {
                Toast.makeText(requireContext(), R.string.pres_guardado, Toast.LENGTH_SHORT).show();
            }
            @Override public void onFail() {
                Toast.makeText(requireContext(), R.string.error_guardar_presupuesto, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cargarCategorias() {
        CategoryStore.loadOnce(requireContext(), new CategoryStore.Callback() {
            @Override
            public void onReady(List<Categoria> cats) {
                List<CategoryBudgetInput> inputs = new ArrayList<>();
                for (Categoria c : cats) {
                    if (c != null && !c.esIngreso) {
                        CategoryBudgetInput input = new CategoryBudgetInput();
                        input.setCategoriaId(c.id);
                        input.setCategoriaNombre(c.nombre);
                        input.setMonto(0);
                        inputs.add(input);
                    }
                }
                categoryAdapter.setItems(inputs);
                cargarPresupuestosGuardados();
            }

            @Override
            public void onError() {
                Toast.makeText(requireContext(), R.string.error_cargar_categorias, Toast.LENGTH_SHORT).show();
                stopRefreshing();
            }
        });
    }

    private void cargarPresupuestosGuardados() {
        CategoryBudgetService.list(requireContext(), anio, mes, new CategoryBudgetService.ListCb() {
            @Override
            public void onOk(List<CategoryBudgetSummary> items) {
                List<CategoryBudgetInput> current = categoryAdapter.getItems();
                for (CategoryBudgetInput input : current) {
                    for (CategoryBudgetSummary summary : items) {
                        if (summary.getCategoriaId() == input.getCategoriaId()) {
                            input.setMonto(summary.getLimite());
                            break;
                        }
                    }
                }
                categoryAdapter.setItems(current);
                stopRefreshing();
            }

            @Override
            public void onFail() {
                Toast.makeText(requireContext(), R.string.error_cargar_presupuesto, Toast.LENGTH_SHORT).show();
                stopRefreshing();
            }
        });
    }

    private void guardarCategorias() {
        btnGuardarCategorias.setEnabled(false);
        List<CategoryBudgetInput> items = categoryAdapter.getItems();
        CategoryBudgetService.save(requireContext(), anio, mes, items, new CategoryBudgetService.SaveCb() {
            @Override
            public void onOk() {
                btnGuardarCategorias.setEnabled(true);
                Toast.makeText(requireContext(), R.string.pres_categorias_guardadas, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFail() {
                btnGuardarCategorias.setEnabled(true);
                Toast.makeText(requireContext(), R.string.error_guardar_categorias, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void crearCategoria() {
        String nombre = etNuevaCategoria.getText() == null ? "" : etNuevaCategoria.getText().toString().trim();
        if (nombre.isEmpty()) {
            Toast.makeText(requireContext(), R.string.pres_category_new_hint, Toast.LENGTH_SHORT).show();
            return;
        }

        btnAgregarCategoria.setEnabled(false);
        Categoria nueva = CategoryStore.createCategoria(requireContext(), nombre, false);
        btnAgregarCategoria.setEnabled(true);

        if (nueva == null) {
            Toast.makeText(requireContext(), R.string.pres_category_create_error, Toast.LENGTH_SHORT).show();
            return;
        }

        CategoryBudgetInput input = new CategoryBudgetInput();
        input.setCategoriaId(nueva.id);
        input.setCategoriaNombre(nueva.nombre);
        input.setMonto(0);

        List<CategoryBudgetInput> current = categoryAdapter.getItems();
        current.add(input);
        Collections.sort(current, Comparator.comparing(CategoryBudgetInput::getCategoriaNombre,
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        categoryAdapter.setItems(current);
        etNuevaCategoria.setText("");
        Toast.makeText(requireContext(), R.string.pres_category_created, Toast.LENGTH_SHORT).show();
    }

    private void stopRefreshing() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
        }
    }
}
