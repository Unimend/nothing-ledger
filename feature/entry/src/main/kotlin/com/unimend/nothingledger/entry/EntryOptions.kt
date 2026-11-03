package com.unimend.nothingledger.entry

import com.unimend.nothingledger.domain.AccountRepository
import com.unimend.nothingledger.domain.CategoryRepository
import com.unimend.nothingledger.model.Account
import com.unimend.nothingledger.model.AccountId
import com.unimend.nothingledger.model.Category
import com.unimend.nothingledger.model.CategoryId
import com.unimend.nothingledger.model.CategoryType
import java.util.Currency
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine

public data class EntryOptionsQuery(
    public val mode: EntryMode,
    public val currency: Currency,
    public val selectedSourceAccountId: AccountId? = null,
    public val selectedDestinationAccountId: AccountId? = null,
)

public data class EntryOptions(
    public val sourceAccounts: List<Account>,
    public val destinationAccounts: List<Account>,
    public val categories: List<Category>,
) {
    public companion object {
        public val Empty: EntryOptions = EntryOptions(emptyList(), emptyList(), emptyList())
    }
}

public sealed interface EntryOptionsState {
    public data object Loading : EntryOptionsState
    public data class Ready(public val options: EntryOptions) : EntryOptionsState
    public data class Failure(public val message: String) : EntryOptionsState
}

/** Shared projection used by both selection correction and the visible selector list. */
public fun buildEntrySelectionOptions(
    accounts: List<Account>,
    categories: List<Category>,
    currency: Currency,
): EntrySelectionOptions {
    val usableAccounts = accounts.usable(currency)
    val usableCategories = categories.usable()
    return EntrySelectionOptions(
        accountIds = usableAccounts.map(Account::id),
        expenseCategoryIds = usableCategories.filter { it.type == CategoryType.EXPENSE }.map(Category::id),
        incomeCategoryIds = usableCategories.filter { it.type == CategoryType.INCOME }.map(Category::id),
    )
}

/**
 * Projects repository data into legal choices for one entry session. It owns no
 * defaults and never changes the user's current selection.
 */
public class ObserveEntryOptions(
    private val accounts: AccountRepository,
    private val categories: CategoryRepository,
) {
    public operator fun invoke(query: EntryOptionsQuery): Flow<EntryOptionsState> =
        combine(accounts.observeAll(), categories.observeAll()) { accountList, categoryList ->
            val usableAccounts = accountList.usable(query.currency)
            val usableCategories = categoryList.usable()
            val options = when (query.mode) {
                EntryMode.EXPENSE -> EntryOptions(
                    sourceAccounts = usableAccounts,
                    destinationAccounts = emptyList(),
                    categories = usableCategories.filter { it.type == CategoryType.EXPENSE },
                )
                EntryMode.INCOME -> EntryOptions(
                    sourceAccounts = emptyList(),
                    destinationAccounts = usableAccounts,
                    categories = usableCategories.filter { it.type == CategoryType.INCOME },
                )
                EntryMode.TRANSFER -> EntryOptions(
                    sourceAccounts = usableAccounts.filter { it.id != query.selectedDestinationAccountId },
                    destinationAccounts = usableAccounts.filter { it.id != query.selectedSourceAccountId },
                    categories = emptyList(),
                )
            }
            val state: EntryOptionsState = EntryOptionsState.Ready(options)
            state
        }.catch { error -> emit(EntryOptionsState.Failure(error.message ?: "Entry options unavailable")) }

}

private fun List<Account>.usable(currency: Currency): List<Account> = asSequence()
    .filterNot(Account::isArchived)
    .filter { it.openingBalance.currency == currency }
    .sortedWith(accountOrder)
    .toList()

private fun List<Category>.usable(): List<Category> = asSequence()
    .filterNot(Category::isArchived)
    .sortedWith(categoryOrder)
    .toList()

private val accountOrder: Comparator<Account> = compareBy<Account> { it.displayOrder }
    .thenBy(String.CASE_INSENSITIVE_ORDER, Account::name)
    .thenBy { it.id.value.toString() }

private val categoryOrder: Comparator<Category> = compareBy<Category> { it.displayOrder }
    .thenBy(String.CASE_INSENSITIVE_ORDER, Category::name)
    .thenBy { it.id.value.toString() }
