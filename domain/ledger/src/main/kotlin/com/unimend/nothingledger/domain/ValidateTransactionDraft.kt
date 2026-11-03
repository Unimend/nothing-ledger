package com.unimend.nothingledger.domain

import com.unimend.nothingledger.model.TransactionType

/** Shared rule boundary for both transaction creation and later edits. */
public class ValidateTransactionDraft(
    private val accounts: AccountRepository,
    private val categories: CategoryRepository,
) {
    public suspend operator fun invoke(draft: TransactionDraft) {
        val participatingAccounts = listOfNotNull(draft.sourceAccountId, draft.destinationAccountId)
            .distinct()
            .associateWith { id -> requireNotNull(accounts.findById(id)) { "Account does not exist" } }
        AccountTransactionPolicy().validate(draft, participatingAccounts)
        when (draft.type) {
            TransactionType.TRANSFER -> require(draft.categoryId == null) { "Transfer must not have a category" }
            else -> RequireUsableCategory(categories)(
                requireNotNull(draft.categoryId) { "Transaction requires a category" },
                draft.type,
            )
        }
    }
}
