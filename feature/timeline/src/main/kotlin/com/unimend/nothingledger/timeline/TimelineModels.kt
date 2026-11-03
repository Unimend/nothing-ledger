package com.unimend.nothingledger.timeline

import com.unimend.nothingledger.model.AccountId
import com.unimend.nothingledger.model.CategoryId
import com.unimend.nothingledger.model.Transaction
import java.time.LocalDate
import java.time.YearMonth

public data class TimelineQuery(
    public val month: YearMonth,
    public val text: String = "",
    public val accountId: AccountId? = null,
    public val categoryId: CategoryId? = null,
) {
    public val normalizedText: String get() = text.trim()
}

public data class CurrencySubtotal(
    public val currencyCode: String,
    public val incomeMinor: Long,
    public val expenseMinor: Long,
) {
    public val netMinor: Long get() = Math.subtractExact(incomeMinor, expenseMinor)
}

public data class MonthSummary(
    public val totals: List<CurrencySubtotal>,
)

public data class TimelineDay(
    public val date: LocalDate,
    public val transactions: List<Transaction>,
)

public sealed interface TimelineState {
    public data class Loading(public val query: TimelineQuery) : TimelineState
    public data class Empty(
        public val query: TimelineQuery,
        public val summary: MonthSummary,
    ) : TimelineState
    public data class Content(
        public val query: TimelineQuery,
        public val days: List<TimelineDay>,
        public val summary: MonthSummary,
    ) : TimelineState
    public data class Failure(
        public val query: TimelineQuery,
        public val message: String,
    ) : TimelineState
}

