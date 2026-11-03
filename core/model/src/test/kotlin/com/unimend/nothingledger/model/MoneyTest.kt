package com.unimend.nothingledger.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.Currency

class MoneyTest {
    private val cny = Currency.getInstance("CNY")

    @Test fun `decimal input is stored as exact minor units`() {
        assertEquals(1234, Money.parseMajorUnits("12.34", cny).minorUnits)
    }

    @Test fun `fraction beyond currency precision is rejected`() {
        assertThrows(ArithmeticException::class.java) {
            Money.parseMajorUnits("12.345", cny)
        }
    }

    @Test fun `different currencies cannot be combined`() {
        assertThrows(IllegalArgumentException::class.java) {
            Money(100, cny) + Money(100, Currency.getInstance("USD"))
        }
    }
}

