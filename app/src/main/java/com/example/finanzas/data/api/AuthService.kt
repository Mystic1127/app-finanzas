package com.example.finanzas.data.api

import android.content.Context
import com.example.finanzas.data.local.LocalRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AuthResult(
    val token: String,
    val userId: Int,
    val nombre: String?,
    val email: String?
)

object AuthService {

    suspend fun login(ctx: Context, email: String, pass: String): AuthResult? = withContext(Dispatchers.IO) {
        val holder = LocalRepository.UserHolder()
        val ok = LocalRepository.getInstance(ctx).login(email, pass, holder)
        if (!ok) null else AuthResult("local-token", holder.id, holder.nombre, holder.email)
    }

    suspend fun register(ctx: Context, nombre: String, email: String, pass: String): AuthResult? = withContext(Dispatchers.IO) {
        val outId = IntArray(1)
        val ok = LocalRepository.getInstance(ctx).registerUser(nombre, email, pass, outId)
        if (!ok) null else AuthResult("local-token", outId[0], nombre, email)
    }
}
