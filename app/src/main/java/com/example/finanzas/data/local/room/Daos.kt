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

    @Query("SELECT * FROM users ORDER BY nombre COLLATE NOCASE, email COLLATE NOCASE")
    fun listAll(): List<UserEntity>

    @Update
    fun update(entity: UserEntity)
}

@Dao
interface CategoriaDao {
    @Query("SELECT * FROM categorias ORDER BY nombre")
    fun listAll(): List<CategoriaEntity>

    @Query("SELECT * FROM categorias WHERE user_id = 0 OR user_id = :userId ORDER BY nombre")
    fun listForUser(userId: Int): List<CategoriaEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insert(entity: CategoriaEntity): Long

    @Query("UPDATE categorias SET nombre=:nombre, es_ingreso=:esIngreso WHERE id=:id AND user_id=:userId")
    fun updateById(id: Int, userId: Int, nombre: String, esIngreso: Int): Int

    @Query("DELETE FROM categorias WHERE id=:id AND user_id=:userId")
    fun deleteById(id: Int, userId: Int): Int

    @Query("SELECT COUNT(*) FROM transacciones WHERE user_id=:userId AND categoria_id=:categoriaId")
    fun countTransactions(userId: Int, categoriaId: Int): Int

    @Query("SELECT COUNT(*) FROM presupuestos_categoria WHERE user_id=:userId AND categoria_id=:categoriaId")
    fun countBudgets(userId: Int, categoriaId: Int): Int

    @Query("SELECT COUNT(*) FROM import_rules WHERE user_id=:userId AND categoria_id=:categoriaId")
    fun countImportRules(userId: Int, categoriaId: Int): Int

    @Query("DELETE FROM categorias WHERE user_id=:userId")
    fun deleteForUser(userId: Int): Int
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
            moneda=:moneda,
            fecha=:fecha,
            account_type=:accountType,
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
        moneda: String,
        fecha: Long,
        accountType: String,
        nota: String?
    ): Int
    @Query("DELETE FROM transacciones WHERE id=:id AND user_id=:userId") fun deleteById(id: Int, userId: Int): Int
    @Query("DELETE FROM transacciones WHERE user_id=:userId") fun deleteForUser(userId: Int): Int
    @Query("SELECT * FROM transacciones WHERE user_id=:userId ORDER BY fecha DESC") fun listAll(userId: Int): List<TransaccionEntity>
    @Query("SELECT * FROM transacciones WHERE user_id=:userId AND fecha>=:start AND fecha<:end ORDER BY fecha DESC")
    fun listBetween(userId: Int, start: Long, end: Long): List<TransaccionEntity>
}

@Dao
interface PresupuestoDao {
    @Query("SELECT * FROM presupuestos WHERE user_id=:userId AND anio=:anio AND mes=:mes")
    fun find(userId: Int, anio: Int, mes: Int): PresupuestoEntity?
    @Query(
        """
        SELECT * FROM presupuestos
        WHERE user_id=:userId
          AND (anio < :anio OR (anio = :anio AND mes <= :mes))
        ORDER BY anio DESC, mes DESC
        LIMIT 1
        """
    )
    fun findLatestUpTo(userId: Int, anio: Int, mes: Int): PresupuestoEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) fun upsert(entity: PresupuestoEntity)
    @Query("DELETE FROM presupuestos WHERE user_id=:userId") fun deleteForUser(userId: Int): Int
}

@Dao
interface PresupuestoCategoriaDao {
    @Query("SELECT * FROM presupuestos_categoria WHERE user_id=:userId AND anio=:anio AND mes=:mes")
    fun listByMonth(userId: Int, anio: Int, mes: Int): List<PresupuestoCategoriaEntity>
    @Query(
        """
        SELECT * FROM presupuestos_categoria
        WHERE user_id=:userId
          AND (anio < :anio OR (anio = :anio AND mes <= :mes))
        ORDER BY anio DESC, mes DESC
        LIMIT 1
        """
    )
    fun findLatestUpTo(userId: Int, anio: Int, mes: Int): PresupuestoCategoriaEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) fun upsert(entity: PresupuestoCategoriaEntity)
    @Query("DELETE FROM presupuestos_categoria WHERE user_id=:userId") fun deleteForUser(userId: Int): Int
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
            moneda=:moneda,
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
        moneda: String,
        fechaObjetivo: Long?
    ): Int
    @Query("DELETE FROM metas WHERE id=:id AND user_id=:userId") fun deleteById(id: Int, userId: Int): Int
    @Query("DELETE FROM metas WHERE user_id=:userId") fun deleteForUser(userId: Int): Int
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
            moneda=:moneda,
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
        moneda: String,
        fechaObjetivo: Long?,
        notificar: Int,
        diasRecordatorio: Int,
        completado: Int
    ): Int
    @Query("DELETE FROM metas_hitos WHERE id=:id AND user_id=:userId") fun deleteById(id: Int, userId: Int): Int
    @Query("DELETE FROM metas_hitos WHERE user_id=:userId AND meta_id=:metaId") fun deleteByMeta(userId: Int, metaId: Int): Int
    @Query("DELETE FROM metas_hitos WHERE user_id=:userId") fun deleteForUser(userId: Int): Int
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
            moneda=:moneda,
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
        moneda: String,
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
    @Query("DELETE FROM recordatorios WHERE user_id=:userId") fun deleteForUser(userId: Int): Int
}

@Dao
interface ImportJobDao {
    @Query("SELECT * FROM import_jobs WHERE user_id=:userId ORDER BY id DESC") fun listAll(userId: Int): List<ImportJobEntity>
    @Query("SELECT * FROM import_jobs WHERE id=:id AND user_id=:userId LIMIT 1") fun findById(id: Int, userId: Int): ImportJobEntity?
    @Insert(onConflict = OnConflictStrategy.ABORT) fun insert(entity: ImportJobEntity): Long
    @Query("UPDATE import_jobs SET estado=:estado WHERE id=:id AND user_id=:userId") fun updateEstado(id: Int, userId: Int, estado: String): Int
    @Query("DELETE FROM import_jobs WHERE user_id=:userId") fun deleteForUser(userId: Int): Int
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
    @Query("DELETE FROM import_rules WHERE user_id=:userId") fun deleteForUser(userId: Int): Int
}
