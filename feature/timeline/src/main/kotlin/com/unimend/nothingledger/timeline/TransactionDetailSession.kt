package com.unimend.nothingledger.timeline

import com.unimend.nothingledger.domain.DeleteReceipt
import com.unimend.nothingledger.domain.TransactionDraft
import com.unimend.nothingledger.domain.TransactionEdit
import com.unimend.nothingledger.domain.UpdateTransactionCommand
import com.unimend.nothingledger.model.Transaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

public enum class TransactionDetailPhase { CLOSED, READY, WORKING, DELETED, ERROR }

public data class TransactionDetailActionState(
    public val phase: TransactionDetailPhase = TransactionDetailPhase.CLOSED,
    public val transaction: Transaction? = null,
    public val canUndoDelete: Boolean = false,
    public val errorMessage: String? = null,
)

/** Narrow application port; its production adapter is the domain TransactionCommands instance. */
public interface TransactionDetailCommands {
    public suspend fun update(command: UpdateTransactionCommand): TransactionEdit
    public suspend fun delete(transaction: Transaction): DeleteReceipt
    public suspend fun undoLastDelete(): Transaction?
}

/** Owns detail/edit/delete UI state without exposing a repository to Compose. */
public class TransactionDetailSession(
    private val commands: TransactionDetailCommands,
) {
    private val mutationLock = Mutex()
    private val mutableState = MutableStateFlow(TransactionDetailActionState())
    public val state: StateFlow<TransactionDetailActionState> = mutableState.asStateFlow()

    public fun open(transaction: Transaction) {
        if (mutableState.value.phase == TransactionDetailPhase.WORKING) return
        mutableState.value = TransactionDetailActionState(
            phase = TransactionDetailPhase.READY,
            transaction = transaction,
        )
    }

    public fun close() {
        if (mutableState.value.phase != TransactionDetailPhase.WORKING) {
            mutableState.value = TransactionDetailActionState()
        }
    }

    public suspend fun update(draft: TransactionDraft): Unit = mutationLock.withLock {
        val current = mutableState.value.transaction ?: return@withLock
        mutableState.value = mutableState.value.copy(
            phase = TransactionDetailPhase.WORKING,
            errorMessage = null,
        )
        runCatching {
            commands.update(UpdateTransactionCommand(current.id, current.updatedAt, draft))
        }.onSuccess { edit ->
            mutableState.value = TransactionDetailActionState(
                phase = TransactionDetailPhase.READY,
                transaction = edit.after,
            )
        }.onFailure { error -> showError(current, error) }
    }

    public suspend fun delete(): Unit = mutationLock.withLock {
        val current = mutableState.value.transaction ?: return@withLock
        mutableState.value = mutableState.value.copy(
            phase = TransactionDetailPhase.WORKING,
            errorMessage = null,
        )
        runCatching { commands.delete(current) }
            .onSuccess {
                mutableState.value = TransactionDetailActionState(
                    phase = TransactionDetailPhase.DELETED,
                    transaction = current,
                    canUndoDelete = true,
                )
            }
            .onFailure { error -> showError(current, error) }
    }

    public suspend fun undoDelete(): Unit = mutationLock.withLock {
        val current = mutableState.value.transaction ?: return@withLock
        if (!mutableState.value.canUndoDelete) return@withLock
        mutableState.value = mutableState.value.copy(
            phase = TransactionDetailPhase.WORKING,
            errorMessage = null,
        )
        runCatching { requireNotNull(commands.undoLastDelete()) { "Deletion is no longer undoable" } }
            .onSuccess { restored ->
                mutableState.value = TransactionDetailActionState(
                    phase = TransactionDetailPhase.READY,
                    transaction = restored,
                )
            }
            .onFailure { error ->
                mutableState.value = TransactionDetailActionState(
                    phase = TransactionDetailPhase.ERROR,
                    transaction = current,
                    canUndoDelete = true,
                    errorMessage = error.message ?: "Transaction restore failed",
                )
            }
    }

    private fun showError(current: Transaction, error: Throwable) {
        mutableState.value = TransactionDetailActionState(
            phase = TransactionDetailPhase.ERROR,
            transaction = current,
            errorMessage = error.message ?: "Transaction update failed",
        )
    }
}
