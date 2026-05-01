package com.example.finanzas.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.finanzas.data.api.SettingsService
import com.example.finanzas.data.model.DashboardModulePref
import com.example.finanzas.data.model.HomeSummary
import com.example.finanzas.data.model.TravelPreference
import com.example.finanzas.di.AppGraph
import kotlinx.coroutines.async
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Calendar

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val graph = AppGraph(application)

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _summary = MutableLiveData<HomeSummary>()
    val summary: LiveData<HomeSummary> = _summary

    private val _error = MutableLiveData<Unit>()
    val error: LiveData<Unit> = _error

    private val _smartAlert = MutableLiveData<String?>()
    val smartAlert: LiveData<String?> = _smartAlert

    private val _insights = MutableLiveData<List<String>>(emptyList())
    val insights: LiveData<List<String>> = _insights

    private val _currencyCode = MutableLiveData(SettingsService.getCurrencyCode(application))
    val currencyCode: LiveData<String> = _currencyCode

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

    fun loadSummary(anio: Int, mes: Int) {
        _loading.value = true
        viewModelScope.launch {
            runCatching {
                _currencyCode.value = SettingsService.getCurrencyCode(getApplication())
                val summaryDeferred = async { graph.dashboardRepository.getSummary(anio, mes) }
                val previousCal = Calendar.getInstance().apply {
                    set(anio, mes - 1, 1)
                    add(Calendar.MONTH, -1)
                }
                val previousPreviousCal = (previousCal.clone() as Calendar).apply {
                    add(Calendar.MONTH, -1)
                }
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

                val summary = hydrateUiPreferences(summaryDeferred.await())
                val currentTx = currentTxDeferred.await()
                val previousTx = prevTxDeferred.await()
                val trend = trendDeferred.await()
                val previousSummary = previousSummaryDeferred.await()
                val previousPreviousTx = prevPrevTxDeferred.await()
                withContext(Dispatchers.Default) {
                    graph.financialDashboardEngine.enrichDashboard(summary, currentTx, previousTx, trend)
                    graph.financialDashboardEngine.applyScoreTrend(summary, previousSummary, previousTx, previousPreviousTx)
                    val smart = graph.smartSpendingAlertUseCase.execute(currentTx, previousTx)
                    if (summary.alertaPrincipal == "Sin alertas relevantes por ahora" && !smart?.message.isNullOrBlank()) {
                        summary.alertaPrincipal = smart?.message
                    }
                    Triple(
                        summary,
                        smart?.message,
                        graph.financialDashboardEngine.buildInsights(summary, currentTx, previousTx)
                    )
                }
            }
                .onSuccess {
                    _summary.value = it.first
                    _smartAlert.value = it.second
                    _insights.value = it.third
                }
                .onFailure { _error.value = Unit }
            _loading.value = false
        }
    }
}
