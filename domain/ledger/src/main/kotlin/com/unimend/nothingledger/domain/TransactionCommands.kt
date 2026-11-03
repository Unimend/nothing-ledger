package com.unimend.nothingledger.domain

import com.unimend.nothingledger.model.Transaction
import com.unimend.nothingledger.model.TransactionId
import java.time.Clock
import java.time.Instant
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

public data class UpdateTransactionCommand(
    public val id: TransactionId,
    /** Optimistic concurrency token obtained from the last visible snapshot. */
    public val expectedUpdatedAt: Instant,
    public val draft: TransactionDraft,
)

public data class TransactionSnapshot(public val transaction: Transaction)

public data class TransactionEdit(
    public val before: TransactionSnapshot,
    public val after: Transaction,
)

public data class DeleteReceipt(
    public val snapshot: TransactionSnapshot,
    public val deletedAt: Instant,
)

/**
 * Serializes transaction mutations within the application process. Every edit
 * re-enters the same validation boundary as creation. Only the latest deletion
 * is recoverable; a later delete deliberately replaces the earlier receipt.
 */
public class TransactionCommands(
    private val transactions: TransactionRepository,
    accounts: AccountRepository,
    categories: CategoryRepository,
    private val clock: Clock,
) {
    private val validate = ValidateTransactionDraft(accounts, categories)
    private val mutationLock = Mutex()
    private var recoverableDelete: DeleteReceipt? = null

    public suspend fun update(command: UpdateTransactionCommand): TransactionEdit = mutationLock.withLock {
        val current = requireNotNull(transactions.findById(command.id)) { "Transaction does not exist" }
        require(current.updatedAt == command.expectedUpdatedAt) { "Transaction changed since it was opened" }
        validate(command.draft)
        val updated = current.copy(
            type = command.draft.type,
            money = command.draft.money,
            occurredAt = command.draft.occurredAt,
            sourceAccountId = command.draft.sourceAccountId,
            destinationAccountId = command.draft.destinationAccountId,
            categoryId = command.draft.categoryId,
            note = command.draft.note?.trim()?.ifEmpty { null },
            updatedAt = clock.instant(),
        )
        check(transactions.update(updated, current.updatedAt)) { "Transaction changed concurrently" }
        TransactionEdit(TransactionSnapshot(current), updated)
    }

    public suspend fun delete(id: TransactionId): DeleteReceipt = mutationLock.withLock {
        val current = requireNotNull(transactions.findById(id)) { "Transaction does not exist" }
        val receipt = DeleteReceipt(TransactionSnapshot(current), clock.instant())
        check(transactions.softDelete(id, receipt.deletedAt, current.updatedAt)) {
            "Transaction changed concurrently"
        }
        recoverableDelete = receipt
        receipt
    }

    public suspend fun undoLastDelete(): Transaction? = mutationLock.withLock {
        val receipt = recoverableDelete ?: return@withLock null
        val restoredAt = clock.instant()
        check(transactions.restore(receipt.snapshot.transaction.id, receipt.deletedAt, restoredAt)) {
            "Deletion is no longer undoable"
        }
        val restored = receipt.snapshot.transaction.copy(updatedAt = restoredAt)
        recoverableDelete = null
        restored
    }

    public suspend fun peekRecoverableDelete(): DeleteReceipt? = mutationLock.withLock { recoverableDelete }
}
