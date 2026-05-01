package com.example.finanzas.ui.adapter;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finanzas.R;
import com.example.finanzas.data.api.SettingsService;
import com.example.finanzas.data.model.CategoryBudgetInput;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

public class CategoryBudgetEditAdapter extends RecyclerView.Adapter<CategoryBudgetEditAdapter.VH> {

    private final List<CategoryBudgetInput> items = new ArrayList<>();

    public void setItems(List<CategoryBudgetInput> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    public List<CategoryBudgetInput> getItems() {
        List<CategoryBudgetInput> copy = new ArrayList<>();
        for (CategoryBudgetInput item : items) {
            CategoryBudgetInput clone = new CategoryBudgetInput();
            clone.setCategoriaId(item.getCategoriaId());
            clone.setCategoriaNombre(item.getCategoriaNombre());
            clone.setMonto(item.getMonto());
            clone.setMoneda(item.getMoneda());
            copy.add(clone);
        }
        return copy;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_budget_edit, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        CategoryBudgetInput item = items.get(position);
        holder.tvNombre.setText(item.getCategoriaNombre());
        holder.tilMonto.setHint(holder.itemView.getContext().getString(R.string.pres_category_hint, item.getCategoriaNombre()));
        holder.tilMonto.setPrefixText(SettingsService.getCurrencySymbol(item.getMoneda()) + " ");

        if (holder.watcher != null) {
            holder.etMonto.removeTextChangedListener(holder.watcher);
        }
        holder.etMonto.setText(item.getMonto() > 0 ? String.valueOf(item.getMonto()) : "");
        holder.watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable s) {
                double value = 0;
                try { value = Double.parseDouble(s.toString()); } catch (Exception ignore) {}
                item.setMonto(value);
            }
        };
        holder.etMonto.addTextChangedListener(holder.watcher);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvNombre;
        final TextInputLayout tilMonto;
        final TextInputEditText etMonto;
        TextWatcher watcher;
        VH(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvCategoriaNombre);
            tilMonto = itemView.findViewById(R.id.tilCategoriaMonto);
            etMonto = itemView.findViewById(R.id.etCategoriaMonto);
        }
    }
}
