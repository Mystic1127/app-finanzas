package com.example.finanzas.ui;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.finanzas.R;
import com.example.finanzas.data.model.SavingsGoal;
import com.example.finanzas.data.model.GoalMilestone;
import com.example.finanzas.ui.adapter.GoalSummaryAdapter;
import com.example.finanzas.ui.viewmodel.GoalsViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
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

        setupDatePicker(etFecha);

        boolean editando = goal != null;
        if (editando) {
            etTitulo.setText(goal.getTitulo());
            etObjetivo.setText(String.valueOf(goal.getMontoObjetivo()));
            etActual.setText(String.valueOf(goal.getMontoActual()));
            if (goal.getFechaObjetivo() != null) {
                java.text.SimpleDateFormat df = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
                etFecha.setText(df.format(goal.getFechaObjetivo()));
            }
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(editando ? R.string.goal_dialog_title_edit : R.string.goal_dialog_title_new)
                .setView(form)
                .setPositiveButton(R.string.goal_btn_save, (dialog, which) -> {
                    String titulo = etTitulo.getText() == null ? "" : etTitulo.getText().toString().trim();
                    String objetivo = etObjetivo.getText() == null ? "" : etObjetivo.getText().toString().trim();
                    String actual = etActual.getText() == null ? "" : etActual.getText().toString().trim();
                    String fecha = etFecha.getText() == null ? "" : etFecha.getText().toString().trim();

                    if (TextUtils.isEmpty(titulo)) {
                        Toast.makeText(requireContext(), R.string.error_campos_obligatorios, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    double objetivoVal = 0;
                    double actualVal = 0;
                    try { objetivoVal = objetivo.isEmpty() ? 0 : Double.parseDouble(objetivo); }
                    catch (NumberFormatException e) { Toast.makeText(requireContext(), R.string.goal_field_target, Toast.LENGTH_SHORT).show(); return; }
                    try { actualVal = actual.isEmpty() ? 0 : Double.parseDouble(actual); }
                    catch (NumberFormatException e) { Toast.makeText(requireContext(), R.string.goal_field_current, Toast.LENGTH_SHORT).show(); return; }

                    JSONObject body = new JSONObject();
                    try {
                        if (editando && goal != null) body.put("id", goal.getId());
                        body.put("titulo", titulo);
                        body.put("monto_objetivo", objetivoVal);
                        body.put("monto_actual", actualVal);
                        if (!fecha.isEmpty()) body.put("fecha_objetivo", fecha);
                    } catch (Exception ignore) { }

                    viewModel.saveGoal(body);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void setupDatePicker(@NonNull EditText input) {
        input.setFocusable(false);
        input.setOnClickListener(v -> showDatePicker(input));
        input.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) showDatePicker(input); });
    }

    private void showDatePicker(@NonNull EditText input) {
        java.text.SimpleDateFormat df = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
        Calendar calendar = Calendar.getInstance();
        String current = input.getText() == null ? "" : input.getText().toString();
        try {
            java.util.Date parsed = df.parse(current);
            if (parsed != null) calendar.setTime(parsed);
        } catch (Exception ignored) { }

        DatePickerDialog dialog = new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            input.setText(df.format(calendar.getTime()));
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

        dialog.show();
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

        boolean editando = milestone != null;
        if (editando) {
            etTitulo.setText(milestone.getTitulo());
            if (milestone.getMontoPlanificado() > 0) {
                etMonto.setText(String.valueOf(milestone.getMontoPlanificado()));
            }
            if (milestone.getFechaObjetivo() != null) {
                java.text.SimpleDateFormat df = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
                etFecha.setText(df.format(milestone.getFechaObjetivo()));
            }
            if (milestone.getDiasRecordatorio() > 0) {
                etDias.setText(String.valueOf(milestone.getDiasRecordatorio()));
            }
            swNotificar.setChecked(milestone.isNotificar());
            cbCompletado.setChecked(milestone.isCompletado());
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(editando ? R.string.goal_milestone_edit : R.string.goal_milestone_new)
                .setView(form)
                .setPositiveButton(R.string.goal_btn_save, (dialog, which) -> {
                    String titulo = etTitulo.getText() == null ? "" : etTitulo.getText().toString().trim();
                    String montoStr = etMonto.getText() == null ? "" : etMonto.getText().toString().trim();
                    String fecha = etFecha.getText() == null ? "" : etFecha.getText().toString().trim();
                    String diasStr = etDias.getText() == null ? "" : etDias.getText().toString().trim();

                    if (titulo.isEmpty()) {
                        Toast.makeText(requireContext(), R.string.error_campos_obligatorios, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    double montoVal = 0;
                    if (!montoStr.isEmpty()) {
                        try {
                            montoVal = Double.parseDouble(montoStr);
                        } catch (NumberFormatException e) {
                            Toast.makeText(requireContext(), R.string.goal_milestone_amount, Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                    int diasVal = 0;
                    if (!diasStr.isEmpty()) {
                        try {
                            diasVal = Integer.parseInt(diasStr);
                        } catch (NumberFormatException e) {
                            Toast.makeText(requireContext(), R.string.goal_milestone_days, Toast.LENGTH_SHORT).show();
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
                        if (!fecha.isEmpty()) body.put("fecha_objetivo", fecha);
                        body.put("notificar", swNotificar.isChecked());
                        body.put("dias_recordatorio", diasVal);
                        body.put("completado", cbCompletado.isChecked());
                    } catch (Exception ignore) { }

                    viewModel.saveMilestone(body);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
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
                java.text.SimpleDateFormat df = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
                body.put("fecha_objetivo", df.format(milestone.getFechaObjetivo()));
            }
            body.put("notificar", milestone.isNotificar());
            body.put("dias_recordatorio", milestone.getDiasRecordatorio());
            body.put("completado", completado);
        } catch (Exception ignore) { }

        viewModel.saveMilestone(body);
    }

    private void observeViewModel() {
        viewModel.loading.observe(getViewLifecycleOwner(), loading -> swipe.setRefreshing(Boolean.TRUE.equals(loading)));
        viewModel.goals.observe(getViewLifecycleOwner(), items -> {
            List<SavingsGoal> safe = items == null ? new ArrayList<>() : items;
            adapter.setItems(new ArrayList<>(safe));
            tvEmpty.setVisibility(safe.isEmpty() ? View.VISIBLE : View.GONE);
        });
        viewModel.message.observe(getViewLifecycleOwner(), msgRes -> {
            if (msgRes != null) Toast.makeText(requireContext(), msgRes, Toast.LENGTH_SHORT).show();
        });
    }
}
