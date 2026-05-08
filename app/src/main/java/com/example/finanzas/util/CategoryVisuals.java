package com.example.finanzas.util;

import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.finanzas.R;
import com.example.finanzas.data.model.Categoria;

import java.text.Normalizer;
import java.util.Locale;

public final class CategoryVisuals {
    private CategoryVisuals() {
    }

    @DrawableRes
    public static int iconFor(@Nullable String name, boolean income) {
        String key = normalize(name);
        if (key.equals("alimentacion")) return R.drawable.ic_restaurant;
        if (key.equals("deudas")) return R.drawable.ic_receipt_long;
        if (key.equals("entretenimiento")) return R.drawable.ic_sports_esports;
        if (key.equals("familia")) return R.drawable.ic_family_restroom;
        if (key.equals("freelance")) return R.drawable.ic_work_history;
        if (key.equals("impuestos")) return R.drawable.ic_account_balance;
        if (key.equals("inversiones")) return R.drawable.ic_trending_up;
        if (key.equals("negocio")) return R.drawable.ic_store;
        if (key.equals("otros ingresos")) return R.drawable.ic_add_card;
        if (key.equals("reembolso")) return R.drawable.ic_assignment_return;
        if (key.equals("regalo recibido")) return R.drawable.ic_redeem;
        if (key.equals("ropa")) return R.drawable.ic_checkroom;
        if (key.equals("servicios")) return R.drawable.ic_home_repair_service;
        if (key.equals("sueldo")) return R.drawable.ic_payments;
        if (key.equals("tecnologia")) return R.drawable.ic_devices;
        if (key.equals("transporte")) return R.drawable.ic_directions_bus;
        if (key.equals("venta")) return R.drawable.ic_sell;
        if (key.equals("vivienda")) return R.drawable.ic_home;
        if (key.equals("delivery")) return R.drawable.ic_delivery_dining;
        if (key.equals("restaurantes")) return R.drawable.ic_restaurant_menu;
        if (key.equals("farmacia")) return R.drawable.ic_local_pharmacy;
        if (key.equals("suscripciones")) return R.drawable.ic_subscriptions;
        if (key.equals("internet / telefono") || key.equals("internet telefono")) return R.drawable.ic_wifi_calling;
        if (key.equals("luz")) return R.drawable.ic_bolt;
        if (key.equals("agua")) return R.drawable.ic_water_drop;
        if (key.equals("gas")) return R.drawable.ic_local_fire_department;
        if (key.equals("reparaciones")) return R.drawable.ic_build;
        if (key.equals("cuidado personal")) return R.drawable.ic_spa;
        if (income) {
            if (contains(key, "salario", "sueldo", "nomina")) return R.drawable.ic_income_salary;
            if (contains(key, "freelance", "independiente", "servicio")) return R.drawable.ic_income_freelance;
            if (contains(key, "negocio", "business", "empresa")) return R.drawable.ic_income_business;
            if (contains(key, "inversion", "dividendo", "interes")) return R.drawable.ic_income_investments;
            if (contains(key, "regalo", "gift")) return R.drawable.ic_income_gift;
            if (contains(key, "reembolso", "refund", "devolucion")) return R.drawable.ic_income_refund;
            if (contains(key, "venta", "sale")) return R.drawable.ic_income_sale;
            return R.drawable.ic_income_other;
        }

        if (contains(key, "salud", "medic", "clinica", "farmacia")) return R.drawable.ic_category_health;
        if (contains(key, "educacion", "curso", "universidad", "colegio")) return R.drawable.ic_category_education;
        if (contains(key, "servicio", "luz", "agua", "internet", "telefono")) return R.drawable.ic_category_services;
        if (contains(key, "super", "mercado", "alimentacion", "comida", "grocer")) return R.drawable.ic_category_groceries;
        if (contains(key, "compra", "shopping", "tienda")) return R.drawable.ic_category_shopping;
        if (contains(key, "ropa", "calzado", "vestimenta")) return R.drawable.ic_category_clothing;
        if (contains(key, "tecnologia", "software", "celular", "laptop")) return R.drawable.ic_category_technology;
        if (contains(key, "viaje", "hotel", "vuelo", "turismo")) return R.drawable.ic_category_travel;
        if (contains(key, "mascota", "pet", "veterin")) return R.drawable.ic_category_pets;
        if (contains(key, "familia", "hijo", "hogar", "vivienda", "alquiler", "casa")) return R.drawable.ic_category_family;
        if (contains(key, "deuda", "prestamo", "credito")) return R.drawable.ic_category_debt;
        if (contains(key, "ahorro", "meta")) return R.drawable.ic_category_savings;
        if (contains(key, "regalo", "gift")) return R.drawable.ic_category_gifts;
        if (contains(key, "trabajo", "oficina")) return R.drawable.ic_category_work;
        if (contains(key, "impuesto", "tax")) return R.drawable.ic_category_taxes;
        return R.drawable.ic_category_other;
    }

