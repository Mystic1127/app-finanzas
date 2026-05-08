package com.example.finanzas.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64

object Prefs {
    @Volatile
    private var cachedPrefs: SharedPreferences? = null

    private const val PREFS_NAME = "finanzas_secure_prefs"
    private const val KEY_PRES = "presupuesto_mensual"
    private const val KEY_TOKEN = "auth_token"
    private const val KEY_TOKEN_ISSUED_AT = "auth_token_issued_at"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_PIN_HASH_PREFIX = "pin_hash_"
    private const val KEY_PIN_ENABLED_PREFIX = "pin_enabled_"
    private const val KEY_TRANS_YEAR_PREFIX = "trans_period_year_"
    private const val KEY_TRANS_MONTH_PREFIX = "trans_period_month_"
    private const val KEY_TESTER_THANKS_PREFIX = "tester_thanks_seen_"

    private const val TOKEN_MAX_AGE_MS = 30L * 24 * 60 * 60 * 1000 // 30 días

    @JvmStatic
    fun setPresupuesto(ctx: Context, value: Double) {
        prefs(ctx).edit().putFloat(KEY_PRES, value.toFloat()).apply()
    }

    @JvmStatic
    fun getPresupuesto(ctx: Context): Double {
        return prefs(ctx).getFloat(KEY_PRES, 0f).toDouble()
    }

    @JvmStatic
    fun setToken(ctx: Context, token: String?) {
        val editor = prefs(ctx).edit()
        if (token.isNullOrBlank()) {
            editor.remove(KEY_TOKEN).remove(KEY_TOKEN_ISSUED_AT).apply()
            return
        }
        editor.putString(KEY_TOKEN, token)
            .putLong(KEY_TOKEN_ISSUED_AT, System.currentTimeMillis())
            .apply()
    }

    @JvmStatic
    fun getToken(ctx: Context): String? {
        val sp = prefs(ctx)
        val token = sp.getString(KEY_TOKEN, null) ?: return null
        val issuedAt = sp.getLong(KEY_TOKEN_ISSUED_AT, 0L)
        if (issuedAt <= 0L) {
            return token
        }
        if ((System.currentTimeMillis() - issuedAt) > TOKEN_MAX_AGE_MS) {
            sp.edit().remove(KEY_TOKEN).remove(KEY_TOKEN_ISSUED_AT).apply()
            return null
        }
        return token
    }

    @JvmStatic
    fun setUserSession(ctx: Context, userId: Long, email: String?, name: String?) {
        prefs(ctx).edit()
            .putLong(KEY_USER_ID, if (userId > 0) userId else -1L)
            .putString(KEY_USER_EMAIL, email ?: "")
            .putString(KEY_USER_NAME, name ?: "")
            .apply()
    }

    @JvmStatic
    fun isLoggedIn(ctx: Context): Boolean {
        return getToken(ctx) != null || getCurrentUserId(ctx) > 0
    }

    @JvmStatic
    fun getCurrentUserId(ctx: Context): Long {
        return prefs(ctx).getLong(KEY_USER_ID, -1L)
    }

    @JvmStatic
    fun getCurrentUserEmail(ctx: Context): String {
        return prefs(ctx).getString(KEY_USER_EMAIL, "") ?: ""
    }

    @JvmStatic
    fun getCurrentUserName(ctx: Context): String {
        return prefs(ctx).getString(KEY_USER_NAME, "") ?: ""
    }

    @JvmStatic
    fun clearUserSession(ctx: Context) {
        prefs(ctx).edit()
            .remove(KEY_USER_ID)
            .remove(KEY_USER_EMAIL)
            .remove(KEY_USER_NAME)
            .apply()
    }

    @JvmStatic
    fun savePin(ctx: Context, pin: String?) {
        if (pin == null) return
        val userId = getCurrentUserId(ctx)
        if (userId <= 0) return

        prefs(ctx).edit()
            .putString(pinHashKey(userId), hashPin(pin))
            .putBoolean(pinEnabledKey(userId), true)
            .apply()
    }

