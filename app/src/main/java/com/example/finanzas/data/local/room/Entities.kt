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

@Entity(tableName = "categorias")
data class CategoriaEntity(
    @PrimaryKey val id: Int,
    val nombre: String,
    @ColumnInfo(name = "es_ingreso") val esIngreso: Int
)

@Entity(
    tableName = "transacciones",
    indices = [Index("fecha"), Index("categoria_id")]
)
data class TransaccionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "categoria_id") val categoriaId: Int,
    @ColumnInfo(name = "es_ingreso") val esIngreso: Int,
    val monto: Double,
    val fecha: Long,
    val nota: String?
)

@Entity(tableName = "presupuestos", primaryKeys = ["anio", "mes"])
data class PresupuestoEntity(
    val anio: Int,
    val mes: Int,
    val monto: Double
)

@Entity(tableName = "presupuestos_categoria", primaryKeys = ["anio", "mes", "categoria_id"])
data class PresupuestoCategoriaEntity(
    val anio: Int,
    val mes: Int,
    @ColumnInfo(name = "categoria_id") val categoriaId: Int,
    val monto: Double
)

@Entity(tableName = "metas")
data class MetaEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val titulo: String,
    @ColumnInfo(name = "monto_objetivo") val montoObjetivo: Double,
    @ColumnInfo(name = "monto_actual") val montoActual: Double,
    @ColumnInfo(name = "fecha_objetivo") val fechaObjetivo: Long?
)

@Entity(tableName = "metas_hitos")
data class MetaHitoEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "meta_id") val metaId: Int,
    val titulo: String,
    @ColumnInfo(name = "monto_planificado") val montoPlanificado: Double,
    @ColumnInfo(name = "fecha_objetivo") val fechaObjetivo: Long?,
    val notificar: Int,
    @ColumnInfo(name = "dias_recordatorio") val diasRecordatorio: Int,
    val completado: Int
)

@Entity(tableName = "recordatorios", indices = [Index("fecha_vencimiento")])
data class RecordatorioEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
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

@Entity(tableName = "import_jobs")
data class ImportJobEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String,
    val tipo: String,
    val estado: String,
    val lineas: String?
)

@Entity(tableName = "import_rules")
data class ImportRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val patron: String,
    @ColumnInfo(name = "es_ingreso") val esIngreso: Int,
    @ColumnInfo(name = "categoria_id") val categoriaId: Int?,
    val nota: String?
)
