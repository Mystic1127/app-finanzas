package com.example.finanzas.domain.usecase

import android.content.Context
import com.example.finanzas.data.api.SettingsService
import com.example.finanzas.data.model.CategoryChartSlice
import com.example.finanzas.data.model.HomeSummary
import com.example.finanzas.data.model.MonthlyTrendPoint
import com.example.finanzas.data.model.SavingsGoal
import com.example.finanzas.data.model.Transaccion
import com.example.finanzas.util.Format
import com.example.finanzas.util.CurrencyConverter
import com.example.finanzas.util.ProFeatureManager
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.ceil

private data class ScoreResult(val value: Int, val explanation: String)

class FinancialDashboardEngine(
    private val context: Context,
    private val recommendationEngine: FinancialRecommendationEngine
) {
    fun enrichDashboard(
        summary: HomeSummary,
        currentTx: List<Transaccion>,
        previousTx: List<Transaccion>,
        trend: List<MonthlyTrendPoint>
    ): HomeSummary {
        summary.chartCategorias.clear()
        summary.chartCategorias.addAll(buildCategoryExpenseChart(currentTx))
        summary.tendenciaMensual.clear()
        summary.tendenciaMensual.addAll(trend)
        applyProductMetrics(summary, currentTx, previousTx)
        applyFinancialIntelligence(summary, currentTx, previousTx)
        applyProRecommendations(summary, currentTx, previousTx)
        return summary
    }

    fun applyScoreTrend(
        summary: HomeSummary,
        previousSummary: HomeSummary?,
        previousTx: List<Transaccion>,
        previousPreviousTx: List<Transaccion>
    ) {
        if (previousSummary == null || previousTx.isEmpty()) {
            summary.scoreTendencia = "Sin datos suficientes"
            return
        }

        val previousAnalysis = FinancialAnalysisRules.analyze(
            previousSummary,
            previousTx,
            previousPreviousTx,
            findUnusualExpense(previousTx, previousPreviousTx) != null
        )

        val diff = summary.scoreFinanciero - previousAnalysis.score
        summary.scoreTendencia = when {
            diff >= 3 -> "Mejoró"
            diff <= -3 -> "Bajó"
            else -> "Se mantuvo"
        }
    }

    fun buildInsights(
        summary: HomeSummary,
        currentTx: List<Transaccion>,
        previousTx: List<Transaccion>
    ): List<String> {
        val insights = mutableListOf<String>()
        if (summary.gastos <= 0 && summary.ingresos <= 0) {
            insights.add("Aún no tienes datos este mes")
            return insights
        }

        if (!summary.mensajeConfianzaProyeccion.isNullOrBlank()) {
            insights.add(summary.mensajeConfianzaProyeccion)
        }
        if (!summary.insightPrincipal.isNullOrBlank()) {
            insights.add(summary.insightPrincipal)
        }

        if (summary.presupuestoMonto > 0) {
            insights.add(
                if (summary.gastos <= summary.presupuestoMonto) {
                    "Estás dentro de tu presupuesto"
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
                    insights.add("Tu gasto en ${top.categoriaNombre} aumentó ${change.toInt()}%")
                }
            }
        }

        if (summary.saldo < 0.0 && summary.saldoActualTotal > 0.0) {
            insights.add("Tu balance del mes es negativo, pero tu saldo actual sigue positivo")
        } else if (summary.gastos > summary.ingresos) {
            insights.add("Tus gastos superan tus ingresos este mes")
        }
        if (summary.presupuestoMonto > 0 && summary.presupuestoPorcentaje >= 80 && !summary.presupuestoExcedido) {
            insights.add("Ya consumiste ${summary.presupuestoPorcentaje.toInt()}% del presupuesto mensual")
        }

        return insights.distinct().take(5)
    }

    private fun applyProRecommendations(
        summary: HomeSummary,
        currentTx: List<Transaccion>,
        previousTx: List<Transaccion>
    ) {
        summary.proUser = ProFeatureManager.isProUser(context)
        summary.recomendacionesInteligentes.clear()
        summary.recomendacionesInteligentes.addAll(
            recommendationEngine.generate(summary, currentTx, previousTx)
        )
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

    private fun applyFinancialIntelligence(
        summary: HomeSummary,
        currentTx: List<Transaccion>,
        previousTx: List<Transaccion>
    ) {
        val unusualExpense = findUnusualExpense(currentTx, previousTx)
        val currentExpenses = currentTx.filter { !it.isEsIngreso }.sumOf { it.monto }
        val previousExpenses = previousTx.filter { !it.isEsIngreso }.sumOf { it.monto }
        val daysInMonth = daysInMonth(summary.anio, summary.mes)
        val elapsedDays = elapsedDaysForMonth(summary.anio, summary.mes, daysInMonth)
        val dailyAverage = currentExpenses / elapsedDays.coerceAtLeast(1)
        val projectedExpenses = dailyAverage * daysInMonth
        val projectedEndBalance = summary.ingresos - projectedExpenses

        summary.gastoPromedioDiario = dailyAverage
        summary.gastoProyectado = projectedExpenses
        summary.diasRestantes = (daysInMonth - elapsedDays).coerceAtLeast(0)
        summary.proyeccionFinMes = projectedEndBalance
        summary.insightPrincipal = buildPrimaryInsight(summary, currentExpenses, previousExpenses)
        summary.alertaPrincipal = summary.alertas.firstOrNull() ?: "Sin alertas relevantes por ahora"
        applySmartSavings(summary, projectedExpenses, projectedEndBalance, elapsedDays)
        val scoreResult = calculateFinancialScore(summary, currentExpenses, previousExpenses, projectedEndBalance, unusualExpense != null)
        summary.scoreFinanciero = scoreResult.value
        summary.scoreEstado = when {
            summary.scoreFinanciero >= 70 -> "Bueno"
            summary.scoreFinanciero >= 40 -> "Riesgo"
            else -> "Crítico"
        }
        summary.scoreExplicacion = scoreResult.explanation

        val analysis = FinancialAnalysisRules.analyze(summary, currentTx, previousTx, unusualExpense != null)
        summary.gastoPromedioDiario = analysis.dailyAverage
        summary.gastoProyectado = analysis.projectedExpenses
        summary.diasRestantes = analysis.remainingDays
        summary.proyeccionFinMes = analysis.projectedEndBalance
        summary.cantidadGastosMes = analysis.expenseCount
        summary.diasConMovimientoMes = analysis.movementDays
        summary.proyeccionPreliminar = analysis.confidence == ProjectionConfidence.LOW
        summary.confianzaProyeccion = analysis.confidence.label
        summary.mensajeConfianzaProyeccion = analysis.confidenceMessage
        summary.insightPrincipal = analysis.primaryInsight
        summary.alertas.clear()
        summary.alertas.addAll(analysis.alerts)
        summary.alertaPrincipal = analysis.alerts.firstOrNull() ?: analysis.confidenceMessage
        summary.ahorroSugerido = analysis.suggestedSaving
        summary.ahorroSugeridoMensaje = analysis.savingMessage
        summary.recomendacionAhorroMeta = buildGoalSavingRecommendation(summary, analysis.suggestedSaving)
        summary.estadoAhorro = analysis.savingStatus
        summary.scoreFinanciero = analysis.score
        summary.scoreEstado = analysis.scoreState
        summary.scoreExplicacion = analysis.scoreExplanation
        summary.weeklyPlan = WeeklyPlanRules.build(summary, currentTx)
    }

    private fun applySmartSavings(
        summary: HomeSummary,
        projectedExpenses: Double,
        projectedEndBalance: Double,
        elapsedDays: Int
    ) {
        val availableMargin = listOf(summary.saldo, projectedEndBalance).minOrNull()?.coerceAtLeast(0.0) ?: 0.0
        val budgetPressure = when {
            summary.presupuestoMonto <= 0.0 -> 0.85
            summary.presupuestoPorcentaje >= 100.0 -> 0.0
            summary.presupuestoPorcentaje >= 90.0 -> 0.35
            summary.presupuestoPorcentaje >= 75.0 -> 0.6
            else -> 1.0
        }
        val categoryBudgetPressure = if (summary.presupuestosCategoria.any { it.limite > 0.0 && it.porcentaje >= 90.0 }) {
            0.75
        } else {
            1.0
        }
        val rawSuggested = availableMargin * budgetPressure * categoryBudgetPressure
        val goalNeed = goalNeedThisMonth(summary)
        val suggested = if (summary.ingresos <= 0.0 || projectedEndBalance <= 0.0) {
            0.0
        } else {
            maxOf(rawSuggested, goalNeed).coerceAtMost(availableMargin).coerceAtLeast(0.0)
        }

        summary.ahorroSugerido = suggested
        summary.ahorroSugeridoMensaje = when {
            summary.ingresos <= 0.0 -> "Registra ingresos para estimar tu ahorro"
            suggested <= 0.0 -> "No se recomienda ahorrar más por ahora"
            suggested < summary.ingresos * 0.05 -> "Tu margen de ahorro es bajo este mes"
            else -> "Podrías ahorrar este mes"
        }
        summary.recomendacionAhorroMeta = buildGoalSavingRecommendation(summary, suggested)
        summary.estadoAhorro = when {
            projectedEndBalance <= 0.0 || suggested <= 0.0 -> "En riesgo"
            suggested < summary.ingresos * 0.08 || projectedExpenses >= summary.ingresos * 0.9 -> "Ajustado"
            else -> "Saludable"
        }

        val remainingDays = daysInMonth(summary.anio, summary.mes) - elapsedDays
        summary.diasRestantes = remainingDays.coerceAtLeast(0)
    }

    private fun buildGoalSavingRecommendation(summary: HomeSummary, suggestedSaving: Double): String {
        val activeGoal = activeGoals(summary.metas).minWithOrNull(
            compareBy<SavingsGoal> { it.fechaObjetivo?.time ?: Long.MAX_VALUE }
                .thenBy { it.id }
        ) ?: return "Crea una meta para recibir recomendaciones de ahorro"

        val remaining = convertGoalAmount((activeGoal.montoObjetivo - activeGoal.montoActual).coerceAtLeast(0.0), activeGoal)
        if (remaining <= 0.0) return "Crea una meta para recibir recomendaciones de ahorro"

        val title = activeGoal.titulo?.takeIf { it.isNotBlank() } ?: "tu meta"
        val currency = SettingsService.getCurrencyCode(context)
        val daysToGoal = daysUntil(activeGoal.fechaObjetivo?.time)
        return if (daysToGoal != null) {
            val daily = remaining / daysToGoal.coerceAtLeast(1)
            val weekly = remaining / ceil(daysToGoal / 7.0).toInt().coerceAtLeast(1)
            "Para $title, ahorra ${Format.money(daily, currency)} por día o ${Format.money(weekly, currency)} por semana"
        } else if (suggestedSaving > 0.0) {
            val weeksLeft = ceil(summary.diasRestantes.coerceAtLeast(1) / 7.0).toInt().coerceAtLeast(1)
            "Para avanzar en $title, separa ${Format.money(suggestedSaving / weeksLeft, currency)} por semana este mes"
        } else {
            "Tu margen actual no alcanza para recomendar aportes a metas"
        }
    }

    private fun goalNeedThisMonth(summary: HomeSummary): Double {
        val activeGoal = activeGoals(summary.metas).minWithOrNull(
            compareBy<SavingsGoal> { it.fechaObjetivo?.time ?: Long.MAX_VALUE }
                .thenBy { it.id }
        ) ?: return 0.0

        val remaining = convertGoalAmount((activeGoal.montoObjetivo - activeGoal.montoActual).coerceAtLeast(0.0), activeGoal)
        if (remaining <= 0.0) return 0.0
        val daysToGoal = daysUntil(activeGoal.fechaObjetivo?.time)
        if (daysToGoal == null) return remaining.coerceAtMost(summary.proyeccionFinMes.coerceAtLeast(0.0))

        val daysThisMonth = summary.diasRestantes.coerceAtLeast(1)
        return ((remaining / daysToGoal.coerceAtLeast(1)) * daysThisMonth)
            .coerceAtMost(remaining)
            .coerceAtLeast(0.0)
    }

    private fun activeGoals(goals: List<SavingsGoal>): List<SavingsGoal> {
        return goals.filter { it.montoObjetivo > 0.0 && it.montoActual < it.montoObjetivo }
    }

    private fun convertGoalAmount(amount: Double, goal: SavingsGoal): Double {
        val base = SettingsService.getCurrencyCode(context)
        return CurrencyConverter.convert(amount, goal.moneda, base, base, SettingsService.getManualRate(context))
    }

    private fun buildCategoryExpenseChart(currentTx: List<Transaccion>): List<CategoryChartSlice> {
        return currentTx
            .asSequence()
            .filter { !it.isEsIngreso && it.monto > 0 }
            .groupBy { tx -> tx.categoriaNombre?.takeIf { it.isNotBlank() } ?: "Sin categoría" }
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
        } else if (summary.presupuestoMonto > 0 && summary.presupuestoPorcentaje >= 85.0) {
            alerts.add("Estás cerca de superar tu presupuesto mensual")
        }

        val categoryAtRisk = summary.presupuestosCategoria
            .filter { it.limite > 0 && it.porcentaje >= 90.0 }
            .maxByOrNull { it.porcentaje }
        if (categoryAtRisk != null) {
            val name = categoryAtRisk.categoriaNombre?.takeIf { it.isNotBlank() } ?: "una categoría"
            alerts.add("Estás cerca de superar el presupuesto de $name")
        }

        val currentExpenses = currentTx.filter { !it.isEsIngreso }.sumOf { it.monto }
        val previousExpenses = previousTx.filter { !it.isEsIngreso }.sumOf { it.monto }
        if (previousExpenses > 0) {
            val change = ((currentExpenses - previousExpenses) / previousExpenses) * 100.0
            if (change >= 15.0) {
                alerts.add("Tus gastos subieron ${change.toInt()}% frente al mes anterior")
            }
        }

        findUnusualExpense(currentTx, previousTx)?.let { tx ->
            val name = tx.categoriaNombre?.takeIf { it.isNotBlank() } ?: "Sin categoría"
            alerts.add("Detectamos un gasto inusualmente alto en $name")
        }
        return alerts
    }

    private fun buildPrimaryInsight(
        summary: HomeSummary,
        currentExpenses: Double,
        previousExpenses: Double
    ): String {
        if (summary.gastos <= 0 && summary.ingresos <= 0) {
            return "Aún no tienes datos suficientes este mes"
        }

        val comparison = if (previousExpenses > 0) {
            val change = ((currentExpenses - previousExpenses) / previousExpenses) * 100.0
            when {
                change >= 5.0 -> "Gastaste ${formatPercent(change)}% más que el mes pasado"
                change <= -5.0 -> "Gastaste ${formatPercent(kotlin.math.abs(change))}% menos que el mes pasado"
                else -> "Tus gastos se mantienen parecidos al mes pasado"
            }
        } else {
            null
        }

        val topCategory = summary.categoriaMayorGasto
            ?.takeIf { it.isNotBlank() && summary.categoriaMayorGastoMonto > 0 }
            ?.let { "Tu mayor gasto fue en $it" }

        return listOfNotNull(comparison, topCategory).take(2).joinToString(". ")
            .ifBlank { "Registra más movimientos para generar insights" }
    }

    private fun findUnusualExpense(
        currentTx: List<Transaccion>,
        previousTx: List<Transaccion>
    ): Transaccion? {
        val recentExpenses = (currentTx + previousTx)
            .asSequence()
            .filter { !it.isEsIngreso && it.monto > 0 }
            .sortedByDescending { it.fecha?.time ?: 0L }
            .take(12)
            .toList()
        if (recentExpenses.size < 4) return null

        val average = recentExpenses.map { it.monto }.average()
        if (average <= 0.0) return null
        return currentTx
            .filter { !it.isEsIngreso && it.monto >= average * 2.0 }
            .maxByOrNull { it.monto }
    }

    private fun calculateFinancialScore(
        summary: HomeSummary,
        currentExpenses: Double,
        previousExpenses: Double,
        projectedEndBalance: Double,
        hasUnusualExpense: Boolean
    ): ScoreResult {
        var score = 100
        val reasons = mutableListOf<String>()

        when {
            summary.ingresos <= 0.0 && currentExpenses > 0.0 -> {
                score -= 25
                reasons.add("no registras ingresos este mes")
            }
            summary.saldo < 0.0 -> {
                score -= 25
                reasons.add("tienes balance mensual negativo")
            }
            summary.saldo > 0.0 -> reasons.add("mantienes saldo positivo")
        }

        if (summary.presupuestoMonto <= 0.0) {
            score -= 6
        } else {
            score -= when {
                summary.presupuestoPorcentaje > 100.0 -> {
                    reasons.add("superaste tu presupuesto")
                    30
                }
                summary.presupuestoPorcentaje >= 90.0 -> {
                    reasons.add("alto uso del presupuesto")
                    22
                }
                summary.presupuestoPorcentaje >= 75.0 -> {
                    reasons.add("uso elevado del presupuesto")
                    10
                }
                else -> 0
            }
        }

        when {
            summary.ahorroSugerido > summary.ingresos * 0.12 -> reasons.add("ahorro proyectado saludable")
            summary.ahorroSugerido <= 0.0 && summary.ingresos > 0.0 -> {
                score -= 15
                reasons.add("no hay margen de ahorro proyectado")
            }
            summary.ahorroSugerido < summary.ingresos * 0.05 && summary.ingresos > 0.0 -> {
                score -= 8
                reasons.add("margen de ahorro bajo")
            }
        }

        if (previousExpenses > 0.0) {
            val change = ((currentExpenses - previousExpenses) / previousExpenses) * 100.0
            score -= when {
                change >= 30.0 -> {
                    reasons.add("gastos altos este mes")
                    18
                }
                change >= 15.0 -> {
                    reasons.add("los gastos subieron frente al mes anterior")
                    8
                }
                else -> 0
            }
        }

        if (hasUnusualExpense) {
            score -= 12
            reasons.add("detectamos un gasto inusual")
        }

        val activeGoals = activeGoals(summary.metas)
        if (activeGoals.isNotEmpty()) {
            val averageProgress = activeGoals.map { it.progreso }.average()
            when {
                averageProgress >= 75.0 -> reasons.add("tus metas avanzan bien")
                averageProgress < 25.0 -> {
                    score -= 8
                    reasons.add("bajo avance en metas")
                }
            }
        }

        if (projectedEndBalance < 0.0) {
            score -= 12
            reasons.add("saldo final estimado negativo")
        }

        val finalScore = score.coerceIn(0, 100)
        val explanation = when {
            finalScore >= 70 && reasons.any { it.contains("saldo positivo") || it.contains("ahorro") } ->
                "Tu score es bueno porque mantienes saldo positivo y ahorro proyectado"
            reasons.any { it.contains("presupuesto") } ->
                "Tu score bajó por ${reasons.first { it.contains("presupuesto") }}"
            reasons.any { it.contains("gastos") || it.contains("gasto") } ->
                "Tu score está en riesgo por gastos altos este mes"
            reasons.isNotEmpty() ->
                "Tu score refleja que ${reasons.take(2).joinToString(" y ")}"
            else -> "Sin datos suficientes para explicar el score"
        }
        return ScoreResult(finalScore, explanation)
    }

    private fun daysInMonth(anio: Int, mes: Int): Int {
        return Calendar.getInstance().apply {
            clear()
            set(anio, mes - 1, 1)
        }.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    private fun elapsedDaysForMonth(anio: Int, mes: Int, daysInMonth: Int): Int {
        val today = Calendar.getInstance()
        return if (today.get(Calendar.YEAR) == anio && today.get(Calendar.MONTH) + 1 == mes) {
            today.get(Calendar.DAY_OF_MONTH).coerceIn(1, daysInMonth)
        } else {
            daysInMonth
        }
    }

    private fun daysUntil(targetMillis: Long?): Int? {
        if (targetMillis == null || targetMillis <= 0L) return null
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val target = Calendar.getInstance().apply {
            timeInMillis = targetMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val diff = target.timeInMillis - today.timeInMillis
        if (diff <= 0L) return 1
        return TimeUnit.MILLISECONDS.toDays(diff).toInt().coerceAtLeast(1)
    }

    private fun formatPercent(value: Double): String {
        return String.format(Locale.US, "%.0f", value)
    }
}
