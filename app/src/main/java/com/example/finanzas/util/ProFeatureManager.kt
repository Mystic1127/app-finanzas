package com.example.finanzas.util

import android.content.Context

object ProFeatureManager {
    private const val PREFS = "finanzas_pro_features"
    private const val KEY_PRO_PREFIX = "is_pro_user_"

    @JvmStatic
    fun isProUser(context: Context): Boolean {
        val userId = Prefs.getCurrentUserId(context.applicationContext)
        if (userId <= 0L) return false
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_PRO_PREFIX + userId, false)
    }

    @JvmStatic
    fun setProUser(context: Context, enabled: Boolean) {
        val userId = Prefs.getCurrentUserId(context.applicationContext)
        if (userId <= 0L) return
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_PRO_PREFIX + userId, enabled)
            .apply()
    }
}
