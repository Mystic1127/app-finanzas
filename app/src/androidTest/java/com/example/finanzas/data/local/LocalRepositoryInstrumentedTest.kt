package com.example.finanzas.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.finanzas.data.api.SettingsService
import com.example.finanzas.data.local.room.AppRoomDatabase
import com.example.finanzas.data.local.room.CategoriaEntity
import com.example.finanzas.data.model.ImportRule
import com.example.finanzas.util.Prefs
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Constructor
import java.util.Calendar

class LocalRepositoryInstrumentedTest {

    private lateinit var context: Context
    private lateinit var db: AppRoomDatabase
    private lateinit var repository: LocalRepository

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        Prefs.clearAuth(context)
        db = Room.inMemoryDatabaseBuilder(context, AppRoomDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = newRepository(db, context)

        db.categoriaDao().insert(CategoriaEntity(id = 1, nombre = "Sueldo", esIngreso = 1))
        db.categoriaDao().insert(CategoriaEntity(id = 2, nombre = "Comida", esIngreso = 0))
        db.categoriaDao().insert(CategoriaEntity(id = 3, nombre = "Transporte", esIngreso = 0))
    }

    @After
    fun tearDown() {
        Prefs.clearAuth(context)
        db.close()
    }

    @Test
    fun multiUserRepositoryKeepsDataIsolated() = runBlocking {
        val userA = createUser("ana@test.com", "Ana")
        val userB = createUser("beto@test.com", "Beto")
        val fecha = millis(2026, 4, 10)

        loginAs(userA, "ana@test.com", "Ana")
        repository.createTransaccion(categoriaId = 2, esIngreso = false, monto = 50.0, nota = "Almuerzo", fecha = fecha)
        assertEquals(1, repository.listTransacciones(2026, 4).size)

        loginAs(userB, "beto@test.com", "Beto")
        assertEquals(0, repository.listTransacciones(2026, 4).size)
        repository.createTransaccion(categoriaId = 3, esIngreso = false, monto = 20.0, nota = "Bus", fecha = fecha)
        assertEquals(1, repository.listTransacciones(2026, 4).size)

        loginAs(userA, "ana@test.com", "Ana")
        val userAItems = repository.listTransacciones(2026, 4)
        assertEquals(1, userAItems.size)
        assertEquals("Almuerzo", userAItems.first().nota)
    }

    @Test
    fun processImportCreatesTransactionsForCurrentUser() = runBlocking {
        val userA = createUser("imports-a@test.com", "Import A")
        val userB = createUser("imports-b@test.com", "Import B")
        val input = JSONArray()
            .put(JSONObject().put("fecha", "2026-04-03").put("descripcion", "Supermercado").put("monto", 125.50).put("es_ingreso", 0))
            .put(JSONObject().put("fecha", "2026-04-04").put("descripcion", "Nomina").put("monto", 2500.00).put("es_ingreso", 1))
            .toString()

        loginAs(userA, "imports-a@test.com", "Import A")
        repository.saveImportRule(ImportRule().apply {
            patron = "Super"
            isEsIngreso = false
            categoriaId = 2
            nota = "Compra importada"
        })
        val importId = repository.createImport("Banco", "csv", input)
        val result = repository.processImport(importId)

        assertEquals(2, result.getInt("procesados"))
        assertEquals(0, result.getInt("errores"))
        val userATransactions = repository.listTransacciones(2026, 4)
        assertEquals(2, userATransactions.size)
        assertTrue(userATransactions.any { it.nota == "Compra importada" && it.categoriaId == 2 })
        assertTrue(userATransactions.any { it.isEsIngreso && it.monto == 2500.0 })

        loginAs(userB, "imports-b@test.com", "Import B")
        assertEquals(0, repository.listTransacciones(2026, 4).size)
    }

