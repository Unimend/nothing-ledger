package com.unimend.nothingledger.timeline

import com.unimend.nothingledger.domain.AccountRepository
import com.unimend.nothingledger.domain.CategoryRepository
import com.unimend.nothingledger.domain.TransactionRepository
import com.unimend.nothingledger.model.Account
import com.unimend.nothingledger.model.AccountBalance
import com.unimend.nothingledger.model.AccountId
import com.unimend.nothingledger.model.AccountKind
import com.unimend.nothingledger.model.Category
import com.unimend.nothingledger.model.CategoryId
import com.unimend.nothingledger.model.CategoryType
import com.unimend.nothingledger.model.Money
import com.unimend.nothingledger.model.Transaction
import com.unimend.nothingledger.model.TransactionId
import com.unimend.nothingledger.model.TransactionType
import java.time.Instant
import java.util.Currency
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TransactionDetailPresentationTest {
    private val source = account(1, "WeChat Wallet", archived = true)
    private val destination = account(2, "Bank Card")
    private val parent = category(1, "Food")
    private val child = category(2, "Lunch", parentId = parent.id)

    @Test fun `expense resolves archived account and readable category path`() {
        val transaction = transaction(
            type = TransactionType.EXPENSE,
            sourceId = source.id,
            categoryId = child.id,
        )

        val detail = buildTransactionDetailPresentation(
            transaction,
            accounts = listOf(source),
            categories = listOf(child, parent),
        )

        assertEquals("WeChat Wallet", detail.sourceAccountName)
        assertTrue(requireNotNull(detail.sourceAccount).isArchived)
        assertEquals("Food / Lunch", detail.categoryLabel)
        assertTrue(detail.referencesComplete)
    }

    @Test fun `transfer resolves both account names and has no category`() {
        val detail = buildTransactionDetailPresentation(
            transaction(TransactionType.TRANSFER, source.id, destination.id),
            listOf(source, destination),
            emptyList(),
        )

        assertEquals("WeChat Wallet", detail.sourceAccountName)
        assertEquals("Bank Card", detail.destinationAccountName)
        assertNull(detail.categoryLabel)
        assertTrue(detail.referencesComplete)
    }

    @Test fun `missing references are explicit instead of inventing labels`() {
        val detail = buildTransactionDetailPresentation(
            transaction(TransactionType.EXPENSE, source.id, categoryId = child.id),
            accounts = emptyList(),
            categories = listOf(child),
        )

        assertNull(detail.sourceAccountName)
        assertEquals("Lunch", detail.categoryLabel)
        assertEquals(
            setOf(MissingDetailReference.SOURCE_ACCOUNT, MissingDetailReference.PARENT_CATEGORY),
            detail.missingReferences,
        )
        assertFalse(detail.referencesComplete)
    }

    @Test fun `observer emits loading then ready and missing states`() = runBlocking {
        val transaction = transaction(TransactionType.EXPENSE, source.id, categoryId = child.id)
        val ready = ObserveTransactionDetail(
            MemoryTransactionRepository(listOf(transaction)),
            MemoryAccountRepository(listOf(source)),
            MemoryCategoryRepository(listOf(parent, child)),
        )(transaction.id).toList()

        assertInstanceOf(TransactionDetailState.Loading::class.java, ready.first())
        assertEquals("Food / Lunch", (ready.last() as TransactionDetailState.Ready).detail.categoryLabel)

        val missing = ObserveTransactionDetail(
            MemoryTransactionRepository(emptyList()),
            MemoryAccountRepository(emptyList()),
            MemoryCategoryRepository(emptyList()),
        )(transaction.id).toList()
        assertInstanceOf(TransactionDetailState.Missing::class.java, missing.last())
        Unit
    }

    private fun account(number: Long, name: String, archived: Boolean = false): Account = Account(
        id = AccountId(UUID(0, number)),
        name = name,
        kind = AccountKind.PAYMENT,
        openingBalance = AccountBalance(0, Currency.getInstance("CNY")),
        displayOrder = number.toInt(),
        archivedAt = Instant.EPOCH.takeIf { archived },
    )

    private fun category(number: Long, name: String, parentId: CategoryId? = null): Category = Category(
        id = CategoryId(UUID(0, number)),
        parentId = parentId,
        name = name,
        type = CategoryType.EXPENSE,
        displayOrder = number.toInt(),
        archivedAt = null,
    )

    private fun transaction(
        type: TransactionType,
        sourceId: AccountId? = null,
        destinationId: AccountId? = null,
        categoryId: CategoryId? = null,
    ): Transaction {
        val now = Instant.parse("2026-11-03T08:00:00Z")
        return Transaction(
            id = TransactionId(UUID.randomUUID()),
            type = type,
            money = Money(2_000, Currency.getInstance("CNY")),
            occurredAt = now,
            sourceAccountId = sourceId,
            destinationAccountId = destinationId,
            categoryId = categoryId,
            note = "Lunch",
            createdAt = now,
            updatedAt = now,
        )
    }

    private class MemoryTransactionRepository(private val values: List<Transaction>) : TransactionRepository {
        override suspend fun insert(transaction: Transaction): Unit = Unit
        override suspend fun findById(id: TransactionId): Transaction? = values.find { it.id == id }
        override suspend fun update(transaction: Transaction, expectedUpdatedAt: Instant): Boolean = false
        override suspend fun softDelete(id: TransactionId, deletedAt: Instant, expectedUpdatedAt: Instant): Boolean = false
        override suspend fun restore(id: TransactionId, deletedAt: Instant, restoredAt: Instant): Boolean = false
        override fun observeActive(): Flow<List<Transaction>> = flowOf(values)
    }

    private class MemoryAccountRepository(private val values: List<Account>) : AccountRepository {
        override suspend fun findById(id: AccountId): Account? = values.find { it.id == id }
        override suspend fun save(account: Account): Unit = Unit
        override fun observeAll(): Flow<List<Account>> = flowOf(values)
    }

    private class MemoryCategoryRepository(private val values: List<Category>) : CategoryRepository {
        override suspend fun findById(id: CategoryId): Category? = values.find { it.id == id }
        override suspend fun save(category: Category): Unit = Unit
        override fun observeAll(): Flow<List<Category>> = flowOf(values)
    }
}
