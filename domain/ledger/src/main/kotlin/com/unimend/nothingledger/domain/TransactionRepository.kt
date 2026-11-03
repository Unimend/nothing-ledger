package com.unimend.nothingledger.domain

import com.unimend.nothingledger.model.Transaction
import com.unimend.nothingledger.model.TransactionId

public interface TransactionRepository {
    public fun save(transaction: Transaction)
    public fun findById(id: TransactionId): Transaction?
    public fun softDelete(id: TransactionId)
}

