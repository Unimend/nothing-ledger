package com.unimend.nothingledger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.unimend.nothingledger.categories.CategoryAction
import com.unimend.nothingledger.categories.CategoryListState
import com.unimend.nothingledger.categories.CategoryManager
import com.unimend.nothingledger.categories.CategoryQuery
import com.unimend.nothingledger.database.LedgerDatabaseHandle
import com.unimend.nothingledger.model.CategoryId
import java.time.Clock
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class CategoriesViewModel(
    database: LedgerDatabaseHandle,
    clock: Clock = Clock.systemUTC(),
) : ViewModel() {
    private val manager = CategoryManager(
        repository = database.categories(),
        clock = clock,
        newId = { CategoryId(UUID.randomUUID()) },
    )
    private val commandMutex = Mutex()
    private val _commandError = MutableStateFlow<String?>(null)

    val state: StateFlow<CategoryListState> = manager.states.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CategoryListState.Loading(CategoryQuery()),
    )
    val commandError: StateFlow<String?> = _commandError.asStateFlow()

    fun dispatch(action: CategoryAction) {
        viewModelScope.launch {
            commandMutex.withLock {
                runCatching { manager.dispatch(action) }
                    .onSuccess { _commandError.value = null }
                    .onFailure { error ->
                        _commandError.value = error.message?.take(64) ?: "Category update failed"
                    }
            }
        }
    }

    companion object {
        fun factory(database: LedgerDatabaseHandle): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                    require(modelClass.isAssignableFrom(CategoriesViewModel::class.java))
                    @Suppress("UNCHECKED_CAST")
                    return CategoriesViewModel(database) as T
                }
            }
    }
}
