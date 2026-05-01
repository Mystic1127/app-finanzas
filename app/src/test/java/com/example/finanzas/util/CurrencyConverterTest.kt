package com.example.finanzas.util

import org.junit.Assert.assertEquals
import org.junit.Test

class CurrencyConverterTest {
    @Test
    fun defaultRatesConvertUsdToSupportedBaseCurrencies() {
        assertEquals(17_537.50, CurrencyConverter.convert(5_000.0, "USD", "PEN"), 0.01)
        assertEquals(4_263.50, CurrencyConverter.convert(5_000.0, "USD", "EUR"), 0.01)
        assertEquals(4_500_000.0, CurrencyConverter.convert(5_000.0, "USD", "CLP"), 0.01)
    }

    @Test
    fun transactionAmountsCanBeConvertedIntoPenBase() {
        assertEquals(35.075, CurrencyConverter.convert(10.0, "USD", "PEN"), 0.0001)
        assertEquals(3.5075, CurrencyConverter.convert(1.0, "USD", "PEN"), 0.0001)
    }

    @Test
    fun manualPenRateOverridesOnlyTheConfiguredForeignCurrency() {
        assertEquals(20.0, CurrencyConverter.convert(10.0, "USD", "PEN", "USD", 2.0), 0.0001)
        assertEquals(10.0, CurrencyConverter.convert(20.0, "PEN", "USD", "USD", 2.0), 0.0001)
    }
}
