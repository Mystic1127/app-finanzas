package com.example.finanzas.data.api;

import com.example.finanzas.data.model.CategoryBudgetSummary;
import com.example.finanzas.data.model.CategoryChartSlice;
import com.example.finanzas.data.model.ConversionSummary;
import com.example.finanzas.data.model.DashboardModulePref;
import com.example.finanzas.data.model.ExchangeRate;
import com.example.finanzas.data.model.GamificationChallenge;
import com.example.finanzas.data.model.GoalMilestone;
import com.example.finanzas.data.model.HomeSummary;
import com.example.finanzas.data.model.CategorySuggestion;
import com.example.finanzas.data.model.ImportJob;
import com.example.finanzas.data.model.ImportRule;
import com.example.finanzas.data.model.MonthlyTrendPoint;
import com.example.finanzas.data.model.PaymentReminder;
import com.example.finanzas.data.model.SavingsGoal;
import com.example.finanzas.data.model.Transaccion;
import com.example.finanzas.data.model.TravelPreference;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class JsonMapper {

    private static final SimpleDateFormat DF = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    public static List<Transaccion> mapTransList(JSONObject res) throws Exception {
        List<Transaccion> out = new ArrayList<>();
        if (res == null || !res.optBoolean("ok", false)) return out;

        JSONArray arr = res.optJSONArray("items");
        if (arr == null) return out;

        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;

            int id = safeInt(o, "id");
            int categoriaId = safeInt(o, "categoria_id");
            String categoriaNombre = o.optString("categoria_nombre", "");
            boolean esIngreso = safeBool(o, "es_ingreso");
            double monto = safeDouble(o, "monto");
            String moneda = o.optString("moneda", "PEN");
            String sFecha = o.optString("fecha", "");
            String nota = o.optString("nota", "");

            Date fecha = parseDateOrNull(sFecha);
            Transaccion t = new Transaccion(
                    id,
                    categoriaId,
                    categoriaNombre,
                    esIngreso,
                    Math.abs(monto),
                    moneda,
                    fecha,
                    nota
            );
            out.add(t);
        }
        return out;
    }

    public static HomeSummary mapHomeSummary(JSONObject res) {
        HomeSummary summary = new HomeSummary();
        if (res == null) return summary;

        summary.setAnio(res.optInt("anio", 0));
        summary.setMes(res.optInt("mes", 0));

        JSONObject totales = res.optJSONObject("totales");
        if (totales != null) {
            summary.setIngresos(totales.optDouble("ingresos", 0));
            summary.setGastos(totales.optDouble("gastos", 0));
            summary.setSaldo(totales.optDouble("saldo", 0));
            summary.setIngresosRecurrentes(totales.optDouble("ingresos_recurrentes", summary.getIngresos()));
            summary.setBalanceVisibleMes(totales.optDouble("balance_visible_mes", summary.getSaldo()));
            summary.setBalanceOperativoMes(totales.optDouble("balance_operativo_mes", summary.getIngresosRecurrentes() - summary.getGastos()));
        }

        JSONObject presupuesto = res.optJSONObject("presupuesto");
        if (presupuesto != null) {
            summary.setPresupuestoMonto(presupuesto.optDouble("monto", 0));
            summary.setPresupuestoRestante(presupuesto.optDouble("restante", 0));
            summary.setPresupuestoPorcentaje(presupuesto.optDouble("porcentaje", 0));
            summary.setPresupuestoExcedido(presupuesto.optBoolean("excedido", false));
            summary.setGastoProyectado(presupuesto.optDouble("gasto_proyectado", 0));
            summary.setGastoPromedioDiario(presupuesto.optDouble("gasto_promedio_diario", 0));
            summary.setDiasRestantes(presupuesto.optInt("dias_restantes", 0));
            summary.setRiesgoPresupuesto(presupuesto.optString("riesgo", ""));
        }

        JSONArray arrBud = res.optJSONArray("presupuestos_categoria");
        if (arrBud != null) {
            for (int i = 0; i < arrBud.length(); i++) {
                JSONObject o = arrBud.optJSONObject(i);
                if (o == null) continue;
                CategoryBudgetSummary b = new CategoryBudgetSummary();
                b.setCategoriaId(o.optInt("categoria_id", 0));
                b.setCategoriaNombre(o.optString("categoria_nombre", ""));
                b.setLimite(o.optDouble("limite", 0));
                b.setGastado(o.optDouble("gastado", 0));
                b.setDisponible(o.optDouble("disponible", 0));
                b.setPorcentaje(o.optDouble("porcentaje", 0));
                b.setAlerta(o.optBoolean("alerta", false));
                summary.getPresupuestosCategoria().add(b);
            }
        }

        JSONArray arrGoals = res.optJSONArray("metas");
        if (arrGoals != null) {
            for (int i = 0; i < arrGoals.length(); i++) {
                JSONObject o = arrGoals.optJSONObject(i);
                if (o == null) continue;
                SavingsGoal g = new SavingsGoal();
                g.setId(o.optInt("id", 0));
                g.setTitulo(o.optString("titulo", ""));
                g.setMontoObjetivo(o.optDouble("monto_objetivo", 0));
                g.setMontoActual(o.optDouble("monto_actual", 0));
                g.setProgreso(o.optDouble("progreso", 0));
                g.setFechaObjetivo(parseDateOrNull(o.optString("fecha_objetivo", null)));
                JSONArray arrHitos = o.optJSONArray("hitos");
                if (arrHitos != null) {
                    for (int j = 0; j < arrHitos.length(); j++) {
                        JSONObject h = arrHitos.optJSONObject(j);
                        if (h == null) continue;
                        GoalMilestone milestone = new GoalMilestone();
                        milestone.setId(h.optInt("id", 0));
                        milestone.setMetaId(h.optInt("meta_id", 0));
                        milestone.setTitulo(h.optString("titulo", ""));
                        milestone.setMontoPlanificado(h.optDouble("monto_planificado", 0));
                        milestone.setFechaObjetivo(parseDateOrNull(h.optString("fecha_objetivo", null)));
                        milestone.setNotificar(h.optBoolean("notificar", false));
                        milestone.setDiasRecordatorio(h.optInt("dias_recordatorio", 0));
                        milestone.setCompletado(h.optBoolean("completado", false));
                        if (h.has("dias_restantes") && !h.isNull("dias_restantes")) {
                            milestone.setDiasRestantes(h.optInt("dias_restantes"));
                        }
                        g.getHitos().add(milestone);
                    }
                }
                summary.getMetas().add(g);
            }
        }

        JSONArray arrRem = res.optJSONArray("recordatorios");
        if (arrRem != null) {
            for (int i = 0; i < arrRem.length(); i++) {
                JSONObject o = arrRem.optJSONObject(i);
                if (o == null) continue;
                PaymentReminder r = new PaymentReminder();
                r.setId(o.optInt("id", 0));
                r.setTitulo(o.optString("titulo", ""));
                r.setMonto(o.optDouble("monto", 0));
                r.setFechaVencimiento(parseDateOrNull(o.optString("fecha_vencimiento", null)));
                r.setDiasRestantes(o.optInt("dias_restantes", 0));
                r.setPagado(o.optBoolean("pagado", false));
                if (!o.isNull("categoria_id")) {
                    r.setCategoriaId(o.optInt("categoria_id"));
                }
                r.setHoraRecordatorio(o.optString("hora_recordatorio", null));
                r.setFrecuencia(o.optString("frecuencia", null));
                r.setNotificar(o.optBoolean("notificar", false));
                r.setDiasRecordatorio(o.optInt("dias_recordatorio", 0));
                if (o.has("google_event_id") && !o.isNull("google_event_id")) {
                    r.setGoogleEventId(o.optString("google_event_id", null));
                }
                if (o.has("notification_id") && !o.isNull("notification_id")) {
                    r.setNotificationId(o.optString("notification_id", null));
                }
                summary.getRecordatorios().add(r);
            }
        }

        JSONArray arrAlerts = res.optJSONArray("alertas");
        if (arrAlerts != null) {
            for (int i = 0; i < arrAlerts.length(); i++) {
                String alert = arrAlerts.optString(i, null);
                if (alert != null && !alert.isEmpty()) {
                    summary.getAlertas().add(alert);
                }
            }
        }

        JSONArray arrPredict = res.optJSONArray("alertas_predictivas");
        if (arrPredict != null) {
            for (int i = 0; i < arrPredict.length(); i++) {
                String alert = arrPredict.optString(i, null);
                if (alert != null && !alert.isEmpty()) {
                    summary.getAlertasPredictivas().add(alert);
                }
            }
        }

        JSONArray arrNotes = res.optJSONArray("notas_informativas");
        if (arrNotes != null) {
            for (int i = 0; i < arrNotes.length(); i++) {
                String note = arrNotes.optString(i, null);
                if (note != null && !note.isEmpty()) {
                    summary.getNotasInformativas().add(note);
                }
            }
        }

        JSONArray arrChartCat = res.optJSONArray("chart_categorias");
        if (arrChartCat != null) {
            for (int i = 0; i < arrChartCat.length(); i++) {
                JSONObject o = arrChartCat.optJSONObject(i);
                if (o == null) continue;
                CategoryChartSlice slice = new CategoryChartSlice();
                slice.setCategoriaNombre(o.optString("categoria", ""));
                slice.setGastado(o.optDouble("gastado", 0));
                slice.setPresupuesto(o.optDouble("presupuesto", 0));
                summary.getChartCategorias().add(slice);
            }
        }

        JSONArray arrTrend = res.optJSONArray("chart_tendencia");
        if (arrTrend != null) {
            for (int i = 0; i < arrTrend.length(); i++) {
                JSONObject o = arrTrend.optJSONObject(i);
                if (o == null) continue;
                MonthlyTrendPoint point = new MonthlyTrendPoint();
                point.setAnio(o.optInt("anio", 0));
                point.setMes(o.optInt("mes", 0));
                point.setEtiqueta(o.optString("label", ""));
                point.setIngresos(o.optDouble("ingresos", 0));
                point.setGastos(o.optDouble("gastos", 0));
                point.setSaldo(o.optDouble("saldo", 0));
                summary.getTendenciaMensual().add(point);
            }
        }

        JSONObject imp = res.optJSONObject("importaciones");
        if (imp != null) {
            summary.setImportacionesPendientes(imp.optInt("pendientes", 0));
        }

        JSONArray arrCambios = res.optJSONArray("tipos_cambio");
        if (arrCambios != null) {
            for (int i = 0; i < arrCambios.length(); i++) {
                JSONObject o = arrCambios.optJSONObject(i);
                if (o == null) continue;
                ExchangeRate rate = new ExchangeRate();
                rate.setMonedaBase(o.optString("moneda_base", null));
                rate.setMonedaDestino(o.optString("moneda_destino", null));
                rate.setTasa(o.optDouble("tasa", 0));
                rate.setActualizadoEn(o.optString("actualizado_en", null));
                summary.getTiposCambio().add(rate);
            }
        }

        JSONObject conversion = res.optJSONObject("conversion");
        if (conversion != null) {
            ConversionSummary conv = new ConversionSummary();
            conv.setEnabled(conversion.optBoolean("enabled", false));
            conv.setMonedaBase(conversion.optString("moneda_base", null));
            conv.setMonedaDestino(conversion.optString("moneda_destino", null));
            conv.setTasa(conversion.optDouble("tasa", 0));
            conv.setSaldo(conversion.optDouble("saldo", 0));
            conv.setIngresos(conversion.optDouble("ingresos", 0));
            conv.setGastos(conversion.optDouble("gastos", 0));
            conv.setActualizadoEn(conversion.optString("actualizado_en", null));
            summary.setConversion(conv);
        }

        JSONObject prefs = res.optJSONObject("preferencias");
        if (prefs != null) {
            JSONArray arrDash = prefs.optJSONArray("dashboard");
            if (arrDash != null) {
                for (int i = 0; i < arrDash.length(); i++) {
                    JSONObject o = arrDash.optJSONObject(i);
                    if (o == null) continue;
                    DashboardModulePref pref = new DashboardModulePref();
                    pref.setId(o.optString("id", null));
                    pref.setVisible(o.optBoolean("visible", true));
                    summary.getDashboardPreferencias().add(pref);
                }
            }
            JSONObject travel = prefs.optJSONObject("travel");
            if (travel != null) {
                TravelPreference tp = new TravelPreference();
                tp.setEnabled(travel.optBoolean("enabled", false));
                tp.setBase(travel.optString("base", null));
                tp.setCurrency(travel.optString("currency", null));
                tp.setRate(travel.optDouble("rate", 0));
                summary.setTravelPreference(tp);
            }
        }

        JSONObject gamificacion = res.optJSONObject("gamificacion");
        if (gamificacion != null) {
            JSONArray retos = gamificacion.optJSONArray("retos");
            if (retos != null) {
                for (int i = 0; i < retos.length(); i++) {
                    JSONObject o = retos.optJSONObject(i);
                    if (o == null) continue;
                    GamificationChallenge reto = new GamificationChallenge();
                    reto.setId(o.optString("id", null));
                    reto.setTitulo(o.optString("titulo", ""));
                    reto.setDescripcion(o.optString("descripcion", ""));
                    summary.getGamificacionRetos().add(reto);
                }
            }
        }

        return summary;
    }

    public static List<CategoryBudgetSummary> mapCategoryBudgets(JSONObject res) {
        List<CategoryBudgetSummary> out = new ArrayList<>();
        if (res == null) return out;

        JSONArray arr = res.optJSONArray("items");
        if (arr == null) return out;

        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            CategoryBudgetSummary b = new CategoryBudgetSummary();
            b.setCategoriaId(o.optInt("categoria_id", 0));
            b.setCategoriaNombre(o.optString("categoria_nombre", ""));
            b.setLimite(o.optDouble("limite", 0));
            b.setGastado(o.optDouble("gastado", 0));
            b.setDisponible(o.optDouble("disponible", 0));
            b.setPorcentaje(o.optDouble("porcentaje", 0));
            b.setAlerta(o.optBoolean("alerta", false));
            out.add(b);
        }

        return out;
    }

    public static List<SavingsGoal> mapGoals(JSONObject res) {
        List<SavingsGoal> out = new ArrayList<>();
        if (res == null) return out;

        JSONArray arr = res.optJSONArray("items");
        if (arr == null) return out;

        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            SavingsGoal g = new SavingsGoal();
            g.setId(o.optInt("id", 0));
            g.setTitulo(o.optString("titulo", ""));
            g.setMontoObjetivo(o.optDouble("monto_objetivo", 0));
            g.setMontoActual(o.optDouble("monto_actual", 0));
            g.setProgreso(o.optDouble("progreso", 0));
            g.setFechaObjetivo(parseDateOrNull(o.optString("fecha_objetivo", null)));
            JSONArray arrHitos = o.optJSONArray("hitos");
            if (arrHitos != null) {
                for (int j = 0; j < arrHitos.length(); j++) {
                    JSONObject h = arrHitos.optJSONObject(j);
                    if (h == null) continue;
                    GoalMilestone milestone = new GoalMilestone();
                    milestone.setId(h.optInt("id", 0));
                    milestone.setMetaId(h.optInt("meta_id", 0));
                    milestone.setTitulo(h.optString("titulo", ""));
                    milestone.setMontoPlanificado(h.optDouble("monto_planificado", 0));
                    milestone.setFechaObjetivo(parseDateOrNull(h.optString("fecha_objetivo", null)));
                    milestone.setNotificar(h.optBoolean("notificar", false));
                    milestone.setDiasRecordatorio(h.optInt("dias_recordatorio", 0));
                    milestone.setCompletado(h.optBoolean("completado", false));
                    if (h.has("dias_restantes") && !h.isNull("dias_restantes")) {
                        milestone.setDiasRestantes(h.optInt("dias_restantes"));
                    }
                    g.getHitos().add(milestone);
                }
            }
            out.add(g);
        }

        return out;
    }

    public static List<PaymentReminder> mapReminders(JSONObject res) {
        List<PaymentReminder> out = new ArrayList<>();
        if (res == null) return out;

        JSONArray arr = res.optJSONArray("items");
        if (arr == null) return out;

        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            PaymentReminder r = new PaymentReminder();
            r.setId(o.optInt("id", 0));
            r.setTitulo(o.optString("titulo", ""));
            r.setMonto(o.optDouble("monto", 0));
            r.setFechaVencimiento(parseDateOrNull(o.optString("fecha_vencimiento", null)));
            r.setDiasRestantes(o.optInt("dias_restantes", 0));
            r.setPagado(o.optBoolean("pagado", false));
            if (!o.isNull("categoria_id")) {
                r.setCategoriaId(o.optInt("categoria_id"));
            }
            r.setHoraRecordatorio(o.optString("hora_recordatorio", null));
            r.setFrecuencia(o.optString("frecuencia", null));
            r.setNotificar(o.optBoolean("notificar", false));
            r.setDiasRecordatorio(o.optInt("dias_recordatorio", 0));
            if (o.has("google_event_id") && !o.isNull("google_event_id")) {
                r.setGoogleEventId(o.optString("google_event_id", null));
            }
            if (o.has("notification_id") && !o.isNull("notification_id")) {
                r.setNotificationId(o.optString("notification_id", null));
            }
            out.add(r);
        }

        return out;
    }

    public static List<ImportJob> mapImportJobs(JSONObject res) {
        List<ImportJob> out = new ArrayList<>();
        if (res == null) return out;
        JSONArray arr = res.optJSONArray("items");
        if (arr == null) return out;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            ImportJob job = new ImportJob();
            job.setId(o.optInt("id", 0));
            job.setNombreArchivo(o.optString("nombre_archivo", ""));
            job.setEstado(o.optString("estado", ""));
            job.setTotal(o.optInt("total", 0));
            job.setProcesados(o.optInt("procesados", 0));
            job.setErrores(o.optInt("errores", 0));
            job.setPendientes(o.optInt("pendientes", 0));
            job.setProcesadas(o.optInt("procesadas", 0));
            job.setOmitidas(o.optInt("omitidas", 0));
            job.setMensajeError(o.optString("mensaje_error", null));
            job.setCreatedAt(parseDateOrNull(o.optString("created_at", null)));
            job.setProcessedAt(parseDateOrNull(o.optString("processed_at", null)));
            out.add(job);
        }
        return out;
    }

    public static List<ImportRule> mapImportRules(JSONObject res) {
        List<ImportRule> out = new ArrayList<>();
        if (res == null) return out;
        JSONArray arr = res.optJSONArray("items");
        if (arr == null) return out;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            ImportRule r = new ImportRule();
            r.setId(o.optInt("id", 0));
            r.setPatron(o.optString("patron", ""));
            if (!o.isNull("categoria_id")) {
                r.setCategoriaId(o.optInt("categoria_id"));
            }
            r.setIngreso(o.optBoolean("es_ingreso", false));
            r.setDescripcion(o.optString("descripcion", null));
            out.add(r);
        }
        return out;
    }

    public static CategorySuggestion mapSuggestion(JSONObject res) {
        CategorySuggestion sug = new CategorySuggestion();
        if (res == null) return sug;
        if (res.optBoolean("ok", false)) {
            JSONObject data = res;
            if (res.has("categoria_id") || res.has("categoria_nombre")) {
                // already top-level suggestion
                assignSuggestionFields(sug, data);
            } else {
                JSONObject body = res.optJSONObject("data");
                if (body != null) {
                    assignSuggestionFields(sug, body);
                }
            }
        } else {
            assignSuggestionFields(sug, res);
        }
        return sug;
    }

    private static void assignSuggestionFields(CategorySuggestion sug, JSONObject data) {
        if (data == null) return;
        if (!data.isNull("categoria_id")) {
            sug.setCategoriaId(data.optInt("categoria_id"));
        }
        sug.setCategoriaNombre(data.optString("categoria_nombre", null));
        sug.setMotivo(data.optString("motivo", ""));
        if (data.has("confidence")) {
            try { sug.setConfidence(data.getDouble("confidence")); }
            catch (Exception ignored) {}
        }
    }

    private static boolean safeBool(JSONObject o, String key) {
        Object v = o.opt(key);
        if (v instanceof Boolean) return (Boolean) v;
        if (v instanceof Number) return ((Number) v).intValue() == 1;
        if (v instanceof String) {
            String s = ((String) v).trim();
            if ("1".equals(s)) return true;
            if ("0".equals(s)) return false;
            return "true".equalsIgnoreCase(s);
        }
        return false;
    }

    private static int safeInt(JSONObject o, String key) {
        try { return o.getInt(key); } catch (Exception e) {
            try { return Integer.parseInt(o.optString(key, "0")); } catch (Exception ex) { return 0; }
        }
    }

    private static double safeDouble(JSONObject o, String key) {
        try { return o.getDouble(key); } catch (Exception e) {
            try { return Double.parseDouble(o.optString(key, "0")); } catch (Exception ex) { return 0d; }
        }
    }

    private static Date parseDateOrNull(String raw) {
        if (raw == null || raw.isEmpty()) return null;
        try {
            synchronized (DF) {
                return DF.parse(raw);
            }
        } catch (ParseException e) {
            try {
                return new Date(Date.parse(raw));
            } catch (Exception ex) {
                return null;
            }
        }
    }
}
