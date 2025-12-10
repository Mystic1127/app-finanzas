package com.example.finanzas.data.api;

import android.content.Context;

import com.example.finanzas.data.local.LocalRepository;
import com.example.finanzas.data.model.Categoria;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CategoryStore {

    public interface Callback {
        void onReady(List<Categoria> cats);
        void onError();
    }

    private static List<Categoria> cache = null;
    private static boolean loading = false;
    private static final List<Callback> pending = new ArrayList<>();

    public static synchronized void loadOnce(Context ctx, Callback cb) {
        if (cache != null) { cb.onReady(cache); return; }
        pending.add(cb);
        if (loading) return;
        loading = true;

        try {
            List<Categoria> out = LocalRepository.getInstance(ctx).listCategorias();
            cache = out;
            loading = false;
            for (Callback k : pending) k.onReady(cache);
        } catch (Exception e) {
            loading = false;
            for (Callback k : pending) k.onError();
        } finally {
            pending.clear();
        }
    }

    public static synchronized Categoria createCategoria(Context ctx, String nombre, boolean esIngreso) {
        try {
            int id = LocalRepository.getInstance(ctx).createCategoria(nombre, esIngreso);
            Categoria nueva = new Categoria(id, nombre, esIngreso);
            if (cache != null) {
                cache = new ArrayList<>(cache);
                cache.add(nueva);
                Collections.sort(cache, Comparator.comparing(c -> c.nombre == null ? "" : c.nombre));
            }
            return nueva;
        } catch (Exception e) {
            return null;
        }
    }
}
