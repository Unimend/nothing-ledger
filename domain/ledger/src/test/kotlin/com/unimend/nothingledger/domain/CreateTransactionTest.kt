package com.unimend.nothingledger.domain

import com.unimend.nothingledger.model.AccountId
import com.unimend.nothingledger.model.Account
import com.unimend.nothingledger.model.AccountBalance
import com.unimend.nothingledger.model.AccountKind
import com.unimend.nothingledger.model.Category
import com.unimend.nothingledger.model.CategoryType
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking

class CreateTransactionTest {
    private val now = Instant.parse("2026-11-03T06:00:00Z")
    private val repository = MemoryRepository()
    private val accountRepository = MemoryAccountRepository()
    private val categoryRepository = MemoryCategoryRepository()
    private val id = TransactionId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
    private val create = CreateTransaction(
        repository,
        accountRepository,
        categoryRepository,
        Clock.fixed(now, ZoneOffset.UTC),
    ) { id }

    @Test fun `expense is validated normalized and saved once`() = runBlocking {
        val account = AccountId(UUID.randomUUID())
        val category = CategoryId(UUID.randomUUID())
        accountRepository.save(Account(account, "Wallet", AccountKind.PAYMENT, AccountBalance(0, Currency.getInstance("CNY")), 0, null))
        categoryRepository.save(Category(category, null, "Food", CategoryType.EXPENSE, 0, null))
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

    @Test fun `blank note becomes absent`() = runBlocking {
        val account = AccountId(UUID.randomUUID())
        val category = CategoryId(UUID.randomUUID())
        accountRepository.save(Account(account, "Bank", AccountKind.BANK, AccountBalance(0, Currency.getInstance("CNY")), 0, null))
        categoryRepository.save(Category(category, null, "Salary", CategoryType.INCOME, 0, null))
        val result = create(
            TransactionDraft(
                type = TransactionType.INCOME,
                money = Money(1, Currency.getInstance("CNY")),
                occurredAt = now,
                destinationAccountId = account,
                categoryId = category,
                note = "   ",
            ),
        )
        assertNull(result.note)
    }

    @Test fun `transfer to same account is rejected before persistence`() = runBlocking {
        val account = AccountId(UUID.randomUUID())
        accountRepository.save(Account(account, "Wallet", AccountKind.PAYMENT, AccountBalance(0, Currency.getInstance("CNY")), 0, null))
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { create(
                TransactionDraft(
                    type = TransactionType.TRANSFER,
                    money = Money(100, Currency.getInstance("CNY")),
                    occurredAt = now,
                    sourceAccountId = account,
                    destinationAccountId = account,
                ),
            ) }
        }
        assertEquals(0, repository.saved.size)
    }

    private class MemoryRepository : TransactionRepository {
        val saved = mutableListOf<Transaction>()
        override suspend fun save(transaction: Transaction) { saved += transaction }
        override suspend fun findById(id: TransactionId): Transaction? = saved.find { it.id == id }
        override suspend fun softDelete(id: TransactionId) = Unit
        override fun observeActive(): Flow<List<Transaction>> = flowOf(saved)
    }

    private class MemoryAccountRepository : AccountRepository {
        private val values = mutableMapOf<AccountId, Account>()
        override suspend fun findById(id: AccountId): Account? = values[id]
        override suspend fun save(account: Account) { values[account.id] = account }
        override fun observeAll(): Flow<List<Account>> = flowOf(values.values.toList())
    }

    private class MemoryCategoryRepository : CategoryRepository {
        private val values = mutableMapOf<CategoryId, Category>()
        override suspend fun findById(id: CategoryId): Category? = values[id]
        override suspend fun save(category: Category) { values[category.id] = category }
        override fun observeAll(): Flow<List<Category>> = flowOf(values.values.toList())
    }
}

