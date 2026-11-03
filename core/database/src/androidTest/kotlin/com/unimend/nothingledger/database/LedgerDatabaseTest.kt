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
import org.junit.Assert.assertNull
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

    @Test fun transactionRoundTripAndSoftDelete() {
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
        val repository = RoomTransactionRepository(
            database.transactionDao(),
            Clock.fixed(now.plusSeconds(60), ZoneOffset.UTC),
        )

        repository.save(transaction)
        assertEquals(transaction, repository.findById(transaction.id))
        repository.softDelete(transaction.id)
        assertNull(repository.findById(transaction.id))
    }

    @Test fun domainCoordinatorPersistsThroughAllRoomRepositories() {
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
}

