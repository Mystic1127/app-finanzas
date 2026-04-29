package com.example.finanzas.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.finanzas.R
import com.example.finanzas.data.api.ReminderService
import com.example.finanzas.data.model.PaymentReminder
import kotlinx.coroutines.launch
import org.json.JSONObject

class RemindersViewModel(application: Application) : AndroidViewModel(application) {
    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _reminders = MutableLiveData<List<PaymentReminder>>(emptyList())
    val reminders: LiveData<List<PaymentReminder>> = _reminders

    private val _message = MutableLiveData<Int>()
    val message: LiveData<Int> = _message

    fun loadReminders(includePaid: Boolean = false) {
        _loading.value = true
        viewModelScope.launch {
            runCatching { ReminderService.list(getApplication(), includePaid) }
                .onSuccess { _reminders.value = it }
                .onFailure { _message.value = R.string.error_cargar_recordatorios }
            _loading.value = false
        }
    }

    fun saveReminder(body: JSONObject) {
        viewModelScope.launch {
            runCatching { ReminderService.save(getApplication(), body) }
                .onSuccess {
                    if (it != null) loadReminders(false) else _message.value = R.string.error_guardar_recordatorio
                }
                .onFailure { _message.value = R.string.error_guardar_recordatorio }
        }
    }

    fun markPaid(id: Int, paid: Boolean) {
        viewModelScope.launch {
            runCatching { ReminderService.marcarPagado(getApplication(), id, paid) }
                .onSuccess { ok -> if (ok) loadReminders(false) else _message.value = R.string.error_guardar_recordatorio }
                .onFailure { _message.value = R.string.error_guardar_recordatorio }
        }
    }

    fun delete(id: Int) {
        viewModelScope.launch {
            runCatching { ReminderService.delete(getApplication(), id) }
                .onSuccess { ok -> if (ok) loadReminders(false) else _message.value = R.string.error_guardar_recordatorio }
                .onFailure { _message.value = R.string.error_guardar_recordatorio }
        }
    }
}
