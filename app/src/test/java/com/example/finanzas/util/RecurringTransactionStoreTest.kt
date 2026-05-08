package com.example.finanzas.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class RecurringTransactionStoreTest {
    @Test
    fun weeklyFrequencyMatchesOriginalWeekdayOnly() {
        val first = date(2026, Calendar.MAY, 6)

        assertTrue(
            RecurringTransactionStore.matchesFrequencyOnDate(
                RecurringTransactionStore.FREQUENCY_WEEKLY,
                first,
                0,
                date(2026, Calendar.MAY, 13)
            )
        )
        assertFalse(
            RecurringTransactionStore.matchesFrequencyOnDate(
                RecurringTransactionStore.FREQUENCY_WEEKLY,
                first,
                0,
                date(2026, Calendar.MAY, 14)
            )
        )
    }

    @Test
    fun monthlyFrequencyUsesLastDayWhenMonthIsShorter() {
        val first = date(2026, Calendar.JANUARY, 31)

        assertTrue(
            RecurringTransactionStore.matchesFrequencyOnDate(
                RecurringTransactionStore.FREQUENCY_MONTHLY,
                first,
                0,
                date(2026, Calendar.FEBRUARY, 28)
            )
        )
        assertTrue(
            RecurringTransactionStore.matchesFrequencyOnDate(
                RecurringTransactionStore.FREQUENCY_MONTHLY,
                first,
                0,
                date(2026, Calendar.MARCH, 31)
            )
        )
        assertFalse(
            RecurringTransactionStore.matchesFrequencyOnDate(
                RecurringTransactionStore.FREQUENCY_MONTHLY,
                first,
                0,
                date(2026, Calendar.MARCH, 30)
            )
        )
    }

    private fun date(year: Int, month: Int, day: Int): Long {
        return Calendar.getInstance().apply {
            clear()
            set(year, month, day, 9, 30, 0)
        }.timeInMillis
    }
}
