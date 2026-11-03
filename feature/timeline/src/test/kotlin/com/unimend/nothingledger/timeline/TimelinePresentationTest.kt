package com.unimend.nothingledger.timeline

import com.unimend.nothingledger.model.AccountId
import com.unimend.nothingledger.model.CategoryId
import com.unimend.nothingledger.model.Money
import com.unimend.nothingledger.model.Transaction
import com.unimend.nothingledger.model.TransactionId
import com.unimend.nothingledger.model.TransactionType
import java.time.Instant
import java.time.YearMonth
import java.util.Currency
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TimelinePresentationTest {
    @Test
    fun `all page states expose their own query`() {
        val query = TimelineQuery(YearMonth.of(2026, 11))
        val states = listOf<TimelineState>(
            TimelineState.Loading(query),
            TimelineState.Empty(query, MonthSummary(emptyList())),
            TimelineState.Content(query, emptyList(), MonthSummary(emptyList())),
            TimelineState.Failure(query, "offline"),
        )
        assertEquals(List(4) { query }, states.map(TimelineState::query))
    }

    @Test
    fun `transaction amount carries an unambiguous direction`() {
        assertEquals("−¥12.34", transaction(TransactionType.EXPENSE, 1234).timelineAmount())
        assertEquals("+¥12.34", transaction(TransactionType.INCOME, 1234).timelineAmount())
        assertEquals("→¥12.34", transaction(TransactionType.TRANSFER, 1234).timelineAmount())
    }

    @Test
    fun `summary formatter preserves sign and minor units`() {
        assertEquals("+¥0.05", formatSigned(5, "CNY"))
        assertEquals("−¥12.34", formatSigned(-1234, "CNY"))
    }

    private fun transaction(type: TransactionType, minor: Long): Transaction {
        val accountA = AccountId(UUID.randomUUID())
        val accountB = AccountId(UUID.randomUUID())
        val category = CategoryId(UUID.randomUUID())
        val now = Instant.EPOCH
        return Transaction(
            TransactionId(UUID.randomUUID()), type, Money(minor, Currency.getInstance("CNY")), now,
            when (type) { TransactionType.INCOME -> null else -> accountA },
            when (type) { TransactionType.EXPENSE -> null else -> accountB },
            if (type == TransactionType.TRANSFER) null else category,
            null, now, now,
        )
    }
}
