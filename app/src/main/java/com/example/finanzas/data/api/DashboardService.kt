package com.example.finanzas.data.api

import android.content.Context
import com.example.finanzas.data.local.LocalRepository
import com.example.finanzas.data.model.HomeSummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object DashboardService {

    interface SummaryCb {
        fun onOk(summary: HomeSummary)
        fun onError()
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    suspend fun getSummary(ctx: Context, anio: Int, mes: Int): HomeSummary = withContext(Dispatchers.IO) {
        LocalRepository.getInstance(ctx).buildHomeSummary(anio, mes)
    }

    @JvmStatic
    fun getSummary(ctx: Context, anio: Int, mes: Int, cb: SummaryCb) {
        scope.launch {
            runCatching { getSummary(ctx, anio, mes) }
                .onSuccess(cb::onOk)
                .onFailure { cb.onError() }
        }
    }
}
