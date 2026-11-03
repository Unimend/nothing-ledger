package com.unimend.nothingledger.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
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
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Currency
import java.util.UUID
import com.unimend.nothingledger.domain.CreateTransaction
import com.unimend.nothingledger.domain.TransactionDraft
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first

@RunWith(AndroidJUnit4::class)
class LedgerDatabaseTest {
    private lateinit var database: LedgerRoomDatabase
    private val now = Instant.parse("2026-11-03T07:00:00Z")

    @Before fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            LedgerRoomDatabase::class.java,
        ).build()
    }

    @After fun closeDatabase() = database.close()

    @Test fun transactionRoundTripAndSoftDelete() = runBlocking {
        val accountId = UUID.randomUUID()
        val categoryId = UUID.randomUUID()
        database.accountDao().insert(
            AccountEntity(accountId.toString(), "Wallet", "PAYMENT", "CNY", 0, 0, null),
        )
        database.categoryDao().insert(
            CategoryEntity(categoryId.toString(), null, "Food", "EXPENSE", 0, null),
        )
        val transaction = Transaction(
            id = TransactionId(UUID.randomUUID()),
            type = TransactionType.EXPENSE,
            money = Money(1880, Currency.getInstance("CNY")),
            occurredAt = now,
            sourceAccountId = AccountId(accountId),
            destinationAccountId = null,
            categoryId = CategoryId(categoryId),
            note = "Lunch",
            createdAt = now,
            updatedAt = now,
        )
        val repository = RoomTransactionRepository(database.transactionDao())

        repository.insert(transaction)
        assertEquals(transaction, repository.findById(transaction.id))
        val edited = transaction.copy(note = "Dinner", updatedAt = now.plusSeconds(30))
        assertTrue(repository.update(edited, transaction.updatedAt))
        assertFalse(repository.update(transaction, transaction.updatedAt))
        val deletedAt = now.plusSeconds(60)
        assertTrue(repository.softDelete(transaction.id, deletedAt, edited.updatedAt))
        assertNull(repository.findById(transaction.id))
        assertTrue(repository.restore(transaction.id, deletedAt, now.plusSeconds(90)))
        assertEquals("Dinner", repository.findById(transaction.id)?.note)
        assertFalse(repository.restore(transaction.id, deletedAt, now.plusSeconds(120)))
    }

    @Test fun domainCoordinatorPersistsThroughAllRoomRepositories() = runBlocking {
        val account = Account(
            AccountId(UUID.randomUUID()), "Wallet", AccountKind.PAYMENT,
            AccountBalance(0, Currency.getInstance("CNY")), 0, null,
        )
        val category = Category(
            CategoryId(UUID.randomUUID()), null, "Food", CategoryType.EXPENSE, 0, null,
        )
        val accountRepository = RoomAccountRepository(database.accountDao())
        val categoryRepository = RoomCategoryRepository(database.categoryDao())
        val transactionRepository = RoomTransactionRepository(database.transactionDao())
        accountRepository.save(account)
        categoryRepository.save(category)

        val transaction = CreateTransaction(
            transactionRepository,
            accountRepository,
            categoryRepository,
            Clock.fixed(now, ZoneOffset.UTC),
        ) {
            TransactionId(UUID.fromString("00000000-0000-0000-0000-000000000010"))
        }(
            TransactionDraft(
                TransactionType.EXPENSE,
                Money(2000, Currency.getInstance("CNY")),
                now,
                sourceAccountId = account.id,
                categoryId = category.id,
            ),
        )

        assertEquals(transaction, transactionRepository.findById(transaction.id))
    }

    @Test fun activeObservationReflectsSavesOrderingAndSoftDeletion() = runBlocking {
        val account = Account(
            AccountId(UUID.randomUUID()), "Wallet", AccountKind.PAYMENT,
            AccountBalance(0, Currency.getInstance("CNY")), 0, null,
        )
        val category = Category(
            CategoryId(UUID.randomUUID()), null, "Food", CategoryType.EXPENSE, 0, null,
        )
        val accountRepository = RoomAccountRepository(database.accountDao())
        val categoryRepository = RoomCategoryRepository(database.categoryDao())
        val transactionRepository = RoomTransactionRepository(database.transactionDao())
        accountRepository.save(account)
        categoryRepository.save(category)
        val create = CreateTransaction(
            transactionRepository,
            accountRepository,
            categoryRepository,
            Clock.fixed(now, ZoneOffset.UTC),
        )

        val older = create(
            TransactionDraft(
                TransactionType.EXPENSE,
                Money(1200, Currency.getInstance("CNY")),
                now.minusSeconds(60),
                sourceAccountId = account.id,
                categoryId = category.id,
                note = "Older",
            ),
        )
        val newer = create(
            TransactionDraft(
                TransactionType.EXPENSE,
                Money(2400, Currency.getInstance("CNY")),
                now,
                sourceAccountId = account.id,
                categoryId = category.id,
                note = "Newer",
            ),
        )

        assertEquals(listOf(newer, older), transactionRepository.observeActive().first())

        transactionRepository.softDelete(newer.id, now.plusSeconds(120), newer.updatedAt)

        assertEquals(listOf(older), transactionRepository.observeActive().first())
    }

    @Test fun stableDefaultIdsCanBeSeededRepeatedlyWithoutDuplicates() = runBlocking {
        val accountId = AccountId(UUID.fromString("10000000-0000-0000-0000-000000000001"))
        val expenseId = CategoryId(UUID.fromString("20000000-0000-0000-0000-000000000001"))
        val accountRepository = RoomAccountRepository(database.accountDao())
        val categoryRepository = RoomCategoryRepository(database.categoryDao())
        val initialAccount = Account(
            accountId, "Wallet", AccountKind.PAYMENT,
            AccountBalance(0, Currency.getInstance("CNY")), 0, null,
        )
        val renamedAccount = initialAccount.copy(name = "WeChat Wallet")
        val initialCategory = Category(expenseId, null, "Expense", CategoryType.EXPENSE, 0, null)
        val renamedCategory = initialCategory.copy(name = "Daily Expense")

        accountRepository.save(initialAccount)
        categoryRepository.save(initialCategory)
        accountRepository.save(renamedAccount)
        categoryRepository.save(renamedCategory)

        assertEquals(listOf(renamedAccount), accountRepository.observeAll().first())
        assertEquals(listOf(renamedCategory), categoryRepository.observeAll().first())
        assertEquals(renamedAccount, accountRepository.findById(accountId))
        assertEquals(renamedCategory, categoryRepository.findById(expenseId))
    }
}

