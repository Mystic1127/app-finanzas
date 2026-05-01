package com.example.finanzas.ui.adapter;

import android.content.Context;
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

import java.util.List;

public class TransaccionAdapter extends ArrayAdapter<Transaccion> {

    public TransaccionAdapter(@NonNull Context ctx, @NonNull List<Transaccion> data) {
        super(ctx, 0, data);
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
        ImageView ivTipo  = v.findViewById(R.id.ivTipo);

        String cat  = t.getCategoriaNombre() == null ? "—" : t.getCategoriaNombre();
        String nota = (t.getNota() == null || t.getNota().isEmpty()) ? "" : " · " + t.getNota();
        tvTitulo.setText(cat + nota);


        tvSub.setText(Format.date(t.getFecha()));

        double mostrado = t.isEsIngreso() ? t.getMonto() : -t.getMonto();
        tvMonto.setText(Format.money(mostrado, t.getMoneda()));

        int color = ContextCompat.getColor(getContext(), t.isEsIngreso() ? R.color.income : R.color.expense);
        tvMonto.setTextColor(color);

        if (ivTipo != null) {
            ivTipo.setImageResource(R.drawable.ic_wallet_24);
            ivTipo.setColorFilter(color);
        }

        return v;
    }
}
