package com.example.finanzas.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.example.finanzas.data.model.PaymentReminder;

import java.util.Calendar;
import java.util.Locale;

public class ReminderScheduler {
    static final String ACTION_REMINDER = "com.example.finanzas.action.REMINDER";
    static final String EXTRA_REMINDER_ID = "extra_reminder_id";
    static final String EXTRA_REMINDER_TITLE = "extra_reminder_title";
    static final String EXTRA_REMINDER_AMOUNT = "extra_reminder_amount";
    static final String EXTRA_REMINDER_DUE = "extra_reminder_due";

    private ReminderScheduler() {}

    public static void schedule(Context context, @Nullable PaymentReminder reminder) {
        if (context == null || reminder == null || !reminder.isNotificar()) {
            cancel(context, reminder);
            return;
        }

        long triggerAt = computeTriggerAt(reminder);
        if (triggerAt <= 0 || triggerAt <= System.currentTimeMillis()) {
            cancel(context, reminder);
            return;
        }

        PendingIntent pi = buildPendingIntent(context, reminder);
        if (pi == null) return;

        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarm == null) return;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarm.canScheduleExactAlarms()) {
                    alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
                } else {
                    alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
                }
            } else {
                alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
            }
        } catch (SecurityException e) {
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        }
    }

    public static void cancel(Context context, @Nullable PaymentReminder reminder) {
        if (context == null || reminder == null) return;
        PendingIntent pi = buildPendingIntent(context, reminder);
        if (pi == null) return;
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarm != null) {
            alarm.cancel(pi);
        }
        pi.cancel();
    }

    private static PendingIntent buildPendingIntent(Context context, PaymentReminder reminder) {
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.setAction(ACTION_REMINDER);
        intent.putExtra(EXTRA_REMINDER_ID, reminder.getId());
        intent.putExtra(EXTRA_REMINDER_TITLE, reminder.getTitulo());
        intent.putExtra(EXTRA_REMINDER_AMOUNT, reminder.getMonto());

        String due = null;
        if (reminder.getFechaVencimiento() != null) {
            java.text.SimpleDateFormat df = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US);
            due = df.format(reminder.getFechaVencimiento());
        }
        if (TextUtils.isEmpty(due)) {
            due = reminder.getFechaVencimiento() == null ? "" : reminder.getFechaVencimiento().toString();
        }
        intent.putExtra(EXTRA_REMINDER_DUE, due);

        int requestCode = reminder.getId();
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(context.getApplicationContext(), requestCode, intent, flags);
    }

    private static long computeTriggerAt(PaymentReminder reminder) {
        if (reminder.getFechaVencimiento() == null) return -1;
        Calendar cal = Calendar.getInstance();
        cal.setTime(reminder.getFechaVencimiento());

        String hora = reminder.getHoraRecordatorio();
        if (!TextUtils.isEmpty(hora)) {
            String[] parts = hora.split(":");
            try {
                int hour = parts.length > 0 ? Integer.parseInt(parts[0]) : 9;
                int minute = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
                cal.set(Calendar.HOUR_OF_DAY, hour);
                cal.set(Calendar.MINUTE, minute);
            } catch (NumberFormatException ignore) {
                cal.set(Calendar.HOUR_OF_DAY, 9);
                cal.set(Calendar.MINUTE, 0);
            }
        } else {
            cal.set(Calendar.HOUR_OF_DAY, 9);
            cal.set(Calendar.MINUTE, 0);
        }

        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        int dias = reminder.getDiasRecordatorio();
        if (dias > 0) {
            cal.add(Calendar.DAY_OF_YEAR, -dias);
        }
        return cal.getTimeInMillis();
    }
}
