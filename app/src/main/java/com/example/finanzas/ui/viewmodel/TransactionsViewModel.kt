package com.example.finanzas.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.finanzas.data.api.TransService
import com.example.finanzas.data.local.LocalRepository
import com.example.finanzas.data.model.Transaccion
import com.example.finanzas.data.model.TransaccionFiltro
import com.example.finanzas.util.PerfLogger
import com.example.finanzas.util.Prefs
import kotlinx.coroutines.launch

class TransactionsViewModel(application: Application) : AndroidViewModel(application) {
    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _items = MutableLiveData<List<Transaccion>>(emptyList())
    val items: LiveData<List<Transaccion>> = _items

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _exportPath = MutableLiveData<String>()
    val exportPath: LiveData<String> = _exportPath

    private val _deleted = MutableLiveData<Boolean?>()
    val deleted: LiveData<Boolean?> = _deleted

    private var loadedYear = 0
    private var loadedMonth = 0
    private var loadedVersion = -1L
    private var loadedFilterKey = ""
    private var loadedUserId = -1L

    @JvmOverloads
    fun load(anio: Int, mes: Int, filtro: TransaccionFiltro?, force: Boolean = false) {
        clearCacheIfUserChanged()
        val loadStart = PerfLogger.now()
        PerfLogger.log("ListaTransaccionesFragment", "loadStart year=$anio month=$mes force=$force")
        val version = LocalRepository.getDataVersion()
        val userId = Prefs.getCurrentUserId(getApplication())
        val filterKey = filtro.cacheKey()
        if (!force &&
            loadedVersion >= 0 &&
            loadedUserId == userId &&
            loadedYear == anio &&
            loadedMonth == mes &&
            loadedVersion == version &&
            loadedFilterKey == filterKey
        ) {
            PerfLogger.logSince("ListaTransaccionesFragment", "loadCacheHit", loadStart)
            return
        }
        _loading.value = loadedVersion < 0 || force
        viewModelScope.launch {
            runCatching { TransService.list(getApplication(), anio, mes, filtro) }
                .onSuccess {
                    loadedUserId = userId
                    loadedYear = anio
                    loadedMonth = mes
                    loadedVersion = version
                    loadedFilterKey = filterKey
                    _items.value = it
                }
                .onFailure { _error.value = it.message ?: "No se pudieron cargar las transacciones" }
            PerfLogger.logSince("ListaTransaccionesFragment", "loadComplete", loadStart)
            _loading.value = false
        }
    }

    fun delete(id: Long) {
        _loading.value = true
        viewModelScope.launch {
            runCatching { TransService.delete(getApplication(), id) }
                .onSuccess { _deleted.value = it }
                .onFailure { _error.value = "No se pudo eliminar" }
            _loading.value = false
        }
    }

    fun clearTransientEvents() {
        _deleted.value = null
    }

    fun export() {
        _loading.value = true
        viewModelScope.launch {
            runCatching { TransService.exportToTxt(getApplication()) }
                .onSuccess { _exportPath.value = it }
                .onFailure { _error.value = "No se pudo exportar" }
            _loading.value = false
        }
    }

    private fun TransaccionFiltro?.cacheKey(): String {
        if (this == null) return ""
        return listOf(
            fechaInicio?.toString().orEmpty(),
            fechaFin?.toString().orEmpty(),
            categoriaId?.toString().orEmpty(),
            orden?.name.orEmpty(),
            tipo?.name.orEmpty(),
            accountType.orEmpty(),
            montoMin?.toString().orEmpty(),
            montoMax?.toString().orEmpty(),
            texto.orEmpty(),
            isAscendente.toString()
        ).joinToString("|")
    }

    fun clearCacheIfUserChanged() {
        val currentUserId = Prefs.getCurrentUserId(getApplication())
        if (loadedUserId > 0 && loadedUserId != currentUserId) {
            clearCache()
        }
    }

    fun clearCache() {
        loadedUserId = -1L
        loadedYear = 0
        loadedMonth = 0
        loadedVersion = -1L
        loadedFilterKey = ""
        _items.value = emptyList()
        _loading.value = false
    }
}
