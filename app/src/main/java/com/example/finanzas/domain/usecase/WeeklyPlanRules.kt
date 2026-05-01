package com.example.finanzas.domain.usecase

import com.example.finanzas.data.model.CategoryBudgetSummary
import com.example.finanzas.data.model.HomeSummary
import com.example.finanzas.data.model.SavingsGoal
import com.example.finanzas.data.model.Transaccion
import com.example.finanzas.data.model.WeeklyPlanCategory
import com.example.finanzas.data.model.WeeklyPlanSummary
import java.text.Normalizer
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.min

object WeeklyPlanRules {
    private const val STATE_STABLE = "Estable"
    private const val STATE_CAUTION = "Cuidado"
    private const val STATE_TIGHT = "Ajustado"

    fun build(
        summary: HomeSummary,
        currentTx: List<Transaccion>,
        today: Calendar = Calendar.getInstance()
    ): WeeklyPlanSummary {
        val expenses = currentTx.filter { !it.isEsIngreso && it.monto > 0.0 }
        val expenseCount = summary.cantidadGastosMes.takeIf { it > 0 } ?: expenses.size
        val movementDays = summary.diasConMovimientoMes.takeIf { it > 0 } ?: movementDays(currentTx)
        val preliminary = summary.proyeccionPreliminar || expenseCount < 3 || movementDays < 3
        val sinDatos = currentTx.isEmpty() && summary.ingresos <= 0.0 && summary.gastos <= 0.0

        val remainingDays = remainingDays(summary, today).coerceAtLeast(1)
        val daysThisWeek = min(7, remainingDays).coerceAtLeast(1)
        val weeksRemaining = ceil(remainingDays / 7.0).toInt().coerceAtLeast(1)

        val currentBalance = summary.saldoActualTotal.coerceAtLeast(0.0)
        val dailyReserve = summary.gastoPromedioDiario.coerceAtLeast(0.0) * daysThisWeek
        val minimumReserve = maxOf(currentBalance * 0.18, dailyReserve).coerceAtMost(currentBalance * 0.45)
        val weeklySavingReserve = weeklySavingReserve(summary, weeksRemaining)
        var weeklyByBalance = ((currentBalance - minimumReserve).coerceAtLeast(0.0) / weeksRemaining) - weeklySavingReserve

        if (summary.saldo < 0.0) {
            weeklyByBalance *= if (abs(summary.saldo) > currentBalance * 0.25) 0.70 else 0.85
        }
        if (preliminary) {
            weeklyByBalance *= 0.85
        }

        val budgetWeeklyLimit = if (summary.presupuestoMonto > 0.0) {
            summary.presupuestoRestante.coerceAtLeast(0.0) / weeksRemaining
        } else {
            Double.POSITIVE_INFINITY
        }
        val categoryAtRisk = summary.presupuestosCategoria.any { it.limite > 0.0 && it.porcentaje >= 85.0 }
        var recommended = min(weeklyByBalance.coerceAtLeast(0.0), budgetWeeklyLimit)
        if (categoryAtRisk) recommended *= 0.90
        if (summary.saldoActualTotal <= 0.0 || sinDatos && currentBalance <= 0.0) recommended = 0.0

        val budgetExceeded = summary.presupuestoMonto > 0.0 && summary.presupuestoRestante < 0.0
        val strongNegativeBalance = summary.saldo < 0.0 && abs(summary.saldo) > currentBalance * 0.25
        val state = when {
            summary.saldoActualTotal <= 0.0 || budgetExceeded || recommended <= 0.01 ||
                (summary.proyeccionFinMes < 0.0 && !preliminary) || strongNegativeBalance -> STATE_TIGHT
            preliminary || categoryAtRisk || summary.presupuestoPorcentaje >= 85.0 || summary.saldo < 0.0 -> STATE_CAUTION
            else -> STATE_STABLE
        }

        return WeeklyPlanSummary().apply {
            montoSemanal = roundMoney(recommended)
            gastoDiario = roundMoney(recommended / daysThisWeek)
            saldoActual = summary.saldoActualTotal
            balanceMensual = summary.saldo
            estado = state
            preliminar = preliminary
            this.sinDatos = sinDatos
            detalleConfianza = if (preliminary) {
                "Tu plan es preliminar porque aún hay pocos movimientos."
            } else {
                "Plan basado en tus movimientos, presupuesto y saldo actual."
            }
            mensaje = buildMessage(state, montoSemanal, sinDatos, currentBalance, preliminary)
            categorias.addAll(buildCategoryPlan(montoSemanal, expenses, summary.presupuestosCategoria, weeksRemaining))
            advertencias.addAll(buildWarnings(summary, preliminary, sinDatos, montoSemanal))
        }
    }

