package com.example.finanzas.data.api

import android.content.Context
import com.example.finanzas.data.local.LocalRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object BudgetService {

    interface GetCb {
        fun onOk(monto: Double)
        fun onFail()
    }

    interface SimpleCb {
        fun onOk()
        fun onFail()
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    suspend fun get(ctx: Context, anio: Int, mes: Int): Double = withContext(Dispatchers.IO) {
        LocalRepository.getInstance(ctx).getPresupuesto(anio, mes)
    }

    suspend fun ensurePlanForMonth(ctx: Context, anio: Int, mes: Int): Boolean = withContext(Dispatchers.IO) {
        LocalRepository.getInstance(ctx).ensurePresupuestoPlanForMonth(anio, mes)
    }

    suspend fun set(ctx: Context, anio: Int, mes: Int, monto: Double) =
        set(ctx, anio, mes, monto, SettingsService.getCurrencyCode(ctx))

    suspend fun set(ctx: Context, anio: Int, mes: Int, monto: Double, moneda: String) = withContext(Dispatchers.IO) {
        LocalRepository.getInstance(ctx).setPresupuesto(anio, mes, monto, moneda)
    }

    @JvmStatic
    fun get(ctx: Context, anio: Int, mes: Int, cb: GetCb) {
        scope.launch {
            runCatching { get(ctx, anio, mes) }
                .onSuccess(cb::onOk)
                .onFailure { cb.onFail() }
        }
    }

    @JvmStatic
    fun set(ctx: Context, anio: Int, mes: Int, monto: Double, cb: SimpleCb) {
        scope.launch {
            runCatching { set(ctx, anio, mes, monto) }
                .onSuccess { cb.onOk() }
                .onFailure { cb.onFail() }
        }
    }
}
