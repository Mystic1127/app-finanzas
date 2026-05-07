package com.example.finanzas.data.api

import android.content.Context
import com.example.finanzas.data.local.LocalRepository
import com.example.finanzas.data.model.CategoryBudgetInput
import com.example.finanzas.data.model.CategoryBudgetSummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object CategoryBudgetService {

    interface ListCb {
        fun onOk(items: List<CategoryBudgetSummary>)
        fun onFail()
    }

    interface SaveCb {
        fun onOk()
        fun onFail()
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    suspend fun list(ctx: Context, anio: Int, mes: Int): List<CategoryBudgetSummary> = withContext(Dispatchers.IO) {
        LocalRepository.getInstance(ctx).listPresupuestosCategoria(anio, mes)
    }

    suspend fun save(ctx: Context, anio: Int, mes: Int, items: List<*>) = withContext(Dispatchers.IO) {
        LocalRepository.getInstance(ctx).savePresupuestosCategoria(anio, mes, items)
    }

    suspend fun savedInputs(ctx: Context, anio: Int, mes: Int): List<CategoryBudgetInput> = withContext(Dispatchers.IO) {
        LocalRepository.getInstance(ctx).listSavedPresupuestosCategoriaInputs(anio, mes)
    }

    @JvmStatic
    fun list(ctx: Context, anio: Int, mes: Int, cb: ListCb) {
        scope.launch {
            runCatching { list(ctx, anio, mes) }
                .onSuccess(cb::onOk)
                .onFailure { cb.onFail() }
        }
    }

    @JvmStatic
    fun save(ctx: Context, anio: Int, mes: Int, items: List<*>, cb: SaveCb) {
        scope.launch {
            runCatching { save(ctx, anio, mes, items) }
                .onSuccess { cb.onOk() }
                .onFailure { cb.onFail() }
        }
    }
}
