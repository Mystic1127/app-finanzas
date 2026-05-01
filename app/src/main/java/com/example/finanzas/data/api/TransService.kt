package com.example.finanzas.data.api

import android.content.Context
import com.example.finanzas.data.local.LocalRepository
import com.example.finanzas.data.model.Transaccion
import com.example.finanzas.data.model.TransaccionFiltro
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object TransService {

    interface ListCb { fun onOk(res: List<Transaccion>); fun onError() }
    interface SimpleCb { fun onOk(newId: Int); fun onError(message: String?) }
    interface VoidCb { fun onOk(); fun onError(message: String?) }
    interface FileCb { fun onOk(path: String); fun onError(message: String?) }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    suspend fun list(ctx: Context, anio: Int, mes: Int, filtro: TransaccionFiltro?): List<Transaccion> =
        withContext(Dispatchers.IO) { LocalRepository.getInstance(ctx).listTransacciones(anio, mes, filtro) }

    suspend fun create(
        ctx: Context,
        categoriaId: Long,
        esIngreso: Boolean,
        monto: Double,
        nota: String,
        fecha: Long
    ): Int = create(ctx, categoriaId, esIngreso, monto, nota, fecha, SettingsService.getCurrencyCode(ctx))

    suspend fun create(
        ctx: Context,
        categoriaId: Long,
        esIngreso: Boolean,
        monto: Double,
        nota: String,
        fecha: Long,
        moneda: String
    ): Int = withContext(Dispatchers.IO) {
        LocalRepository.getInstance(ctx).createTransaccion(categoriaId.toInt(), esIngreso, monto, nota, fecha, moneda)
    }

    suspend fun update(
        ctx: Context,
        id: Long,
        categoriaId: Long,
        esIngreso: Boolean,
        monto: Double,
        nota: String,
        fecha: Long
    ): Boolean = update(ctx, id, categoriaId, esIngreso, monto, nota, fecha, SettingsService.getCurrencyCode(ctx))

    suspend fun update(
        ctx: Context,
        id: Long,
        categoriaId: Long,
        esIngreso: Boolean,
        monto: Double,
        nota: String,
        fecha: Long,
        moneda: String
    ): Boolean = withContext(Dispatchers.IO) {
        LocalRepository.getInstance(ctx)
            .updateTransaccion(id.toInt(), categoriaId.toInt(), esIngreso, monto, nota, fecha, moneda)
    }

    suspend fun delete(ctx: Context, id: Long): Boolean = withContext(Dispatchers.IO) {
        LocalRepository.getInstance(ctx).deleteTransaccion(id.toInt())
    }

    suspend fun exportToTxt(ctx: Context): String = withContext(Dispatchers.IO) {
        LocalRepository.getInstance(ctx).exportTransacciones()
    }

    @JvmStatic
    fun list(ctx: Context, anio: Int, mes: Int, cb: ListCb) = list(ctx, anio, mes, null, cb)

    @JvmStatic
    fun list(ctx: Context, anio: Int, mes: Int, filtro: TransaccionFiltro?, cb: ListCb) {
        scope.launch {
            runCatching { list(ctx, anio, mes, filtro) }
                .onSuccess(cb::onOk)
                .onFailure { cb.onError() }
        }
    }

    @JvmStatic
    fun create(ctx: Context, categoriaId: Long, esIngreso: Boolean, monto: Double, nota: String, fecha: Long, cb: SimpleCb) {
        create(ctx, categoriaId, esIngreso, monto, nota, fecha, SettingsService.getCurrencyCode(ctx), cb)
    }

    @JvmStatic
    fun create(ctx: Context, categoriaId: Long, esIngreso: Boolean, monto: Double, nota: String, fecha: Long, moneda: String, cb: SimpleCb) {
        scope.launch {
            runCatching { create(ctx, categoriaId, esIngreso, monto, nota, fecha, moneda) }
                .onSuccess(cb::onOk)
                .onFailure { cb.onError("No se pudo crear") }
        }
    }

    @JvmStatic
    fun update(ctx: Context, id: Long, categoriaId: Long, esIngreso: Boolean, monto: Double, nota: String, fecha: Long, cb: VoidCb) {
        update(ctx, id, categoriaId, esIngreso, monto, nota, fecha, SettingsService.getCurrencyCode(ctx), cb)
    }

    @JvmStatic
    fun update(ctx: Context, id: Long, categoriaId: Long, esIngreso: Boolean, monto: Double, nota: String, fecha: Long, moneda: String, cb: VoidCb) {
        scope.launch {
            runCatching { update(ctx, id, categoriaId, esIngreso, monto, nota, fecha, moneda) }
                .onSuccess { if (it) cb.onOk() else cb.onError("No se pudo actualizar") }
                .onFailure { cb.onError("No se pudo actualizar") }
        }
    }

    @JvmStatic
    fun delete(ctx: Context, id: Long, cb: VoidCb) {
        scope.launch {
            runCatching { delete(ctx, id) }
                .onSuccess { if (it) cb.onOk() else cb.onError("No se pudo eliminar") }
                .onFailure { cb.onError("No se pudo eliminar") }
        }
    }

    @JvmStatic
    fun exportToTxt(ctx: Context, cb: FileCb) {
        scope.launch {
            runCatching { exportToTxt(ctx) }
                .onSuccess(cb::onOk)
                .onFailure { cb.onError("No se pudo exportar") }
        }
    }
}
