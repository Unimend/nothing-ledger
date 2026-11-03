package com.unimend.nothingledger

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.unimend.nothingledger.database.LedgerDatabaseHandle
import com.unimend.nothingledger.domain.CreateTransaction
import com.unimend.nothingledger.domain.TransactionDraft
import com.unimend.nothingledger.entry.AmountInput
import com.unimend.nothingledger.entry.EntryAction
import com.unimend.nothingledger.entry.EntryEffect
import com.unimend.nothingledger.entry.EntryMode
import com.unimend.nothingledger.entry.EntryReducer
import com.unimend.nothingledger.entry.EntryState
import com.unimend.nothingledger.model.Account
import com.unimend.nothingledger.model.AccountBalance
import com.unimend.nothingledger.model.AccountKind
import com.unimend.nothingledger.model.AccountId
import com.unimend.nothingledger.model.Category
import com.unimend.nothingledger.model.CategoryType
import com.unimend.nothingledger.model.CategoryId
import com.unimend.nothingledger.model.Money
import com.unimend.nothingledger.model.TransactionType
import java.time.Clock
import java.time.Instant
import java.util.Currency
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal enum class SaveStatus { INITIALIZING, READY, SAVING, SAVED, ERROR }

internal data class EntryUiState(
    val entry: EntryState,
    val status: SaveStatus,
    val errorMessage: String? = null,
)

