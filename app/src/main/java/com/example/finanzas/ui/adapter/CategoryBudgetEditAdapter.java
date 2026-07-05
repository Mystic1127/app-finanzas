package com.example.finanzas.ui.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finanzas.R;
import com.example.finanzas.data.api.SettingsService;
import com.example.finanzas.data.model.CategoryBudgetInput;
import com.example.finanzas.data.model.Categoria;
import com.example.finanzas.util.CategoryVisuals;
import com.example.finanzas.util.Format;
import com.example.finanzas.util.MicroAnimations;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CategoryBudgetEditAdapter extends RecyclerView.Adapter<CategoryBudgetEditAdapter.VH> {

    public interface Listener {
        void onBudgetChanged(@NonNull List<CategoryBudgetInput> items);
        void onBudgetDone(@NonNull List<CategoryBudgetInput> items);
        void onDelete(@NonNull CategoryBudgetInput item);
    }

    private final List<CategoryBudgetInput> items = new ArrayList<>();
    @Nullable
    private Listener listener;

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }

    public void setItems(List<CategoryBudgetInput> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    public List<CategoryBudgetInput> getItems() {
        List<CategoryBudgetInput> copy = new ArrayList<>();
        for (CategoryBudgetInput item : items) copy.add(copyOf(item));
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
        String currency = item.getMoneda();
        String symbol = SettingsService.getCurrencySymbol(currency);
        Categoria categoria = new Categoria(item.getCategoriaId(), item.getCategoriaNombre(), false);
        int accent = CategoryVisuals.colorFor(holder.itemView.getContext(), categoria);
        if (holder.boundCategoryId != item.getCategoriaId()) {
            holder.boundCategoryId = item.getCategoriaId();
            holder.lastProgress = -1;
        }

        holder.tvNombre.setText(item.getCategoriaNombre());
        holder.tilMonto.setPrefixText(symbol + " ");
        holder.icon.setImageResource(CategoryVisuals.iconFor(holder.itemView.getContext(), categoria));
        holder.icon.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), android.R.color.white));
        GradientDrawable iconBg = new GradientDrawable();
        iconBg.setShape(GradientDrawable.OVAL);
        iconBg.setColor(accent);
        holder.iconBg.setBackground(iconBg);

        if (holder.watcher != null) holder.etMonto.removeTextChangedListener(holder.watcher);
        holder.etMonto.setImeOptions(EditorInfo.IME_ACTION_DONE);
        holder.etMonto.setText(moneyNumber(item.getMonto()));
        holder.etMonto.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) {
                if (Math.abs(item.getMonto()) < 0.005) {
                    holder.etMonto.setText("");
                }
                return;
            }
            String value = holder.etMonto.getText() == null ? "" : holder.etMonto.getText().toString().trim();
            if (value.isEmpty()) {
                holder.etMonto.setText(moneyNumber(0.0));
            }
        });
        holder.etMonto.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_DONE) return false;
            hideKeyboard(view);
            view.clearFocus();
            if (listener != null) listener.onBudgetDone(getItems());
            return true;
        });
        holder.watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable s) {
                double value = parseAmount(s == null ? "" : s.toString());
                item.setMonto(value);
                item.setDisponible(value - item.getGastado());
                item.setPorcentaje(value > 0 ? (item.getGastado() / value) * 100.0 : 0.0);
                bindAmounts(holder, item, currency);
                if (listener != null) listener.onBudgetChanged(getItems());
            }
        };
        holder.etMonto.addTextChangedListener(holder.watcher);

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(copyOf(item));
        });
        bindAmounts(holder, item, currency);
    }

    private void bindAmounts(@NonNull VH holder, @NonNull CategoryBudgetInput item, @NonNull String currency) {
        double percentage = Math.max(0.0, Math.min(999.0, item.getPorcentaje()));
        holder.tvGastado.setText(Format.money(item.getGastado(), currency));
        holder.tvDisponible.setText(Format.money(item.getDisponible(), currency));
        holder.tvDisponible.setTextColor(ContextCompat.getColor(
                holder.itemView.getContext(),
                item.getDisponible() >= 0 ? R.color.income : R.color.expense
        ));
        boolean showProgress = item.getMonto() > 0;
        holder.progressContainer.setVisibility(showProgress ? View.VISIBLE : View.GONE);
        if (showProgress) {
            int progress = (int) Math.max(0, Math.min(100, Math.round(percentage)));
            boolean animateProgress = holder.lastProgress >= 0
                    && holder.lastProgress != progress
                    && MicroAnimations.areAnimationsEnabled(holder.itemView.getContext());
            holder.progress.setProgressCompat(progress, animateProgress);
            holder.lastProgress = progress;
            holder.progress.setIndicatorColor(ContextCompat.getColor(holder.itemView.getContext(),
                    item.getDisponible() >= 0 ? R.color.income : R.color.expense));
            holder.progress.setTrackColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.md_theme_outlineVariant));
            holder.tvPercent.setText(String.format(Locale.US, "%d%%", progress));
        } else {
            holder.lastProgress = -1;
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private static CategoryBudgetInput copyOf(@NonNull CategoryBudgetInput item) {
        CategoryBudgetInput clone = new CategoryBudgetInput();
        clone.setCategoriaId(item.getCategoriaId());
        clone.setCategoriaNombre(item.getCategoriaNombre());
        clone.setMonto(item.getMonto());
        clone.setMoneda(item.getMoneda());
        clone.setGastado(item.getGastado());
        clone.setDisponible(item.getDisponible());
        clone.setPorcentaje(item.getPorcentaje());
        return clone;
    }

    private static double parseAmount(@NonNull String raw) {
        String clean = raw.trim().replaceAll("[^0-9,.-]", "");
        if (clean.isEmpty()) return 0.0;
        int lastComma = clean.lastIndexOf(',');
        int lastDot = clean.lastIndexOf('.');
        if (lastComma >= 0 && lastDot >= 0) {
            clean = lastComma > lastDot ? clean.replace(".", "").replace(',', '.') : clean.replace(",", "");
        } else if (lastComma >= 0) {
            clean = clean.replace(',', '.');
        }
        try {
            return Math.max(0.0, Double.parseDouble(clean));
        } catch (NumberFormatException ignored) {
            return 0.0;
        }
    }

    private static String moneyNumber(double amount) {
        if (Math.abs(amount - Math.rint(amount)) < 0.005) {
            if (Math.abs(amount) < 0.005) return "0.00";
            return String.format(Locale.US, "%.0f", amount);
        }
        return String.format(Locale.US, "%.2f", amount);
    }

    private static void hideKeyboard(@NonNull View view) {
        InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    static class VH extends RecyclerView.ViewHolder {
        final FrameLayout iconBg;
        final ImageView icon;
        final ImageView btnDelete;
        final TextView tvNombre;
        final TextInputLayout tilMonto;
        final TextInputEditText etMonto;
        final View progressContainer;
        final LinearProgressIndicator progress;
        final TextView tvPercent;
        final TextView tvGastado;
        final TextView tvDisponible;
        TextWatcher watcher;
        int boundCategoryId = RecyclerView.NO_POSITION;
        int lastProgress = -1;

        VH(@NonNull View itemView) {
            super(itemView);
            iconBg = itemView.findViewById(R.id.categoryIconBg);
            icon = itemView.findViewById(R.id.imgCategoriaIcon);
            btnDelete = itemView.findViewById(R.id.btnDeleteCategoryBudget);
            tvNombre = itemView.findViewById(R.id.tvCategoriaNombre);
            tilMonto = itemView.findViewById(R.id.tilCategoriaMonto);
            etMonto = itemView.findViewById(R.id.etCategoriaMonto);
            progressContainer = itemView.findViewById(R.id.progressContainer);
            progress = itemView.findViewById(R.id.progresoCategoria);
            tvPercent = itemView.findViewById(R.id.tvPorcentajeCategoria);
            tvGastado = itemView.findViewById(R.id.tvGastado);
            tvDisponible = itemView.findViewById(R.id.tvDisponible);
        }
    }
}
