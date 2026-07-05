package com.example.finanzas.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finanzas.R;
import com.example.finanzas.data.model.ImportRule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImportRuleAdapter extends RecyclerView.Adapter<ImportRuleAdapter.Holder> {

    public interface Listener {
        void onRuleSelected(ImportRule rule);
        void onRuleLongPress(ImportRule rule);
    }

    private final List<ImportRule> items = new ArrayList<>();
    private final Listener listener;
    private Map<Integer, String> categoryNames = Collections.emptyMap();

    public ImportRuleAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setItems(List<ImportRule> nuevos) {
        items.clear();
        if (nuevos != null) items.addAll(nuevos);
        notifyDataSetChanged();
    }

    public void setCategoryNames(@Nullable Map<Integer, String> nombres) {
        if (nombres == null) {
            this.categoryNames = Collections.emptyMap();
        } else {
            this.categoryNames = new HashMap<>(nombres);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_import_rule, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        ImportRule rule = items.get(position);
        holder.tvPattern.setText(rule.getPatron());
        String categoria = holder.itemView.getContext().getString(R.string.import_rule_any_category);
        if (rule.getCategoriaId() != null && categoryNames.containsKey(rule.getCategoriaId())) {
            categoria = categoryNames.get(rule.getCategoriaId());
        } else if (rule.getCategoriaId() != null) {
            categoria = String.valueOf(rule.getCategoriaId());
        }
        String detalle = holder.itemView.getContext().getString(
                R.string.import_rule_detail,
                categoria,
                rule.isIngreso() ? holder.itemView.getContext().getString(R.string.tipo_ingreso_on)
                        : holder.itemView.getContext().getString(R.string.tipo_ingreso_off));
        if (rule.getDescripcion() != null && !rule.getDescripcion().isEmpty()) {
            detalle += " • " + rule.getDescripcion();
        }
        holder.tvDetails.setText(detalle);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onRuleSelected(rule);
        });
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) listener.onRuleLongPress(rule);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView tvPattern;
        final TextView tvDetails;

        Holder(@NonNull View itemView) {
            super(itemView);
            tvPattern = itemView.findViewById(R.id.tvRulePattern);
            tvDetails = itemView.findViewById(R.id.tvRuleDetails);
        }
    }
}
