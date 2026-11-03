package com.unimend.nothingledger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.unimend.nothingledger.database.LedgerDatabaseHandle
import com.unimend.nothingledger.domain.TransactionCommands
import com.unimend.nothingledger.domain.TransactionDraft
import com.unimend.nothingledger.domain.UpdateTransactionCommand
import com.unimend.nothingledger.model.Transaction
import com.unimend.nothingledger.timeline.TransactionDetailCommands
import com.unimend.nothingledger.timeline.TransactionDetailSession
import com.unimend.nothingledger.timeline.TransactionDetailActionState
import java.time.Clock
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

internal class TransactionDetailActionsViewModel(
    database: LedgerDatabaseHandle,
    clock: Clock = Clock.systemUTC(),
) : ViewModel() {
    private val domainCommands = TransactionCommands(
        database.transactions(), database.accounts(), database.categories(), clock,
    )
    private val session = TransactionDetailSession(
        object : TransactionDetailCommands {
            override suspend fun update(command: UpdateTransactionCommand) = domainCommands.update(command)
            override suspend fun delete(transaction: Transaction) = domainCommands.delete(transaction.id)
            override suspend fun undoLastDelete(): Transaction? = domainCommands.undoLastDelete()
        },
    )

    val state: StateFlow<TransactionDetailActionState> = session.state

    fun open(transaction: Transaction): Unit = session.open(transaction)
    fun close(): Unit = session.close()
    fun update(draft: TransactionDraft) { viewModelScope.launch { session.update(draft) } }
    fun delete() { viewModelScope.launch { session.delete() } }
    fun undoDelete() { viewModelScope.launch { session.undoDelete() } }

    companion object {
        fun factory(database: LedgerDatabaseHandle): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                    require(modelClass.isAssignableFrom(TransactionDetailActionsViewModel::class.java))
                    @Suppress("UNCHECKED_CAST")
                    return TransactionDetailActionsViewModel(database) as T
                }
            }
    }
}
