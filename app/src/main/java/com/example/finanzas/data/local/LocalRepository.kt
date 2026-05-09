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
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
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
        private val DEFAULT_CATEGORIES = listOf(
            DefaultCategory("Alimentaci\u00f3n", false),
            DefaultCategory("Salud", false),
            DefaultCategory("Educaci\u00f3n", false),
            DefaultCategory("Servicios", false),
            DefaultCategory("Compras", false),
            DefaultCategory("Supermercado", false),
            DefaultCategory("Delivery", false),
            DefaultCategory("Restaurantes", false),
            DefaultCategory("Farmacia", false),
            DefaultCategory("Suscripciones", false),
            DefaultCategory("Internet / Tel\u00e9fono", false),
            DefaultCategory("Luz", false),
            DefaultCategory("Agua", false),
            DefaultCategory("Gas", false),
            DefaultCategory("Reparaciones", false),
            DefaultCategory("Cuidado personal", false),
            DefaultCategory("Ropa", false),
            DefaultCategory("Tecnolog\u00eda", false),
            DefaultCategory("Viajes", false),
            DefaultCategory("Mascotas", false),
            DefaultCategory("Familia", false),
            DefaultCategory("Deudas", false),
            DefaultCategory("Entretenimiento", false),
            DefaultCategory("Transporte", false),
            DefaultCategory("Vivienda", false),
            DefaultCategory("Ahorro", false),
            DefaultCategory("Regalos", false),
            DefaultCategory("Trabajo", false),
            DefaultCategory("Impuestos", false),
            DefaultCategory("Otros", false),
            DefaultCategory("Sueldo", true),
            DefaultCategory("Freelance", true),
            DefaultCategory("Negocio", true),
            DefaultCategory("Inversiones", true),
            DefaultCategory("Regalo recibido", true),
            DefaultCategory("Reembolso", true),
            DefaultCategory("Venta", true),
            DefaultCategory("Otros ingresos", true)
        )
        @Volatile
        private var instance: LocalRepository? = null

        private fun normalizeAccountTypeForMessage(value: String?): String {
            val normalized = SettingsService.normalizeAccountType(value)
            return when (normalized) {
                "CASH" -> "CASH"
                "CARD" -> "CARD"
                else -> "ACCOUNT"
            }
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
        when (normalizeAccountTypeForMessage(accountType)) {
            "CASH" -> "Saldo insuficiente en Efectivo"
        "CARD" -> "Saldo insuficiente en Tarjeta"
            else -> "Saldo insuficiente en la cuenta seleccionada"
        }
    )

    private data class ImportRow(
        val fecha: Long,
        val descripcion: String,
        val monto: Double,
        val esIngreso: Boolean,
        val moneda: String?,
        val categoriaId: Int?,
        val categoriaNombre: String?,
        val nota: String?
    )

    private data class DefaultCategory(val name: String, val income: Boolean)

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

    suspend fun resetPassword(email: String, newPassword: String): Boolean = withContext(Dispatchers.IO) {
        val user = db.userDao().findByEmail(email) ?: return@withContext false
        db.userDao().update(user.copy(password = PasswordSecurity.hashPassword(newPassword)))
        true
    }

    fun getUserByEmailBlocking(email: String): User? = kotlinx.coroutines.runBlocking { getUserByEmail(email) }

    fun changePasswordBlocking(email: String, oldPassword: String, newPassword: String): Boolean =
        kotlinx.coroutines.runBlocking { changePassword(email, oldPassword, newPassword) }

    fun resetPasswordBlocking(email: String, newPassword: String): Boolean =
        kotlinx.coroutines.runBlocking { resetPassword(email, newPassword) }

    suspend fun createCategoria(nombre: String, esIngreso: Boolean): Int = withContext(Dispatchers.IO) {
        val current = db.categoriaDao().listAll()
        val nextId = ((current.maxOfOrNull { it.id } ?: 0) + 1)
        db.categoriaDao().insert(CategoriaEntity(nextId, currentUserId(), nombre, if (esIngreso) 1 else 0))
        bumpDataVersion()
        nextId
    }

    suspend fun listCategorias(): List<Categoria> = withContext(Dispatchers.IO) {
        val userId = currentUserId()
        ensureDefaultCategories(userId)
        db.categoriaDao().listForUser(userId).map {
            Categoria(
                it.id,
                it.userId,
                it.nombre,
                it.esIngreso == 1,
                it.userId == userId &&
                    !it.nombre.equals(Transaccion.INITIAL_BALANCE_CATEGORY, ignoreCase = true) &&
                    !it.nombre.equals(Transaccion.TRANSFER_CATEGORY, ignoreCase = true)
            )
        }.filterNot { it.esIngreso && isDeprecatedIncomeCategory(it.nombre) }
    }

    private fun isDeprecatedIncomeCategory(name: String?): Boolean {
        val normalized = categoryKey(name, true).substringAfter(':')
        return normalized == "salario" || normalized == "inversion"
    }

    @Synchronized
    private fun ensureDefaultCategories(userId: Int) {
        val dao = db.categoriaDao()
        val existing = dao.listForUser(userId)
        val existingKeys = existing
            .map { categoryKey(it.nombre, it.esIngreso == 1) }
            .toMutableSet()
        var nextId = (dao.listAll().maxOfOrNull { it.id } ?: 0) + 1
        for (category in DEFAULT_CATEGORIES) {
            val key = categoryKey(category.name, category.income)
            if (key in existingKeys) continue
            dao.insert(
                CategoriaEntity(
                    id = nextId++,
                    userId = 0,
                    nombre = category.name,
                    esIngreso = if (category.income) 1 else 0
                )
            )
            existingKeys.add(key)
        }
    }

    private fun categoryKey(name: String?, income: Boolean): String {
        val raw = name?.trim().orEmpty().lowercase(Locale.ROOT)
        val normalized = Normalizer.normalize(raw, Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        return "${if (income) "I" else "E"}:$normalized"
    }

    suspend fun updateCategoria(id: Int, nombre: String, esIngreso: Boolean): Boolean = withContext(Dispatchers.IO) {
        val userId = currentUserId()
        val cleanName = nombre.trim()
        if (id <= 0 || cleanName.isBlank()) return@withContext false
        val category = db.categoriaDao().listForUser(userId).firstOrNull { it.id == id } ?: return@withContext false
        if (category.userId != userId || category.nombre.equals(Transaccion.INITIAL_BALANCE_CATEGORY, ignoreCase = true)) {
            return@withContext false
        }
        if ((category.esIngreso == 1) != esIngreso) {
            val inUse = db.categoriaDao().countTransactions(userId, id) > 0 ||
                db.categoriaDao().countBudgets(userId, id) > 0 ||
                db.categoriaDao().countImportRules(userId, id) > 0
            if (inUse) return@withContext false
        }
        val updated = db.categoriaDao().updateById(id, userId, cleanName, if (esIngreso) 1 else 0)
        if (updated > 0) bumpDataVersion()
        updated > 0
    }

    suspend fun deleteCategoria(id: Int): Boolean = withContext(Dispatchers.IO) {
        val userId = currentUserId()
        val category = db.categoriaDao().listForUser(userId).firstOrNull { it.id == id } ?: return@withContext false
        if (category.userId != userId || category.nombre.equals(Transaccion.INITIAL_BALANCE_CATEGORY, ignoreCase = true)) {
            return@withContext false
        }
        val inUse = db.categoriaDao().countTransactions(userId, id) > 0 ||
            db.categoriaDao().countBudgets(userId, id) > 0 ||
            db.categoriaDao().countImportRules(userId, id) > 0
        if (inUse) return@withContext false
        val deleted = db.categoriaDao().deleteById(id, userId)
        if (deleted > 0) bumpDataVersion()
        deleted > 0
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

    suspend fun createFinancialAccount(name: String, initialBalance: Double, currency: String, last4: String = ""): FinancialAccount = withContext(Dispatchers.IO) {
        val cleanName = name.trim()
        require(cleanName.isNotBlank()) { "Nombre de cuenta requerido" }
        require(isValidAmount(initialBalance)) { "Monto invalido" }
        val userId = currentUserId()
        migrateLegacyInitialBalancesIfNeeded(userId)
        val account = SettingsService.addFinancialAccount(appContext, cleanName, last4)
        if (initialBalance > 0.0) {
            db.withTransaction {
                val categoryId = ensureInitialBalanceCategory(userId)
                insertInitialBalanceTransaction(
                    userId = userId,
                    categoryId = categoryId,
                    amount = initialBalance,
                    currency = CurrencyConverter.normalize(currency),
                    accountType = account.id,
                    note = "${Transaccion.INITIAL_BALANCE_ACCOUNT_NOTE_PREFIX} ${account.name}"
                )
            }
        }
        bumpDataVersion()
        account
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
        val filterAccountType = filtro?.accountType?.takeIf { it.isNotBlank() }?.let { normalizeAccountType(it) }
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
                val amountInRange = (filtro?.montoMin == null || tx.monto >= filtro.montoMin!!)
                    && (filtro?.montoMax == null || tx.monto <= filtro.montoMax!!)
                val typeMatches = when (filtro?.tipo) {
                    TransaccionFiltro.Tipo.INGRESOS -> tx.isEsIngreso && !tx.isInitialBalance && !tx.isTransfer
                    TransaccionFiltro.Tipo.GASTOS -> !tx.isEsIngreso && !tx.isTransfer
                    TransaccionFiltro.Tipo.TRANSFERENCIAS -> tx.isTransfer
                    else -> true
                }
                val accountMatches = filterAccountType == null ||
                    normalizeAccountType(tx.accountType) == filterAccountType ||
                    (tx.isTransfer && tx.transferDestinationAccountType == filterAccountType)
                amountInRange && typeMatches && accountMatches && (
                searchText == null ||
                    (tx.displayNote ?: "").lowercase(Locale.ROOT).contains(searchText) ||
                    (tx.categoriaNombre ?: "").lowercase(Locale.ROOT).contains(searchText) ||
                    movementTypeLabel(tx).contains(searchText) ||
                    SettingsService.getFinancialAccountName(appContext, tx.accountType).lowercase(Locale.ROOT).contains(searchText) ||
                    (tx.isTransfer && SettingsService.getFinancialAccountName(appContext, tx.transferDestinationAccountType).lowercase(Locale.ROOT).contains(searchText)) ||
                    tx.monto.toString().contains(searchText)
                )
            }
            .toList()

        val orden = filtro?.orden ?: TransaccionFiltro.Orden.FECHA
        val ascendente = filtro?.isAscendente == true
        when (orden) {
            TransaccionFiltro.Orden.NOMBRE -> {
                val sorted = list.sortedWith(
                    compareBy<Transaccion> { it.nota.orEmpty().lowercase(Locale.ROOT) }
                        .thenBy { it.id }
                )
                if (ascendente) sorted else sorted.reversed()
            }
            TransaccionFiltro.Orden.CATEGORIA -> {
                val sorted = list.sortedWith(
                    compareBy<Transaccion> { it.categoriaNombre.orEmpty().lowercase(Locale.ROOT) }
                        .thenBy { it.id }
                )
                if (ascendente) sorted else sorted.reversed()
            }
            TransaccionFiltro.Orden.MONTO -> {
                val sorted = list.sortedWith(compareBy<Transaccion> { it.monto }.thenBy { it.id })
                if (ascendente) sorted else sorted.reversed()
            }
            TransaccionFiltro.Orden.FECHA -> {
                if (ascendente) {
                    list.sortedWith(compareBy<Transaccion> { it.fecha?.time ?: 0L }.thenBy { it.id })
                } else {
                    list.sortedWith(
                        compareByDescending<Transaccion> { it.fecha?.time ?: 0L }
                            .thenByDescending { it.id }
                    )
                }
            }
        }
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
            .sumOf { signedAccountDelta(it, accountType, base, manualRate) }
        return movement
    }

    private fun transferDestination(note: String?): String? {
        val raw = note?.trim().orEmpty()
        if (!raw.lowercase(Locale.ROOT).startsWith(Transaccion.TRANSFER_NOTE_PREFIX)) return null
        val close = raw.indexOf(']')
        if (close <= Transaccion.TRANSFER_NOTE_PREFIX.length) return null
        return normalizeAccountType(raw.substring(Transaccion.TRANSFER_NOTE_PREFIX.length, close))
    }

    private fun isTransferEntity(entity: TransaccionEntity, categories: Map<Int, CategoriaEntity>? = null): Boolean {
        val name = categories?.get(entity.categoriaId)?.nombre.orEmpty()
        return name.equals(Transaccion.TRANSFER_CATEGORY, ignoreCase = true) || transferDestination(entity.nota) != null
    }

    private fun signedAccountDelta(entity: TransaccionEntity, targetAccountType: String, base: String, manualRate: Double): Double {
        val normalizedTarget = normalizeAccountType(targetAccountType)
        val source = normalizeAccountType(entity.accountType)
        val amountInBase = convertToBase(entity.monto, entity.moneda, base, manualRate)
        val destination = transferDestination(entity.nota)
        if (destination != null) {
            return when (normalizedTarget) {
                source -> -amountInBase
                destination -> amountInBase
                else -> 0.0
            }
        }
        if (source != normalizedTarget) return 0.0
        return if (entity.esIngreso == 1) amountInBase else -amountInBase
    }

    suspend fun createTransaccion(categoriaId: Int, esIngreso: Boolean, monto: Double, nota: String?, fecha: Long, moneda: String = baseCurrency(), accountType: String = "CARD"): Int = withContext(Dispatchers.IO) {
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

    suspend fun createTransfer(originAccountType: String, destinationAccountType: String, monto: Double, nota: String?, fecha: Long, moneda: String = baseCurrency()): Int = withContext(Dispatchers.IO) {
        require(monto > 0.0) { "Monto invalido" }
        require(fecha > 0L) { "Fecha invalida" }
        val userId = currentUserId()
        val origin = normalizeAccountType(originAccountType)
        val destination = normalizeAccountType(destinationAccountType)
        require(origin != destination) { "El origen y destino deben ser distintos" }
        require(SettingsService.listFinancialAccounts(appContext).isNotEmpty() || origin != "CASH" || destination != "CASH") {
            "Agrega al menos una tarjeta o cuenta para transferir"
        }
        val normalizedCurrency = CurrencyConverter.normalize(moneda)
        val base = baseCurrency()
        val rate = manualRate()
        val amountInBase = convertToBase(monto, normalizedCurrency, base, rate)
        val available = accountBalanceInBase(userId, origin, base, rate, null)
        if (amountInBase - available > BALANCE_EPSILON) {
            throw InsufficientBalanceException(origin)
        }
        val categoryId = ensureTransferCategory(userId)
        db.transaccionDao().insert(
            TransaccionEntity(
                userId = userId,
                categoriaId = categoryId,
                esIngreso = 0,
                monto = monto,
                moneda = normalizedCurrency,
                fecha = fecha,
                accountType = origin,
                nota = Transaccion.buildTransferNote(destination, nota)
            )
        ).toInt().also { bumpDataVersion() }
    }

    suspend fun updateTransfer(id: Int, originAccountType: String, destinationAccountType: String, monto: Double, nota: String?, fecha: Long, moneda: String = baseCurrency()): Boolean = withContext(Dispatchers.IO) {
        require(id > 0) { "Transaccion invalida" }
        require(monto > 0.0) { "Monto invalido" }
        require(fecha > 0L) { "Fecha invalida" }
        val userId = currentUserId()
        val origin = normalizeAccountType(originAccountType)
        val destination = normalizeAccountType(destinationAccountType)
        require(origin != destination) { "El origen y destino deben ser distintos" }
        val normalizedCurrency = CurrencyConverter.normalize(moneda)
        val base = baseCurrency()
        val rate = manualRate()
        val amountInBase = convertToBase(monto, normalizedCurrency, base, rate)
        val available = accountBalanceInBase(userId, origin, base, rate, id)
        if (amountInBase - available > BALANCE_EPSILON) {
            throw InsufficientBalanceException(origin)
        }
        val categoryId = ensureTransferCategory(userId)
        val updated = db.transaccionDao().updateById(
            id = id,
            userId = userId,
            categoriaId = categoryId,
            esIngreso = 0,
            monto = monto,
            moneda = normalizedCurrency,
            fecha = fecha,
            accountType = origin,
            nota = Transaccion.buildTransferNote(destination, nota)
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
                .append(if (it.isTransfer) "Transferencia" else if (it.isEsIngreso) "Ingreso" else "Gasto").append(" | ")
                .append(it.monto).append(" | ")
                .append(it.moneda ?: baseCurrency()).append(" | ")
                .append(SettingsService.getFinancialAccountName(appContext, it.accountType)).append(" | ")
                .append(it.displayNote ?: "").append('\n')
        }

        BufferedWriter(FileWriter(outFile)).use { it.write(sb.toString()) }
        outFile.absolutePath
    }

    suspend fun getPresupuesto(anio: Int, mes: Int): Double = withContext(Dispatchers.IO) {
        val userId = currentUserId()
        findEffectivePresupuesto(userId, anio, mes)?.let { convertToBase(it.monto, it.moneda) } ?: 0.0
    }

    suspend fun setPresupuesto(anio: Int, mes: Int, monto: Double, moneda: String = baseCurrency()) = withContext(Dispatchers.IO) {
        db.presupuestoDao().upsert(PresupuestoEntity(currentUserId(), anio, mes, monto, CurrencyConverter.normalize(moneda)))
        bumpDataVersion()
    }

    suspend fun ensurePresupuestoPlanForMonth(anio: Int, mes: Int): Boolean = withContext(Dispatchers.IO) {
        val userId = currentUserId()
        var copied = false
        db.withTransaction {
            val presupuestoDao = db.presupuestoDao()
            val categoriaDao = db.presupuestoCategoriaDao()
            val currentBudget = presupuestoDao.find(userId, anio, mes)
            val currentCategoryBudgets = categoriaDao.listByMonth(userId, anio, mes)

            if (currentBudget == null) {
                val previousBudget = presupuestoDao.findLatestUpTo(userId, anio, mes)
                if (previousBudget != null) {
                    presupuestoDao.upsert(
                        PresupuestoEntity(
                            userId = userId,
                            anio = anio,
                            mes = mes,
                            monto = previousBudget.monto,
                            moneda = previousBudget.moneda
                        )
                    )
                    copied = true
                }
            }

            if (currentBudget == null && currentCategoryBudgets.isEmpty()) {
                val latestCategoryBudget = categoriaDao.findLatestUpTo(userId, anio, mes)
                if (latestCategoryBudget != null) {
                    val previousItems = categoriaDao.listByMonth(userId, latestCategoryBudget.anio, latestCategoryBudget.mes)
                    previousItems.forEach { previous ->
                        categoriaDao.upsert(
                            PresupuestoCategoriaEntity(
                                userId = userId,
                                anio = anio,
                                mes = mes,
                                categoriaId = previous.categoriaId,
                                monto = previous.monto,
                                moneda = previous.moneda
                            )
                        )
                    }
                    if (previousItems.isNotEmpty()) copied = true
                }
            }
        }
        if (copied) bumpDataVersion()
        copied
    }

    private fun findEffectivePresupuesto(userId: Int, anio: Int, mes: Int): PresupuestoEntity? {
        return db.presupuestoDao().find(userId, anio, mes)
            ?: db.presupuestoDao().findLatestUpTo(userId, anio, mes)
    }

    private fun listEffectivePresupuestosCategoria(userId: Int, anio: Int, mes: Int): List<PresupuestoCategoriaEntity> {
        val exact = db.presupuestoCategoriaDao().listByMonth(userId, anio, mes)
        if (exact.isNotEmpty()) return exact

        val latest = db.presupuestoCategoriaDao().findLatestUpTo(userId, anio, mes) ?: return emptyList()
        return db.presupuestoCategoriaDao().listByMonth(userId, latest.anio, latest.mes)
    }

    suspend fun listPresupuestosCategoria(anio: Int, mes: Int): List<CategoryBudgetSummary> = withContext(Dispatchers.IO) {
        val base = baseCurrency()
        val rate = manualRate()
        val userId = currentUserId()
        val catNames = db.categoriaDao().listForUser(userId).associateBy { it.id }
        val budgets = listEffectivePresupuestosCategoria(userId, anio, mes)
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

        val gastos = db.transaccionDao().listAll(userId)
            .filter { it.esIngreso == 0 && it.fecha >= start && it.fecha < end && !isTransferEntity(it, catNames) }
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
        val userId = currentUserId()
        db.presupuestoCategoriaDao().deleteByMonth(userId, anio, mes)
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
                    PresupuestoCategoriaEntity(userId, anio, mes, catId, monto, CurrencyConverter.normalize(moneda))
                )
            }
        }
        bumpDataVersion()
    }

    suspend fun listSavedPresupuestosCategoriaInputs(anio: Int, mes: Int): List<CategoryBudgetInput> = withContext(Dispatchers.IO) {
        val userId = currentUserId()
        val base = baseCurrency()
        val catNames = db.categoriaDao().listForUser(userId).associateBy { it.id }
        db.presupuestoCategoriaDao().listByMonth(userId, anio, mes).map {
            CategoryBudgetInput().apply {
                categoriaId = it.categoriaId
                categoriaNombre = catNames[it.categoriaId]?.nombre ?: ""
                monto = it.monto
                moneda = CurrencyConverter.normalize(it.moneda.ifBlank { base })
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
            .map { it.toPaymentReminder() }
    }

    suspend fun saveReminder(reminder: PaymentReminder): Boolean = withContext(Dispatchers.IO) {
        val userId = currentUserId()
        val titulo = reminder.titulo ?: ""
        val fechaVencimiento = reminder.fechaVencimiento?.time ?: 0
        val pagado = if (reminder.isPagado) 1 else 0
        val notificar = if (reminder.isNotificar) 1 else 0

        val saved = if (reminder.id > 0) {
            db.recordatorioDao().updateById(
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
            ) > 0
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
            id > 0
        }
        if (saved) {
            if (reminder.isNotificar && !reminder.isPagado) {
                ReminderScheduler.schedule(appContext, reminder)
            } else {
                ReminderScheduler.cancel(appContext, reminder)
            }
            bumpDataVersion()
        }
        saved
    }

    suspend fun markReminderPaid(id: Int, paid: Boolean): Boolean = withContext(Dispatchers.IO) {
        val userId = currentUserId()
        val reminder = db.recordatorioDao().listAll(userId).firstOrNull { it.id == id }?.toPaymentReminder()
        if (paid && reminder != null && isRecurringReminder(reminder.frecuencia)) {
            val nextDue = nextRecurringDueDate(reminder.fechaVencimiento, reminder.frecuencia)
            reminder.fechaVencimiento = nextDue
            reminder.isPagado = false
            val updated = db.recordatorioDao().updateById(
                id = reminder.id,
                userId = userId,
                titulo = reminder.titulo ?: "",
                monto = reminder.monto,
                moneda = CurrencyConverter.normalize(reminder.moneda),
                fechaVencimiento = nextDue?.time ?: 0,
                pagado = 0,
                categoriaId = reminder.categoriaId,
                horaRecordatorio = reminder.horaRecordatorio,
                frecuencia = reminder.frecuencia,
                notificar = if (reminder.isNotificar) 1 else 0,
                diasRecordatorio = reminder.diasRecordatorio,
                googleEventId = reminder.googleEventId,
                notificationId = reminder.notificationId
            ) > 0
            if (updated) {
                if (reminder.isNotificar) ReminderScheduler.schedule(appContext, reminder) else ReminderScheduler.cancel(appContext, reminder)
                bumpDataVersion()
            }
            return@withContext updated
        }
        (db.recordatorioDao().markPaid(id, userId, if (paid) 1 else 0) > 0).also {
            if (it) {
                reminder?.let { existing ->
                    existing.isPagado = paid
                    if (paid) ReminderScheduler.cancel(appContext, existing) else ReminderScheduler.schedule(appContext, existing)
                }
                bumpDataVersion()
            }
        }
    }

    suspend fun deleteReminder(id: Int): Boolean = withContext(Dispatchers.IO) {
        val userId = currentUserId()
        val reminder = db.recordatorioDao().listAll(userId).firstOrNull { it.id == id }?.toPaymentReminder()
        (db.recordatorioDao().deleteById(id, userId) > 0).also {
            if (it) {
                reminder?.let { existing -> ReminderScheduler.cancel(appContext, existing) }
                bumpDataVersion()
            }
        }
    }

    private fun RecordatorioEntity.toPaymentReminder(): PaymentReminder {
        return PaymentReminder().apply {
            id = this@toPaymentReminder.id
            titulo = this@toPaymentReminder.titulo
            monto = this@toPaymentReminder.monto
            moneda = CurrencyConverter.normalize(this@toPaymentReminder.moneda)
            if (this@toPaymentReminder.fechaVencimiento > 0) fechaVencimiento = Date(this@toPaymentReminder.fechaVencimiento)
            pagado = this@toPaymentReminder.pagado == 1
            categoriaId = this@toPaymentReminder.categoriaId
            horaRecordatorio = this@toPaymentReminder.horaRecordatorio
            frecuencia = this@toPaymentReminder.frecuencia
            notificar = this@toPaymentReminder.notificar == 1
            diasRecordatorio = this@toPaymentReminder.diasRecordatorio
            googleEventId = this@toPaymentReminder.googleEventId
            notificationId = this@toPaymentReminder.notificationId
            diasRestantes = daysUntilLocalDate(this@toPaymentReminder.fechaVencimiento)
        }
    }

    private fun daysUntilLocalDate(dateMillis: Long): Int {
        if (dateMillis <= 0L) return 0
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val due = Instant.ofEpochMilli(dateMillis).atZone(zone).toLocalDate()
        return ChronoUnit.DAYS.between(today, due).coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt()
    }

    private fun isRecurringReminder(frequency: String?): Boolean {
        return "mensual".equals(frequency, ignoreCase = true) ||
            "trimestral".equals(frequency, ignoreCase = true)
    }

    private fun nextRecurringDueDate(currentDueDate: Date?, frequency: String?): Date? {
        if (currentDueDate == null) return null
        val months = when {
            "mensual".equals(frequency, ignoreCase = true) -> 1L
            "trimestral".equals(frequency, ignoreCase = true) -> 3L
            else -> return currentDueDate
        }
        val zone = ZoneId.systemDefault()
        val currentDue = currentDueDate.toInstant().atZone(zone).toLocalDate()
        val today = LocalDate.now(zone)
        var nextDue = currentDue.plusMonths(months)
        while (!nextDue.isAfter(today)) {
            nextDue = nextDue.plusMonths(months)
        }
        return Date.from(nextDue.atStartOfDay(zone).toInstant())
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
        SettingsService.clearFinancialAccounts(appContext)
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
        val existing = db.transaccionDao().listAll(userId).toMutableList()
        var procesados = 0
        var errores = 0
        var duplicados = 0
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
                    ?: row.categoriaId
                    ?: categoryIdFromImportRow(row, categorias, esIngreso)
                    ?: categorias.firstOrNull { it.esIngreso == if (esIngreso) 1 else 0 }?.id
                    ?: 0

                if (categoriaId <= 0 || row.monto <= 0.0 || row.fecha <= 0L) {
                    errores++
                    return@forEach
                }

                val nota = rule?.nota?.takeIf { it.isNotBlank() } ?: row.nota?.takeIf { it.isNotBlank() } ?: row.descripcion
                val currency = CurrencyConverter.normalize(row.moneda ?: baseCurrency())
                if (isDuplicateImport(existing, row, categoriaId, esIngreso, currency, nota)) {
                    duplicados++
                    return@forEach
                }
                try {
                    validateSufficientBalance(
                        userId = userId,
                        isIncome = esIngreso,
                        amount = row.monto,
                        currency = currency,
                        accountType = "CARD",
                        excludingTransactionId = null
                    )
                } catch (e: InsufficientBalanceException) {
                    omitidosSaldo++
                    return@forEach
                }
                val entity = TransaccionEntity(
                    userId = userId,
                    categoriaId = categoriaId,
                    esIngreso = if (esIngreso) 1 else 0,
                    monto = row.monto,
                    moneda = currency,
                    fecha = row.fecha,
                    accountType = "CARD",
                    nota = nota
                )
                db.transaccionDao().insert(entity)
                existing.add(entity)
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
            put("duplicados", duplicados)
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
            val moneda = item.optString("moneda", "").takeIf { it.isNotBlank() }
            val categoriaId = if (item.has("categoria_id")) item.optInt("categoria_id", 0).takeIf { it > 0 } else null
            val categoriaNombre = item.optString("categoria", "").takeIf { it.isNotBlank() }
            val nota = item.optString("nota", "").takeIf { it.isNotBlank() }
            if (fecha > 0L && monto > 0.0) {
                rows.add(ImportRow(fecha, descripcion, monto, esIngreso, moneda, categoriaId, categoriaNombre, nota))
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
                val moneda = parts.getOrNull(4)?.takeIf { it.isNotBlank() }
                val categoria = parts.getOrNull(5)?.takeIf { it.isNotBlank() }
                if (fecha <= 0L || monto <= 0.0) null else ImportRow(fecha, descripcion, monto, esIngreso, moneda, null, categoria, null)
            }
            .toList()
    }

    private fun categoryIdFromImportRow(row: ImportRow, categorias: List<CategoriaEntity>, esIngreso: Boolean): Int? {
        val name = row.categoriaNombre?.trim()?.takeIf { it.isNotBlank() } ?: return null
        return categorias.firstOrNull {
            it.esIngreso == (if (esIngreso) 1 else 0) && it.nombre.equals(name, ignoreCase = true)
        }?.id
    }

    private fun isDuplicateImport(
        existing: List<TransaccionEntity>,
        row: ImportRow,
        categoriaId: Int,
        esIngreso: Boolean,
        moneda: String,
        nota: String
    ): Boolean {
        return existing.any {
            it.fecha == row.fecha &&
                it.categoriaId == categoriaId &&
                it.esIngreso == (if (esIngreso) 1 else 0) &&
                kotlin.math.abs(it.monto - row.monto) < BALANCE_EPSILON &&
                CurrencyConverter.normalize(it.moneda).equals(moneda, ignoreCase = true) &&
                (it.nota ?: "").trim().equals(nota.trim(), ignoreCase = true)
        }
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

    private fun buildAccountBalances(transactionsInBase: List<Transaccion>): List<AccountBalance> {
        val balances = linkedMapOf<String, Double>()
        for (tx in transactionsInBase) {
            val accountType = normalizeAccountType(tx.accountType)
            if (tx.isTransfer) {
                val destination = normalizeAccountType(tx.transferDestinationAccountType)
                balances[accountType] = (balances[accountType] ?: 0.0) - tx.monto
                if (destination != accountType) {
                    balances[destination] = (balances[destination] ?: 0.0) + tx.monto
                }
                continue
            }
            if (accountType == "CASH") continue
            balances[accountType] = (balances[accountType] ?: 0.0) + if (tx.isEsIngreso) tx.monto else -tx.monto
        }

        val output = mutableListOf<AccountBalance>()
        output.add(AccountBalance("CARD", "Tarjeta", balances["CARD"] ?: 0.0, true))

        val known = mutableSetOf("CARD")
        for (account in SettingsService.listFinancialAccounts(appContext)) {
            val id = normalizeAccountType(account.id)
            known.add(id)
            output.add(AccountBalance(id, account.name, balances[id] ?: 0.0, false))
        }

        for ((id, balance) in balances) {
            if (id == "CASH" || id in known) continue
            output.add(AccountBalance(id, SettingsService.getFinancialAccountName(appContext, id), balance, false))
        }
        return output
    }

    private fun accountDelta(tx: Transaccion, accountType: String): Double {
        val target = normalizeAccountType(accountType)
        val source = normalizeAccountType(tx.accountType)
        if (tx.isTransfer) {
            val destination = normalizeAccountType(tx.transferDestinationAccountType)
            return when (target) {
                source -> -tx.monto
                destination -> tx.monto
                else -> 0.0
            }
        }
        if (source != target) return 0.0
        return if (tx.isEsIngreso) tx.monto else -tx.monto
    }

    private fun isRealIncome(tx: Transaccion): Boolean = tx.isEsIngreso && !tx.isInitialBalance && !tx.isTransfer

    private fun isRealExpense(tx: Transaccion): Boolean = !tx.isEsIngreso && !tx.isTransfer

    private fun movementTypeLabel(tx: Transaccion): String = when {
        tx.isTransfer -> "transferencia"
        tx.isEsIngreso -> "ingreso"
        else -> "gasto"
    }

    suspend fun buildHomeSummary(anio: Int, mes: Int): HomeSummary = withContext(Dispatchers.IO) {
        val summary = HomeSummary()
        summary.anio = anio
        summary.mes = mes

        val base = baseCurrency()
        val rate = manualRate()
        val trans = listTransacciones(anio, mes).map { it.toBaseCurrencyCopy(base, rate) }
        val ingresos = trans.filter { it.isEsIngreso && !it.isTransfer }.sumOf { it.monto }
        val gastos = trans.filter { isRealExpense(it) }.sumOf { it.monto }
        val initialBalanceIncome = trans.filter { it.isInitialBalance }.sumOf { it.monto }
        val recurringIncome = (ingresos - initialBalanceIncome).coerceAtLeast(0.0)
        summary.ingresos = ingresos
        summary.gastos = gastos
        summary.saldo = ingresos - gastos
        summary.ingresosRecurrentes = recurringIncome
        summary.balanceVisibleMes = summary.saldo
        summary.balanceOperativoMes = recurringIncome - gastos

        val allRawTrans = listTodasTransacciones()
        val allTrans = allRawTrans.map { it.toBaseCurrencyCopy(base, rate) }
        val includedCardIds = SettingsService.listCardAccounts(appContext)
            .filter { it.isIncludedInTotal }
            .map { normalizeAccountType(it.id) }
            .toSet()
        val initialCash = allTrans.sumOf { if (it.isInitialBalance && it.isCash) it.monto else 0.0 }
        val initialCard = allTrans.sumOf { if (it.isInitialBalance && !it.isCash) it.monto else 0.0 }
        val cashMovement = allTrans.sumOf { accountDelta(it, "CASH") }
        val nonCashMovement = allTrans.sumOf {
            val source = normalizeAccountType(it.accountType)
            if (it.isTransfer) {
                val destination = normalizeAccountType(it.transferDestinationAccountType)
                (if (source != "CASH" && includedCardIds.contains(source)) -it.monto else 0.0) +
                    (if (destination != "CASH" && includedCardIds.contains(destination)) it.monto else 0.0)
            } else if (!it.isCash) {
                if (includedCardIds.contains(source)) {
                    if (it.isEsIngreso) it.monto else -it.monto
                } else {
                    0.0
                }
            } else {
                0.0
            }
        }
        summary.initialCashBalance = initialCash
        summary.initialCardBalance = initialCard
        summary.efectivo = cashMovement
        summary.tarjetaCuenta = nonCashMovement
        summary.saldoActualTotal = summary.efectivo + nonCashMovement
        summary.accountBalances.addAll(buildAccountBalances(allTrans))
        summary.latestTransactions.addAll(
            allRawTrans
                .filter { !it.isInitialBalance }
                .sortedWith(compareByDescending<Transaccion> { it.fecha?.time ?: 0L }.thenByDescending { it.id })
                .take(5)
        )

        val presMonto = getPresupuesto(anio, mes)
        summary.presupuestoMonto = presMonto
        val presRestante = presMonto - gastos
        summary.presupuestoRestante = presRestante
        summary.presupuestoPorcentaje = if (presMonto > 0) (gastos / presMonto) * 100.0 else 0.0
        summary.presupuestoExcedido = presRestante < 0
        summary.gastoProyectado = gastos
        val daysInMonth = daysInMonth(anio, mes)
        val elapsedDays = elapsedDaysForMonth(anio, mes, daysInMonth)
        summary.gastoPromedioDiario = gastos / elapsedDays.coerceAtLeast(1)
        summary.diasRestantes = (daysInMonth - elapsedDays).coerceAtLeast(0)
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
        val categoriasFast = db.categoriaDao().listForUser(userId).associateBy { it.id }
        val ingresos = monthTrans
            .filter { it.esIngreso == 1 && !isTransferEntity(it, categoriasFast) }
            .sumOf { convertToBase(it.monto, it.moneda, base, rate) }
        val gastos = monthTrans
            .filter { it.esIngreso == 0 && !isTransferEntity(it, categoriasFast) }
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
        val categorias = categoriasFast
        val allTransModels = allTrans
            .map {
                Transaccion(
                    it.id,
                    it.categoriaId,
                    categorias[it.categoriaId]?.nombre ?: "",
                    it.esIngreso == 1,
                    convertToBase(it.monto, it.moneda, base, rate),
                    base,
                    Date(it.fecha),
                    normalizeAccountType(it.accountType),
                    it.nota
                )
            }
            .sortedWith(compareByDescending<Transaccion> { it.fecha?.time ?: 0L }.thenByDescending { it.id })
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
        val cashMovement = allTrans.sumOf { signedAccountDelta(it, "CASH", base, rate) }
        val includedCardIds = SettingsService.listCardAccounts(appContext)
            .filter { it.isIncludedInTotal }
            .map { normalizeAccountType(it.id) }
            .toSet()
        val nonCashMovement = allTrans.sumOf {
            val source = normalizeAccountType(it.accountType)
            val destination = transferDestination(it.nota)
            val amount = convertToBase(it.monto, it.moneda, base, rate)
            if (destination != null) {
                (if (source != "CASH" && includedCardIds.contains(source)) -amount else 0.0) +
                    (if (destination != "CASH" && includedCardIds.contains(destination)) amount else 0.0)
            } else if (source != "CASH") {
                if (includedCardIds.contains(source)) {
                    if (it.esIngreso == 1) amount else -amount
                } else {
                    0.0
                }
            } else {
                0.0
            }
        }
        summary.initialCashBalance = initialCash
        summary.initialCardBalance = initialCard
        summary.efectivo = cashMovement
        summary.tarjetaCuenta = nonCashMovement
        summary.saldoActualTotal = summary.efectivo + nonCashMovement
        summary.accountBalances.addAll(buildAccountBalances(allTransModels))
        summary.latestTransactions.addAll(allTransModels.filter { !it.isInitialBalance }.take(5))

        val presMonto = findEffectivePresupuesto(userId, anio, mes)
            ?.let { convertToBase(it.monto, it.moneda, base, rate) }
            ?: 0.0
        summary.presupuestoMonto = presMonto
        summary.presupuestoRestante = presMonto - gastos
        summary.presupuestoPorcentaje = if (presMonto > 0) (gastos / presMonto) * 100.0 else 0.0
        summary.presupuestoExcedido = summary.presupuestoRestante < 0
        summary.gastoProyectado = gastos
        val daysInMonth = daysInMonth(anio, mes)
        val elapsedDays = elapsedDaysForMonth(anio, mes, daysInMonth)
        summary.gastoPromedioDiario = gastos / elapsedDays.coerceAtLeast(1)
        summary.diasRestantes = (daysInMonth - elapsedDays).coerceAtLeast(0)

        summary
    }

    private fun daysInMonth(anio: Int, mes: Int): Int {
        return Calendar.getInstance().apply {
            clear()
            set(anio, mes - 1, 1)
        }.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    private fun elapsedDaysForMonth(anio: Int, mes: Int, daysInMonth: Int): Int {
        val today = Calendar.getInstance()
        return if (today.get(Calendar.YEAR) == anio && today.get(Calendar.MONTH) + 1 == mes) {
            today.get(Calendar.DAY_OF_MONTH).coerceIn(1, daysInMonth)
        } else {
            daysInMonth
        }
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

    private fun ensureTransferCategory(userId: Int): Int {
        db.categoriaDao().listForUser(userId)
            .firstOrNull { it.nombre.equals(Transaccion.TRANSFER_CATEGORY, ignoreCase = true) }
            ?.let { return it.id }

        val nextId = (db.categoriaDao().listAll().maxOfOrNull { it.id } ?: 0) + 1
        db.categoriaDao().insert(
            CategoriaEntity(
                id = nextId,
                userId = userId,
                nombre = Transaccion.TRANSFER_CATEGORY,
                esIngreso = 0
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
            clean.equals(Transaccion.INITIAL_BALANCE_CATEGORY, ignoreCase = true) ||
            clean.startsWith(Transaccion.INITIAL_BALANCE_ACCOUNT_NOTE_PREFIX, ignoreCase = true)
    }

    private fun isValidAmount(value: Double): Boolean {
        return !value.isNaN() && !value.isInfinite() && value >= 0.0
    }

    private fun normalizeAccountType(value: String?): String {
        return SettingsService.normalizeAccountType(value)
    }
}
