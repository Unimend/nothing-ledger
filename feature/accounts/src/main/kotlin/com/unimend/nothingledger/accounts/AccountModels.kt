package com.unimend.nothingledger.accounts

import com.unimend.nothingledger.model.Account
import com.unimend.nothingledger.model.AccountBalance
import com.unimend.nothingledger.model.AccountKind
import java.util.Currency

public data class AccountListItem(
    public val account: Account,
    /** Read-only value derived from immutable transactions. */
    public val currentBalance: AccountBalance,
)

public sealed interface AccountsState {
    public data object Loading : AccountsState
    public data class Content(
        public val active: List<AccountListItem>,
        public val archived: List<AccountListItem>,
    ) : AccountsState
    public data class Failure(public val message: String) : AccountsState
}

public data class CreateAccountCommand(
    public val name: String,
    public val kind: AccountKind,
    public val openingBalanceMinor: Long = 0,
    public val currency: Currency,
)
