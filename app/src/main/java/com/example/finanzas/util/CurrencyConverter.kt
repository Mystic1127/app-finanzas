package com.example.finanzas.util

import java.util.Locale

object CurrencyConverter {
    private val penPerUnit = mapOf(
        "PEN" to 1.0,
        "USD" to 3.5075,
        "EUR" to 4.113404479887416,
        "CLP" to 0.003897222222222222
    )

    @JvmStatic
    fun normalize(code: String?): String {
        val safe = code?.trim()?.uppercase(Locale.ROOT).orEmpty()
        return if (safe == "PEN" || safe == "USD" || safe == "EUR" || safe == "CLP") safe else "PEN"
    }

    @JvmStatic
    fun symbol(code: String?): String {
        return when (normalize(code)) {
            "USD" -> "$"
            "EUR" -> "€"
            "CLP" -> "CLP$"
            else -> "S/"
        }
    }

    @JvmStatic
    fun supportedCurrencies(): List<String> = listOf("PEN", "USD", "EUR", "CLP")

    @JvmStatic
    fun convert(amount: Double, from: String?, to: String?, manualCurrency: String? = null, manualPenRate: Double = 0.0): Double {
        val source = normalize(from)
        val target = normalize(to)
        if (source == target) return amount

        val amountInPen = amount * penRate(source, manualCurrency, manualPenRate)
        return amountInPen / penRate(target, manualCurrency, manualPenRate)
    }

    private fun penRate(currency: String, manualCurrency: String?, manualPenRate: Double): Double {
        val normalizedManualCurrency = normalize(manualCurrency)
        if (manualPenRate > 0.0 && currency == normalizedManualCurrency && currency != "PEN") {
            return manualPenRate
        }
        return penPerUnit[currency] ?: 1.0
    }
}
