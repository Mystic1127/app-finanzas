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
        assertTrue(result.primaryInsight.contains("saldo actual esta positivo", ignoreCase = true))
        assertTrue(result.score >= 60)
        assertTrue(result.savingMessage.contains("espera mas movimientos", ignoreCase = true))
    }

    @Test
    fun initialBalanceTransactionIsNotTreatedAsRecurringMonthlyIncome() {
        val summary = HomeSummary().apply {
            anio = 2026
            mes = 5
            ingresos = 60.0
            gastos = 15.0
            saldo = 45.0
            efectivo = 45.0
            saldoActualTotal = 45.0
            chartCategorias.add(slice("Alimentacion", 15.0))
        }
        val tx = listOf(
            initialBalanceTx(amount = 60.0, day = 1, accountType = "CASH"),
            tx(income = false, amount = 15.0, day = 2)
        )

        val result = FinancialAnalysisRules.analyze(summary, tx, emptyList(), false, today(day = 2))

        assertEquals(0.0, result.recurringIncome, 0.001)
        assertEquals(45.0, result.visibleBalance, 0.001)
        assertEquals(-15.0, result.operatingBalance, 0.001)
        assertTrue(result.primaryInsight.contains("saldo actual esta positivo", ignoreCase = true))
        assertTrue(result.alerts.none { it.contains("No hay ingresos registrados", ignoreCase = true) })
        assertTrue(result.infoNotes.any { it.contains("saldo inicial registrado", ignoreCase = true) })
        assertEquals(0.0, result.suggestedSaving, 0.001)
    }

    @Test
    fun initialBalancesAreVisibleIncomeButNotRecurringForRealCase() {
        val summary = HomeSummary().apply {
            anio = 2026
            mes = 5
            ingresos = 160.0
            gastos = 10.0
            saldo = 150.0
            initialCashBalance = 60.0
            initialCardBalance = 100.0
            efectivo = 50.0
            tarjetaCuenta = 100.0
            saldoActualTotal = 150.0
            chartCategorias.add(slice("Alimentacion", 10.0))
        }
        val tx = listOf(
            initialBalanceTx(amount = 100.0, day = 1, accountType = "CARD"),
            initialBalanceTx(amount = 60.0, day = 1, accountType = "CASH"),
            tx(income = false, amount = 10.0, day = 2, accountType = "CASH")
        )

        val result = FinancialAnalysisRules.analyze(summary, tx, emptyList(), false, today(day = 2))

        assertEquals(0.0, result.recurringIncome, 0.001)
        assertEquals(150.0, result.visibleBalance, 0.001)
        assertEquals(-10.0, result.operatingBalance, 0.001)
        assertTrue(result.primaryInsight.contains("saldo actual esta positivo", ignoreCase = true))
        assertTrue(result.primaryInsight.contains("preliminar", ignoreCase = true))
        assertTrue(result.primaryInsight.contains("balance del mes", ignoreCase = true).not())
        assertTrue(result.primaryInsight.length <= 120)
        assertTrue(result.alerts.none { it.contains("sin ingresos", ignoreCase = true) })
        assertTrue(result.infoNotes.any { it.contains("saldo inicial registrado", ignoreCase = true) })
        assertTrue(result.savingStatus.contains("Preliminar", ignoreCase = true))
        assertTrue(result.savingMessage.length <= 110)
    }

    @Test
    fun initialBalanceWithoutExpensesIsStableAndPreliminary() {
        val summary = HomeSummary().apply {
            anio = 2026
            mes = 5
            ingresos = 160.0
            gastos = 0.0
            saldo = 160.0
            initialCashBalance = 60.0
            initialCardBalance = 100.0
            efectivo = 60.0
            tarjetaCuenta = 100.0
            saldoActualTotal = 160.0
        }
        val tx = listOf(
            initialBalanceTx(amount = 100.0, day = 1, accountType = "CARD"),
            initialBalanceTx(amount = 60.0, day = 1, accountType = "CASH")
        )

        val result = FinancialAnalysisRules.analyze(summary, tx, emptyList(), false, today(day = 1))

        assertEquals(ProjectionConfidence.LOW, result.confidence)
        assertTrue(result.primaryInsight.contains("saldo actual esta estable", ignoreCase = true))
        assertTrue(result.alerts.isEmpty())
        assertTrue(result.infoNotes.any { it.contains("preliminar", ignoreCase = true) })
    }

    @Test
    fun expenseWithoutIncomeOrInitialBalanceWarnsStrongly() {
        val summary = HomeSummary().apply {
            anio = 2026
            mes = 5
            ingresos = 0.0
            gastos = 10.0
            saldo = -10.0
            saldoActualTotal = -10.0
            chartCategorias.add(slice("Alimentacion", 10.0))
        }
        val tx = listOf(tx(income = false, amount = 10.0, day = 2))

        val result = FinancialAnalysisRules.analyze(summary, tx, emptyList(), false, today(day = 2))

        assertTrue(result.alerts.any { it.contains("No hay ingresos ni saldo suficiente", ignoreCase = true) })
        assertEquals("Riesgo", result.scoreState)
        assertTrue(result.savingStatus.contains("riesgo", ignoreCase = true))
    }

    private fun tx(income: Boolean, amount: Double, day: Int): Transaccion {
        return tx(income, amount, day, "CARD")
    }

    private fun tx(income: Boolean, amount: Double, day: Int, accountType: String): Transaccion {
        return Transaccion(
            0,
            if (income) 1 else 2,
            if (income) "Sueldo" else "Alimentacion",
            income,
            amount,
            "PEN",
            Date(today(day).timeInMillis),
            accountType,
            null
        )
    }

    private fun initialBalanceTx(amount: Double, day: Int, accountType: String): Transaccion {
        return Transaccion(
            0,
            99,
            Transaccion.INITIAL_BALANCE_CATEGORY,
            true,
            amount,
            "PEN",
            Date(today(day).timeInMillis),
            accountType,
            if (accountType == "CASH") Transaccion.INITIAL_BALANCE_CASH_NOTE else Transaccion.INITIAL_BALANCE_CARD_NOTE
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
