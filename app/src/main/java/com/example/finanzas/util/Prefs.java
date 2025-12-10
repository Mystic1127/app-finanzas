package com.example.finanzas.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import androidx.annotation.Nullable;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class Prefs {
    private static final String P = "finanzas_prefs";
    private static final String KEY_PRES = "presupuesto_mensual";
    private static final String KEY_TOKEN = "auth_token";
    private static final String KEY_PIN_HASH_PREFIX = "pin_hash_";
    private static final String KEY_PIN_ENABLED_PREFIX = "pin_enabled_";
    private static final String KEY_TRANS_YEAR_PREFIX = "trans_period_year_";
    private static final String KEY_TRANS_MONTH_PREFIX = "trans_period_month_";

    public static void setPresupuesto(Context ctx, double v){
        SharedPreferences sp = ctx.getSharedPreferences(P, Context.MODE_PRIVATE);
        sp.edit().putFloat(KEY_PRES, (float) v).apply();
    }

    public static double getPresupuesto(Context ctx){
        return ctx.getSharedPreferences(P, Context.MODE_PRIVATE)
                .getFloat(KEY_PRES, 0f);
    }

    public static void setToken(Context ctx, String token) {
        ctx.getSharedPreferences(P, Context.MODE_PRIVATE)
                .edit().putString(KEY_TOKEN, token).apply();
    }

    @Nullable
    public static String getToken(Context ctx) {
        return ctx.getSharedPreferences(P, Context.MODE_PRIVATE)
                .getString(KEY_TOKEN, null);
    }

    public static void savePin(Context ctx, String pin) {
        if (pin == null) return;
        long userId = currentUserId(ctx);
        if (userId <= 0) return;

        SharedPreferences sp = ctx.getSharedPreferences(P, Context.MODE_PRIVATE);
        sp.edit()
                .putString(pinHashKey(userId), hashPin(pin))
                .putBoolean(pinEnabledKey(userId), true)
                .apply();
    }

    public static boolean hasPin(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(P, Context.MODE_PRIVATE);
        long userId = currentUserId(ctx);
        if (userId <= 0) return false;
        return sp.getBoolean(pinEnabledKey(userId), false)
                && sp.contains(pinHashKey(userId));
    }

    public static boolean verifyPin(Context ctx, String pin) {
        if (pin == null) return false;
        SharedPreferences sp = ctx.getSharedPreferences(P, Context.MODE_PRIVATE);
        long userId = currentUserId(ctx);
        if (userId <= 0) return false;
        String stored = sp.getString(pinHashKey(userId), null);
        if (stored == null) return false;
        return stored.equals(hashPin(pin));
    }

    public static void clearPin(Context ctx) {
        long userId = currentUserId(ctx);
        if (userId <= 0) return;
        SharedPreferences sp = ctx.getSharedPreferences(P, Context.MODE_PRIVATE);
        sp.edit()
                .remove(pinHashKey(userId))
                .putBoolean(pinEnabledKey(userId), false)
                .apply();
    }

    public static void clearAuth(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(P, Context.MODE_PRIVATE);
        long userId = currentUserId(ctx);
        sp.edit().remove(KEY_TOKEN).apply();
        if (userId > 0) {
            sp.edit()
                    .remove(pinHashKey(userId))
                    .putBoolean(pinEnabledKey(userId), false)
                    .apply();
        }
    }

    public static void setLastTransactionsPeriod(Context ctx, int year, int month) {
        long userId = currentUserId(ctx);
        if (userId <= 0) return;
        SharedPreferences sp = ctx.getSharedPreferences(P, Context.MODE_PRIVATE);
        sp.edit()
                .putInt(KEY_TRANS_YEAR_PREFIX + userId, year)
                .putInt(KEY_TRANS_MONTH_PREFIX + userId, month)
                .apply();
    }

    public static int getLastTransactionsYear(Context ctx) {
        long userId = currentUserId(ctx);
        if (userId <= 0) return -1;
        return ctx.getSharedPreferences(P, Context.MODE_PRIVATE)
                .getInt(KEY_TRANS_YEAR_PREFIX + userId, -1);
    }

    public static int getLastTransactionsMonth(Context ctx) {
        long userId = currentUserId(ctx);
        if (userId <= 0) return -1;
        return ctx.getSharedPreferences(P, Context.MODE_PRIVATE)
                .getInt(KEY_TRANS_MONTH_PREFIX + userId, -1);
    }

    public static void clearLastTransactionsPeriod(Context ctx) {
        long userId = currentUserId(ctx);
        if (userId <= 0) return;
        SharedPreferences sp = ctx.getSharedPreferences(P, Context.MODE_PRIVATE);
        sp.edit()
                .remove(KEY_TRANS_YEAR_PREFIX + userId)
                .remove(KEY_TRANS_MONTH_PREFIX + userId)
                .apply();
    }

    public static long getCurrentUserId(Context ctx) {
        return currentUserId(ctx);
    }

    private static String hashPin(String pin) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(pin.getBytes(StandardCharsets.UTF_8));
            return Base64.encodeToString(dig, Base64.NO_WRAP);
        } catch (Exception e) {
            return pin;
        }
    }

    private static long currentUserId(Context ctx) {
        return ctx.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                .getLong("user_id", -1);
    }

    private static String pinHashKey(long userId) {
        return KEY_PIN_HASH_PREFIX + userId;
    }

    private static String pinEnabledKey(long userId) {
        return KEY_PIN_ENABLED_PREFIX + userId;
    }
}
