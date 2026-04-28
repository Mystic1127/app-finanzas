package com.example.finanzas.data.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object DbCoroutine {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    fun <T> io(
        block: () -> T,
        onSuccess: (T) -> Unit,
        onError: (() -> Unit)? = null
    ) {
        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) { block() }
                onSuccess(result)
            } catch (_: Throwable) {
                onError?.invoke()
            }
        }
    }
}