    private fun buildMessage(
        state: String,
        recommended: Double,
        sinDatos: Boolean,
        saldoActual: Double,
        preliminary: Boolean
    ): String {
        if (sinDatos && saldoActual <= 0.0) {
            return "Registra algunos movimientos para mejorar tu plan semanal."
        }
        if (recommended <= 0.01 || state == STATE_TIGHT) {
            return "Esta semana conviene priorizar gastos esenciales."
        }
        if (preliminary) {
            return "Tu plan es preliminar; úsalo como una guía prudente."
        }
        return if (state == STATE_STABLE) {
            "Si mantienes este ritmo, llegarías bien a fin de mes."
        } else {
            "Tu margen es limitado. Prioriza gastos necesarios."
        }
    }

    private fun weeklySavingReserve(summary: HomeSummary, weeksRemaining: Int): Double {
        val smartSaving = summary.ahorroSugerido.coerceAtLeast(0.0) / weeksRemaining
        val goalReserve = if (activeGoals(summary.metas).isNotEmpty()) {
            summary.saldoActualTotal.coerceAtLeast(0.0) * 0.03 / weeksRemaining
        } else {
            0.0
        }
        return maxOf(smartSaving, goalReserve)
    }

    private fun buildWarnings(
        summary: HomeSummary,
        preliminary: Boolean,
        sinDatos: Boolean,
        weeklyAmount: Double
    ): List<String> {
        val warnings = mutableListOf<String>()
        if (sinDatos) warnings.add("Registra algunos movimientos para mejorar tu plan semanal.")
        if (preliminary) warnings.add("Tu plan es preliminar porque aún hay pocos movimientos.")
        if (summary.presupuestoMonto > 0.0 && summary.presupuestoRestante < 0.0) {
            warnings.add("Tu presupuesto mensual está superado; reduce gastos esta semana.")
        } else if (summary.presupuestoMonto > 0.0 && summary.presupuestoPorcentaje >= 85.0) {
            warnings.add("Tu presupuesto mensual está cerca del límite.")
        }
        summary.presupuestosCategoria
            .filter { it.limite > 0.0 && it.porcentaje >= 85.0 }
            .sortedByDescending { it.porcentaje }
            .take(2)
            .forEach {
                val name = it.categoriaNombre?.takeIf { value -> value.isNotBlank() } ?: "una categoría"
                warnings.add("$name está cerca del límite, reduce gastos en esa categoría.")
            }
        if (activeGoals(summary.metas).isNotEmpty()) {
            warnings.add("Se reservó un margen prudente para tus metas activas.")
        }
        if (summary.ahorroSugerido > 0.0) {
            warnings.add("El ahorro inteligente se considera como reserva antes de recomendar gasto.")
        }
        if (weeklyAmount > 0.0 && summary.efectivo >= 0.0 && summary.efectivo < weeklyAmount * 0.25 && summary.tarjetaCuenta > weeklyAmount) {
            warnings.add("Tu saldo en efectivo está bajo; usa Tarjeta/Cuenta si corresponde.")
        }
        if (weeklyAmount > 0.0 && summary.tarjetaCuenta >= 0.0 && summary.tarjetaCuenta < weeklyAmount * 0.25 && summary.efectivo > weeklyAmount) {
            warnings.add("Tu saldo en Tarjeta/Cuenta está bajo; usa Efectivo si corresponde.")
        }
        return warnings.distinct().take(5)
    }

