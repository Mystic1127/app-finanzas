package com.example.finanzas.data.api;

import android.content.Context;

import androidx.annotation.Nullable;

import com.example.finanzas.data.local.LocalRepository;
import com.example.finanzas.data.model.Transaccion;

import java.util.List;

public class TransService {

    public interface ListCb   { void onOk(List<Transaccion> res); void onError(); }
    public interface SimpleCb { void onOk(int newId);      void onError(@Nullable String message); }
    public interface VoidCb   { void onOk();               void onError(@Nullable String message); }
    public interface FileCb   { void onOk(String path);    void onError(@Nullable String message); }

    public static void list(Context ctx, int anio, int mes, ListCb cb){
        try {
            List<Transaccion> list = LocalRepository.getInstance(ctx).listTransacciones(anio, mes);
            cb.onOk(list);
        } catch (Exception e) {
            cb.onError();
        }
    }

    public static void create(Context ctx, long categoriaId, boolean esIngreso, double monto,
                              String nota, long fecha, SimpleCb cb){
        try {
            int id = LocalRepository.getInstance(ctx)
                    .createTransaccion((int) categoriaId, esIngreso, monto, nota, fecha);
            cb.onOk(id);
        } catch (Exception e) {
            cb.onError(e.getMessage());
        }
    }

    public static void update(Context ctx, long id, long categoriaId, boolean esIngreso,
                              double monto, String nota, long fecha, VoidCb cb){
        boolean ok = LocalRepository.getInstance(ctx)
                .updateTransaccion((int) id, (int) categoriaId, esIngreso, monto, nota, fecha);
        if (ok) cb.onOk(); else cb.onError("No se pudo actualizar");
    }

    public static void delete(Context ctx, long id, VoidCb cb){
        boolean ok = LocalRepository.getInstance(ctx).deleteTransaccion((int) id);
        if (ok) cb.onOk(); else cb.onError("No se pudo eliminar");
    }

    public static void exportToTxt(Context ctx, FileCb cb) {
        try {
            String path = LocalRepository.getInstance(ctx).exportTransacciones();
            cb.onOk(path);
        } catch (Exception e) {
            cb.onError(e.getMessage());
        }
    }
}
