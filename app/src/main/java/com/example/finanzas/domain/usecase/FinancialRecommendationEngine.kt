package com.example.finanzas.domain.usecase

import com.example.finanzas.data.model.HomeSummary
import com.example.finanzas.data.model.Transaccion

class FinancialRecommendationEngine {
    fun generate(
        summary: HomeSummary,
        currentTransactions: List<Transaccion>,
        previousTransactions: List<Transaccion>
    ): List<String> {
        if (summary.ingresos <= 0.0 && summary.gastos <= 0.0) {
            return listOf("Registra ingresos y gastos para recibir recomendaciones inteligentes")
        }

        val recommendations = mutableListOf<String>()
        addBudgetRecommendations(summary, recommendations)
        addCategoryRecommendation(summary, recommendations)
        addSavingRecommendation(summary, recommendations)
        addUnusualExpenseRecommendation(currentTransactions, previousTransactions, recommendations)
        addGoalRecommendation(summary, recommendations)
        addScoreRecommendation(summary, recommendations)

        return recommendations.distinct().take(4).ifEmpty {
            listOf("Mantén este ritmo y revisa tu presupuesto cada semana")
        }
    }

    private fun addBudgetRecommendations(summary: HomeSummary, out: MutableList<String>) {
        if (summary.presupuestoMonto > 0.0 && summary.presupuestoPorcentaje >= 90.0) {
            out.add("Tu presupuesto mensual está cerca del límite")
        }

        val category = summary.presupuestosCategoria
            .filter { it.limite > 0.0 && it.porcentaje >= 85.0 }
            .maxByOrNull { it.porcentaje }
        if (category != null) {
            val name = category.categoriaNombre?.takeIf { it.isNotBlank() } ?: "esta categoría"
            out.add("Tu presupuesto de $name está cerca del límite")
        }
    }

    private fun addCategoryRecommendation(summary: HomeSummary, out: MutableList<String>) {
        val category = summary.categoriaMayorGasto?.takeIf { it.isNotBlank() } ?: return
        if (summary.categoriaMayorGastoMonto > 0.0 && summary.scoreFinanciero < 70) {
            out.add("Reduce gastos en $category para mejorar tu score")
        }
    }

    private fun addSavingRecommendation(summary: HomeSummary, out: MutableList<String>) {
        when {
            summary.ahorroSugerido > 0.0 && summary.scoreFinanciero >= 70 ->
                out.add("Puedes aumentar tu ahorro si mantienes este ritmo")
            summary.ahorroSugerido <= 0.0 && summary.ingresos > 0.0 ->
                out.add("Prioriza recuperar margen antes de separar más ahorro")
        }
    }

    private fun addUnusualExpenseRecommendation(
        currentTransactions: List<Transaccion>,
        previousTransactions: List<Transaccion>,
        out: MutableList<String>
    ) {
        val expenses = (currentTransactions + previousTransactions)
            .filter { !it.isEsIngreso && it.monto > 0.0 }
            .sortedByDescending { it.fecha?.time ?: 0L }
            .take(12)
        if (expenses.size < 4) return

        val average = expenses.map { it.monto }.average()
        if (currentTransactions.any { !it.isEsIngreso && it.monto >= average * 2.0 }) {
            out.add("Revisa tus gastos inusuales de este mes")
        }
    }

    private fun addGoalRecommendation(summary: HomeSummary, out: MutableList<String>) {
        val activeGoal = summary.metas.firstOrNull { it.montoObjetivo > 0.0 && it.montoActual < it.montoObjetivo }
        if (activeGoal == null) {
            out.add("Crea una meta para convertir tu ahorro en un objetivo concreto")
            return
        }

        if (activeGoal.progreso < 25.0 && summary.ahorroSugerido > 0.0) {
            val name = activeGoal.titulo?.takeIf { it.isNotBlank() } ?: "tu meta"
            out.add("Separa parte del ahorro sugerido para acelerar $name")
        }
    }

    private fun addScoreRecommendation(summary: HomeSummary, out: MutableList<String>) {
        if (summary.scoreFinanciero < 40) {
            out.add("Revisa presupuesto y gastos variables para salir de estado crítico")
        } else if (summary.scoreFinanciero in 40..69) {
            out.add("Tu score está en riesgo; controla las categorías con más gasto")
        }
    }
}
