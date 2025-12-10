package com.example.finanzas.data.api;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.example.finanzas.data.local.LocalRepository;

public class BudgetService {

    public interface GetCb { void onOk(double monto); void onFail(); }
    public interface SimpleCb { void onOk(); void onFail(); }

    public static void get(Context ctx, int anio, int mes, GetCb cb) {
        new Handler(Looper.getMainLooper()).post(() -> {
            double monto = LocalRepository.getInstance(ctx).getPresupuesto(anio, mes);
            cb.onOk(monto);
        });
    }

    public static void set(Context ctx, int anio, int mes, double monto, SimpleCb cb) {
        new Handler(Looper.getMainLooper()).post(() -> {
            LocalRepository.getInstance(ctx).setPresupuesto(anio, mes, monto);
            cb.onOk();
        });
    }
}
