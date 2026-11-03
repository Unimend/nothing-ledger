package com.unimend.nothingledger.timeline

import com.unimend.nothingledger.model.AccountId
import com.unimend.nothingledger.model.CategoryId
import com.unimend.nothingledger.model.Money
import com.unimend.nothingledger.model.Transaction
import com.unimend.nothingledger.model.TransactionId
import com.unimend.nothingledger.model.TransactionType
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.util.Currency
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TimelineStatisticsTest {
    private val zone = ZoneId.of("Asia/Shanghai")
    private val month = YearMonth.of(2026, 11)
    private val food = id("00000000-0000-0000-0000-000000000010")
    private val travel = id("00000000-0000-0000-0000-000000000011")

    @Test
    fun `currencies are isolated and never added together`() {
        val result = buildTimelineStatistics(
            month,
            listOf(
                tx(TransactionType.INCOME, 10_000, "CNY", "2026-11-01T00:00:00Z", food),
                tx(TransactionType.EXPENSE, 2_500, "CNY", "2026-11-01T01:00:00Z", food),
                tx(TransactionType.INCOME, 999, "USD", "2026-11-01T02:00:00Z", food),
            ),
            zone,
        )

        assertEquals(listOf("CNY", "USD"), result.currencies.map(CurrencyMonthStatistics::currencyCode))
        assertEquals(MonthOverview(10_000, 2_500), result.currencies[0].overview)
        assertEquals(MonthOverview(999, 0), result.currencies[1].overview)
    }

    @Test
    fun `daily trend uses local dates and includes zero days`() {
        val result = buildTimelineStatistics(
            month,
            listOf(tx(TransactionType.EXPENSE, 88, "CNY", "2026-10-31T16:30:00Z", food)),
            zone,
        ).currencies.single()

        assertEquals(30, result.dailyTrend.size)
        assertEquals(88, result.dailyTrend.first().expenseMinor)
        assertTrue(result.dailyTrend.drop(1).all { it.incomeMinor == 0L && it.expenseMinor == 0L })
    }

    @Test
    fun `expense category shares are stable exact basis point projections`() {
        val result = buildTimelineStatistics(
            month,
            listOf(
                tx(TransactionType.EXPENSE, 3_000, "CNY", "2026-11-02T00:00:00Z", food),
                tx(TransactionType.EXPENSE, 1_000, "CNY", "2026-11-02T01:00:00Z", travel),
            ),
            zone,
        ).currencies.single()

        assertEquals(
            listOf(
                CategoryExpenseShare(food, 3_000, 7_500),
                CategoryExpenseShare(travel, 1_000, 2_500),
            ),
            result.expenseByCategory,
        )
    }

    @Test
    fun `transfers and transactions outside month do not affect statistics`() {
        val transactions = listOf(
            tx(TransactionType.TRANSFER, 5_000, "CNY", "2026-11-03T00:00:00Z", null),
            tx(TransactionType.EXPENSE, 100, "CNY", "2026-12-01T00:00:00Z", food),
        )
        assertTrue(buildTimelineStatistics(month, transactions, zone).currencies.isEmpty())
    }

    @Test
    fun `timeline failure and empty map without inventing data`() {
        val query = TimelineQuery(month)
        assertInstanceOf(
            TimelineStatisticsState.Failure::class.java,
            TimelineState.Failure(query, "offline").toStatistics(zone),
        )
        val empty = TimelineState.Empty(query, MonthSummary(emptyList())).toStatistics(zone)
            as TimelineStatisticsState.Ready
        assertTrue(empty.value.currencies.isEmpty())
    }

    private fun id(value: String): CategoryId = CategoryId(UUID.fromString(value))

    private fun tx(
        type: TransactionType,
        amount: Long,
        currency: String,
        occurredAt: String,
        categoryId: CategoryId?,
    ): Transaction {
        val source = AccountId(UUID.fromString("00000000-0000-0000-0000-000000000020"))
        val destination = AccountId(UUID.fromString("00000000-0000-0000-0000-000000000021"))
        val time = Instant.parse(occurredAt)
        return Transaction(
            TransactionId(UUID.randomUUID()), type, Money(amount, Currency.getInstance(currency)), time,
            if (type == TransactionType.INCOME) null else source,
            if (type == TransactionType.EXPENSE) null else destination,
            if (type == TransactionType.TRANSFER) null else requireNotNull(categoryId),
            null, time, time,
        )
    }
}
