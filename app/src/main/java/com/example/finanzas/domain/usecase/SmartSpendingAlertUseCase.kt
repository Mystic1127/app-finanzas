package com.example.finanzas.domain.usecase

import com.example.finanzas.data.model.Transaccion
import java.util.Locale

data class SmartSpendingAlert(val message: String)

class SmartSpendingAlertUseCase {
    fun execute(currentMonth: List<Transaccion>, previousMonth: List<Transaccion>, thresholdPercent: Double = 30.0): SmartSpendingAlert? {
        val current = currentMonth.filter { !it.isEsIngreso }.groupBy { it.categoriaNombre ?: "Sin categoría" }.mapValues { it.value.sumOf { t -> t.monto } }
        val previous = previousMonth.filter { !it.isEsIngreso }.groupBy { it.categoriaNombre ?: "Sin categoría" }.mapValues { it.value.sumOf { t -> t.monto } }

        var topCategory: String? = null
        var topGrowth = 0.0
        current.forEach { (cat, currAmount) ->
            val prevAmount = previous[cat] ?: 0.0
            if (prevAmount > 0.0) {
                val growth = ((currAmount - prevAmount) / prevAmount) * 100.0
                if (growth > thresholdPercent && growth > topGrowth) {
                    topGrowth = growth
                    topCategory = cat
                }
            }
        }

        return topCategory?.let {
            SmartSpendingAlert("Estás gastando ${String.format(Locale.US, "%.0f", topGrowth)}% más en $it que el mes pasado")
        }
    }
}
