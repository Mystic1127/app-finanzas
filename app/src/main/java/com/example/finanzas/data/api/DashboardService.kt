package com.example.finanzas.data.api

import android.content.Context
import com.example.finanzas.data.model.HomeSummary
import com.example.finanzas.data.repository.DashboardRepository

object DashboardService {

    suspend fun getSummary(ctx: Context, anio: Int, mes: Int): HomeSummary {
        return DashboardRepository(ctx).getSummary(anio, mes)
    }
}
