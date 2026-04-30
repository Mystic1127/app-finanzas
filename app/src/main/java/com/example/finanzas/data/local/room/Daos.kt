package com.example.finanzas.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(entity: UserEntity): Long

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    fun findByEmail(email: String): UserEntity?

    @Update
    fun update(entity: UserEntity)
}

@Dao
interface CategoriaDao {
    @Query("SELECT * FROM categorias ORDER BY nombre")
    fun listAll(): List<CategoriaEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insert(entity: CategoriaEntity): Long
}

@Dao
interface TransaccionDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insert(entity: TransaccionEntity): Long
    @Query(
        """
        UPDATE transacciones
        SET categoria_id=:categoriaId,
            es_ingreso=:esIngreso,
            monto=:monto,
            fecha=:fecha,
            nota=:nota
        WHERE id=:id AND user_id=:userId
        """
    )
    fun updateById(
        id: Int,
        userId: Int,
        categoriaId: Int,
        esIngreso: Int,
        monto: Double,
        fecha: Long,
        nota: String?
    ): Int
    @Query("DELETE FROM transacciones WHERE id=:id AND user_id=:userId") fun deleteById(id: Int, userId: Int): Int
    @Query("SELECT * FROM transacciones WHERE user_id=:userId ORDER BY fecha DESC") fun listAll(userId: Int): List<TransaccionEntity>
}

@Dao
interface PresupuestoDao {
    @Query("SELECT * FROM presupuestos WHERE user_id=:userId AND anio=:anio AND mes=:mes")
    fun find(userId: Int, anio: Int, mes: Int): PresupuestoEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) fun upsert(entity: PresupuestoEntity)
}

@Dao
interface PresupuestoCategoriaDao {
    @Query("SELECT * FROM presupuestos_categoria WHERE user_id=:userId AND anio=:anio AND mes=:mes")
    fun listByMonth(userId: Int, anio: Int, mes: Int): List<PresupuestoCategoriaEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) fun upsert(entity: PresupuestoCategoriaEntity)
}

@Dao
interface MetaDao {
    @Query("SELECT * FROM metas WHERE user_id=:userId ORDER BY id DESC") fun listAll(userId: Int): List<MetaEntity>
    @Query("SELECT COUNT(*) FROM metas WHERE id=:id AND user_id=:userId")
    fun countById(id: Int, userId: Int): Int
    @Insert(onConflict = OnConflictStrategy.ABORT) fun insert(entity: MetaEntity): Long
    @Query(
        """
        UPDATE metas
        SET titulo=:titulo,
            monto_objetivo=:montoObjetivo,
            monto_actual=:montoActual,
            fecha_objetivo=:fechaObjetivo
        WHERE id=:id AND user_id=:userId
        """
    )
    fun updateById(
        id: Int,
        userId: Int,
        titulo: String,
        montoObjetivo: Double,
        montoActual: Double,
        fechaObjetivo: Long?
    ): Int
    @Query("DELETE FROM metas WHERE id=:id AND user_id=:userId") fun deleteById(id: Int, userId: Int): Int
}

@Dao
interface MetaHitoDao {
    @Query("SELECT * FROM metas_hitos WHERE user_id=:userId AND meta_id=:metaId") fun listByMeta(userId: Int, metaId: Int): List<MetaHitoEntity>
    @Insert(onConflict = OnConflictStrategy.ABORT) fun insert(entity: MetaHitoEntity): Long
    @Query(
        """
        UPDATE metas_hitos
        SET meta_id=:metaId,
            titulo=:titulo,
            monto_planificado=:montoPlanificado,
            fecha_objetivo=:fechaObjetivo,
            notificar=:notificar,
            dias_recordatorio=:diasRecordatorio,
            completado=:completado
        WHERE id=:id AND user_id=:userId
        """
    )
    fun updateById(
        id: Int,
        userId: Int,
        metaId: Int,
        titulo: String,
        montoPlanificado: Double,
        fechaObjetivo: Long?,
        notificar: Int,
        diasRecordatorio: Int,
        completado: Int
    ): Int
    @Query("DELETE FROM metas_hitos WHERE id=:id AND user_id=:userId") fun deleteById(id: Int, userId: Int): Int
    @Query("DELETE FROM metas_hitos WHERE user_id=:userId AND meta_id=:metaId") fun deleteByMeta(userId: Int, metaId: Int): Int
}

@Dao
interface RecordatorioDao {
    @Query("SELECT * FROM recordatorios WHERE user_id=:userId ORDER BY fecha_vencimiento ASC") fun listAll(userId: Int): List<RecordatorioEntity>
    @Insert(onConflict = OnConflictStrategy.ABORT) fun insert(entity: RecordatorioEntity): Long
    @Query(
        """
        UPDATE recordatorios
        SET titulo=:titulo,
            monto=:monto,
            fecha_vencimiento=:fechaVencimiento,
            pagado=:pagado,
            categoria_id=:categoriaId,
            hora_recordatorio=:horaRecordatorio,
            frecuencia=:frecuencia,
            notificar=:notificar,
            dias_recordatorio=:diasRecordatorio,
            google_event_id=:googleEventId,
            notification_id=:notificationId
        WHERE id=:id AND user_id=:userId
        """
    )
    fun updateById(
        id: Int,
        userId: Int,
        titulo: String,
        monto: Double,
        fechaVencimiento: Long,
        pagado: Int,
        categoriaId: Int?,
        horaRecordatorio: String?,
        frecuencia: String?,
        notificar: Int,
        diasRecordatorio: Int,
        googleEventId: String?,
        notificationId: String?
    ): Int
    @Query("UPDATE recordatorios SET pagado=:pagado WHERE id=:id AND user_id=:userId") fun markPaid(id: Int, userId: Int, pagado: Int): Int
    @Query("DELETE FROM recordatorios WHERE id=:id AND user_id=:userId") fun deleteById(id: Int, userId: Int): Int
}

@Dao
interface ImportJobDao {
    @Query("SELECT * FROM import_jobs WHERE user_id=:userId ORDER BY id DESC") fun listAll(userId: Int): List<ImportJobEntity>
    @Query("SELECT * FROM import_jobs WHERE id=:id AND user_id=:userId LIMIT 1") fun findById(id: Int, userId: Int): ImportJobEntity?
    @Insert(onConflict = OnConflictStrategy.ABORT) fun insert(entity: ImportJobEntity): Long
    @Query("UPDATE import_jobs SET estado=:estado WHERE id=:id AND user_id=:userId") fun updateEstado(id: Int, userId: Int, estado: String): Int
}

@Dao
interface ImportRuleDao {
    @Query("SELECT * FROM import_rules WHERE user_id=:userId") fun listAll(userId: Int): List<ImportRuleEntity>
    @Insert(onConflict = OnConflictStrategy.ABORT) fun insert(entity: ImportRuleEntity): Long
    @Query(
        """
        UPDATE import_rules
        SET patron=:patron,
            es_ingreso=:esIngreso,
            categoria_id=:categoriaId,
            nota=:nota
        WHERE id=:id AND user_id=:userId
        """
    )
    fun updateById(
        id: Int,
        userId: Int,
        patron: String,
        esIngreso: Int,
        categoriaId: Int?,
        nota: String?
    ): Int
    @Query("DELETE FROM import_rules WHERE id=:id AND user_id=:userId") fun deleteById(id: Int, userId: Int): Int
}
