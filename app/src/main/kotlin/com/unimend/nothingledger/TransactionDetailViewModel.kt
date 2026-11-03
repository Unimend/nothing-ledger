package com.unimend.nothingledger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.unimend.nothingledger.database.LedgerDatabaseHandle
import com.unimend.nothingledger.model.TransactionId
import com.unimend.nothingledger.timeline.ObserveTransactionDetail
import com.unimend.nothingledger.timeline.TransactionDetailState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

internal class TransactionDetailViewModel(
    database: LedgerDatabaseHandle,
    id: TransactionId,
) : ViewModel() {
    val state: StateFlow<TransactionDetailState> = ObserveTransactionDetail(
        transactions = database.transactions(),
        accounts = database.accounts(),
        categories = database.categories(),
    )(id).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TransactionDetailState.Loading(id),
    )

    companion object {
        fun factory(database: LedgerDatabaseHandle, id: TransactionId): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                    require(modelClass.isAssignableFrom(TransactionDetailViewModel::class.java))
                    @Suppress("UNCHECKED_CAST")
                    return TransactionDetailViewModel(database, id) as T
                }
            }
    }
}
