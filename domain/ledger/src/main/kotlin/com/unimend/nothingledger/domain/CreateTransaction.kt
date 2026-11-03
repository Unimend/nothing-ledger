package com.unimend.nothingledger.domain

import com.unimend.nothingledger.model.Transaction
import com.unimend.nothingledger.model.TransactionId
import java.time.Clock
import java.util.UUID

public class CreateTransaction(
    private val repository: TransactionRepository,
    private val clock: Clock,
    private val newId: () -> TransactionId = { TransactionId(UUID.randomUUID()) },
) {
    public operator fun invoke(draft: TransactionDraft): Transaction {
        val now = clock.instant()
        val transaction = Transaction(
            id = newId(),
            type = draft.type,
            money = draft.money,
            occurredAt = draft.occurredAt,
            sourceAccountId = draft.sourceAccountId,
            destinationAccountId = draft.destinationAccountId,
            categoryId = draft.categoryId,
            note = draft.note?.trim()?.ifEmpty { null },
            createdAt = now,
            updatedAt = now,
        )
        repository.save(transaction)
        return transaction
    }
}