    @Test
    fun budgetCreateAndUpdateAreScopedToCurrentUser() = runBlocking {
        val userA = createUser("budget-a@test.com", "Budget A")
        val userB = createUser("budget-b@test.com", "Budget B")

        loginAs(userA, "budget-a@test.com", "Budget A")
        repository.setPresupuesto(2026, 4, 1000.0)
        assertEquals(1000.0, repository.getPresupuesto(2026, 4), 0.001)
        repository.setPresupuesto(2026, 4, 1800.0)
        assertEquals(1800.0, repository.getPresupuesto(2026, 4), 0.001)

        loginAs(userB, "budget-b@test.com", "Budget B")
        assertEquals(0.0, repository.getPresupuesto(2026, 4), 0.001)
        repository.setPresupuesto(2026, 4, 250.0)
        assertEquals(250.0, repository.getPresupuesto(2026, 4), 0.001)

        loginAs(userA, "budget-a@test.com", "Budget A")
        assertEquals(1800.0, repository.getPresupuesto(2026, 4), 0.001)
    }

    @Test
    fun invalidTransactionDataIsRejected() = runBlocking {
        val userA = createUser("invalid@test.com", "Invalid")
        loginAs(userA, "invalid@test.com", "Invalid")

        assertInvalidTransaction {
            repository.createTransaccion(categoriaId = 2, esIngreso = false, monto = 0.0, nota = "No debe entrar", fecha = millis(2026, 4, 1))
        }
        assertInvalidTransaction {
            repository.createTransaccion(categoriaId = 0, esIngreso = false, monto = 10.0, nota = "Sin categoria", fecha = millis(2026, 4, 1))
        }

        assertEquals(0, repository.listTransacciones(2026, 4).size)
    }

    @Test
    fun invalidImportRowsAreNotInserted() = runBlocking {
        val userA = createUser("bad-import@test.com", "Bad Import")
        loginAs(userA, "bad-import@test.com", "Bad Import")
        val input = JSONArray()
            .put(JSONObject().put("fecha", "fecha-mal").put("descripcion", "Fila invalida").put("monto", 100.0).put("es_ingreso", 0))
            .put(JSONObject().put("fecha", "2026-04-04").put("descripcion", "Monto cero").put("monto", 0.0).put("es_ingreso", 0))
            .toString()

        val importId = repository.createImport("Archivo invalido", "csv", input)
        val result = repository.processImport(importId)

        assertEquals(0, result.getInt("procesados"))
        assertEquals(0, repository.listTransacciones(2026, 4).size)
    }

    @Test
    fun newlyCreatedCategoryCanBeUsedByTransactionWithCashAndTime() = runBlocking {
        val userA = createUser("cash-category@test.com", "Cash Category")
        loginAs(userA, "cash-category@test.com", "Cash Category")

        val categoryId = repository.createCategoria("Mascotas", false)
        val fecha = millis(2026, 4, 12, 17, 45)
        repository.createTransaccion(
            categoriaId = categoryId,
            esIngreso = false,
            monto = 33.0,
            nota = "Arena",
            fecha = fecha,
            moneda = "PEN",
            accountType = "CASH"
        )

        val items = repository.listTransacciones(2026, 4)
        assertEquals(1, items.size)
        assertEquals("Mascotas", items.first().categoriaNombre)
        assertTrue(items.first().isCash)
        assertEquals(fecha, items.first().fecha.time)
    }

    @Test
    fun explicitCurrenciesAreKeptForGoalsAndReminders() = runBlocking {
        val userA = createUser("currency@test.com", "Currency")
        loginAs(userA, "currency@test.com", "Currency")

        assertTrue(repository.saveGoal(0, "Viaje", 100.0, 25.0, null, "USD"))
        val goal = repository.listGoals().first()
        assertEquals("USD", goal.moneda)

        val reminder = com.example.finanzas.data.model.PaymentReminder().apply {
            titulo = "Seguro"
            monto = 80.0
            moneda = "EUR"
            fechaVencimiento = java.util.Date(millis(2026, 4, 20))
            notificar = false
        }
        assertTrue(repository.saveReminder(reminder))
        assertEquals("EUR", repository.listReminders(true).first().moneda)
    }

