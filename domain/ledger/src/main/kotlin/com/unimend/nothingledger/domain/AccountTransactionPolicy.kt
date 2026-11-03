package com.unimend.nothingledger.domain

import com.unimend.nothingledger.model.Account
import com.unimend.nothingledger.model.AccountId
import com.unimend.nothingledger.model.TransactionType

/**
 * Central rule for whether accounts may participate in a newly created entry.
 * Historical transactions remain valid after an account is archived.
 */
public class AccountTransactionPolicy {
    public fun validate(draft: TransactionDraft, accounts: Map<AccountId, Account>) {
        when (draft.type) {
            TransactionType.EXPENSE -> requireActive(draft.sourceAccountId, accounts, "source")
            TransactionType.INCOME -> requireActive(draft.destinationAccountId, accounts, "destination")
            TransactionType.TRANSFER -> {
                requireActive(draft.sourceAccountId, accounts, "source")
                requireActive(draft.destinationAccountId, accounts, "destination")
            }
        }
        val used = listOfNotNull(
            draft.sourceAccountId?.let(accounts::get),
            draft.destinationAccountId?.let(accounts::get),
        )
        require(used.all { it.openingBalance.currency == draft.money.currency }) {
            "Transaction currency must match every participating account"
        }
        if (draft.type == TransactionType.TRANSFER) {
            require(used.map { it.openingBalance.currency }.distinct().size == 1) {
                "Cross-currency transfer is not supported"
            }
        }
    }

    private fun requireActive(
        id: AccountId?,
        accounts: Map<AccountId, Account>,
        role: String,
    ) {
        requireNotNull(id) { "Transaction requires a $role account" }
        val account = requireNotNull(accounts[id]) { "Unknown $role account" }
        require(!account.isArchived) { "Archived account cannot be used for a new transaction" }
    }
}

