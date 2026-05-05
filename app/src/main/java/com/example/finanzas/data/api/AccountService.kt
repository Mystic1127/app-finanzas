package com.example.finanzas.data.api

import android.content.Context
import com.example.finanzas.data.local.LocalRepository
import com.example.finanzas.data.model.FinancialAccount
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object AccountService {
    interface CreateCb {
        fun onOk(account: FinancialAccount)
        fun onError(message: String?)
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @JvmStatic
    fun create(ctx: Context, name: String, initialBalance: Double, currency: String, cb: CreateCb) {
        val appContext = ctx.applicationContext
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    LocalRepository.getInstance(appContext).createFinancialAccount(name, initialBalance, currency)
                }
            }.onSuccess { cb.onOk(it) }
                .onFailure { cb.onError(it.message) }
        }
    }
}
