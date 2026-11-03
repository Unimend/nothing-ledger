package com.unimend.nothingledger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.unimend.nothingledger.accounts.AccountsState
import com.unimend.nothingledger.accounts.CreateAccountCommand
import com.unimend.nothingledger.accounts.ManageAccounts
import com.unimend.nothingledger.accounts.ObserveAccounts
import com.unimend.nothingledger.database.LedgerDatabaseHandle
import com.unimend.nothingledger.model.AccountId
import com.unimend.nothingledger.model.AccountKind
import java.time.Clock
import java.util.Currency
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal sealed interface AccountEditor {
    data object Closed : AccountEditor
    data class Create(val name: String = "") : AccountEditor
    data class Rename(val id: AccountId, val originalName: String, val name: String) : AccountEditor
}

internal data class AccountEditorState(
    val editor: AccountEditor = AccountEditor.Closed,
    val busy: Boolean = false,
    val error: String? = null,
)

internal class AccountsViewModel(
    database: LedgerDatabaseHandle,
    clock: Clock = Clock.systemUTC(),
) : ViewModel() {
    private val manager = ManageAccounts(database.accounts(), clock)
    val accounts: StateFlow<AccountsState> = ObserveAccounts(database.accounts(), database.transactions())()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccountsState.Loading)

    private val _editor = MutableStateFlow(AccountEditorState())
    val editor: StateFlow<AccountEditorState> = _editor.asStateFlow()

    fun beginCreate() { _editor.value = AccountEditorState(AccountEditor.Create()) }
    fun beginRename(id: AccountId, name: String) {
        _editor.value = AccountEditorState(AccountEditor.Rename(id, name, name))
    }
    fun changeName(name: String) {
        _editor.value = _editor.value.copy(
            editor = when (val current = _editor.value.editor) {
                AccountEditor.Closed -> current
                is AccountEditor.Create -> current.copy(name = name)
                is AccountEditor.Rename -> current.copy(name = name)
            },
            error = null,
        )
    }
    fun cancel() { _editor.value = AccountEditorState() }

    fun save() {
        if (_editor.value.busy) return
        val current = _editor.value.editor
        if (current == AccountEditor.Closed) return
        _editor.value = _editor.value.copy(busy = true, error = null)
        viewModelScope.launch {
            runCatching {
                when (current) {
                    AccountEditor.Closed -> Unit
                    is AccountEditor.Create -> manager.create(
                        CreateAccountCommand(current.name, AccountKind.PAYMENT, currency = Currency.getInstance("CNY")),
                    )
                    is AccountEditor.Rename -> manager.rename(current.id, current.name)
                }
            }.onSuccess { _editor.value = AccountEditorState() }
                .onFailure { error ->
                    _editor.value = _editor.value.copy(
                        busy = false,
                        error = error.message ?: "Account update failed",
                    )
                }
        }
    }

    fun archive(id: AccountId) {
        viewModelScope.launch {
            runCatching { manager.archive(id) }
                .onFailure { error -> _editor.value = _editor.value.copy(error = error.message) }
        }
    }

    companion object {
        fun factory(database: LedgerDatabaseHandle): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                require(modelClass.isAssignableFrom(AccountsViewModel::class.java))
                @Suppress("UNCHECKED_CAST")
                return AccountsViewModel(database) as T
            }
        }
    }
}
