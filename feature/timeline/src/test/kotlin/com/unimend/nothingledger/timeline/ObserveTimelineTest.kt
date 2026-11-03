package com.unimend.nothingledger.timeline

import com.unimend.nothingledger.domain.TransactionRepository
import com.unimend.nothingledger.model.AccountId
import com.unimend.nothingledger.model.CategoryId
import com.unimend.nothingledger.model.Money
import com.unimend.nothingledger.model.Transaction
import com.unimend.nothingledger.model.TransactionId
import com.unimend.nothingledger.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.util.Currency
import java.util.UUID

class ObserveTimelineTest {
    private val zone = ZoneId.of("Asia/Shanghai")
    private val cny = Currency.getInstance("CNY")
    private val accountA = AccountId(UUID.randomUUID())
    private val accountB = AccountId(UUID.randomUUID())
    private val expenseCategory = CategoryId(UUID.randomUUID())
    private val incomeCategory = CategoryId(UUID.randomUUID())

    @Test fun `local day grouping respects user zone and uses stable newest order`() {
        val laterCreated = expense("2026-10-31T16:30:00Z", 200, "B", createdAt = "2026-10-31T17:00:00Z")
        val earlierCreated = expense("2026-10-31T16:30:00Z", 100, "A", createdAt = "2026-10-31T16:40:00Z")
        val priorDay = expense("2026-10-31T15:30:00Z", 50, "C")
        val state = buildState(
            TimelineQuery(YearMonth.of(2026, 11)),
            listOf(earlierCreated, priorDay, laterCreated),
            zone,
        ) as TimelineState.Content
        assertEquals(listOf("B", "A"), state.days[0].transactions.map(Transaction::note))
        assertEquals(1, state.days.size)
    }

    @Test fun `monthly subtotal excludes transfer and keeps income expense separate`() {
        val transactions = listOf(
            expense("2026-11-02T00:00:00Z", 1200, "meal"),
            income("2026-11-03T00:00:00Z", 5000, "salary"),
            transfer("2026-11-04T00:00:00Z", 3000),
        )
        val state = buildState(TimelineQuery(YearMonth.of(2026, 11)), transactions, zone) as TimelineState.Content
        assertEquals(CurrencySubtotal("CNY", 5000, 1200), state.summary.totals.single())
        assertEquals(3800, state.summary.totals.single().netMinor)
    }

    @Test fun `query filters text account and category together`() {
        val wanted = expense("2026-11-02T00:00:00Z", 100, "Coffee beans")
        val other = income("2026-11-03T00:00:00Z", 200, "Coffee refund")
        val query = TimelineQuery(
            month = YearMonth.of(2026, 11),
            text = " coffee ",
            accountId = accountA,
            categoryId = expenseCategory,
        )
        val state = buildState(query, listOf(other, wanted), zone) as TimelineState.Content
        assertEquals(listOf(wanted.id), state.days.single().transactions.map(Transaction::id))
    }

    @Test fun `observer emits loading before empty result`() = runBlocking {
        val states = ObserveTimeline(FakeRepository(emptyList()), zone)(
            TimelineQuery(YearMonth.of(2026, 11)),
        ).toList()
        assertInstanceOf(TimelineState.Loading::class.java, states.first())
        val empty = assertInstanceOf(TimelineState.Empty::class.java, states.last())
        assertTrue(empty.summary.totals.isEmpty())
    }

    private fun expense(occurred: String, amount: Long, note: String, createdAt: String = occurred) = Transaction(
        TransactionId(UUID.randomUUID()), TransactionType.EXPENSE, Money(amount, cny), Instant.parse(occurred),
        accountA, null, expenseCategory, note, Instant.parse(createdAt), Instant.parse(createdAt),
    )

    private fun income(occurred: String, amount: Long, note: String) = Transaction(
        TransactionId(UUID.randomUUID()), TransactionType.INCOME, Money(amount, cny), Instant.parse(occurred),
        null, accountB, incomeCategory, note, Instant.parse(occurred), Instant.parse(occurred),
    )

    private fun transfer(occurred: String, amount: Long) = Transaction(
        TransactionId(UUID.randomUUID()), TransactionType.TRANSFER, Money(amount, cny), Instant.parse(occurred),
        accountA, accountB, null, null, Instant.parse(occurred), Instant.parse(occurred),
    )

    private class FakeRepository(private val values: List<Transaction>) : TransactionRepository {
        override suspend fun save(transaction: Transaction) = Unit
        override suspend fun findById(id: TransactionId): Transaction? = values.find { it.id == id }
        override suspend fun softDelete(id: TransactionId) = Unit
        override fun observeActive(): Flow<List<Transaction>> = flowOf(values)
    }
}

