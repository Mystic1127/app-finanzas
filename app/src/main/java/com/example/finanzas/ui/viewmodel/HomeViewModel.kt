package com.example.finanzas.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.finanzas.data.api.DashboardService
import com.example.finanzas.data.model.HomeSummary
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _summary = MutableLiveData<HomeSummary>()
    val summary: LiveData<HomeSummary> = _summary

    private val _error = MutableLiveData<Unit>()
    val error: LiveData<Unit> = _error

    fun loadSummary(anio: Int, mes: Int) {
        _loading.value = true
        viewModelScope.launch {
            runCatching { DashboardService.getSummary(getApplication(), anio, mes) }
                .onSuccess { _summary.value = it }
                .onFailure { _error.value = Unit }
            _loading.value = false
        }
    }
}
