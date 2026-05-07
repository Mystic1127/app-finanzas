package com.example.finanzas.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.finanzas.R;
import com.example.finanzas.data.model.Categoria;

import org.json.JSONObject;

import java.util.Iterator;

public final class CategoryPrefs {
    private static final String PREFS = "finanzas_settings";
    private static final String KEY_META = "category_meta";

    public static final String ICON_GROCERIES = "groceries";
    public static final String ICON_TRANSPORT = "transport";
    public static final String ICON_SERVICES = "services";
    public static final String ICON_HEALTH = "health";
    public static final String ICON_EDUCATION = "education";
    public static final String ICON_SHOPPING = "shopping";
    public static final String ICON_DEBT = "debt";
    public static final String ICON_PETS = "pets";
    public static final String ICON_SAVINGS = "savings";
    public static final String ICON_GIFT = "gift";
    public static final String ICON_WORK = "work";
    public static final String ICON_OTHER = "other";
    public static final String ICON_WORK_HISTORY = "work_history";
    public static final String ICON_CHECKROOM = "checkroom";
    public static final String ICON_RESTAURANT = "restaurant";
    public static final String ICON_ENTERTAINMENT = "entertainment";
    public static final String ICON_FAMILY = "family";
    public static final String ICON_ACCOUNT_BALANCE = "account_balance";
    public static final String ICON_TRENDING_UP = "trending_up";
    public static final String ICON_STORE = "store";
    public static final String ICON_ADD_CARD = "add_card";
    public static final String ICON_RETURN = "return";
    public static final String ICON_REDEEM = "redeem";
    public static final String ICON_HOME_REPAIR = "home_repair";
    public static final String ICON_PAYMENTS = "payments";
    public static final String ICON_DEVICES = "devices";
    public static final String ICON_BUS = "bus";
    public static final String ICON_SELL = "sell";
    public static final String ICON_HOME = "home";
    public static final String ICON_DELIVERY = "delivery";
    public static final String ICON_RESTAURANT_MENU = "restaurant_menu";
    public static final String ICON_PHARMACY = "pharmacy";
    public static final String ICON_SUBSCRIPTIONS = "subscriptions";
    public static final String ICON_WIFI_CALLING = "wifi_calling";
    public static final String ICON_BOLT = "bolt";
    public static final String ICON_WATER = "water";
    public static final String ICON_FIRE = "fire";
    public static final String ICON_BUILD = "build";
    public static final String ICON_SPA = "spa";

    public static final int[] PALETTE = new int[] {
            Color.rgb(31, 122, 85),
            Color.rgb(45, 145, 225),
            Color.rgb(126, 78, 220),
            Color.rgb(235, 78, 92),
            Color.rgb(245, 145, 25),
            Color.rgb(238, 194, 45),
            Color.rgb(32, 165, 170),
            Color.rgb(92, 145, 55),
            Color.rgb(190, 72, 125),
            Color.rgb(60, 125, 190),
            Color.rgb(155, 95, 30),
            Color.rgb(78, 105, 170),
            Color.rgb(214, 101, 41),
            Color.rgb(43, 145, 124),
            Color.rgb(126, 78, 178),
            Color.rgb(55, 112, 185),
            Color.rgb(218, 166, 35),
            Color.rgb(199, 82, 42),
            Color.rgb(94, 117, 134),
            Color.rgb(174, 83, 136),
            Color.rgb(96, 100, 112),
            Color.rgb(32, 132, 137),
            Color.rgb(80, 128, 83),
            Color.rgb(176, 109, 40)
    };

    private CategoryPrefs() { }

    @NonNull
    public static Meta meta(@NonNull Context context, @NonNull Categoria categoria) {
        JSONObject object = read(context).optJSONObject(String.valueOf(categoria.id));
        Meta meta = new Meta();
        meta.iconKey = object == null ? inferIcon(categoria.nombre, categoria.esIngreso) : object.optString("icon", inferIcon(categoria.nombre, categoria.esIngreso));
        meta.color = object == null ? CategoryVisuals.colorFor(context, categoria.nombre, categoria.esIngreso) : parseColor(object.optString("color"), CategoryVisuals.colorFor(context, categoria.nombre, categoria.esIngreso));
        meta.deleted = object != null && object.optBoolean("deleted", false);
        return meta;
    }

