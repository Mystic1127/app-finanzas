package com.example.finanzas.util

import android.content.Context
import com.example.finanzas.data.local.LocalDatabase
import com.example.finanzas.data.local.LocalRepository
import com.example.finanzas.data.local.room.RecurringTransactionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object RecurringTransactionStore {
    private const val PREFS = "finanzas_settings"
    private const val KEY_PREFIX = "recurring_transactions_user_"
    const val FREQUENCY_WEEKDAYS = "WEEKDAYS"
    const val FREQUENCY_EVERYDAY = "EVERYDAY"
    const val FREQUENCY_CUSTOM = "CUSTOM"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    @JvmStatic
    fun saveTemplate(
        context: Context,
        sourceTransactionId: Int,
        frequency: String?,
        daysMask: Int,
        isTransfer: Boolean,
        categoryId: Int,
        isIncome: Boolean,
        amount: Double,
        note: String?,
        firstDate: Long,
        currency: String,
        accountType: String,
        destinationAccountType: String?,
        labelId: String?
    ) {
        val cleanFrequency = normalizeFrequency(frequency) ?: return
        val cleanDaysMask = normalizeDaysMask(cleanFrequency, daysMask)
        if (cleanFrequency == FREQUENCY_CUSTOM && cleanDaysMask == 0) return
        val appContext = context.applicationContext
        scope.launch {
            val userId = Prefs.getCurrentUserId(appContext).toInt()
            if (userId <= 0 || sourceTransactionId <= 0) return@launch
            val dao = LocalDatabase.getInstance(appContext).room.recurringTransactionDao()
            val existing = dao.findBySource(userId, sourceTransactionId)
            dao.upsert(
                RecurringTransactionEntity(
                    id = existing?.id ?: 0,
                    userId = userId,
                    sourceTransactionId = sourceTransactionId,
                    frequency = cleanFrequency,
                    daysMask = cleanDaysMask,
                    isActive = 1,
                    isTransfer = if (isTransfer) 1 else 0,
                    categoryId = categoryId,
                    isIncome = if (isIncome) 1 else 0,
                    amount = amount,
                    currency = CurrencyConverter.normalize(currency),
                    accountType = com.example.finanzas.data.api.SettingsService.normalizeAccountType(accountType),
                    destinationAccountType = destinationAccountType?.let {
                        com.example.finanzas.data.api.SettingsService.normalizeAccountType(it)
                    },
                    note = note.orEmpty(),
                    labelId = labelId?.takeIf { TransactionLabelStore.findLabel(appContext, it) != null },
                    firstDate = firstDate,
                    lastGeneratedDay = existing?.lastGeneratedDay ?: dayFormat.format(firstDate)
                )
            )
        }
    }

    @JvmStatic
    fun deleteTemplateForSource(context: Context, sourceTransactionId: Int) {
        val appContext = context.applicationContext
        scope.launch {
            val userId = Prefs.getCurrentUserId(appContext).toInt()
            if (userId <= 0 || sourceTransactionId <= 0) return@launch
            LocalDatabase.getInstance(appContext).room.recurringTransactionDao().deleteBySource(userId, sourceTransactionId)
        }
    }

    @JvmStatic
    fun findTemplateForSource(context: Context, sourceTransactionId: Int): RecurringTransactionEntity? {
        val appContext = context.applicationContext
        val userId = Prefs.getCurrentUserId(appContext).toInt()
        if (userId <= 0 || sourceTransactionId <= 0) return null
        return LocalDatabase.getInstance(appContext).room.recurringTransactionDao().findBySource(userId, sourceTransactionId)
    }

    @JvmStatic
    fun processDueAsync(context: Context) {
        val appContext = context.applicationContext
        scope.launch {
            runCatching { processDue(appContext) }
        }
    }

    private suspend fun processDue(context: Context) {
        val userId = Prefs.getCurrentUserId(context).toInt()
        if (userId <= 0) return
        migrateLegacyPrefs(context, userId)
        val db = LocalDatabase.getInstance(context).room
        val dao = db.recurringTransactionDao()
        val repo = LocalRepository.getInstance(context)
        val today = Calendar.getInstance()
        val todayDay = dayFormat.format(today.time)

        for (item in dao.listActive(userId)) {
            if (!isDueToday(item, today, todayDay)) continue
            val marker = "[recurrent:${item.id}:$todayDay]"
            if (dao.countGeneratedMarker(userId, marker) > 0) {
                dao.updateLastGeneratedDay(item.id, userId, todayDay)
                continue
            }

            val note = listOf(marker, item.note.orEmpty()).filter { it.isNotBlank() }.joinToString(" ")
            val newId = if (item.isTransfer == 1) {
                repo.createTransfer(
                    item.accountType,
                    item.destinationAccountType ?: "CASH",
                    item.amount,
                    note,
                    today.timeInMillis,
                    item.currency
                )
            } else {
                if (item.categoryId <= 0) continue
                repo.createTransaccion(
                    item.categoryId,
                    item.isIncome == 1,
                    item.amount,
                    note,
                    today.timeInMillis,
                    item.currency,
                    item.accountType
                )
            }
            if (!item.labelId.isNullOrBlank() && TransactionLabelStore.findLabel(context, item.labelId) != null) {
                TransactionLabelStore.setLabel(context, newId, item.labelId)
            }
            dao.updateLastGeneratedDay(item.id, userId, todayDay)
        }
    }

    private fun isDueToday(item: RecurringTransactionEntity, today: Calendar, todayDay: String): Boolean {
        if (item.lastGeneratedDay == todayDay) return false
        if (item.firstDate > today.timeInMillis) return false
        return when (item.frequency) {
            FREQUENCY_WEEKDAYS -> {
                val day = today.get(Calendar.DAY_OF_WEEK)
                day != Calendar.SATURDAY && day != Calendar.SUNDAY
            }
            FREQUENCY_EVERYDAY -> true
            FREQUENCY_CUSTOM -> (item.daysMask and bitForCalendarDay(today.get(Calendar.DAY_OF_WEEK))) != 0
            else -> false
        }
    }

    private fun normalizeFrequency(frequency: String?): String? {
        return when (frequency?.trim()?.uppercase(Locale.ROOT)) {
            FREQUENCY_WEEKDAYS -> FREQUENCY_WEEKDAYS
            "DAILY", FREQUENCY_EVERYDAY -> FREQUENCY_EVERYDAY
            FREQUENCY_CUSTOM -> FREQUENCY_CUSTOM
            else -> null
        }
    }

    private fun normalizeDaysMask(frequency: String, daysMask: Int): Int {
        return when (frequency) {
            FREQUENCY_WEEKDAYS -> listOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY)
                .fold(0) { acc, day -> acc or bitForCalendarDay(day) }
            FREQUENCY_EVERYDAY -> (0..6).fold(0) { acc, offset -> acc or (1 shl offset) }
            else -> daysMask and 0x7F
        }
    }

    @JvmStatic
    fun bitForCalendarDay(dayOfWeek: Int): Int = 1 shl (dayOfWeek - Calendar.SUNDAY)

    private fun migrateLegacyPrefs(context: Context, userId: Int) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val key = KEY_PREFIX + userId
        val raw = prefs.getString(key, "[]") ?: "[]"
        if (raw == "[]") return
        val dao = LocalDatabase.getInstance(context).room.recurringTransactionDao()
        runCatching {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val sourceId = item.optInt("source_id", 0)
                if (sourceId <= 0 || dao.findBySource(userId, sourceId) != null) continue
                val frequency = normalizeFrequency(item.optString("frequency", "")) ?: continue
                val firstDate = item.optLong("first_date", 0L).takeIf { it > 0L } ?: continue
                dao.upsert(
                    RecurringTransactionEntity(
                        userId = userId,
                        sourceTransactionId = sourceId,
                        frequency = frequency,
                        daysMask = normalizeDaysMask(frequency, 0),
                        isActive = if (item.optBoolean("paused", false)) 0 else 1,
                        isTransfer = if (item.optBoolean("is_transfer", false)) 1 else 0,
                        categoryId = item.optInt("category_id", 0),
                        isIncome = if (item.optBoolean("is_income", false)) 1 else 0,
                        amount = item.optDouble("amount", 0.0),
                        currency = CurrencyConverter.normalize(item.optString("currency", "PEN")),
                        accountType = item.optString("account_type", "CARD"),
                        destinationAccountType = item.optString("destination_account_type", "").takeIf { it.isNotBlank() },
                        note = item.optString("note", ""),
                        labelId = null,
                        firstDate = firstDate,
                        lastGeneratedDay = item.optString("last_generated_day", dayFormat.format(firstDate))
                    )
                )
            }
            prefs.edit().remove(key).apply()
        }
    }
}
