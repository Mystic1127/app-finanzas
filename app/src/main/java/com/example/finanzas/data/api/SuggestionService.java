package com.example.finanzas.data.api;

import android.content.Context;

import com.example.finanzas.data.local.LocalRepository;
import com.example.finanzas.data.model.CategorySuggestion;
import com.example.finanzas.data.model.Transaccion;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SuggestionService {

    public interface Callback {
        void onSuccess(CategorySuggestion suggestion);
        void onError();
    }

    public static void suggest(Context ctx, String nota, boolean esIngreso, double monto, Callback cb) {
        try {
            List<Transaccion> trans = LocalRepository.getInstance(ctx).listTransacciones(
                    java.util.Calendar.getInstance().get(java.util.Calendar.YEAR),
                    java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1
            );
            Map<Integer, Integer> counts = new HashMap<>();
            for (Transaccion t : trans) {
                if (t.isEsIngreso() != esIngreso) continue;
                if (nota != null && !nota.isEmpty() && t.getNota() != null && t.getNota().toLowerCase().contains(nota.toLowerCase())) {
                    counts.put(t.getCategoriaId(), counts.getOrDefault(t.getCategoriaId(), 0) + 2);
                }
                if (monto > 0 && Math.abs(t.getMonto() - monto) < 1) {
                    counts.put(t.getCategoriaId(), counts.getOrDefault(t.getCategoriaId(), 0) + 1);
                }
            }
            int bestCat = counts.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(-1);
            if (bestCat > 0) {
                CategorySuggestion suggestion = new CategorySuggestion();
                suggestion.setCategoriaId(bestCat);
                suggestion.setCategoriaNombre(null);
                suggestion.setConfidence(0.8);
                cb.onSuccess(suggestion);
            } else {
                cb.onSuccess(new CategorySuggestion());
            }
        } catch (Exception e) {
            cb.onError();
        }
    }
}
