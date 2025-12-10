package com.example.finanzas.data.api;

import android.content.Context;

import com.example.finanzas.data.local.LocalRepository;
import com.example.finanzas.data.model.CategoryBudgetSummary;

import java.util.List;

public class CategoryBudgetService {

    public interface ListCb { void onOk(List<CategoryBudgetSummary> items); void onFail(); }
    public interface SaveCb { void onOk(); void onFail(); }

    public static void list(Context ctx, int anio, int mes, ListCb cb) {
        try {
            List<CategoryBudgetSummary> list = LocalRepository.getInstance(ctx).listPresupuestosCategoria(anio, mes);
            cb.onOk(list);
        } catch (Exception e) {
            cb.onFail();
        }
    }

    public static void save(Context ctx, int anio, int mes, List<?> items, SaveCb cb) {
        try {
            LocalRepository.getInstance(ctx).savePresupuestosCategoria(anio, mes, items);
            cb.onOk();
        } catch (Exception e) {
            cb.onFail();
        }
    }
}
