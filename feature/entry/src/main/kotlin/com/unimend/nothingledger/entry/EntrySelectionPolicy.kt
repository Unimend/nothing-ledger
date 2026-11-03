package com.unimend.nothingledger.entry

import com.unimend.nothingledger.model.AccountId
import com.unimend.nothingledger.model.CategoryId

/** Active, ordered choices supplied by the account/category systems. */
public data class EntrySelectionOptions(
    public val accountIds: List<AccountId> = emptyList(),
    public val expenseCategoryIds: List<CategoryId> = emptyList(),
    public val incomeCategoryIds: List<CategoryId> = emptyList(),
) {
    init {
        require(accountIds.distinct().size == accountIds.size) { "Account options must be unique" }
        require(expenseCategoryIds.distinct().size == expenseCategoryIds.size) { "Expense category options must be unique" }
        require(incomeCategoryIds.distinct().size == incomeCategoryIds.size) { "Income category options must be unique" }
    }
}

/**
 * Owns mode-specific selection rules so Compose never performs a sequence of corrective dispatches.
 * Existing valid choices are retained; unavailable or incompatible choices are replaced predictably.
 */
public object EntrySelectionPolicy {
    public fun apply(state: EntryState, options: EntrySelectionOptions): EntryState = when (state.mode) {
        EntryMode.EXPENSE -> state.copy(
            sourceAccountId = state.sourceAccountId.validIn(options.accountIds) ?: options.accountIds.firstOrNull(),
            destinationAccountId = null,
            categoryId = state.categoryId.validIn(options.expenseCategoryIds)
                ?: options.expenseCategoryIds.firstOrNull(),
        )
        EntryMode.INCOME -> state.copy(
            sourceAccountId = null,
            destinationAccountId = state.destinationAccountId.validIn(options.accountIds)
                ?: options.accountIds.firstOrNull(),
            categoryId = state.categoryId.validIn(options.incomeCategoryIds)
                ?: options.incomeCategoryIds.firstOrNull(),
        )
        EntryMode.TRANSFER -> {
            val source = state.sourceAccountId.validIn(options.accountIds) ?: options.accountIds.firstOrNull()
            state.copy(
                sourceAccountId = source,
                destinationAccountId = state.destinationAccountId
                    .validIn(options.accountIds)
                    ?.takeUnless { it == source }
                    ?: options.accountIds.firstOrNull { it != source },
                categoryId = null,
            )
        }
    }

    private fun <T> T?.validIn(values: List<T>): T? = this?.takeIf(values::contains)
}