    public static boolean isDeleted(@NonNull Context context, @NonNull Categoria categoria) {
        return meta(context, categoria).deleted;
    }

    public static boolean hasMeta(@NonNull Context context, int categoryId) {
        return read(context).has(String.valueOf(categoryId));
    }

    public static void saveMeta(@NonNull Context context, int categoryId, @NonNull String iconKey, @ColorInt int color, boolean deleted) {
        JSONObject all = read(context);
        JSONObject one = all.optJSONObject(String.valueOf(categoryId));
        if (one == null) one = new JSONObject();
        try {
            one.put("icon", iconKey);
            one.put("color", String.format("#%06X", 0xFFFFFF & color));
            one.put("deleted", deleted);
            all.put(String.valueOf(categoryId), one);
            write(context, all);
        } catch (Exception ignored) { }
    }

    public static void setDeleted(@NonNull Context context, @NonNull Categoria categoria, boolean deleted) {
        Meta meta = meta(context, categoria);
        saveMeta(context, categoria.id, meta.iconKey, meta.color, deleted);
    }

    @DrawableRes
    public static int iconFor(@Nullable String key) {
        if (ICON_WORK_HISTORY.equals(key)) return R.drawable.ic_work_history;
        if (ICON_CHECKROOM.equals(key)) return R.drawable.ic_checkroom;
        if (ICON_RESTAURANT.equals(key)) return R.drawable.ic_restaurant;
        if (ICON_ENTERTAINMENT.equals(key)) return R.drawable.ic_sports_esports;
        if (ICON_FAMILY.equals(key)) return R.drawable.ic_family_restroom;
        if (ICON_ACCOUNT_BALANCE.equals(key)) return R.drawable.ic_account_balance;
        if (ICON_TRENDING_UP.equals(key)) return R.drawable.ic_trending_up;
        if (ICON_STORE.equals(key)) return R.drawable.ic_store;
        if (ICON_ADD_CARD.equals(key)) return R.drawable.ic_add_card;
        if (ICON_RETURN.equals(key)) return R.drawable.ic_assignment_return;
        if (ICON_REDEEM.equals(key)) return R.drawable.ic_redeem;
        if (ICON_HOME_REPAIR.equals(key)) return R.drawable.ic_home_repair_service;
        if (ICON_PAYMENTS.equals(key)) return R.drawable.ic_payments;
        if (ICON_DEVICES.equals(key)) return R.drawable.ic_devices;
        if (ICON_BUS.equals(key)) return R.drawable.ic_directions_bus;
        if (ICON_SELL.equals(key)) return R.drawable.ic_sell;
        if (ICON_HOME.equals(key)) return R.drawable.ic_home;
        if (ICON_DELIVERY.equals(key)) return R.drawable.ic_delivery_dining;
        if (ICON_RESTAURANT_MENU.equals(key)) return R.drawable.ic_restaurant_menu;
        if (ICON_PHARMACY.equals(key)) return R.drawable.ic_local_pharmacy;
        if (ICON_SUBSCRIPTIONS.equals(key)) return R.drawable.ic_subscriptions;
        if (ICON_WIFI_CALLING.equals(key)) return R.drawable.ic_wifi_calling;
        if (ICON_BOLT.equals(key)) return R.drawable.ic_bolt;
        if (ICON_WATER.equals(key)) return R.drawable.ic_water_drop;
        if (ICON_FIRE.equals(key)) return R.drawable.ic_local_fire_department;
        if (ICON_BUILD.equals(key)) return R.drawable.ic_build;
        if (ICON_SPA.equals(key)) return R.drawable.ic_spa;
        if (ICON_TRANSPORT.equals(key)) return R.drawable.ic_category_travel;
        if (ICON_SERVICES.equals(key)) return R.drawable.ic_category_services;
        if (ICON_HEALTH.equals(key)) return R.drawable.ic_category_health;
        if (ICON_EDUCATION.equals(key)) return R.drawable.ic_category_education;
        if (ICON_SHOPPING.equals(key)) return R.drawable.ic_category_shopping;
        if (ICON_DEBT.equals(key)) return R.drawable.ic_receipt_long;
        if (ICON_PETS.equals(key)) return R.drawable.ic_category_pets;
        if (ICON_SAVINGS.equals(key)) return R.drawable.ic_category_savings;
        if (ICON_GIFT.equals(key)) return R.drawable.ic_category_gifts;
        if (ICON_WORK.equals(key)) return R.drawable.ic_category_work;
        if (ICON_OTHER.equals(key)) return R.drawable.ic_category_other;
        return R.drawable.ic_category_groceries;
    }

