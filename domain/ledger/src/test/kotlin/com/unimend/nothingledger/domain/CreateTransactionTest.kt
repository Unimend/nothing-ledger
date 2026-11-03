package com.unimend.nothingledger.domain

import com.unimend.nothingledger.model.AccountId
import com.unimend.nothingledger.model.CategoryId
import com.unimend.nothingledger.model.Money
import com.unimend.nothingledger.model.Transaction
import com.unimend.nothingledger.model.TransactionId
import com.unimend.nothingledger.model.TransactionType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Currency
import java.util.UUID

class CreateTransactionTest {
    private val now = Instant.parse("2026-11-03T06:00:00Z")
    private val repository = MemoryRepository()
    private val id = TransactionId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
    private val create = CreateTransaction(repository, Clock.fixed(now, ZoneOffset.UTC)) { id }

    @Test fun `expense is validated normalized and saved once`() {
        val account = AccountId(UUID.randomUUID())
        val category = CategoryId(UUID.randomUUID())
        val result = create(
            TransactionDraft(
                type = TransactionType.EXPENSE,
                money = Money(1280, Currency.getInstance("CNY")),
                occurredAt = now,
                sourceAccountId = account,
                categoryId = category,
                note = "  午餐  ",
            ),
        )
        assertEquals("午餐", result.note)
        assertEquals(result, repository.saved.single())
    }

    @Test fun `blank note becomes absent`() {
        val result = create(
            TransactionDraft(
                type = TransactionType.INCOME,
                money = Money(1, Currency.getInstance("CNY")),
                occurredAt = now,
                destinationAccountId = AccountId(UUID.randomUUID()),
                categoryId = CategoryId(UUID.randomUUID()),
                note = "   ",
            ),
        )
        assertNull(result.note)
    }

    @Test fun `transfer to same account is rejected before persistence`() {
        val account = AccountId(UUID.randomUUID())
        assertThrows(IllegalArgumentException::class.java) {
            create(
                TransactionDraft(
                    type = TransactionType.TRANSFER,
                    money = Money(100, Currency.getInstance("CNY")),
                    occurredAt = now,
                    sourceAccountId = account,
                    destinationAccountId = account,
                ),
            )
        }
        assertEquals(0, repository.saved.size)
    }

    private class MemoryRepository : TransactionRepository {
        val saved = mutableListOf<Transaction>()
        override fun save(transaction: Transaction) { saved += transaction }
        override fun findById(id: TransactionId): Transaction? = saved.find { it.id == id }
        override fun softDelete(id: TransactionId) = Unit
    }
}

