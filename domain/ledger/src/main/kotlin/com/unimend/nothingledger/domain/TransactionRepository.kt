package com.unimend.nothingledger.domain

import com.unimend.nothingledger.model.Transaction
import com.unimend.nothingledger.model.TransactionId
import java.time.Instant
import kotlinx.coroutines.flow.Flow

/** Infrastructure port. Presentation code must use transaction use cases, never call mutations here. */
public interface TransactionRepository {
    /** Insert-only; an existing identity is a conflict rather than an edit. */
    public suspend fun insert(transaction: Transaction)
    public suspend fun findById(id: TransactionId): Transaction?
    /** Returns false when the visible revision changed or was deleted. */
    public suspend fun update(transaction: Transaction, expectedUpdatedAt: Instant): Boolean
    /** Returns false when the visible revision changed or was already deleted. */
    public suspend fun softDelete(id: TransactionId, deletedAt: Instant, expectedUpdatedAt: Instant): Boolean
    /** Returns false unless [deletedAt] identifies the exact current deletion. */
    public suspend fun restore(id: TransactionId, deletedAt: Instant, restoredAt: Instant): Boolean
    public fun observeActive(): Flow<List<Transaction>>
}

