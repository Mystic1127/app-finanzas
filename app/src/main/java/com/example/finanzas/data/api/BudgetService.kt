package com.example.finanzas.data.api

import android.content.Context
import com.example.finanzas.data.local.LocalRepository

object BudgetService {

    interface GetCb {
        fun onOk(monto: Double)
        fun onFail()
    }

    interface SimpleCb {
        fun onOk()
        fun onFail()
    }

    @JvmStatic
    fun get(ctx: Context, anio: Int, mes: Int, cb: GetCb) {
        DbCoroutine.io(
            block = { LocalRepository.getInstance(ctx).getPresupuesto(anio, mes) },
            onSuccess = cb::onOk,
            onError = cb::onFail
        )
    }

    @JvmStatic
    fun set(ctx: Context, anio: Int, mes: Int, monto: Double, cb: SimpleCb) {
        DbCoroutine.io(
            block = {
                LocalRepository.getInstance(ctx).setPresupuesto(anio, mes, monto)
                Unit
            },
            onSuccess = { cb.onOk() },
            onError = cb::onFail
        )
    }
}
