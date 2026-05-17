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
import androidx.navigation.fragment.NavHostFragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.finanzas.R;
import com.example.finanzas.data.model.CategoryChartSlice;
import com.example.finanzas.data.model.FinancialReport;
import com.example.finanzas.data.model.MonthlyTrendPoint;
import com.example.finanzas.data.model.Transaccion;
import com.example.finanzas.ui.view.SpendlyDecorBackgroundDrawable;
import com.example.finanzas.ui.viewmodel.ReportsViewModel;
import com.example.finanzas.util.Format;
import com.example.finanzas.util.PerfLogger;
import com.example.finanzas.util.UiFormUtils;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.util.Calendar;
import java.util.List;

public class ReportsFragment extends Fragment {
    private SwipeRefreshLayout swipe;
    private ProgressBar progress;
    private TextView tvEmpty;
    private TextView tvPeriod;
    private TextView tvStatus;
    private TextView tvIncome;
    private TextView tvExpense;
    private TextView tvBalance;
    private TextView tvCurrent;
    private TextView tvScore;
    private TextView tvProjection;
    private TextView tvCategories;
    private TextView tvTrend;
    private TextView tvTransactions;
    private View detailsContainer;
    private MaterialButton btnToggleDetails;
    private ReportsViewModel viewModel;
    private boolean manualRefresh;
    private long perfStartMs;
    private long loadStartMs;
    private boolean firstRenderLogged;
    private FinancialReport lastRenderedReport;
    private int selectedYear;
    private int selectedMonth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_reports, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.setBackground(new SpendlyDecorBackgroundDrawable(requireContext()));
        perfStartMs = PerfLogger.now();
        firstRenderLogged = false;
        swipe = view.findViewById(R.id.swipeReports);
        progress = view.findViewById(R.id.progressReports);
        tvEmpty = view.findViewById(R.id.tvReportsEmpty);
        tvPeriod = view.findViewById(R.id.tvReportPeriod);
        tvStatus = view.findViewById(R.id.tvReportStatus);
        tvIncome = view.findViewById(R.id.tvReportIncome);
        tvExpense = view.findViewById(R.id.tvReportExpense);
        tvBalance = view.findViewById(R.id.tvReportBalance);
        tvCurrent = view.findViewById(R.id.tvReportCurrent);
        tvScore = view.findViewById(R.id.tvReportScore);
        tvProjection = view.findViewById(R.id.tvReportProjection);
        tvCategories = view.findViewById(R.id.tvReportCategories);
        tvTrend = view.findViewById(R.id.tvReportTrend);
        tvTransactions = view.findViewById(R.id.tvReportTransactions);
        detailsContainer = view.findViewById(R.id.reportDetailsContainer);
        btnToggleDetails = view.findViewById(R.id.btnReportToggleDetails);
        MaterialButton btnExport = view.findViewById(R.id.btnExportReportPdf);
        MaterialButton btnPreviousMonth = view.findViewById(R.id.btnReportPreviousMonth);
        MaterialButton btnCurrentMonth = view.findViewById(R.id.btnReportCurrentMonth);
        MaterialButton btnNextMonth = view.findViewById(R.id.btnReportNextMonth);
        view.findViewById(R.id.btnReportsBack).setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());

        Calendar now = Calendar.getInstance();
        selectedYear = now.get(Calendar.YEAR);
        selectedMonth = now.get(Calendar.MONTH) + 1;

        lastRenderedReport = null;
        viewModel = new ViewModelProvider(requireActivity()).get(ReportsViewModel.class);
        viewModel.clearCacheIfUserChanged();
        swipe.setOnRefreshListener(() -> {
            manualRefresh = true;
            loadStartMs = PerfLogger.now();
            PerfLogger.log("ReportsFragment", "loadStart force=true");
            loadSelectedMonth(true);
        });
        btnExport.setOnClickListener(v -> viewModel.exportPdf());
        btnToggleDetails.setOnClickListener(v -> toggleDetails());
        btnPreviousMonth.setOnClickListener(v -> shiftSelectedMonth(-1));
        btnCurrentMonth.setOnClickListener(v -> resetToCurrentMonth());
        btnNextMonth.setOnClickListener(v -> shiftSelectedMonth(1));
        FinancialReport cachedReport = viewModel.getReport().getValue();
        if (cachedReport != null) {
            render(cachedReport);
        }
        observeViewModel();
        PerfLogger.logSince("ReportsFragment", "onViewCreated", perfStartMs);
    }

    @Override
    public void onDestroyView() {
        lastRenderedReport = null;
        super.onDestroyView();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadStartMs = PerfLogger.now();
        PerfLogger.log("ReportsFragment", "loadStart");
        loadSelectedMonth(false);
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
        if (report == lastRenderedReport) return;
        lastRenderedReport = report;
        if (!firstRenderLogged) {
            firstRenderLogged = true;
            PerfLogger.logSince("ReportsFragment", "firstRender", perfStartMs);
        }
        String currency = report.getCurrencyCode();
        tvEmpty.setVisibility(report.getHasData() ? View.GONE : View.VISIBLE);
        tvPeriod.setText(report.getMonthLabel());
        tvStatus.setText(getString(R.string.reports_status_chip, report.getStatus(), currency));
        tvIncome.setText(metricText(getString(R.string.home_ingresos), Format.money(report.getSummary().getIngresos(), currency)));
        tvExpense.setText(metricText(getString(R.string.home_gastos), Format.money(report.getSummary().getGastos(), currency)));
        tvBalance.setText(metricText(getString(R.string.home_balance), Format.money(report.getSummary().getSaldo(), currency)));
        tvCurrent.setText(metricText(getString(R.string.home_total_current), Format.money(report.getSummary().getSaldoActualTotal(), currency)));
        tvScore.setText(metricText(getString(R.string.home_financial_score_label), report.getSummary().getScoreFinanciero() + "/100"));
        tvProjection.setText(metricText(getString(R.string.home_predict_projected), Format.money(report.getSummary().getProyeccionFinMes(), currency)));
        tvCategories.setText(buildCategories(report.getTopCategories(), currency));
        tvTrend.setText(buildTrend(report.getTrend(), currency));
        tvTransactions.setText(buildTransactions(report.getRecentTransactions(), currency));
    }

    private String metricText(@NonNull String label, @NonNull String value) {
        return label + "\n" + value;
    }

    private void toggleDetails() {
        boolean show = detailsContainer.getVisibility() != View.VISIBLE;
        detailsContainer.setVisibility(show ? View.VISIBLE : View.GONE);
        btnToggleDetails.setText(show ? R.string.reports_view_less : R.string.reports_view_more);
    }

    private void loadSelectedMonth(boolean force) {
        viewModel.load(selectedYear, selectedMonth, force);
    }

    private void shiftSelectedMonth(int delta) {
        Calendar cal = Calendar.getInstance();
        cal.set(selectedYear, selectedMonth - 1, 1);
        cal.add(Calendar.MONTH, delta);
        selectedYear = cal.get(Calendar.YEAR);
        selectedMonth = cal.get(Calendar.MONTH) + 1;
        lastRenderedReport = null;
        loadStartMs = PerfLogger.now();
        loadSelectedMonth(false);
    }

    private void resetToCurrentMonth() {
        Calendar now = Calendar.getInstance();
        selectedYear = now.get(Calendar.YEAR);
        selectedMonth = now.get(Calendar.MONTH) + 1;
        lastRenderedReport = null;
        loadStartMs = PerfLogger.now();
        loadSelectedMonth(false);
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
        StringBuilder out = new StringBuilder("Balance mensual (Saldo inicial aparece como ingreso especial)\n");
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
        for (int i = 0; i < items.size(); i++) {
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
            if (i < items.size() - 1) out.append("\n");
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
