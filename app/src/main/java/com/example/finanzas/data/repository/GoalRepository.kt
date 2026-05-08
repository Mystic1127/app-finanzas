package com.example.finanzas.data.repository

import android.content.Context
import com.example.finanzas.data.api.GoalService
import com.example.finanzas.data.model.SavingsGoal

class GoalRepository(private val context: Context) {
    suspend fun list(): List<SavingsGoal> = GoalService.list(context)
}
