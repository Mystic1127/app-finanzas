package com.example.finanzas.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finanzas.R;
import com.example.finanzas.data.api.SettingsService;
import com.example.finanzas.data.model.SavingsGoal;
import com.example.finanzas.data.model.GoalMilestone;
import com.example.finanzas.util.Format;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;

public class GoalSummaryAdapter extends RecyclerView.Adapter<GoalSummaryAdapter.VH> {

    public interface Listener {
        void onGoalClick(SavingsGoal goal);
        void onGoalLongClick(SavingsGoal goal);
        void onMilestoneClick(View anchor, SavingsGoal goal, GoalMilestone milestone);
        void onAddMilestone(SavingsGoal goal);
    }

    private final List<SavingsGoal> items = new ArrayList<>();
    private Listener listener;

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setItems(List<SavingsGoal> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_goal, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        SavingsGoal item = items.get(position);
        String currency = SettingsService.getCurrencyCode(holder.itemView.getContext());
        holder.tvTitulo.setText(item.getTitulo());

        String monto = holder.itemView.getContext().getString(
                R.string.goal_amount_detail,
                Format.money(item.getMontoActual(), currency),
                Format.money(item.getMontoObjetivo(), currency)
        );
        holder.tvMonto.setText(monto);

        int progress = (int) Math.round(Math.min(100, Math.max(0, item.getProgreso())));
        holder.progress.setProgressCompat(progress, true);
        holder.tvFecha.setText(item.getFechaObjetivo() == null
                ? holder.itemView.getContext().getString(R.string.goal_without_deadline)
                : holder.itemView.getContext().getString(R.string.goal_deadline, Format.date(item.getFechaObjetivo())));

        if (holder.chipGroup != null) {
            holder.chipGroup.removeAllViews();
            LayoutInflater inflater = LayoutInflater.from(holder.itemView.getContext());
            if (item.getHitos() != null && !item.getHitos().isEmpty()) {
                for (GoalMilestone milestone : item.getHitos()) {
                    Chip chip = (Chip) inflater.inflate(R.layout.chip_goal_milestone, holder.chipGroup, false);
                    StringBuilder label = new StringBuilder();
                    if (milestone.getTitulo() != null) {
                        label.append(milestone.getTitulo());
                    }
                    if (milestone.getMontoPlanificado() > 0) {
                        if (label.length() > 0) label.append(" • ");
                        label.append(Format.money(milestone.getMontoPlanificado(), currency));
                    }
                    if (milestone.getFechaObjetivo() != null) {
                        if (label.length() > 0) label.append(" • ");
                        label.append(Format.date(milestone.getFechaObjetivo()));
                    }
                    chip.setText(label.toString());
                    chip.setEnabled(!milestone.isCompletado());
                    chip.setAlpha(milestone.isCompletado() ? 0.6f : 1f);
                    chip.setOnClickListener(v -> {
                        if (listener != null) listener.onMilestoneClick(v, item, milestone);
                    });
                    holder.chipGroup.addView(chip);
                }
            }

            Chip addChip = (Chip) inflater.inflate(R.layout.chip_goal_add, holder.chipGroup, false);
            addChip.setText(holder.itemView.getContext().getString(R.string.goal_add_milestone));
            addChip.setOnClickListener(v -> {
                if (listener != null) listener.onAddMilestone(item);
            });
            holder.chipGroup.addView(addChip);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onGoalClick(item);
        });
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) listener.onGoalLongClick(item);
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
        final LinearProgressIndicator progress;
        final ChipGroup chipGroup;
        VH(@NonNull View itemView) {
            super(itemView);
            tvTitulo = itemView.findViewById(R.id.tvGoalTitulo);
            tvMonto = itemView.findViewById(R.id.tvGoalMonto);
            tvFecha = itemView.findViewById(R.id.tvGoalFecha);
            progress = itemView.findViewById(R.id.progresoMeta);
            chipGroup = itemView.findViewById(R.id.chipGroupHitos);
        }
    }
}
