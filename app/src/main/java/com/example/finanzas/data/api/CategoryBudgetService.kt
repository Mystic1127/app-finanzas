package com.example.finanzas.data.api

import android.content.Context
import com.example.finanzas.data.local.LocalRepository
import com.example.finanzas.data.model.CategoryBudgetSummary

object CategoryBudgetService {

    interface ListCb {
        fun onOk(items: List<CategoryBudgetSummary>)
        fun onFail()
    }

    interface SaveCb {
        fun onOk()
        fun onFail()
    }

    @JvmStatic
    fun list(ctx: Context, anio: Int, mes: Int, cb: ListCb) {
        DbCoroutine.io(
            block = { LocalRepository.getInstance(ctx).listPresupuestosCategoria(anio, mes) },
            onSuccess = cb::onOk,
            onError = cb::onFail
        )
    }

    @JvmStatic
    fun save(ctx: Context, anio: Int, mes: Int, items: List<*>, cb: SaveCb) {
        DbCoroutine.io(
            block = {
                LocalRepository.getInstance(ctx).savePresupuestosCategoria(anio, mes, items)
                Unit
            },
            onSuccess = { cb.onOk() },
            onError = cb::onFail
        )
    }
}
