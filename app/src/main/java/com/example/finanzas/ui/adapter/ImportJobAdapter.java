package com.example.finanzas.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finanzas.R;
import com.example.finanzas.data.model.ImportJob;

import java.util.ArrayList;
import java.util.List;

public class ImportJobAdapter extends RecyclerView.Adapter<ImportJobAdapter.Holder> {

    public interface Listener {
        void onProcess(ImportJob job);
    }

    private final List<ImportJob> items = new ArrayList<>();
    private final Listener listener;

    public ImportJobAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setItems(List<ImportJob> nuevos) {
        items.clear();
        if (nuevos != null) items.addAll(nuevos);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_import_job, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        ImportJob job = items.get(position);
        holder.tvName.setText(job.getNombreArchivo());
        String estado = holder.itemView.getContext().getString(R.string.import_status_format, job.getEstado());
        holder.tvStatus.setText(estado);
        holder.tvCounts.setText(holder.itemView.getContext().getString(
                R.string.import_counts_format,
                job.getTotal(),
                job.getProcesados(),
                job.getPendientes(),
                job.getErrores()));
        boolean puedeProcesar = "pendiente".equalsIgnoreCase(job.getEstado())
                || "procesando".equalsIgnoreCase(job.getEstado());
        holder.btnProcesar.setVisibility(puedeProcesar ? View.VISIBLE : View.GONE);
        holder.btnProcesar.setOnClickListener(v -> {
            if (listener != null) listener.onProcess(job);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView tvName;
        final TextView tvStatus;
        final TextView tvCounts;
        final com.google.android.material.button.MaterialButton btnProcesar;

        Holder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvImportName);
            tvStatus = itemView.findViewById(R.id.tvImportStatus);
            tvCounts = itemView.findViewById(R.id.tvImportCounts);
            btnProcesar = itemView.findViewById(R.id.btnProcesarImport);
        }
    }
}