    @DrawableRes
    public static int iconFor(@NonNull Context context, @NonNull Categoria categoria) {
        return CategoryPrefs.iconFor(CategoryPrefs.meta(context, categoria).iconKey);
    }

    @ColorInt
    public static int colorFor(@NonNull Context context, @Nullable String name, boolean income) {
        String key = normalize(name);
        Integer exact = exactColor(key);
        if (exact != null) return exact;
        if (income) {
            if (contains(key, "salario", "sueldo")) return color(context, R.color.income);
            if (contains(key, "inversion")) return color(context, R.color.chart_pie_3);
            if (contains(key, "regalo", "reembolso", "venta")) return color(context, R.color.chart_pie_4);
            return color(context, R.color.chart_pie_1);
        }
        if (contains(key, "vivienda", "alquiler", "casa", "familia")) return color(context, R.color.chart_pie_4);
        if (contains(key, "entretenimiento", "ocio", "cine", "netflix")) return color(context, R.color.chart_pie_2);
        if (contains(key, "super", "mercado", "alimentacion", "comida")) return color(context, R.color.chart_pie_1);
        if (contains(key, "transporte", "viaje")) return color(context, R.color.chart_pie_6);
        if (contains(key, "salud", "deuda", "impuesto")) return color(context, R.color.expense);
        if (contains(key, "tecnologia", "servicio")) return color(context, R.color.chart_pie_3);
        if (contains(key, "ropa", "compra", "regalo")) return color(context, R.color.chart_pie_5);
        return color(context, R.color.chart_pie_8);
    }

    @Nullable
    private static Integer exactColor(@NonNull String key) {
        switch (key) {
            case "alimentacion": return Color.rgb(214, 101, 41);
            case "delivery": return Color.rgb(218, 118, 47);
            case "restaurantes": return Color.rgb(190, 78, 45);
            case "farmacia":
            case "cuidado personal": return Color.rgb(43, 145, 124);
            case "suscripciones":
            case "entretenimiento": return Color.rgb(126, 78, 178);
            case "internet / telefono":
            case "internet telefono":
            case "tecnologia": return Color.rgb(55, 112, 185);
            case "luz": return Color.rgb(218, 166, 35);
            case "agua": return Color.rgb(41, 142, 189);
            case "gas": return Color.rgb(199, 82, 42);
            case "reparaciones":
            case "servicios": return Color.rgb(94, 117, 134);
            case "deudas": return Color.rgb(171, 65, 75);
            case "familia": return Color.rgb(174, 83, 136);
            case "impuestos": return Color.rgb(96, 100, 112);
            case "ropa": return Color.rgb(151, 92, 53);
            case "transporte": return Color.rgb(32, 132, 137);
            case "vivienda": return Color.rgb(80, 128, 83);
            case "sueldo": return Color.rgb(31, 122, 85);
            case "freelance": return Color.rgb(45, 118, 169);
            case "inversiones": return Color.rgb(54, 139, 75);
            case "negocio": return Color.rgb(121, 105, 52);
            case "otros ingresos": return Color.rgb(84, 114, 172);
            case "reembolso": return Color.rgb(35, 151, 140);
            case "regalo recibido": return Color.rgb(185, 86, 128);
            case "venta": return Color.rgb(176, 109, 40);
            default: return null;
        }
    }

    @ColorInt
    public static int colorFor(@NonNull Context context, @NonNull Categoria categoria) {
        return CategoryPrefs.meta(context, categoria).color;
    }

    @NonNull
    public static String normalize(@Nullable String raw) {
        if (TextUtils.isEmpty(raw)) return "";
        String clean = Normalizer.normalize(raw.toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        return clean.replaceAll("\\p{InCombiningDiacriticalMarks}+", "").trim();
    }

    private static boolean contains(@NonNull String text, @NonNull String... parts) {
        for (String part : parts) {
            if (text.contains(part)) return true;
        }
        return false;
    }

    private static int color(@NonNull Context context, int resId) {
        return ContextCompat.getColor(context, resId);
    }
}
