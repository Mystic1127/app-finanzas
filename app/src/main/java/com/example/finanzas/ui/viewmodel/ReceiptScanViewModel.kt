package com.example.finanzas.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.finanzas.data.api.SettingsService
import com.example.finanzas.data.local.LocalRepository
import com.example.finanzas.data.model.ReceiptDraft
import com.example.finanzas.util.CurrencyConverter
import com.example.finanzas.util.ProFeatureManager
import kotlinx.coroutines.launch
import java.util.Date

class ReceiptScanViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = LocalRepository.getInstance(application)

    private val _isProUser = MutableLiveData(ProFeatureManager.isProUser(application))
    val isProUser: LiveData<Boolean> = _isProUser

    private val _draft = MutableLiveData<ReceiptDraft>()
    val draft: LiveData<ReceiptDraft> = _draft

    private val _saved = MutableLiveData<Boolean>()
    val saved: LiveData<Boolean> = _saved

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun refreshProState() {
        _isProUser.value = ProFeatureManager.isProUser(getApplication())
    }

    fun simulateDetectedReceipt() {
        if (_isProUser.value != true) {
            _error.value = "Disponible en Pro"
            return
        }
        _draft.value = ReceiptDraft().apply {
            monto = 48.90
            fecha = Date()
            comercio = "Compra detectada"
            moneda = CurrencyConverter.normalize(SettingsService.getCurrencyCode(getApplication()))
        }
    }

    fun createTransactionFromReceipt(
        categoriaId: Int,
        monto: Double,
        fecha: Long,
        comercio: String,
        moneda: String
    ) {
        if (_isProUser.value != true) {
            _error.value = "Disponible en Pro"
            return
        }
        if (categoriaId <= 0 || monto <= 0.0 || fecha <= 0L) {
            _error.value = "Completa los datos del recibo"
            return
        }

        viewModelScope.launch {
            runCatching {
                repository.createTransaccion(
                    categoriaId = categoriaId,
                    esIngreso = false,
                    monto = monto,
                    nota = comercio.ifBlank { "Recibo escaneado" },
                    fecha = fecha,
                    moneda = CurrencyConverter.normalize(moneda)
                )
            }
                .onSuccess { _saved.value = true }
                .onFailure {
                    _error.value = if (it is LocalRepository.InsufficientBalanceException) {
                        it.message
                    } else {
                        "No se pudo crear la transacción"
                    }
                }
        }
    }
}
