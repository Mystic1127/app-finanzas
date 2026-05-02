package com.example.finanzas.ui.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
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
import com.example.finanzas.data.model.Transaccion;
import com.example.finanzas.util.Format;
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
        TextView tvSub    = v.findViewById(R.id.tvSub);
        TextView tvMonto  = v.findViewById(R.id.tvMonto);
        TextView tvLabel  = v.findViewById(R.id.tvTransactionLabel);
        View labelColor   = v.findViewById(R.id.viewTransactionLabelColor);
        ImageView ivTipo  = v.findViewById(R.id.ivTipo);

        String cat  = t.getCategoriaNombre() == null ? "—" : t.getCategoriaNombre();
        String nota = (t.getNota() == null || t.getNota().isEmpty()) ? "" : " · " + t.getNota();
        tvTitulo.setText(cat + nota);


        String account = t.isCash()
                ? getContext().getString(R.string.transaction_account_cash)
                : getContext().getString(R.string.transaction_account_card);
        tvSub.setText(Format.date(t.getFecha()) + " " + formatTime(t) + " · " + account);

        double mostrado = t.isEsIngreso() ? t.getMonto() : -t.getMonto();
        tvMonto.setText(Format.money(mostrado, t.getMoneda()));

        int color = ContextCompat.getColor(getContext(), t.isEsIngreso() ? R.color.income : R.color.expense);
        tvMonto.setTextColor(color);

        if (ivTipo != null) {
            ivTipo.setImageResource(R.drawable.ic_wallet_24);
            ivTipo.setColorFilter(color);
        }

        TransactionLabelStore.Label label = labels.get(t.getId());
        if (label != null) {
            int labelInt = label.colorInt();
            if (labelColor != null) {
                GradientDrawable bar = new GradientDrawable();
                bar.setColor(labelInt);
                bar.setCornerRadius(dp(3));
                labelColor.setBackground(bar);
                labelColor.setVisibility(View.VISIBLE);
            }
            if (tvLabel != null) {
                tvLabel.setText(label.name);
                tvLabel.setTextColor(labelReadableText(labelInt));
                GradientDrawable chip = new GradientDrawable();
                chip.setColor(withAlpha(labelInt, 44));
                chip.setStroke(1, withAlpha(labelInt, 120));
                chip.setCornerRadius(dp(10));
                tvLabel.setBackground(chip);
                tvLabel.setVisibility(View.VISIBLE);
            }
        } else {
            if (labelColor != null) labelColor.setVisibility(View.GONE);
            if (tvLabel != null) tvLabel.setVisibility(View.GONE);
        }

        return v;
    }

    private String formatTime(@NonNull Transaccion tx) {
        if (tx.getFecha() == null) return "";
        return new SimpleDateFormat("HH:mm", Locale.US).format(tx.getFecha());
    }

    private int dp(int value) {
        return Math.round(value * getContext().getResources().getDisplayMetrics().density);
    }

    private int withAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    private int labelReadableText(int color) {
        double luminance = (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255.0;
        return luminance > 0.55 ? Color.rgb(22, 33, 27) : Color.rgb(245, 250, 247);
    }
}
