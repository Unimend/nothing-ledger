package com.unimend.nothingledger.timeline

import com.unimend.nothingledger.model.AccountId
import com.unimend.nothingledger.model.CategoryId
import com.unimend.nothingledger.model.Money
import com.unimend.nothingledger.model.Transaction
import com.unimend.nothingledger.model.TransactionId
import com.unimend.nothingledger.model.TransactionType
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Currency
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ObserveCalendarTest {
    private val zone = ZoneId.of("Asia/Shanghai")
    private val account = AccountId(UUID.randomUUID())
    private val otherAccount = AccountId(UUID.randomUUID())
    private val category = CategoryId(UUID.randomUUID())

    @Test fun `calendar uses local date and summarizes income expense per day`() {
        val expense = transaction(TransactionType.EXPENSE, "2026-10-31T16:30:00Z", 1200, "CNY")
        val income = transaction(TransactionType.INCOME, "2026-11-01T03:00:00Z", 5000, "CNY")

        val calendar = buildCalendarMonth(
            CalendarQuery(YearMonth.of(2026, 11)),
            listOf(expense, income),
            zone,
        )
        val first = calendar.day(1)

        assertEquals(2, first.transactionCount)
        assertEquals(CalendarDayTotal("CNY", incomeMinor = 5000, expenseMinor = 1200), first.totals.single())
    }

    @Test fun `different currencies remain separate and transfers do not affect totals`() {
        val cny = transaction(TransactionType.EXPENSE, "2026-11-02T02:00:00Z", 800, "CNY")
        val usd = transaction(TransactionType.INCOME, "2026-11-02T03:00:00Z", 300, "USD")
        val transfer = transaction(TransactionType.TRANSFER, "2026-11-02T04:00:00Z", 9000, "CNY")

        val day = buildCalendarMonth(
            CalendarQuery(YearMonth.of(2026, 11)),
            listOf(cny, usd, transfer),
            zone,
        ).day(2)

        assertEquals(
            listOf(
                CalendarDayTotal("CNY", incomeMinor = 0, expenseMinor = 800),
                CalendarDayTotal("USD", incomeMinor = 300, expenseMinor = 0),
            ),
            day.totals,
        )
        assertEquals(3, day.transactionCount)
        assertTrue(day.hasTransfer)
    }

    @Test fun `selected day contains only its transactions in newest first order`() {
        val older = transaction(TransactionType.EXPENSE, "2026-11-03T01:00:00Z", 100, "CNY")
        val newer = transaction(TransactionType.EXPENSE, "2026-11-03T08:00:00Z", 200, "CNY")
        val otherDay = transaction(TransactionType.EXPENSE, "2026-11-04T01:00:00Z", 300, "CNY")

        val calendar = buildCalendarMonth(
            CalendarQuery(YearMonth.of(2026, 11), LocalDate.of(2026, 11, 3)),
            listOf(older, otherDay, newer),
            zone,
        )

        assertEquals(listOf(newer.id, older.id), calendar.selectedTransactions.map(Transaction::id))
    }

    @Test fun `weeks start Monday and contain complete rows`() {
        val calendar = buildCalendarMonth(CalendarQuery(YearMonth.of(2026, 11)), emptyList(), zone)

        assertTrue(calendar.weeks.all { it.size == 7 })
        assertEquals(6, calendar.weeks.first().indexOfFirst { it?.date?.dayOfMonth == 1 })
        assertEquals(30, calendar.weeks.flatten().filterNotNull().size)
        assertFalse(calendar.day(30).hasTransfer)
    }

    private fun CalendarMonth.day(day: Int): CalendarDay =
        weeks.flatten().filterNotNull().single { it.date.dayOfMonth == day }

    private fun transaction(
        type: TransactionType,
        occurredAt: String,
        amount: Long,
        currencyCode: String,
    ): Transaction {
        val instant = Instant.parse(occurredAt)
        return Transaction(
            id = TransactionId(UUID.randomUUID()),
            type = type,
            money = Money(amount, Currency.getInstance(currencyCode)),
            occurredAt = instant,
            sourceAccountId = if (type == TransactionType.INCOME) null else account,
            destinationAccountId = when (type) {
                TransactionType.EXPENSE -> null
                TransactionType.INCOME -> account
                TransactionType.TRANSFER -> otherAccount
            },
            categoryId = if (type == TransactionType.TRANSFER) null else category,
            note = null,
            createdAt = instant,
            updatedAt = instant,
        )
    }
}
