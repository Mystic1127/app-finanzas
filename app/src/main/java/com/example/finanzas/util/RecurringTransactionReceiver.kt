package com.example.finanzas.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class RecurringTransactionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) return
        val pendingResult = goAsync()
        RecurringTransactionStore.processDueAndScheduleAsync(context.applicationContext) {
            pendingResult.finish()
        }
    }
}
