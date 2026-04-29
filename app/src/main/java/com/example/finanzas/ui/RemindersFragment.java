package com.example.finanzas.ui;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.finanzas.R;
import com.example.finanzas.data.model.PaymentReminder;
import com.example.finanzas.ui.adapter.ReminderSummaryAdapter;
import com.example.finanzas.ui.viewmodel.RemindersViewModel;
import com.example.finanzas.util.ReminderScheduler;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
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
        Spinner spFrecuencia = form.findViewById(R.id.spReminderFrecuencia);
        SwitchMaterial swNotificar = form.findViewById(R.id.swReminderNotificar);

        final String[] freqValues = new String[]{"once", "mensual", "trimestral"};
        String[] freqLabels = new String[]{
                getString(R.string.reminder_frequency_once),
                getString(R.string.reminder_frequency_monthly),
                getString(R.string.reminder_frequency_quarterly)
        };
        ArrayAdapter<String> freqAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, freqLabels);
        freqAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spFrecuencia.setAdapter(freqAdapter);

        boolean editando = reminder != null;
        if (editando) {
            etTitulo.setText(reminder.getTitulo());
            etMonto.setText(String.valueOf(reminder.getMonto()));
            if (reminder.getFechaVencimiento() != null) {
                java.text.SimpleDateFormat df = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
                etFecha.setText(df.format(reminder.getFechaVencimiento()));
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
            spFrecuencia.setSelection(selIndex);
            swNotificar.setChecked(reminder.isNotificar());
        } else {
            swNotificar.setChecked(true);
        }

        setupDatePicker(etFecha);
        setupTimePicker(etHora);

        new AlertDialog.Builder(requireContext())
                .setTitle(editando ? R.string.reminder_dialog_title_edit : R.string.reminder_dialog_title_new)
                .setView(form)
                .setPositiveButton(R.string.reminder_btn_save, (dialog, which) -> {
                    String titulo = etTitulo.getText() == null ? "" : etTitulo.getText().toString().trim();
                    String montoStr = etMonto.getText() == null ? "" : etMonto.getText().toString().trim();
                    String fecha = etFecha.getText() == null ? "" : etFecha.getText().toString().trim();
                    String hora = etHora.getText() == null ? "" : etHora.getText().toString().trim();
                    String diasStr = etDias.getText() == null ? "" : etDias.getText().toString().trim();
                    int freqIndex = spFrecuencia.getSelectedItemPosition();
                    if (freqIndex < 0 || freqIndex >= freqValues.length) freqIndex = 0;
                    String frecuencia = freqValues[freqIndex];
                    boolean notificar = swNotificar.isChecked();

                    if (TextUtils.isEmpty(titulo) || TextUtils.isEmpty(montoStr) || TextUtils.isEmpty(fecha)) {
                        Toast.makeText(requireContext(), R.string.error_campos_obligatorios, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    double montoVal = parseMontoSeguro(montoStr);
                    if (montoVal <= 0) {
                        Toast.makeText(requireContext(), R.string.reminder_field_amount, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int diasVal = 0;
                    if (!diasStr.isEmpty()) {
                        try {
                            diasVal = Integer.parseInt(diasStr);
                        } catch (NumberFormatException e) {
                            Toast.makeText(requireContext(), R.string.reminder_field_days, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (diasVal < 0) diasVal = 0;
                    }

                    // 👇 copia final para poder usarla dentro de la clase interna
                    final int finalDiasVal = diasVal;


                    String notificationId = reminder != null ? reminder.getNotificationId() : null;
                    if (notificar && (notificationId == null || notificationId.isEmpty())) {
                        notificationId = UUID.randomUUID().toString();
                    }

                    JSONObject body = new JSONObject();
                    try {
                        if (editando && reminder != null) body.put("id", reminder.getId());
                        body.put("titulo", titulo);
                        body.put("monto", montoVal);
                        body.put("fecha_vencimiento", fecha);
                        if (!TextUtils.isEmpty(hora)) body.put("hora_recordatorio", hora);
                        body.put("dias_recordatorio", finalDiasVal);
                        body.put("frecuencia", frecuencia);
                        body.put("notificar", notificar);
                        if (!TextUtils.isEmpty(notificationId)) {
                            body.put("notification_id", notificationId);
                        }
                    } catch (Exception ignore) { }

                    String finalNotificationId = notificationId;
                    viewModel.saveReminder(body);
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


    private void setupTimePicker(@NonNull EditText input) {
        input.setFocusable(false);
        input.setClickable(true);
        input.setOnClickListener(v -> showTimePicker(input));
        input.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) showTimePicker(input); });
    }

    private void showTimePicker(@NonNull EditText input) {
        Calendar calendar = Calendar.getInstance();
        String current = input.getText() == null ? "" : input.getText().toString().trim();
        if (current.matches("^\\d{2}:\\d{2}$")) {
            try {
                String[] parts = current.split(":");
                calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(parts[0]));
                calendar.set(Calendar.MINUTE, Integer.parseInt(parts[1]));
            } catch (Exception ignored) { }
        }

        TimePickerDialog dialog = new TimePickerDialog(requireContext(),
                (view, hourOfDay, minute) -> input.setText(String.format(Locale.US, "%02d:%02d", hourOfDay, minute)),
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true);
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
            if (msgRes != null) Toast.makeText(requireContext(), msgRes, Toast.LENGTH_SHORT).show();
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
}
