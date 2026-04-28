package com.example.finanzas.data.api

import android.content.Context
import com.example.finanzas.data.local.LocalRepository

object AuthService {

    interface Callback {
        fun onSuccess(token: String, userId: Int, nombre: String?, email: String?)
        fun onError()
    }

    @JvmStatic
    fun login(ctx: Context, email: String, pass: String, cb: Callback) {
        DbCoroutine.io(
            block = {
                val holder = LocalRepository.UserHolder()
                val ok = LocalRepository.getInstance(ctx).login(email, pass, holder)
                if (!ok) null else holder
            },
            onSuccess = { holder ->
                if (holder == null) cb.onError()
                else cb.onSuccess("local-token", holder.id, holder.nombre, holder.email)
            },
            onError = cb::onError
        )
    }

    @JvmStatic
    fun register(ctx: Context, nombre: String, email: String, pass: String, cb: Callback) {
        DbCoroutine.io(
            block = {
                val outId = IntArray(1)
                val ok = LocalRepository.getInstance(ctx).registerUser(nombre, email, pass, outId)
                if (ok) outId[0] else -1
            },
            onSuccess = { id ->
                if (id > 0) cb.onSuccess("local-token", id, nombre, email) else cb.onError()
            },
            onError = cb::onError
        )
    }
}
