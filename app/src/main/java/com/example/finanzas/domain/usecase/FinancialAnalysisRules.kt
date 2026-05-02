package com.example.finanzas.domain.usecase

import com.example.finanzas.data.model.HomeSummary
import com.example.finanzas.data.model.Transaccion
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

enum class ProjectionConfidence(val label: String) {
    LOW("Baja"),
    MEDIUM("Media"),
    HIGH("Alta")
}

data class FinancialAnalysisResult(
    val projectedExpenses: Double,
    val projectedEndBalance: Double,
    val dailyAverage: Double,
    val remainingDays: Int,
    val confidence: ProjectionConfidence,
    val confidenceMessage: String,
    val expenseCount: Int,
    val movementDays: Int,
    val primaryInsight: String,
    val alerts: List<String>,
    val suggestedSaving: Double,
    val savingMessage: String,
    val savingStatus: String,
    val score: Int,
    val scoreState: String,
    val scoreExplanation: String,
    val recurringIncome: Double,
    val visibleBalance: Double,
    val operatingBalance: Double,
    val infoNotes: List<String>
)

object FinancialAnalysisRules {
    private data class AnalysisMoney(
        val visibleIncome: Double,
        val recurringIncome: Double,
        val initialBalanceIncome: Double,
        val visibleBalance: Double,
        val operatingBalance: Double,
        val hasInitialBalance: Boolean
    )

    fun analyze(
        summary: HomeSummary,
        currentTx: List<Transaccion>,
        previousTx: List<Transaccion>,
        hasUnusualExpense: Boolean,
        today: Calendar = Calendar.getInstance()
    ): FinancialAnalysisResult {
        val expenses = currentTx.filter { !it.isEsIngreso && it.monto > 0.0 }
        val money = analysisMoney(summary, currentTx)
        val expenseCount = expenses.size
        val movementDays = currentTx.mapNotNull { dayKey(it.fecha?.time) }.distinct().size
        val expenseDays = expenses.mapNotNull { dayKey(it.fecha?.time) }.distinct().size.coerceAtLeast(1)
        val daysInMonth = daysInMonth(summary.anio, summary.mes)
        val elapsedDays = elapsedDaysForMonth(summary.anio, summary.mes, daysInMonth, today)
        val remainingDays = (daysInMonth - elapsedDays).coerceAtLeast(0)
        val confidence = confidence(expenseCount, movementDays)
        val confidenceMessage = when (confidence) {
            ProjectionConfidence.LOW -> "Proyeccion preliminar: aun hay pocos datos para estimar con precision."
            ProjectionConfidence.MEDIUM -> "Proyeccion con confianza media; puede cambiar con nuevos movimientos."
            ProjectionConfidence.HIGH -> "Proyeccion con confianza alta segun los movimientos registrados."
        }

        val currentExpenses = expenses.sumOf { it.monto }
        val previousExpenses = previousTx.filter { !it.isEsIngreso }.sumOf { it.monto }
        val observedDailyAverage = currentExpenses / expenseDays
        val projectedExpenses = when (confidence) {
            ProjectionConfidence.LOW ->
                currentExpenses + observedDailyAverage * minOf(remainingDays, expenseDays) * 0.5
            ProjectionConfidence.MEDIUM ->
                currentExpenses + observedDailyAverage * minOf(remainingDays, expenseDays * 3)
            ProjectionConfidence.HIGH ->
                (currentExpenses / elapsedDays.coerceAtLeast(1)) * daysInMonth
        }.coerceAtLeast(currentExpenses)
        val projectedAdditionalExpense = (projectedExpenses - currentExpenses).coerceAtLeast(0.0)
        val projectedEndBalance = summary.saldoActualTotal - projectedAdditionalExpense

        val alerts = buildAlerts(summary, currentTx, previousTx, hasUnusualExpense, confidence, projectedEndBalance, money)
        val infoNotes = buildInfoNotes(summary, confidence, money)
        val suggestedSaving = calculateSuggestedSaving(summary, projectedEndBalance, confidence, money.recurringIncome)
        val savingMessage = savingMessage(summary, suggestedSaving, confidence, money)
        val savingStatus = savingStatus(summary, suggestedSaving, projectedEndBalance, confidence, money)
        val score = calculateScore(summary, currentExpenses, previousExpenses, projectedEndBalance, confidence, hasUnusualExpense, money)
        val scoreState = when {
            score >= 75 -> "Bueno"
            score >= 70 && summary.saldoActualTotal > 0.0 -> "Estable"
            score >= 55 -> "Atento"
            else -> "Riesgo"
        }

        return FinancialAnalysisResult(
            projectedExpenses = projectedExpenses,
            projectedEndBalance = projectedEndBalance,
            dailyAverage = observedDailyAverage,
            remainingDays = remainingDays,
            confidence = confidence,
            confidenceMessage = confidenceMessage,
            expenseCount = expenseCount,
            movementDays = movementDays,
            primaryInsight = buildPrimaryInsight(summary, currentExpenses, previousExpenses, confidence, money),
            alerts = alerts,
            suggestedSaving = suggestedSaving,
            savingMessage = savingMessage,
            savingStatus = savingStatus,
            score = score,
            scoreState = scoreState,
            scoreExplanation = buildScoreExplanation(summary, projectedEndBalance, confidence, scoreState, money),
            recurringIncome = money.recurringIncome,
            visibleBalance = money.visibleBalance,
            operatingBalance = money.operatingBalance,
            infoNotes = infoNotes
        )
    }