    private fun buildCategoryPlan(
        weeklyAmount: Double,
        expenses: List<Transaccion>,
        budgets: List<CategoryBudgetSummary>,
        weeksRemaining: Int
    ): List<WeeklyPlanCategory> {
        if (weeklyAmount <= 0.01) return defaultCategoryWeights().map {
            WeeklyPlanCategory(it.first, 0.0, false)
        }

        val weights = when {
            expenses.mapNotNull { it.categoriaNombre?.takeIf(String::isNotBlank) }.distinct().size >= 2 ->
                expenseBasedWeights(expenses)
            budgets.any { !it.categoriaNombre.isNullOrBlank() && it.disponible > 0.0 } ->
                budgetBasedWeights(budgets)
            else -> defaultCategoryWeights()
        }

        val budgetByName = budgets.associateBy { normalizeName(it.categoriaNombre) }
        val planned = weights.map { (name, weight) ->
            val raw = weeklyAmount * weight
            val budget = budgetByName[normalizeName(name)]
            val cap = budget?.takeIf { it.limite > 0.0 }?.disponible?.coerceAtLeast(0.0)?.div(weeksRemaining)
            val limited = cap != null && raw > cap
            WeeklyPlanCategory(name, roundMoney(if (cap != null) min(raw, cap) else raw), limited)
        }.toMutableList()

        val assigned = planned.sumOf { it.monto }
        val freeIndex = planned.indexOfFirst { normalizeName(it.nombre).contains("libre") || normalizeName(it.nombre).contains("otro") }
        if (weeklyAmount > assigned && freeIndex >= 0) {
            planned[freeIndex].monto = roundMoney(planned[freeIndex].monto + (weeklyAmount - assigned))
        }
        return planned.filter { it.monto > 0.0 }.take(5)
    }

    private fun expenseBasedWeights(expenses: List<Transaccion>): List<Pair<String, Double>> {
        val totals = expenses
            .groupBy { it.categoriaNombre?.takeIf(String::isNotBlank) ?: "Otros/Libre" }
            .mapValues { entry -> entry.value.sumOf { it.monto } }
            .toList()
            .sortedByDescending { it.second }
            .take(4)
        val total = totals.sumOf { it.second }.coerceAtLeast(1.0)
        val weighted = totals.map { it.first to (it.second / total) * 0.88 }
        return normalizeWeights(weighted + ("Otros/Libre" to 0.12))
    }

    private fun budgetBasedWeights(budgets: List<CategoryBudgetSummary>): List<Pair<String, Double>> {
        val usable = budgets
            .filter { !it.categoriaNombre.isNullOrBlank() && it.disponible > 0.0 }
            .sortedByDescending { it.disponible }
            .take(4)
        val total = usable.sumOf { it.disponible }.coerceAtLeast(1.0)
        val weighted = usable.map { (it.categoriaNombre ?: "Otros/Libre") to (it.disponible / total) * 0.85 }
        return normalizeWeights(weighted + ("Otros/Libre" to 0.15))
    }

    private fun defaultCategoryWeights(): List<Pair<String, Double>> = listOf(
        "Alimentación" to 0.43,
        "Transporte" to 0.19,
        "Servicios" to 0.16,
        "Ocio/Entretenimiento" to 0.11,
        "Otros/Libre" to 0.11
    )

    private fun normalizeWeights(weights: List<Pair<String, Double>>): List<Pair<String, Double>> {
        val grouped = weights
            .groupBy { normalizeName(it.first) }
            .map { (_, values) -> values.first().first to values.sumOf { it.second } }
        val total = grouped.sumOf { it.second }.coerceAtLeast(1.0)
        return grouped.map { it.first to it.second / total }
    }

    private fun activeGoals(goals: List<SavingsGoal>): List<SavingsGoal> {
        return goals.filter { it.montoObjetivo > 0.0 && it.montoActual < it.montoObjetivo }
    }

    private fun remainingDays(summary: HomeSummary, today: Calendar): Int {
        val daysInMonth = today.cloneCalendar(summary.anio, summary.mes).getActualMaximum(Calendar.DAY_OF_MONTH)
        return if (today.get(Calendar.YEAR) == summary.anio && today.get(Calendar.MONTH) + 1 == summary.mes) {
            (daysInMonth - today.get(Calendar.DAY_OF_MONTH) + 1).coerceAtLeast(1)
        } else {
            daysInMonth.coerceAtLeast(1)
        }
    }

    private fun movementDays(transactions: List<Transaccion>): Int {
        return transactions.mapNotNull { tx ->
            tx.fecha?.let {
                Calendar.getInstance().apply { time = it }.let { cal ->
                    "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}"
                }
            }
        }.distinct().size
    }

    private fun Calendar.cloneCalendar(year: Int, month: Int): Calendar {
        return (clone() as Calendar).apply {
            clear()
            set(year, month - 1, 1)
        }
    }

    private fun normalizeName(value: String?): String {
        val clean = value?.trim()?.lowercase().orEmpty()
        return Normalizer.normalize(clean, Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
    }

    private fun roundMoney(value: Double): Double = kotlin.math.round(value * 100.0) / 100.0
}
