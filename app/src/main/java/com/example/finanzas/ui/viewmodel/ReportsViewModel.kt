package com.example.finanzas.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.finanzas.data.api.SettingsService
import com.example.finanzas.data.model.FinancialReport
import com.example.finanzas.di.AppGraph
import com.example.finanzas.util.Format
import com.example.finanzas.util.MonthlyReportPdfExporter
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.util.Calendar

class ReportsViewModel(application: Application) : AndroidViewModel(application) {
    private val graph = AppGraph(application)
    private val pdfExporter = MonthlyReportPdfExporter(application.applicationContext)

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _report = MutableLiveData<FinancialReport>()
    val report: LiveData<FinancialReport> = _report

    private val _pdfPath = MutableLiveData<String>()
    val pdfPath: LiveData<String> = _pdfPath

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun loadCurrentMonth() {
        val cal = Calendar.getInstance()
        load(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
    }

    fun load(anio: Int, mes: Int) {
        _loading.value = true
        viewModelScope.launch {
            runCatching {
                val previousCal = Calendar.getInstance().apply {
                    set(anio, mes - 1, 1)
                    add(Calendar.MONTH, -1)
                }
                val previousPreviousCal = (previousCal.clone() as Calendar).apply {
                    add(Calendar.MONTH, -1)
                }

                val summaryDeferred = async { graph.dashboardRepository.getSummary(anio, mes) }
                val currentTxDeferred = async { graph.dashboardRepository.listTransactions(anio, mes) }
                val previousTxDeferred = async {
                    graph.dashboardRepository.listTransactions(
                        previousCal.get(Calendar.YEAR),
                        previousCal.get(Calendar.MONTH) + 1
                    )
                }
                val previousPreviousTxDeferred = async {
                    graph.dashboardRepository.listTransactions(
                        previousPreviousCal.get(Calendar.YEAR),
                        previousPreviousCal.get(Calendar.MONTH) + 1
                    )
                }
                val previousSummaryDeferred = async {
                    graph.dashboardRepository.getSummary(
                        previousCal.get(Calendar.YEAR),
                        previousCal.get(Calendar.MONTH) + 1
                    )
                }
                val trendDeferred = async { graph.dashboardRepository.buildMonthlyTrend(anio, mes) }

                val summary = summaryDeferred.await()
                val currentTx = currentTxDeferred.await()
                val previousTx = previousTxDeferred.await()
                val trend = trendDeferred.await()

                graph.financialDashboardEngine.enrichDashboard(summary, currentTx, previousTx, trend)
                graph.financialDashboardEngine.applyScoreTrend(
                    summary,
                    previousSummaryDeferred.await(),
                    previousTx,
                    previousPreviousTxDeferred.await()
                )

                val status = when {
                    summary.saldo < 0.0 || summary.proyeccionFinMes < 0.0 -> "Negativo"
                    summary.presupuestoMonto > 0.0 && summary.presupuestoPorcentaje >= 85.0 -> "Ajustado"
                    summary.ahorroSugerido <= 0.0 && summary.ingresos > 0.0 -> "Ajustado"
                    else -> "Positivo"
                }

                FinancialReport(
                    summary = summary,
                    currencyCode = SettingsService.getCurrencyCode(getApplication()),
                    topCategories = summary.chartCategorias.sortedByDescending { it.gastado }.take(3),
                    recentTransactions = currentTx.sortedByDescending { it.fecha?.time ?: 0L }.take(8),
                    trend = trend,
                    status = status,
                    monthLabel = Format.monthYear(anio, mes)
                )
            }
                .onSuccess { _report.value = it }
                .onFailure { _error.value = it.message }
            _loading.value = false
        }
    }

    fun exportPdf() {
        val current = _report.value
        if (current == null) {
            _error.value = "No hay reporte cargado para exportar"
            return
        }

        _loading.value = true
        viewModelScope.launch {
            runCatching { pdfExporter.export(current) }
                .onSuccess { _pdfPath.value = it.absolutePath }
                .onFailure { _error.value = it.message ?: "No se pudo generar el PDF" }
            _loading.value = false
        }
    }
}
