package com.example.finanzas.data.api

import android.content.Context
import com.example.finanzas.data.local.LocalRepository
import com.example.finanzas.data.model.Categoria
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @Volatile
    private var cache: List<Categoria>? = null
    @Volatile
    private var loading = false
    private val pending = mutableListOf<Callback>()

    suspend fun load(ctx: Context): List<Categoria> = withContext(Dispatchers.IO) {
        LocalRepository.getInstance(ctx).listCategorias()
    }

    suspend fun create(ctx: Context, nombre: String, esIngreso: Boolean): Categoria = withContext(Dispatchers.IO) {
        val id = LocalRepository.getInstance(ctx).createCategoria(nombre, esIngreso)
        Categoria(id, nombre, esIngreso)
    }

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

        scope.launch {
            runCatching { load(ctx) }
                .onSuccess { cats ->
                    val callbacks = synchronized(this@CategoryStore) {
                        cache = cats
                        loading = false
                        pending.toList().also { pending.clear() }
                    }
                    callbacks.forEach { it.onReady(cats) }
                }
                .onFailure {
                    val callbacks = synchronized(this@CategoryStore) {
                        loading = false
                        pending.toList().also { pending.clear() }
                    }
                    callbacks.forEach { it.onError() }
                }
        }
    }

    @JvmStatic
    fun createCategoria(ctx: Context, nombre: String, esIngreso: Boolean, cb: CreateCallback) {
        scope.launch {
            runCatching { create(ctx, nombre, esIngreso) }
                .onSuccess { nueva ->
                    synchronized(this@CategoryStore) {
                        val current = cache
                        if (current != null) {
                            val updated = current.toMutableList()
                            updated.add(nueva)
                            updated.sortBy { it.nombre ?: "" }
                            cache = Collections.unmodifiableList(updated)
                        }
                    }
                    cb.onReady(nueva)
                }
                .onFailure { cb.onError() }
        }
    }
}
