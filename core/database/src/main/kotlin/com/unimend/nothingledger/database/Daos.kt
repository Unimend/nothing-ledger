package com.unimend.nothingledger.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
internal interface AccountDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(account: AccountEntity)

    @Upsert suspend fun save(account: AccountEntity)
    @Query("SELECT * FROM accounts WHERE id = :id") suspend fun findById(id: String): AccountEntity?
    @Query("SELECT * FROM accounts ORDER BY displayOrder, name") fun observeAll(): Flow<List<AccountEntity>>
}

@Dao
internal interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(category: CategoryEntity)

    @Upsert suspend fun save(category: CategoryEntity)
    @Query("SELECT * FROM categories WHERE id = :id") suspend fun findById(id: String): CategoryEntity?
    @Query("SELECT * FROM categories ORDER BY displayOrder, name") fun observeAll(): Flow<List<CategoryEntity>>
}

@Dao
internal interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions WHERE id = :id AND deletedAtEpochMillis IS NULL")
    suspend fun findActiveById(id: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE deletedAtEpochMillis IS NULL ORDER BY occurredAtEpochMillis DESC")
    fun observeActive(): Flow<List<TransactionEntity>>

    @Query("UPDATE transactions SET deletedAtEpochMillis = :deletedAt, updatedAtEpochMillis = :deletedAt WHERE id = :id AND deletedAtEpochMillis IS NULL")
    suspend fun softDelete(id: String, deletedAt: Long): Int
}

