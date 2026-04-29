package com.example.finanzas.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.finanzas.data.api.DashboardService
import com.example.finanzas.data.model.HomeSummary
import com.example.finanzas.data.model.TransaccionFiltro
import com.example.finanzas.data.model.DashboardModulePref
import com.example.finanzas.data.model.TravelPreference
import com.example.finanzas.data.api.SettingsService
import org.json.JSONObject
import com.example.finanzas.di.AppGraph
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
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


    private fun hydrateUiPreferences(summary: HomeSummary): HomeSummary {
        runCatching {
            val dash = JSONObject(SettingsService.getDashboardRaw(getApplication()))
            val modules = dash.optJSONArray("modules")
            if (modules != null) {
                summary.dashboardPreferencias.clear()
                for (i in 0 until modules.length()) {
                    val item = modules.optJSONObject(i) ?: continue
                    summary.dashboardPreferencias.add(DashboardModulePref(item.optString("id"), item.optBoolean("visible", true)))
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
                summary.travelPreference = TravelPreference(enabled, base, currency, rate)
            }
        }
        return summary
    }

    fun loadSummary(anio: Int, mes: Int) {
        _loading.value = true
        viewModelScope.launch {
            runCatching {
                val summaryDeferred = async { DashboardService.getSummary(getApplication(), anio, mes) }
                val previousCal = Calendar.getInstance().apply { set(anio, mes - 1, 1); add(Calendar.MONTH, -1) }
                val currentTxDeferred = async { graph.transactionRepository.list(anio, mes, TransaccionFiltro()) }
                val prevTxDeferred = async { graph.transactionRepository.list(previousCal.get(Calendar.YEAR), previousCal.get(Calendar.MONTH) + 1, TransaccionFiltro()) }

                val summary = hydrateUiPreferences(summaryDeferred.await())
                val smart = graph.smartSpendingAlertUseCase.execute(currentTxDeferred.await(), prevTxDeferred.await())
                Pair(summary, smart?.message)
            }
                .onSuccess {
                    _summary.value = it.first
                    _smartAlert.value = it.second
                }
                .onFailure { _error.value = Unit }
            _loading.value = false
        }
    }
}
