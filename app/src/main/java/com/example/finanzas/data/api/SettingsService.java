package com.example.finanzas.data.api;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.finanzas.util.Prefs;

import org.json.JSONObject;

import java.util.Locale;

public class SettingsService {

    public interface SaveCb { void onSuccess(); void onFail(); }

    private static final String PREFS = "finanzas_settings";
    private static final String KEY_DASHBOARD_LEGACY = "dashboard_prefs";
    private static final String KEY_TRAVEL_LEGACY = "travel_prefs";
    private static final String KEY_DASHBOARD_PREFIX = "dashboard_prefs_user_";
    private static final String KEY_TRAVEL_PREFIX = "travel_prefs_user_";

    public static void save(Context ctx, boolean notificationsEnabled, SaveCb cb) {
        cb.onSuccess();
    }

    public static String getDashboardRaw(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return getRawForUser(sp, dashboardKey(currentUserId(ctx)), KEY_DASHBOARD_LEGACY);
    }

    public static String getTravelRaw(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return getRawForUser(sp, travelKey(currentUserId(ctx)), KEY_TRAVEL_LEGACY);
    }

    public static String getCurrencyCode(Context ctx) {
        try {
            JSONObject body = new JSONObject(getTravelRaw(ctx));
            String code = body.optString("currency", body.optString("base", "PEN"));
            return normalizeCurrency(code);
        } catch (Exception e) {
            return "PEN";
        }
    }

    public static String getCurrencySymbol(Context ctx) {
        String code = getCurrencyCode(ctx);
        if ("USD".equals(code)) return "$";
        if ("EUR".equals(code)) return "€";
        return "S/";
    }

    public static double getManualRate(Context ctx) {
        try {
            JSONObject body = new JSONObject(getTravelRaw(ctx));
            return Math.max(0.0, body.optDouble("rate", 0.0));
        } catch (Exception e) {
            return 0.0;
        }
    }

    public static void saveDashboard(Context ctx, JSONObject body, SaveCb cb) {
        try {
            SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            sp.edit().putString(dashboardKey(currentUserId(ctx)), body != null ? body.toString() : "{}").apply();
            cb.onSuccess();
        } catch (Exception e) {
            cb.onFail();
        }
    }

    public static void saveTravel(Context ctx, JSONObject body, SaveCb cb) {
        try {
            SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            sp.edit().putString(travelKey(currentUserId(ctx)), body != null ? body.toString() : "{}").apply();
            cb.onSuccess();
        } catch (Exception e) {
            cb.onFail();
        }
    }

    public static void saveCurrency(Context ctx, String currencyCode, double manualRate, SaveCb cb) {
        try {
            JSONObject body;
            try {
                body = new JSONObject(getTravelRaw(ctx));
            } catch (Exception e) {
                body = new JSONObject();
            }
            String code = normalizeCurrency(currencyCode);
            body.put("enabled", false);
            body.put("base", code);
            body.put("currency", code);
            body.put("rate", Math.max(0.0, manualRate));
            saveTravel(ctx, body, cb);
        } catch (Exception e) {
            cb.onFail();
        }
    }

    private static long currentUserId(Context ctx) {
        long userId = Prefs.getCurrentUserId(ctx.getApplicationContext());
        if (userId <= 0) {
            throw new IllegalStateException("No hay usuario autenticado");
        }
        return userId;
    }

    private static String dashboardKey(long userId) {
        return KEY_DASHBOARD_PREFIX + userId;
    }

    private static String travelKey(long userId) {
        return KEY_TRAVEL_PREFIX + userId;
    }

    private static String getRawForUser(SharedPreferences sp, String userKey, String legacyKey) {
        String userValue = sp.getString(userKey, null);
        if (userValue != null) {
            return userValue;
        }

        String legacyValue = sp.getString(legacyKey, null);
        if (legacyValue != null) {
            sp.edit().putString(userKey, legacyValue).apply();
            return legacyValue;
        }

        return "{}";
    }

    private static String normalizeCurrency(String code) {
        String safe = code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
        if ("USD".equals(safe) || "EUR".equals(safe) || "PEN".equals(safe)) {
            return safe;
        }
        return "PEN";
    }
}
