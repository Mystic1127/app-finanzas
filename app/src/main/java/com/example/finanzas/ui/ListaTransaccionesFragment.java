package com.example.finanzas.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.finanzas.R;
import com.example.finanzas.data.api.TransService;
import com.example.finanzas.data.model.Transaccion;
import com.example.finanzas.ui.adapter.TransaccionAdapter;
import com.example.finanzas.util.Format;
import com.example.finanzas.util.Prefs;
import com.google.android.material.button.MaterialButton;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ListaTransaccionesFragment extends Fragment {

    private TransaccionAdapter adapter;
    private ProgressBar progress;
    private TextView tvPeriodo;
    private SwipeRefreshLayout swipeRefreshLayout;
    private int selectedYear = 0;
    private int selectedMonth = 0;
    private boolean announcePeriod = false;
    private boolean pendingPrefClear = false;
    private MaterialButton btnExportar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_lista_transacciones, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        ListView listView = v.findViewById(R.id.listView);
        tvPeriodo = v.findViewById(R.id.tvPeriodo);
        progress = v.findViewById(R.id.progressLista);
        swipeRefreshLayout = v.findViewById(R.id.swipeTransacciones);
        btnExportar = v.findViewById(R.id.btnExportar);
        adapter = new TransaccionAdapter(requireContext(), new ArrayList<>());
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            Transaccion t = adapter.getItem(position);
            if (t == null) return;

            Bundle args = new Bundle();
            args.putInt(NuevaTransaccionFragment.EXTRA_ID, t.getId());
            args.putInt(NuevaTransaccionFragment.EXTRA_CAT_ID, t.getCategoriaId());
            args.putString(NuevaTransaccionFragment.EXTRA_CAT_NOMBRE, t.getCategoriaNombre());
            args.putBoolean(NuevaTransaccionFragment.EXTRA_ES_INGRESO, t.isEsIngreso());
            args.putDouble(NuevaTransaccionFragment.EXTRA_MONTO, t.getMonto());
            args.putString(NuevaTransaccionFragment.EXTRA_NOTA,
                    t.getNota() == null ? "" : t.getNota());
            if (t.getFecha() != null) {
                args.putLong(NuevaTransaccionFragment.EXTRA_FECHA, t.getFecha().getTime());
            }

            Navigation.findNavController(view).navigate(R.id.nav_new, args);
        });

        listView.setOnItemLongClickListener((p, view, pos, id) -> {
            Transaccion t = adapter.getItem(pos);
            if (t == null) return true;

            new android.app.AlertDialog.Builder(requireContext())
                    .setTitle(R.string.btn_eliminar)
                    .setMessage(R.string.pres_confirm_delete)
                    .setPositiveButton(R.string.btn_eliminar, (d, w) -> eliminarRemotoYRefrescar(t.getId()))
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
            return true;
        });

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(this::cargarTransacciones);
        }

        if (btnExportar != null) {
            btnExportar.setOnClickListener(v1 -> exportarTransacciones());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        cargarTransacciones();
    }

    private void cargarTransacciones() {
        int[] periodo = resolvePeriodo();
        final int anio = periodo[0];
        final int mes = periodo[1];
        final boolean avisarPeriodo = announcePeriod;
        final boolean limpiarPref = pendingPrefClear;
        actualizarPeriodoLabel(anio, mes);
        showLoading(true);
        if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(true);

        TransService.list(requireContext(), anio, mes, new TransService.ListCb() {
            @Override
            public void onOk(List<Transaccion> items) {
                adapter.clear();
                adapter.addAll(items);
                adapter.notifyDataSetChanged();
                if (limpiarPref) {
                    Prefs.clearLastTransactionsPeriod(requireContext());
                }
                if (avisarPeriodo) {
                    Toast.makeText(requireContext(),
                            getString(R.string.transactions_loaded_period, Format.monthYear(anio, mes)),
                            Toast.LENGTH_LONG).show();
                }
                announcePeriod = false;
                pendingPrefClear = false;
                showLoading(false);
                stopRefreshing();
            }

            @Override
            public void onError() {
                showLoading(false);
                stopRefreshing();
                Toast.makeText(requireContext(), R.string.error_cargar_transacciones, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private int[] resolvePeriodo() {
        if (selectedYear > 0 && selectedMonth > 0) {
            return new int[]{selectedYear, selectedMonth};
        }

        int prefYear = Prefs.getLastTransactionsYear(requireContext());
        int prefMonth = Prefs.getLastTransactionsMonth(requireContext());
        if (prefYear > 0 && prefMonth >= 1 && prefMonth <= 12) {
            selectedYear = prefYear;
            selectedMonth = prefMonth;
            announcePeriod = true;
            pendingPrefClear = true;
            return new int[]{selectedYear, selectedMonth};
        }

        Calendar cal = Calendar.getInstance();
        selectedYear = cal.get(Calendar.YEAR);
        selectedMonth = cal.get(Calendar.MONTH) + 1;
        announcePeriod = false;
        pendingPrefClear = false;
        return new int[]{selectedYear, selectedMonth};
    }

    private void actualizarPeriodoLabel(int anio, int mes) {
        if (tvPeriodo == null) return;
        tvPeriodo.setVisibility(View.VISIBLE);
        tvPeriodo.setText(getString(R.string.transactions_period_label, Format.monthYear(anio, mes)));

        Calendar cal = Calendar.getInstance();
        int actualYear = cal.get(Calendar.YEAR);
        int actualMonth = cal.get(Calendar.MONTH) + 1;
        boolean esActual = anio == actualYear && mes == actualMonth;

        tvPeriodo.setEnabled(!esActual);
        if (esActual) {
            tvPeriodo.setOnClickListener(null);
        } else {
            tvPeriodo.setOnClickListener(v -> {
                selectedYear = 0;
                selectedMonth = 0;
                Prefs.clearLastTransactionsPeriod(requireContext());
                announcePeriod = false;
                pendingPrefClear = false;
                Toast.makeText(requireContext(), R.string.transactions_period_reset, Toast.LENGTH_SHORT).show();
                cargarTransacciones();
            });
        }
    }

    private void eliminarRemotoYRefrescar(int id) {
        showLoading(true);
        TransService.delete(requireContext(), id, new TransService.VoidCb() {
            @Override
            public void onOk() {
                Toast.makeText(requireContext(), R.string.trans_deleted, Toast.LENGTH_SHORT).show();
                cargarTransacciones();
            }

            @Override
            public void onError(@Nullable String message) {
                showLoading(false);
                if (message != null && !message.isEmpty()) {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(requireContext(), R.string.error_eliminar_transaccion, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void exportarTransacciones() {
        showLoading(true);
        TransService.exportToTxt(requireContext(), new TransService.FileCb() {
            @Override
            public void onOk(String path) {
                showLoading(false);
                Toast.makeText(requireContext(),
                        getString(R.string.transactions_export_success, path),
                        Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(@Nullable String message) {
                showLoading(false);
                String msg = message == null || message.isEmpty()
                        ? getString(R.string.transactions_export_error)
                        : message;
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showLoading(boolean show) {
        if (progress != null) progress.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void stopRefreshing() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
        }
    }
}
