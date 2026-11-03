package com.unimend.nothingledger.accounts

import com.unimend.nothingledger.domain.AccountRepository
import com.unimend.nothingledger.domain.TransactionRepository
import com.unimend.nothingledger.model.*
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Currency
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AccountsFeatureTest {
    private val currency = Currency.getInstance("CNY")
    private val now = Instant.parse("2026-11-03T00:00:00Z")
    private val id = AccountId(UUID.fromString("00000000-0000-0000-0000-000000000001"))

    @Test
    fun `balance is projected and archived accounts remain readable`() = runBlocking {
        val account = account(id, opening = 1_000).archive(now)
        val state = ObserveAccounts(
            FakeAccounts(listOf(account)), FakeTransactions(listOf(expense(account.id, 250))),
        )().first { it is AccountsState.Content } as AccountsState.Content

        assertTrue(state.active.isEmpty())
        assertEquals(750, state.archived.single().currentBalance.minorUnits)
    }

    @Test
    fun `create normalizes name and assigns the next display order`() = runBlocking {
        val accounts = FakeAccounts(listOf(account(id, displayOrder = 4)))
        val manager = ManageAccounts(accounts, Clock.fixed(now, ZoneOffset.UTC)) {
            AccountId(UUID.fromString("00000000-0000-0000-0000-000000000002"))
        }

        val created = manager.create(CreateAccountCommand("  工资卡  ", AccountKind.BANK, 500, currency))

        assertEquals("工资卡", created.name)
        assertEquals(5, created.displayOrder)
        assertEquals(500, created.openingBalance.minorUnits)
    }

    @Test
    fun `archive is idempotent and never rewrites opening balance`() = runBlocking {
        val original = account(id, name = "现金", opening = 8_800)
        val accounts = FakeAccounts(listOf(original))
        val manager = ManageAccounts(accounts, Clock.fixed(now, ZoneOffset.UTC))

        val archived = manager.archive(id)
        val repeated = manager.archive(id)

        assertTrue(archived.isArchived)
        assertEquals(archived, repeated)
        assertFalse(original.isArchived)
        assertEquals(original.openingBalance, archived.openingBalance)
    }

    @Test
    fun `concurrent creates cannot bypass active name uniqueness`() = runBlocking {
        val accounts = FakeAccounts(emptyList(), saveDelayMillis = 25)
        var nextId = 1L
        val manager = ManageAccounts(accounts, Clock.fixed(now, ZoneOffset.UTC)) {
            AccountId(UUID(0, nextId++))
        }
        val command = CreateAccountCommand("Cash", AccountKind.CASH, currency = currency)

        val results = listOf(
            async { runCatching { manager.create(command) } },
            async { runCatching { manager.create(command) } },
        ).map { it.await() }

        assertEquals(1, results.count { it.isSuccess })
        assertEquals(1, results.count { it.isFailure })
        assertEquals(1, accounts.snapshot().count { it.name == "Cash" })
    }

    private fun account(
        accountId: AccountId,
        name: String = "微信钱包",
        opening: Long = 0,
        displayOrder: Int = 0,
    ): Account = Account(accountId, name, AccountKind.PAYMENT, AccountBalance(opening, currency), displayOrder, null)

    private fun expense(accountId: AccountId, amount: Long): Transaction = Transaction(
        TransactionId(UUID.randomUUID()), TransactionType.EXPENSE, Money(amount, currency), now,
        accountId, null, CategoryId(UUID.randomUUID()), null, now, now,
    )
}

private class FakeAccounts(
    initial: List<Account>,
    private val saveDelayMillis: Long = 0,
) : AccountRepository {
    private val values = MutableStateFlow(initial)
    override suspend fun findById(id: AccountId): Account? = values.value.find { it.id == id }
    override suspend fun save(account: Account) {
        if (saveDelayMillis > 0) delay(saveDelayMillis)
        values.value = values.value.filterNot { it.id == account.id } + account
    }
    override fun observeAll(): Flow<List<Account>> = values
    fun snapshot(): List<Account> = values.value
}

private class FakeTransactions(initial: List<Transaction>) : TransactionRepository {
    private val values = MutableStateFlow(initial)
    override suspend fun save(transaction: Transaction) { values.value += transaction }
    override suspend fun findById(id: TransactionId): Transaction? = values.value.find { it.id == id }
    override suspend fun softDelete(id: TransactionId) { values.value = values.value.filterNot { it.id == id } }
    override fun observeActive(): Flow<List<Transaction>> = values
}