    private fun confidence(expenseCount: Int, movementDays: Int): ProjectionConfidence = when {
        expenseCount < 3 || movementDays < 3 -> ProjectionConfidence.LOW
        expenseCount < 8 || movementDays < 7 -> ProjectionConfidence.MEDIUM
        else -> ProjectionConfidence.HIGH
    }

    private fun analysisMoney(summary: HomeSummary, currentTx: List<Transaccion>): AnalysisMoney {
        val initialBalanceIncome = currentTx
            .filter { it.isInitialBalance && it.monto > 0.0 }
            .sumOf { it.monto }
        val recurringIncome = (summary.ingresos - initialBalanceIncome).coerceAtLeast(0.0)
        val visibleBalance = summary.ingresos - summary.gastos
        val operatingBalance = recurringIncome - summary.gastos
        val hasInitialBalance = initialBalanceIncome > 0.0 || summary.saldoInicialTotal > 0.0
        return AnalysisMoney(
            visibleIncome = summary.ingresos,
            recurringIncome = recurringIncome,
            initialBalanceIncome = initialBalanceIncome,
            visibleBalance = visibleBalance,
            operatingBalance = operatingBalance,
            hasInitialBalance = hasInitialBalance
        )
    }

    private fun buildAlerts(
        summary: HomeSummary,
        currentTx: List<Transaccion>,
        previousTx: List<Transaccion>,
        hasUnusualExpense: Boolean,
        confidence: ProjectionConfidence,
        projectedEndBalance: Double,
        money: AnalysisMoney
    ): List<String> {
        val alerts = mutableListOf<String>()
        if (summary.saldoActualTotal < 0.0) {
            alerts.add("Tu saldo actual real esta en negativo.")
        } else if (projectedEndBalance < 0.0 && confidence != ProjectionConfidence.LOW) {
            alerts.add("Si mantienes este ritmo de gasto, tu saldo podria ajustarse hacia fin de mes.")
        }

        if (summary.presupuestoMonto > 0 && summary.gastos > summary.presupuestoMonto) {
            alerts.add("Tus gastos superaron el presupuesto mensual.")
        } else if (summary.presupuestoMonto > 0 && summary.presupuestoPorcentaje >= 85.0) {
            alerts.add("Estas cerca de superar tu presupuesto mensual.")
        }

        if (hasUnusualExpense && confidence != ProjectionConfidence.LOW) {
            alerts.add("Detectamos un gasto inusualmente alto este mes.")
        }

        dominantCategory(summary)?.let { (name, percent) ->
            if (percent >= 70.0 && summary.cantidadGastosMes >= 3) {
                alerts.add("Tu gasto esta muy concentrado en $name (${percent.roundToInt()}%).")
            }
        }

        if (money.recurringIncome <= 0.0 && summary.gastos > 0.0 && summary.saldoActualTotal <= 0.0) {
            alerts.add("No hay ingresos ni saldo suficiente para cubrir tus gastos.")
        }

        val currentExpenses = currentTx.filter { !it.isEsIngreso }.sumOf { it.monto }
        val previousExpenses = previousTx.filter { !it.isEsIngreso }.sumOf { it.monto }
        if (previousExpenses > 0.0 && confidence != ProjectionConfidence.LOW) {
            val change = ((currentExpenses - previousExpenses) / previousExpenses) * 100.0
            if (change >= 20.0) alerts.add("Tus gastos subieron ${change.roundToInt()}% frente al mes anterior.")
        }

        return alerts.distinct().take(4)
    }

