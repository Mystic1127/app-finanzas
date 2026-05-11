package com.example.finanzas.data.api

import android.content.Context
import com.example.finanzas.data.cloud.CloudSyncService
import com.example.finanzas.data.local.LocalRepository
import com.example.finanzas.data.model.PaymentReminder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale

object ReminderService {

    interface ListCb { fun onOk(items: List<PaymentReminder>); fun onFail() }
    interface SaveCb { fun onOk(id: Int, responseNotificationId: String?); fun onFail() }
    interface SimpleCb { fun onOk(); fun onFail() }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    suspend fun list(ctx: Context, soloPendientes: Boolean): List<PaymentReminder> = withContext(Dispatchers.IO) {
        val list = LocalRepository.getInstance(ctx).listReminders(!soloPendientes)
        if (soloPendientes) list.filterNot { it.isPagado } else list
    }

    suspend fun save(ctx: Context, body: JSONObject): PaymentReminder? = withContext(Dispatchers.IO) {
        val reminder = PaymentReminder().apply {
            setId(body.optInt("id", 0))
            setTitulo(body.optString("titulo", ""))
            setMonto(body.optDouble("monto", 0.0))
            setMoneda(body.optString("moneda", SettingsService.getCurrencyCode(ctx)))
            val fechaStr = body.optString("fecha_vencimiento", null)
            if (!fechaStr.isNullOrEmpty()) {
                try {
                    setFechaVencimiento(SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(fechaStr))
                } catch (_: Exception) {
                }
            }
            setPagado(body.optBoolean("pagado", false) || body.optInt("pagado", 0) == 1)
            if (body.has("categoria_id") && !body.isNull("categoria_id")) setCategoriaId(body.optInt("categoria_id"))
            setHoraRecordatorio(body.optString("hora_recordatorio", null))
            setFrecuencia(body.optString("frecuencia", null))
            setNotificar(body.optBoolean("notificar", false) || body.optInt("notificar", 0) == 1)
            setDiasRecordatorio(body.optInt("dias_recordatorio", 0))
            setGoogleEventId(body.optString("google_event_id", null))
            setNotificationId(body.optString("notification_id", null))
        }
        if (LocalRepository.getInstance(ctx).saveReminder(reminder)) {
            CloudSyncService.scheduleUpload(ctx)
            reminder
        } else null
    }

    suspend fun marcarPagado(ctx: Context, id: Int, pagado: Boolean): Boolean = withContext(Dispatchers.IO) {
        LocalRepository.getInstance(ctx).markReminderPaid(id, pagado)
            .also { if (it) CloudSyncService.scheduleUpload(ctx) }
    }

    suspend fun delete(ctx: Context, id: Int): Boolean = withContext(Dispatchers.IO) {
        LocalRepository.getInstance(ctx).deleteReminder(id)
            .also { if (it) CloudSyncService.scheduleUpload(ctx) }
    }

    @JvmStatic
    fun list(ctx: Context, soloPendientes: Boolean, cb: ListCb) {
        scope.launch { runCatching { list(ctx, soloPendientes) }.onSuccess(cb::onOk).onFailure { cb.onFail() } }
    }

    @JvmStatic
    fun save(ctx: Context, body: JSONObject, cb: SaveCb) {
        scope.launch {
            runCatching { save(ctx, body) }
                .onSuccess { r -> if (r != null) cb.onOk(r.id, r.notificationId) else cb.onFail() }
                .onFailure { cb.onFail() }
        }
    }

    @JvmStatic
    fun marcarPagado(ctx: Context, id: Int, pagado: Boolean, cb: SimpleCb) {
        scope.launch {
            runCatching { marcarPagado(ctx, id, pagado) }
                .onSuccess { if (it) cb.onOk() else cb.onFail() }
                .onFailure { cb.onFail() }
        }
    }

    @JvmStatic
    fun delete(ctx: Context, id: Int, cb: SimpleCb) {
        scope.launch {
            runCatching { delete(ctx, id) }
                .onSuccess { if (it) cb.onOk() else cb.onFail() }
                .onFailure { cb.onFail() }
        }
    }
}
