package com.example.finanzas.data.local.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "users", indices = [Index(value = ["email"], unique = true)])
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String?,
    val email: String,
    val password: String
)

@Entity(tableName = "categorias", indices = [Index("user_id")])
data class CategoriaEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "user_id", defaultValue = "0") val userId: Int = 0,
    val nombre: String,
    @ColumnInfo(name = "es_ingreso") val esIngreso: Int
)

@Entity(tableName = "transacciones", indices = [Index("user_id"), Index("fecha"), Index("categoria_id")])
data class TransaccionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "user_id") val userId: Int,
    @ColumnInfo(name = "categoria_id") val categoriaId: Int,
    @ColumnInfo(name = "es_ingreso") val esIngreso: Int,
    val monto: Double,
    @ColumnInfo(defaultValue = "'PEN'") val moneda: String = "PEN",
    val fecha: Long,
    val nota: String?
)

@Entity(tableName = "presupuestos", primaryKeys = ["user_id", "anio", "mes"])
data class PresupuestoEntity(
    @ColumnInfo(name = "user_id") val userId: Int,
    val anio: Int,
    val mes: Int,
    val monto: Double
)

@Entity(tableName = "presupuestos_categoria", primaryKeys = ["user_id", "anio", "mes", "categoria_id"])
data class PresupuestoCategoriaEntity(
    @ColumnInfo(name = "user_id") val userId: Int,
    val anio: Int,
    val mes: Int,
    @ColumnInfo(name = "categoria_id") val categoriaId: Int,
    val monto: Double
)

@Entity(tableName = "metas", indices = [Index("user_id")])
data class MetaEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "user_id") val userId: Int,
    val titulo: String,
    @ColumnInfo(name = "monto_objetivo") val montoObjetivo: Double,
    @ColumnInfo(name = "monto_actual") val montoActual: Double,
    @ColumnInfo(name = "fecha_objetivo") val fechaObjetivo: Long?
)

@Entity(tableName = "metas_hitos", indices = [Index("user_id"), Index("meta_id")])
data class MetaHitoEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "user_id") val userId: Int,
    @ColumnInfo(name = "meta_id") val metaId: Int,
    val titulo: String,
    @ColumnInfo(name = "monto_planificado") val montoPlanificado: Double,
    @ColumnInfo(name = "fecha_objetivo") val fechaObjetivo: Long?,
    val notificar: Int,
    @ColumnInfo(name = "dias_recordatorio") val diasRecordatorio: Int,
    val completado: Int
)

@Entity(tableName = "recordatorios", indices = [Index("user_id"), Index("fecha_vencimiento")])
data class RecordatorioEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "user_id") val userId: Int,
    val titulo: String,
    val monto: Double,
    @ColumnInfo(name = "fecha_vencimiento") val fechaVencimiento: Long,
    val pagado: Int,
    @ColumnInfo(name = "categoria_id") val categoriaId: Int?,
    @ColumnInfo(name = "hora_recordatorio") val horaRecordatorio: String?,
    val frecuencia: String?,
    val notificar: Int,
    @ColumnInfo(name = "dias_recordatorio") val diasRecordatorio: Int,
    @ColumnInfo(name = "google_event_id") val googleEventId: String?,
    @ColumnInfo(name = "notification_id") val notificationId: String?
)

@Entity(tableName = "import_jobs", indices = [Index("user_id")])
data class ImportJobEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "user_id") val userId: Int,
    val nombre: String,
    val tipo: String,
    val estado: String,
    val lineas: String?
)

@Entity(tableName = "import_rules", indices = [Index("user_id")])
data class ImportRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "user_id") val userId: Int,
    val patron: String,
    @ColumnInfo(name = "es_ingreso") val esIngreso: Int,
    @ColumnInfo(name = "categoria_id") val categoriaId: Int?,
    val nota: String?
)
