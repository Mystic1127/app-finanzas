package com.example.finanzas.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.finanzas.R;
import com.example.finanzas.data.api.SettingsService;
import com.example.finanzas.data.model.PaymentReminder;
import com.example.finanzas.ui.adapter.ReminderSummaryAdapter;
import com.example.finanzas.ui.viewmodel.RemindersViewModel;
import com.example.finanzas.util.UiFormUtils;
import com.example.finanzas.util.ReminderScheduler;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RemindersFragment extends Fragment {

    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipe;
    private ReminderSummaryAdapter adapter;
    private TextView tvEmpty;
    private RemindersViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_reminders, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        swipe = v.findViewById(R.id.swipeReminders);
        tvEmpty = v.findViewById(R.id.tvRemindersEmpty);
        androidx.recyclerview.widget.RecyclerView rv = v.findViewById(R.id.rvReminders);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ReminderSummaryAdapter();
        rv.setAdapter(adapter);
        viewModel = new ViewModelProvider(this).get(RemindersViewModel.class);

        adapter.setListener(new ReminderSummaryAdapter.Listener() {
            @Override public void onReminderClick(PaymentReminder reminder) { mostrarDialogo(reminder); }
            @Override public void onReminderMarkPaid(PaymentReminder reminder) { marcarPagado(reminder); }
            @Override public void onReminderLongClick(PaymentReminder reminder) { confirmarEliminar(reminder); }
        });

        FloatingActionButton fab = v.findViewById(R.id.fabAddReminder);
        fab.setOnClickListener(view -> mostrarDialogo(null));

        swipe.setOnRefreshListener(this::cargarRecordatorios);
        observeViewModel();
    }

    @Override
    public void onResume() {
        super.onResume();
        cargarRecordatorios();
    }

    private void cargarRecordatorios() {
        viewModel.loadReminders(false);
    }

    private void mostrarDialogo(@Nullable PaymentReminder reminder) {
        View form = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_reminder, null, false);
        EditText etTitulo = form.findViewById(R.id.etReminderTitulo);
        EditText etMonto = form.findViewById(R.id.etReminderMonto);
        EditText etFecha = form.findViewById(R.id.etReminderFecha);
        EditText etHora = form.findViewById(R.id.etReminderHora);
        EditText etDias = form.findViewById(R.id.etReminderDias);
        MaterialAutoCompleteTextView actFrecuencia = form.findViewById(R.id.actReminderFrecuencia);
        SwitchMaterial swNotificar = form.findViewById(R.id.swReminderNotificar);
        applyCurrencyPrefix(etMonto);

        final String[] freqValues = new String[]{"once", "mensual", "trimestral"};
        String[] freqLabels = new String[]{
                getString(R.string.reminder_frequency_once),
                getString(R.string.reminder_frequency_monthly),
                getString(R.string.reminder_frequency_quarterly)
        };
        ArrayAdapter<String> freqAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, freqLabels);
        freqAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        actFrecuencia.setAdapter(freqAdapter);
        actFrecuencia.setInputType(0);
        actFrecuencia.setOnFocusChangeListener((view, hasFocus) -> { if (hasFocus) actFrecuencia.showDropDown(); });
        actFrecuencia.setOnClickListener(view -> actFrecuencia.showDropDown());
        actFrecuencia.setText(freqLabels[0], false);

        boolean editando = reminder != null;
        if (editando) {
            etTitulo.setText(reminder.getTitulo());
            etMonto.setText(String.valueOf(reminder.getMonto()));
            if (reminder.getFechaVencimiento() != null) {
                etFecha.setText(UiFormUtils.formatUiDate(reminder.getFechaVencimiento()));
            }
            if (reminder.getHoraRecordatorio() != null) {
                String hora = reminder.getHoraRecordatorio();
                if (hora.length() >= 5) {
                    etHora.setText(hora.substring(0, 5));
                } else {
                    etHora.setText(hora);
                }
            }
            if (reminder.getDiasRecordatorio() > 0) {
                etDias.setText(String.valueOf(reminder.getDiasRecordatorio()));
            }
            String freq = reminder.getFrecuencia() == null ? "once" : reminder.getFrecuencia();
            int selIndex = 0;
            for (int i = 0; i < freqValues.length; i++) {
                if (freqValues[i].equalsIgnoreCase(freq)) {
                    selIndex = i;
                    break;
                }
            }
            actFrecuencia.setText(freqLabels[selIndex], false);
            swNotificar.setChecked(reminder.isNotificar());
        } else {
            swNotificar.setChecked(true);
        }

        UiFormUtils.bindDatePicker(requireContext(), etFecha);
        UiFormUtils.bindTimePicker(requireContext(), etHora);
        UiFormUtils.clearErrorOnTextChange(etTitulo, etMonto, etFecha, etHora, etDias);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(editando ? R.string.reminder_dialog_title_edit : R.string.reminder_dialog_title_new)
                .setView(form)
                .setPositiveButton(R.string.reminder_btn_save, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(button -> {
            clearErrors(etTitulo, etMonto, etFecha, etHora, etDias);

            String titulo = etTitulo.getText() == null ? "" : etTitulo.getText().toString().trim();
            String montoStr = etMonto.getText() == null ? "" : etMonto.getText().toString().trim();
            String fecha = etFecha.getText() == null ? "" : etFecha.getText().toString().trim();
            String hora = etHora.getText() == null ? "" : etHora.getText().toString().trim();
            String diasStr = etDias.getText() == null ? "" : etDias.getText().toString().trim();
            int freqIndex = findFrequencyIndex(actFrecuencia.getText() == null ? "" : actFrecuencia.getText().toString(), freqLabels);
            if (freqIndex < 0 || freqIndex >= freqValues.length) freqIndex = 0;
            String frecuencia = freqValues[freqIndex];
            boolean notificar = swNotificar.isChecked();

            boolean valido = true;
            if (TextUtils.isEmpty(titulo)) {
                setFieldError(etTitulo, getString(R.string.error_campos_obligatorios));
                valido = false;
            }
            if (TextUtils.isEmpty(montoStr)) {
                setFieldError(etMonto, getString(R.string.error_campos_obligatorios));
                valido = false;
            }
            if (TextUtils.isEmpty(fecha)) {
                setFieldError(etFecha, getString(R.string.error_campos_obligatorios));
                valido = false;
            } else if (!UiFormUtils.isValidUiDate(fecha)) {
                setFieldError(etFecha, "Usa el formato dd/MM/yyyy");
                valido = false;
            }
            if (!TextUtils.isEmpty(hora) && !UiFormUtils.isValidTime(hora)) {
                setFieldError(etHora, "Usa el formato HH:mm");
                valido = false;
            }
            if (!valido) {
                return;
            }

            double montoVal = parseMontoSeguro(montoStr);
            if (montoVal <= 0) {
                setFieldError(etMonto, "Ingresa un monto mayor a 0");
                return;
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

            String notificationId = reminder != null ? reminder.getNotificationId() : null;
            if (notificar && (notificationId == null || notificationId.isEmpty())) {
                notificationId = UUID.randomUUID().toString();
            }

            String fechaIso = UiFormUtils.uiDateToIso(fecha);
            if (fechaIso == null) {
                setFieldError(etFecha, "Usa el formato dd/MM/yyyy");
                return;
            }

            JSONObject body = new JSONObject();
            try {
                if (editando && reminder != null) body.put("id", reminder.getId());
                body.put("titulo", titulo);
                body.put("monto", montoVal);
                body.put("fecha_vencimiento", fechaIso);
                if (!TextUtils.isEmpty(hora)) body.put("hora_recordatorio", hora);
                body.put("dias_recordatorio", diasVal);
                body.put("frecuencia", frecuencia);
                body.put("notificar", notificar);
                if (!TextUtils.isEmpty(notificationId)) {
                    body.put("notification_id", notificationId);
                }
            } catch (Exception ignore) { }

            viewModel.saveReminder(body);
            dialog.dismiss();
        }));
        dialog.show();
    }

    private void marcarPagado(PaymentReminder reminder) {
        ReminderScheduler.cancel(requireContext(), reminder);
        viewModel.markPaid(reminder.getId(), true);
    }

    private void confirmarEliminar(PaymentReminder reminder) {
        new AlertDialog.Builder(requireContext())
                .setMessage(R.string.reminder_confirm_delete)
                .setPositiveButton(R.string.btn_eliminar, (d, w) -> {
                    ReminderScheduler.cancel(requireContext(), reminder);
                    viewModel.delete(reminder.getId());
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void observeViewModel() {
        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> swipe.setRefreshing(Boolean.TRUE.equals(loading)));
        viewModel.getReminders().observe(getViewLifecycleOwner(), items -> {
            List<PaymentReminder> safeItems = items == null ? new ArrayList<>() : items;
            adapter.setItems(new ArrayList<>(safeItems));
            tvEmpty.setVisibility(safeItems.isEmpty() ? View.VISIBLE : View.GONE);
            for (PaymentReminder item : safeItems) {
                if (item == null) continue;
                if (item.isNotificar() && !item.isPagado()) {
                    ReminderScheduler.schedule(requireContext(), item);
                } else {
                    ReminderScheduler.cancel(requireContext(), item);
                }
            }
        });
        viewModel.getMessage().observe(getViewLifecycleOwner(), msgRes -> {
            if (msgRes != null) UiFormUtils.showMessage(requireView(), msgRes);
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
        try {
            return Double.parseDouble(limpio);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void clearErrors(@NonNull EditText... fields) {
        UiFormUtils.clearErrors(fields);
    }

    private void setFieldError(@NonNull EditText field, @NonNull String message) {
        UiFormUtils.setError(field, message);
    }

    private int findFrequencyIndex(@NonNull String selected, @NonNull String[] labels) {
        for (int i = 0; i < labels.length; i++) {
            if (labels[i].equals(selected)) {
                return i;
            }
        }
        return 0;
    }

    private void applyCurrencyPrefix(@NonNull EditText field) {
        String prefix = SettingsService.getCurrencySymbol(requireContext()) + " ";
        ViewParent parent = field.getParent();
        while (parent != null && !(parent instanceof TextInputLayout)) {
            parent = parent.getParent();
        }
        if (parent instanceof TextInputLayout) {
            ((TextInputLayout) parent).setPrefixText(prefix);
        }
    }
}
