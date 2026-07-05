package com.example.finanzas.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class RecurringTransactionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) return
        val action = intent?.action.orEmpty()
        if (action.isBlank() ||
            action != RecurringTransactionStore.ACTION_RECURRING_TRANSACTION &&
            action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }
        val pendingResult = goAsync()
        RecurringTransactionStore.processDueAndScheduleAsync(context.applicationContext) {
            pendingResult.finish()
        }
    }
}
