package com.example.finanzas.data.api;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

public class SettingsService {

    public interface SaveCb { void onSuccess(); void onFail(); }

    private static final String PREFS = "finanzas_settings";
    private static final String KEY_DASHBOARD = "dashboard_prefs";
    private static final String KEY_TRAVEL = "travel_prefs";

    public static void save(Context ctx, boolean notificationsEnabled, SaveCb cb) {
        cb.onSuccess();
    }

    public static String getDashboardRaw(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return sp.getString(KEY_DASHBOARD, "{}");
    }

    public static String getTravelRaw(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return sp.getString(KEY_TRAVEL, "{}");
    }

    public static void saveDashboard(Context ctx, JSONObject body, SaveCb cb) {
        try {
            SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            sp.edit().putString(KEY_DASHBOARD, body != null ? body.toString() : "{}").apply();
            cb.onSuccess();
        } catch (Exception e) {
            cb.onFail();
        }
    }

    public static void saveTravel(Context ctx, JSONObject body, SaveCb cb) {
        try {
            SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            sp.edit().putString(KEY_TRAVEL, body != null ? body.toString() : "{}").apply();
            cb.onSuccess();
        } catch (Exception e) {
            cb.onFail();
        }
    }
}
