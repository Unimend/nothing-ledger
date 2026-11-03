package com.unimend.nothingledger.domain

import com.unimend.nothingledger.model.Account
import com.unimend.nothingledger.model.AccountBalance
import com.unimend.nothingledger.model.Transaction
import com.unimend.nothingledger.model.TransactionType

/** Calculates balance from immutable facts; it never stores or mutates a balance. */
public class AccountBalanceCalculator {
    public fun calculate(account: Account, transactions: Iterable<Transaction>): AccountBalance =
        transactions.fold(account.openingBalance) { balance, transaction ->
            apply(balance, account, transaction)
        }

    private fun apply(
        balance: AccountBalance,
        account: Account,
        transaction: Transaction,
    ): AccountBalance = when (transaction.type) {
        TransactionType.EXPENSE -> {
            if (transaction.sourceAccountId == account.id) balance - transaction.money else balance
        }
        TransactionType.INCOME -> {
            if (transaction.destinationAccountId == account.id) balance + transaction.money else balance
        }
        TransactionType.TRANSFER -> when (account.id) {
            transaction.sourceAccountId -> balance - transaction.money
            transaction.destinationAccountId -> balance + transaction.money
            else -> balance
        }
    }
}

