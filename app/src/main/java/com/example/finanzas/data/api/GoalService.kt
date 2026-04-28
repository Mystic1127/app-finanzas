package com.example.finanzas.data.api

import android.content.Context
import com.example.finanzas.data.local.LocalRepository
import com.example.finanzas.data.model.SavingsGoal
import org.json.JSONObject

object GoalService {

    interface ListCb {
        fun onOk(items: List<SavingsGoal>)
        fun onFail()
    }

    interface SaveCb {
        fun onOk()
        fun onFail()
    }

    interface DeleteCb {
        fun onOk()
        fun onFail()
    }

    interface MilestoneSaveCb {
        fun onOk(id: Int)
        fun onFail()
    }

    @JvmStatic
    fun list(ctx: Context, cb: ListCb) {
        DbCoroutine.io(
            block = { LocalRepository.getInstance(ctx).listGoals() },
            onSuccess = cb::onOk,
            onError = cb::onFail
        )
    }

    @JvmStatic
    fun save(ctx: Context, body: JSONObject, cb: SaveCb) {
        DbCoroutine.io(
            block = {
                val id = body.optInt("id", 0)
                val titulo = body.optString("titulo", "")
                val objetivo = body.optDouble("monto_objetivo", 0.0)
                val actual = body.optDouble("monto_actual", 0.0)
                val fecha = body.optString("fecha_objetivo", null)
                LocalRepository.getInstance(ctx).saveGoal(id, titulo, objetivo, actual, fecha)
            },
            onSuccess = { ok -> if (ok) cb.onOk() else cb.onFail() },
            onError = cb::onFail
        )
    }

    @JvmStatic
    fun delete(ctx: Context, id: Int, cb: DeleteCb) {
        DbCoroutine.io(
            block = { LocalRepository.getInstance(ctx).deleteGoal(id) },
            onSuccess = { ok -> if (ok) cb.onOk() else cb.onFail() },
            onError = cb::onFail
        )
    }

    @JvmStatic
    fun saveMilestone(ctx: Context, body: JSONObject, cb: MilestoneSaveCb) {
        DbCoroutine.io(
            block = {
                val id = body.optInt("id", 0)
                val metaId = body.optInt("meta_id", 0)
                val titulo = body.optString("titulo", "")
                val monto = body.optDouble("monto_planificado", 0.0)
                val fecha = body.optString("fecha_objetivo", null)
                val notificar = body.optInt("notificar", 0) == 1 || body.optBoolean("notificar", false)
                val dias = body.optInt("dias_recordatorio", 0)
                val completado = body.optInt("completado", 0) == 1 || body.optBoolean("completado", false)
                LocalRepository.getInstance(ctx)
                    .saveMilestone(id, metaId, titulo, monto, fecha, notificar, dias, completado)
            },
            onSuccess = { newId -> if (newId > 0) cb.onOk(newId) else cb.onFail() },
            onError = cb::onFail
        )
    }

    @JvmStatic
    fun deleteMilestone(ctx: Context, id: Int, cb: DeleteCb) {
        DbCoroutine.io(
            block = { LocalRepository.getInstance(ctx).deleteMilestone(id) },
            onSuccess = { ok -> if (ok) cb.onOk() else cb.onFail() },
            onError = cb::onFail
        )
    }
}
