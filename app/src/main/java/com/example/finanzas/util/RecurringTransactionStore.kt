package com.example.finanzas.util

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.finanzas.R
import com.example.finanzas.data.api.SettingsService
import com.example.finanzas.data.local.LocalDatabase
import com.example.finanzas.data.local.LocalRepository
import com.example.finanzas.data.local.room.RecurringTransactionEntity
import com.example.finanzas.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.random.Random

object RecurringTransactionStore {
    private const val PREFS = "finanzas_settings"
    private const val KEY_PREFIX = "recurring_transactions_user_"
    private const val CHANNEL_ID = "recurring_transactions"
    private const val REQUEST_CODE_OFFSET = 600_000

    const val FREQUENCY_WEEKDAYS = "WEEKDAYS"
    const val FREQUENCY_EVERYDAY = "EVERYDAY"
    const val FREQUENCY_WEEKLY = "WEEKLY"
    const val FREQUENCY_MONTHLY = "MONTHLY"
    const val FREQUENCY_CUSTOM = "CUSTOM"
    const val ACTION_RECURRING_TRANSACTION = "com.example.finanzas.action.RECURRING_TRANSACTION"

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
                    accountType = SettingsService.normalizeAccountType(accountType),
                    destinationAccountType = destinationAccountType?.let { SettingsService.normalizeAccountType(it) },
                    note = note.orEmpty(),
                    labelId = labelId?.takeIf { TransactionLabelStore.findLabel(appContext, it) != null },
                    firstDate = firstDate,
                    lastGeneratedDay = existing?.lastGeneratedDay ?: dayFormat.format(firstDate)
                )
            )
            dao.findBySource(userId, sourceTransactionId)?.let { scheduleNext(appContext, it) }
        }
    }

    @JvmStatic
    fun deleteTemplateForSource(context: Context, sourceTransactionId: Int) {
        val appContext = context.applicationContext
        scope.launch {
            val userId = Prefs.getCurrentUserId(appContext).toInt()
            if (userId <= 0 || sourceTransactionId <= 0) return@launch
            val dao = LocalDatabase.getInstance(appContext).room.recurringTransactionDao()
            dao.findBySource(userId, sourceTransactionId)?.let { cancelAlarm(appContext, it) }
            dao.deleteBySource(userId, sourceTransactionId)
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
    fun listTemplates(context: Context): List<RecurringTransactionEntity> {
        val appContext = context.applicationContext
        val userId = Prefs.getCurrentUserId(appContext).toInt()
        if (userId <= 0) return emptyList()
        migrateLegacyPrefs(appContext, userId)
        return LocalDatabase.getInstance(appContext).room.recurringTransactionDao().listAll(userId)
    }

    @JvmStatic
    fun setTemplateActive(context: Context, templateId: Int, active: Boolean): Boolean {
        val appContext = context.applicationContext
        val userId = Prefs.getCurrentUserId(appContext).toInt()
        if (userId <= 0 || templateId <= 0) return false
        val dao = LocalDatabase.getInstance(appContext).room.recurringTransactionDao()
        val item = dao.listAll(userId).firstOrNull { it.id == templateId } ?: return false
        val changed = dao.setActive(templateId, userId, if (active) 1 else 0) > 0
        if (changed) {
            if (active) {
                dao.listAll(userId).firstOrNull { it.id == templateId }?.let { scheduleNext(appContext, it) }
            } else {
                cancelAlarm(appContext, item)
            }
        }
        return changed
    }

    @JvmStatic
    fun deleteTemplate(context: Context, templateId: Int): Boolean {
        val appContext = context.applicationContext
        val userId = Prefs.getCurrentUserId(appContext).toInt()
        if (userId <= 0 || templateId <= 0) return false
        val item = LocalDatabase.getInstance(appContext).room.recurringTransactionDao()
            .listAll(userId)
            .firstOrNull { it.id == templateId } ?: return false
        cancelAlarm(appContext, item)
        return LocalDatabase.getInstance(appContext).room.recurringTransactionDao()
            .deleteBySource(userId, item.sourceTransactionId) > 0
    }

    @JvmStatic
    fun nextTriggerAtMillis(item: RecurringTransactionEntity): Long? = nextTriggerAt(item)

    @JvmStatic
    fun findTemplate(context: Context, templateId: Int): RecurringTransactionEntity? {
        val appContext = context.applicationContext
        val userId = Prefs.getCurrentUserId(appContext).toInt()
        if (userId <= 0 || templateId <= 0) return null
        return LocalDatabase.getInstance(appContext).room.recurringTransactionDao()
            .listAll(userId)
            .firstOrNull { it.id == templateId }
    }

    @JvmStatic
    fun saveRule(
        context: Context,
        templateId: Int?,
        sourceTransactionId: Int?,
        frequency: String?,
        daysMask: Int,
        isActive: Boolean,
        isTransfer: Boolean,
        categoryId: Int,
        isIncome: Boolean,
        amount: Double,
        note: String?,
        firstDate: Long,
        currency: String,
        accountType: String,
        destinationAccountType: String?
    ): Boolean {
        val cleanFrequency = normalizeFrequency(frequency) ?: return false
        val cleanDaysMask = normalizeDaysMask(cleanFrequency, daysMask)
        if (cleanFrequency == FREQUENCY_CUSTOM && cleanDaysMask == 0) return false
        val appContext = context.applicationContext
        val userId = Prefs.getCurrentUserId(appContext).toInt()
        if (userId <= 0 || amount <= 0.0) return false
        val dao = LocalDatabase.getInstance(appContext).room.recurringTransactionDao()
        val existing = templateId?.takeIf { it > 0 }?.let { id ->
            dao.listAll(userId).firstOrNull { it.id == id }
        }
        // Positive source ids belong to real transactions. Negative source ids are standalone
        // rules created from Settings, so transaction lookup flows intentionally ignore them.
        val sourceId = existing?.sourceTransactionId
            ?: sourceTransactionId?.takeIf { it != 0 }
            ?: generateStandaloneSourceId(userId, dao.listStandaloneSourceIds().toSet())
        val entity = RecurringTransactionEntity(
            id = existing?.id ?: 0,
            userId = userId,
            sourceTransactionId = sourceId,
            frequency = cleanFrequency,
            daysMask = cleanDaysMask,
            isActive = if (isActive) 1 else 0,
            isTransfer = if (isTransfer) 1 else 0,
            categoryId = if (isTransfer) 0 else categoryId,
            isIncome = if (isIncome) 1 else 0,
            amount = amount,
            currency = CurrencyConverter.normalize(currency),
            accountType = SettingsService.normalizeAccountType(accountType),
            destinationAccountType = if (isTransfer) {
                SettingsService.normalizeAccountType(destinationAccountType ?: "CASH")
            } else {
                null
            },
            note = note.orEmpty(),
            labelId = existing?.labelId,
            firstDate = firstDate,
            lastGeneratedDay = existing?.lastGeneratedDay
        )
        val id = dao.upsert(entity)
        val saved = if (id > 0) dao.listAll(userId).firstOrNull { it.id == id.toInt() } else dao.findBySource(userId, sourceId)
        if (saved != null) {
            if (saved.isActive == 1) scheduleNext(appContext, saved) else cancelAlarm(appContext, saved)
        }
        return true
    }

    private fun generateStandaloneSourceId(userId: Int, usedSourceIds: Set<Int>): Int {
        val bucket = (userId.coerceAtLeast(1).toLong() % 200_000L).let { if (it == 0L) 200_000L else it }
        val firstCandidate = -((bucket * 10_000L) + 1L)
        repeat(9_999) { offset ->
            val candidate = (firstCandidate - offset).toInt()
            if (candidate < 0 && candidate !in usedSourceIds) return candidate
        }
        repeat(12) {
            val candidate = -Random.nextInt(1_000_000, Int.MAX_VALUE)
            if (candidate !in usedSourceIds) return candidate
        }
        val minUsed = usedSourceIds.minOrNull() ?: 0
        return if (minUsed > Int.MIN_VALUE) minUsed.coerceAtMost(0) - 1 else -1
    }

    @JvmStatic
    fun processDueAsync(context: Context) {
        processDueAndScheduleAsync(context, null)
    }

    fun processDueAndScheduleAsync(context: Context, onComplete: (() -> Unit)?) {
        val appContext = context.applicationContext
        scope.launch {
            runCatching { processDue(appContext) }
            runCatching { scheduleAllActive(appContext) }
            onComplete?.invoke()
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
            val occurrenceTime = occurrenceTimeForToday(item, today) ?: today.timeInMillis
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
                    occurrenceTime,
                    item.currency
                )
            } else {
                if (item.categoryId <= 0) continue
                repo.createTransaccion(
                    item.categoryId,
                    item.isIncome == 1,
                    item.amount,
                    note,
                    occurrenceTime,
                    item.currency,
                    item.accountType
                )
            }
            if (!item.labelId.isNullOrBlank() && TransactionLabelStore.findLabel(context, item.labelId) != null) {
                TransactionLabelStore.setLabel(context, newId, item.labelId)
            }
            dao.updateLastGeneratedDay(item.id, userId, todayDay)
            notifyGenerated(context, item, newId)
        }
    }

    private fun isDueToday(item: RecurringTransactionEntity, today: Calendar, todayDay: String): Boolean {
        if (item.lastGeneratedDay == todayDay) return false
        if (item.firstDate > today.timeInMillis) return false
        val occurrenceTime = occurrenceTimeForToday(item, today) ?: return false
        if (today.timeInMillis < occurrenceTime) return false
        return matchesDay(item, today)
    }

    private fun occurrenceTimeForToday(item: RecurringTransactionEntity, today: Calendar): Long? {
        if (!matchesDay(item, today)) return null
        val first = Calendar.getInstance().apply { timeInMillis = item.firstDate }
        return Calendar.getInstance().apply {
            timeInMillis = today.timeInMillis
            set(Calendar.HOUR_OF_DAY, first.get(Calendar.HOUR_OF_DAY))
            set(Calendar.MINUTE, first.get(Calendar.MINUTE))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun matchesDay(item: RecurringTransactionEntity, calendar: Calendar): Boolean {
        return when (item.frequency) {
            FREQUENCY_WEEKDAYS -> {
                val day = calendar.get(Calendar.DAY_OF_WEEK)
                day != Calendar.SATURDAY && day != Calendar.SUNDAY
            }
            FREQUENCY_EVERYDAY -> true
            FREQUENCY_WEEKLY -> {
                val first = Calendar.getInstance().apply { timeInMillis = item.firstDate }
                calendar.get(Calendar.DAY_OF_WEEK) == first.get(Calendar.DAY_OF_WEEK)
            }
            FREQUENCY_MONTHLY -> {
                val first = Calendar.getInstance().apply { timeInMillis = item.firstDate }
                calendar.get(Calendar.DAY_OF_MONTH) == monthlyTriggerDay(first, calendar)
            }
            FREQUENCY_CUSTOM -> (item.daysMask and bitForCalendarDay(calendar.get(Calendar.DAY_OF_WEEK))) != 0
            else -> false
        }
    }

    private fun monthlyTriggerDay(first: Calendar, targetMonth: Calendar): Int {
        return first.get(Calendar.DAY_OF_MONTH).coerceAtMost(targetMonth.getActualMaximum(Calendar.DAY_OF_MONTH))
    }

    private fun scheduleAllActive(context: Context) {
        val userId = Prefs.getCurrentUserId(context).toInt()
        if (userId <= 0) return
        val dao = LocalDatabase.getInstance(context).room.recurringTransactionDao()
        dao.listActive(userId).forEach { scheduleNext(context, it) }
    }

    private fun scheduleNext(context: Context, item: RecurringTransactionEntity) {
        if (item.id <= 0 || item.isActive != 1) return
        val triggerAt = nextTriggerAt(item) ?: run {
            cancelAlarm(context, item)
            return
        }
        val pi = buildPendingIntent(context, item) ?: return
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarm.canScheduleExactAlarms()) {
                    alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
                } else {
                    alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
                }
            } else {
                alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            }
        } catch (_: SecurityException) {
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    private fun cancelAlarm(context: Context, item: RecurringTransactionEntity) {
        val pi = buildPendingIntent(context, item) ?: return
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        alarm?.cancel(pi)
        pi.cancel()
    }

    private fun buildPendingIntent(context: Context, item: RecurringTransactionEntity): PendingIntent? {
        if (item.id <= 0) return null
        val intent = Intent(context.applicationContext, RecurringTransactionReceiver::class.java).apply {
            action = ACTION_RECURRING_TRANSACTION
            putExtra("template_id", item.id)
        }
        return PendingIntent.getBroadcast(
            context.applicationContext,
            REQUEST_CODE_OFFSET + item.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun nextTriggerAt(item: RecurringTransactionEntity, nowMillis: Long = System.currentTimeMillis()): Long? {
        val first = Calendar.getInstance().apply { timeInMillis = item.firstDate }
        val candidate = Calendar.getInstance().apply {
            timeInMillis = maxOf(nowMillis, item.firstDate)
            set(Calendar.HOUR_OF_DAY, first.get(Calendar.HOUR_OF_DAY))
            set(Calendar.MINUTE, first.get(Calendar.MINUTE))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (candidate.timeInMillis <= nowMillis) {
            candidate.add(Calendar.DAY_OF_YEAR, 1)
        }
        repeat(370) {
            if (candidate.timeInMillis >= item.firstDate && matchesDay(item, candidate)) {
                return candidate.timeInMillis
            }
            candidate.add(Calendar.DAY_OF_YEAR, 1)
        }
        return null
    }

    private fun notifyGenerated(context: Context, item: RecurringTransactionEntity, transactionId: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        ensureChannel(context)
        val amount = Format.money(item.amount, item.currency)
        val content = when {
            item.isTransfer == 1 -> context.getString(R.string.recurring_transaction_notification_transfer, amount)
            item.isIncome == 1 -> context.getString(R.string.recurring_transaction_notification_income, amount)
            else -> context.getString(R.string.recurring_transaction_notification_expense, amount)
        }
        val openIntent = Intent(context, MainActivity::class.java)
        val contentIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE_OFFSET + transactionId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_list_24)
            .setContentTitle(context.getString(R.string.recurring_transaction_notification_title))
            .setContentText(content)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(REQUEST_CODE_OFFSET + transactionId, notification)
        } catch (_: SecurityException) {
        }
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.recurring_transaction_channel),
            NotificationManager.IMPORTANCE_HIGH
        )
        nm.createNotificationChannel(channel)
    }

    private fun normalizeFrequency(frequency: String?): String? {
        return when (frequency?.trim()?.uppercase(Locale.ROOT)) {
            FREQUENCY_WEEKDAYS -> FREQUENCY_WEEKDAYS
            "DAILY", FREQUENCY_EVERYDAY -> FREQUENCY_EVERYDAY
            "SEMANAL", FREQUENCY_WEEKLY -> FREQUENCY_WEEKLY
            "MENSUAL", FREQUENCY_MONTHLY -> FREQUENCY_MONTHLY
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

    internal fun matchesFrequencyOnDate(frequency: String, firstDate: Long, daysMask: Int, targetDate: Long): Boolean {
        val normalizedFrequency = normalizeFrequency(frequency) ?: return false
        val item = RecurringTransactionEntity(
            id = 1,
            userId = 1,
            sourceTransactionId = 1,
            frequency = normalizedFrequency,
            daysMask = normalizeDaysMask(normalizedFrequency, daysMask),
            isActive = 1,
            isTransfer = 0,
            categoryId = 1,
            isIncome = 0,
            amount = 1.0,
            currency = "PEN",
            accountType = "CARD",
            destinationAccountType = null,
            note = "",
            labelId = null,
            firstDate = firstDate,
            lastGeneratedDay = null
        )
        val target = Calendar.getInstance().apply { timeInMillis = targetDate }
        return matchesDay(item, target)
    }

    @JvmStatic
    fun bitForCalendarDay(dayOfWeek: Int): Int = 1 shl (dayOfWeek - Calendar.SUNDAY)

    internal fun isStandaloneRuleSourceId(sourceTransactionId: Int): Boolean = sourceTransactionId < 0

    internal fun isRealTransactionSourceId(sourceTransactionId: Int): Boolean = sourceTransactionId > 0

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
