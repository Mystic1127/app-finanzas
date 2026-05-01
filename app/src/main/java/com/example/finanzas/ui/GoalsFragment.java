package com.example.finanzas.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.PopupMenu;
import android.view.ViewParent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.finanzas.R;
import com.example.finanzas.data.api.SettingsService;
import com.example.finanzas.data.model.SavingsGoal;
import com.example.finanzas.data.model.GoalMilestone;
import com.example.finanzas.ui.adapter.GoalSummaryAdapter;
import com.example.finanzas.ui.viewmodel.GoalsViewModel;
import com.example.finanzas.util.UiFormUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class GoalsFragment extends Fragment {

    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipe;
    private GoalSummaryAdapter adapter;
    private TextView tvEmpty;
    private GoalsViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_goals, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        swipe = v.findViewById(R.id.swipeGoals);
        tvEmpty = v.findViewById(R.id.tvGoalsEmpty);
        androidx.recyclerview.widget.RecyclerView rv = v.findViewById(R.id.rvGoals);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new GoalSummaryAdapter();
        rv.setAdapter(adapter);
        viewModel = new ViewModelProvider(this).get(GoalsViewModel.class);

        adapter.setListener(new GoalSummaryAdapter.Listener() {
            @Override public void onGoalClick(SavingsGoal goal) { mostrarDialogo(goal); }
            @Override public void onGoalLongClick(SavingsGoal goal) { confirmarEliminar(goal); }
            @Override public void onMilestoneClick(View anchor, SavingsGoal goal, GoalMilestone milestone) {
                mostrarMenuHito(anchor, goal, milestone);
            }
            @Override public void onAddMilestone(SavingsGoal goal) { mostrarDialogoHito(goal, null); }
        });

        FloatingActionButton fab = v.findViewById(R.id.fabAddGoal);
        fab.setOnClickListener(view -> mostrarDialogo(null));

        swipe.setOnRefreshListener(this::cargarMetas);
        observeViewModel();
    }

    @Override
    public void onResume() {
        super.onResume();
        cargarMetas();
    }

    private void cargarMetas() {
        viewModel.loadGoals();
    }

    private void mostrarDialogo(@Nullable SavingsGoal goal) {
        View form = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_goal, null, false);
        EditText etTitulo = form.findViewById(R.id.etGoalTitulo);
        EditText etObjetivo = form.findViewById(R.id.etGoalObjetivo);
        EditText etActual = form.findViewById(R.id.etGoalActual);
        EditText etFecha = form.findViewById(R.id.etGoalFecha);

        applyCurrencyPrefix(etObjetivo, etActual);
        setupDatePicker(etFecha);
        UiFormUtils.clearErrorOnTextChange(etTitulo, etObjetivo, etActual, etFecha);

        boolean editando = goal != null;
        if (editando) {
            etTitulo.setText(goal.getTitulo());
            etObjetivo.setText(String.valueOf(goal.getMontoObjetivo()));
            etActual.setText(String.valueOf(goal.getMontoActual()));
            if (goal.getFechaObjetivo() != null) {
                etFecha.setText(UiFormUtils.formatUiDate(goal.getFechaObjetivo()));
            }
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(editando ? R.string.goal_dialog_title_edit : R.string.goal_dialog_title_new)
                .setView(form)
                .setPositiveButton(R.string.goal_btn_save, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(button -> {
                    clearErrors(etTitulo, etObjetivo, etActual, etFecha);
                    String titulo = etTitulo.getText() == null ? "" : etTitulo.getText().toString().trim();
                    String objetivo = etObjetivo.getText() == null ? "" : etObjetivo.getText().toString().trim();
                    String actual = etActual.getText() == null ? "" : etActual.getText().toString().trim();
                    String fecha = etFecha.getText() == null ? "" : etFecha.getText().toString().trim();

                    boolean valido = true;
                    if (TextUtils.isEmpty(titulo)) {
                        setFieldError(etTitulo, getString(R.string.error_campos_obligatorios));
                        valido = false;
                    }
                    if (TextUtils.isEmpty(objetivo)) {
                        setFieldError(etObjetivo, getString(R.string.error_campos_obligatorios));
                        valido = false;
                    }
                    if (!TextUtils.isEmpty(fecha) && !UiFormUtils.isValidUiDate(fecha)) {
                        setFieldError(etFecha, "Usa el formato dd/MM/yyyy");
                        valido = false;
                    }
                    if (!valido) {
                        return;
                    }

                    double objetivoVal;
                    double actualVal;
                    try {
                        objetivoVal = parseDecimal(objetivo);
                    } catch (NumberFormatException e) {
                        setFieldError(etObjetivo, "Ingresa un monto valido");
                        return;
                    }
                    try {
                        actualVal = actual.isEmpty() ? 0 : parseDecimal(actual);
                    } catch (NumberFormatException e) {
                        setFieldError(etActual, "Ingresa un monto valido");
                        return;
                    }
                    if (objetivoVal <= 0) {
                        setFieldError(etObjetivo, "Ingresa un monto mayor a 0");
                        return;
                    }
                    if (actualVal < 0) {
                        setFieldError(etActual, "Ingresa un monto valido");
                        return;
                    }

                    JSONObject body = new JSONObject();
                    try {
                        if (editando && goal != null) body.put("id", goal.getId());
                        body.put("titulo", titulo);
                        body.put("monto_objetivo", objetivoVal);
                        body.put("monto_actual", actualVal);
                        String fechaIso = UiFormUtils.uiDateToIso(fecha);
                        if (fechaIso != null) body.put("fecha_objetivo", fechaIso);
                    } catch (Exception ignore) { }

                    viewModel.saveGoal(body);
                    dialog.dismiss();
                }));
        dialog.show();
    }

    private void setupDatePicker(@NonNull EditText input) {
        UiFormUtils.bindDatePicker(requireContext(), input);
    }

    private void confirmarEliminar(SavingsGoal goal) {
        new AlertDialog.Builder(requireContext())
                .setMessage(R.string.goal_confirm_delete)
                .setPositiveButton(R.string.btn_eliminar, (d, w) -> viewModel.deleteGoal(goal.getId()))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void mostrarDialogoHito(SavingsGoal goal, @Nullable GoalMilestone milestone) {
        View form = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_goal_milestone, null, false);
        EditText etTitulo = form.findViewById(R.id.etMilestoneTitulo);
        EditText etMonto = form.findViewById(R.id.etMilestoneMonto);
        EditText etFecha = form.findViewById(R.id.etMilestoneFecha);
        EditText etDias = form.findViewById(R.id.etMilestoneDias);
        SwitchMaterial swNotificar = form.findViewById(R.id.swMilestoneNotificar);
        CheckBox cbCompletado = form.findViewById(R.id.cbMilestoneCompletado);

        applyCurrencyPrefix(etMonto);
        boolean editando = milestone != null;
        if (editando) {
            etTitulo.setText(milestone.getTitulo());
            if (milestone.getMontoPlanificado() > 0) {
                etMonto.setText(String.valueOf(milestone.getMontoPlanificado()));
            }
            if (milestone.getFechaObjetivo() != null) {
                etFecha.setText(UiFormUtils.formatUiDate(milestone.getFechaObjetivo()));
            }
            if (milestone.getDiasRecordatorio() > 0) {
                etDias.setText(String.valueOf(milestone.getDiasRecordatorio()));
            }
            swNotificar.setChecked(milestone.isNotificar());
            cbCompletado.setChecked(milestone.isCompletado());
        }

        setupDatePicker(etFecha);
        UiFormUtils.clearErrorOnTextChange(etTitulo, etMonto, etFecha, etDias);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(editando ? R.string.goal_milestone_edit : R.string.goal_milestone_new)
                .setView(form)
                .setPositiveButton(R.string.goal_btn_save, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(button -> {
                    clearErrors(etTitulo, etMonto, etFecha, etDias);
                    String titulo = etTitulo.getText() == null ? "" : etTitulo.getText().toString().trim();
                    String montoStr = etMonto.getText() == null ? "" : etMonto.getText().toString().trim();
                    String fecha = etFecha.getText() == null ? "" : etFecha.getText().toString().trim();
                    String diasStr = etDias.getText() == null ? "" : etDias.getText().toString().trim();

                    boolean valido = true;
                    if (titulo.isEmpty()) {
                        setFieldError(etTitulo, getString(R.string.error_campos_obligatorios));
                        valido = false;
                    }
                    if (!fecha.isEmpty() && !UiFormUtils.isValidUiDate(fecha)) {
                        setFieldError(etFecha, "Usa el formato dd/MM/yyyy");
                        valido = false;
                    }
                    if (!valido) {
                        return;
                    }

                    double montoVal = 0;
                    if (!montoStr.isEmpty()) {
                        try {
                            montoVal = parseDecimal(montoStr);
                        } catch (NumberFormatException e) {
                            setFieldError(etMonto, "Ingresa un monto valido");
                            return;
                        }
                        if (montoVal < 0) {
                            setFieldError(etMonto, "Ingresa un monto valido");
                            return;
                        }
                    }
                    int diasVal = 0;
                    if (!diasStr.isEmpty()) {
                        try {
                            diasVal = Integer.parseInt(diasStr);
                        } catch (NumberFormatException e) {
                            setFieldError(etDias, "Ingresa un numero valido");
                            return;
                        }
                        if (diasVal < 0) {
                            setFieldError(etDias, "Ingresa un numero valido");
                            return;
                        }
                    }

                    JSONObject body = new JSONObject();
                    try {
                        if (editando && milestone != null) {
                            body.put("id", milestone.getId());
                        }
                        body.put("meta_id", goal.getId());
                        body.put("titulo", titulo);
                        body.put("monto_planificado", montoVal);
                        String fechaIso = UiFormUtils.uiDateToIso(fecha);
                        if (fechaIso != null) body.put("fecha_objetivo", fechaIso);
                        body.put("notificar", swNotificar.isChecked());
                        body.put("dias_recordatorio", diasVal);
                        body.put("completado", cbCompletado.isChecked());
                    } catch (Exception ignore) { }

                    viewModel.saveMilestone(body);
                    dialog.dismiss();
                }));
        dialog.show();
    }

    private void mostrarMenuHito(View anchor, SavingsGoal goal, GoalMilestone milestone) {
        PopupMenu popup = new PopupMenu(requireContext(), anchor);
        popup.inflate(R.menu.menu_goal_milestone);
        MenuItem toggle = popup.getMenu().findItem(R.id.action_toggle_complete);
        toggle.setTitle(milestone.isCompletado() ? R.string.goal_milestone_mark_pending : R.string.goal_milestone_mark_done);
        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_toggle_complete) {
                actualizarHito(goal, milestone, !milestone.isCompletado());
                return true;
            } else if (itemId == R.id.action_edit) {
                mostrarDialogoHito(goal, milestone);
                return true;
            } else if (itemId == R.id.action_delete) {
                new AlertDialog.Builder(requireContext())
                        .setMessage(R.string.goal_milestone_delete_confirm)
                        .setPositiveButton(R.string.btn_eliminar, (d, w) -> viewModel.deleteMilestone(milestone.getId()))
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void actualizarHito(SavingsGoal goal, GoalMilestone milestone, boolean completado) {
        JSONObject body = new JSONObject();
        try {
            body.put("id", milestone.getId());
            body.put("meta_id", goal.getId());
            body.put("titulo", milestone.getTitulo());
            body.put("monto_planificado", milestone.getMontoPlanificado());
            if (milestone.getFechaObjetivo() != null) {
                body.put("fecha_objetivo", UiFormUtils.formatIsoDate(milestone.getFechaObjetivo()));
            }
            body.put("notificar", milestone.isNotificar());
            body.put("dias_recordatorio", milestone.getDiasRecordatorio());
            body.put("completado", completado);
        } catch (Exception ignore) { }

        viewModel.saveMilestone(body);
    }

    private void observeViewModel() {
        viewModel.getLoading().observe(getViewLifecycleOwner(),
                loading -> swipe.setRefreshing(Boolean.TRUE.equals(loading)));

        viewModel.getGoals().observe(getViewLifecycleOwner(), items -> {
            List<SavingsGoal> safe = items == null ? new ArrayList<>() : items;
            adapter.setItems(new ArrayList<>(safe));
            tvEmpty.setVisibility(safe.isEmpty() ? View.VISIBLE : View.GONE);
        });

        viewModel.getMessage().observe(getViewLifecycleOwner(), msgRes -> {
            if (msgRes != null) {
                UiFormUtils.showMessage(requireView(), msgRes);
            }
        });
    }

    private void clearErrors(@NonNull EditText... fields) {
        UiFormUtils.clearErrors(fields);
    }

    private void setFieldError(@NonNull EditText field, @NonNull String message) {
        UiFormUtils.setError(field, message);
    }

    private double parseDecimal(@NonNull String value) {
        return Double.parseDouble(value.trim().replace(',', '.'));
    }

    private void applyCurrencyPrefix(@NonNull EditText... fields) {
        String prefix = SettingsService.getCurrencySymbol(requireContext()) + " ";
        for (EditText field : fields) {
            ViewParent parent = field.getParent();
            while (parent != null && !(parent instanceof TextInputLayout)) {
                parent = parent.getParent();
            }
            if (parent instanceof TextInputLayout) {
                ((TextInputLayout) parent).setPrefixText(prefix);
            }
        }
    }
}
