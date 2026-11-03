package com.unimend.nothingledger.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.unimend.nothingledger.domain.AccountRepository
import com.unimend.nothingledger.domain.CategoryRepository
import com.unimend.nothingledger.domain.TransactionRepository

@Database(
    entities = [AccountEntity::class, CategoryEntity::class, TransactionEntity::class],
    version = 1,
    exportSchema = true,
)
internal abstract class LedgerRoomDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
}

public object LedgerDatabaseFactory {
    public fun create(context: Context): LedgerDatabaseHandle {
        val database = Room.databaseBuilder(
            context.applicationContext,
            LedgerRoomDatabase::class.java,
            "nothing-ledger.db",
        ).build()
        return LedgerDatabaseHandle(database)
    }
}

public class LedgerDatabaseHandle internal constructor(
    internal val database: LedgerRoomDatabase,
) : AutoCloseable {
    private val transactionRepository: TransactionRepository by lazy {
        RoomTransactionRepository(database.transactionDao())
    }
    private val accountRepository: AccountRepository by lazy {
        RoomAccountRepository(database.accountDao())
    }
    private val categoryRepository: CategoryRepository by lazy {
        RoomCategoryRepository(database.categoryDao())
    }

    /** Exposes domain ports only; Room implementations stay behind the data boundary. */
    public fun transactions(): TransactionRepository = transactionRepository

    /** Exposes domain ports only; Room implementations stay behind the data boundary. */
    public fun accounts(): AccountRepository = accountRepository

    /** Exposes domain ports only; Room implementations stay behind the data boundary. */
    public fun categories(): CategoryRepository = categoryRepository

    override fun close(): Unit = database.close()
}

