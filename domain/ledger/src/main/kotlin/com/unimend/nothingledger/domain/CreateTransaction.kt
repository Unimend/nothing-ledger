package com.unimend.nothingledger.domain

import com.unimend.nothingledger.model.Transaction
import com.unimend.nothingledger.model.TransactionId
import java.time.Clock
import java.util.UUID

public class CreateTransaction(
    private val repository: TransactionRepository,
    private val accounts: AccountRepository,
    private val categories: CategoryRepository,
    private val clock: Clock,
    private val newId: () -> TransactionId = { TransactionId(UUID.randomUUID()) },
) {
    public suspend operator fun invoke(draft: TransactionDraft): Transaction {
        val participatingAccounts = listOfNotNull(draft.sourceAccountId, draft.destinationAccountId)
            .distinct()
            .associateWith { id -> requireNotNull(accounts.findById(id)) { "Account does not exist" } }
        AccountTransactionPolicy().validate(draft, participatingAccounts)
        when (draft.type) {
            com.unimend.nothingledger.model.TransactionType.TRANSFER ->
                require(draft.categoryId == null) { "Transfer must not have a category" }
            else -> RequireUsableCategory(categories)(
                requireNotNull(draft.categoryId) { "Transaction requires a category" },
                draft.type,
            )
        }
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

