package com.example.finanzas.data.api

import android.content.Context
import com.example.finanzas.data.local.LocalRepository
import com.example.finanzas.data.model.Categoria
import java.util.Collections

object CategoryStore {

    interface Callback {
        fun onReady(cats: List<Categoria>)
        fun onError()
    }
    interface CreateCallback {
        fun onReady(categoria: Categoria)
        fun onError()
    }

    @Volatile
    private var cache: List<Categoria>? = null
    @Volatile
    private var loading = false
    private val pending = mutableListOf<Callback>()

    @JvmStatic
    @Synchronized
    fun loadOnce(ctx: Context, cb: Callback) {
        val localCache = cache
        if (localCache != null) {
            cb.onReady(localCache)
            return
        }

        pending.add(cb)
        if (loading) return
        loading = true

        DbCoroutine.io(
            block = { LocalRepository.getInstance(ctx).listCategorias() },
            onSuccess = { cats ->
                val callbacks = synchronized(this) {
                    cache = cats
                    loading = false
                    val copy = pending.toList()
                    pending.clear()
                    copy
                }
                callbacks.forEach { it.onReady(cats) }
            },
            onError = {
                val callbacks = synchronized(this) {
                    loading = false
                    val copy = pending.toList()
                    pending.clear()
                    copy
                }
                callbacks.forEach { it.onError() }
            }
        )
    }

    @JvmStatic
    fun createCategoria(ctx: Context, nombre: String, esIngreso: Boolean, cb: CreateCallback) {
        DbCoroutine.io(
            block = {
                val id = LocalRepository.getInstance(ctx).createCategoria(nombre, esIngreso)
                Categoria(id, nombre, esIngreso)
            },
            onSuccess = { nueva ->
                synchronized(this) {
                    val current = cache
                    if (current != null) {
                        val updated = current.toMutableList()
                        updated.add(nueva)
                        updated.sortBy { it.nombre ?: "" }
                        cache = Collections.unmodifiableList(updated)
                    }
                }
                cb.onReady(nueva)
            },
            onError = cb::onError
        )
    }
}
