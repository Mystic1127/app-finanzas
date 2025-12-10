package com.example.finanzas.data.api;

import android.content.Context;

import com.example.finanzas.data.local.LocalRepository;
import com.example.finanzas.data.model.PaymentReminder;

import org.json.JSONObject;

import java.util.List;

import androidx.annotation.Nullable;

public class ReminderService {

    public interface ListCb { void onOk(List<PaymentReminder> items); void onFail(); }
    public interface SaveCb { void onOk(int id, @Nullable String responseNotificationId); void onFail(); }
    public interface SimpleCb { void onOk(); void onFail(); }

    public static void list(Context ctx, boolean soloPendientes, ListCb cb) {
        try {
            List<PaymentReminder> list = LocalRepository.getInstance(ctx).listReminders(!soloPendientes ? true : false);
            if (soloPendientes) {
                list.removeIf(PaymentReminder::isPagado);
            }
            cb.onOk(list);
        } catch (Exception e) {
            cb.onFail();
        }
    }

    public static void save(Context ctx, JSONObject body, SaveCb cb) {
        PaymentReminder reminder = new PaymentReminder();
        reminder.setId(body.optInt("id", 0));
        reminder.setTitulo(body.optString("titulo", ""));
        reminder.setMonto(body.optDouble("monto", 0));
        String fechaStr = body.optString("fecha_vencimiento", null);
        if (fechaStr != null && !fechaStr.isEmpty()) {
            try {
                java.text.SimpleDateFormat df = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
                reminder.setFechaVencimiento(df.parse(fechaStr));
            } catch (Exception ignore) { }
        }
        reminder.setPagado(body.optBoolean("pagado", false) || body.optInt("pagado", 0) == 1);
        if (body.has("categoria_id") && !body.isNull("categoria_id")) {
            reminder.setCategoriaId(body.optInt("categoria_id"));
        }
        reminder.setHoraRecordatorio(body.optString("hora_recordatorio", null));
        reminder.setFrecuencia(body.optString("frecuencia", null));
        reminder.setNotificar(body.optBoolean("notificar", false) || body.optInt("notificar", 0) == 1);
        reminder.setDiasRecordatorio(body.optInt("dias_recordatorio", 0));
        reminder.setGoogleEventId(body.optString("google_event_id", null));
        reminder.setNotificationId(body.optString("notification_id", null));
        boolean ok = LocalRepository.getInstance(ctx).saveReminder(reminder);
        if (ok) cb.onOk(reminder.getId(), reminder.getNotificationId()); else cb.onFail();
    }

    public static void marcarPagado(Context ctx, int id, boolean pagado, SimpleCb cb) {
        boolean ok = LocalRepository.getInstance(ctx).markReminderPaid(id, pagado);
        if (ok) cb.onOk(); else cb.onFail();
    }

    public static void delete(Context ctx, int id, SimpleCb cb) {
        boolean ok = LocalRepository.getInstance(ctx).deleteReminder(id);
        if (ok) cb.onOk(); else cb.onFail();
    }
}
