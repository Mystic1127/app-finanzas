package com.example.finanzas.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.finanzas.data.api.TransService
import com.example.finanzas.data.model.Transaccion
import com.example.finanzas.data.model.TransaccionFiltro
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

    private val _deleted = MutableLiveData<Boolean>()
    val deleted: LiveData<Boolean> = _deleted

    fun load(anio: Int, mes: Int, filtro: TransaccionFiltro?) {
        _loading.value = true
        viewModelScope.launch {
            runCatching { TransService.list(getApplication(), anio, mes, filtro) }
                .onSuccess { _items.value = it }
                .onFailure { _error.value = null }
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

    fun export() {
        _loading.value = true
        viewModelScope.launch {
            runCatching { TransService.exportToTxt(getApplication()) }
                .onSuccess { _exportPath.value = it }
                .onFailure { _error.value = "No se pudo exportar" }
            _loading.value = false
        }
    }
}
