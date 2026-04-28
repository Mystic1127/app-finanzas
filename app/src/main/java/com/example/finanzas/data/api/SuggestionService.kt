package com.example.finanzas.data.api

import android.content.Context
import com.example.finanzas.data.local.LocalRepository
import com.example.finanzas.data.model.CategorySuggestion
import java.util.Calendar
import kotlin.math.abs

object SuggestionService {

    interface Callback {
        fun onSuccess(suggestion: CategorySuggestion)
        fun onError()
    }

    @JvmStatic
    fun suggest(ctx: Context, nota: String?, esIngreso: Boolean, monto: Double, cb: Callback) {
        DbCoroutine.io(
            block = {
                val now = Calendar.getInstance()
                val trans = LocalRepository.getInstance(ctx)
                    .listTransacciones(now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1)

                val counts = mutableMapOf<Int, Int>()
                trans.forEach { t ->
                    if (t.isEsIngreso != esIngreso) return@forEach

                    if (!nota.isNullOrEmpty() && !t.nota.isNullOrEmpty() &&
                        t.nota.lowercase().contains(nota.lowercase())
                    ) {
                        counts[t.categoriaId] = (counts[t.categoriaId] ?: 0) + 2
                    }

                    if (monto > 0 && abs(t.monto - monto) < 1) {
                        counts[t.categoriaId] = (counts[t.categoriaId] ?: 0) + 1
                    }
                }

                val bestCat = counts.maxByOrNull { it.value }?.key ?: -1
                if (bestCat > 0) {
                    CategorySuggestion().apply {
                        categoriaId = bestCat
                        categoriaNombre = null
                        confidence = 0.8
                    }
                } else {
                    CategorySuggestion()
                }
            },
            onSuccess = cb::onSuccess,
            onError = cb::onError
        )
    }
}
