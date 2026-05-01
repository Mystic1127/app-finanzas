package com.example.finanzas.util;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewParent;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public final class UiFormUtils {

    private static final Locale LOCALE = Locale.US;
    private static final String UI_DATE_PATTERN = "dd/MM/yyyy";
    private static final String ISO_DATE_PATTERN = "yyyy-MM-dd";

    private UiFormUtils() {
    }

    public static void bindDatePicker(@NonNull Context context, @NonNull EditText input) {
        input.setFocusable(false);
        input.setClickable(true);
        input.setInputType(InputType.TYPE_NULL);
        input.setOnClickListener(v -> showDatePicker(context, input));
        input.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) showDatePicker(context, input);
        });
    }

    public static void bindTimePicker(@NonNull Context context, @NonNull EditText input) {
        input.setFocusable(false);
        input.setClickable(true);
        input.setInputType(InputType.TYPE_NULL);
        input.setOnClickListener(v -> showTimePicker(context, input));
        input.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) showTimePicker(context, input);
        });
    }

    public static void showDatePicker(@NonNull Context context, @NonNull EditText input) {
        Calendar calendar = Calendar.getInstance();
        Date currentDate = parseUiDate(input.getText() == null ? "" : input.getText().toString());
        if (currentDate != null) {
            calendar.setTime(currentDate);
        }

        DatePickerDialog dialog = new DatePickerDialog(context, (view, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            input.setText(formatUiDate(calendar.getTime()));
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    public static void showTimePicker(@NonNull Context context, @NonNull EditText input) {
        Calendar calendar = Calendar.getInstance();
        String current = input.getText() == null ? "" : input.getText().toString().trim();
        if (isValidTime(current)) {
            String[] parts = current.split(":");
            calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(parts[0]));
            calendar.set(Calendar.MINUTE, Integer.parseInt(parts[1]));
        }

        TimePickerDialog dialog = new TimePickerDialog(context,
                (view, hourOfDay, minute) -> input.setText(String.format(LOCALE, "%02d:%02d", hourOfDay, minute)),
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true);
        dialog.show();
    }

    @NonNull
    public static String formatUiDate(@Nullable Date date) {
        return date == null ? "" : uiDateFormat().format(date);
    }

    @NonNull
    public static String formatIsoDate(@Nullable Date date) {
        return date == null ? "" : isoDateFormat().format(date);
    }

    @Nullable
    public static Date parseUiDate(@Nullable String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        String value = raw.trim();
        Date date = parseStrict(uiDateFormat(), value);
        if (date != null) return date;
        return parseStrict(isoDateFormat(), value);
    }

    @Nullable
    public static String uiDateToIso(@Nullable String raw) {
        Date date = parseUiDate(raw);
        return date == null ? null : formatIsoDate(date);
    }

    public static boolean isValidUiDate(@Nullable String raw) {
        return parseUiDate(raw) != null;
    }

    public static boolean isValidTime(@Nullable String raw) {
        if (raw == null || !raw.matches("^\\d{2}:\\d{2}$")) return false;
        try {
            String[] parts = raw.split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);
            return hour >= 0 && hour <= 23 && minute >= 0 && minute <= 59;
        } catch (Exception e) {
            return false;
        }
    }

    public static void clearErrors(@NonNull EditText... fields) {
        for (EditText field : fields) {
            clearError(field);
        }
    }

    public static void clearError(@NonNull EditText field) {
        TextInputLayout layout = findTextInputLayout(field);
        if (layout != null) {
            layout.setError(null);
            layout.setErrorEnabled(false);
        } else {
            field.setError(null);
        }
    }

    public static void setError(@NonNull EditText field, @NonNull String message) {
        TextInputLayout layout = findTextInputLayout(field);
        if (layout != null) {
            layout.setError(message);
        } else {
            field.setError(message);
        }
    }

    public static void clearErrorOnTextChange(@NonNull EditText... fields) {
        for (EditText field : fields) {
            field.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    clearError(field);
                }
                @Override public void afterTextChanged(Editable s) { }
            });
        }
    }

    public static void showMessage(@NonNull View anchor, int resId) {
        Snackbar.make(anchor, resId, Snackbar.LENGTH_SHORT).show();
    }

    public static void showMessage(@NonNull View anchor, @NonNull CharSequence message) {
        Snackbar.make(anchor, message, Snackbar.LENGTH_LONG).show();
    }

    public static void setActionLoading(@Nullable TextView action, boolean loading) {
        if (action == null) return;
        action.setEnabled(!loading);
        action.setAlpha(loading ? 0.65f : 1f);
    }

    @Nullable
    private static TextInputLayout findTextInputLayout(@NonNull View field) {
        ViewParent parent = field.getParent();
        while (parent instanceof View) {
            if (parent instanceof TextInputLayout) {
                return (TextInputLayout) parent;
            }
            parent = parent.getParent();
        }
        return null;
    }

    @Nullable
    private static Date parseStrict(@NonNull SimpleDateFormat format, @NonNull String value) {
        try {
            return format.parse(value);
        } catch (ParseException e) {
            return null;
        }
    }

    @NonNull
    private static SimpleDateFormat uiDateFormat() {
        SimpleDateFormat df = new SimpleDateFormat(UI_DATE_PATTERN, LOCALE);
        df.setLenient(false);
        return df;
    }

    @NonNull
    private static SimpleDateFormat isoDateFormat() {
        SimpleDateFormat df = new SimpleDateFormat(ISO_DATE_PATTERN, LOCALE);
        df.setLenient(false);
        return df;
    }
}
