package com.unimend.nothingledger.accounts

import com.unimend.nothingledger.domain.AccountBalanceCalculator
import com.unimend.nothingledger.domain.AccountRepository
import com.unimend.nothingledger.domain.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart

public class ObserveAccounts(
    accounts: AccountRepository,
    transactions: TransactionRepository,
    private val balanceCalculator: AccountBalanceCalculator = AccountBalanceCalculator(),
) {
    private val accountFlow = accounts.observeAll()
    private val transactionFlow = transactions.observeActive()

    public operator fun invoke(): Flow<AccountsState> =
        combine(accountFlow, transactionFlow) { accountList, transactionList ->
            val items = accountList.map { account ->
                AccountListItem(account, balanceCalculator.calculate(account, transactionList))
            }
            val order = compareBy<AccountListItem> { it.account.displayOrder }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.account.name }
                .thenBy { it.account.id.value.toString() }
            val content: AccountsState = AccountsState.Content(
                active = items.filterNot { it.account.isArchived }.sortedWith(order),
                archived = items.filter { it.account.isArchived }.sortedWith(order),
            )
            content
        }.onStart { emit(AccountsState.Loading) }
            .catch { error -> emit(AccountsState.Failure(error.message ?: "Accounts unavailable")) }
}
