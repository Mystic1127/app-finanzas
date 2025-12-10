package com.example.finanzas.data.api;

import android.content.Context;

import com.example.finanzas.data.local.LocalRepository;
import com.example.finanzas.data.model.SavingsGoal;

import org.json.JSONObject;

import java.util.List;

public class GoalService {

    public interface ListCb { void onOk(List<SavingsGoal> items); void onFail(); }
    public interface SaveCb { void onOk(); void onFail(); }
    public interface DeleteCb { void onOk(); void onFail(); }
    public interface MilestoneSaveCb { void onOk(int id); void onFail(); }

    public static void list(Context ctx, ListCb cb) {
        try {
            cb.onOk(LocalRepository.getInstance(ctx).listGoals());
        } catch (Exception e) {
            cb.onFail();
        }
    }

    public static void save(Context ctx, JSONObject body, SaveCb cb) {
        int id = body.optInt("id", 0);
        String titulo = body.optString("titulo", "");
        double objetivo = body.optDouble("monto_objetivo", 0);
        double actual = body.optDouble("monto_actual", 0);
        String fecha = body.optString("fecha_objetivo", null);
        boolean ok = LocalRepository.getInstance(ctx).saveGoal(id, titulo, objetivo, actual, fecha);
        if (ok) cb.onOk(); else cb.onFail();
    }

    public static void delete(Context ctx, int id, DeleteCb cb) {
        boolean ok = LocalRepository.getInstance(ctx).deleteGoal(id);
        if (ok) cb.onOk(); else cb.onFail();
    }

    public static void saveMilestone(Context ctx, JSONObject body, MilestoneSaveCb cb) {
        int id = body.optInt("id", 0);
        int metaId = body.optInt("meta_id", 0);
        String titulo = body.optString("titulo", "");
        double monto = body.optDouble("monto_planificado", 0);
        String fecha = body.optString("fecha_objetivo", null);
        boolean notificar = body.optInt("notificar", 0) == 1 || body.optBoolean("notificar", false);
        int dias = body.optInt("dias_recordatorio", 0);
        boolean completado = body.optInt("completado", 0) == 1 || body.optBoolean("completado", false);
        int newId = LocalRepository.getInstance(ctx).saveMilestone(id, metaId, titulo, monto, fecha, notificar, dias, completado);
        if (newId > 0) cb.onOk(newId); else cb.onFail();
    }

    public static void deleteMilestone(Context ctx, int id, DeleteCb cb) {
        boolean ok = LocalRepository.getInstance(ctx).deleteMilestone(id);
        if (ok) cb.onOk(); else cb.onFail();
    }
}
