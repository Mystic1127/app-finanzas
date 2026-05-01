package com.example.finanzas.di

import android.content.Context
import com.example.finanzas.data.repository.BudgetRepository
import com.example.finanzas.data.repository.DashboardRepository
import com.example.finanzas.data.repository.GoalRepository
import com.example.finanzas.data.repository.ReminderRepository
import com.example.finanzas.data.repository.TransactionRepository
import com.example.finanzas.domain.usecase.FinancialRecommendationEngine
import com.example.finanzas.domain.usecase.FinancialDashboardEngine
import com.example.finanzas.domain.usecase.SmartSpendingAlertUseCase

class AppGraph(context: Context) {
    private val appContext = context.applicationContext

    val transactionRepository by lazy { TransactionRepository(appContext) }
    val dashboardRepository by lazy { DashboardRepository(appContext) }
    val budgetRepository by lazy { BudgetRepository(appContext) }
    val goalRepository by lazy { GoalRepository(appContext) }
    val reminderRepository by lazy { ReminderRepository(appContext) }

    val smartSpendingAlertUseCase by lazy { SmartSpendingAlertUseCase() }
    val financialRecommendationEngine by lazy { FinancialRecommendationEngine() }
    val financialDashboardEngine by lazy { FinancialDashboardEngine(appContext, financialRecommendationEngine) }
}
