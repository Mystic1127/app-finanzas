package com.example.finanzas.data.local.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.finanzas.util.PasswordSecurity

@Database(
    entities = [
        UserEntity::class,
        CategoriaEntity::class,
        TransaccionEntity::class,
        PresupuestoEntity::class,
        PresupuestoCategoriaEntity::class,
        MetaEntity::class,
        MetaHitoEntity::class,
        RecordatorioEntity::class,
        ImportJobEntity::class,
        ImportRuleEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppRoomDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun categoriaDao(): CategoriaDao
    abstract fun transaccionDao(): TransaccionDao
    abstract fun presupuestoDao(): PresupuestoDao
    abstract fun presupuestoCategoriaDao(): PresupuestoCategoriaDao
    abstract fun metaDao(): MetaDao
    abstract fun metaHitoDao(): MetaHitoDao
    abstract fun recordatorioDao(): RecordatorioDao
    abstract fun importJobDao(): ImportJobDao
    abstract fun importRuleDao(): ImportRuleDao

    companion object {
        @JvmStatic
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val c = db.query("SELECT id, password FROM users")
                c.use {
                    while (it.moveToNext()) {
                        val id = it.getInt(0)
                        val pass = it.getString(1) ?: continue
                        if (PasswordSecurity.looksLikeHashed(pass)) continue
                        val hashed = PasswordSecurity.hashPassword(pass)
                        db.execSQL("UPDATE users SET password=? WHERE id=?", arrayOf(hashed, id))
                    }
                }
            }
        }

        @JvmStatic
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transacciones_fecha ON transacciones(fecha)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transacciones_categoria_id ON transacciones(categoria_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_recordatorios_fecha_vencimiento ON recordatorios(fecha_vencimiento)")
            }
        }

        @JvmStatic
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS users_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        nombre TEXT,
                        email TEXT NOT NULL,
                        password TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO users_new (id, nombre, email, password)
                    SELECT id, nombre, COALESCE(email, ''), COALESCE(password, '')
                    FROM users
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE users")
                db.execSQL("ALTER TABLE users_new RENAME TO users")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_users_email ON users(email)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS categorias_new (
                        id INTEGER PRIMARY KEY NOT NULL,
                        nombre TEXT NOT NULL,
                        es_ingreso INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO categorias_new (id, nombre, es_ingreso)
                    SELECT id, COALESCE(nombre, ''), COALESCE(es_ingreso, 0)
                    FROM categorias
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE categorias")
                db.execSQL("ALTER TABLE categorias_new RENAME TO categorias")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS transacciones_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        categoria_id INTEGER NOT NULL,
                        es_ingreso INTEGER NOT NULL,
                        monto REAL NOT NULL,
                        fecha INTEGER NOT NULL,
                        nota TEXT
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO transacciones_new (id, categoria_id, es_ingreso, monto, fecha, nota)
                    SELECT id, COALESCE(categoria_id, 0), COALESCE(es_ingreso, 0), COALESCE(monto, 0), COALESCE(fecha, 0), nota
                    FROM transacciones
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE transacciones")
                db.execSQL("ALTER TABLE transacciones_new RENAME TO transacciones")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transacciones_fecha ON transacciones(fecha)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transacciones_categoria_id ON transacciones(categoria_id)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS presupuestos_new (
                        anio INTEGER NOT NULL,
                        mes INTEGER NOT NULL,
                        monto REAL NOT NULL,
                        PRIMARY KEY(anio, mes)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO presupuestos_new (anio, mes, monto)
                    SELECT COALESCE(anio, 0), COALESCE(mes, 0), COALESCE(monto, 0)
                    FROM presupuestos
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE presupuestos")
                db.execSQL("ALTER TABLE presupuestos_new RENAME TO presupuestos")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS presupuestos_categoria_new (
                        anio INTEGER NOT NULL,
                        mes INTEGER NOT NULL,
                        categoria_id INTEGER NOT NULL,
                        monto REAL NOT NULL,
                        PRIMARY KEY(anio, mes, categoria_id)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO presupuestos_categoria_new (anio, mes, categoria_id, monto)
                    SELECT COALESCE(anio, 0), COALESCE(mes, 0), COALESCE(categoria_id, 0), COALESCE(monto, 0)
                    FROM presupuestos_categoria
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE presupuestos_categoria")
                db.execSQL("ALTER TABLE presupuestos_categoria_new RENAME TO presupuestos_categoria")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS metas_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        titulo TEXT NOT NULL,
                        monto_objetivo REAL NOT NULL,
                        monto_actual REAL NOT NULL,
                        fecha_objetivo INTEGER
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO metas_new (id, titulo, monto_objetivo, monto_actual, fecha_objetivo)
                    SELECT id, COALESCE(titulo, ''), COALESCE(monto_objetivo, 0), COALESCE(monto_actual, 0), fecha_objetivo
                    FROM metas
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE metas")
                db.execSQL("ALTER TABLE metas_new RENAME TO metas")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS metas_hitos_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        meta_id INTEGER NOT NULL,
                        titulo TEXT NOT NULL,
                        monto_planificado REAL NOT NULL,
                        fecha_objetivo INTEGER,
                        notificar INTEGER NOT NULL,
                        dias_recordatorio INTEGER NOT NULL,
                        completado INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO metas_hitos_new (id, meta_id, titulo, monto_planificado, fecha_objetivo, notificar, dias_recordatorio, completado)
                    SELECT id, COALESCE(meta_id, 0), COALESCE(titulo, ''), COALESCE(monto_planificado, 0), fecha_objetivo, COALESCE(notificar, 0), COALESCE(dias_recordatorio, 0), COALESCE(completado, 0)
                    FROM metas_hitos
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE metas_hitos")
                db.execSQL("ALTER TABLE metas_hitos_new RENAME TO metas_hitos")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS recordatorios_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        titulo TEXT NOT NULL,
                        monto REAL NOT NULL,
                        fecha_vencimiento INTEGER NOT NULL,
                        pagado INTEGER NOT NULL,
                        categoria_id INTEGER,
                        hora_recordatorio TEXT,
                        frecuencia TEXT,
                        notificar INTEGER NOT NULL,
                        dias_recordatorio INTEGER NOT NULL,
                        google_event_id TEXT,
                        notification_id TEXT
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO recordatorios_new (id, titulo, monto, fecha_vencimiento, pagado, categoria_id, hora_recordatorio, frecuencia, notificar, dias_recordatorio, google_event_id, notification_id)
                    SELECT id, COALESCE(titulo, ''), COALESCE(monto, 0), COALESCE(fecha_vencimiento, 0), COALESCE(pagado, 0), categoria_id, hora_recordatorio, frecuencia, COALESCE(notificar, 0), COALESCE(dias_recordatorio, 0), google_event_id, notification_id
                    FROM recordatorios
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE recordatorios")
                db.execSQL("ALTER TABLE recordatorios_new RENAME TO recordatorios")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_recordatorios_fecha_vencimiento ON recordatorios(fecha_vencimiento)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS import_jobs_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        nombre TEXT NOT NULL,
                        tipo TEXT NOT NULL,
                        estado TEXT NOT NULL,
                        lineas TEXT
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO import_jobs_new (id, nombre, tipo, estado, lineas)
                    SELECT id, COALESCE(nombre, ''), COALESCE(tipo, ''), COALESCE(estado, ''), lineas
                    FROM import_jobs
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE import_jobs")
                db.execSQL("ALTER TABLE import_jobs_new RENAME TO import_jobs")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS import_rules_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        patron TEXT NOT NULL,
                        es_ingreso INTEGER NOT NULL,
                        categoria_id INTEGER,
                        nota TEXT
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO import_rules_new (id, patron, es_ingreso, categoria_id, nota)
                    SELECT id, COALESCE(patron, ''), COALESCE(es_ingreso, 0), categoria_id, nota
                    FROM import_rules
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE import_rules")
                db.execSQL("ALTER TABLE import_rules_new RENAME TO import_rules")
            }
        }

        @JvmStatic
        fun build(context: Context): AppRoomDatabase {
            return Room.databaseBuilder(context, AppRoomDatabase::class.java, "finanzas_local.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        db.execSQL("INSERT INTO categorias (id, nombre, es_ingreso) VALUES (1, 'Salario', 1)")
                        db.execSQL("INSERT INTO categorias (id, nombre, es_ingreso) VALUES (2, 'Inversión', 1)")
                        db.execSQL("INSERT INTO categorias (id, nombre, es_ingreso) VALUES (3, 'Alimentación', 0)")
                        db.execSQL("INSERT INTO categorias (id, nombre, es_ingreso) VALUES (4, 'Vivienda', 0)")
                        db.execSQL("INSERT INTO categorias (id, nombre, es_ingreso) VALUES (5, 'Transporte', 0)")
                        db.execSQL("INSERT INTO categorias (id, nombre, es_ingreso) VALUES (6, 'Entretenimiento', 0)")
                    }
                })
                .build()
        }
    }
}
