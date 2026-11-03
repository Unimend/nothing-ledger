package com.unimend.nothingledger.entry

import com.unimend.nothingledger.model.AccountId
import com.unimend.nothingledger.model.CategoryId
import java.time.Instant
import java.util.Currency

public enum class EntryMode { EXPENSE, INCOME, TRANSFER }

/**
 * Keypad amount represented as minor-unit digits. "1234" means 12.34 for CNY.
 * No floating-point value exists anywhere in the input session.
 */
public data class AmountInput(
    public val digits: String = "",
) {
    init {
        require(digits.all(Char::isDigit)) { "Amount input accepts digits only" }
        require(digits.length <= MAX_DIGITS) { "Amount is too large" }
    }

    public val minorUnits: Long
        get() = digits.ifEmpty { "0" }.toLong()

    public fun append(digit: Int): AmountInput {
        require(digit in 0..9) { "Digit must be between 0 and 9" }
        val next = (digits + digit).trimStart('0').takeIf(String::isNotEmpty) ?: "0"
        return if (next.length > MAX_DIGITS) this else AmountInput(next)
    }

    public fun backspace(): AmountInput = AmountInput(digits.dropLast(1))

    public companion object {
        public const val MAX_DIGITS: Int = 12
    }
}

public enum class EntryValidationError {
    AMOUNT_REQUIRED,
    SOURCE_ACCOUNT_REQUIRED,
    DESTINATION_ACCOUNT_REQUIRED,
    CATEGORY_REQUIRED,
    SAME_TRANSFER_ACCOUNT,
    NOTE_TOO_LONG,
}

public data class EntrySnapshot(
    public val mode: EntryMode,
    public val amount: AmountInput,
    public val sourceAccountId: AccountId?,
    public val destinationAccountId: AccountId?,
    public val categoryId: CategoryId?,
    public val note: String,
    public val occurredAt: Instant,
)

public data class EntryState(
    public val mode: EntryMode,
    public val amount: AmountInput,
    public val sourceAccountId: AccountId?,
    public val destinationAccountId: AccountId?,
    public val categoryId: CategoryId?,
    public val note: String,
    public val occurredAt: Instant,
    public val currency: Currency,
    public val validationErrors: Set<EntryValidationError> = emptySet(),
    public val recoverableDraft: EntrySnapshot? = null,
) {
    public fun snapshot(): EntrySnapshot = EntrySnapshot(
        mode, amount, sourceAccountId, destinationAccountId, categoryId, note, occurredAt,
    )

    public companion object {
        public fun initial(now: Instant, currency: Currency): EntryState = EntryState(
            mode = EntryMode.EXPENSE,
            amount = AmountInput(),
            sourceAccountId = null,
            destinationAccountId = null,
            categoryId = null,
            note = "",
            occurredAt = now,
            currency = currency,
        )
    }
}

