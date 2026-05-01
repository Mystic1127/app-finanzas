package com.example.finanzas.ui.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finanzas.R;
import com.example.finanzas.data.model.PaymentReminder;
import com.example.finanzas.util.Format;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class ReminderSummaryAdapter extends RecyclerView.Adapter<ReminderSummaryAdapter.VH> {

    public interface Listener {
        void onReminderClick(PaymentReminder reminder);
        void onReminderMarkPaid(PaymentReminder reminder);
        void onReminderLongClick(PaymentReminder reminder);
    }

    private final List<PaymentReminder> items = new ArrayList<>();
    private Listener listener;

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setItems(List<PaymentReminder> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reminder, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        PaymentReminder item = items.get(position);
        String currency = item.getMoneda();
        holder.tvTitulo.setText(item.getTitulo());
        holder.tvMonto.setText(holder.itemView.getContext().getString(R.string.reminder_amount, Format.money(item.getMonto(), currency)));

        String fechaTexto = Format.date(item.getFechaVencimiento());
        if (!item.isPagado()) {
            int dias = item.getDiasRestantes();
            if (dias < 0) {
                fechaTexto = holder.itemView.getContext().getString(R.string.reminder_overdue, fechaTexto, Math.abs(dias));
            } else if (dias == 0) {
                fechaTexto = holder.itemView.getContext().getString(R.string.reminder_due_today, fechaTexto);
            } else {
                fechaTexto = holder.itemView.getContext().getString(R.string.reminder_due_in, fechaTexto, dias);
            }
        } else {
            fechaTexto = holder.itemView.getContext().getString(R.string.reminder_paid_on, fechaTexto);
        }
        holder.tvFecha.setText(fechaTexto);

        if (item.isPagado()) {
            holder.btnPagado.setText(R.string.reminder_mark_paid_done);
            holder.btnPagado.setEnabled(false);
        } else {
            holder.btnPagado.setText(R.string.reminder_mark_paid);
            holder.btnPagado.setEnabled(true);
            holder.btnPagado.setOnClickListener(v -> {
                if (listener != null) listener.onReminderMarkPaid(item);
            });
        }

        if (holder.tvProgramacion != null) {
            StringBuilder detalle = new StringBuilder();
            if (item.getDiasRecordatorio() > 0) {
                detalle.append(holder.itemView.getContext().getString(R.string.reminder_schedule_notify, item.getDiasRecordatorio()));
            }
            String hora = item.getHoraRecordatorio();
            if (!TextUtils.isEmpty(hora)) {
                String horaCorta = hora.length() >= 5 ? hora.substring(0, 5) : hora;
                if (detalle.length() > 0) detalle.append(" • ");
                detalle.append(holder.itemView.getContext().getString(R.string.reminder_schedule_time, horaCorta));
            }
            String frecuencia = item.getFrecuencia();
            if (!TextUtils.isEmpty(frecuencia) && !"once".equalsIgnoreCase(frecuencia)) {
                String label = null;
                if ("mensual".equalsIgnoreCase(frecuencia)) {
                    label = holder.itemView.getContext().getString(R.string.reminder_frequency_monthly);
                } else if ("trimestral".equalsIgnoreCase(frecuencia)) {
                    label = holder.itemView.getContext().getString(R.string.reminder_frequency_quarterly);
                }
                if (!TextUtils.isEmpty(label)) {
                    if (detalle.length() > 0) detalle.append(" • ");
                    detalle.append(holder.itemView.getContext().getString(R.string.reminder_schedule_frequency, label));
                }
            }
            if (detalle.length() > 0) {
                holder.tvProgramacion.setVisibility(View.VISIBLE);
                holder.tvProgramacion.setText(detalle.toString());
            } else {
                holder.tvProgramacion.setVisibility(View.GONE);
                holder.tvProgramacion.setText("");
            }
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onReminderClick(item);
        });
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) listener.onReminderLongClick(item);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvTitulo;
        final TextView tvMonto;
        final TextView tvFecha;
        final MaterialButton btnPagado;
        final TextView tvProgramacion;
        VH(@NonNull View itemView) {
            super(itemView);
            tvTitulo = itemView.findViewById(R.id.tvReminderTitulo);
            tvMonto = itemView.findViewById(R.id.tvReminderMonto);
            tvFecha = itemView.findViewById(R.id.tvReminderFecha);
            btnPagado = itemView.findViewById(R.id.btnMarcarPagado);
            tvProgramacion = itemView.findViewById(R.id.tvReminderProgramacion);
        }
    }
}
