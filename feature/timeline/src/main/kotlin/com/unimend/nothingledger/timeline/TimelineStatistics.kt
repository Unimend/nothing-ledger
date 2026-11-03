package com.unimend.nothingledger.timeline

import com.unimend.nothingledger.model.CategoryId
import com.unimend.nothingledger.model.Transaction
import com.unimend.nothingledger.model.TransactionType
import java.math.BigInteger
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

public data class MonthOverview(
    public val incomeMinor: Long,
    public val expenseMinor: Long,
) {
    public val netMinor: Long get() = Math.subtractExact(incomeMinor, expenseMinor)
}

public data class DailyCashFlow(
    public val date: LocalDate,
    public val incomeMinor: Long,
    public val expenseMinor: Long,
) {
    public val netMinor: Long get() = Math.subtractExact(incomeMinor, expenseMinor)
}

public data class CategoryExpenseShare(
    public val categoryId: CategoryId,
    public val expenseMinor: Long,
    /** 10_000 means 100%; integer basis points avoid floating-point drift. */
    public val shareBasisPoints: Int,
)

public data class CurrencyMonthStatistics(
    public val currencyCode: String,
    public val overview: MonthOverview,
    /** Contains every date in the visible month, including zero-value days. */
    public val dailyTrend: List<DailyCashFlow>,
    public val expenseByCategory: List<CategoryExpenseShare>,
)

public data class MonthStatistics(
    public val month: YearMonth,
    /** Each currency is an isolated bucket and is never converted or combined. */
    public val currencies: List<CurrencyMonthStatistics>,
)

public sealed interface TimelineStatisticsState {
    public data class Loading(public val month: YearMonth) : TimelineStatisticsState
    public data class Ready(public val value: MonthStatistics) : TimelineStatisticsState
    public data class Failure(public val month: YearMonth, public val message: String) : TimelineStatisticsState
}

public fun buildTimelineStatistics(
    month: YearMonth,
    transactions: Iterable<Transaction>,
    zoneId: ZoneId,
): MonthStatistics {
    val relevant = transactions.asSequence()
        .filter { it.type != TransactionType.TRANSFER }
        .filter { YearMonth.from(it.occurredAt.atZone(zoneId)) == month }
        .toList()
    val statistics = relevant
        .groupBy { it.money.currency.currencyCode }
        .toSortedMap()
        .map { (currencyCode, currencyTransactions) ->
            buildCurrencyStatistics(month, currencyCode, currencyTransactions, zoneId)
        }
    return MonthStatistics(month, statistics)
}

public fun TimelineState.toStatistics(zoneId: ZoneId): TimelineStatisticsState = when (this) {
    is TimelineState.Loading -> TimelineStatisticsState.Loading(query.month)
    is TimelineState.Failure -> TimelineStatisticsState.Failure(query.month, message)
    is TimelineState.Empty -> TimelineStatisticsState.Ready(MonthStatistics(query.month, emptyList()))
    is TimelineState.Content -> TimelineStatisticsState.Ready(
        buildTimelineStatistics(query.month, days.flatMap(TimelineDay::transactions), zoneId),
    )
}

private fun buildCurrencyStatistics(
    month: YearMonth,
    currencyCode: String,
    transactions: List<Transaction>,
    zoneId: ZoneId,
): CurrencyMonthStatistics {
    var income = 0L
    var expense = 0L
    transactions.forEach { transaction ->
        when (transaction.type) {
            TransactionType.INCOME -> income = Math.addExact(income, transaction.money.minorUnits)
            TransactionType.EXPENSE -> expense = Math.addExact(expense, transaction.money.minorUnits)
            TransactionType.TRANSFER -> Unit
        }
    }
    val byDate = transactions.groupBy { it.occurredAt.atZone(zoneId).toLocalDate() }
    val trend = (1..month.lengthOfMonth()).map { day ->
        val date = month.atDay(day)
        var dayIncome = 0L
        var dayExpense = 0L
        byDate[date].orEmpty().forEach { transaction ->
            when (transaction.type) {
                TransactionType.INCOME -> dayIncome = Math.addExact(dayIncome, transaction.money.minorUnits)
                TransactionType.EXPENSE -> dayExpense = Math.addExact(dayExpense, transaction.money.minorUnits)
                TransactionType.TRANSFER -> Unit
            }
        }
        DailyCashFlow(date, dayIncome, dayExpense)
    }
    val categoryAmounts = transactions.asSequence()
        .filter { it.type == TransactionType.EXPENSE }
        .groupBy { requireNotNull(it.categoryId) { "Expense transaction has no category" } }
        .mapValues { (_, values) -> values.fold(0L) { total, item -> Math.addExact(total, item.money.minorUnits) } }
    val shares = categoryAmounts.map { (categoryId, amount) ->
        CategoryExpenseShare(categoryId, amount, shareBasisPoints(amount, expense))
    }.sortedWith(
        compareByDescending<CategoryExpenseShare>(CategoryExpenseShare::expenseMinor)
            .thenBy { it.categoryId.value.toString() },
    )
    return CurrencyMonthStatistics(currencyCode, MonthOverview(income, expense), trend, shares)
}

private fun shareBasisPoints(amount: Long, total: Long): Int {
    if (total == 0L) return 0
    return BigInteger.valueOf(amount)
        .multiply(BigInteger.valueOf(BASIS_POINT_SCALE.toLong()))
        .divide(BigInteger.valueOf(total))
        .intValueExact()
}

private const val BASIS_POINT_SCALE: Int = 10_000
