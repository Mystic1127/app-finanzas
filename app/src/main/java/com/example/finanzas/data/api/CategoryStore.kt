package com.example.finanzas.data.api

import android.content.Context
import com.example.finanzas.data.cloud.CloudSyncService
import com.example.finanzas.data.local.LocalRepository
import com.example.finanzas.data.model.Categoria
import com.example.finanzas.util.Prefs
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

    interface SimpleCallback {
        fun onSuccess()
        fun onError()
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @Volatile
    private var cacheUserId: Long = 0L
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
        CloudSyncService.scheduleUpload(ctx)
        Categoria(id, Prefs.getCurrentUserId(ctx).toInt(), nombre, esIngreso, true).also { nueva ->
            val userId = Prefs.getCurrentUserId(ctx)
            synchronized(this@CategoryStore) {
                val current = cache
                if (current != null && cacheUserId == userId) {
                    cache = Collections.unmodifiableList(
                        (current + nueva).distinctBy { it.id }.sortedBy { it.nombre ?: "" }
                    )
                } else {
                    cache = null
                    loading = false
                }
            }
        }
    }

    suspend fun update(ctx: Context, categoria: Categoria): Boolean = withContext(Dispatchers.IO) {
        LocalRepository.getInstance(ctx).updateCategoria(categoria.id, categoria.nombre ?: "", categoria.esIngreso)
            .also {
                if (it) {
                    clearCache()
                    CloudSyncService.scheduleUpload(ctx)
                }
            }
    }

    suspend fun delete(ctx: Context, categoriaId: Int): Boolean = withContext(Dispatchers.IO) {
        LocalRepository.getInstance(ctx).deleteCategoria(categoriaId)
            .also {
                if (it) {
                    clearCache()
                    CloudSyncService.scheduleUpload(ctx)
                }
            }
    }

    @JvmStatic
    @Synchronized
    fun loadOnce(ctx: Context, cb: Callback) {
        val userId = Prefs.getCurrentUserId(ctx)
        if (cacheUserId != userId) {
            cache = null
            loading = false
            pending.clear()
        }
        val localCache = cache
        if (localCache != null && cacheUserId == userId) {
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
                        cacheUserId = userId
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
        val userId = Prefs.getCurrentUserId(ctx)
        scope.launch {
            runCatching { create(ctx, nombre, esIngreso) }
                .onSuccess { nueva ->
                    synchronized(this@CategoryStore) {
                        val current = cache
                        if (current != null && cacheUserId == userId) {
                            cache = Collections.unmodifiableList(
                                (current + nueva).distinctBy { it.id }.sortedBy { it.nombre ?: "" }
                            )
                        }
                    }
                    cb.onReady(nueva)
                }
                .onFailure { cb.onError() }
        }
    }

    @JvmStatic
    fun updateCategoria(ctx: Context, categoria: Categoria, cb: SimpleCallback) {
        scope.launch {
            runCatching { update(ctx, categoria) }
                .onSuccess { if (it) cb.onSuccess() else cb.onError() }
                .onFailure { cb.onError() }
        }
    }

    @JvmStatic
    fun deleteCategoria(ctx: Context, categoriaId: Int, cb: SimpleCallback) {
        scope.launch {
            runCatching { delete(ctx, categoriaId) }
                .onSuccess { if (it) cb.onSuccess() else cb.onError() }
                .onFailure { cb.onError() }
        }
    }

    @JvmStatic
    @Synchronized
    fun clearCache() {
        cacheUserId = 0L
        cache = null
        loading = false
        pending.clear()
    }
}
