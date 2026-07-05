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

    @Test
    fun negativeSourceTransactionIdMeansStandaloneRule() {
        assertTrue(RecurringTransactionStore.isStandaloneRuleSourceId(-42))
        assertFalse(RecurringTransactionStore.isStandaloneRuleSourceId(42))
        assertTrue(RecurringTransactionStore.isRealTransactionSourceId(42))
        assertFalse(RecurringTransactionStore.isRealTransactionSourceId(-42))
    }

    @Test
    fun customFrequencyMatchesOnlySelectedWeekdays() {
        val first = date(2026, Calendar.MAY, 4)
        val mask = RecurringTransactionStore.bitForCalendarDay(Calendar.MONDAY) or
            RecurringTransactionStore.bitForCalendarDay(Calendar.WEDNESDAY) or
            RecurringTransactionStore.bitForCalendarDay(Calendar.FRIDAY)

        assertTrue(
            RecurringTransactionStore.matchesFrequencyOnDate(
                RecurringTransactionStore.FREQUENCY_CUSTOM,
                first,
                mask,
                date(2026, Calendar.MAY, 6)
            )
        )
        assertFalse(
            RecurringTransactionStore.matchesFrequencyOnDate(
                RecurringTransactionStore.FREQUENCY_CUSTOM,
                first,
                mask,
                date(2026, Calendar.MAY, 7)
            )
        )
    }

    @Test
    fun customFrequencyWithoutSelectedDaysDoesNotMatch() {
        val first = date(2026, Calendar.MAY, 4)

        assertFalse(
            RecurringTransactionStore.matchesFrequencyOnDate(
                RecurringTransactionStore.FREQUENCY_CUSTOM,
                first,
                0,
                date(2026, Calendar.MAY, 4)
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
