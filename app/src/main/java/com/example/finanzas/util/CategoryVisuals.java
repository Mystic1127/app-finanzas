package com.example.finanzas.util;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.finanzas.R;

import java.text.Normalizer;
import java.util.Locale;

public final class CategoryVisuals {
    private CategoryVisuals() {
    }

    @DrawableRes
    public static int iconFor(@Nullable String name, boolean income) {
        String key = normalize(name);
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

    @ColorInt
    public static int colorFor(@NonNull Context context, @Nullable String name, boolean income) {
        String key = normalize(name);
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
