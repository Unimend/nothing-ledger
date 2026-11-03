package com.unimend.nothingledger.timeline

import com.unimend.nothingledger.domain.TransactionRepository
import com.unimend.nothingledger.model.Transaction
import com.unimend.nothingledger.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import java.time.ZoneId

public class ObserveTimeline(
    private val repository: TransactionRepository,
    private val zoneId: ZoneId,
) {
    public operator fun invoke(query: TimelineQuery): Flow<TimelineState> =
        repository.observeActive()
            .map { transactions -> buildState(query, transactions, zoneId) }
            .onStart { emit(TimelineState.Loading(query)) }
            .catch { error ->
                emit(TimelineState.Failure(query, error.message ?: "Timeline unavailable"))
            }
}

public fun buildState(
    query: TimelineQuery,
    transactions: List<Transaction>,
    zoneId: ZoneId,
): TimelineState {
    val filtered = transactions.asSequence()
        .filter { transaction ->
            query.month == query.monthFrom(transaction, zoneId) &&
                query.matchesText(transaction) &&
                query.matchesAccount(transaction) &&
                (query.categoryId == null || transaction.categoryId == query.categoryId)
        }
        .sortedWith(transactionOrder)
        .toList()
    val summary = summarize(filtered)
    if (filtered.isEmpty()) return TimelineState.Empty(query, summary)
    val days = filtered
        .groupBy { it.occurredAt.atZone(zoneId).toLocalDate() }
        .map { (date, items) -> TimelineDay(date, items) }
        .sortedByDescending(TimelineDay::date)
    return TimelineState.Content(query, days, summary)
}

private val transactionOrder: Comparator<Transaction> =
    compareByDescending<Transaction> { it.occurredAt }
        .thenByDescending { it.createdAt }
        .thenBy { it.id.value.toString() }

private fun TimelineQuery.monthFrom(transaction: Transaction, zoneId: ZoneId) =
    java.time.YearMonth.from(transaction.occurredAt.atZone(zoneId))

private fun TimelineQuery.matchesText(transaction: Transaction): Boolean {
    val search = normalizedText
    return search.isEmpty() || transaction.note?.contains(search, ignoreCase = true) == true
}

private fun TimelineQuery.matchesAccount(transaction: Transaction): Boolean =
    accountId == null || transaction.sourceAccountId == accountId || transaction.destinationAccountId == accountId

private data class MutableSubtotal(var income: Long = 0, var expense: Long = 0)

private fun summarize(transactions: List<Transaction>): MonthSummary {
    val values = sortedMapOf<String, MutableSubtotal>()
    transactions.forEach { transaction ->
        if (transaction.type == TransactionType.TRANSFER) return@forEach
        val code = transaction.money.currency.currencyCode
        val subtotal = values.getOrPut(code) { MutableSubtotal() }
        when (transaction.type) {
            TransactionType.INCOME -> subtotal.income = Math.addExact(subtotal.income, transaction.money.minorUnits)
            TransactionType.EXPENSE -> subtotal.expense = Math.addExact(subtotal.expense, transaction.money.minorUnits)
            TransactionType.TRANSFER -> Unit
        }
    }
    return MonthSummary(
        values.map { (code, value) -> CurrencySubtotal(code, value.income, value.expense) },
    )
}

