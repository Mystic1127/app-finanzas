package com.example.finanzas.util;

import java.text.DateFormatSymbols;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Currency;
import java.util.Date;
import java.util.Locale;

public class Format {
    private static final Locale LOCALE = new Locale("es", "PE");
    private static final NumberFormat CURRENCY = NumberFormat.getCurrencyInstance(LOCALE);
    private static final SimpleDateFormat DATE = new SimpleDateFormat("dd/MM/yyyy", LOCALE);
    private static final NumberFormat PERCENT = NumberFormat.getPercentInstance(LOCALE);
    private static final DateFormatSymbols DFS = new DateFormatSymbols(LOCALE);

    static {
        PERCENT.setMaximumFractionDigits(1);
    }

    public static String money(double v) { return CURRENCY.format(v); }
    public static String money(double v, String currencyCode) {
        if (currencyCode == null || currencyCode.trim().isEmpty()) {
            return money(v);
        }
        try {
            String code = CurrencyConverter.normalize(currencyCode);
            Currency.getInstance(code);
            DecimalFormat nf = (DecimalFormat) NumberFormat.getNumberInstance(LOCALE);
            nf.setMinimumFractionDigits(2);
            nf.setMaximumFractionDigits(2);
            return CurrencyConverter.symbol(code) + " " + nf.format(v);
        } catch (Exception e) {
            return money(v);
        }
    }
    public static String date(Date d) { return d == null ? "—" : DATE.format(d); }
    public static String percent(double v) { return PERCENT.format(v / 100.0); }

    public static String monthYear(int year, int month) {
        if (month < 1 || month > 12) {
            return year + "-" + month;
        }
        String[] months = DFS.getMonths();
        String name = months != null && months.length >= month ? months[month - 1] : "";
        if (name == null || name.isEmpty()) {
            name = String.format(LOCALE, "%02d", month);
        } else {
            name = name.substring(0, 1).toUpperCase(LOCALE) + name.substring(1);
        }
        return name + " " + year;
    }
}
