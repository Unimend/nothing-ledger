package com.unimend.nothingledger.domain

import com.unimend.nothingledger.model.AccountId
import com.unimend.nothingledger.model.CategoryId
import com.unimend.nothingledger.model.Money
import com.unimend.nothingledger.model.TransactionType
import java.time.Instant

public data class TransactionDraft(
    public val type: TransactionType,
    public val money: Money,
    public val occurredAt: Instant,
    public val sourceAccountId: AccountId? = null,
    public val destinationAccountId: AccountId? = null,
    public val categoryId: CategoryId? = null,
    public val note: String? = null,
)

