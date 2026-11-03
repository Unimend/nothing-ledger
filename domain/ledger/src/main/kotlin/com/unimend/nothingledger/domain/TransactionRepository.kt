package com.unimend.nothingledger.domain

import com.unimend.nothingledger.model.Transaction
import com.unimend.nothingledger.model.TransactionId
import kotlinx.coroutines.flow.Flow

public interface TransactionRepository {
    public suspend fun save(transaction: Transaction)
    public suspend fun findById(id: TransactionId): Transaction?
    public suspend fun softDelete(id: TransactionId)
    public fun observeActive(): Flow<List<Transaction>>
}

