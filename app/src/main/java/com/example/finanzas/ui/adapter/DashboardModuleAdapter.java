package com.example.finanzas.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finanzas.R;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DashboardModuleAdapter extends RecyclerView.Adapter<DashboardModuleAdapter.VH> {

    public interface Listener { void onVisibilityChanged(); }

    private final List<ModuleItem> items = new ArrayList<>();
    private Listener listener;

    public void setListener(Listener listener) { this.listener = listener; }

    public void setItems(List<ModuleItem> modules) {
        items.clear();
        if (modules != null) {
            items.addAll(modules);
        }
        notifyDataSetChanged();
    }

    public List<ModuleItem> getItems() { return items; }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dashboard_module, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        ModuleItem item = items.get(position);
        holder.tvTitle.setText(item.getTitle());
        holder.swVisible.setOnCheckedChangeListener(null);
        holder.swVisible.setChecked(item.isVisible());
        holder.swVisible.setOnCheckedChangeListener((buttonView, isChecked) -> {
            item.setVisible(isChecked);
            if (listener != null) listener.onVisibilityChanged();
        });
        holder.handle.setOnTouchListener((v, event) -> {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void moveItem(int from, int to) {
        if (from == to) return;
        Collections.swap(items, from, to);
        notifyItemMoved(from, to);
    }

    static class VH extends RecyclerView.ViewHolder {
        final ImageView handle;
        final TextView tvTitle;
        final SwitchMaterial swVisible;
        VH(@NonNull View itemView) {
            super(itemView);
            handle = itemView.findViewById(R.id.ivHandle);
            tvTitle = itemView.findViewById(R.id.tvModuleTitle);
            swVisible = itemView.findViewById(R.id.swVisible);
        }
    }

    public static class ModuleItem {
        private String id;
        private String title;
        private boolean visible;

        public ModuleItem(String id, String title, boolean visible) {
            this.id = id;
            this.title = title;
            this.visible = visible;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public boolean isVisible() { return visible; }
        public void setVisible(boolean visible) { this.visible = visible; }
    }
}
