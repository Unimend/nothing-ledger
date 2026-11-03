package com.unimend.nothingledger.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Currency

public data class Money(
    public val minorUnits: Long,
    public val currency: Currency,
) : Comparable<Money> {
    init {
        require(minorUnits >= 0) { "Money amount must not be negative" }
    }

    public operator fun plus(other: Money): Money {
        require(currency == other.currency) { "Cannot add different currencies" }
        return copy(minorUnits = Math.addExact(minorUnits, other.minorUnits))
    }

    override fun compareTo(other: Money): Int {
        require(currency == other.currency) { "Cannot compare different currencies" }
        return minorUnits.compareTo(other.minorUnits)
    }

    public companion object {
        public fun parseMajorUnits(value: String, currency: Currency): Money {
            val scale = currency.defaultFractionDigits
            require(scale >= 0) { "Currency must have a fixed fraction digit count" }
            val minorUnits = value.trim()
                .toBigDecimal()
                .movePointRight(scale)
                .setScale(0, RoundingMode.UNNECESSARY)
                .longValueExact()
            return Money(minorUnits, currency)
        }

        public fun zero(currency: Currency): Money = Money(0, currency)
    }

    public fun toMajorUnits(): BigDecimal =
        BigDecimal.valueOf(minorUnits, currency.defaultFractionDigits)
}

