package com.example.finanzas.data.local;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.finanzas.data.model.CategoryBudgetInput;
import com.example.finanzas.data.model.CategoryBudgetSummary;
import com.example.finanzas.data.model.Categoria;
import com.example.finanzas.data.model.GoalMilestone;
import com.example.finanzas.data.model.HomeSummary;
import com.example.finanzas.data.model.ImportJob;
import com.example.finanzas.data.model.ImportRule;
import com.example.finanzas.data.model.PaymentReminder;
import com.example.finanzas.data.model.SavingsGoal;
import com.example.finanzas.data.model.Transaccion;
import com.example.finanzas.data.model.TransaccionFiltro;

import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LocalRepository {

    private static final SimpleDateFormat DF = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private static LocalRepository instance;
    private final LocalDatabase helper;

    public static synchronized LocalRepository getInstance(Context context) {
        if (instance == null) {
            instance = new LocalRepository(LocalDatabase.getInstance(context));
        }
        return instance;
    }

    private LocalRepository(LocalDatabase helper) {
        this.helper = helper;
    }

    // region Autenticación
    public boolean registerUser(String nombre, String email, String password, int[] outId) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("nombre", nombre);
        cv.put("email", email);
        cv.put("password", password);
        long id = db.insertWithOnConflict("users", null, cv, SQLiteDatabase.CONFLICT_IGNORE);
        if (id <= 0) return false;
        if (outId != null && outId.length > 0) outId[0] = (int) id;
        return true;
    }

    public boolean login(String email, String password, UserHolder holder) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT id, nombre, email FROM users WHERE email=? AND password=?", new String[]{email, password})) {
            if (c.moveToFirst()) {
                if (holder != null) {
                    holder.id = c.getInt(0);
                    holder.nombre = c.getString(1);
                    holder.email = c.getString(2);
                }
                return true;
            }
        }
        return false;
    }

    public static class UserHolder {
        public int id;
        public String nombre;
        public String email;
    }
    // endregion

    // region Categorías
    public int createCategoria(@NonNull String nombre, boolean esIngreso) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("nombre", nombre);
        cv.put("es_ingreso", esIngreso ? 1 : 0);
        long id = db.insert("categorias", null, cv);
        if (id == -1) throw new IllegalStateException("No se pudo crear la categoría");
        return (int) id;
    }

    public List<Categoria> listCategorias() {
        List<Categoria> out = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT id, nombre, es_ingreso FROM categorias ORDER BY nombre", null)) {
            while (c.moveToNext()) {
                out.add(new Categoria(c.getInt(0), c.getString(1), c.getInt(2) == 1));
            }
        }
        return out;
    }
    // endregion

    // region Transacciones
    public List<Transaccion> listTransacciones(int anio, int mes) {
        return listTransacciones(anio, mes, null);
    }

    public List<Transaccion> listTransacciones(int anio, int mes, @Nullable TransaccionFiltro filtro) {
        Calendar cal = Calendar.getInstance();
        cal.set(anio, mes - 1, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long start = cal.getTimeInMillis();
        cal.add(Calendar.MONTH, 1);
        long end = cal.getTimeInMillis();

        if (filtro != null) {
            if (filtro.getFechaInicio() != null) start = filtro.getFechaInicio();
            if (filtro.getFechaFin() != null) end = filtro.getFechaFin();
        }

        List<Transaccion> out = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        StringBuilder sql = new StringBuilder("SELECT t.id, t.categoria_id, c.nombre, t.es_ingreso, t.monto, t.fecha, t.nota " +
                "FROM transacciones t JOIN categorias c ON c.id = t.categoria_id " +
                "WHERE t.fecha >= ? AND t.fecha < ?");
        List<String> args = new ArrayList<>();
        args.add(String.valueOf(start));
        args.add(String.valueOf(end));

        if (filtro != null && filtro.getCategoriaId() != null) {
            sql.append(" AND t.categoria_id = ?");
            args.add(String.valueOf(filtro.getCategoriaId()));
        }

        String order = "t.fecha";
        if (filtro != null && filtro.getOrden() != null) {
            switch (filtro.getOrden()) {
                case NOMBRE:
                    order = "t.nota";
                    break;
                case CATEGORIA:
                    order = "c.nombre";
                    break;
                default:
                    order = "t.fecha";
                    break;
            }
        }
        order = order + (filtro != null && filtro.isAscendente() ? " ASC" : " DESC");
        sql.append(" ORDER BY ").append(order);

        try (Cursor c = db.rawQuery(sql.toString(), args.toArray(new String[0]))) {
            while (c.moveToNext()) {
                Transaccion t = new Transaccion(
                        c.getInt(0),
                        c.getInt(1),
                        c.getString(2),
                        c.getInt(3) == 1,
                        c.getDouble(4),
                        new Date(c.getLong(5)),
                        c.getString(6)
                );
                out.add(t);
            }
        }
        return out;
    }

    public int createTransaccion(int categoriaId, boolean esIngreso, double monto, String nota) {
        return createTransaccion(categoriaId, esIngreso, monto, nota, System.currentTimeMillis());
    }

    public int createTransaccion(int categoriaId, boolean esIngreso, double monto, String nota, long fecha) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("categoria_id", categoriaId);
        cv.put("es_ingreso", esIngreso ? 1 : 0);
        cv.put("monto", monto);
        cv.put("fecha", fecha);
        cv.put("nota", nota);
        return (int) db.insert("transacciones", null, cv);
    }

    public boolean updateTransaccion(int id, int categoriaId, boolean esIngreso, double monto, String nota, long fecha) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("categoria_id", categoriaId);
        cv.put("es_ingreso", esIngreso ? 1 : 0);
        cv.put("monto", monto);
        cv.put("fecha", fecha);
        cv.put("nota", nota);
        return db.update("transacciones", cv, "id=?", new String[]{String.valueOf(id)}) > 0;
    }

    public List<Transaccion> listTodasTransacciones() {
        List<Transaccion> out = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        String sql = "SELECT t.id, t.categoria_id, c.nombre, t.es_ingreso, t.monto, t.fecha, t.nota " +
                "FROM transacciones t JOIN categorias c ON c.id = t.categoria_id ORDER BY t.fecha DESC";
        try (Cursor c = db.rawQuery(sql, null)) {
            while (c.moveToNext()) {
                Transaccion t = new Transaccion(
                        c.getInt(0),
                        c.getInt(1),
                        c.getString(2),
                        c.getInt(3) == 1,
                        c.getDouble(4),
                        new Date(c.getLong(5)),
                        c.getString(6)
                );
                out.add(t);
            }
        }
        return out;
    }

    public String exportTransacciones() throws Exception {
        List<Transaccion> transacciones = listTodasTransacciones();
        if (transacciones.isEmpty()) {
            throw new IllegalStateException("No hay transacciones para exportar");
        }

        File dir = new File(helper.getContext().getFilesDir(), "exports");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IllegalStateException("No se pudo crear el directorio de exportación");
        }

        SimpleDateFormat fileDf = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US);
        SimpleDateFormat dateDf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        File outFile = new File(dir, "transacciones-" + fileDf.format(new Date()) + ".txt");

        StringBuilder sb = new StringBuilder();
        sb.append("ID | Fecha | Categoría | Tipo | Monto | Nota\n");
        for (Transaccion t : transacciones) {
            String fecha = dateDf.format(t.getFecha());
            String tipo = t.isEsIngreso() ? "Ingreso" : "Gasto";
            sb.append(t.getId())
                    .append(" | ")
                    .append(fecha)
                    .append(" | ")
                    .append(t.getCategoriaNombre())
                    .append(" | ")
                    .append(tipo)
                    .append(" | ")
                    .append(t.getMonto())
                    .append(" | ")
                    .append(t.getNota() == null ? "" : t.getNota())
                    .append('\n');
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outFile))) {
            writer.write(sb.toString());
        }

        return outFile.getAbsolutePath();
    }

    public boolean deleteTransaccion(int id) {
        SQLiteDatabase db = helper.getWritableDatabase();
        return db.delete("transacciones", "id=?", new String[]{String.valueOf(id)}) > 0;
    }
    // endregion

    // region Presupuestos
    public double getPresupuesto(int anio, int mes) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT monto FROM presupuestos WHERE anio=? AND mes=?", new String[]{String.valueOf(anio), String.valueOf(mes)})) {
            if (c.moveToFirst()) return c.getDouble(0);
        }
        return 0.0;
    }

    public void setPresupuesto(int anio, int mes, double monto) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("anio", anio);
        cv.put("mes", mes);
        cv.put("monto", monto);
        db.insertWithOnConflict("presupuestos", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public List<CategoryBudgetSummary> listPresupuestosCategoria(int anio, int mes) {
        SQLiteDatabase db = helper.getReadableDatabase();
        List<CategoryBudgetSummary> out = new ArrayList<>();
        String sql = "SELECT pc.categoria_id, c.nombre, pc.monto FROM presupuestos_categoria pc JOIN categorias c ON c.id = pc.categoria_id WHERE pc.anio=? AND pc.mes=?";
        try (Cursor c = db.rawQuery(sql, new String[]{String.valueOf(anio), String.valueOf(mes)})) {
            while (c.moveToNext()) {
                CategoryBudgetSummary b = new CategoryBudgetSummary();
                b.setCategoriaId(c.getInt(0));
                b.setCategoriaNombre(c.getString(1));
                b.setLimite(c.getDouble(2));
                out.add(b);
            }
        }

        // calcular gasto real
        Calendar cal = Calendar.getInstance();
        cal.set(anio, mes - 1, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long start = cal.getTimeInMillis();
        cal.add(Calendar.MONTH, 1);
        long end = cal.getTimeInMillis();

        String gastoSql = "SELECT categoria_id, SUM(monto) FROM transacciones WHERE es_ingreso=0 AND fecha>=? AND fecha<? GROUP BY categoria_id";
        try (Cursor c = db.rawQuery(gastoSql, new String[]{String.valueOf(start), String.valueOf(end)})) {
            while (c.moveToNext()) {
                int catId = c.getInt(0);
                double gastado = c.getDouble(1);
                CategoryBudgetSummary target = null;
                for (CategoryBudgetSummary b : out) {
                    if (b.getCategoriaId() == catId) { target = b; break; }
                }
                if (target == null) {
                    target = new CategoryBudgetSummary();
                    target.setCategoriaId(catId);
                    target.setLimite(0);
                    target.setCategoriaNombre(findCategoriaNombre(catId));
                    out.add(target);
                }
                target.setGastado(gastado);
                target.setDisponible(target.getLimite() - gastado);
                double porcentaje = target.getLimite() > 0 ? (gastado / target.getLimite()) * 100.0 : 0;
                target.setPorcentaje(porcentaje);
                target.setAlerta(target.getLimite() > 0 && gastado > target.getLimite());
            }
        }
        return out;
    }

    public void savePresupuestosCategoria(int anio, int mes, List<?> items) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            for (Object raw : items) {
                int categoriaId = 0;
                double monto = 0;
                if (raw instanceof CategoryBudgetSummary) {
                    CategoryBudgetSummary item = (CategoryBudgetSummary) raw;
                    categoriaId = item.getCategoriaId();
                    monto = item.getLimite();
                } else if (raw instanceof CategoryBudgetInput) {
                    CategoryBudgetInput item = (CategoryBudgetInput) raw;
                    categoriaId = item.getCategoriaId();
                    monto = item.getMonto();
                }
                if (categoriaId <= 0) continue;
                ContentValues cv = new ContentValues();
                cv.put("anio", anio);
                cv.put("mes", mes);
                cv.put("categoria_id", categoriaId);
                cv.put("monto", monto);
                db.insertWithOnConflict("presupuestos_categoria", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }
    // endregion

    // region Metas
    public List<SavingsGoal> listGoals() {
        List<SavingsGoal> out = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT id, titulo, monto_objetivo, monto_actual, fecha_objetivo FROM metas ORDER BY id DESC", null)) {
            while (c.moveToNext()) {
                SavingsGoal g = new SavingsGoal();
                g.setId(c.getInt(0));
                g.setTitulo(c.getString(1));
                g.setMontoObjetivo(c.getDouble(2));
                g.setMontoActual(c.getDouble(3));
                long fecha = c.getLong(4);
                if (fecha > 0) g.setFechaObjetivo(new Date(fecha));
                g.setProgreso(g.getMontoObjetivo() > 0 ? (g.getMontoActual() / g.getMontoObjetivo()) * 100.0 : 0);
                loadMilestonesForGoal(db, g);
                out.add(g);
            }
        }
        return out;
    }

    private void loadMilestonesForGoal(SQLiteDatabase db, SavingsGoal g) {
        try (Cursor c = db.rawQuery("SELECT id, titulo, monto_planificado, fecha_objetivo, notificar, dias_recordatorio, completado FROM metas_hitos WHERE meta_id=?", new String[]{String.valueOf(g.getId())})) {
            while (c.moveToNext()) {
                GoalMilestone m = new GoalMilestone();
                m.setId(c.getInt(0));
                m.setMetaId(g.getId());
                m.setTitulo(c.getString(1));
                m.setMontoPlanificado(c.getDouble(2));
                long fecha = c.getLong(3);
                if (fecha > 0) m.setFechaObjetivo(new Date(fecha));
                m.setNotificar(c.getInt(4) == 1);
                m.setDiasRecordatorio(c.getInt(5));
                m.setCompletado(c.getInt(6) == 1);
                g.getHitos().add(m);
            }
        }
    }

    public boolean saveGoal(int id, String titulo, double objetivo, double actual, String fechaStr) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("titulo", titulo);
        cv.put("monto_objetivo", objetivo);
        cv.put("monto_actual", actual);
        if (!TextUtils.isEmpty(fechaStr)) {
            try {
                Date parsed = DF.parse(fechaStr);
                if (parsed != null) cv.put("fecha_objetivo", parsed.getTime());
            } catch (ParseException ignore) { }
        }
        if (id > 0) {
            return db.update("metas", cv, "id=?", new String[]{String.valueOf(id)}) > 0;
        }
        return db.insert("metas", null, cv) > 0;
    }

    public boolean deleteGoal(int id) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.delete("metas_hitos", "meta_id=?", new String[]{String.valueOf(id)});
        return db.delete("metas", "id=?", new String[]{String.valueOf(id)}) > 0;
    }

    public int saveMilestone(int id, int metaId, String titulo, double monto, String fechaStr, boolean notificar, int dias, boolean completado) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("meta_id", metaId);
        cv.put("titulo", titulo);
        cv.put("monto_planificado", monto);
        cv.put("notificar", notificar ? 1 : 0);
        cv.put("dias_recordatorio", dias);
        cv.put("completado", completado ? 1 : 0);
        if (!TextUtils.isEmpty(fechaStr)) {
            try {
                Date parsed = DF.parse(fechaStr);
                if (parsed != null) cv.put("fecha_objetivo", parsed.getTime());
            } catch (ParseException ignore) { }
        }
        if (id > 0) {
            db.update("metas_hitos", cv, "id=?", new String[]{String.valueOf(id)});
            return id;
        }
        return (int) db.insert("metas_hitos", null, cv);
    }

    public boolean deleteMilestone(int id) {
        SQLiteDatabase db = helper.getWritableDatabase();
        return db.delete("metas_hitos", "id=?", new String[]{String.valueOf(id)}) > 0;
    }
    // endregion

    // region Recordatorios
    public List<PaymentReminder> listReminders(boolean includePaid) {
        List<PaymentReminder> out = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        String where = includePaid ? "" : "WHERE pagado=0";
        try (Cursor c = db.rawQuery("SELECT id, titulo, monto, fecha_vencimiento, pagado, categoria_id, hora_recordatorio, frecuencia, notificar, dias_recordatorio, google_event_id, notification_id FROM recordatorios " + where + " ORDER BY fecha_vencimiento ASC", null)) {
            while (c.moveToNext()) {
                PaymentReminder r = new PaymentReminder();
                r.setId(c.getInt(0));
                r.setTitulo(c.getString(1));
                r.setMonto(c.getDouble(2));
                long fecha = c.getLong(3);
                if (fecha > 0) r.setFechaVencimiento(new Date(fecha));
                r.setPagado(c.getInt(4) == 1);
                if (!c.isNull(5)) r.setCategoriaId(c.getInt(5));
                r.setHoraRecordatorio(c.getString(6));
                r.setFrecuencia(c.getString(7));
                r.setNotificar(c.getInt(8) == 1);
                r.setDiasRecordatorio(c.getInt(9));
                r.setGoogleEventId(c.getString(10));
                r.setNotificationId(c.getString(11));
                out.add(r);
            }
        }
        return out;
    }

    public boolean saveReminder(PaymentReminder reminder) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("titulo", reminder.getTitulo());
        cv.put("monto", reminder.getMonto());
        cv.put("fecha_vencimiento", reminder.getFechaVencimiento() != null ? reminder.getFechaVencimiento().getTime() : 0);
        cv.put("pagado", reminder.isPagado() ? 1 : 0);
        if (reminder.getCategoriaId() != null) cv.put("categoria_id", reminder.getCategoriaId()); else cv.putNull("categoria_id");
        cv.put("hora_recordatorio", reminder.getHoraRecordatorio());
        cv.put("frecuencia", reminder.getFrecuencia());
        cv.put("notificar", reminder.isNotificar() ? 1 : 0);
        cv.put("dias_recordatorio", reminder.getDiasRecordatorio());
        cv.put("google_event_id", reminder.getGoogleEventId());
        cv.put("notification_id", reminder.getNotificationId());
        if (reminder.getId() > 0) {
            return db.update("recordatorios", cv, "id=?", new String[]{String.valueOf(reminder.getId())}) > 0;
        }
        reminder.setId((int) db.insert("recordatorios", null, cv));
        return reminder.getId() > 0;
    }

    public boolean markReminderPaid(int id, boolean paid) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("pagado", paid ? 1 : 0);
        return db.update("recordatorios", cv, "id=?", new String[]{String.valueOf(id)}) > 0;
    }

    public boolean deleteReminder(int id) {
        SQLiteDatabase db = helper.getWritableDatabase();
        return db.delete("recordatorios", "id=?", new String[]{String.valueOf(id)}) > 0;
    }
    // endregion

    // region Importaciones
    public List<ImportJob> listImports() {
        List<ImportJob> out = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT id, nombre, tipo, estado FROM import_jobs ORDER BY id DESC", null)) {
            while (c.moveToNext()) {
                ImportJob j = new ImportJob();
                j.setId(c.getInt(0));
                j.setNombreArchivo(c.getString(1));
                j.setTipo(c.getString(2));
                j.setEstado(c.getString(3));
                out.add(j);
            }
        }
        return out;
    }

    public int createImport(String nombre, String tipo, String lineasJson) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("nombre", nombre);
        cv.put("tipo", tipo);
        cv.put("estado", "pendiente");
        cv.put("lineas", lineasJson);
        return (int) db.insert("import_jobs", null, cv);
    }

    public List<ImportRule> listImportRules() {
        List<ImportRule> out = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT id, patron, es_ingreso, categoria_id, nota FROM import_rules", null)) {
            while (c.moveToNext()) {
                ImportRule r = new ImportRule();
                r.setId(c.getInt(0));
                r.setPatron(c.getString(1));
                r.setEsIngreso(c.getInt(2) == 1);
                r.setCategoriaId(c.getInt(3));
                r.setNota(c.getString(4));
                out.add(r);
            }
        }
        return out;
    }

    public int saveImportRule(ImportRule rule) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("patron", rule.getPatron());
        cv.put("es_ingreso", rule.isEsIngreso() ? 1 : 0);
        cv.put("categoria_id", rule.getCategoriaId());
        cv.put("nota", rule.getNota());
        if (rule.getId() > 0) {
            db.update("import_rules", cv, "id=?", new String[]{String.valueOf(rule.getId())});
            return rule.getId();
        }
        return (int) db.insert("import_rules", null, cv);
    }

    public void deleteImportRule(int id) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.delete("import_rules", "id=?", new String[]{String.valueOf(id)});
    }

    public JSONObject processImport(int id) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("estado", "procesado");
        db.update("import_jobs", cv, "id=?", new String[]{String.valueOf(id)});

        JSONObject resp = new JSONObject();
        try {
            resp.put("procesados", 0);
            resp.put("errores", 0);
            resp.put("omitidos_saldo", 0);
        } catch (Exception ignore) { }
        return resp;
    }
    // endregion

    // region Dashboard
    public HomeSummary buildHomeSummary(int anio, int mes) {
        HomeSummary summary = new HomeSummary();
        summary.setAnio(anio);
        summary.setMes(mes);

        List<Transaccion> trans = listTransacciones(anio, mes);
        double ingresos = 0, gastos = 0;
        for (Transaccion t : trans) {
            if (t.isEsIngreso()) ingresos += t.getMonto(); else gastos += t.getMonto();
        }
        summary.setIngresos(ingresos);
        summary.setGastos(gastos);
        summary.setSaldo(ingresos - gastos);

        double presMonto = getPresupuesto(anio, mes);
        summary.setPresupuestoMonto(presMonto);
        double presRestante = presMonto - gastos;
        summary.setPresupuestoRestante(presRestante);
        summary.setPresupuestoPorcentaje(presMonto > 0 ? (gastos / presMonto) * 100.0 : 0);
        summary.setPresupuestoExcedido(presRestante < 0);
        summary.setGastoProyectado(gastos);
        summary.setGastoPromedioDiario(gastos / Math.max(1, Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH)));
        summary.setDiasRestantes(Math.max(0, Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH) - Calendar.getInstance().get(Calendar.DAY_OF_MONTH)));
        summary.getPresupuestosCategoria().addAll(listPresupuestosCategoria(anio, mes));
        summary.getMetas().addAll(listGoals());
        summary.getRecordatorios().addAll(listReminders(false));
        summary.setImportacionesPendientes(listImports().size());
        return summary;
    }
    // endregion

    private String findCategoriaNombre(int catId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT nombre FROM categorias WHERE id=?", new String[]{String.valueOf(catId)})) {
            if (c.moveToFirst()) return c.getString(0);
        }
        return "";
    }
}