    private fun buildInfoNotes(
        summary: HomeSummary,
        confidence: ProjectionConfidence,
        money: AnalysisMoney
    ): List<String> {
        val notes = mutableListOf<String>()
        val hasAnyData = summary.gastos > 0.0 || summary.ingresos > 0.0 || summary.saldoActualTotal > 0.0
        if (confidence == ProjectionConfidence.LOW && hasAnyData) {
            notes.add("El analisis aun es preliminar porque hay pocos movimientos.")
        }
        if (money.hasInitialBalance && money.recurringIncome <= 0.0 && summary.saldoActualTotal > 0.0) {
            notes.add("Ya tienes saldo inicial registrado. Registra ingresos y gastos habituales para mejorar el analisis.")
        }
        if (money.hasInitialBalance && money.operatingBalance < 0.0 && money.visibleBalance >= 0.0 && summary.gastos > 0.0) {
            notes.add("El balance operativo excluye el saldo inicial; sirve solo para proyectar ingresos habituales.")
        }
        return notes.distinct().take(3)
    }

    private fun buildPrimaryInsight(
        summary: HomeSummary,
        currentExpenses: Double,
        previousExpenses: Double,
        confidence: ProjectionConfidence,
        money: AnalysisMoney
    ): String {
        if (money.recurringIncome <= 0.0 && summary.gastos <= 0.0 && summary.saldoActualTotal > 0.0) {
            return "Tu saldo actual esta estable. Aun no hay suficientes movimientos para evaluar tu ritmo mensual."
        }
        if (money.recurringIncome <= 0.0 && summary.gastos <= 0.0) {
            return "Aun no tienes datos suficientes este mes."
        }
        if (confidence == ProjectionConfidence.LOW) {
            return when {
                summary.saldoActualTotal > 0.0 && summary.gastos > summary.saldoActualTotal ->
                    "Tu saldo actual sigue positivo, pero el margen esta ajustado."
                summary.saldoActualTotal > 0.0 ->
                    "Tu saldo actual esta positivo. El analisis aun es preliminar."
                money.recurringIncome > summary.gastos ->
                    "Vas bien: tus gastos estan por debajo de tus ingresos, aunque aun hay pocos datos."
                money.recurringIncome <= 0.0 && summary.saldoActualTotal <= 0.0 ->
                    "No hay ingresos registrados y tu saldo esta ajustado."
                else -> "Aun hay pocos datos para una proyeccion precisa."
            }
        }

        dominantCategory(summary)?.let { (name, percent) ->
            if (percent >= 45.0) return "Tu mayor gasto hasta ahora es $name (${percent.roundToInt()}% de tus gastos)."
        }

        if (previousExpenses > 0.0) {
            val change = ((currentExpenses - previousExpenses) / previousExpenses) * 100.0
            if (change >= 10.0) return "Tus gastos van ${change.roundToInt()}% por encima del mes anterior."
            if (change <= -10.0) return "Tus gastos van ${kotlin.math.abs(change).roundToInt()}% por debajo del mes anterior."
        }

        return when {
            summary.saldoActualTotal > 0.0 && money.operatingBalance < 0.0 && money.visibleBalance >= 0.0 ->
                "Tu saldo actual sigue positivo; la proyeccion separa el saldo inicial de tus ingresos habituales."
            summary.saldoActualTotal > 0.0 && money.visibleBalance < 0.0 ->
                "Tus gastos del mes superan los ingresos visibles, aunque tu saldo actual sigue positivo."
            money.recurringIncome > summary.gastos ->
                "Vas bien: tus gastos estan por debajo de tus ingresos."
            else -> "Revisa tus gastos principales para mantener tu saldo bajo control."
        }
    }

