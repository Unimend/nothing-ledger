package com.unimend.nothingledger.timeline

import com.unimend.nothingledger.model.Transaction
import java.time.LocalDate
import java.time.YearMonth

public data class CalendarQuery(
    public val month: YearMonth,
    public val selectedDate: LocalDate? = null,
) {
    init {
        require(selectedDate == null || YearMonth.from(selectedDate) == month) {
            "Selected date must belong to the visible month"
        }
    }
}

public data class CalendarDayTotal(
    public val currencyCode: String,
    public val incomeMinor: Long,
    public val expenseMinor: Long,
)

public data class CalendarDay(
    public val date: LocalDate,
    public val totals: List<CalendarDayTotal>,
    public val transactionCount: Int,
    public val hasTransfer: Boolean,
)

public data class CalendarMonth(
    public val query: CalendarQuery,
    /** Complete Monday-first weeks; null cells are padding outside the visible month. */
    public val weeks: List<List<CalendarDay?>>,
    public val selectedTransactions: List<Transaction>,
)

public sealed interface CalendarState {
    public data class Loading(public val query: CalendarQuery) : CalendarState
    public data class Ready(public val month: CalendarMonth) : CalendarState
    public data class Failure(public val query: CalendarQuery, public val message: String) : CalendarState
}
