package com.example.finanzas.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.finanzas.data.api.SettingsService
import com.example.finanzas.data.local.LocalRepository
import com.example.finanzas.data.model.FinancialReport
import com.example.finanzas.di.AppGraph
import com.example.finanzas.util.Format
import com.example.finanzas.util.MonthlyReportPdfExporter
import com.example.finanzas.util.PerfLogger
import com.example.finanzas.util.Prefs
import kotlinx.coroutines.async
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class ReportsViewModel(application: Application) : AndroidViewModel(application) {
    private val graph = AppGraph(application)
    private val pdfExporter = MonthlyReportPdfExporter(application.applicationContext)

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _report = MutableLiveData<FinancialReport?>()
    val report: LiveData<FinancialReport?> = _report

    private val _pdfPath = MutableLiveData<String?>()
    val pdfPath: LiveData<String?> = _pdfPath

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private var loadedYear = 0
    private var loadedMonth = 0
    private var loadedVersion = -1L
    private var loadedUserId = -1L

    @JvmOverloads
    fun loadCurrentMonth(force: Boolean = false) {
        val cal = Calendar.getInstance()
        load(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, force)
    }

    @JvmOverloads
    fun load(anio: Int, mes: Int, force: Boolean = false) {
        clearCacheIfUserChanged()
        val loadStart = PerfLogger.now()
        PerfLogger.log("ReportsFragment", "loadStart year=$anio month=$mes force=$force")
        val version = LocalRepository.getDataVersion()
        val userId = Prefs.getCurrentUserId(getApplication())
        if (!force && loadedVersion >= 0 && loadedUserId == userId && loadedYear == anio && loadedMonth == mes && loadedVersion == version) {
            PerfLogger.logSince("ReportsFragment", "loadCacheHit", loadStart)
            return
        }
        _loading.value = loadedVersion < 0 || force
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

                val previousSummary = previousSummaryDeferred.await()
                val previousPreviousTx = previousPreviousTxDeferred.await()
                withContext(Dispatchers.Default) {
                    graph.financialDashboardEngine.enrichDashboard(summary, currentTx, previousTx, trend)
                    graph.financialDashboardEngine.applyScoreTrend(
                        summary,
                        previousSummary,
                        previousTx,
                        previousPreviousTx
                    )
                }

                val status = when {
                    summary.saldoActualTotal < 0.0 -> "Saldo actual negativo"
                    summary.proyeccionFinMes < 0.0 && !summary.isProyeccionPreliminar -> "Proyección ajustada"
                    summary.saldo < 0.0 && summary.saldoActualTotal > 0.0 -> "Gasto visible mayor que ingresos"
                    summary.presupuestoMonto > 0.0 && summary.presupuestoPorcentaje >= 85.0 -> "Presupuesto ajustado"
                    summary.ahorroSugerido <= 0.0 && summary.ingresos > 0.0 -> "Ahorro ajustado"
                    else -> "Saludable"
                }

                FinancialReport(
                    summary = summary,
                    currencyCode = SettingsService.getCurrencyCode(getApplication()),
                    topCategories = summary.chartCategorias.sortedByDescending { it.gastado }.take(8),
                    recentTransactions = currentTx.sortedByDescending { it.fecha?.time ?: 0L },
                    trend = trend,
                    status = status,
                    monthLabel = Format.monthYear(anio, mes)
                )
            }
                .onSuccess {
                    loadedUserId = userId
                    loadedYear = anio
                    loadedMonth = mes
                    loadedVersion = version
                    _report.value = it
                }
                .onFailure { _error.value = it.message }
            PerfLogger.logSince("ReportsFragment", "loadComplete", loadStart)
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

    fun clearCacheIfUserChanged() {
        val currentUserId = Prefs.getCurrentUserId(getApplication())
        if (loadedUserId > 0 && loadedUserId != currentUserId) {
            clearCache()
        }
    }

    fun clearCache() {
        loadedUserId = -1L
        loadedYear = 0
        loadedMonth = 0
        loadedVersion = -1L
        graph.dashboardRepository.clearCache()
        _report.value = null
        _pdfPath.value = null
        _loading.value = false
    }
}
