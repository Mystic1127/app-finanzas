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

    public static void list(Context ctx, int anio, int mes, ListCb cb){
        try {
            List<Transaccion> list = LocalRepository.getInstance(ctx).listTransacciones(anio, mes);
            cb.onOk(list);
        } catch (Exception e) {
            cb.onError();
        }
    }

    public static void create(Context ctx, long categoriaId, boolean esIngreso, double monto,
                              String nota, SimpleCb cb){
        try {
            int id = LocalRepository.getInstance(ctx).createTransaccion((int) categoriaId, esIngreso, monto, nota);
            cb.onOk(id);
        } catch (Exception e) {
            cb.onError(e.getMessage());
        }
    }

    public static void update(Context ctx, long id, long categoriaId, boolean esIngreso,
                              double monto, String nota, VoidCb cb){
        boolean ok = LocalRepository.getInstance(ctx)
                .updateTransaccion((int) id, (int) categoriaId, esIngreso, monto, nota);
        if (ok) cb.onOk(); else cb.onError("No se pudo actualizar");
    }

    public static void delete(Context ctx, long id, VoidCb cb){
        boolean ok = LocalRepository.getInstance(ctx).deleteTransaccion((int) id);
        if (ok) cb.onOk(); else cb.onError("No se pudo eliminar");
    }
}
