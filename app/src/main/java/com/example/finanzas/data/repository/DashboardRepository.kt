package com.example.finanzas.data.repository

import android.content.Context
import com.example.finanzas.data.local.LocalRepository
import com.example.finanzas.data.model.HomeSummary
import com.example.finanzas.data.model.MonthlyTrendPoint
import com.example.finanzas.data.model.Transaccion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale

class DashboardRepository(context: Context) {
    private val local = LocalRepository.getInstance(context.applicationContext)

    suspend fun getSummary(anio: Int, mes: Int): HomeSummary = withContext(Dispatchers.IO) {
        local.buildHomeSummary(anio, mes)
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
                val ingresos = tx.filter { it.isEsIngreso }.sumOf { it.monto }
                val gastos = tx.filter { !it.isEsIngreso }.sumOf { it.monto }
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
