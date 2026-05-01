package com.example.finanzas.domain.usecase

import com.example.finanzas.data.model.CategoryChartSlice
import com.example.finanzas.data.model.HomeSummary
import com.example.finanzas.data.model.Transaccion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.Date

class FinancialAnalysisRulesTest {
    @Test
    fun oneExpenseWithIncomeKeepsProjectionPreliminaryAndNonAlarmist() {
        val summary = HomeSummary().apply {
            anio = 2026
            mes = 5
            ingresos = 1200.0
            gastos = 80.0
            saldo = 1120.0
            saldoActualTotal = 1120.0
            chartCategorias.add(slice("Alimentacion", 80.0))
        }
        val tx = listOf(
            tx(income = true, amount = 1200.0, day = 1),
            tx(income = false, amount = 80.0, day = 1)
        )

        val result = FinancialAnalysisRules.analyze(summary, tx, emptyList(), false, today(day = 1))

        assertEquals(ProjectionConfidence.LOW, result.confidence)
        assertTrue(result.projectedExpenses < 300.0)
        assertTrue(result.projectedEndBalance > 900.0)
        assertTrue(result.score >= 70)
        assertTrue(result.confidenceMessage.contains("preliminar", ignoreCase = true))
    }

    @Test
    fun initialBalanceWithOneExpenseDoesNotTreatMonthlyBalanceAsRealNegativeCash() {
        val summary = HomeSummary().apply {
            anio = 2026
            mes = 5
            ingresos = 0.0
            gastos = 50.0
            saldo = -50.0
            efectivo = 68.90
            tarjetaCuenta = 1150.30
            saldoActualTotal = 1219.20
            chartCategorias.add(slice("Alimentacion", 50.0))
        }
        val tx = listOf(tx(income = false, amount = 50.0, day = 1))

        val result = FinancialAnalysisRules.analyze(summary, tx, emptyList(), false, today(day = 1))

        assertEquals(ProjectionConfidence.LOW, result.confidence)
        assertTrue(result.projectedEndBalance > 1100.0)
        assertTrue(result.primaryInsight.contains("saldo actual es positivo", ignoreCase = true))
        assertTrue(result.score >= 60)
        assertTrue(result.savingMessage.contains("preliminar", ignoreCase = true))
    }

    private fun tx(income: Boolean, amount: Double, day: Int): Transaccion {
        return Transaccion(
            0,
            if (income) 1 else 2,
            if (income) "Sueldo" else "Alimentacion",
            income,
            amount,
            "PEN",
            Date(today(day).timeInMillis),
            "CARD",
            null
        )
    }

    private fun slice(name: String, amount: Double): CategoryChartSlice {
        return CategoryChartSlice().apply {
            categoriaNombre = name
            gastado = amount
        }
    }

    private fun today(day: Int): Calendar {
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.MAY)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
}
