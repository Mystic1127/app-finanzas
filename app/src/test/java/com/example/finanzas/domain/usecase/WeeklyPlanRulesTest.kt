package com.example.finanzas.domain.usecase

import com.example.finanzas.data.model.CategoryBudgetSummary
import com.example.finanzas.data.model.HomeSummary
import com.example.finanzas.data.model.SavingsGoal
import com.example.finanzas.data.model.Transaccion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.Date

class WeeklyPlanRulesTest {
    @Test
    fun newUserWithoutBalanceGetsEmptyPreliminaryPlan() {
        val plan = WeeklyPlanRules.build(
            HomeSummary().apply {
                anio = 2026
                mes = 5
            },
            emptyList(),
            today(day = 1)
        )

        assertEquals("Ajustado", plan.estado)
        assertTrue(plan.sinDatos)
        assertEquals(0.0, plan.montoSemanal, 0.01)
        assertTrue(plan.mensaje.contains("Registra", ignoreCase = true))
    }

    @Test
    fun initialBalanceOnlyDoesNotRecommendSpendingEverything() {
        val plan = WeeklyPlanRules.build(
            HomeSummary().apply {
                anio = 2026
                mes = 5
                saldoActualTotal = 1200.0
                efectivo = 200.0
                tarjetaCuenta = 1000.0
            },
            emptyList(),
            today(day = 1)
        )

        assertTrue(plan.preliminar)
        assertTrue(plan.montoSemanal > 0.0)
        assertTrue(plan.montoSemanal < 250.0)
    }

    @Test
    fun fewMovementsCreateReasonablePreliminaryPlan() {
        val tx = listOf(
            tx(income = true, amount = 1200.0, category = "Sueldo", day = 1),
            tx(income = false, amount = 80.0, category = "Alimentación", day = 1)
        )
        val plan = WeeklyPlanRules.build(
            HomeSummary().apply {
                anio = 2026
                mes = 5
                ingresos = 1200.0
                gastos = 80.0
                saldo = 1120.0
                saldoActualTotal = 1120.0
                efectivo = 120.0
                tarjetaCuenta = 1000.0
                gastoPromedioDiario = 80.0
                cantidadGastosMes = 1
                diasConMovimientoMes = 1
                proyeccionPreliminar = true
            },
            tx,
            today(day = 1)
        )

        assertEquals("Cuidado", plan.estado)
        assertTrue(plan.montoSemanal > 0.0)
        assertTrue(plan.detalleConfianza.contains("preliminar", ignoreCase = true))
    }

    @Test
    fun categoryNearBudgetLimitReducesThatCategory() {
        val foodBudget = CategoryBudgetSummary().apply {
            categoriaNombre = "Alimentación"
            limite = 100.0
            gastado = 92.0
            disponible = 8.0
            porcentaje = 92.0
        }
        val plan = WeeklyPlanRules.build(
            HomeSummary().apply {
                anio = 2026
                mes = 5
                ingresos = 1200.0
                gastos = 250.0
                saldo = 950.0
                saldoActualTotal = 950.0
                efectivo = 100.0
                tarjetaCuenta = 850.0
                presupuestoMonto = 900.0
                presupuestoRestante = 650.0
                presupuestoPorcentaje = 27.0
                gastoPromedioDiario = 50.0
                cantidadGastosMes = 5
                diasConMovimientoMes = 4
                presupuestosCategoria.add(foodBudget)
            },
            listOf(
                tx(income = false, amount = 120.0, category = "Alimentación", day = 1),
                tx(income = false, amount = 60.0, category = "Transporte", day = 2),
                tx(income = false, amount = 70.0, category = "Servicios", day = 3)
            ),
            today(day = 10)
        )

        val food = plan.categorias.first { it.nombre == "Alimentación" }
        assertTrue(food.limitadoPorPresupuesto)
        assertTrue(food.monto <= 3.0)
        assertTrue(plan.advertencias.any { it.contains("Alimentación") })
    }

    @Test
    fun activeGoalReservesMargin() {
        val baseSummary = HomeSummary().apply {
            anio = 2026
            mes = 5
            ingresos = 1500.0
            gastos = 300.0
            saldo = 1200.0
            saldoActualTotal = 1200.0
            efectivo = 200.0
            tarjetaCuenta = 1000.0
            gastoPromedioDiario = 30.0
            cantidadGastosMes = 5
            diasConMovimientoMes = 4
        }
        val withoutGoal = WeeklyPlanRules.build(baseSummary, sampleExpenses(), today(day = 10))
        baseSummary.metas.add(SavingsGoal().apply {
            titulo = "Emergencia"
            montoObjetivo = 1000.0
            montoActual = 200.0
        })
        val withGoal = WeeklyPlanRules.build(baseSummary, sampleExpenses(), today(day = 10))

        assertTrue(withGoal.montoSemanal < withoutGoal.montoSemanal)
        assertTrue(withGoal.advertencias.any { it.contains("metas", ignoreCase = true) })
    }

    private fun sampleExpenses(): List<Transaccion> = listOf(
        tx(income = false, amount = 100.0, category = "Alimentación", day = 1),
        tx(income = false, amount = 80.0, category = "Transporte", day = 2),
        tx(income = false, amount = 120.0, category = "Servicios", day = 3)
    )

    private fun tx(income: Boolean, amount: Double, category: String, day: Int): Transaccion {
        return Transaccion(
            0,
            if (income) 1 else 2,
            category,
            income,
            amount,
            "PEN",
            Date(today(day).timeInMillis),
            "CARD",
            null
        )
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
