package com.example.finanzas.data.api;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

import com.example.finanzas.data.local.LocalRepository;
import com.example.finanzas.util.CurrencyConverter;
import com.example.finanzas.util.Prefs;

import org.json.JSONObject;

public class SettingsService {

    public interface SaveCb { void onSuccess(); void onFail(); }

    private static final String PREFS = "finanzas_settings";
    private static final String KEY_DASHBOARD_LEGACY = "dashboard_prefs";
    private static final String KEY_TRAVEL_LEGACY = "travel_prefs";
    private static final String KEY_DASHBOARD_PREFIX = "dashboard_prefs_user_";
    private static final String KEY_TRAVEL_PREFIX = "travel_prefs_user_";
    private static final String KEY_INITIAL_BALANCES_PREFIX = "initial_balances_user_";
    private static final String KEY_THEME_MODE = "theme_mode";
    public static final String THEME_SYSTEM = "system";
    public static final String THEME_LIGHT = "light";
    public static final String THEME_DARK = "dark";

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
        return CurrencyConverter.symbol(getCurrencyCode(ctx));
    }

    public static String getCurrencySymbol(String currencyCode) {
        return CurrencyConverter.symbol(currencyCode);
    }

    public static boolean hasCurrencyConfigured(Context ctx) {
        try {
            SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            String raw = getRawForUser(sp, travelKey(currentUserId(ctx)), KEY_TRAVEL_LEGACY);
            JSONObject body = new JSONObject(raw);
            String code = body.optString("currency", body.optString("base", ""));
            return !code.trim().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    public static double getManualRate(Context ctx) {
        try {
            JSONObject body = new JSONObject(getTravelRaw(ctx));
            return Math.max(0.0, body.optDouble("rate", 0.0));
        } catch (Exception e) {
            return 0.0;
        }
    }

    public static double getInitialCashBalance(Context ctx) {
        return getInitialBalanceRaw(ctx, "cash");
    }

    public static double getInitialCardBalance(Context ctx) {
        return getInitialBalanceRaw(ctx, "card");
    }

    public static String getInitialBalancesCurrency(Context ctx) {
        try {
            JSONObject body = new JSONObject(getInitialBalancesRaw(ctx));
            return normalizeCurrency(body.optString("currency", getCurrencyCode(ctx)));
        } catch (Exception e) {
            return getCurrencyCode(ctx);
        }
    }

    public static String getInitialBalancesRaw(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String raw = sp.getString(initialBalancesKey(currentUserId(ctx)), null);
        return raw != null ? raw : "{}";
    }

    public static void saveInitialBalances(Context ctx, double cashBalance, double cardBalance, String currencyCode, SaveCb cb) {
        try {
            if (!isValidAmount(cashBalance) || !isValidAmount(cardBalance)) {
                cb.onFail();
                return;
            }
            JSONObject body = new JSONObject();
            body.put("cash", cashBalance);
            body.put("card", cardBalance);
            body.put("currency", normalizeCurrency(currencyCode == null || currencyCode.trim().isEmpty() ? getCurrencyCode(ctx) : currencyCode));
            SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            sp.edit().putString(initialBalancesKey(currentUserId(ctx)), body.toString()).apply();
            LocalRepository.invalidateDataVersion();
            cb.onSuccess();
        } catch (Exception e) {
            cb.onFail();
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
            LocalRepository.invalidateDataVersion();
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

    private static String initialBalancesKey(long userId) {
        return KEY_INITIAL_BALANCES_PREFIX + userId;
    }

    private static double getInitialBalanceRaw(Context ctx, String key) {
        try {
            JSONObject body = new JSONObject(getInitialBalancesRaw(ctx));
            double value = body.optDouble(key, 0.0);
            return isValidAmount(value) ? value : 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    public static void prepareCurrencySetupForNewUser(Context ctx) {
        try {
            SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            sp.edit().putString(travelKey(currentUserId(ctx)), "{}").apply();
        } catch (Exception ignored) {
        }
    }

    public static String getThemeMode(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String mode = sp.getString(KEY_THEME_MODE, THEME_SYSTEM);
        return normalizeThemeMode(mode);
    }

    public static void saveThemeMode(Context ctx, String mode) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_THEME_MODE, normalizeThemeMode(mode))
                .apply();
    }

    public static void applyThemeMode(Context ctx) {
        AppCompatDelegate.setDefaultNightMode(appCompatNightMode(getThemeMode(ctx)));
    }

    private static boolean isValidAmount(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value) && value >= 0.0;
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
        return CurrencyConverter.normalize(code);
    }

    private static String normalizeThemeMode(String mode) {
        if (THEME_LIGHT.equals(mode)) return THEME_LIGHT;
        if (THEME_DARK.equals(mode)) return THEME_DARK;
        return THEME_SYSTEM;
    }

    private static int appCompatNightMode(String mode) {
        if (THEME_LIGHT.equals(mode)) return AppCompatDelegate.MODE_NIGHT_NO;
        if (THEME_DARK.equals(mode)) return AppCompatDelegate.MODE_NIGHT_YES;
        return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
    }
}
