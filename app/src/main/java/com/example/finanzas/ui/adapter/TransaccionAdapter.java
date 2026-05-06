package com.example.finanzas.ui.adapter;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.finanzas.R;
import com.example.finanzas.data.api.SettingsService;
import com.example.finanzas.data.model.Transaccion;
import com.example.finanzas.util.CategoryVisuals;
import com.example.finanzas.util.Format;
import com.example.finanzas.util.LabelColorUtils;
import com.example.finanzas.util.TransactionLabelStore;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TransaccionAdapter extends ArrayAdapter<Transaccion> {
    private Map<Integer, TransactionLabelStore.Label> labels = new HashMap<>();

    public TransaccionAdapter(@NonNull Context ctx, @NonNull List<Transaccion> data) {
        super(ctx, 0, data);
    }

    public void setLabels(@Nullable Map<Integer, TransactionLabelStore.Label> labels) {
        this.labels = labels == null ? new HashMap<>() : new HashMap<>(labels);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View v = convertView;
        if (v == null) {
            v = LayoutInflater.from(getContext()).inflate(R.layout.item_transaccion, parent, false);
        }

        Transaccion t = getItem(position);
        if (t == null) return v;

        TextView tvTitulo = v.findViewById(R.id.tvTitulo);
        TextView tvSub = v.findViewById(R.id.tvSub);
        TextView tvMonto = v.findViewById(R.id.tvMonto);
        TextView tvLabel = v.findViewById(R.id.tvTransactionLabel);
        View labelColor = v.findViewById(R.id.viewTransactionLabelColor);
        ImageView ivTipo = v.findViewById(R.id.ivTipo);

        String cat = t.isTransfer() ? "Transferencia" : (t.getCategoriaNombre() == null ? "-" : t.getCategoriaNombre());
        String displayNote = t.getDisplayNote();
        String nota = (displayNote == null || displayNote.isEmpty()) ? "" : " - " + displayNote;
        tvTitulo.setText(cat + nota);

        String account = SettingsService.getFinancialAccountName(getContext(), t.getAccountType());
        if (t.isTransfer()) {
            String destination = SettingsService.getFinancialAccountName(getContext(), t.getTransferDestinationAccountType());
            tvSub.setText(Format.date(t.getFecha()) + " " + formatTime(t) + " - " + account + " -> " + destination);
        } else {
            tvSub.setText(Format.date(t.getFecha()) + " " + formatTime(t) + " - " + account);
        }

        double mostrado = t.isTransfer() ? t.getMonto() : (t.isEsIngreso() ? t.getMonto() : -t.getMonto());
        tvMonto.setText(Format.money(mostrado, t.getMoneda()));
        tvMonto.setSingleLine(true);
        tvMonto.setEllipsize(TextUtils.TruncateAt.END);
        tvMonto.setMaxWidth(dp(132));

        int typeColor = t.isTransfer()
                ? ContextCompat.getColor(getContext(), R.color.chartAccent)
                : CategoryVisuals.colorFor(getContext(), t.getCategoriaNombre(), t.isEsIngreso());
        tvMonto.setTextColor(ContextCompat.getColor(getContext(),
                t.isTransfer() ? R.color.chartAccent : (t.isEsIngreso() ? R.color.income : R.color.expense)));
        tvTitulo.setTextColor(ContextCompat.getColor(getContext(), R.color.md_theme_onSurface));
        tvSub.setTextColor(ContextCompat.getColor(getContext(), R.color.md_theme_onSurfaceVariant));

        TransactionLabelStore.Label label = labels.get(t.getId());
        applyRowBackground(v, label);

        if (label != null) {
            int labelInt = label.colorInt();
            int accent = LabelColorUtils.accentOnSurface(getContext(), labelInt);
            if (labelColor != null) {
                GradientDrawable bar = new GradientDrawable();
                bar.setColor(accent);
                bar.setCornerRadius(dp(3));
                labelColor.setBackground(bar);
                labelColor.setVisibility(View.VISIBLE);
            }
            if (ivTipo != null) {
                ivTipo.setImageResource(t.isTransfer() ? R.drawable.ic_transferencia : CategoryVisuals.iconFor(t.getCategoriaNombre(), t.isEsIngreso()));
                ivTipo.setColorFilter(accent);
                GradientDrawable iconBg = new GradientDrawable();
                iconBg.setShape(GradientDrawable.OVAL);
                iconBg.setColor(LabelColorUtils.iconBackground(getContext(), labelInt));
                ivTipo.setBackground(iconBg);
                ivTipo.setPadding(dp(5), dp(5), dp(5), dp(5));
            }
            if (tvLabel != null) {
                int chipBackground = LabelColorUtils.chipBackground(getContext(), labelInt, false);
                tvLabel.setText(label.name);
                tvLabel.setTextColor(LabelColorUtils.textOnTint(getContext(), labelInt, chipBackground));

                GradientDrawable chip = new GradientDrawable();
                chip.setColor(chipBackground);
                chip.setStroke(dp(1), LabelColorUtils.cardStroke(getContext(), labelInt));
                chip.setCornerRadius(dp(10));
                tvLabel.setBackground(chip);

                GradientDrawable marker = new GradientDrawable();
                marker.setColor(accent);
                marker.setCornerRadius(dp(2));
                marker.setSize(dp(12), dp(3));
                marker.setBounds(0, 0, dp(12), dp(3));
                tvLabel.setCompoundDrawables(marker, null, null, null);
                tvLabel.setCompoundDrawablePadding(dp(6));
                tvLabel.setVisibility(View.VISIBLE);
            }
        } else {
            if (labelColor != null) labelColor.setVisibility(View.GONE);
            if (ivTipo != null) {
                ivTipo.setImageResource(t.isTransfer() ? R.drawable.ic_transferencia : CategoryVisuals.iconFor(t.getCategoriaNombre(), t.isEsIngreso()));
                ivTipo.setColorFilter(typeColor);
                GradientDrawable iconBg = new GradientDrawable();
                iconBg.setShape(GradientDrawable.OVAL);
                iconBg.setColor(LabelColorUtils.iconBackground(getContext(), typeColor));
                ivTipo.setBackground(iconBg);
                ivTipo.setPadding(dp(5), dp(5), dp(5), dp(5));
            }
            if (tvLabel != null) {
                tvLabel.setVisibility(View.GONE);
                tvLabel.setBackground(null);
                tvLabel.setCompoundDrawables(null, null, null, null);
            }
        }

        return v;
    }

    private void applyRowBackground(@NonNull View row, @Nullable TransactionLabelStore.Label label) {
        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(dp(18));
        if (label == null) {
            background.setColor(ContextCompat.getColor(getContext(), R.color.md_theme_surface));
            background.setStroke(dp(1), ContextCompat.getColor(getContext(), R.color.md_theme_outlineVariant));
        } else {
            int labelColor = label.colorInt();
            background.setColor(LabelColorUtils.cardBackground(getContext(), labelColor));
            background.setStroke(dp(1), LabelColorUtils.cardStroke(getContext(), labelColor));
        }
        row.setBackground(background);
    }

    private String formatTime(@NonNull Transaccion tx) {
        if (tx.getFecha() == null) return "";
        return new SimpleDateFormat("HH:mm", Locale.US).format(tx.getFecha());
    }

    private int dp(int value) {
        return Math.round(value * getContext().getResources().getDisplayMetrics().density);
    }
}
