package com.unimend.nothingledger.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

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
    public fun transactions(): RoomTransactionRepository = RoomTransactionRepository(database.transactionDao())
    public fun accounts(): RoomAccountRepository = RoomAccountRepository(database.accountDao())
    public fun categories(): RoomCategoryRepository = RoomCategoryRepository(database.categoryDao())
    override fun close(): Unit = database.close()
}