    @NonNull
    public static String inferIcon(@Nullable String name, boolean income) {
        String key = CategoryVisuals.normalize(name);
        if (key.equals("alimentacion")) return ICON_RESTAURANT;
        if (key.equals("deudas")) return ICON_DEBT;
        if (key.equals("entretenimiento")) return ICON_ENTERTAINMENT;
        if (key.equals("familia")) return ICON_FAMILY;
        if (key.equals("freelance")) return ICON_WORK_HISTORY;
        if (key.equals("impuestos")) return ICON_ACCOUNT_BALANCE;
        if (key.equals("inversiones")) return ICON_TRENDING_UP;
        if (key.equals("negocio")) return ICON_STORE;
        if (key.equals("otros ingresos")) return ICON_ADD_CARD;
        if (key.equals("reembolso")) return ICON_RETURN;
        if (key.equals("regalo recibido")) return ICON_REDEEM;
        if (key.equals("ropa")) return ICON_CHECKROOM;
        if (key.equals("servicios")) return ICON_HOME_REPAIR;
        if (key.equals("sueldo")) return ICON_PAYMENTS;
        if (key.equals("tecnologia")) return ICON_DEVICES;
        if (key.equals("transporte")) return ICON_BUS;
        if (key.equals("venta")) return ICON_SELL;
        if (key.equals("vivienda")) return ICON_HOME;
        if (key.equals("delivery")) return ICON_DELIVERY;
        if (key.equals("restaurantes")) return ICON_RESTAURANT_MENU;
        if (key.equals("farmacia")) return ICON_PHARMACY;
        if (key.equals("suscripciones")) return ICON_SUBSCRIPTIONS;
        if (key.equals("internet / telefono") || key.equals("internet telefono")) return ICON_WIFI_CALLING;
        if (key.equals("luz")) return ICON_BOLT;
        if (key.equals("agua")) return ICON_WATER;
        if (key.equals("gas")) return ICON_FIRE;
        if (key.equals("reparaciones")) return ICON_BUILD;
        if (key.equals("cuidado personal")) return ICON_SPA;
        if (income) return ICON_ADD_CARD;
        if (key.contains("transporte") || key.contains("viaje")) return ICON_BUS;
        if (key.contains("servicio") || key.contains("luz") || key.contains("internet")) return ICON_HOME_REPAIR;
        if (key.contains("salud")) return ICON_HEALTH;
        if (key.contains("educacion")) return ICON_EDUCATION;
        if (key.contains("compra")) return ICON_SHOPPING;
        if (key.contains("deuda")) return ICON_DEBT;
        if (key.contains("mascota")) return ICON_PETS;
        if (key.contains("ahorro")) return ICON_SAVINGS;
        if (key.contains("regalo")) return ICON_GIFT;
        if (key.contains("trabajo")) return ICON_WORK;
        if (key.contains("aliment") || key.contains("super") || key.contains("comida")) return ICON_GROCERIES;
        return ICON_OTHER;
    }

    private static JSONObject read(@NonNull Context context) {
        String raw = prefs(context).getString(KEY_META, "{}");
        try {
            return new JSONObject(raw == null ? "{}" : raw);
        } catch (Exception ignored) {
            return new JSONObject();
        }
    }

    private static void write(@NonNull Context context, @NonNull JSONObject object) {
        prefs(context).edit().putString(KEY_META, object.toString()).apply();
    }

    private static SharedPreferences prefs(@NonNull Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    @ColorInt
    private static int parseColor(@Nullable String raw, @ColorInt int fallback) {
        try {
            return Color.parseColor(raw);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    public static class Meta {
        public String iconKey;
        @ColorInt public int color;
        public boolean deleted;
    }
}
