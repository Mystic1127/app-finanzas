package com.example.finanzas.data.api

import android.content.Context
import com.example.finanzas.data.local.LocalRepository
import com.example.finanzas.data.model.Transaccion
import com.example.finanzas.data.model.TransaccionFiltro

object TransService {

    interface ListCb {
        fun onOk(res: List<Transaccion>)
        fun onError()
    }

    interface SimpleCb {
        fun onOk(newId: Int)
        fun onError(message: String?)
    }

    interface VoidCb {
        fun onOk()
        fun onError(message: String?)
    }

    interface FileCb {
        fun onOk(path: String)
        fun onError(message: String?)
    }

    @JvmStatic
    fun list(ctx: Context, anio: Int, mes: Int, cb: ListCb) {
        list(ctx, anio, mes, null, cb)
    }

    @JvmStatic
    fun list(ctx: Context, anio: Int, mes: Int, filtro: TransaccionFiltro?, cb: ListCb) {
        DbCoroutine.io(
            block = { LocalRepository.getInstance(ctx).listTransacciones(anio, mes, filtro) },
            onSuccess = cb::onOk,
            onError = cb::onError
        )
    }

    @JvmStatic
    fun create(
        ctx: Context,
        categoriaId: Long,
        esIngreso: Boolean,
        monto: Double,
        nota: String,
        fecha: Long,
        cb: SimpleCb
    ) {
        DbCoroutine.io(
            block = {
                LocalRepository.getInstance(ctx)
                    .createTransaccion(categoriaId.toInt(), esIngreso, monto, nota, fecha)
            },
            onSuccess = cb::onOk,
            onError = { cb.onError("No se pudo crear") }
        )
    }

    @JvmStatic
    fun update(
        ctx: Context,
        id: Long,
        categoriaId: Long,
        esIngreso: Boolean,
        monto: Double,
        nota: String,
        fecha: Long,
        cb: VoidCb
    ) {
        DbCoroutine.io(
            block = {
                LocalRepository.getInstance(ctx)
                    .updateTransaccion(id.toInt(), categoriaId.toInt(), esIngreso, monto, nota, fecha)
            },
            onSuccess = { ok -> if (ok) cb.onOk() else cb.onError("No se pudo actualizar") },
            onError = { cb.onError("No se pudo actualizar") }
        )
    }

    @JvmStatic
    fun delete(ctx: Context, id: Long, cb: VoidCb) {
        DbCoroutine.io(
            block = { LocalRepository.getInstance(ctx).deleteTransaccion(id.toInt()) },
            onSuccess = { ok -> if (ok) cb.onOk() else cb.onError("No se pudo eliminar") },
            onError = { cb.onError("No se pudo eliminar") }
        )
    }

    @JvmStatic
    fun exportToTxt(ctx: Context, cb: FileCb) {
        DbCoroutine.io(
            block = { LocalRepository.getInstance(ctx).exportTransacciones() },
            onSuccess = cb::onOk,
            onError = { cb.onError("No se pudo exportar") }
        )
    }
}
