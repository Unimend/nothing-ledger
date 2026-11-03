package com.unimend.nothingledger

import com.unimend.nothingledger.model.AccountId
import com.unimend.nothingledger.model.CategoryId
import java.util.Currency
import java.util.UUID

internal object LedgerDefaults {
    val walletId: AccountId = AccountId(UUID.fromString("10000000-0000-0000-0000-000000000001"))
    val expenseCategoryId: CategoryId = CategoryId(UUID.fromString("20000000-0000-0000-0000-000000000001"))
    val incomeCategoryId: CategoryId = CategoryId(UUID.fromString("20000000-0000-0000-0000-000000000002"))
    val currency: Currency = Currency.getInstance("CNY")
}

