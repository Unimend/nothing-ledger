package com.unimend.nothingledger.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
internal interface AccountDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insert(account: AccountEntity)
}

@Dao
internal interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insert(category: CategoryEntity)
}

@Dao
internal interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insert(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions WHERE id = :id AND deletedAtEpochMillis IS NULL")
    fun findActiveById(id: String): TransactionEntity?

    @Query("UPDATE transactions SET deletedAtEpochMillis = :deletedAt, updatedAtEpochMillis = :deletedAt WHERE id = :id AND deletedAtEpochMillis IS NULL")
    fun softDelete(id: String, deletedAt: Long): Int
}

