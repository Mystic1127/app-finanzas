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
            Color.rgb(78, 105, 170)
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
        if (ICON_TRANSPORT.equals(key)) return R.drawable.ic_category_travel;
        if (ICON_SERVICES.equals(key)) return R.drawable.ic_category_services;
        if (ICON_HEALTH.equals(key)) return R.drawable.ic_category_health;
        if (ICON_EDUCATION.equals(key)) return R.drawable.ic_category_education;
        if (ICON_SHOPPING.equals(key)) return R.drawable.ic_category_shopping;
        if (ICON_DEBT.equals(key)) return R.drawable.ic_category_debt;
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
        if (income) return ICON_SAVINGS;
        if (key.contains("transporte") || key.contains("viaje")) return ICON_TRANSPORT;
        if (key.contains("servicio") || key.contains("luz") || key.contains("internet")) return ICON_SERVICES;
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
