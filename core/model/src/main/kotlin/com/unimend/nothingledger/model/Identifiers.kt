package com.unimend.nothingledger.model

import java.util.UUID

@JvmInline public value class TransactionId(public val value: UUID)
@JvmInline public value class AccountId(public val value: UUID)
@JvmInline public value class CategoryId(public val value: UUID)

