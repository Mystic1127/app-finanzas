package com.example.finanzas.data.api;

import android.content.Context;

import com.example.finanzas.data.local.LocalRepository;
import com.example.finanzas.data.model.HomeSummary;

public class DashboardService {

    public interface SummaryCb { void onOk(HomeSummary summary); void onError(); }

    public static void getSummary(Context ctx, int anio, int mes, SummaryCb cb) {
        try {
            HomeSummary summary = LocalRepository.getInstance(ctx).buildHomeSummary(anio, mes);
            cb.onOk(summary);
        } catch (Exception e) {
            cb.onError();
        }
    }
}
