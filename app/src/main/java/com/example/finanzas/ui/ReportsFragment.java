package com.example.finanzas.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.finanzas.R;
import com.example.finanzas.data.model.CategoryChartSlice;
import com.example.finanzas.data.model.FinancialReport;
import com.example.finanzas.data.model.MonthlyTrendPoint;
import com.example.finanzas.data.model.Transaccion;
import com.example.finanzas.ui.viewmodel.ReportsViewModel;
import com.example.finanzas.util.Format;
import com.example.finanzas.util.PerfLogger;
import com.example.finanzas.util.UiFormUtils;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.util.List;

public class ReportsFragment extends Fragment {
    private SwipeRefreshLayout swipe;
    private ProgressBar progress;
    private TextView tvEmpty;
    private TextView tvPeriod;
    private TextView tvStatus;
    private TextView tvSummary;
    private TextView tvCategories;
    private TextView tvTrend;
    private TextView tvTransactions;
    private ReportsViewModel viewModel;
    private boolean manualRefresh;
    private long perfStartMs;
    private long loadStartMs;
    private boolean firstRenderLogged;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_reports, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        perfStartMs = PerfLogger.now();
        firstRenderLogged = false;
        swipe = view.findViewById(R.id.swipeReports);
        progress = view.findViewById(R.id.progressReports);
        tvEmpty = view.findViewById(R.id.tvReportsEmpty);
        tvPeriod = view.findViewById(R.id.tvReportPeriod);
        tvStatus = view.findViewById(R.id.tvReportStatus);
        tvSummary = view.findViewById(R.id.tvReportSummary);
        tvCategories = view.findViewById(R.id.tvReportCategories);
        tvTrend = view.findViewById(R.id.tvReportTrend);
        tvTransactions = view.findViewById(R.id.tvReportTransactions);
        MaterialButton btnExport = view.findViewById(R.id.btnExportReportPdf);

