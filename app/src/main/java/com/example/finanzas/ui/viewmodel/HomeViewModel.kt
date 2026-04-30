package com.example.finanzas.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.finanzas.data.api.SettingsService
import com.example.finanzas.data.model.CategoryChartSlice
import com.example.finanzas.data.model.DashboardModulePref
import com.example.finanzas.data.model.HomeSummary
import com.example.finanzas.data.model.MonthlyTrendPoint
import com.example.finanzas.data.model.Transaccion
import com.example.finanzas.data.model.TravelPreference
import com.example.finanzas.di.AppGraph
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
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

    private fun enrichDashboard(
        summary: HomeSummary,
        currentTx: List<Transaccion>,
        previousTx: List<Transaccion>,
        trend: List<MonthlyTrendPoint>
    ): HomeSummary {
        summary.chartCategorias.clear()
        summary.chartCategorias.addAll(buildCategoryExpenseChart(currentTx))
        summary.tendenciaMensual.clear()
        summary.tendenciaMensual.addAll(trend)
        summary.alertas.clear()
        summary.alertas.addAll(buildAlerts(summary, currentTx, previousTx))
        applyProductMetrics(summary, currentTx, previousTx)
        return summary
    }

    private fun applyProductMetrics(
        summary: HomeSummary,
        currentTx: List<Transaccion>,
        previousTx: List<Transaccion>
    ) {
        val currentExpenses = currentTx.filter { !it.isEsIngreso }.sumOf { it.monto }
        val previousExpenses = previousTx.filter { !it.isEsIngreso }.sumOf { it.monto }
        summary.gastosMesAnterior = previousExpenses
        summary.variacionGastosPorcentaje = if (previousExpenses > 0) {
            ((currentExpenses - previousExpenses) / previousExpenses) * 100.0
        } else {
            0.0
        }

        val top = summary.chartCategorias.maxByOrNull { it.gastado }
        summary.categoriaMayorGasto = top?.categoriaNombre
        summary.categoriaMayorGastoMonto = top?.gastado ?: 0.0

        summary.estadoFinanciero = when {
            summary.presupuestoMonto > 0 && summary.gastos > summary.presupuestoMonto -> "EXCEDIDO"
            summary.gastos > summary.ingresos && summary.ingresos > 0 -> "RIESGO"
            summary.presupuestoMonto > 0 && summary.presupuestoPorcentaje >= 80 -> "RIESGO"
            else -> "CONTROLADO"
        }
    }

    private fun buildCategoryExpenseChart(currentTx: List<Transaccion>): List<CategoryChartSlice> {
        return currentTx
            .asSequence()
            .filter { !it.isEsIngreso && it.monto > 0 }
            .groupBy { tx -> tx.categoriaNombre?.takeIf { it.isNotBlank() } ?: "Sin categoria" }
            .map { (name, items) ->
                CategoryChartSlice().apply {
                    categoriaNombre = name
                    gastado = items.sumOf { it.monto }
                    presupuesto = 0.0
                }
            }
            .filter { it.gastado > 0 }
            .sortedByDescending { it.gastado }
            .take(8)
            .toList()
    }

    private fun buildAlerts(
        summary: HomeSummary,
        currentTx: List<Transaccion>,
        previousTx: List<Transaccion>
    ): List<String> {
        val alerts = mutableListOf<String>()
        if (summary.presupuestoMonto > 0 && summary.gastos > summary.presupuestoMonto) {
            alerts.add("Tus gastos superaron el presupuesto mensual")
        }

        val currentExpenses = currentTx.filter { !it.isEsIngreso }.sumOf { it.monto }
        val previousExpenses = previousTx.filter { !it.isEsIngreso }.sumOf { it.monto }
        if (previousExpenses > 0) {
            val change = ((currentExpenses - previousExpenses) / previousExpenses) * 100.0
            if (change >= 15.0) {
                alerts.add("Tus gastos subieron ${change.toInt()}% frente al mes anterior")
            }
        }
        return alerts
    }

    private fun buildInsights(
        summary: HomeSummary,
        currentTx: List<Transaccion>,
        previousTx: List<Transaccion>
    ): List<String> {
        val insights = mutableListOf<String>()
        if (summary.gastos <= 0 && summary.ingresos <= 0) {
            insights.add("Aun no tienes datos este mes")
            return insights
        }

        if (summary.presupuestoMonto > 0) {
            insights.add(
                if (summary.gastos <= summary.presupuestoMonto) {
                    "Estas dentro de tu presupuesto"
                } else {
                    "Necesitas reducir gastos para volver al presupuesto"
                }
            )
        }

        val top = summary.chartCategorias.maxByOrNull { it.gastado }
        if (top != null && top.gastado > 0) {
            insights.add("Tu mayor gasto es ${top.categoriaNombre}")
            val previousByCategory = previousTx
                .filter { !it.isEsIngreso && it.categoriaNombre == top.categoriaNombre }
                .sumOf { it.monto }
            if (previousByCategory > 0) {
                val change = ((top.gastado - previousByCategory) / previousByCategory) * 100.0
                if (change >= 10.0) {
                    insights.add("Tu gasto en ${top.categoriaNombre} aumento ${change.toInt()}%")
                }
            }
        }

        if (summary.gastos > summary.ingresos) {
            insights.add("Tus gastos superan tus ingresos este mes")
        }
        if (summary.presupuestoMonto > 0 && summary.presupuestoPorcentaje >= 80 && !summary.presupuestoExcedido) {
            insights.add("Ya consumiste ${summary.presupuestoPorcentaje.toInt()}% del presupuesto mensual")
        }

        return insights
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
                val currentTxDeferred = async { graph.dashboardRepository.listTransactions(anio, mes) }
                val prevTxDeferred = async {
                    graph.dashboardRepository.listTransactions(
                        previousCal.get(Calendar.YEAR),
                        previousCal.get(Calendar.MONTH) + 1
                    )
                }
                val trendDeferred = async { graph.dashboardRepository.buildMonthlyTrend(anio, mes) }

                val summary = hydrateUiPreferences(summaryDeferred.await())
                val currentTx = currentTxDeferred.await()
                val previousTx = prevTxDeferred.await()
                enrichDashboard(summary, currentTx, previousTx, trendDeferred.await())
                val smart = graph.smartSpendingAlertUseCase.execute(currentTx, previousTx)
                Triple(summary, smart?.message, buildInsights(summary, currentTx, previousTx))
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
