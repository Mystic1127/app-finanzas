package com.example.finanzas.data.model

data class FinancialReport(
    val summary: HomeSummary,
    val currencyCode: String,
    val topCategories: List<CategoryChartSlice>,
    val recentTransactions: List<Transaccion>,
    val trend: List<MonthlyTrendPoint>,
    val status: String,
    val monthLabel: String,
    val generatedAt: Long = System.currentTimeMillis()
) {
    val hasData: Boolean
        get() = summary.ingresos > 0.0 || summary.gastos > 0.0 || recentTransactions.isNotEmpty()
}
