package com.example.finanzas.data.repository

import android.content.Context
import com.example.finanzas.data.api.ReminderService
import com.example.finanzas.data.model.PaymentReminder

class ReminderRepository(private val context: Context) {
    suspend fun list(includePaid: Boolean): List<PaymentReminder> = ReminderService.list(context, includePaid)
}
