package com.example.finanzas.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class TransactionLabelStore {
    private static final String PREFS = "finanzas_settings";
    private static final String KEY_LABELS_PREFIX = "transaction_labels_user_";
    private static final String KEY_ASSIGNMENTS_PREFIX = "transaction_label_assignments_user_";

    private TransactionLabelStore() { }

    public static final class Label {
        public final String id;
        public final String name;
        public final String colorHex;

        public Label(@NonNull String id, @NonNull String name, @NonNull String colorHex) {
            this.id = id;
            this.name = name;
            this.colorHex = normalizeHex(colorHex);
        }

        public int colorInt() {
            try {
                return Color.parseColor(colorHex);
            } catch (IllegalArgumentException e) {
                return Color.parseColor("#4FA37A");
            }
        }
    }

    @NonNull
    public static List<Label> listLabels(@NonNull Context context) {
        List<Label> out = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(prefs(context).getString(labelsKey(context), "[]"));
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.optJSONObject(i);
                if (item == null) continue;
                String id = item.optString("id", "");
                String name = item.optString("name", "");
                String color = item.optString("color", "");
                if (!TextUtils.isEmpty(id) && !TextUtils.isEmpty(name) && isValidHex(color)) {
                    out.add(new Label(id, name, color));
                }
            }
        } catch (Exception ignored) { }
        return out;
    }

    @Nullable
    public static Label findLabel(@NonNull Context context, @Nullable String labelId) {
        if (TextUtils.isEmpty(labelId)) return null;
        for (Label label : listLabels(context)) {
            if (label.id.equals(labelId)) return label;
        }
        return null;
    }

    @NonNull
    public static Label createLabel(@NonNull Context context, @NonNull String name, @NonNull String colorHex) {
        String cleanName = name.trim();
        String cleanColor = normalizeHex(colorHex);
        if (cleanName.isEmpty() || !isValidHex(cleanColor)) {
            throw new IllegalArgumentException("Invalid label");
        }
        List<Label> labels = listLabels(context);
        Label label = new Label("label_" + System.currentTimeMillis(), cleanName, cleanColor);
        labels.add(label);
        saveLabels(context, labels);
        return label;
    }

    public static void assignLabel(@NonNull Context context, @NonNull String labelId, @NonNull List<Integer> transactionIds) {
        Map<Integer, String> assignments = listAssignments(context);
        for (Integer id : transactionIds) {
            if (id != null && id > 0) assignments.put(id, labelId);
        }
        saveAssignments(context, assignments);
    }

    @NonNull
    public static Map<Integer, String> listAssignments(@NonNull Context context) {
        Map<Integer, String> out = new LinkedHashMap<>();
        try {
            JSONObject object = new JSONObject(prefs(context).getString(assignmentsKey(context), "{}"));
            JSONArray names = object.names();
            if (names == null) return out;
            for (int i = 0; i < names.length(); i++) {
                String key = names.optString(i, "");
                if (TextUtils.isEmpty(key)) continue;
                int txId = Integer.parseInt(key);
                String labelId = object.optString(key, "");
                if (!TextUtils.isEmpty(labelId)) out.put(txId, labelId);
            }
        } catch (Exception ignored) { }
        return out;
    }

    @NonNull
    public static Map<Integer, Label> assignedLabelDetails(@NonNull Context context) {
        Map<String, Label> labelsById = new HashMap<>();
        for (Label label : listLabels(context)) labelsById.put(label.id, label);
        Map<Integer, Label> out = new HashMap<>();
        for (Map.Entry<Integer, String> assignment : listAssignments(context).entrySet()) {
            Label label = labelsById.get(assignment.getValue());
            if (label != null) out.put(assignment.getKey(), label);
        }
        return out;
    }

    public static boolean isValidHex(@Nullable String raw) {
        return raw != null && raw.trim().matches("#[0-9a-fA-F]{6}");
    }

    @NonNull
    public static String normalizeHex(@Nullable String raw) {
        String clean = raw == null ? "" : raw.trim();
        if (!clean.startsWith("#")) clean = "#" + clean;
        return clean.toUpperCase(Locale.ROOT);
    }

    private static void saveLabels(Context context, List<Label> labels) {
        JSONArray array = new JSONArray();
        for (Label label : labels) {
            JSONObject item = new JSONObject();
            try {
                item.put("id", label.id);
                item.put("name", label.name);
                item.put("color", label.colorHex);
                array.put(item);
            } catch (Exception ignored) { }
        }
        prefs(context).edit().putString(labelsKey(context), array.toString()).apply();
    }

    private static void saveAssignments(Context context, Map<Integer, String> assignments) {
        JSONObject object = new JSONObject();
        for (Map.Entry<Integer, String> entry : assignments.entrySet()) {
            try {
                object.put(String.valueOf(entry.getKey()), entry.getValue());
            } catch (Exception ignored) { }
        }
        prefs(context).edit().putString(assignmentsKey(context), object.toString()).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static String labelsKey(Context context) {
        return KEY_LABELS_PREFIX + Prefs.getCurrentUserId(context);
    }

    private static String assignmentsKey(Context context) {
        return KEY_ASSIGNMENTS_PREFIX + Prefs.getCurrentUserId(context);
    }
}
