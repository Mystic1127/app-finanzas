package com.example.finanzas.data.api;

import android.content.Context;

import com.example.finanzas.data.local.LocalRepository;
import com.example.finanzas.data.model.ImportJob;
import com.example.finanzas.data.model.ImportRule;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

public class ImportService {

    public interface ListCallback {
        void onSuccess(List<ImportJob> items);
        void onError();
    }

    public interface RulesCallback {
        void onSuccess(List<ImportRule> rules);
        void onError();
    }

    public interface CreateCallback {
        void onSuccess(int importId);
        void onError();
    }

    public interface SimpleCallback {
        void onSuccess();
        void onError();
    }

    public interface ProcessCallback {
        void onSuccess(JSONObject response);
        void onError();
    }

    public static void list(Context ctx, ListCallback cb) {
        try {
            cb.onSuccess(LocalRepository.getInstance(ctx).listImports());
        } catch (Exception e) {
            cb.onError();
        }
    }

    public static void listRules(Context ctx, RulesCallback cb) {
        try {
            cb.onSuccess(LocalRepository.getInstance(ctx).listImportRules());
        } catch (Exception e) {
            cb.onError();
        }
    }

    public static void create(Context ctx, String tipo, String nombre, JSONArray lineas, CreateCallback cb) {
        try {
            int id = LocalRepository.getInstance(ctx).createImport(nombre, tipo, lineas != null ? lineas.toString() : "[]");
            cb.onSuccess(id);
        } catch (Exception e) {
            cb.onError();
        }
    }

    public static void saveRule(Context ctx, ImportRule rule, SimpleCallback cb) {
        try {
            LocalRepository.getInstance(ctx).saveImportRule(rule);
            cb.onSuccess();
        } catch (Exception e) {
            cb.onError();
        }
    }

    public static void deleteRule(Context ctx, int ruleId, SimpleCallback cb) {
        try {
            LocalRepository.getInstance(ctx).deleteImportRule(ruleId);
            cb.onSuccess();
        } catch (Exception e) {
            cb.onError();
        }
    }

    public static void process(Context ctx, int importId, ProcessCallback cb) {
        try {
            JSONObject result = LocalRepository.getInstance(ctx).processImport(importId);
            cb.onSuccess(result);
        } catch (Exception e) {
            cb.onError();
        }
    }
}
