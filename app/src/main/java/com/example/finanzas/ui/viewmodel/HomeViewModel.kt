package com.example.finanzas.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.finanzas.data.api.SettingsService
import com.example.finanzas.data.local.LocalRepository
import com.example.finanzas.data.model.AnalysisChartState
import com.example.finanzas.data.model.DashboardModulePref
import com.example.finanzas.data.model.HomeSummary
import com.example.finanzas.data.model.TravelPreference
import com.example.finanzas.di.AppGraph
import com.example.finanzas.util.PerfLogger
import com.example.finanzas.util.Prefs
import com.patrykandpatrick.vico.views.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.views.cartesian.data.columnSeries
import com.patrykandpatrick.vico.views.cartesian.data.lineSeries
import kotlinx.coroutines.async
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Calendar

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val graph = AppGraph(application)

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _summary = MutableLiveData<HomeSummary?>()
    val summary: LiveData<HomeSummary?> = _summary

    private val _error = MutableLiveData<Unit>()
    val error: LiveData<Unit> = _error

    private val _smartAlert = MutableLiveData<String?>()
    val smartAlert: LiveData<String?> = _smartAlert

    private val _insights = MutableLiveData<List<String>>(emptyList())
    val insights: LiveData<List<String>> = _insights

    private val _currencyCode = MutableLiveData(SettingsService.getCurrencyCode(application))
    val currencyCode: LiveData<String> = _currencyCode

    val incomeExpenseChartProducer = CartesianChartModelProducer()
    val expenseTrendChartProducer = CartesianChartModelProducer()
    val balanceChartProducer = CartesianChartModelProducer()
    val savingsChartProducer = CartesianChartModelProducer()

    private val _analysisCharts = MutableLiveData<AnalysisChartState?>()
    val analysisCharts: LiveData<AnalysisChartState?> = _analysisCharts

    private var loadedYear = 0
    private var loadedMonth = 0
    private var loadedVersion = -1L
    private var loadedUserId = -1L
    private var loadGeneration = 0L
    private var currentLoadJob: Job? = null

    private fun hydrateUiPreferences(summary: HomeSummary): HomeSummary {
        runCatching {
            val dash = JSONObject(SettingsService.getDashboardRaw(getApplication()))
            val modules = dash.optJSONArray("modules")
            if (modules != null) {
                summary.dashboardPreferencias.clear()
                for (i in 0 until modules.length()) {
                    val item = modules.optJSONObject(i) ?: continue
                    summary.dashboardPreferencias.add(
                        DashboardModulePref(
                            item.optString("id"),
                            item.optBoolean("visible", true)
                        )
                    )
                }
            }
        }

        runCatching {
            val travel = JSONObject(SettingsService.getTravelRaw(getApplication()))
            val enabled = travel.optBoolean("enabled", false)
            val base = travel.optString("base", "")
            val currency = travel.optString("currency", "")
            val rate = travel.optDouble("rate", 0.0)
            if (enabled || base.isNotBlank() || currency.isNotBlank() || rate > 0) {
                summary.travelPreference = TravelPreference().apply {
                    isEnabled = enabled
                    this.base = base
                    this.currency = currency
                    this.rate = rate
                }
            }
        }
        return summary
    }

    @JvmOverloads
    fun loadSummary(anio: Int, mes: Int, force: Boolean = false) {
        clearCacheIfUserChanged()
        val loadStart = PerfLogger.now()
        PerfLogger.log("HomeFragment", "loadStart year=$anio month=$mes force=$force")
        val version = LocalRepository.getDataVersion()
        val userId = Prefs.getCurrentUserId(getApplication())
        val current = _summary.value
        if (!force && current != null && loadedUserId == userId && loadedYear == anio && loadedMonth == mes && loadedVersion == version) {
            _currencyCode.value = SettingsService.getCurrencyCode(getApplication())
            PerfLogger.logSince("HomeFragment", "loadCacheHit", loadStart)
            return
        }
        currentLoadJob?.cancel()
        val generation = ++loadGeneration
        _loading.value = current == null || force
        currentLoadJob = viewModelScope.launch {
            try {
                _currencyCode.value = SettingsService.getCurrencyCode(getApplication())
                val summary = hydrateUiPreferences(graph.dashboardRepository.getFastSummary(anio, mes))
                if (!isCurrentLoad(generation)) return@launch
                if (isStaleVersion(version)) {
                    retryStaleLoad(generation, anio, mes, force, version, loadStart)
                    return@launch
                }
                publishSummary(summary, userId, anio, mes, version, clearDerived = true)
                PerfLogger.logSince("HomeFragment", "summaryReady", loadStart)

                val previousCal = Calendar.getInstance().apply {
                    set(anio, mes - 1, 1)
                    add(Calendar.MONTH, -1)
                }
                val previousPreviousCal = (previousCal.clone() as Calendar).apply {
                    add(Calendar.MONTH, -1)
                }
                val fullSummaryDeferred = async { graph.dashboardRepository.getSummary(anio, mes) }
                val currentTxDeferred = async { graph.dashboardRepository.listTransactions(anio, mes) }
                val prevTxDeferred = async {
                    graph.dashboardRepository.listTransactions(
                        previousCal.get(Calendar.YEAR),
                        previousCal.get(Calendar.MONTH) + 1
                    )
                }
                val prevPrevTxDeferred = async {
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
                val analysisChartsDeferred = async { graph.dashboardRepository.buildAnalysisChartState(anio, mes) }

                val fullSummary = hydrateUiPreferences(fullSummaryDeferred.await())
                if (!isCurrentLoad(generation)) return@launch
                if (isStaleVersion(version)) {
                    retryStaleLoad(generation, anio, mes, force, version, loadStart)
                    return@launch
                }

                val currentTx = currentTxDeferred.await()
                val previousTx = prevTxDeferred.await()
                val trend = trendDeferred.await()
                val previousSummary = previousSummaryDeferred.await()
                val previousPreviousTx = prevPrevTxDeferred.await()
                val analysisCharts = analysisChartsDeferred.await()
                val enriched = withContext(Dispatchers.Default) {
                    graph.financialDashboardEngine.enrichDashboard(fullSummary, currentTx, previousTx, trend)
                    graph.financialDashboardEngine.applyScoreTrend(fullSummary, previousSummary, previousTx, previousPreviousTx)
                    val smart = graph.smartSpendingAlertUseCase.execute(currentTx, previousTx)
                    if (fullSummary.alertaPrincipal == "Sin alertas relevantes por ahora" && !smart?.message.isNullOrBlank()) {
                        fullSummary.alertaPrincipal = smart?.message
                    }
                    Triple(
                        fullSummary,
                        smart?.message,
                        graph.financialDashboardEngine.buildInsights(fullSummary, currentTx, previousTx)
                    )
                }
                if (!isCurrentLoad(generation)) return@launch
                if (isStaleVersion(version)) {
                    retryStaleLoad(generation, anio, mes, force, version, loadStart)
                    return@launch
                }
                updateAnalysisChartModels(analysisCharts)
                _analysisCharts.value = analysisCharts
                publishSummary(enriched.first, userId, anio, mes, version, clearDerived = false)
                _smartAlert.value = enriched.second
                _insights.value = enriched.third
                PerfLogger.logSince("HomeFragment", "enrichmentReady", loadStart)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Throwable) {
                if (isCurrentLoad(generation)) _error.value = Unit
            } finally {
                if (isCurrentLoad(generation)) {
                    PerfLogger.logSince("HomeFragment", "loadComplete", loadStart)
                    _loading.value = false
                    currentLoadJob = null
                }
            }
        }
    }

    private fun publishSummary(
        summary: HomeSummary,
        userId: Long,
        anio: Int,
        mes: Int,
        version: Long,
        clearDerived: Boolean
    ) {
        loadedUserId = userId
        loadedYear = anio
        loadedMonth = mes
        loadedVersion = version
        if (clearDerived) {
            _smartAlert.value = null
            _insights.value = emptyList()
        }
        _summary.value = summary
    }

    private suspend fun updateAnalysisChartModels(state: AnalysisChartState) {
        val xValues = state.months.indices.map { it.toDouble() }
        incomeExpenseChartProducer.runTransaction {
            val latest = state.months.lastOrNull()
            if (latest != null && (latest.ingresos > 0.0 || latest.gastos > 0.0)) {
                columnSeries {
                    series(listOf(0.0), listOf(latest.ingresos))
                    series(listOf(0.0), listOf(latest.gastos))
                }
            }
        }
        expenseTrendChartProducer.runTransaction {
            if (state.hasExpenseTrend()) {
                lineSeries {
                    series(xValues, state.months.map { it.gastos })
                }
            }
        }
        balanceChartProducer.runTransaction {
            if (state.hasBalance()) {
                lineSeries {
                    series(xValues, state.months.map { it.balance })
                }
            }
        }
        savingsChartProducer.runTransaction {
            if (state.hasSavings()) {
                columnSeries {
                    series(xValues, state.months.map { it.ahorro })
                }
            }
        }
    }

    private fun isCurrentLoad(generation: Long): Boolean = generation == loadGeneration

    private fun isStaleVersion(version: Long): Boolean = LocalRepository.getDataVersion() != version

    private fun retryStaleLoad(generation: Long, anio: Int, mes: Int, force: Boolean, version: Long, loadStart: Long) {
        if (!isCurrentLoad(generation)) return
        PerfLogger.log("HomeFragment", "staleLoadDiscarded requestedVersion=$version currentVersion=${LocalRepository.getDataVersion()}")
        PerfLogger.logSince("HomeFragment", "staleDiscarded", loadStart)
        loadGeneration++
        viewModelScope.launch { loadSummary(anio, mes, force) }
    }

    fun hasFreshSummary(anio: Int, mes: Int): Boolean {
        return _summary.value != null &&
            loadedUserId == Prefs.getCurrentUserId(getApplication()) &&
            loadedYear == anio &&
            loadedMonth == mes &&
            loadedVersion == LocalRepository.getDataVersion()
    }

    fun clearCacheIfUserChanged() {
        val currentUserId = Prefs.getCurrentUserId(getApplication())
        if (loadedUserId > 0 && loadedUserId != currentUserId) {
            clearCache()
        }
    }

    fun clearCache() {
        loadGeneration++
        currentLoadJob?.cancel()
        currentLoadJob = null
        loadedUserId = -1L
        loadedYear = 0
        loadedMonth = 0
        loadedVersion = -1L
        graph.dashboardRepository.clearCache()
        _summary.value = null
        _analysisCharts.value = null
        _smartAlert.value = null
        _insights.value = emptyList()
        _loading.value = false
    }
}
