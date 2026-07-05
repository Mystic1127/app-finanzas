package com.example.finanzas.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.finanzas.data.cloud.CloudSyncService;

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
    private static final String[] PALETTE = new String[]{
            "#4FA37A", "#43A047", "#00ACC1", "#1E88E5", "#3949AB", "#7B1FA2",
            "#D81B60", "#E91E63", "#E53935", "#F4511E", "#FB8C00", "#F9A825",
            "#FBC02D", "#8CBF26", "#66A61E", "#2E7D32", "#00897B", "#0097A7",
            "#1E88E5", "#303F9F", "#673AB7", "#7E3FF2", "#AD2A9C", "#6A4C75",
            "#D69E3D", "#9C6B3E", "#A63A2B", "#6D4C41", "#5D4037", "#A1887F",
            "#B0B7BC", "#747C82", "#455A64", "#303A40", "#1F1F1F", "#121212"
    };

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
        if (cleanName.isEmpty() || !isValidHex(colorHex)) {
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

    public static void setLabel(@NonNull Context context, int transactionId, @Nullable String labelId) {
        if (transactionId <= 0) return;
        Map<Integer, String> assignments = listAssignments(context);
        if (TextUtils.isEmpty(labelId) || findLabel(context, labelId) == null) {
            assignments.remove(transactionId);
        } else {
            assignments.put(transactionId, labelId);
        }
        saveAssignments(context, assignments);
    }

    @Nullable
    public static String assignedLabelId(@NonNull Context context, int transactionId) {
        if (transactionId <= 0) return null;
        return listAssignments(context).get(transactionId);
    }

    public static void updateLabel(@NonNull Context context, @NonNull String labelId, @NonNull String name, @NonNull String colorHex) {
        String cleanName = name.trim();
        String cleanColor = normalizeHex(colorHex);
        if (TextUtils.isEmpty(labelId) || cleanName.isEmpty() || !isValidHex(colorHex)) {
            throw new IllegalArgumentException("Invalid label");
        }
        List<Label> labels = listLabels(context);
        List<Label> updated = new ArrayList<>();
        for (Label label : labels) {
            updated.add(label.id.equals(labelId) ? new Label(label.id, cleanName, cleanColor) : label);
        }
        saveLabels(context, updated);
    }

    public static void deleteLabel(@NonNull Context context, @NonNull String labelId) {
        if (TextUtils.isEmpty(labelId)) return;
        List<Label> labels = listLabels(context);
        List<Label> kept = new ArrayList<>();
        for (Label label : labels) {
            if (!label.id.equals(labelId)) kept.add(label);
        }
        saveLabels(context, kept);

        Map<Integer, String> assignments = listAssignments(context);
        List<Integer> toRemove = new ArrayList<>();
        for (Map.Entry<Integer, String> entry : assignments.entrySet()) {
            if (labelId.equals(entry.getValue())) toRemove.add(entry.getKey());
        }
        for (Integer id : toRemove) assignments.remove(id);
        saveAssignments(context, assignments);
    }

    @NonNull
    public static JSONObject exportSyncData(@NonNull Context context) {
        JSONObject body = new JSONObject();
        try {
            body.put("labels", new JSONArray(prefs(context).getString(labelsKey(context), "[]")));
            body.put("assignments", new JSONObject(prefs(context).getString(assignmentsKey(context), "{}")));
        } catch (Exception ignored) { }
        return body;
    }

    public static void importSyncData(@NonNull Context context, @Nullable JSONObject body) {
        if (body == null) return;
        try {
            prefs(context).edit()
                    .putString(labelsKey(context), body.optJSONArray("labels") == null ? "[]" : body.optJSONArray("labels").toString())
                    .putString(assignmentsKey(context), body.optJSONObject("assignments") == null ? "{}" : body.optJSONObject("assignments").toString())
                    .apply();
        } catch (Exception ignored) { }
    }

    public static void clearForCurrentUser(@NonNull Context context) {
        prefs(context).edit()
                .remove(labelsKey(context))
                .remove(assignmentsKey(context))
                .apply();
        CloudSyncService.scheduleUpload(context);
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
        return raw != null && raw.trim().matches("^#[0-9A-Fa-f]{6}$");
    }

    @NonNull
    public static String[] paletteColors() {
        return PALETTE.clone();
    }

    @NonNull
    public static String normalizeHex(@Nullable String raw) {
        String clean = raw == null ? "" : raw.trim();
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
        CloudSyncService.scheduleUpload(context);
    }

    private static void saveAssignments(Context context, Map<Integer, String> assignments) {
        JSONObject object = new JSONObject();
        for (Map.Entry<Integer, String> entry : assignments.entrySet()) {
            try {
                object.put(String.valueOf(entry.getKey()), entry.getValue());
            } catch (Exception ignored) { }
        }
        prefs(context).edit().putString(assignmentsKey(context), object.toString()).apply();
        CloudSyncService.scheduleUpload(context);
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
