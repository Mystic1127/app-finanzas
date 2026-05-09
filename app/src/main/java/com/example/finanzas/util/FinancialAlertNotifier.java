package com.example.finanzas.util;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.finanzas.R;
import com.example.finanzas.data.model.HomeSummary;
import com.example.finanzas.data.model.PaymentReminder;
import com.example.finanzas.ui.MainActivity;

public final class FinancialAlertNotifier {
    private static final String PREFS = "finanzas_settings";
    private static final String CHANNEL_ID = "financial_alerts";
    private static final String KEY_PREFIX = "financial_alert_notification_";

    private FinancialAlertNotifier() { }

    public static void maybeNotifyImportantAlert(@NonNull Context context, @NonNull HomeSummary summary, @NonNull String currency) {
        if (!hasNotificationPermission(context)) return;
        ensureChannel(context);

        for (PaymentReminder reminder : summary.getRecordatorios()) {
            if (reminder != null && !reminder.isPagado() && reminder.getDiasRestantes() < 0) {
                String title = context.getString(R.string.notification_overdue_title);
                String body = safe(reminder.getTitulo(), context.getString(R.string.home_reminder_without_title))
                        + " - " + Format.money(reminder.getMonto(), reminder.getMoneda());
                notifyOnce(context, "reminder_" + reminder.getId(), title, body, 4010 + reminder.getId());
                return;
            }
        }

        for (PaymentReminder reminder : summary.getRecordatorios()) {
            if (reminder != null && !reminder.isPagado() && reminder.isNotificar()
                    && reminder.getDiasRestantes() >= 0 && reminder.getDiasRestantes() <= 1) {
                String title = reminder.getDiasRestantes() == 0
                        ? context.getString(R.string.notification_reminder_today_title)
                        : context.getString(R.string.notification_reminder_tomorrow_title);
                String body = safe(reminder.getTitulo(), context.getString(R.string.home_reminder_without_title))
                        + " - " + Format.money(reminder.getMonto(), reminder.getMoneda());
                notifyOnce(context, "reminder_due_" + reminder.getId() + "_" + reminder.getDiasRestantes(), title, body, 4110 + reminder.getId());
                return;
            }
        }

        if (summary.isPresupuestoExcedido() && summary.getPresupuestoMonto() > 0.0) {
            String body = Format.money(summary.getGastos(), currency)
                    + " de " + Format.money(summary.getPresupuestoMonto(), currency);
            notifyOnce(context, "budget_" + summary.getAnio() + "_" + summary.getMes(), context.getString(R.string.notification_budget_title), body, 3101);
            return;
        }

        if (summary.getPresupuestoMonto() > 0.0 && summary.getPresupuestoPorcentaje() >= 90.0) {
            String body = Format.money(summary.getGastos(), currency)
                    + " de " + Format.money(summary.getPresupuestoMonto(), currency);
            notifyOnce(context, "budget_near_" + summary.getAnio() + "_" + summary.getMes(), context.getString(R.string.notification_budget_near_title), body, 3105);
            return;
        }

        if (!summary.isProyeccionPreliminar() && summary.getProyeccionFinMes() < 0.0) {
            notifyOnce(context, "projection_negative_" + summary.getAnio() + "_" + summary.getMes(),
                    context.getString(R.string.notification_projection_title),
                    Format.money(summary.getProyeccionFinMes(), currency),
                    3106);
            return;
        }

        if (summary.getEfectivo() > 0.0 && summary.getEfectivo() < criticalThreshold(summary)) {
            notifyOnce(context, "cash_" + summary.getAnio() + "_" + summary.getMes(),
                    context.getString(R.string.notification_account_title),
                    context.getString(R.string.transaction_account_cash) + ": " + Format.money(summary.getEfectivo(), currency),
                    3102);
            return;
        }

        if (summary.getTarjetaCuenta() > 0.0 && summary.getTarjetaCuenta() < criticalThreshold(summary)) {
            notifyOnce(context, "card_" + summary.getAnio() + "_" + summary.getMes(),
                    context.getString(R.string.notification_account_title),
                    context.getString(R.string.transaction_account_card) + ": " + Format.money(summary.getTarjetaCuenta(), currency),
                    3103);
            return;
        }

        if (summary.getSaldoActualTotal() > 0.0 && summary.getSaldoActualTotal() < criticalThreshold(summary)) {
            notifyOnce(context, "low_balance_" + summary.getAnio() + "_" + summary.getMes(),
                    context.getString(R.string.notification_low_balance_title),
                    Format.money(summary.getSaldoActualTotal(), currency),
                    3104);
        }
    }

    private static double criticalThreshold(HomeSummary summary) {
        if (summary.getPresupuestoMonto() > 0.0) {
            return Math.max(20.0, summary.getPresupuestoMonto() * 0.08);
        }
        return 20.0;
    }

    private static void notifyOnce(Context context, String key, String title, String body, int id) {
        long userId = Prefs.getCurrentUserId(context);
        if (userId <= 0) return;
        String prefKey = KEY_PREFIX + userId + "_" + key;
        SharedPreferences sp = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (sp.getBoolean(prefKey, false)) return;

        Intent openIntent = new Intent(context, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                id,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH);
        try {
            NotificationManagerCompat.from(context).notify(id, builder.build());
            sp.edit().putBoolean(prefKey, true).apply();
        } catch (SecurityException ignored) { }
    }

    private static boolean hasNotificationPermission(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    private static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm == null || nm.getNotificationChannel(CHANNEL_ID) != null) return;
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_alerts),
                NotificationManager.IMPORTANCE_HIGH
        );
        nm.createNotificationChannel(channel);
    }

    private static String safe(String value, String fallback) {
        return TextUtils.isEmpty(value) ? fallback : value;
    }
}