    private fun calculateSuggestedSaving(
        summary: HomeSummary,
        projectedEndBalance: Double,
        confidence: ProjectionConfidence,
        incomeForAnalysis: Double
    ): Double {
        if (incomeForAnalysis <= 0.0) return 0.0
        val reserve = maxOf(summary.gastos * 1.25, summary.saldoActualTotal * 0.25, 0.0)
        val realMargin = (summary.saldoActualTotal - reserve).coerceAtLeast(0.0)
        val projectedMargin = (projectedEndBalance - reserve).coerceAtLeast(0.0)
        val baseMargin = minOf(realMargin, projectedMargin)
        if (baseMargin <= 0.0 || summary.saldoActualTotal <= 0.0) return 0.0
        val confidenceFactor = when (confidence) {
            ProjectionConfidence.LOW -> 0.08
            ProjectionConfidence.MEDIUM -> 0.12
            ProjectionConfidence.HIGH -> 0.18
        }
        val budgetFactor = when {
            summary.presupuestoMonto > 0.0 && summary.presupuestoPorcentaje >= 100.0 -> 0.0
            summary.presupuestoMonto > 0.0 && summary.presupuestoPorcentaje >= 85.0 -> 0.5
            else -> 1.0
        }
        return (baseMargin * confidenceFactor * budgetFactor).coerceAtLeast(0.0)
    }

    private fun savingMessage(
        summary: HomeSummary,
        suggestedSaving: Double,
        confidence: ProjectionConfidence,
        money: AnalysisMoney
    ): String = when {
        suggestedSaving > 0.0 && confidence == ProjectionConfidence.LOW ->
            "Podrias separar un ahorro pequeno, pero el analisis aun es preliminar."
        suggestedSaving > 0.0 ->
            "Podrias ahorrar este mes sin comprometer tu saldo disponible."
        summary.saldoActualTotal > 0.0 && confidence == ProjectionConfidence.LOW && money.hasInitialBalance ->
            "Tu saldo sigue positivo; espera mas movimientos antes de separar ahorro."
        summary.saldoActualTotal > 0.0 && money.operatingBalance < 0.0 ->
            "Tu saldo sigue positivo, pero espera mas movimientos antes de apartar ahorro."
        summary.saldoActualTotal > 0.0 && money.recurringIncome <= 0.0 ->
            "Tienes saldo disponible; registra ingresos habituales antes de comprometerlo."
        else -> "No se recomienda ahorrar mas por ahora porque tu margen disponible es bajo."
    }

    private fun savingStatus(
        summary: HomeSummary,
        suggestedSaving: Double,
        projectedEndBalance: Double,
        confidence: ProjectionConfidence,
        money: AnalysisMoney
    ): String = when {
        summary.saldoActualTotal <= 0.0 -> "En riesgo"
        confidence == ProjectionConfidence.LOW && money.hasInitialBalance && suggestedSaving <= 0.0 -> "Preliminar"
        suggestedSaving > 0.0 && confidence == ProjectionConfidence.LOW -> "Prudente"
        suggestedSaving > 0.0 -> "Saludable"
        projectedEndBalance > 0.0 && confidence == ProjectionConfidence.LOW -> "Preliminar"
        projectedEndBalance > 0.0 -> "Ajustado"
        else -> "En riesgo"
    }

