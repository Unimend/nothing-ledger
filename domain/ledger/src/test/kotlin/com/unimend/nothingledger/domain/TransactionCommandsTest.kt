package com.unimend.nothingledger.domain

import com.unimend.nothingledger.model.*
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Currency
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TransactionCommandsTest {
    private val cny = Currency.getInstance("CNY")
    private val createdAt = Instant.parse("2026-11-01T00:00:00Z")
    private val editedAt = Instant.parse("2026-11-03T00:00:00Z")
    private val accountId = AccountId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
    private val categoryId = CategoryId(UUID.fromString("00000000-0000-0000-0000-000000000002"))
    private val transactionId = TransactionId(UUID.fromString("00000000-0000-0000-0000-000000000003"))
    private val account = Account(accountId, "现金", AccountKind.CASH, AccountBalance(0, cny), 0, null)
    private val category = Category(categoryId, null, "餐饮", CategoryType.EXPENSE, 0, null)

    @Test
    fun `update preserves identity and creation time while sharing create validation`() = runBlocking {
        val repository = FakeTransactionRepository(listOf(transaction()))
        val commands = commands(repository)
        val result = commands.update(
            UpdateTransactionCommand(transactionId, createdAt, draft(amount = 450, note = "  lunch  ")),
        )

        assertEquals(transactionId, result.after.id)
        assertEquals(createdAt, result.after.createdAt)
        assertEquals(editedAt, result.after.updatedAt)
        assertEquals("lunch", result.after.note)
        assertEquals(100, result.before.transaction.money.minorUnits)
        assertEquals(450, repository.findById(transactionId)?.money?.minorUnits)
    }

    @Test
    fun `stale edit is rejected without writing`() {
        val repository = FakeTransactionRepository(listOf(transaction()))
        val commands = commands(repository)

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                commands.update(
                    UpdateTransactionCommand(transactionId, Instant.EPOCH, draft(amount = 999)),
                )
            }
        }
        assertEquals(100, runBlocking { repository.findById(transactionId)?.money?.minorUnits })
    }

    @Test
    fun `edit cannot bypass archived account rule`() {
        val repository = FakeTransactionRepository(listOf(transaction()))
        val archivedAccounts = FakeAccountRepository(listOf(account.archive(editedAt)))
        val commands = TransactionCommands(
            repository, archivedAccounts, FakeCategoryRepository(listOf(category)), fixedClock(),
        )

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { commands.update(UpdateTransactionCommand(transactionId, createdAt, draft(200))) }
        }
        assertEquals(100, runBlocking { repository.findById(transactionId)?.money?.minorUnits })
    }

    @Test
    fun `only latest deletion can be restored and undo is consumed`() = runBlocking {
        val secondId = TransactionId(UUID.fromString("00000000-0000-0000-0000-000000000004"))
        val repository = FakeTransactionRepository(listOf(transaction(), transaction(secondId)))
        val commands = commands(repository)

        commands.delete(transactionId)
        commands.delete(secondId)
        val restored = commands.undoLastDelete()

        assertEquals(secondId, restored?.id)
        assertNull(repository.findById(transactionId))
        assertEquals(secondId, repository.findById(secondId)?.id)
        assertNull(commands.undoLastDelete())
        assertNull(commands.peekRecoverableDelete())
    }

    @Test
    fun `delete receipt is an immutable pre-delete snapshot`() = runBlocking {
        val repository = FakeTransactionRepository(listOf(transaction()))
        val commands = commands(repository)

        val receipt = commands.delete(transactionId)

        assertEquals(transaction(), receipt.snapshot.transaction)
        assertEquals(editedAt, receipt.deletedAt)
        assertTrue(repository.findById(transactionId) == null)
    }

    private fun commands(repository: FakeTransactionRepository): TransactionCommands = TransactionCommands(
        repository, FakeAccountRepository(listOf(account)), FakeCategoryRepository(listOf(category)), fixedClock(),
    )

    private fun fixedClock(): Clock = Clock.fixed(editedAt, ZoneOffset.UTC)

    private fun draft(amount: Long, note: String? = null): TransactionDraft = TransactionDraft(
        TransactionType.EXPENSE, Money(amount, cny), createdAt, accountId, null, categoryId, note,
    )

    private fun transaction(id: TransactionId = transactionId): Transaction = Transaction(
        id, TransactionType.EXPENSE, Money(100, cny), createdAt,
        accountId, null, categoryId, null, createdAt, createdAt,
    )
}

private class FakeTransactionRepository(initial: List<Transaction>) : TransactionRepository {
    private val active = MutableStateFlow(initial.associateBy(Transaction::id))
    private val deleted = mutableMapOf<TransactionId, Pair<Transaction, Instant>>()
    override suspend fun insert(transaction: Transaction) { active.value += transaction.id to transaction }
    override suspend fun findById(id: TransactionId): Transaction? = active.value[id]
    override suspend fun update(transaction: Transaction, expectedUpdatedAt: Instant): Boolean {
        val current = active.value[transaction.id] ?: return false
        if (current.updatedAt != expectedUpdatedAt) return false
        active.value += transaction.id to transaction
        return true
    }
    override suspend fun softDelete(id: TransactionId, deletedAt: Instant, expectedUpdatedAt: Instant): Boolean {
        val current = active.value[id] ?: return false
        if (current.updatedAt != expectedUpdatedAt) return false
        deleted[id] = current to deletedAt
        active.value -= id
        return true
    }
    override suspend fun restore(id: TransactionId, deletedAt: Instant, restoredAt: Instant): Boolean {
        val entry = deleted[id] ?: return false
        if (entry.second != deletedAt || id in active.value) return false
        active.value += id to entry.first.copy(updatedAt = restoredAt)
        deleted.remove(id)
        return true
    }
    override fun observeActive(): Flow<List<Transaction>> = MutableStateFlow(active.value.values.toList())
}

private class FakeAccountRepository(private val values: List<Account>) : AccountRepository {
    override suspend fun findById(id: AccountId): Account? = values.find { it.id == id }
    override suspend fun save(account: Account): Unit = error("Not used")
    override fun observeAll(): Flow<List<Account>> = MutableStateFlow(values)
}

private class FakeCategoryRepository(private val values: List<Category>) : CategoryRepository {
    override suspend fun findById(id: CategoryId): Category? = values.find { it.id == id }
    override suspend fun save(category: Category): Unit = error("Not used")
    override fun observeAll(): Flow<List<Category>> = MutableStateFlow(values)
}
