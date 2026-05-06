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
        RecurringTransactionEntity::class,
        MetaEntity::class,
        MetaHitoEntity::class,
        RecordatorioEntity::class,
        ImportJobEntity::class,
        ImportRuleEntity::class
    ],
    version = 9,
    exportSchema = false
)
abstract class AppRoomDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun categoriaDao(): CategoriaDao
    abstract fun transaccionDao(): TransaccionDao
    abstract fun presupuestoDao(): PresupuestoDao
    abstract fun presupuestoCategoriaDao(): PresupuestoCategoriaDao
    abstract fun recurringTransactionDao(): RecurringTransactionDao
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
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        db.execSQL("INSERT INTO categorias (id, user_id, nombre, es_ingreso) VALUES (1, 0, 'Salario', 1)")
                        db.execSQL("INSERT INTO categorias (id, user_id, nombre, es_ingreso) VALUES (2, 0, 'Inversión', 1)")
                        db.execSQL("INSERT INTO categorias (id, user_id, nombre, es_ingreso) VALUES (3, 0, 'Alimentación', 0)")
                        db.execSQL("INSERT INTO categorias (id, user_id, nombre, es_ingreso) VALUES (4, 0, 'Vivienda', 0)")
                        db.execSQL("INSERT INTO categorias (id, user_id, nombre, es_ingreso) VALUES (5, 0, 'Transporte', 0)")
                        db.execSQL("INSERT INTO categorias (id, user_id, nombre, es_ingreso) VALUES (6, 0, 'Entretenimiento', 0)")
                    }
                })
                .build()
        }

        @JvmStatic
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                rebuildUserScopedTables(db)
            }
        }

        @JvmStatic
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                if (!hasColumn(db, "transacciones", "moneda")) {
                    db.execSQL("ALTER TABLE transacciones ADD COLUMN moneda TEXT NOT NULL DEFAULT 'PEN'")
                }
            }
        }

        @JvmStatic
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                if (!hasColumn(db, "categorias", "user_id")) {
                    db.execSQL("ALTER TABLE categorias ADD COLUMN user_id INTEGER NOT NULL DEFAULT 0")
                }
                db.execSQL("CREATE INDEX IF NOT EXISTS index_categorias_user_id ON categorias(user_id)")
            }
        }

        @JvmStatic
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                if (!hasColumn(db, "transacciones", "account_type")) {
                    db.execSQL("ALTER TABLE transacciones ADD COLUMN account_type TEXT NOT NULL DEFAULT 'CARD'")
                }
                if (!hasColumn(db, "presupuestos", "moneda")) {
                    db.execSQL("ALTER TABLE presupuestos ADD COLUMN moneda TEXT NOT NULL DEFAULT 'PEN'")
                }
                if (!hasColumn(db, "presupuestos_categoria", "moneda")) {
                    db.execSQL("ALTER TABLE presupuestos_categoria ADD COLUMN moneda TEXT NOT NULL DEFAULT 'PEN'")
                }
                if (!hasColumn(db, "metas", "moneda")) {
                    db.execSQL("ALTER TABLE metas ADD COLUMN moneda TEXT NOT NULL DEFAULT 'PEN'")
                }
                if (!hasColumn(db, "metas_hitos", "moneda")) {
                    db.execSQL("ALTER TABLE metas_hitos ADD COLUMN moneda TEXT NOT NULL DEFAULT 'PEN'")
                }
                if (!hasColumn(db, "recordatorios", "moneda")) {
                    db.execSQL("ALTER TABLE recordatorios ADD COLUMN moneda TEXT NOT NULL DEFAULT 'PEN'")
                }
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transacciones_account_type ON transacciones(account_type)")
            }
        }

        @JvmStatic
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS transacciones_recurrentes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        user_id INTEGER NOT NULL,
                        source_transaction_id INTEGER NOT NULL,
                        frequency TEXT NOT NULL,
                        days_mask INTEGER NOT NULL DEFAULT 0,
                        is_active INTEGER NOT NULL DEFAULT 1,
                        is_transfer INTEGER NOT NULL,
                        category_id INTEGER NOT NULL,
                        is_income INTEGER NOT NULL,
                        amount REAL NOT NULL,
                        currency TEXT NOT NULL DEFAULT 'PEN',
                        account_type TEXT NOT NULL DEFAULT 'CARD',
                        destination_account_type TEXT,
                        note TEXT,
                        label_id TEXT,
                        first_date INTEGER NOT NULL,
                        last_generated_day TEXT
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transacciones_recurrentes_user_id ON transacciones_recurrentes(user_id)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_transacciones_recurrentes_source_transaction_id ON transacciones_recurrentes(source_transaction_id)")
            }
        }

        private fun rebuildUserScopedTables(db: SupportSQLiteDatabase) {
            rebuildTransacciones(db)
            rebuildPresupuestos(db)
            rebuildPresupuestosCategoria(db)
            rebuildMetas(db)
            rebuildMetasHitos(db)
            rebuildRecordatorios(db)
            rebuildImportJobs(db)
            rebuildImportRules(db)
        }

        private fun rebuildTransacciones(db: SupportSQLiteDatabase) {
            db.execSQL("DROP TABLE IF EXISTS transacciones_new")
            db.execSQL(
                """
                CREATE TABLE transacciones_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    user_id INTEGER NOT NULL,
                    categoria_id INTEGER NOT NULL,
                    es_ingreso INTEGER NOT NULL,
                    monto REAL NOT NULL,
                    fecha INTEGER NOT NULL,
                    nota TEXT
                )
                """.trimIndent()
            )
            val userExpr = if (hasColumn(db, "transacciones", "user_id")) "COALESCE(user_id, 1)" else "1"
            db.execSQL(
                """
                INSERT INTO transacciones_new (id, user_id, categoria_id, es_ingreso, monto, fecha, nota)
                SELECT id, $userExpr, COALESCE(categoria_id, 0), COALESCE(es_ingreso, 0), COALESCE(monto, 0), COALESCE(fecha, 0), nota
                FROM transacciones
                """.trimIndent()
            )
            db.execSQL("DROP TABLE transacciones")
            db.execSQL("ALTER TABLE transacciones_new RENAME TO transacciones")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_transacciones_user_id ON transacciones(user_id)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_transacciones_fecha ON transacciones(fecha)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_transacciones_categoria_id ON transacciones(categoria_id)")
        }

        private fun rebuildPresupuestos(db: SupportSQLiteDatabase) {
            db.execSQL("DROP TABLE IF EXISTS presupuestos_new")
            db.execSQL(
                """
                CREATE TABLE presupuestos_new (
                    user_id INTEGER NOT NULL,
                    anio INTEGER NOT NULL,
                    mes INTEGER NOT NULL,
                    monto REAL NOT NULL,
                    PRIMARY KEY(user_id, anio, mes)
                )
                """.trimIndent()
            )
            val userExpr = if (hasColumn(db, "presupuestos", "user_id")) "COALESCE(user_id, 1)" else "1"
            db.execSQL(
                """
                INSERT OR REPLACE INTO presupuestos_new (user_id, anio, mes, monto)
                SELECT $userExpr, COALESCE(anio, 0), COALESCE(mes, 0), COALESCE(monto, 0)
                FROM presupuestos
                """.trimIndent()
            )
            db.execSQL("DROP TABLE presupuestos")
            db.execSQL("ALTER TABLE presupuestos_new RENAME TO presupuestos")
        }

        private fun rebuildPresupuestosCategoria(db: SupportSQLiteDatabase) {
            db.execSQL("DROP TABLE IF EXISTS presupuestos_categoria_new")
            db.execSQL(
                """
                CREATE TABLE presupuestos_categoria_new (
                    user_id INTEGER NOT NULL,
                    anio INTEGER NOT NULL,
                    mes INTEGER NOT NULL,
                    categoria_id INTEGER NOT NULL,
                    monto REAL NOT NULL,
                    PRIMARY KEY(user_id, anio, mes, categoria_id)
                )
                """.trimIndent()
            )
            val userExpr = if (hasColumn(db, "presupuestos_categoria", "user_id")) "COALESCE(user_id, 1)" else "1"
            db.execSQL(
                """
                INSERT OR REPLACE INTO presupuestos_categoria_new (user_id, anio, mes, categoria_id, monto)
                SELECT $userExpr, COALESCE(anio, 0), COALESCE(mes, 0), COALESCE(categoria_id, 0), COALESCE(monto, 0)
                FROM presupuestos_categoria
                """.trimIndent()
            )
            db.execSQL("DROP TABLE presupuestos_categoria")
            db.execSQL("ALTER TABLE presupuestos_categoria_new RENAME TO presupuestos_categoria")
        }

        private fun rebuildMetas(db: SupportSQLiteDatabase) {
            db.execSQL("DROP TABLE IF EXISTS metas_new")
            db.execSQL(
                """
                CREATE TABLE metas_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    user_id INTEGER NOT NULL,
                    titulo TEXT NOT NULL,
                    monto_objetivo REAL NOT NULL,
                    monto_actual REAL NOT NULL,
                    fecha_objetivo INTEGER
                )
                """.trimIndent()
            )
            val userExpr = if (hasColumn(db, "metas", "user_id")) "COALESCE(user_id, 1)" else "1"
            db.execSQL(
                """
                INSERT INTO metas_new (id, user_id, titulo, monto_objetivo, monto_actual, fecha_objetivo)
                SELECT id, $userExpr, COALESCE(titulo, ''), COALESCE(monto_objetivo, 0), COALESCE(monto_actual, 0), fecha_objetivo
                FROM metas
                """.trimIndent()
            )
            db.execSQL("DROP TABLE metas")
            db.execSQL("ALTER TABLE metas_new RENAME TO metas")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_metas_user_id ON metas(user_id)")
        }

        private fun rebuildMetasHitos(db: SupportSQLiteDatabase) {
            db.execSQL("DROP TABLE IF EXISTS metas_hitos_new")
            db.execSQL(
                """
                CREATE TABLE metas_hitos_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    user_id INTEGER NOT NULL,
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
            val userExpr = if (hasColumn(db, "metas_hitos", "user_id")) "COALESCE(user_id, 1)" else "1"
            db.execSQL(
                """
                INSERT INTO metas_hitos_new (id, user_id, meta_id, titulo, monto_planificado, fecha_objetivo, notificar, dias_recordatorio, completado)
                SELECT id, $userExpr, COALESCE(meta_id, 0), COALESCE(titulo, ''), COALESCE(monto_planificado, 0), fecha_objetivo, COALESCE(notificar, 0), COALESCE(dias_recordatorio, 0), COALESCE(completado, 0)
                FROM metas_hitos
                """.trimIndent()
            )
            db.execSQL("DROP TABLE metas_hitos")
            db.execSQL("ALTER TABLE metas_hitos_new RENAME TO metas_hitos")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_metas_hitos_user_id ON metas_hitos(user_id)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_metas_hitos_meta_id ON metas_hitos(meta_id)")
        }

        private fun rebuildRecordatorios(db: SupportSQLiteDatabase) {
            db.execSQL("DROP TABLE IF EXISTS recordatorios_new")
            db.execSQL(
                """
                CREATE TABLE recordatorios_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    user_id INTEGER NOT NULL,
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
            val userExpr = if (hasColumn(db, "recordatorios", "user_id")) "COALESCE(user_id, 1)" else "1"
            db.execSQL(
                """
                INSERT INTO recordatorios_new (id, user_id, titulo, monto, fecha_vencimiento, pagado, categoria_id, hora_recordatorio, frecuencia, notificar, dias_recordatorio, google_event_id, notification_id)
                SELECT id, $userExpr, COALESCE(titulo, ''), COALESCE(monto, 0), COALESCE(fecha_vencimiento, 0), COALESCE(pagado, 0), categoria_id, hora_recordatorio, frecuencia, COALESCE(notificar, 0), COALESCE(dias_recordatorio, 0), google_event_id, notification_id
                FROM recordatorios
                """.trimIndent()
            )
            db.execSQL("DROP TABLE recordatorios")
            db.execSQL("ALTER TABLE recordatorios_new RENAME TO recordatorios")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_recordatorios_user_id ON recordatorios(user_id)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_recordatorios_fecha_vencimiento ON recordatorios(fecha_vencimiento)")
        }

        private fun rebuildImportJobs(db: SupportSQLiteDatabase) {
            db.execSQL("DROP TABLE IF EXISTS import_jobs_new")
            db.execSQL(
                """
                CREATE TABLE import_jobs_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    user_id INTEGER NOT NULL,
                    nombre TEXT NOT NULL,
                    tipo TEXT NOT NULL,
                    estado TEXT NOT NULL,
                    lineas TEXT
                )
                """.trimIndent()
            )
            val userExpr = if (hasColumn(db, "import_jobs", "user_id")) "COALESCE(user_id, 1)" else "1"
            db.execSQL(
                """
                INSERT INTO import_jobs_new (id, user_id, nombre, tipo, estado, lineas)
                SELECT id, $userExpr, COALESCE(nombre, ''), COALESCE(tipo, ''), COALESCE(estado, ''), lineas
                FROM import_jobs
                """.trimIndent()
            )
            db.execSQL("DROP TABLE import_jobs")
            db.execSQL("ALTER TABLE import_jobs_new RENAME TO import_jobs")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_import_jobs_user_id ON import_jobs(user_id)")
        }

        private fun rebuildImportRules(db: SupportSQLiteDatabase) {
            db.execSQL("DROP TABLE IF EXISTS import_rules_new")
            db.execSQL(
                """
                CREATE TABLE import_rules_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    user_id INTEGER NOT NULL,
                    patron TEXT NOT NULL,
                    es_ingreso INTEGER NOT NULL,
                    categoria_id INTEGER,
                    nota TEXT
                )
                """.trimIndent()
            )
            val userExpr = if (hasColumn(db, "import_rules", "user_id")) "COALESCE(user_id, 1)" else "1"
            db.execSQL(
                """
                INSERT INTO import_rules_new (id, user_id, patron, es_ingreso, categoria_id, nota)
                SELECT id, $userExpr, COALESCE(patron, ''), COALESCE(es_ingreso, 0), categoria_id, nota
                FROM import_rules
                """.trimIndent()
            )
            db.execSQL("DROP TABLE import_rules")
            db.execSQL("ALTER TABLE import_rules_new RENAME TO import_rules")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_import_rules_user_id ON import_rules(user_id)")
        }

        private fun hasColumn(db: SupportSQLiteDatabase, tableName: String, columnName: String): Boolean {
            db.query("PRAGMA table_info($tableName)").use { cursor ->
                val nameIndex = cursor.getColumnIndex("name")
                while (cursor.moveToNext()) {
                    if (cursor.getString(nameIndex) == columnName) return true
                }
            }
            return false
        }
    }
}
