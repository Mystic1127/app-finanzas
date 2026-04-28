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
    version = 3,
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
        fun build(context: Context): AppRoomDatabase {
            return Room.databaseBuilder(context, AppRoomDatabase::class.java, "finanzas_local.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
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