        viewModel = new ViewModelProvider(requireActivity()).get(ReportsViewModel.class);
        viewModel.clearCacheIfUserChanged();
        swipe.setOnRefreshListener(() -> {
            manualRefresh = true;
            loadStartMs = PerfLogger.now();
            PerfLogger.log("ReportsFragment", "loadStart force=true");
            viewModel.loadCurrentMonth(true);
        });
        btnExport.setOnClickListener(v -> viewModel.exportPdf());
        FinancialReport cachedReport = viewModel.getReport().getValue();
        if (cachedReport != null) {
            render(cachedReport);
        }
        observeViewModel();
        PerfLogger.logSince("ReportsFragment", "onViewCreated", perfStartMs);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadStartMs = PerfLogger.now();
        PerfLogger.log("ReportsFragment", "loadStart");
        viewModel.loadCurrentMonth();
    }

    private void observeViewModel() {
        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            boolean active = Boolean.TRUE.equals(loading);
            boolean hasReport = viewModel.getReport().getValue() != null;
            if (progress != null) progress.setVisibility(active && !hasReport ? View.VISIBLE : View.GONE);
            if (swipe != null) swipe.setRefreshing(active && manualRefresh);
            if (!active) {
                PerfLogger.logSince("ReportsFragment", "loadComplete", loadStartMs);
                manualRefresh = false;
            }
        });
        viewModel.getReport().observe(getViewLifecycleOwner(), report -> {
            if (report != null) render(report);
        });
        viewModel.getPdfPath().observe(getViewLifecycleOwner(), path -> {
            if (path != null && !path.trim().isEmpty()) {
                UiFormUtils.showMessage(requireView(), getString(R.string.reports_pdf_ready, path));
                sharePdf(path);
            }
        });
        viewModel.getError().observe(getViewLifecycleOwner(), message -> {
            if (message == null || message.trim().isEmpty()) {
                UiFormUtils.showMessage(requireView(), R.string.reports_pdf_error);
            } else {
                UiFormUtils.showMessage(requireView(), message);
            }
        });
    }

    private void render(@NonNull FinancialReport report) {
        if (!firstRenderLogged) {
            firstRenderLogged = true;
            PerfLogger.logSince("ReportsFragment", "firstRender", perfStartMs);
        }
        String currency = report.getCurrencyCode();
        tvEmpty.setVisibility(report.getHasData() ? View.GONE : View.VISIBLE);
        tvPeriod.setText(report.getMonthLabel());
        tvStatus.setText(getString(R.string.reports_status_format, report.getStatus(), currency));
        tvSummary.setText(getString(
                R.string.reports_summary_format,
                Format.money(report.getSummary().getIngresos(), currency),
                Format.money(report.getSummary().getGastos(), currency),
                Format.money(report.getSummary().getSaldo(), currency),
                Format.money(report.getSummary().getAhorroSugerido(), currency),
                report.getSummary().getScoreFinanciero(),
                Format.money(report.getSummary().getEfectivo(), currency),
                Format.money(report.getSummary().getTarjetaCuenta(), currency),
                Format.money(report.getSummary().getSaldoActualTotal(), currency)
        ));
        tvCategories.setText(buildCategories(report.getTopCategories(), currency));
        tvTrend.setText(buildTrend(report.getTrend(), currency));
        tvTransactions.setText(buildTransactions(report.getRecentTransactions(), currency));
    }

    private String buildCategories(@Nullable List<CategoryChartSlice> items, @NonNull String currency) {
        if (items == null || items.isEmpty()) return getString(R.string.reports_no_categories);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            CategoryChartSlice item = items.get(i);
            String name = item.getCategoriaNombre() == null || item.getCategoriaNombre().trim().isEmpty()
                    ? getString(R.string.home_uncategorized)
                    : item.getCategoriaNombre();
            out.append(i + 1)
                    .append(". ")
                    .append(name)
                    .append(": ")
                    .append(Format.money(item.getGastado(), currency));
            if (i < items.size() - 1) out.append("\n");
        }
        return out.toString();
    }

    private String buildTrend(@Nullable List<MonthlyTrendPoint> points, @NonNull String currency) {
        if (points == null || points.size() < 2) return getString(R.string.reports_no_trend);
        StringBuilder out = new StringBuilder();
        for (MonthlyTrendPoint point : points) {
            if (point == null) continue;
            out.append(point.getEtiqueta())
                    .append(": ")
                    .append(Format.money(point.getSaldo(), currency))
                    .append("\n");
        }
        return out.toString().trim();
    }

    private String buildTransactions(@Nullable List<Transaccion> items, @NonNull String currency) {
        if (items == null || items.isEmpty()) return getString(R.string.reports_no_transactions);
        StringBuilder out = new StringBuilder();
        int max = Math.min(6, items.size());
        for (int i = 0; i < max; i++) {
            Transaccion tx = items.get(i);
            String type = tx.isEsIngreso() ? getString(R.string.tipo_ingreso_on) : getString(R.string.tipo_ingreso_off);
            double signed = tx.isEsIngreso() ? tx.getMonto() : -tx.getMonto();
            out.append(Format.date(tx.getFecha()))
                    .append(" · ")
                    .append(type)
                    .append(" · ")
                    .append(tx.getCategoriaNombre() == null ? getString(R.string.home_uncategorized) : tx.getCategoriaNombre())
                    .append(" · ")
                    .append(Format.money(signed, currency));
            if (i < max - 1) out.append("\n");
        }
        return out.toString();
    }

    private void sharePdf(@NonNull String path) {
        File file = new File(path);
        if (!file.exists()) {
            UiFormUtils.showMessage(requireView(), R.string.reports_pdf_error);
            return;
        }
        Uri uri = FileProvider.getUriForFile(
                requireContext(),
                requireContext().getPackageName() + ".fileprovider",
                file
        );
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, getString(R.string.reports_share_title)));
    }
}
