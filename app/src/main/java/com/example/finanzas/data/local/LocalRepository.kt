package com.example.finanzas.data.local

import android.content.Context
import com.example.finanzas.data.local.room.*
import com.example.finanzas.data.model.*
import com.example.finanzas.util.PasswordSecurity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class LocalRepository private constructor(
    private val db: AppRoomDatabase,
    private val appContext: Context
) {
    companion object {
        private val df = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        @Volatile
        private var instance: LocalRepository? = null

        @JvmStatic
        fun getInstance(context: Context): LocalRepository {
            return instance ?: synchronized(this) {
                instance ?: LocalRepository(
                    LocalDatabase.getInstance(context).room,
                    context.applicationContext
                ).also { instance = it }
            }
        }
    }

    class UserHolder {
        var id: Int = 0
        var nombre: String? = null
        var email: String? = null
    }

    suspend fun registerUser(nombre: String, email: String, password: String, outId: IntArray?): Boolean = withContext(Dispatchers.IO) {
        val id = db.userDao().insert(UserEntity(nombre = nombre, email = email, password = PasswordSecurity.hashPassword(password)))
        if (id <= 0) return@withContext false
        if (outId != null && outId.isNotEmpty()) outId[0] = id.toInt()
        true
    }

    suspend fun login(email: String, password: String, holder: UserHolder?): Boolean = withContext(Dispatchers.IO) {
        val user = db.userDao().findByEmail(email) ?: return@withContext false
        if (!PasswordSecurity.verifyPassword(password, user.password)) return@withContext false
        holder?.apply {
            id = user.id
            nombre = user.nombre
            this.email = user.email
        }
        true
    }


    suspend fun getUserByEmail(email: String): User? = withContext(Dispatchers.IO) {
        db.userDao().findByEmail(email)?.let { User(it.id.toLong(), it.nombre, it.email) }
    }

    suspend fun changePassword(email: String, oldPassword: String, newPassword: String): Boolean = withContext(Dispatchers.IO) {
        val user = db.userDao().findByEmail(email) ?: return@withContext false
        if (!PasswordSecurity.verifyPassword(oldPassword, user.password)) return@withContext false
        db.userDao().update(user.copy(password = PasswordSecurity.hashPassword(newPassword)))
        true
    }

    fun getUserByEmailBlocking(email: String): User? = kotlinx.coroutines.runBlocking { getUserByEmail(email) }

    fun changePasswordBlocking(email: String, oldPassword: String, newPassword: String): Boolean =
        kotlinx.coroutines.runBlocking { changePassword(email, oldPassword, newPassword) }
    suspend fun createCategoria(nombre: String, esIngreso: Boolean): Int = withContext(Dispatchers.IO) {
        val current = db.categoriaDao().listAll()
        val nextId = ((current.maxOfOrNull { it.id } ?: 0) + 1)
        db.categoriaDao().insert(CategoriaEntity(nextId, nombre, if (esIngreso) 1 else 0))
        nextId
    }

    suspend fun listCategorias(): List<Categoria> = withContext(Dispatchers.IO) {
        db.categoriaDao().listAll().map { Categoria(it.id, it.nombre, it.esIngreso == 1) }
    }

    suspend fun listTransacciones(anio: Int, mes: Int, filtro: TransaccionFiltro? = null): List<Transaccion> = withContext(Dispatchers.IO) {
        val cal = Calendar.getInstance().apply {
            set(anio, mes - 1, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        var start = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        var end = cal.timeInMillis

        filtro?.let {
            if (it.fechaInicio != null) start = it.fechaInicio!!
            if (it.fechaFin != null) end = it.fechaFin!!
        }

        val categorias = db.categoriaDao().listAll().associateBy { it.id }
        var list = db.transaccionDao().listAll()
            .asSequence()
            .filter { it.fecha >= start && it.fecha < end }
            .filter { filtro?.categoriaId == null || it.categoriaId == filtro.categoriaId }
            .map {
                Transaccion(
                    it.id,
                    it.categoriaId,
                    categorias[it.categoriaId]?.nombre ?: "",
                    it.esIngreso == 1,
                    it.monto,
                    Date(it.fecha),
                    it.nota
                )
            }
            .toList()

        list = when (filtro?.orden) {
            TransaccionFiltro.Orden.NOMBRE -> list.sortedBy { it.nota ?: "" }
            TransaccionFiltro.Orden.CATEGORIA -> list.sortedBy { it.categoriaNombre ?: "" }
            else -> list.sortedBy { it.fecha }
        }
        if (filtro == null || !filtro.isAscendente) list.reversed() else list
    }

    suspend fun createTransaccion(categoriaId: Int, esIngreso: Boolean, monto: Double, nota: String?, fecha: Long = System.currentTimeMillis()): Int = withContext(Dispatchers.IO) {
        db.transaccionDao().insert(
            TransaccionEntity(categoriaId = categoriaId, esIngreso = if (esIngreso) 1 else 0, monto = monto, fecha = fecha, nota = nota)
        ).toInt()
    }

    suspend fun updateTransaccion(id: Int, categoriaId: Int, esIngreso: Boolean, monto: Double, nota: String?, fecha: Long): Boolean = withContext(Dispatchers.IO) {
        val updated = db.transaccionDao().update(
            TransaccionEntity(id = id, categoriaId = categoriaId, esIngreso = if (esIngreso) 1 else 0, monto = monto, fecha = fecha, nota = nota)
        )
        updated > 0
    }

    suspend fun deleteTransaccion(id: Int): Boolean = withContext(Dispatchers.IO) { db.transaccionDao().deleteById(id) > 0 }

    suspend fun listTodasTransacciones(): List<Transaccion> = withContext(Dispatchers.IO) {
        val cats = db.categoriaDao().listAll().associateBy { it.id }
        db.transaccionDao().listAll().sortedByDescending { it.fecha }.map {
            Transaccion(it.id, it.categoriaId, cats[it.categoriaId]?.nombre ?: "", it.esIngreso == 1, it.monto, Date(it.fecha), it.nota)
        }
    }

    suspend fun exportTransacciones(): String = withContext(Dispatchers.IO) {
        val transacciones = listTodasTransacciones()
        if (transacciones.isEmpty()) throw IllegalStateException("No hay transacciones para exportar")

        val dir = File(appContext.filesDir, "exports")
        if (!dir.exists() && !dir.mkdirs()) throw IllegalStateException("No se pudo crear el directorio de exportación")

        val fileDf = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)
        val dateDf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val outFile = File(dir, "transacciones-${fileDf.format(Date())}.txt")

        val sb = StringBuilder().append("ID | Fecha | Categoría | Tipo | Monto | Nota\n")
        transacciones.forEach {
            sb.append(it.id).append(" | ")
                .append(dateDf.format(it.fecha)).append(" | ")
                .append(it.categoriaNombre).append(" | ")
                .append(if (it.isEsIngreso) "Ingreso" else "Gasto").append(" | ")
                .append(it.monto).append(" | ")
                .append(it.nota ?: "").append('\n')
        }

        BufferedWriter(FileWriter(outFile)).use { it.write(sb.toString()) }
        outFile.absolutePath
    }

    suspend fun getPresupuesto(anio: Int, mes: Int): Double = withContext(Dispatchers.IO) {
        db.presupuestoDao().find(anio, mes)?.monto ?: 0.0
    }

    suspend fun setPresupuesto(anio: Int, mes: Int, monto: Double) = withContext(Dispatchers.IO) {
        db.presupuestoDao().upsert(PresupuestoEntity(anio, mes, monto))
    }

    suspend fun listPresupuestosCategoria(anio: Int, mes: Int): List<CategoryBudgetSummary> = withContext(Dispatchers.IO) {
        val catNames = db.categoriaDao().listAll().associateBy { it.id }
        val budgets = db.presupuestoCategoriaDao().listByMonth(anio, mes)
        val out = budgets.map {
            CategoryBudgetSummary().apply {
                categoriaId = it.categoriaId
                categoriaNombre = catNames[it.categoriaId]?.nombre ?: ""
                limite = it.monto
            }
        }.toMutableList()

        val cal = Calendar.getInstance().apply {
            set(anio, mes - 1, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        val end = cal.timeInMillis

        val gastos = db.transaccionDao().listAll()
            .filter { it.esIngreso == 0 && it.fecha >= start && it.fecha < end }
            .groupBy { it.categoriaId }
            .mapValues { e -> e.value.sumOf { it.monto } }

        gastos.forEach { (catId, gastado) ->
            var target = out.firstOrNull { it.categoriaId == catId }
            if (target == null) {
                target = CategoryBudgetSummary().apply {
                    categoriaId = catId
                    categoriaNombre = catNames[catId]?.nombre ?: ""
                    limite = 0.0
                }
                out.add(target)
            }
            target.gastado = gastado
            target.disponible = target.limite - gastado
            target.porcentaje = if (target.limite > 0) (gastado / target.limite) * 100.0 else 0.0
            target.alerta = target.limite > 0 && gastado > target.limite
        }
        out
    }

    suspend fun savePresupuestosCategoria(anio: Int, mes: Int, items: List<*>) = withContext(Dispatchers.IO) {
        items.forEach { raw ->
            val catId: Int
            val monto: Double
            when (raw) {
                is CategoryBudgetSummary -> {
                    catId = raw.categoriaId
                    monto = raw.limite
                }
                is CategoryBudgetInput -> {
                    catId = raw.categoriaId
                    monto = raw.monto
                }
                else -> return@forEach
            }
            if (catId > 0) db.presupuestoCategoriaDao().upsert(PresupuestoCategoriaEntity(anio, mes, catId, monto))
        }
    }

    suspend fun listGoals(): List<SavingsGoal> = withContext(Dispatchers.IO) {
        db.metaDao().listAll().map { meta ->
            SavingsGoal().apply {
                id = meta.id
                titulo = meta.titulo
                montoObjetivo = meta.montoObjetivo
                montoActual = meta.montoActual
                if (meta.fechaObjetivo != null && meta.fechaObjetivo > 0) fechaObjetivo = Date(meta.fechaObjetivo)
                progreso = if (montoObjetivo > 0) (montoActual / montoObjetivo) * 100.0 else 0.0
                hitos.addAll(db.metaHitoDao().listByMeta(meta.id).map { h ->
                    GoalMilestone().apply {
                        id = h.id
                        metaId = h.metaId
                        titulo = h.titulo
                        montoPlanificado = h.montoPlanificado
                        if (h.fechaObjetivo != null && h.fechaObjetivo > 0) fechaObjetivo = Date(h.fechaObjetivo)
                        notificar = h.notificar == 1
                        diasRecordatorio = h.diasRecordatorio
                        completado = h.completado == 1
                    }
                })
            }
        }
    }

    suspend fun saveGoal(id: Int, titulo: String, objetivo: Double, actual: Double, fechaStr: String?): Boolean = withContext(Dispatchers.IO) {
        val fecha = if (!fechaStr.isNullOrBlank()) runCatching { df.parse(fechaStr)?.time }.getOrNull() else null
        val newId = db.metaDao().upsert(MetaEntity(if (id > 0) id else 0, titulo, objetivo, actual, fecha))
        newId > 0
    }

    suspend fun deleteGoal(id: Int): Boolean = withContext(Dispatchers.IO) {
        db.metaHitoDao().deleteByMeta(id)
        db.metaDao().deleteById(id) > 0
    }

    suspend fun saveMilestone(id: Int, metaId: Int, titulo: String, monto: Double, fechaStr: String?, notificar: Boolean, dias: Int, completado: Boolean): Int = withContext(Dispatchers.IO) {
        val fecha = if (!fechaStr.isNullOrBlank()) runCatching { df.parse(fechaStr)?.time }.getOrNull() else null
        db.metaHitoDao().upsert(
            MetaHitoEntity(
                if (id > 0) id else 0,
                metaId,
                titulo,
                monto,
                fecha,
                if (notificar) 1 else 0,
                dias,
                if (completado) 1 else 0
            )
        ).toInt()
    }

    suspend fun deleteMilestone(id: Int): Boolean = withContext(Dispatchers.IO) { db.metaHitoDao().deleteById(id) > 0 }

    suspend fun listReminders(includePaid: Boolean): List<PaymentReminder> = withContext(Dispatchers.IO) {
        db.recordatorioDao().listAll()
            .filter { includePaid || it.pagado == 0 }
            .map {
                PaymentReminder().apply {
                    id = it.id
                    titulo = it.titulo
                    monto = it.monto
                    if (it.fechaVencimiento > 0) fechaVencimiento = Date(it.fechaVencimiento)
                    pagado = it.pagado == 1
                    categoriaId = it.categoriaId
                    horaRecordatorio = it.horaRecordatorio
                    frecuencia = it.frecuencia
                    notificar = it.notificar == 1
                    diasRecordatorio = it.diasRecordatorio
                    googleEventId = it.googleEventId
                    notificationId = it.notificationId
                }
            }
    }

    suspend fun saveReminder(reminder: PaymentReminder): Boolean = withContext(Dispatchers.IO) {
        val id = db.recordatorioDao().upsert(
            RecordatorioEntity(
                if (reminder.id > 0) reminder.id else 0,
                reminder.titulo ?: "",
                reminder.monto,
                reminder.fechaVencimiento?.time ?: 0,
                if (reminder.isPagado) 1 else 0,
                reminder.categoriaId,
                reminder.horaRecordatorio,
                reminder.frecuencia,
                if (reminder.isNotificar) 1 else 0,
                reminder.diasRecordatorio,
                reminder.googleEventId,
                reminder.notificationId
            )
        )
        if (reminder.id <= 0) reminder.id = id.toInt()
        id > 0
    }

    suspend fun markReminderPaid(id: Int, paid: Boolean): Boolean = withContext(Dispatchers.IO) {
        db.recordatorioDao().markPaid(id, if (paid) 1 else 0) > 0
    }

    suspend fun deleteReminder(id: Int): Boolean = withContext(Dispatchers.IO) { db.recordatorioDao().deleteById(id) > 0 }

    suspend fun listImports(): List<ImportJob> = withContext(Dispatchers.IO) {
        db.importJobDao().listAll().map {
            ImportJob().apply {
                id = it.id
                nombreArchivo = it.nombre
                tipo = it.tipo
                estado = it.estado
            }
        }
    }

    suspend fun createImport(nombre: String, tipo: String, lineasJson: String): Int = withContext(Dispatchers.IO) {
        db.importJobDao().insert(ImportJobEntity(nombre = nombre, tipo = tipo, estado = "pendiente", lineas = lineasJson)).toInt()
    }

    suspend fun listImportRules(): List<ImportRule> = withContext(Dispatchers.IO) {
        db.importRuleDao().listAll().map {
            ImportRule().apply {
                id = it.id
                patron = it.patron
                esIngreso = it.esIngreso == 1
                categoriaId = it.categoriaId
                nota = it.nota
            }
        }
    }

    suspend fun saveImportRule(rule: ImportRule): Int = withContext(Dispatchers.IO) {
        db.importRuleDao().upsert(
            ImportRuleEntity(
                if (rule.id > 0) rule.id else 0,
                rule.patron ?: "",
                if (rule.isEsIngreso) 1 else 0,
                rule.categoriaId,
                rule.nota
            )
        ).toInt()
    }

    suspend fun deleteImportRule(id: Int) = withContext(Dispatchers.IO) { db.importRuleDao().deleteById(id) }

    suspend fun processImport(id: Int): JSONObject = withContext(Dispatchers.IO) {
        db.importJobDao().updateEstado(id, "procesado")
        JSONObject().apply {
            put("procesados", 0)
            put("errores", 0)
            put("omitidos_saldo", 0)
        }
    }

    suspend fun buildHomeSummary(anio: Int, mes: Int): HomeSummary = withContext(Dispatchers.IO) {
        val summary = HomeSummary()
        summary.anio = anio
        summary.mes = mes

        val trans = listTransacciones(anio, mes)
        val ingresos = trans.filter { it.isEsIngreso }.sumOf { it.monto }
        val gastos = trans.filterNot { it.isEsIngreso }.sumOf { it.monto }
        summary.ingresos = ingresos
        summary.gastos = gastos
        summary.saldo = ingresos - gastos

        val presMonto = getPresupuesto(anio, mes)
        summary.presupuestoMonto = presMonto
        val presRestante = presMonto - gastos
        summary.presupuestoRestante = presRestante
        summary.presupuestoPorcentaje = if (presMonto > 0) (gastos / presMonto) * 100.0 else 0.0
        summary.presupuestoExcedido = presRestante < 0
        summary.gastoProyectado = gastos
        summary.gastoPromedioDiario = gastos / maxOf(1, Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH))
        summary.diasRestantes = maxOf(0, Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH) - Calendar.getInstance().get(Calendar.DAY_OF_MONTH))
        summary.presupuestosCategoria.addAll(listPresupuestosCategoria(anio, mes))
        summary.metas.addAll(listGoals())
        summary.recordatorios.addAll(listReminders(false))
        summary.importacionesPendientes = listImports().size

        summary
    }
}
