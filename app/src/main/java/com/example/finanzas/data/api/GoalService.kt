package com.example.finanzas.data.api

import android.content.Context
import com.example.finanzas.data.cloud.CloudSyncService
import com.example.finanzas.data.local.LocalRepository
import com.example.finanzas.data.model.SavingsGoal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

object GoalService {

    interface ListCb { fun onOk(items: List<SavingsGoal>); fun onFail() }
    interface SaveCb { fun onOk(); fun onFail() }
    interface DeleteCb { fun onOk(); fun onFail() }
    interface MilestoneSaveCb { fun onOk(id: Int); fun onFail() }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    suspend fun list(ctx: Context): List<SavingsGoal> = withContext(Dispatchers.IO) {
        LocalRepository.getInstance(ctx).listGoals()
    }

    suspend fun save(ctx: Context, body: JSONObject): Boolean = withContext(Dispatchers.IO) {
        val id = body.optInt("id", 0)
        val titulo = body.optString("titulo", "")
        val objetivo = body.optDouble("monto_objetivo", 0.0)
        val actual = body.optDouble("monto_actual", 0.0)
        val fecha = body.optString("fecha_objetivo", null)
        val moneda = body.optString("moneda", SettingsService.getCurrencyCode(ctx))
        LocalRepository.getInstance(ctx).saveGoal(id, titulo, objetivo, actual, fecha, moneda)
            .also { if (it) CloudSyncService.scheduleUpload(ctx) }
    }

    suspend fun delete(ctx: Context, id: Int): Boolean = withContext(Dispatchers.IO) {
        LocalRepository.getInstance(ctx).deleteGoal(id)
            .also { if (it) CloudSyncService.scheduleUpload(ctx) }
    }

    suspend fun saveMilestone(ctx: Context, body: JSONObject): Int = withContext(Dispatchers.IO) {
        val id = body.optInt("id", 0)
        val metaId = body.optInt("meta_id", 0)
        val titulo = body.optString("titulo", "")
        val monto = body.optDouble("monto_planificado", 0.0)
        val fecha = body.optString("fecha_objetivo", null)
        val moneda = body.optString("moneda", SettingsService.getCurrencyCode(ctx))
        val notificar = body.optInt("notificar", 0) == 1 || body.optBoolean("notificar", false)
        val dias = body.optInt("dias_recordatorio", 0)
        val completado = body.optInt("completado", 0) == 1 || body.optBoolean("completado", false)
        LocalRepository.getInstance(ctx).saveMilestone(id, metaId, titulo, monto, fecha, notificar, dias, completado, moneda)
            .also { if (it > 0) CloudSyncService.scheduleUpload(ctx) }
    }

    suspend fun deleteMilestone(ctx: Context, id: Int): Boolean = withContext(Dispatchers.IO) {
        LocalRepository.getInstance(ctx).deleteMilestone(id)
            .also { if (it) CloudSyncService.scheduleUpload(ctx) }
    }

    @JvmStatic
    fun list(ctx: Context, cb: ListCb) {
        scope.launch { runCatching { list(ctx) }.onSuccess(cb::onOk).onFailure { cb.onFail() } }
    }

    @JvmStatic
    fun save(ctx: Context, body: JSONObject, cb: SaveCb) {
        scope.launch {
            runCatching { save(ctx, body) }
                .onSuccess { if (it) cb.onOk() else cb.onFail() }
                .onFailure { cb.onFail() }
        }
    }

    @JvmStatic
    fun delete(ctx: Context, id: Int, cb: DeleteCb) {
        scope.launch {
            runCatching { delete(ctx, id) }
                .onSuccess { if (it) cb.onOk() else cb.onFail() }
                .onFailure { cb.onFail() }
        }
    }

    @JvmStatic
    fun saveMilestone(ctx: Context, body: JSONObject, cb: MilestoneSaveCb) {
        scope.launch {
            runCatching { saveMilestone(ctx, body) }
                .onSuccess { if (it > 0) cb.onOk(it) else cb.onFail() }
                .onFailure { cb.onFail() }
        }
    }

    @JvmStatic
    fun deleteMilestone(ctx: Context, id: Int, cb: DeleteCb) {
        scope.launch {
            runCatching { deleteMilestone(ctx, id) }
                .onSuccess { if (it) cb.onOk() else cb.onFail() }
                .onFailure { cb.onFail() }
        }
    }
}
