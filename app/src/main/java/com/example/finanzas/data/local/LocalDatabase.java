package com.example.finanzas.data.local;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class LocalDatabase extends SQLiteOpenHelper {

    public static final String DB_NAME = "finanzas_local.db";
    public static final int DB_VERSION = 1;

    private static LocalDatabase instance;
    private final Context context;

    public static synchronized LocalDatabase getInstance(Context context) {
        if (instance == null) {
            instance = new LocalDatabase(context.getApplicationContext());
        }
        return instance;
    }

    private LocalDatabase(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE users (id INTEGER PRIMARY KEY AUTOINCREMENT, nombre TEXT, email TEXT UNIQUE, password TEXT)");
        db.execSQL("CREATE TABLE categorias (id INTEGER PRIMARY KEY, nombre TEXT, es_ingreso INTEGER)");
        db.execSQL("CREATE TABLE transacciones (id INTEGER PRIMARY KEY AUTOINCREMENT, categoria_id INTEGER, es_ingreso INTEGER, monto REAL, fecha INTEGER, nota TEXT)");
        db.execSQL("CREATE TABLE presupuestos (anio INTEGER, mes INTEGER, monto REAL, PRIMARY KEY(anio, mes))");
        db.execSQL("CREATE TABLE presupuestos_categoria (anio INTEGER, mes INTEGER, categoria_id INTEGER, monto REAL, PRIMARY KEY(anio, mes, categoria_id))");
        db.execSQL("CREATE TABLE metas (id INTEGER PRIMARY KEY AUTOINCREMENT, titulo TEXT, monto_objetivo REAL, monto_actual REAL, fecha_objetivo INTEGER)");
        db.execSQL("CREATE TABLE metas_hitos (id INTEGER PRIMARY KEY AUTOINCREMENT, meta_id INTEGER, titulo TEXT, monto_planificado REAL, fecha_objetivo INTEGER, notificar INTEGER, dias_recordatorio INTEGER, completado INTEGER)");
        db.execSQL("CREATE TABLE recordatorios (id INTEGER PRIMARY KEY AUTOINCREMENT, titulo TEXT, monto REAL, fecha_vencimiento INTEGER, pagado INTEGER, categoria_id INTEGER, hora_recordatorio TEXT, frecuencia TEXT, notificar INTEGER, dias_recordatorio INTEGER, google_event_id TEXT, notification_id TEXT)");
        db.execSQL("CREATE TABLE import_jobs (id INTEGER PRIMARY KEY AUTOINCREMENT, nombre TEXT, tipo TEXT, estado TEXT, lineas TEXT)");
        db.execSQL("CREATE TABLE import_rules (id INTEGER PRIMARY KEY AUTOINCREMENT, patron TEXT, es_ingreso INTEGER, categoria_id INTEGER, nota TEXT)");
        seedCategorias(db);
    }

    private void seedCategorias(SQLiteDatabase db) {
        db.execSQL("INSERT INTO categorias (id, nombre, es_ingreso) VALUES (1, 'Salario', 1)");
        db.execSQL("INSERT INTO categorias (id, nombre, es_ingreso) VALUES (2, 'Inversión', 1)");
        db.execSQL("INSERT INTO categorias (id, nombre, es_ingreso) VALUES (3, 'Alimentación', 0)");
        db.execSQL("INSERT INTO categorias (id, nombre, es_ingreso) VALUES (4, 'Vivienda', 0)");
        db.execSQL("INSERT INTO categorias (id, nombre, es_ingreso) VALUES (5, 'Transporte', 0)");
        db.execSQL("INSERT INTO categorias (id, nombre, es_ingreso) VALUES (6, 'Entretenimiento', 0)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // For this migration we can simply recreate everything
        db.execSQL("DROP TABLE IF EXISTS users");
        db.execSQL("DROP TABLE IF EXISTS categorias");
        db.execSQL("DROP TABLE IF EXISTS transacciones");
        db.execSQL("DROP TABLE IF EXISTS presupuestos");
        db.execSQL("DROP TABLE IF EXISTS presupuestos_categoria");
        db.execSQL("DROP TABLE IF EXISTS metas");
        db.execSQL("DROP TABLE IF EXISTS metas_hitos");
        db.execSQL("DROP TABLE IF EXISTS recordatorios");
        db.execSQL("DROP TABLE IF EXISTS import_jobs");
        db.execSQL("DROP TABLE IF EXISTS import_rules");
        onCreate(db);
    }

    public Context getContext() {
        return context;
    }
}
