package com.example.finanzas.data.repository

import android.content.Context
import com.example.finanzas.data.local.LocalRepository
import com.example.finanzas.data.model.HomeSummary
import com.example.finanzas.data.model.MonthlyTrendPoint
import com.example.finanzas.data.model.Transaccion
import com.example.finanzas.util.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale

class DashboardRepository(context: Context) {
    private val appContext = context.applicationContext
    private val local = LocalRepository.getInstance(appContext)
    private data class SummaryCacheKey(val userId: Long, val anio: Int, val mes: Int, val version: Long)
    @Volatile
    private var cachedSummaryKey: SummaryCacheKey? = null
    @Volatile
    private var cachedSummary: HomeSummary? = null
    @Volatile
    private var cachedFastSummaryKey: SummaryCacheKey? = null
    @Volatile
    private var cachedFastSummary: HomeSummary? = null

    suspend fun getSummary(anio: Int, mes: Int): HomeSummary = withContext(Dispatchers.IO) {
        val key = SummaryCacheKey(Prefs.getCurrentUserId(appContext), anio, mes, LocalRepository.getDataVersion())
        cachedSummary?.takeIf { cachedSummaryKey == key } ?: local.buildHomeSummary(anio, mes).also {
            cachedSummaryKey = key
            cachedSummary = it
        }
    }

    suspend fun getFastSummary(anio: Int, mes: Int): HomeSummary = withContext(Dispatchers.IO) {
        val key = SummaryCacheKey(Prefs.getCurrentUserId(appContext), anio, mes, LocalRepository.getDataVersion())
        cachedFastSummary?.takeIf { cachedFastSummaryKey == key } ?: local.buildHomeSummaryFast(anio, mes).also {
            cachedFastSummaryKey = key
            cachedFastSummary = it
        }
    }

    fun clearCache() {
        cachedSummaryKey = null
        cachedSummary = null
        cachedFastSummaryKey = null
        cachedFastSummary = null
    }

    suspend fun listTransactions(anio: Int, mes: Int): List<Transaccion> = withContext(Dispatchers.IO) {
        local.listTransaccionesEnMonedaBase(anio, mes)
    }

    suspend fun buildMonthlyTrend(anio: Int, mes: Int, months: Int = 6): List<MonthlyTrendPoint> =
        withContext(Dispatchers.IO) {
            val locale = Locale("es", "PE")
            val start = Calendar.getInstance().apply {
                set(Calendar.YEAR, anio)
                set(Calendar.MONTH, mes - 1)
                set(Calendar.DAY_OF_MONTH, 1)
                add(Calendar.MONTH, -(months - 1))
            }

            (0 until months).map { offset ->
                val cal = start.clone() as Calendar
                cal.add(Calendar.MONTH, offset)
                val year = cal.get(Calendar.YEAR)
                val month = cal.get(Calendar.MONTH) + 1
                val tx = local.listTransaccionesEnMonedaBase(year, month)
                val ingresos = tx.filter { it.isEsIngreso && !it.isTransfer }.sumOf { it.monto }
                val gastos = tx.filter { !it.isEsIngreso && !it.isTransfer }.sumOf { it.monto }
                MonthlyTrendPoint().apply {
                    this.anio = year
                    this.mes = month
                    etiqueta = cal.getDisplayName(Calendar.MONTH, Calendar.SHORT, locale)
                        ?.replace(".", "")
                        ?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
                        ?: "$month/$year"
                    this.ingresos = ingresos
                    this.gastos = gastos
                    saldo = ingresos - gastos
                }
            }
        }
}
