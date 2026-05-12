package com.example.finanzas.data.model

data class FinancialAssistantMonthlyAggregate(
    val year: Int,
    val month: Int,
    val currency: String,
    val totalIncome: Double,
    val totalExpense: Double,
    val currentBalance: Double,
    val estimatedMonthEndBalance: Double,
    val projectedExpense: Double,
    val budgetTotal: Double,
    val budgetUsed: Double,
    val budgetRemaining: Double,
    val topExpenseCategoryName: String?,
    val topExpenseCategoryAmount: Double,
    val activeGoalsCount: Int,
    val activeGoalsTotalTarget: Double,
    val activeGoalsSavedAmount: Double,
    val recurringExpensesTotal: Double?,
    val financialScore: Int?,
    val riskHints: List<String>
)

enum class FinancialAssistantRiskLevel {
    LOW,
    MEDIUM,
    HIGH
}

enum class FinancialAssistantGeneratedBy {
    ai,
    local_fallback
}

data class FinancialAssistantResult(
    val summary: String,
    val suggestedSavingAmount: Double,
    val riskLevel: FinancialAssistantRiskLevel,
    val riskLabel: String,
    val recommendedAction: String,
    val alerts: List<String>,
    val positiveInsight: String,
    val mainConcern: String,
    val generatedBy: FinancialAssistantGeneratedBy
)

sealed class FinancialAssistantUiState {
    data object Idle : FinancialAssistantUiState()
    data object Loading : FinancialAssistantUiState()
    data class Ready(val result: FinancialAssistantResult) : FinancialAssistantUiState()
}
