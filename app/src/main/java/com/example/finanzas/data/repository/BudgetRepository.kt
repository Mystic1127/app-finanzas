package com.example.finanzas.data.repository

import android.content.Context
import com.example.finanzas.data.api.BudgetService

class BudgetRepository(private val context: Context) {
    suspend fun get(anio: Int, mes: Int): Double = BudgetService.get(context, anio, mes)
    suspend fun set(anio: Int, mes: Int, monto: Double) = BudgetService.set(context, anio, mes, monto)
}