    @Test
    fun initialBalancesAreScopedByCurrentUser() = runBlocking {
        val userA = createUser("initial-a@test.com", "Initial A")
        val userB = createUser("initial-b@test.com", "Initial B")

        loginAs(userA, "initial-a@test.com", "Initial A")
        saveInitialBalances(120.0, 350.0, "PEN")
        val userASummary = repository.buildHomeSummary(2026, 4)
        assertEquals(120.0, userASummary.efectivo, 0.001)
        assertEquals(350.0, userASummary.tarjetaCuenta, 0.001)
        assertEquals(470.0, userASummary.saldoActualTotal, 0.001)
        assertEquals(0.0, userASummary.ingresos, 0.001)
        assertEquals(0.0, userASummary.saldo, 0.001)

        loginAs(userB, "initial-b@test.com", "Initial B")
        val userBSummary = repository.buildHomeSummary(2026, 4)
        assertEquals(0.0, userBSummary.efectivo, 0.001)
        assertEquals(0.0, userBSummary.tarjetaCuenta, 0.001)
        assertEquals(0.0, userBSummary.saldoActualTotal, 0.001)
    }

    @Test
    fun initialBalancesAndAccountTypedTransactionsBuildCurrentBalances() = runBlocking {
        val userA = createUser("initial-tx@test.com", "Initial Tx")
        loginAs(userA, "initial-tx@test.com", "Initial Tx")
        saveInitialBalances(100.0, 500.0, "PEN")

        repository.createTransaccion(
            categoriaId = 1,
            esIngreso = true,
            monto = 80.0,
            nota = "Ingreso efectivo",
            fecha = millis(2026, 4, 3),
            moneda = "PEN",
            accountType = "CASH"
        )
        repository.createTransaccion(
            categoriaId = 2,
            esIngreso = false,
            monto = 30.0,
            nota = "Gasto efectivo",
            fecha = millis(2026, 4, 4),
            moneda = "PEN",
            accountType = "CASH"
        )
        repository.createTransaccion(
            categoriaId = 2,
            esIngreso = false,
            monto = 50.0,
            nota = "Gasto tarjeta",
            fecha = millis(2026, 4, 5),
            moneda = "PEN",
            accountType = "CARD"
        )

        val summary = repository.buildHomeSummary(2026, 4)
        assertEquals(150.0, summary.efectivo, 0.001)
        assertEquals(450.0, summary.tarjetaCuenta, 0.001)
        assertEquals(600.0, summary.saldoActualTotal, 0.001)
        assertEquals(80.0, summary.ingresos, 0.001)
        assertEquals(80.0, summary.gastos, 0.001)
        assertEquals(0.0, summary.saldo, 0.001)
    }

    private suspend fun createUser(email: String, name: String): Int {
        val out = IntArray(1)
        assertTrue(repository.registerUser(name, email, "123456", out))
        return out[0]
    }

    private suspend fun assertInvalidTransaction(block: suspend () -> Unit) {
        var thrown = false
        try {
            block()
        } catch (expected: IllegalArgumentException) {
            thrown = true
        }
        assertTrue(thrown)
    }

    private fun loginAs(userId: Int, email: String, name: String) {
        Prefs.setUserSession(context, userId.toLong(), email, name)
    }

    private fun saveInitialBalances(cash: Double, card: Double, currency: String) {
        var ok = false
        SettingsService.saveInitialBalances(context, cash, card, currency, object : SettingsService.SaveCb {
            override fun onSuccess() {
                ok = true
            }

            override fun onFail() {
                ok = false
            }
        })
        assertTrue(ok)
    }

    private fun millis(year: Int, month: Int, day: Int): Long = millis(year, month, day, 12, 0)

    private fun millis(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun newRepository(db: AppRoomDatabase, context: Context): LocalRepository {
        val constructor: Constructor<LocalRepository> =
            LocalRepository::class.java.getDeclaredConstructor(AppRoomDatabase::class.java, Context::class.java)
        constructor.isAccessible = true
        return constructor.newInstance(db, context.applicationContext)
    }
}
