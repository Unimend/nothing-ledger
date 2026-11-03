package com.unimend.nothingledger.timeline

import com.unimend.nothingledger.domain.TransactionRepository
import com.unimend.nothingledger.model.Transaction
import com.unimend.nothingledger.model.TransactionType
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

public class ObserveCalendar(
    private val repository: TransactionRepository,
    private val zoneId: ZoneId,
) {
    public operator fun invoke(query: CalendarQuery): Flow<CalendarState> =
        repository.observeActive()
            .map { transactions ->
                val state: CalendarState = CalendarState.Ready(buildCalendarMonth(query, transactions, zoneId))
                state
            }
            .onStart { emit(CalendarState.Loading(query)) }
            .catch { error -> emit(CalendarState.Failure(query, error.message ?: "Calendar unavailable")) }
}

public fun buildCalendarMonth(
    query: CalendarQuery,
    transactions: List<Transaction>,
    zoneId: ZoneId,
): CalendarMonth {
    val byDate = transactions.asSequence()
        .filter { query.month == java.time.YearMonth.from(it.occurredAt.atZone(zoneId)) }
        .groupBy { it.occurredAt.atZone(zoneId).toLocalDate() }
    val days = (1..query.month.lengthOfMonth()).map { dayNumber ->
        val date = query.month.atDay(dayNumber)
        val dayTransactions = byDate[date].orEmpty().sortedWith(calendarTransactionOrder)
        CalendarDay(
            date = date,
            totals = summarizeCalendarDay(dayTransactions),
            transactionCount = dayTransactions.size,
            hasTransfer = dayTransactions.any { it.type == TransactionType.TRANSFER },
        )
    }
    val leadingCells = query.month.atDay(1).dayOfWeek.value - 1
    val cells = buildList<CalendarDay?> {
        repeat(leadingCells) { add(null) }
        addAll(days)
        while (size % DAYS_PER_WEEK != 0) add(null)
    }
    val selectedTransactions = query.selectedDate
        ?.let { selected -> byDate[selected].orEmpty().sortedWith(calendarTransactionOrder) }
        .orEmpty()
    return CalendarMonth(query, cells.chunked(DAYS_PER_WEEK), selectedTransactions)
}

private fun summarizeCalendarDay(transactions: List<Transaction>): List<CalendarDayTotal> {
    data class MutableTotal(var income: Long = 0, var expense: Long = 0)
    val totals = sortedMapOf<String, MutableTotal>()
    transactions.forEach { transaction ->
        if (transaction.type == TransactionType.TRANSFER) return@forEach
        val total = totals.getOrPut(transaction.money.currency.currencyCode) { MutableTotal() }
        when (transaction.type) {
            TransactionType.INCOME -> total.income = Math.addExact(total.income, transaction.money.minorUnits)
            TransactionType.EXPENSE -> total.expense = Math.addExact(total.expense, transaction.money.minorUnits)
            TransactionType.TRANSFER -> Unit
        }
    }
    return totals.map { (code, total) -> CalendarDayTotal(code, total.income, total.expense) }
}

private val calendarTransactionOrder: Comparator<Transaction> =
    compareByDescending<Transaction>(Transaction::occurredAt)
        .thenByDescending(Transaction::createdAt)
        .thenBy { it.id.value.toString() }

private const val DAYS_PER_WEEK: Int = 7
