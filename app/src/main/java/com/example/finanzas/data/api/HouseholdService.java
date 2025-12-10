package com.example.finanzas.data.api;

import android.content.Context;

import com.example.finanzas.data.local.LocalRepository;
import com.example.finanzas.data.model.HouseholdSummary;

import java.util.List;

public class HouseholdService {

    public interface ListCallback {
        void onSuccess(List<HouseholdSummary> items);
        void onError();
    }

    public interface CreateCallback {
        void onSuccess(int id, String codigo);
        void onError();
    }

    public interface SimpleCallback { void onSuccess(); void onError(); }

    public interface InviteCallback { void onSuccess(String codigo); void onError(); }

    public static void list(Context ctx, int anio, int mes, ListCallback cb) {
        try {
            cb.onSuccess(LocalRepository.getInstance(ctx).listHouseholds());
        } catch (Exception e) {
            cb.onError();
        }
    }

    public static void create(Context ctx, String nombre, CreateCallback cb) {
        int id = LocalRepository.getInstance(ctx).createHousehold(nombre);
        cb.onSuccess(id, "LOCAL" + id);
    }

    public static void join(Context ctx, String codigo, String rol, SimpleCallback cb) {
        cb.onSuccess();
    }

    public static void leave(Context ctx, int householdId, SimpleCallback cb) {
        cb.onSuccess();
    }

    public static void invite(Context ctx, int householdId, InviteCallback cb) {
        cb.onSuccess("LOCAL" + householdId);
    }
}
