package com.example.finanzas.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finanzas.R;
import com.example.finanzas.data.model.HouseholdSummary;
import com.example.finanzas.util.Format;

import java.util.ArrayList;
import java.util.List;

public class HouseholdSummaryAdapter extends RecyclerView.Adapter<HouseholdSummaryAdapter.Holder> {

    public interface Listener {
        void onSelect(HouseholdSummary household);
    }

    private final List<HouseholdSummary> items = new ArrayList<>();
    private Listener listener;

    public void setItems(List<HouseholdSummary> nuevos) {
        items.clear();
        if (nuevos != null) items.addAll(nuevos);
        notifyDataSetChanged();
    }

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_household, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        HouseholdSummary h = items.get(position);
        holder.tvName.setText(h.getNombre());
        holder.tvRole.setText(holder.itemView.getContext().getString(R.string.household_role_label, h.getRol()));
        holder.tvMembers.setText(holder.itemView.getContext().getString(
                R.string.household_members_label, h.getMiembros()));
        holder.tvTotals.setText(holder.itemView.getContext().getString(
                R.string.household_totals_label,
                Format.money(h.getIngresos()),
                Format.money(h.getGastos()),
                Format.money(h.getSaldo())));
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onSelect(h);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView tvName;
        final TextView tvRole;
        final TextView tvMembers;
        final TextView tvTotals;

        Holder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvHouseholdName);
            tvRole = itemView.findViewById(R.id.tvHouseholdRole);
            tvMembers = itemView.findViewById(R.id.tvHouseholdMembers);
            tvTotals = itemView.findViewById(R.id.tvHouseholdTotals);
        }
    }
}
