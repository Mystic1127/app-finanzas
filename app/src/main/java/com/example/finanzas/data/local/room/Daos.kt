package com.example.finanzas.data.local.room

import androidx.room.Dao
import androidx.room.Delete
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

    @Update
    fun update(entity: TransaccionEntity): Int

    @Query("DELETE FROM transacciones WHERE id=:id")
    fun deleteById(id: Int): Int

    @Query("SELECT * FROM transacciones ORDER BY fecha DESC")
    fun listAll(): List<TransaccionEntity>
}

@Dao
interface PresupuestoDao {
    @Query("SELECT * FROM presupuestos WHERE anio=:anio AND mes=:mes")
    fun find(anio: Int, mes: Int): PresupuestoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(entity: PresupuestoEntity)
}

@Dao
interface PresupuestoCategoriaDao {
    @Query("SELECT * FROM presupuestos_categoria WHERE anio=:anio AND mes=:mes")
    fun listByMonth(anio: Int, mes: Int): List<PresupuestoCategoriaEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(entity: PresupuestoCategoriaEntity)
}

@Dao
interface MetaDao {
    @Query("SELECT * FROM metas ORDER BY id DESC")
    fun listAll(): List<MetaEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(entity: MetaEntity): Long

    @Query("DELETE FROM metas WHERE id=:id")
    fun deleteById(id: Int): Int
}

@Dao
interface MetaHitoDao {
    @Query("SELECT * FROM metas_hitos WHERE meta_id=:metaId")
    fun listByMeta(metaId: Int): List<MetaHitoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(entity: MetaHitoEntity): Long

    @Query("DELETE FROM metas_hitos WHERE id=:id")
    fun deleteById(id: Int): Int

    @Query("DELETE FROM metas_hitos WHERE meta_id=:metaId")
    fun deleteByMeta(metaId: Int): Int
}

@Dao
interface RecordatorioDao {
    @Query("SELECT * FROM recordatorios ORDER BY fecha_vencimiento ASC")
    fun listAll(): List<RecordatorioEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(entity: RecordatorioEntity): Long

    @Query("UPDATE recordatorios SET pagado=:pagado WHERE id=:id")
    fun markPaid(id: Int, pagado: Int): Int

    @Query("DELETE FROM recordatorios WHERE id=:id")
    fun deleteById(id: Int): Int
}

@Dao
interface ImportJobDao {
    @Query("SELECT * FROM import_jobs ORDER BY id DESC")
    fun listAll(): List<ImportJobEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insert(entity: ImportJobEntity): Long

    @Query("UPDATE import_jobs SET estado=:estado WHERE id=:id")
    fun updateEstado(id: Int, estado: String): Int
}

@Dao
interface ImportRuleDao {
    @Query("SELECT * FROM import_rules")
    fun listAll(): List<ImportRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(entity: ImportRuleEntity): Long

    @Query("DELETE FROM import_rules WHERE id=:id")
    fun deleteById(id: Int): Int
}
