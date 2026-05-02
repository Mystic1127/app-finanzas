package com.example.finanzas.data.local

import android.content.Context
import androidx.room.withTransaction
import com.example.finanzas.data.api.SettingsService
import com.example.finanzas.data.local.room.*
import com.example.finanzas.data.model.*
import com.example.finanzas.util.CurrencyConverter
import com.example.finanzas.util.PasswordSecurity
import com.example.finanzas.util.Prefs
import com.example.finanzas.util.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

class LocalRepository private constructor(
    private val db: AppRoomDatabase,
    private val appContext: Context
) {
    private fun currentUserId(): Int {
        val userId = Prefs.getCurrentUserId(appContext)
        check(userId > 0L && userId <= Int.MAX_VALUE) { "No hay usuario autenticado" }
        return userId.toInt()
    }

    private fun baseCurrency(): String = SettingsService.getCurrencyCode(appContext)

    private fun manualRate(): Double = SettingsService.getManualRate(appContext)

    private fun convertToBase(amount: Double, currency: String?): Double =
        convertToBase(amount, currency, baseCurrency(), manualRate())

    private fun convertToBase(amount: Double, currency: String?, base: String, manualRate: Double): Double {
        return CurrencyConverter.convert(amount, currency, base, base, manualRate)
    }

    private fun Transaccion.toBaseCurrencyCopy(base: String, manualRate: Double): Transaccion {
        return Transaccion(
            id,
            categoriaId,
            categoriaNombre,
            isEsIngreso,
            convertToBase(monto, moneda, base, manualRate),
            base,
            fecha,
            accountType,
            nota
        )
    }
    companion object {
        private const val BALANCE_EPSILON = 0.005
        private val df = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        private val dataVersion = AtomicLong(0L)
        @Volatile
        private var instance: LocalRepository? = null

        private fun normalizeAccountTypeForMessage(value: String?): String {
            return if ("CASH".equals(value, ignoreCase = true)) "CASH" else "CARD"
        }

        @JvmStatic
        fun getInstance(context: Context): LocalRepository {
            return instance ?: synchronized(this) {
                instance ?: LocalRepository(
                    LocalDatabase.getInstance(context).room,
                    context.applicationContext
                ).also { instance = it }
            }
        }

        @JvmStatic
        fun getDataVersion(): Long = dataVersion.get()

        @JvmStatic
        fun invalidateDataVersion() {
            bumpDataVersion()
        }

        private fun bumpDataVersion() {
            dataVersion.incrementAndGet()
        }
    }

    class UserHolder {
        var id: Int = 0
        var nombre: String? = null
        var email: String? = null
    }

    class InsufficientBalanceException(accountType: String) : IllegalStateException(
        if (normalizeAccountTypeForMessage(accountType) == "CASH") {
            "Saldo insuficiente en Efectivo"
        } else {
            "Saldo insuficiente en Tarjeta/Cuenta"
        }
    )

    private data class ImportRow(
        val fecha: Long,
        val descripcion: String,
        val monto: Double,
        val esIngreso: Boolean
    )

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
        db.userDao().findByEmail(email)?.let { User(it.id, it.nombre, it.email) }
    }

    suspend fun listUsers(): List<User> = withContext(Dispatchers.IO) {
        db.userDao().listAll().map { User(it.id, it.nombre, it.email) }
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
        db.categoriaDao().insert(CategoriaEntity(nextId, currentUserId(), nombre, if (esIngreso) 1 else 0))
        bumpDataVersion()
        nextId
    }

    suspend fun listCategorias(): List<Categoria> = withContext(Dispatchers.IO) {
        db.categoriaDao().listForUser(currentUserId()).map { Categoria(it.id, it.nombre, it.esIngreso == 1) }
    }

    suspend fun hasInitialBalanceConfigured(): Boolean = withContext(Dispatchers.IO) {
        val userId = currentUserId()
        migrateLegacyInitialBalancesIfNeeded(userId)
        SettingsService.isInitialBalanceConfigured(appContext) || hasInitialBalanceTransactions(userId)
    }

    suspend fun configureInitialBalances(cashBalance: Double, cardBalance: Double, currency: String): Boolean = withContext(Dispatchers.IO) {
        require(isValidAmount(cashBalance) && isValidAmount(cardBalance)) { "Monto invalido" }
        require(cashBalance > 0.0 || cardBalance > 0.0) { "Ingresa al menos un saldo inicial" }

        val userId = currentUserId()
        migrateLegacyInitialBalancesIfNeeded(userId)
        if (SettingsService.isInitialBalanceConfigured(appContext) || hasInitialBalanceTransactions(userId)) {
            return@withContext false
        }

        val normalizedCurrency = CurrencyConverter.normalize(currency)
        db.withTransaction {
            val categoryId = ensureInitialBalanceCategory(userId)
            if (cashBalance > 0.0) {
                insertInitialBalanceTransaction(
                    userId = userId,
                    categoryId = categoryId,
                    amount = cashBalance,
                    currency = normalizedCurrency,
                    accountType = "CASH",
                    note = Transaccion.INITIAL_BALANCE_CASH_NOTE
                )
            }
            if (cardBalance > 0.0) {
                insertInitialBalanceTransaction(
                    userId = userId,
                    categoryId = categoryId,
                    amount = cardBalance,
                    currency = normalizedCurrency,
                    accountType = "CARD",
                    note = Transaccion.INITIAL_BALANCE_CARD_NOTE
                )
            }
        }
        SettingsService.markInitialBalanceConfigured(appContext, normalizedCurrency)
        bumpDataVersion()
        true
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

        val userId = currentUserId()
        migrateLegacyInitialBalancesIfNeeded(userId)
        val categorias = db.categoriaDao().listForUser(userId).associateBy { it.id }
        val searchText = filtro?.texto?.trim()?.takeIf { it.isNotEmpty() }?.lowercase(Locale.ROOT)
        var list = db.transaccionDao().listBetween(userId, start, end)
            .asSequence()
            .filter { filtro?.categoriaId == null || it.categoriaId == filtro.categoriaId }
            .map {
                Transaccion(
                    it.id,
                    it.categoriaId,
                    categorias[it.categoriaId]?.nombre ?: "",
                    it.esIngreso == 1,
                    it.monto,
                    CurrencyConverter.normalize(it.moneda),
                    Date(it.fecha),
                    normalizeAccountType(it.accountType),
                    it.nota
                )
            }
            .filter { tx ->
                searchText == null ||
                    (tx.nota ?: "").lowercase(Locale.ROOT).contains(searchText) ||
                    (tx.categoriaNombre ?: "").lowercase(Locale.ROOT).contains(searchText) ||
                    (if (tx.isEsIngreso) "ingreso" else "gasto").contains(searchText) ||
                    tx.monto.toString().contains(searchText)
            }
            .toList()

        list = when (filtro?.orden) {
            TransaccionFiltro.Orden.NOMBRE -> list.sortedBy { it.nota ?: "" }
            TransaccionFiltro.Orden.CATEGORIA -> list.sortedBy { it.categoriaNombre ?: "" }
            else -> list.sortedBy { it.fecha }
        }
        if (filtro == null || !filtro.isAscendente) list.reversed() else list
    }

    suspend fun listTransaccionesEnMonedaBase(anio: Int, mes: Int, filtro: TransaccionFiltro? = null): List<Transaccion> = withContext(Dispatchers.IO) {
        val base = baseCurrency()
        val rate = manualRate()
        listTransacciones(anio, mes, filtro).map { it.toBaseCurrencyCopy(base, rate) }
    }

    private fun validateSufficientBalance(
        userId: Int,
        isIncome: Boolean,
        amount: Double,
        currency: String,
        accountType: String,
        excludingTransactionId: Int?
    ) {
        if (isIncome) return
        val base = baseCurrency()
        val rate = manualRate()
        val expenseInBase = convertToBase(amount, currency, base, rate)
        val available = accountBalanceInBase(userId, normalizeAccountType(accountType), base, rate, excludingTransactionId)
        if (expenseInBase - available > BALANCE_EPSILON) {
            throw InsufficientBalanceException(accountType)
        }
    }

    private fun accountBalanceInBase(
        userId: Int,
        accountType: String,
        base: String,
        manualRate: Double,
        excludingTransactionId: Int?
    ): Double {
        migrateLegacyInitialBalancesIfNeeded(userId)
        val movement = db.transaccionDao().listAll(userId)
            .asSequence()
            .filter { excludingTransactionId == null || it.id != excludingTransactionId }
            .filter { normalizeAccountType(it.accountType) == accountType }
            .sumOf {
                val amountInBase = convertToBase(it.monto, it.moneda, base, manualRate)
                if (it.esIngreso == 1) amountInBase else -amountInBase
            }
        return movement
    }

    suspend fun createTransaccion(categoriaId: Int, esIngreso: Boolean, monto: Double, nota: String?, fecha: Long = System.currentTimeMillis(), moneda: String = baseCurrency(), accountType: String = "CARD"): Int = withContext(Dispatchers.IO) {
        require(categoriaId > 0) { "Categoria invalida" }
        require(monto > 0.0) { "Monto invalido" }
        require(fecha > 0L) { "Fecha invalida" }
        val userId = currentUserId()
        val normalizedAccountType = normalizeAccountType(accountType)
        val normalizedCurrency = CurrencyConverter.normalize(moneda)
        validateSufficientBalance(
            userId = userId,
            isIncome = esIngreso,
            amount = monto,
            currency = normalizedCurrency,
            accountType = normalizedAccountType,
            excludingTransactionId = null
        )
        db.transaccionDao().insert(
            TransaccionEntity(
                userId = userId,
                categoriaId = categoriaId,
                esIngreso = if (esIngreso) 1 else 0,
                monto = monto,
                moneda = normalizedCurrency,
                fecha = fecha,
                accountType = normalizedAccountType,
                nota = nota
            )
        ).toInt().also { bumpDataVersion() }
    }

    suspend fun updateTransaccion(id: Int, categoriaId: Int, esIngreso: Boolean, monto: Double, nota: String?, fecha: Long, moneda: String = baseCurrency(), accountType: String = "CARD"): Boolean = withContext(Dispatchers.IO) {
        require(id > 0) { "Transaccion invalida" }
        require(categoriaId > 0) { "Categoria invalida" }
        require(monto > 0.0) { "Monto invalido" }
        require(fecha > 0L) { "Fecha invalida" }
        val userId = currentUserId()
        val normalizedAccountType = normalizeAccountType(accountType)
        val normalizedCurrency = CurrencyConverter.normalize(moneda)
        validateSufficientBalance(
            userId = userId,
            isIncome = esIngreso,
            amount = monto,
            currency = normalizedCurrency,
            accountType = normalizedAccountType,
            excludingTransactionId = id
        )
        val updated = db.transaccionDao().updateById(
            id = id,
            userId = userId,
            categoriaId = categoriaId,
            esIngreso = if (esIngreso) 1 else 0,
            monto = monto,
            moneda = normalizedCurrency,
            fecha = fecha,
            accountType = normalizedAccountType,
            nota = nota
        )
        if (updated > 0) bumpDataVersion()
        updated > 0
    }

    suspend fun deleteTransaccion(id: Int): Boolean = withContext(Dispatchers.IO) {
        (db.transaccionDao().deleteById(id, currentUserId()) > 0).also { if (it) bumpDataVersion() }
    }

    suspend fun listTodasTransacciones(): List<Transaccion> = withContext(Dispatchers.IO) {
        val userId = currentUserId()
        migrateLegacyInitialBalancesIfNeeded(userId)
        val cats = db.categoriaDao().listForUser(userId).associateBy { it.id }
        db.transaccionDao().listAll(userId).sortedByDescending { it.fecha }.map {
            Transaccion(it.id, it.categoriaId, cats[it.categoriaId]?.nombre ?: "", it.esIngreso == 1, it.monto, CurrencyConverter.normalize(it.moneda), Date(it.fecha), normalizeAccountType(it.accountType), it.nota)
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

        val sb = StringBuilder().append("ID | Fecha | Categoría | Tipo | Monto | Moneda | Cuenta | Nota\n")
        transacciones.forEach {
            sb.append(it.id).append(" | ")
                .append(dateDf.format(it.fecha)).append(" | ")
                .append(it.categoriaNombre).append(" | ")
                .append(if (it.isEsIngreso) "Ingreso" else "Gasto").append(" | ")
                .append(it.monto).append(" | ")
                .append(it.moneda ?: baseCurrency()).append(" | ")
                .append(if (it.isCash) "Efectivo" else "Tarjeta/Cuenta").append(" | ")
                .append(it.nota ?: "").append('\n')
        }

        BufferedWriter(FileWriter(outFile)).use { it.write(sb.toString()) }
        outFile.absolutePath
    }

    suspend fun getPresupuesto(anio: Int, mes: Int): Double = withContext(Dispatchers.IO) {
        db.presupuestoDao().find(currentUserId(), anio, mes)?.let { convertToBase(it.monto, it.moneda) } ?: 0.0
    }

    suspend fun setPresupuesto(anio: Int, mes: Int, monto: Double, moneda: String = baseCurrency()) = withContext(Dispatchers.IO) {
        db.presupuestoDao().upsert(PresupuestoEntity(currentUserId(), anio, mes, monto, CurrencyConverter.normalize(moneda)))
        bumpDataVersion()
    }

    suspend fun listPresupuestosCategoria(anio: Int, mes: Int): List<CategoryBudgetSummary> = withContext(Dispatchers.IO) {
        val base = baseCurrency()
        val rate = manualRate()
        val catNames = db.categoriaDao().listForUser(currentUserId()).associateBy { it.id }
        val budgets = db.presupuestoCategoriaDao().listByMonth(currentUserId(), anio, mes)
        val out = budgets.map {
            CategoryBudgetSummary().apply {
                categoriaId = it.categoriaId
                categoriaNombre = catNames[it.categoriaId]?.nombre ?: ""
                limite = convertToBase(it.monto, it.moneda, base, rate)
                moneda = base
            }
        }.toMutableList()

        val cal = Calendar.getInstance().apply {
            set(anio, mes - 1, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        val end = cal.timeInMillis

        val gastos = db.transaccionDao().listAll(currentUserId())
            .filter { it.esIngreso == 0 && it.fecha >= start && it.fecha < end }
            .groupBy { it.categoriaId }
            .mapValues { e -> e.value.sumOf { convertToBase(it.monto, it.moneda, base, rate) } }

        gastos.forEach { (catId, gastado) ->
            var target = out.firstOrNull { it.categoriaId == catId }
            if (target == null) {
                target = CategoryBudgetSummary().apply {
                    categoriaId = catId
                    categoriaNombre = catNames[catId]?.nombre ?: ""
                    limite = 0.0
                    moneda = base
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
            if (catId > 0) {
                val moneda = when (raw) {
                    is CategoryBudgetSummary -> raw.moneda
                    is CategoryBudgetInput -> raw.moneda
                    else -> baseCurrency()
                }
                db.presupuestoCategoriaDao().upsert(
                    PresupuestoCategoriaEntity(currentUserId(), anio, mes, catId, monto, CurrencyConverter.normalize(moneda))
                )
                bumpDataVersion()
            }
        }
    }

    suspend fun listGoals(): List<SavingsGoal> = withContext(Dispatchers.IO) {
        db.metaDao().listAll(currentUserId()).map { meta ->
            SavingsGoal().apply {
                id = meta.id
                titulo = meta.titulo
                moneda = CurrencyConverter.normalize(meta.moneda)
                montoObjetivo = meta.montoObjetivo
                montoActual = meta.montoActual
                if (meta.fechaObjetivo != null && meta.fechaObjetivo > 0) fechaObjetivo = Date(meta.fechaObjetivo)
                progreso = if (montoObjetivo > 0) (montoActual / montoObjetivo) * 100.0 else 0.0
                hitos.addAll(db.metaHitoDao().listByMeta(currentUserId(), meta.id).map { h ->
                    GoalMilestone().apply {
                        id = h.id
                        metaId = h.metaId
                        titulo = h.titulo
                        montoPlanificado = h.montoPlanificado
                        moneda = CurrencyConverter.normalize(h.moneda)
                        if (h.fechaObjetivo != null && h.fechaObjetivo > 0) fechaObjetivo = Date(h.fechaObjetivo)
                        notificar = h.notificar == 1
                        diasRecordatorio = h.diasRecordatorio
                        completado = h.completado == 1
                    }
                })
            }
        }
    }

    suspend fun saveGoal(id: Int, titulo: String, objetivo: Double, actual: Double, fechaStr: String?, moneda: String = baseCurrency()): Boolean = withContext(Dispatchers.IO) {
        val userId = currentUserId()
        val fecha = if (!fechaStr.isNullOrBlank()) runCatching { df.parse(fechaStr)?.time }.getOrNull() else null
        val ok = if (id > 0) {
            db.metaDao().updateById(id, userId, titulo, objetivo, actual, CurrencyConverter.normalize(moneda), fecha) > 0
        } else {
            db.metaDao().insert(
                MetaEntity(
                    userId = userId,
                    titulo = titulo,
                    montoObjetivo = objetivo,
                    montoActual = actual,
                    moneda = CurrencyConverter.normalize(moneda),
                    fechaObjetivo = fecha
                )
            ) > 0
        }
        if (ok) bumpDataVersion()
        ok
    }

    suspend fun deleteGoal(id: Int): Boolean = withContext(Dispatchers.IO) {
        db.metaHitoDao().deleteByMeta(currentUserId(), id)
        (db.metaDao().deleteById(id, currentUserId()) > 0).also { if (it) bumpDataVersion() }
    }

    suspend fun saveMilestone(id: Int, metaId: Int, titulo: String, monto: Double, fechaStr: String?, notificar: Boolean, dias: Int, completado: Boolean, moneda: String = baseCurrency()): Int = withContext(Dispatchers.IO) {
        val userId = currentUserId()
        if (metaId <= 0 || db.metaDao().countById(metaId, userId) <= 0) return@withContext 0
        val fecha = if (!fechaStr.isNullOrBlank()) runCatching { df.parse(fechaStr)?.time }.getOrNull() else null
        val notificarInt = if (notificar) 1 else 0
        val completadoInt = if (completado) 1 else 0
        if (id > 0) {
            val updated = db.metaHitoDao().updateById(id, userId, metaId, titulo, monto, CurrencyConverter.normalize(moneda), fecha, notificarInt, dias, completadoInt)
            if (updated > 0) id.also { bumpDataVersion() } else 0
        } else {
            db.metaHitoDao().insert(
                MetaHitoEntity(
                    userId = userId,
                    metaId = metaId,
                    titulo = titulo,
                    montoPlanificado = monto,
                    moneda = CurrencyConverter.normalize(moneda),
                    fechaObjetivo = fecha,
                    notificar = notificarInt,
                    diasRecordatorio = dias,
                    completado = completadoInt
                )
            ).toInt().also { bumpDataVersion() }
        }
    }

    suspend fun deleteMilestone(id: Int): Boolean = withContext(Dispatchers.IO) {
        (db.metaHitoDao().deleteById(id, currentUserId()) > 0).also { if (it) bumpDataVersion() }
    }

    suspend fun listReminders(includePaid: Boolean): List<PaymentReminder> = withContext(Dispatchers.IO) {
        db.recordatorioDao().listAll(currentUserId())
            .filter { includePaid || it.pagado == 0 }
            .map {
                PaymentReminder().apply {
                    id = it.id
                    titulo = it.titulo
                    monto = it.monto
                    moneda = CurrencyConverter.normalize(it.moneda)
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
        val userId = currentUserId()
        val titulo = reminder.titulo ?: ""
        val fechaVencimiento = reminder.fechaVencimiento?.time ?: 0
        val pagado = if (reminder.isPagado) 1 else 0
        val notificar = if (reminder.isNotificar) 1 else 0

        if (reminder.id > 0) {
            (db.recordatorioDao().updateById(
                id = reminder.id,
                userId = userId,
                titulo = titulo,
                monto = reminder.monto,
                moneda = CurrencyConverter.normalize(reminder.moneda),
                fechaVencimiento = fechaVencimiento,
                pagado = pagado,
                categoriaId = reminder.categoriaId,
                horaRecordatorio = reminder.horaRecordatorio,
                frecuencia = reminder.frecuencia,
                notificar = notificar,
                diasRecordatorio = reminder.diasRecordatorio,
                googleEventId = reminder.googleEventId,
                notificationId = reminder.notificationId
            ) > 0).also { if (it) bumpDataVersion() }
        } else {
            val id = db.recordatorioDao().insert(
                RecordatorioEntity(
                    userId = userId,
                    titulo = titulo,
                    monto = reminder.monto,
                    moneda = CurrencyConverter.normalize(reminder.moneda),
                    fechaVencimiento = fechaVencimiento,
                    pagado = pagado,
                    categoriaId = reminder.categoriaId,
                    horaRecordatorio = reminder.horaRecordatorio,
                    frecuencia = reminder.frecuencia,
                    notificar = notificar,
                    diasRecordatorio = reminder.diasRecordatorio,
                    googleEventId = reminder.googleEventId,
                    notificationId = reminder.notificationId
                )
            )
            reminder.id = id.toInt()
            if (id > 0) bumpDataVersion()
            id > 0
        }
    }

    suspend fun markReminderPaid(id: Int, paid: Boolean): Boolean = withContext(Dispatchers.IO) {
        (db.recordatorioDao().markPaid(id, currentUserId(), if (paid) 1 else 0) > 0).also { if (it) bumpDataVersion() }
    }

    suspend fun deleteReminder(id: Int): Boolean = withContext(Dispatchers.IO) {
        (db.recordatorioDao().deleteById(id, currentUserId()) > 0).also { if (it) bumpDataVersion() }
    }

    suspend fun listImports(): List<ImportJob> = withContext(Dispatchers.IO) {
        db.importJobDao().listAll(currentUserId()).map {
            ImportJob().apply {
                id = it.id
                nombreArchivo = it.nombre
                tipo = it.tipo
                estado = it.estado
            }
        }
    }

    suspend fun createImport(nombre: String, tipo: String, lineasJson: String): Int = withContext(Dispatchers.IO) {
        db.importJobDao().insert(ImportJobEntity(userId = currentUserId(), nombre = nombre, tipo = tipo, estado = "pendiente", lineas = lineasJson)).toInt()
    }

    suspend fun listImportRules(): List<ImportRule> = withContext(Dispatchers.IO) {
        db.importRuleDao().listAll(currentUserId()).map {
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
        val userId = currentUserId()
        val patron = rule.patron ?: ""
        val esIngreso = if (rule.isEsIngreso) 1 else 0
        if (rule.id > 0) {
            val updated = db.importRuleDao().updateById(rule.id, userId, patron, esIngreso, rule.categoriaId, rule.nota)
            if (updated > 0) rule.id else 0
        } else {
            val id = db.importRuleDao().insert(
                ImportRuleEntity(
                    userId = userId,
                    patron = patron,
                    esIngreso = esIngreso,
                    categoriaId = rule.categoriaId,
                    nota = rule.nota
                )
            ).toInt()
            rule.id = id
            id
        }
    }

    suspend fun deleteImportRule(id: Int) = withContext(Dispatchers.IO) { db.importRuleDao().deleteById(id, currentUserId()) }

    suspend fun deleteCurrentUserFinancialData(): Boolean = withContext(Dispatchers.IO) {
        val userId = currentUserId()
        val reminders = db.recordatorioDao().listAll(userId).map {
            PaymentReminder().apply {
                id = it.id
                titulo = it.titulo
                monto = it.monto
                moneda = CurrencyConverter.normalize(it.moneda)
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
        reminders.forEach { ReminderScheduler.cancel(appContext, it) }

        db.withTransaction {
            db.transaccionDao().deleteForUser(userId)
            db.presupuestoCategoriaDao().deleteForUser(userId)
            db.presupuestoDao().deleteForUser(userId)
            db.metaHitoDao().deleteForUser(userId)
            db.metaDao().deleteForUser(userId)
            db.recordatorioDao().deleteForUser(userId)
            db.importRuleDao().deleteForUser(userId)
            db.importJobDao().deleteForUser(userId)
            db.categoriaDao().deleteForUser(userId)
        }
        SettingsService.clearInitialBalances(appContext)
        Prefs.clearLastTransactionsPeriod(appContext)
        bumpDataVersion()
        true
    }

    suspend fun processImport(id: Int): JSONObject = withContext(Dispatchers.IO) {
        val userId = currentUserId()
        val job = db.importJobDao().findById(id, userId) ?: throw IllegalArgumentException("Importacion no encontrada")
        val categorias = db.categoriaDao().listForUser(userId)
        val rules = db.importRuleDao().listAll(userId)
        val rows = parseImportRows(job.lineas)
        var procesados = 0
        var errores = 0
        var omitidosSaldo = 0
        var suggestedYear = 0
        var suggestedMonth = 0

        rows.forEach { row ->
            runCatching {
                val rule = rules.firstOrNull { rule ->
                    rule.patron.isNotBlank() &&
                        row.descripcion.contains(rule.patron, ignoreCase = true)
                }
                val esIngreso = rule?.let { it.esIngreso == 1 } ?: row.esIngreso
                val categoriaId = rule?.categoriaId
                    ?: categorias.firstOrNull { it.esIngreso == if (esIngreso) 1 else 0 }?.id
                    ?: 0

                if (categoriaId <= 0 || row.monto <= 0.0 || row.fecha <= 0L) {
                    errores++
                    return@forEach
                }

                val nota = rule?.nota?.takeIf { it.isNotBlank() } ?: row.descripcion
                try {
                    validateSufficientBalance(
                        userId = userId,
                        isIncome = esIngreso,
                        amount = row.monto,
                        currency = baseCurrency(),
                        accountType = "CARD",
                        excludingTransactionId = null
                    )
                } catch (e: InsufficientBalanceException) {
                    omitidosSaldo++
                    return@forEach
                }
                db.transaccionDao().insert(
                    TransaccionEntity(
                        userId = userId,
                        categoriaId = categoriaId,
                        esIngreso = if (esIngreso) 1 else 0,
                        monto = row.monto,
                        moneda = baseCurrency(),
                        fecha = row.fecha,
                        accountType = "CARD",
                        nota = nota
                    )
                )
                if (suggestedYear == 0 || suggestedMonth == 0) {
                    Calendar.getInstance().apply {
                        timeInMillis = row.fecha
                        suggestedYear = get(Calendar.YEAR)
                        suggestedMonth = get(Calendar.MONTH) + 1
                    }
                }
                procesados++
            }.onFailure {
                errores++
            }
        }

        db.importJobDao().updateEstado(id, userId, if (procesados > 0) "procesado" else "error")
        if (procesados > 0) bumpDataVersion()
        JSONObject().apply {
            put("procesados", procesados)
            put("errores", errores)
            put("omitidos_saldo", omitidosSaldo)
            if (suggestedYear > 0 && suggestedMonth > 0) {
                put("anio_sugerido", suggestedYear)
                put("mes_sugerido", suggestedMonth)
            }
        }
    }

    private fun parseImportRows(raw: String?): List<ImportRow> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { parseImportRowsFromJson(raw) }
            .getOrElse { parseImportRowsFromText(raw) }
    }

    private fun parseImportRowsFromJson(raw: String): List<ImportRow> {
        val array = JSONArray(raw)
        val rows = mutableListOf<ImportRow>()
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val fecha = parseImportDate(item.optString("fecha", ""))
            val descripcion = item.optString("descripcion", item.optString("nota", "")).trim()
            val monto = kotlin.math.abs(item.optDouble("monto", 0.0))
            val esIngreso = item.optInt("es_ingreso", if (item.optBoolean("es_ingreso", false)) 1 else 0) == 1
            if (fecha > 0L && monto > 0.0) {
                rows.add(ImportRow(fecha, descripcion, monto, esIngreso))
            }
        }
        return rows
    }

    private fun parseImportRowsFromText(raw: String): List<ImportRow> {
        return raw.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { line ->
                val separator = if (line.contains(';')) ';' else ','
                val parts = line.split(separator).map { it.trim() }
                if (parts.size < 3) return@mapNotNull null
                val fecha = parseImportDate(parts[0])
                val descripcion = parts[1]
                val signedAmount = parseImportAmount(parts[2])
                val explicitType = parts.getOrNull(3)?.uppercase(Locale.ROOT)
                val esIngreso = when {
                    explicitType != null -> explicitType.startsWith("I") || explicitType == "1" || explicitType == "+"
                    else -> signedAmount >= 0.0
                }
                val monto = kotlin.math.abs(signedAmount)
                if (fecha <= 0L || monto <= 0.0) null else ImportRow(fecha, descripcion, monto, esIngreso)
            }
            .toList()
    }

    private fun parseImportDate(raw: String): Long {
        val formats = listOf("yyyy-MM-dd", "dd/MM/yyyy", "dd-MM-yyyy")
        formats.forEach { pattern ->
            val parsed = runCatching {
                SimpleDateFormat(pattern, Locale.US).apply { isLenient = false }.parse(raw.trim())?.time
            }.getOrNull()
            if (parsed != null) return parsed
        }
        return 0L
    }

    private fun parseImportAmount(raw: String): Double {
        var clean = raw.trim().replace(Regex("[^0-9,.-]"), "")
        if (clean.isEmpty()) return 0.0
        val lastComma = clean.lastIndexOf(',')
        val lastDot = clean.lastIndexOf('.')
        clean = when {
            lastComma >= 0 && lastDot >= 0 && lastComma > lastDot -> clean.replace(".", "").replace(',', '.')
            lastComma >= 0 && lastDot >= 0 -> clean.replace(",", "")
            lastComma >= 0 -> clean.replace(',', '.')
            else -> clean
        }
        return clean.toDoubleOrNull() ?: 0.0
    }

    suspend fun buildHomeSummary(anio: Int, mes: Int): HomeSummary = withContext(Dispatchers.IO) {
        val summary = HomeSummary()
        summary.anio = anio
        summary.mes = mes

        val base = baseCurrency()
        val rate = manualRate()
        val trans = listTransacciones(anio, mes).map { it.toBaseCurrencyCopy(base, rate) }
        val ingresos = trans.filter { it.isEsIngreso }.sumOf { it.monto }
        val gastos = trans.filterNot { it.isEsIngreso }.sumOf { it.monto }
        val initialBalanceIncome = trans.filter { it.isInitialBalance }.sumOf { it.monto }
        val recurringIncome = (ingresos - initialBalanceIncome).coerceAtLeast(0.0)
        summary.ingresos = ingresos
        summary.gastos = gastos
        summary.saldo = ingresos - gastos
        summary.ingresosRecurrentes = recurringIncome
        summary.balanceVisibleMes = summary.saldo
        summary.balanceOperativoMes = recurringIncome - gastos

        val allTrans = listTodasTransacciones().map { it.toBaseCurrencyCopy(base, rate) }
        val initialCash = allTrans.sumOf { if (it.isInitialBalance && it.isCash) it.monto else 0.0 }
        val initialCard = allTrans.sumOf { if (it.isInitialBalance && !it.isCash) it.monto else 0.0 }
        val cashMovement = allTrans.sumOf { if (it.isCash) if (it.isEsIngreso) it.monto else -it.monto else 0.0 }
        val cardMovement = allTrans.sumOf { if (!it.isCash) if (it.isEsIngreso) it.monto else -it.monto else 0.0 }
        summary.initialCashBalance = initialCash
        summary.initialCardBalance = initialCard
        summary.efectivo = cashMovement
        summary.tarjetaCuenta = cardMovement
        summary.saldoActualTotal = summary.efectivo + summary.tarjetaCuenta

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

    suspend fun buildHomeSummaryFast(anio: Int, mes: Int): HomeSummary = withContext(Dispatchers.IO) {
        val summary = HomeSummary()
        summary.anio = anio
        summary.mes = mes

        val userId = currentUserId()
        migrateLegacyInitialBalancesIfNeeded(userId)
        val base = baseCurrency()
        val rate = manualRate()
        val cal = Calendar.getInstance().apply {
            set(anio, mes - 1, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        val end = cal.timeInMillis

        val monthTrans = db.transaccionDao().listBetween(userId, start, end)
        val initialCategoryIds = initialBalanceCategoryIds(userId)
        val ingresos = monthTrans
            .filter { it.esIngreso == 1 }
            .sumOf { convertToBase(it.monto, it.moneda, base, rate) }
        val gastos = monthTrans
            .filter { it.esIngreso == 0 }
            .sumOf { convertToBase(it.monto, it.moneda, base, rate) }
        val initialBalanceIncome = monthTrans
            .filter { isInitialBalanceEntity(it, initialCategoryIds) }
            .sumOf { convertToBase(it.monto, it.moneda, base, rate) }
        val recurringIncome = (ingresos - initialBalanceIncome).coerceAtLeast(0.0)
        summary.ingresos = ingresos
        summary.gastos = gastos
        summary.saldo = ingresos - gastos
        summary.ingresosRecurrentes = recurringIncome
        summary.balanceVisibleMes = summary.saldo
        summary.balanceOperativoMes = recurringIncome - gastos

        val allTrans = db.transaccionDao().listAll(userId)
        val initialCash = allTrans.sumOf {
            if (isInitialBalanceEntity(it, initialCategoryIds) && "CASH".equals(it.accountType, ignoreCase = true)) {
                convertToBase(it.monto, it.moneda, base, rate)
            } else {
                0.0
            }
        }
        val initialCard = allTrans.sumOf {
            if (isInitialBalanceEntity(it, initialCategoryIds) && !"CASH".equals(it.accountType, ignoreCase = true)) {
                convertToBase(it.monto, it.moneda, base, rate)
            } else {
                0.0
            }
        }
        val cashMovement = allTrans.sumOf {
            if ("CASH".equals(it.accountType, ignoreCase = true)) {
                if (it.esIngreso == 1) convertToBase(it.monto, it.moneda, base, rate) else -convertToBase(it.monto, it.moneda, base, rate)
            } else {
                0.0
            }
        }
        val cardMovement = allTrans.sumOf {
            if (!"CASH".equals(it.accountType, ignoreCase = true)) {
                if (it.esIngreso == 1) convertToBase(it.monto, it.moneda, base, rate) else -convertToBase(it.monto, it.moneda, base, rate)
            } else {
                0.0
            }
        }
        summary.initialCashBalance = initialCash
        summary.initialCardBalance = initialCard
        summary.efectivo = cashMovement
        summary.tarjetaCuenta = cardMovement
        summary.saldoActualTotal = summary.efectivo + summary.tarjetaCuenta

        val presMonto = db.presupuestoDao().find(userId, anio, mes)
            ?.let { convertToBase(it.monto, it.moneda, base, rate) }
            ?: 0.0
        summary.presupuestoMonto = presMonto
        summary.presupuestoRestante = presMonto - gastos
        summary.presupuestoPorcentaje = if (presMonto > 0) (gastos / presMonto) * 100.0 else 0.0
        summary.presupuestoExcedido = summary.presupuestoRestante < 0
        summary.gastoProyectado = gastos
        summary.gastoPromedioDiario = gastos / maxOf(1, Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH))
        summary.diasRestantes = maxOf(0, Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH) - Calendar.getInstance().get(Calendar.DAY_OF_MONTH))

        summary
    }

    @Synchronized
    private fun migrateLegacyInitialBalancesIfNeeded(userId: Int): Boolean {
        if (hasInitialBalanceTransactions(userId)) {
            if (!SettingsService.isInitialBalanceConfigured(appContext)) {
                SettingsService.markInitialBalanceConfigured(appContext, SettingsService.getInitialBalancesCurrency(appContext))
            }
            return true
        }
        if (SettingsService.isInitialBalanceConfigured(appContext)) {
            return true
        }
        if (!SettingsService.hasLegacyInitialBalances(appContext)) {
            return false
        }

        val currency = SettingsService.getInitialBalancesCurrency(appContext)
        val cash = SettingsService.getInitialCashBalance(appContext)
        val card = SettingsService.getInitialCardBalance(appContext)
        val categoryId = ensureInitialBalanceCategory(userId)
        var inserted = false
        if (cash > 0.0) {
            insertInitialBalanceTransaction(
                userId = userId,
                categoryId = categoryId,
                amount = cash,
                currency = currency,
                accountType = "CASH",
                note = Transaccion.INITIAL_BALANCE_CASH_NOTE
            )
            inserted = true
        }
        if (card > 0.0) {
            insertInitialBalanceTransaction(
                userId = userId,
                categoryId = categoryId,
                amount = card,
                currency = currency,
                accountType = "CARD",
                note = Transaccion.INITIAL_BALANCE_CARD_NOTE
            )
            inserted = true
        }
        if (inserted) {
            SettingsService.markInitialBalanceConfigured(appContext, currency)
            bumpDataVersion()
        }
        return inserted
    }

    private fun hasInitialBalanceTransactions(userId: Int): Boolean {
        val categoryIds = initialBalanceCategoryIds(userId)
        if (categoryIds.isEmpty()) return false
        return db.transaccionDao().listAll(userId).any { isInitialBalanceEntity(it, categoryIds) }
    }

    private fun initialBalanceCategoryIds(userId: Int): Set<Int> {
        return db.categoriaDao().listForUser(userId)
            .asSequence()
            .filter { it.esIngreso == 1 && it.nombre.equals(Transaccion.INITIAL_BALANCE_CATEGORY, ignoreCase = true) }
            .map { it.id }
            .toSet()
    }

    private fun ensureInitialBalanceCategory(userId: Int): Int {
        db.categoriaDao().listForUser(userId)
            .firstOrNull { it.esIngreso == 1 && it.nombre.equals(Transaccion.INITIAL_BALANCE_CATEGORY, ignoreCase = true) }
            ?.let { return it.id }

        val nextId = (db.categoriaDao().listAll().maxOfOrNull { it.id } ?: 0) + 1
        db.categoriaDao().insert(
            CategoriaEntity(
                id = nextId,
                userId = userId,
                nombre = Transaccion.INITIAL_BALANCE_CATEGORY,
                esIngreso = 1
            )
        )
        return nextId
    }

    private fun insertInitialBalanceTransaction(
        userId: Int,
        categoryId: Int,
        amount: Double,
        currency: String,
        accountType: String,
        note: String
    ) {
        db.transaccionDao().insert(
            TransaccionEntity(
                userId = userId,
                categoriaId = categoryId,
                esIngreso = 1,
                monto = amount,
                moneda = CurrencyConverter.normalize(currency),
                fecha = System.currentTimeMillis(),
                accountType = normalizeAccountType(accountType),
                nota = note
            )
        )
    }

    private fun isInitialBalanceEntity(entity: TransaccionEntity, categoryIds: Set<Int>): Boolean {
        return entity.esIngreso == 1 && entity.categoriaId in categoryIds && isInitialBalanceNote(entity.nota)
    }

    private fun isInitialBalanceNote(note: String?): Boolean {
        val clean = note?.trim().orEmpty()
        return clean.equals(Transaccion.INITIAL_BALANCE_CASH_NOTE, ignoreCase = true) ||
            clean.equals(Transaccion.INITIAL_BALANCE_CARD_NOTE, ignoreCase = true) ||
            clean.equals(Transaccion.INITIAL_BALANCE_CATEGORY, ignoreCase = true)
    }

    private fun isValidAmount(value: Double): Boolean {
        return !value.isNaN() && !value.isInfinite() && value >= 0.0
    }

    private fun normalizeAccountType(value: String?): String {
        return if ("CASH".equals(value, ignoreCase = true)) "CASH" else "CARD"
    }
}
