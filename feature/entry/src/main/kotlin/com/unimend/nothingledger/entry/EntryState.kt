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
        require(digits.matches(INPUT_PATTERN)) { "Amount must be a decimal number with at most two fraction digits" }
        require(digits.substringBefore('.').length <= MAX_WHOLE_DIGITS) { "Amount is too large" }
    }

    public val minorUnits: Long
        get() {
            if (digits.isEmpty()) return 0
            val whole = digits.substringBefore('.').ifEmpty { "0" }.toLong()
            val fraction = digits.substringAfter('.', "").padEnd(2, '0').take(2).ifEmpty { "0" }.toLong()
            return Math.addExact(Math.multiplyExact(whole, 100L), fraction)
        }

    public val displayText: String get() = digits.ifEmpty { "0" }

    public fun append(digit: Int): AmountInput {
        require(digit in 0..9) { "Digit must be between 0 and 9" }
        if ('.' in digits) {
            val fractionLength = digits.substringAfter('.').length
            return if (fractionLength >= 2) this else AmountInput(digits + digit)
        }
        val next = when (digits) {
            "", "0" -> digit.toString()
            else -> digits + digit
        }
        return if (next.length > MAX_WHOLE_DIGITS) this else AmountInput(next)
    }

    public fun appendDecimalSeparator(): AmountInput = when {
        '.' in digits -> this
        digits.isEmpty() -> AmountInput("0.")
        else -> AmountInput("$digits.")
    }

    public fun backspace(): AmountInput = AmountInput(digits.dropLast(1))

    public companion object {
        public const val MAX_WHOLE_DIGITS: Int = 10
        private val INPUT_PATTERN: Regex = Regex("(?:\\d+)?(?:\\.\\d{0,2})?")
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

