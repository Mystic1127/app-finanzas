package com.example.finanzas.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.finanzas.R
import com.example.finanzas.data.api.GoalService
import com.example.finanzas.data.model.SavingsGoal
import kotlinx.coroutines.launch
import org.json.JSONObject

class GoalsViewModel(application: Application) : AndroidViewModel(application) {
    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _goals = MutableLiveData<List<SavingsGoal>>(emptyList())
    val goals: LiveData<List<SavingsGoal>> = _goals

    private val _message = MutableLiveData<Int>()
    val message: LiveData<Int> = _message

    fun loadGoals() {
        _loading.value = true
        viewModelScope.launch {
            runCatching { GoalService.list(getApplication()) }
                .onSuccess { _goals.value = it }
                .onFailure { _message.value = R.string.error_cargar_metas }
            _loading.value = false
        }
    }

    fun saveGoal(body: JSONObject) = action { GoalService.save(getApplication(), body) }
    fun deleteGoal(id: Int) = action { GoalService.delete(getApplication(), id) }
    fun saveMilestone(body: JSONObject) = action { GoalService.saveMilestone(getApplication(), body) > 0 }
    fun deleteMilestone(id: Int) = action { GoalService.deleteMilestone(getApplication(), id) }

    private fun action(block: suspend () -> Boolean) {
        viewModelScope.launch {
            runCatching { block() }
                .onSuccess { ok ->
                    if (ok) loadGoals() else _message.value = R.string.error_guardar_meta
                }
                .onFailure { _message.value = R.string.error_guardar_meta }
        }
    }
}
