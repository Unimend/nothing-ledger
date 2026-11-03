package com.unimend.nothingledger.model

import java.time.Instant
import java.util.Currency

public enum class AccountKind {
    CASH,
    BANK,
    PAYMENT,
    CREDIT,
    INVESTMENT,
    OTHER,
}

/**
 * A signed account balance. Unlike [Money], a balance may be negative for
 * credit, overdraft, or deliberately adjusted accounts.
 */
public data class AccountBalance(
    public val minorUnits: Long,
    public val currency: Currency,
) {
    public operator fun plus(money: Money): AccountBalance {
        require(currency == money.currency) { "Cannot combine different currencies" }
        return copy(minorUnits = Math.addExact(minorUnits, money.minorUnits))
    }

    public operator fun minus(money: Money): AccountBalance {
        require(currency == money.currency) { "Cannot combine different currencies" }
        return copy(minorUnits = Math.subtractExact(minorUnits, money.minorUnits))
    }
}

public data class Account(
    public val id: AccountId,
    public val name: String,
    public val kind: AccountKind,
    public val openingBalance: AccountBalance,
    public val displayOrder: Int,
    public val archivedAt: Instant?,
) {
    init {
        require(name.isNotBlank()) { "Account name must not be blank" }
        require(name == name.trim()) { "Account name must be normalized" }
        require(name.length <= MAX_NAME_LENGTH) { "Account name is too long" }
        require(displayOrder >= 0) { "Display order must not be negative" }
    }

    public val isArchived: Boolean get() = archivedAt != null

    public fun archive(at: Instant): Account {
        require(!isArchived) { "Account is already archived" }
        return copy(archivedAt = at)
    }

    public fun restore(): Account {
        require(isArchived) { "Account is already active" }
        return copy(archivedAt = null)
    }

    public companion object {
        public const val MAX_NAME_LENGTH: Int = 80
    }
}

