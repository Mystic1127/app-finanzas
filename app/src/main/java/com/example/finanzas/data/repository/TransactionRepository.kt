package com.example.finanzas.data.repository

import android.content.Context
import com.example.finanzas.data.api.TransService
import com.example.finanzas.data.model.Transaccion
import com.example.finanzas.data.model.TransaccionFiltro

class TransactionRepository(private val context: Context) {
    suspend fun list(anio: Int, mes: Int, filtro: TransaccionFiltro? = null): List<Transaccion> =
        TransService.list(context, anio, mes, filtro)
}
