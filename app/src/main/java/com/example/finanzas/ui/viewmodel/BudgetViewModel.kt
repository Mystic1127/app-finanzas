package com.example.finanzas.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.finanzas.data.api.BudgetService
import com.example.finanzas.data.api.CategoryBudgetService
import com.example.finanzas.data.api.CategoryStore
import com.example.finanzas.data.api.SettingsService
import com.example.finanzas.data.model.Categoria
import com.example.finanzas.data.model.CategoryBudgetInput
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class BudgetViewModel(application: Application) : AndroidViewModel(application) {
    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _budget = MutableLiveData<Double>()
    val budget: LiveData<Double> = _budget

    private val _categoryBudgets = MutableLiveData<List<CategoryBudgetInput>>(emptyList())
    val categoryBudgets: LiveData<List<CategoryBudgetInput>> = _categoryBudgets

    private val _message = MutableLiveData<Int>()
    val message: LiveData<Int> = _message

    fun load(anio: Int, mes: Int) {
        _loading.value = true
        viewModelScope.launch {
            runCatching {
                val presupuestoDeferred = async { BudgetService.get(getApplication(), anio, mes) }
                val categoriasDeferred = async { CategoryStore.load(getApplication()) }
                val guardadosDeferred = async { CategoryBudgetService.list(getApplication(), anio, mes) }

                val presupuesto = presupuestoDeferred.await()
                val categorias = categoriasDeferred.await()
                val guardados = guardadosDeferred.await()

                val out = categorias.filter { !it.esIngreso }.map { cat: Categoria ->
                    CategoryBudgetInput().apply {
                        categoriaId = cat.id
                        categoriaNombre = cat.nombre
                        monto = guardados.firstOrNull { it.categoriaId == cat.id }?.limite ?: 0.0
                        moneda = guardados.firstOrNull { it.categoriaId == cat.id }?.moneda
                            ?: SettingsService.getCurrencyCode(getApplication())
                    }
                }
                Triple(presupuesto, out, Unit)
            }.onSuccess {
                _budget.value = it.first
                _categoryBudgets.value = it.second
            }.onFailure {
                _message.value = com.example.finanzas.R.string.error_cargar_presupuesto
            }
            _loading.value = false
        }
    }

    fun saveBudget(anio: Int, mes: Int, monto: Double) {
        saveBudget(anio, mes, monto, SettingsService.getCurrencyCode(getApplication()))
    }

    fun saveBudget(anio: Int, mes: Int, monto: Double, moneda: String) {
        _loading.value = true
        viewModelScope.launch {
            runCatching { BudgetService.set(getApplication(), anio, mes, monto, moneda) }
                .onSuccess { _message.value = com.example.finanzas.R.string.pres_guardado }
                .onFailure { _message.value = com.example.finanzas.R.string.error_guardar_presupuesto }
            _loading.value = false
        }
    }

    fun saveCategoryBudgets(anio: Int, mes: Int, items: List<CategoryBudgetInput>) {
        _loading.value = true
        viewModelScope.launch {
            runCatching { CategoryBudgetService.save(getApplication(), anio, mes, items) }
                .onSuccess { _message.value = com.example.finanzas.R.string.pres_categorias_guardadas }
                .onFailure { _message.value = com.example.finanzas.R.string.error_guardar_categorias }
            _loading.value = false
        }
    }

    fun createCategory(nombre: String, current: List<CategoryBudgetInput>) {
        _loading.value = true
        viewModelScope.launch {
            runCatching { CategoryStore.create(getApplication(), nombre, false) }
                .onSuccess { nueva ->
                    val next = current.toMutableList()
                    next.add(CategoryBudgetInput().apply {
                    categoriaId = nueva.id
                    categoriaNombre = nueva.nombre
                    monto = 0.0
                    moneda = SettingsService.getCurrencyCode(getApplication())
                })
                    next.sortBy { it.categoriaNombre ?: "" }
                    _categoryBudgets.value = next
                    _message.value = com.example.finanzas.R.string.pres_category_created
                }
                .onFailure { _message.value = com.example.finanzas.R.string.pres_category_create_error }
            _loading.value = false
        }
    }
}