internal class EntryViewModel(
    private val database: LedgerDatabaseHandle,
    private val savedStateHandle: SavedStateHandle,
    private val clock: Clock = Clock.systemUTC(),
) : ViewModel() {
    private val accounts = database.accounts()
    private val categories = database.categories()
    private val createTransaction = CreateTransaction(database.transactions(), accounts, categories, clock)
    private val _uiState = MutableStateFlow(
        EntryUiState(restoreEntry(), SaveStatus.INITIALIZING),
    )
    val uiState: StateFlow<EntryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { ensureDefaults() }
                .onSuccess { _uiState.value = _uiState.value.copy(status = SaveStatus.READY) }
                .onFailure { error -> showError(error) }
        }
    }

    fun dispatch(action: EntryAction) {
        if (_uiState.value.status == SaveStatus.SAVING) return
        val transition = EntryReducer.reduce(_uiState.value.entry, action)
        updateEntry(transition.state)
        val submit = transition.effect as? EntryEffect.Submit ?: return
        _uiState.value = _uiState.value.copy(status = SaveStatus.SAVING, errorMessage = null)
        viewModelScope.launch {
            runCatching { createTransaction(submit.value.toDraft()) }
                .onSuccess {
                    val fresh = defaultEntry(clock.instant())
                    savedStateHandle.clearEntry()
                    _uiState.value = EntryUiState(fresh, SaveStatus.SAVED)
                }
                .onFailure(::showError)
        }
    }

    private suspend fun ensureDefaults() {
        if (accounts.findById(LedgerDefaults.walletId) == null) {
            accounts.save(
                Account(LedgerDefaults.walletId, "微信钱包", AccountKind.PAYMENT, AccountBalance(0, LedgerDefaults.currency), 0, null),
            )
        }
        if (categories.findById(LedgerDefaults.expenseCategoryId) == null) {
            categories.save(
                Category(LedgerDefaults.expenseCategoryId, null, "日常支出", CategoryType.EXPENSE, 0, null),
            )
        }
        if (categories.findById(LedgerDefaults.incomeCategoryId) == null) {
            categories.save(
                Category(LedgerDefaults.incomeCategoryId, null, "日常收入", CategoryType.INCOME, 1, null),
            )
        }
    }

    private fun updateEntry(entry: EntryState) {
        savedStateHandle[KEY_MODE] = entry.mode.name
        savedStateHandle[KEY_AMOUNT] = entry.amount.digits
        savedStateHandle[KEY_SOURCE] = entry.sourceAccountId?.value?.toString()
        savedStateHandle[KEY_DESTINATION] = entry.destinationAccountId?.value?.toString()
        savedStateHandle[KEY_CATEGORY] = entry.categoryId?.value?.toString()
        savedStateHandle[KEY_NOTE] = entry.note
        savedStateHandle[KEY_OCCURRED_AT] = entry.occurredAt.toEpochMilli()
        val currentStatus = _uiState.value.status
        _uiState.value = _uiState.value.copy(
            entry = entry,
            status = if (currentStatus == SaveStatus.SAVED || currentStatus == SaveStatus.ERROR) SaveStatus.READY else currentStatus,
            errorMessage = null,
        )
    }

    private fun restoreEntry(): EntryState {
        val fallback = defaultEntry(clock.instant())
        val mode = savedStateHandle.get<String>(KEY_MODE)?.let { runCatching { EntryMode.valueOf(it) }.getOrNull() } ?: fallback.mode
        return fallback.copy(
            mode = mode,
            amount = savedStateHandle.get<String>(KEY_AMOUNT)
                ?.let { runCatching { AmountInput(it) }.getOrNull() } ?: fallback.amount,
            sourceAccountId = if (savedStateHandle.contains(KEY_SOURCE)) {
                savedStateHandle.uuid(KEY_SOURCE)?.let(::AccountId)
            } else fallback.sourceAccountId,
            destinationAccountId = savedStateHandle.uuid(KEY_DESTINATION)?.let(::AccountId),
            categoryId = if (savedStateHandle.contains(KEY_CATEGORY)) {
                savedStateHandle.uuid(KEY_CATEGORY)?.let(::CategoryId)
            } else fallback.categoryId,
            note = savedStateHandle[KEY_NOTE] ?: "",
            occurredAt = savedStateHandle.get<Long>(KEY_OCCURRED_AT)?.let(Instant::ofEpochMilli) ?: fallback.occurredAt,
        )
    }

    private fun showError(error: Throwable) {
        _uiState.value = _uiState.value.copy(
            status = SaveStatus.ERROR,
            errorMessage = error.message?.take(48) ?: error::class.java.simpleName,
        )
    }

    companion object {
        fun factory(database: LedgerDatabaseHandle): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                    require(modelClass.isAssignableFrom(EntryViewModel::class.java))
                    @Suppress("UNCHECKED_CAST")
                    return EntryViewModel(database, extras.createSavedStateHandle()) as T
                }
            }

        private const val KEY_MODE = "entry.mode"
        private const val KEY_AMOUNT = "entry.amount"
        private const val KEY_SOURCE = "entry.source"
        private const val KEY_DESTINATION = "entry.destination"
        private const val KEY_CATEGORY = "entry.category"
        private const val KEY_NOTE = "entry.note"
        private const val KEY_OCCURRED_AT = "entry.occurredAt"
    }
}

private fun defaultEntry(now: Instant): EntryState = EntryState.initial(now, LedgerDefaults.currency).copy(
    sourceAccountId = LedgerDefaults.walletId,
    categoryId = LedgerDefaults.expenseCategoryId,
)

private fun com.unimend.nothingledger.entry.EntrySubmission.toDraft(): TransactionDraft = TransactionDraft(
    type = when (mode) {
        EntryMode.EXPENSE -> TransactionType.EXPENSE
        EntryMode.INCOME -> TransactionType.INCOME
        EntryMode.TRANSFER -> TransactionType.TRANSFER
    },
    money = Money(amountMinorUnits, Currency.getInstance(currencyCode)),
    occurredAt = occurredAt,
    sourceAccountId = sourceAccountId,
    destinationAccountId = destinationAccountId,
    categoryId = categoryId,
    note = note,
)

private fun SavedStateHandle.uuid(key: String): UUID? =
    get<String>(key)?.let { runCatching { UUID.fromString(it) }.getOrNull() }

private fun SavedStateHandle.clearEntry() {
    listOf("entry.mode", "entry.amount", "entry.source", "entry.destination", "entry.category", "entry.note", "entry.occurredAt")
        .forEach { key -> remove<Any>(key) }
}