    @JvmStatic
    fun hasPin(ctx: Context): Boolean {
        val userId = getCurrentUserId(ctx)
        if (userId <= 0) return false
        val sp = prefs(ctx)
        return sp.getBoolean(pinEnabledKey(userId), false) && sp.contains(pinHashKey(userId))
    }

    @JvmStatic
    fun verifyPin(ctx: Context, pin: String?): Boolean {
        if (pin == null) return false
        val userId = getCurrentUserId(ctx)
        if (userId <= 0) return false
        val stored = prefs(ctx).getString(pinHashKey(userId), null) ?: return false
        return stored == hashPin(pin)
    }

    @JvmStatic
    fun clearPin(ctx: Context) {
        val userId = getCurrentUserId(ctx)
        if (userId <= 0) return
        prefs(ctx).edit()
            .remove(pinHashKey(userId))
            .putBoolean(pinEnabledKey(userId), false)
            .apply()
    }

    @JvmStatic
    fun clearAuth(ctx: Context) {
        prefs(ctx).edit()
            .remove(KEY_TOKEN)
            .remove(KEY_TOKEN_ISSUED_AT)
            .remove(KEY_USER_ID)
            .remove(KEY_USER_EMAIL)
            .remove(KEY_USER_NAME)
            .apply()
    }

    @JvmStatic
    fun setLastTransactionsPeriod(ctx: Context, year: Int, month: Int) {
        val userId = getCurrentUserId(ctx)
        if (userId <= 0) return
        prefs(ctx).edit()
            .putInt(KEY_TRANS_YEAR_PREFIX + userId, year)
            .putInt(KEY_TRANS_MONTH_PREFIX + userId, month)
            .apply()
    }

    @JvmStatic
    fun getLastTransactionsYear(ctx: Context): Int {
        val userId = getCurrentUserId(ctx)
        if (userId <= 0) return -1
        return prefs(ctx).getInt(KEY_TRANS_YEAR_PREFIX + userId, -1)
    }

    @JvmStatic
    fun getLastTransactionsMonth(ctx: Context): Int {
        val userId = getCurrentUserId(ctx)
        if (userId <= 0) return -1
        return prefs(ctx).getInt(KEY_TRANS_MONTH_PREFIX + userId, -1)
    }

    @JvmStatic
    fun clearLastTransactionsPeriod(ctx: Context) {
        val userId = getCurrentUserId(ctx)
        if (userId <= 0) return
        prefs(ctx).edit()
            .remove(KEY_TRANS_YEAR_PREFIX + userId)
            .remove(KEY_TRANS_MONTH_PREFIX + userId)
            .apply()
    }

    @JvmStatic
    fun hasSeenTesterThanks(ctx: Context): Boolean {
        val userId = getCurrentUserId(ctx)
        if (userId <= 0) return true
        return prefs(ctx).getBoolean(KEY_TESTER_THANKS_PREFIX + userId, false)
    }

    @JvmStatic
    fun markTesterThanksSeen(ctx: Context) {
        val userId = getCurrentUserId(ctx)
        if (userId <= 0) return
        prefs(ctx).edit()
            .putBoolean(KEY_TESTER_THANKS_PREFIX + userId, true)
            .apply()
    }

    private fun hashPin(pin: String): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(pin.toByteArray(StandardCharsets.UTF_8))
            Base64.getEncoder().encodeToString(digest)
        } catch (_: Exception) {
            pin
        }
    }

    private fun pinHashKey(userId: Long): String = KEY_PIN_HASH_PREFIX + userId

    private fun pinEnabledKey(userId: Long): String = KEY_PIN_ENABLED_PREFIX + userId

    private fun prefs(ctx: Context): SharedPreferences {
        cachedPrefs?.let { return it }
        return synchronized(this) {
            cachedPrefs ?: run {
                val appContext = ctx.applicationContext
                try {
                    val masterKey = MasterKey.Builder(appContext)
                        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                        .build()

                    EncryptedSharedPreferences.create(
                        appContext,
                        PREFS_NAME,
                        masterKey,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                    )
                } catch (_: Exception) {
                    appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                }.also { cachedPrefs = it }
            }
        }
    }
}
