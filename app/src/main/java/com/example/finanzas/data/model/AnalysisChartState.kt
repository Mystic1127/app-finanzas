package com.example.finanzas.data.model

import kotlin.math.abs

data class AnalysisMonthlyPoint(
    val anio: Int,
    val mes: Int,
    val label: String,
    val ingresos: Double,
    val gastos: Double,
    val ahorro: Double,
    val balance: Double,
)

data class AnalysisCategoryPoint(
    val name: String,
    val amount: Double,
)

data class AnalysisChartState(
    val currencyCode: String,
    val months: List<AnalysisMonthlyPoint>,
    val categories: List<AnalysisCategoryPoint>,
) {
    fun hasIncomeExpense(): Boolean = months.any { it.ingresos > 0.0 || it.gastos > 0.0 }

    fun hasExpenseTrend(): Boolean = months.any { it.gastos > 0.0 }

    fun hasBalance(): Boolean = months.any { abs(it.balance) > 0.005 }

    fun hasSavings(): Boolean = months.any { abs(it.ahorro) > 0.005 }

    fun hasCategories(): Boolean = categories.isNotEmpty()
}
