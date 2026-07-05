package com.example.finanzas.data.local.room

import android.content.Context
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RoomUserIsolationInstrumentedTest {

    private lateinit var context: Context
    private lateinit var db: AppRoomDatabase

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppRoomDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        db.categoriaDao().insert(CategoriaEntity(id = 1, nombre = "Sueldo", esIngreso = 1))
        db.categoriaDao().insert(CategoriaEntity(id = 2, nombre = "Comida", esIngreso = 0))
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertAndListTransactions_filtersByUserId() {
        val userA = db.userDao().insert(UserEntity(nombre = "Ana", email = "ana@test.com", password = "x")).toInt()
        val userB = db.userDao().insert(UserEntity(nombre = "Beto", email = "beto@test.com", password = "x")).toInt()

        db.transaccionDao().insert(
            TransaccionEntity(userId = userA, categoriaId = 2, esIngreso = 0, monto = 25.0, fecha = 1_714_521_600_000L, nota = "A")
        )
        db.transaccionDao().insert(
            TransaccionEntity(userId = userB, categoriaId = 2, esIngreso = 0, monto = 80.0, fecha = 1_714_521_600_000L, nota = "B")
        )

        val aItems = db.transaccionDao().listAll(userA)
        val bItems = db.transaccionDao().listAll(userB)

        assertEquals(1, aItems.size)
        assertEquals(1, bItems.size)
        assertEquals("A", aItems.first().nota)
        assertEquals("B", bItems.first().nota)
        assertTrue(aItems.none { it.userId == userB })
        assertTrue(bItems.none { it.userId == userA })
    }

    @Test
    fun deleteTransaction_requiresMatchingUserId() {
        val userA = db.userDao().insert(UserEntity(nombre = "Ana", email = "ana-delete@test.com", password = "x")).toInt()
        val userB = db.userDao().insert(UserEntity(nombre = "Beto", email = "beto-delete@test.com", password = "x")).toInt()

        val txId = db.transaccionDao().insert(
            TransaccionEntity(userId = userA, categoriaId = 2, esIngreso = 0, monto = 44.0, fecha = 1_714_521_600_000L, nota = "Solo A")
        ).toInt()

        assertEquals(0, db.transaccionDao().deleteById(txId, userB))
        assertEquals(1, db.transaccionDao().listAll(userA).size)
        assertEquals(1, db.transaccionDao().deleteById(txId, userA))
        assertEquals(0, db.transaccionDao().listAll(userA).size)
    }

    @Test
    fun budgetsAreIndependentByUserId() {
        val userA = db.userDao().insert(UserEntity(nombre = "Ana", email = "ana-budget@test.com", password = "x")).toInt()
        val userB = db.userDao().insert(UserEntity(nombre = "Beto", email = "beto-budget@test.com", password = "x")).toInt()

        db.presupuestoDao().upsert(PresupuestoEntity(userId = userA, anio = 2026, mes = 4, monto = 1200.0))
        db.presupuestoDao().upsert(PresupuestoEntity(userId = userB, anio = 2026, mes = 4, monto = 300.0))
        db.presupuestoDao().upsert(PresupuestoEntity(userId = userA, anio = 2026, mes = 4, monto = 1500.0))

        assertEquals(1500.0, db.presupuestoDao().find(userA, 2026, 4)?.monto ?: 0.0, 0.001)
        assertEquals(300.0, db.presupuestoDao().find(userB, 2026, 4)?.monto ?: 0.0, 0.001)
    }
}
