package com.unimend.nothingledger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.unimend.nothingledger.database.LedgerDatabaseHandle
import com.unimend.nothingledger.entry.EntryOptionsQuery
import com.unimend.nothingledger.entry.EntryOptionsState
import com.unimend.nothingledger.entry.EntryState
import com.unimend.nothingledger.entry.ObserveEntryOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.ExperimentalCoroutinesApi

/** Thin Android lifecycle adapter; all option rules live in feature:entry. */
@OptIn(ExperimentalCoroutinesApi::class)
internal class EntrySelectorsViewModel(database: LedgerDatabaseHandle) : ViewModel() {
    private val context = MutableStateFlow<EntryState?>(null)
    private val observeOptions = ObserveEntryOptions(database.accounts(), database.categories())

    val state: StateFlow<EntryOptionsState> = context
        .flatMapLatest { entry ->
            if (entry == null) {
                flowOf(EntryOptionsState.Loading)
            } else {
                observeOptions(
                    EntryOptionsQuery(
                        mode = entry.mode,
                        currency = entry.currency,
                        selectedSourceAccountId = entry.sourceAccountId,
                        selectedDestinationAccountId = entry.destinationAccountId,
                    ),
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = EntryOptionsState.Loading,
        )

    fun updateContext(entry: EntryState) {
        context.value = entry
    }

    companion object {
        fun factory(database: LedgerDatabaseHandle): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                    require(modelClass.isAssignableFrom(EntrySelectorsViewModel::class.java))
                    @Suppress("UNCHECKED_CAST")
                    return EntrySelectorsViewModel(database) as T
                }
            }
    }
}

