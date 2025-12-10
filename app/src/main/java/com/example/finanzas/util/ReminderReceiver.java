package com.example.finanzas.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.text.TextUtils;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.finanzas.R;
import com.example.finanzas.ui.MainActivity;

public class ReminderReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "reminders";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) return;
        ensureChannel(context);
        int reminderId = intent.getIntExtra(ReminderScheduler.EXTRA_REMINDER_ID, 0);
        String title = intent.getStringExtra(ReminderScheduler.EXTRA_REMINDER_TITLE);
        double amount = intent.getDoubleExtra(ReminderScheduler.EXTRA_REMINDER_AMOUNT, 0);
        String due = intent.getStringExtra(ReminderScheduler.EXTRA_REMINDER_DUE);
        if (TextUtils.isEmpty(title)) {
            title = context.getString(R.string.reminder_dialog_title_new);
        }
        String amountText = context.getString(R.string.reminder_amount, com.example.finanzas.util.Format.money(amount));
        String dueText = due == null ? "" : due;
        StringBuilder content = new StringBuilder(amountText);
        if (!TextUtils.isEmpty(dueText)) {
            content.append(" • ").append(dueText);
        }

        Intent openIntent = new Intent(context, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                reminderId,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_list_24)
                .setContentTitle(title)
                .setContentText(content.toString())
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManagerCompat.from(context).notify(reminderId, builder.build());
    }

    private void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm == null) return;
        NotificationChannel existing = nm.getNotificationChannel(CHANNEL_ID);
        if (existing != null) return;
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.reminder_dialog_title_new),
                NotificationManager.IMPORTANCE_HIGH
        );
        nm.createNotificationChannel(channel);
    }
}
