package com.unimend.nothingledger.timeline

import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CalendarDayCellTest {
    private val date = LocalDate.of(2026, 11, 3)

    @Test
    fun `single currency maps income expense transfer selection and today`() {
        val state = CalendarDay(
            date = date,
            totals = listOf(CalendarDayTotal("CNY", 865_000, 12_345)),
            transactionCount = 3,
            hasTransfer = true,
        ).toCellState(selected = true, today = date)

        assertTrue(state.selected)
        assertTrue(state.today)
        assertEquals("8.7K", state.incomeLabel)
        assertEquals("123", state.expenseLabel)
        assertTrue(state.hasTransfer)
        assertFalse(state.hasMultipleCurrencies)
    }

    @Test
    fun `multiple currencies suppress misleading combined amount labels`() {
        val state = CalendarDay(
            date = date,
            totals = listOf(
                CalendarDayTotal("CNY", 10_000, 0),
                CalendarDayTotal("USD", 500, 0),
            ),
            transactionCount = 2,
            hasTransfer = false,
        ).toCellState(selected = false, today = date.minusDays(1))

        assertTrue(state.hasMultipleCurrencies)
        assertNull(state.incomeLabel)
        assertNull(state.expenseLabel)
        assertFalse(state.today)
    }

    @Test
    fun `empty day stays a real day without fake amount labels`() {
        val state = CalendarDay(date, emptyList(), 0, false)
            .toCellState(selected = false, today = date.minusDays(1))

        assertNull(state.incomeLabel)
        assertNull(state.expenseLabel)
        assertFalse(state.hasMultipleCurrencies)
    }

    @Test
    fun `extreme value remains compact and never overflows`() {
        assertEquals("92233720.4B", compactCalendarMinor(Long.MAX_VALUE))
        assertEquals("92233720.4B", compactCalendarMinor(Long.MIN_VALUE))
    }
}
