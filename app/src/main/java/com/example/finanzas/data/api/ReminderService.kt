package com.example.finanzas.data.api

import android.content.Context
import com.example.finanzas.data.local.LocalRepository
import com.example.finanzas.data.model.PaymentReminder
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale

object ReminderService {

    interface ListCb {
        fun onOk(items: List<PaymentReminder>)
        fun onFail()
    }

    interface SaveCb {
        fun onOk(id: Int, responseNotificationId: String?)
        fun onFail()
    }

    interface SimpleCb {
        fun onOk()
        fun onFail()
    }

    @JvmStatic
    fun list(ctx: Context, soloPendientes: Boolean, cb: ListCb) {
        DbCoroutine.io(
            block = {
                val list = LocalRepository.getInstance(ctx).listReminders(!soloPendientes)
                if (soloPendientes) list.filterNot { it.isPagado } else list
            },
            onSuccess = cb::onOk,
            onError = cb::onFail
        )
    }

    @JvmStatic
    fun save(ctx: Context, body: JSONObject, cb: SaveCb) {
        DbCoroutine.io(
            block = {
                val reminder = PaymentReminder().apply {
                    id = body.optInt("id", 0)
                    titulo = body.optString("titulo", "")
                    monto = body.optDouble("monto", 0.0)

                    val fechaStr = body.optString("fecha_vencimiento", null)
                    if (!fechaStr.isNullOrEmpty()) {
                        try {
                            val df = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                            fechaVencimiento = df.parse(fechaStr)
                        } catch (_: Exception) {
                        }
                    }

                    pagado = body.optBoolean("pagado", false) || body.optInt("pagado", 0) == 1
                    if (body.has("categoria_id") && !body.isNull("categoria_id")) {
                        categoriaId = body.optInt("categoria_id")
                    }
                    horaRecordatorio = body.optString("hora_recordatorio", null)
                    frecuencia = body.optString("frecuencia", null)
                    notificar = body.optBoolean("notificar", false) || body.optInt("notificar", 0) == 1
                    diasRecordatorio = body.optInt("dias_recordatorio", 0)
                    googleEventId = body.optString("google_event_id", null)
                    notificationId = body.optString("notification_id", null)
                }

                val ok = LocalRepository.getInstance(ctx).saveReminder(reminder)
                if (ok) reminder else null
            },
            onSuccess = { reminder ->
                if (reminder != null) cb.onOk(reminder.id, reminder.notificationId) else cb.onFail()
            },
            onError = cb::onFail
        )
    }

    @JvmStatic
    fun marcarPagado(ctx: Context, id: Int, pagado: Boolean, cb: SimpleCb) {
        DbCoroutine.io(
            block = { LocalRepository.getInstance(ctx).markReminderPaid(id, pagado) },
            onSuccess = { ok -> if (ok) cb.onOk() else cb.onFail() },
            onError = cb::onFail
        )
    }

    @JvmStatic
    fun delete(ctx: Context, id: Int, cb: SimpleCb) {
        DbCoroutine.io(
            block = { LocalRepository.getInstance(ctx).deleteReminder(id) },
            onSuccess = { ok -> if (ok) cb.onOk() else cb.onFail() },
            onError = cb::onFail
        )
    }
}
