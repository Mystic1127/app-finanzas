package com.example.finanzas.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finanzas.R;
import com.example.finanzas.data.api.SettingsService;
import com.example.finanzas.data.model.CategoryBudgetSummary;
import com.example.finanzas.util.Format;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.List;

public class CategoryBudgetSummaryAdapter extends RecyclerView.Adapter<CategoryBudgetSummaryAdapter.VH> {

    private final List<CategoryBudgetSummary> items = new ArrayList<>();

    public void setItems(List<CategoryBudgetSummary> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_budget_category, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        CategoryBudgetSummary item = items.get(position);
        String currency = SettingsService.getCurrencyCode(holder.itemView.getContext());
        holder.tvNombre.setText(item.getCategoriaNombre());

        String detalle = holder.itemView.getContext().getString(
                R.string.home_budget_detail,
                Format.money(item.getGastado(), currency),
                Format.money(item.getLimite(), currency),
                Format.money(item.getDisponible(), currency)
        );
        holder.tvDetalle.setText(detalle);

        int progress = (int) Math.round(Math.min(100, Math.max(0, item.getPorcentaje())));
        holder.progress.setProgressCompat(progress, true);
        holder.tvPorcentaje.setText(Format.percent(item.getPorcentaje()));

        int color = item.isAlerta()
                ? holder.itemView.getResources().getColor(R.color.md_theme_error, holder.itemView.getContext().getTheme())
                : holder.itemView.getResources().getColor(R.color.md_theme_primary, holder.itemView.getContext().getTheme());
        holder.progress.setIndicatorColor(color);
        holder.tvPorcentaje.setTextColor(color);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvNombre;
        final TextView tvDetalle;
        final TextView tvPorcentaje;
        final LinearProgressIndicator progress;
        VH(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvNombreCategoria);
            tvDetalle = itemView.findViewById(R.id.tvDetalleCategoria);
            tvPorcentaje = itemView.findViewById(R.id.tvPorcentajeCategoria);
            progress = itemView.findViewById(R.id.progresoCategoria);
        }
    }
}