    private fun calculateScore(
        summary: HomeSummary,
        currentExpenses: Double,
        previousExpenses: Double,
        projectedEndBalance: Double,
        confidence: ProjectionConfidence,
        hasUnusualExpense: Boolean,
        money: AnalysisMoney
    ): Int {
        var score = 62
        score += when {
            summary.saldoActualTotal > maxOf(summary.gastos, 1.0) -> 16
            summary.saldoActualTotal > 0.0 -> 8
            else -> -30
        }
        score += when {
            money.operatingBalance > 0.0 -> 10
            money.operatingBalance < 0.0 && summary.saldoActualTotal > 0.0 -> -5
            money.operatingBalance < 0.0 -> -16
            else -> 0
        }
        if (money.recurringIncome <= 0.0 && currentExpenses > 0.0) {
            score += if (summary.saldoActualTotal > 0.0) -4 else -14
        }
        score += when {
            summary.presupuestoMonto <= 0.0 -> -2
            summary.presupuestoPorcentaje > 100.0 -> -22
            summary.presupuestoPorcentaje >= 85.0 -> -10
            else -> 6
        }
        score += when {
            projectedEndBalance < 0.0 && confidence == ProjectionConfidence.HIGH -> -18
            projectedEndBalance < 0.0 && confidence == ProjectionConfidence.MEDIUM -> -10
            projectedEndBalance < 0.0 -> -3
            projectedEndBalance > summary.saldoActualTotal * 0.5 -> 5
            else -> 0
        }
        if (previousExpenses > 0.0 && confidence != ProjectionConfidence.LOW) {
            val change = ((currentExpenses - previousExpenses) / previousExpenses) * 100.0
            if (change >= 30.0) score -= 10 else if (change <= -15.0) score += 4
        }
        if (hasUnusualExpense) score -= if (confidence == ProjectionConfidence.LOW) 3 else 8
        dominantCategory(summary)?.let { (_, percent) ->
            if (percent >= 70.0 && summary.cantidadGastosMes >= 3) score -= 5
        }
        return score.coerceIn(0, 100)
    }

    private fun buildScoreExplanation(
        summary: HomeSummary,
        projectedEndBalance: Double,
        confidence: ProjectionConfidence,
        scoreState: String,
        money: AnalysisMoney
    ): String {
        val parts = mutableListOf<String>()
        parts.add(if (summary.saldoActualTotal > 0.0) "saldo actual positivo" else "saldo actual ajustado")
        when {
            money.operatingBalance < 0.0 && money.visibleBalance >= 0.0 && money.hasInitialBalance ->
                parts.add("balance operativo preliminar")
            money.visibleBalance < 0.0 ->
                parts.add("balance visible ajustado")
            money.operatingBalance < 0.0 ->
                parts.add("balance operativo ajustado")
        }
        if (money.recurringIncome <= 0.0 && summary.gastos > 0.0 && summary.saldoActualTotal > 0.0) {
            parts.add("ingresos habituales pendientes")
        }
        if (confidence == ProjectionConfidence.LOW) parts.add("proyeccion preliminar")
        if (summary.presupuestoMonto > 0.0) {
            parts.add(if (summary.presupuestoPorcentaje <= 85.0) "presupuesto bajo control" else "presupuesto cerca del limite")
        }
        dominantCategory(summary)?.let { (name, _) -> parts.add("mayor gasto en $name") }
        if (projectedEndBalance < 0.0 && confidence != ProjectionConfidence.LOW) parts.add("saldo estimado ajustado")
        return "Score $scoreState: ${parts.distinct().take(4).joinToString(", ")}."
    }

    private fun dominantCategory(summary: HomeSummary): Pair<String, Double>? {
        val top = summary.chartCategorias.maxByOrNull { it.gastado } ?: return null
        if (summary.gastos <= 0.0 || top.gastado <= 0.0) return null
        val name = top.categoriaNombre?.takeIf { it.isNotBlank() } ?: "Sin categoria"
        return name to ((top.gastado / summary.gastos) * 100.0)
    }

    private fun dayKey(millis: Long?): String? {
        if (millis == null || millis <= 0L) return null
        return Calendar.getInstance().apply { timeInMillis = millis }.let {
            "%04d-%02d-%02d".format(Locale.US, it.get(Calendar.YEAR), it.get(Calendar.MONTH) + 1, it.get(Calendar.DAY_OF_MONTH))
        }
    }

    private fun daysInMonth(anio: Int, mes: Int): Int {
        return Calendar.getInstance().apply {
            clear()
            set(anio, mes - 1, 1)
        }.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    private fun elapsedDaysForMonth(anio: Int, mes: Int, daysInMonth: Int, today: Calendar): Int {
        return if (today.get(Calendar.YEAR) == anio && today.get(Calendar.MONTH) + 1 == mes) {
            today.get(Calendar.DAY_OF_MONTH).coerceIn(1, daysInMonth)
        } else {
            daysInMonth
        }
    }
}
