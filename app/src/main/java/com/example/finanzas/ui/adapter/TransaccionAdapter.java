package com.example.finanzas.ui.adapter;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finanzas.R;
import com.example.finanzas.data.api.SettingsService;
import com.example.finanzas.data.model.Categoria;
import com.example.finanzas.data.model.Transaccion;
import com.example.finanzas.util.CategoryVisuals;
import com.example.finanzas.util.Format;
import com.example.finanzas.util.LabelColorUtils;
import com.example.finanzas.util.TransactionLabelStore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class TransaccionAdapter extends ListAdapter<TransaccionAdapter.TransactionRow, TransaccionAdapter.VH> {
    public interface Listener {
        void onClick(@NonNull Transaccion transaction);
        boolean onLongClick(@NonNull Transaccion transaction);
    }

    private final Context context;
    private final LayoutInflater inflater;
    private final Listener listener;
    private final float density;
    private final SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm", Locale.US);
    private final Map<String, String> accountNameCache = new HashMap<>();

    public TransaccionAdapter(@NonNull Context ctx, @NonNull Listener listener) {
        super(DIFF_CALLBACK);
        this.context = ctx;
        this.inflater = LayoutInflater.from(ctx);
        this.listener = listener;
        this.density = ctx.getResources().getDisplayMetrics().density;
        setHasStableIds(true);
    }

    public void submitTransactions(
            @NonNull List<Transaccion> transactions,
            @Nullable Map<Integer, TransactionLabelStore.Label> labels
    ) {
        submitTransactions(transactions, labels, null);
    }

    public void submitTransactions(
            @NonNull List<Transaccion> transactions,
            @Nullable Map<Integer, TransactionLabelStore.Label> labels,
            @Nullable Runnable commitCallback
    ) {
        accountNameCache.clear();
        Map<Integer, TransactionLabelStore.Label> safeLabels = labels == null ? new HashMap<>() : labels;
        List<TransactionRow> rows = new ArrayList<>(transactions.size());
        for (Transaccion transaction : transactions) {
            if (transaction != null) {
                rows.add(new TransactionRow(transaction, safeLabels.get(transaction.getId())));
            }
        }
        submitList(rows, commitCallback);
    }

    @Override
    public long getItemId(int position) {
        Transaccion transaction = getItem(position).transaction;
        return transaction == null ? RecyclerView.NO_ID : transaction.getId();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(inflater.inflate(R.layout.item_transaccion, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        TransactionRow row = getItem(position);
        holder.bind(row.transaction, row.label);
    }

    private void bindClickHandlers(@NonNull VH holder, @NonNull Transaccion transaction) {
        holder.itemView.setOnClickListener(v -> listener.onClick(transaction));
        holder.itemView.setOnLongClickListener(v -> listener.onLongClick(transaction));
    }

    private String accountNameFor(@Nullable String accountType) {
        String key = accountType == null ? "CARD" : accountType;
        String cached = accountNameCache.get(key);
        if (cached != null) return cached;
        String name = SettingsService.getFinancialAccountName(context, key);
        accountNameCache.put(key, name);
        return name;
    }

    private String formatTime(@NonNull Transaccion tx) {
        Date fecha = tx.getFecha();
        return fecha == null ? "" : timeFormatter.format(fecha);
    }

    private int dp(int value) {
        return Math.round(value * density);
    }

    public class VH extends RecyclerView.ViewHolder {
        private final TextView tvTitulo;
        private final TextView tvSub;
        private final TextView tvMonto;
        private final TextView tvLabel;
        private final View labelColor;
        private final ImageView ivTipo;

        VH(@NonNull View itemView) {
            super(itemView);
            tvTitulo = itemView.findViewById(R.id.tvTitulo);
            tvSub = itemView.findViewById(R.id.tvSub);
            tvMonto = itemView.findViewById(R.id.tvMonto);
            tvLabel = itemView.findViewById(R.id.tvTransactionLabel);
            labelColor = itemView.findViewById(R.id.viewTransactionLabelColor);
            ivTipo = itemView.findViewById(R.id.ivTipo);
        }

        void bind(@NonNull Transaccion t, @Nullable TransactionLabelStore.Label label) {
            bindClickHandlers(this, t);

            String cat = t.isTransfer() ? "Transferencia" : (t.getCategoriaNombre() == null ? "-" : t.getCategoriaNombre());
            String displayNote = t.getDisplayNote();
            String nota = (displayNote == null || displayNote.isEmpty()) ? "" : " - " + displayNote;
            tvTitulo.setText(cat + nota);

            String account = accountNameFor(t.getAccountType());
            if (t.isTransfer()) {
                String destination = accountNameFor(t.getTransferDestinationAccountType());
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
                    ? ContextCompat.getColor(context, R.color.chartAccent)
                    : CategoryVisuals.colorFor(context, categoryFor(t));
            tvMonto.setTextColor(ContextCompat.getColor(context,
                    t.isTransfer() ? R.color.chartAccent : (t.isEsIngreso() ? R.color.income : R.color.expense)));
            tvTitulo.setTextColor(ContextCompat.getColor(context, R.color.md_theme_onSurface));
            tvSub.setTextColor(ContextCompat.getColor(context, R.color.md_theme_onSurfaceVariant));

            applyRowBackground(itemView, label);
            if (label != null) {
                bindLabelState(t, label);
            } else {
                bindDefaultState(t, typeColor);
            }
        }

        private void bindLabelState(@NonNull Transaccion t, @NonNull TransactionLabelStore.Label label) {
            int labelInt = label.colorInt();
            int accent = LabelColorUtils.accentOnSurface(context, labelInt);
            if (labelColor != null) {
                GradientDrawable bar = new GradientDrawable();
                bar.setColor(accent);
                bar.setCornerRadius(dp(3));
                labelColor.setBackground(bar);
                labelColor.setVisibility(View.VISIBLE);
            }
            if (ivTipo != null) {
                ivTipo.setImageResource(t.isTransfer() ? R.drawable.ic_transferencia : CategoryVisuals.iconFor(context, categoryFor(t)));
                ivTipo.setColorFilter(accent);
                GradientDrawable iconBg = new GradientDrawable();
                iconBg.setShape(GradientDrawable.OVAL);
                iconBg.setColor(LabelColorUtils.iconBackground(context, labelInt));
                ivTipo.setBackground(iconBg);
                ivTipo.setPadding(dp(5), dp(5), dp(5), dp(5));
            }
            if (tvLabel != null) {
                int chipBackground = LabelColorUtils.chipBackground(context, labelInt, false);
                tvLabel.setText(label.name);
                tvLabel.setTextColor(LabelColorUtils.textOnTint(context, labelInt, chipBackground));

                GradientDrawable chip = new GradientDrawable();
                chip.setColor(chipBackground);
                chip.setStroke(dp(1), LabelColorUtils.cardStroke(context, labelInt));
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
        }

        private void bindDefaultState(@NonNull Transaccion t, int typeColor) {
            if (labelColor != null) labelColor.setVisibility(View.GONE);
            if (ivTipo != null) {
                ivTipo.setImageResource(t.isTransfer() ? R.drawable.ic_transferencia : CategoryVisuals.iconFor(context, categoryFor(t)));
                ivTipo.setColorFilter(typeColor);
                GradientDrawable iconBg = new GradientDrawable();
                iconBg.setShape(GradientDrawable.OVAL);
                iconBg.setColor(LabelColorUtils.iconBackground(context, typeColor));
                ivTipo.setBackground(iconBg);
                ivTipo.setPadding(dp(5), dp(5), dp(5), dp(5));
            }
            if (tvLabel != null) {
                tvLabel.setVisibility(View.GONE);
                tvLabel.setBackground(null);
                tvLabel.setCompoundDrawables(null, null, null, null);
            }
        }
    }

    private void applyRowBackground(@NonNull View row, @Nullable TransactionLabelStore.Label label) {
        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(dp(18));
        if (label == null) {
            background.setColor(ContextCompat.getColor(context, R.color.md_theme_surface));
            background.setStroke(dp(1), ContextCompat.getColor(context, R.color.md_theme_outlineVariant));
        } else {
            int labelColor = label.colorInt();
            background.setColor(LabelColorUtils.cardBackground(context, labelColor));
            background.setStroke(dp(1), LabelColorUtils.cardStroke(context, labelColor));
        }
        row.setBackground(background);
    }

    @NonNull
    private Categoria categoryFor(@NonNull Transaccion transaction) {
        return new Categoria(
                transaction.getCategoriaId(),
                transaction.getCategoriaNombre(),
                transaction.isEsIngreso()
        );
    }

    public static final class TransactionRow {
        final Transaccion transaction;
        @Nullable final TransactionLabelStore.Label label;

        TransactionRow(@NonNull Transaccion transaction, @Nullable TransactionLabelStore.Label label) {
            this.transaction = transaction;
            this.label = label;
        }
    }

    private static final DiffUtil.ItemCallback<TransactionRow> DIFF_CALLBACK = new DiffUtil.ItemCallback<TransactionRow>() {
        @Override
        public boolean areItemsTheSame(@NonNull TransactionRow oldItem, @NonNull TransactionRow newItem) {
            return oldItem.transaction.getId() == newItem.transaction.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull TransactionRow oldItem, @NonNull TransactionRow newItem) {
            Transaccion oldTx = oldItem.transaction;
            Transaccion newTx = newItem.transaction;
            return oldTx.getCategoriaId() == newTx.getCategoriaId()
                    && oldTx.isEsIngreso() == newTx.isEsIngreso()
                    && oldTx.isTransfer() == newTx.isTransfer()
                    && oldTx.isInitialBalance() == newTx.isInitialBalance()
                    && Double.compare(oldTx.getMonto(), newTx.getMonto()) == 0
                    && Objects.equals(oldTx.getCategoriaNombre(), newTx.getCategoriaNombre())
                    && Objects.equals(oldTx.getMoneda(), newTx.getMoneda())
                    && Objects.equals(timeMillis(oldTx.getFecha()), timeMillis(newTx.getFecha()))
                    && Objects.equals(oldTx.getAccountType(), newTx.getAccountType())
                    && Objects.equals(oldTx.getTransferDestinationAccountType(), newTx.getTransferDestinationAccountType())
                    && Objects.equals(oldTx.getNota(), newTx.getNota())
                    && Objects.equals(oldTx.getDisplayNote(), newTx.getDisplayNote())
                    && sameLabel(oldItem.label, newItem.label);
        }
    };

    @Nullable
    private static Long timeMillis(@Nullable Date date) {
        return date == null ? null : date.getTime();
    }

    private static boolean sameLabel(
            @Nullable TransactionLabelStore.Label oldLabel,
            @Nullable TransactionLabelStore.Label newLabel
    ) {
        if (oldLabel == null || newLabel == null) return oldLabel == newLabel;
        return Objects.equals(oldLabel.id, newLabel.id)
                && Objects.equals(oldLabel.name, newLabel.name)
                && Objects.equals(oldLabel.colorHex, newLabel.colorHex);
    }
}
