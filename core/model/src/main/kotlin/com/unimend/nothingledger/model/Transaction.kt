package com.unimend.nothingledger.model

import java.time.Instant

public enum class TransactionType { EXPENSE, INCOME, TRANSFER }

public data class Transaction(
    public val id: TransactionId,
    public val type: TransactionType,
    public val money: Money,
    public val occurredAt: Instant,
    public val sourceAccountId: AccountId?,
    public val destinationAccountId: AccountId?,
    public val categoryId: CategoryId?,
    public val note: String?,
    public val createdAt: Instant,
    public val updatedAt: Instant,
) {
    init {
        require(note == null || note.length <= MAX_NOTE_LENGTH) { "Note is too long" }
        when (type) {
            TransactionType.EXPENSE -> {
                require(sourceAccountId != null) { "Expense requires a source account" }
                require(destinationAccountId == null) { "Expense cannot have a destination account" }
                require(categoryId != null) { "Expense requires a category" }
            }
            TransactionType.INCOME -> {
                require(sourceAccountId == null) { "Income cannot have a source account" }
                require(destinationAccountId != null) { "Income requires a destination account" }
                require(categoryId != null) { "Income requires a category" }
            }
            TransactionType.TRANSFER -> {
                require(sourceAccountId != null && destinationAccountId != null) {
                    "Transfer requires source and destination accounts"
                }
                require(sourceAccountId != destinationAccountId) { "Transfer accounts must be different" }
                require(categoryId == null) { "Transfer must not affect category statistics" }
            }
        }
    }

    public companion object {
        public const val MAX_NOTE_LENGTH: Int = 500
    }
}

