package com.unimend.nothingledger.domain

import com.unimend.nothingledger.model.Account
import com.unimend.nothingledger.model.AccountBalance
import com.unimend.nothingledger.model.AccountId
import com.unimend.nothingledger.model.AccountKind
import com.unimend.nothingledger.model.CategoryId
import com.unimend.nothingledger.model.Money
import com.unimend.nothingledger.model.Transaction
import com.unimend.nothingledger.model.TransactionId
import com.unimend.nothingledger.model.TransactionType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.Currency
import java.util.UUID

class AccountBalanceCalculatorTest {
    private val cny = Currency.getInstance("CNY")
    private val accountA = account(10_000)
    private val accountB = account(0)
    private val calculator = AccountBalanceCalculator()

    @Test fun `income expense and transfer are projected exactly once`() {
        val transactions = listOf(
            transaction(TransactionType.INCOME, 5_000, destination = accountA.id),
            transaction(TransactionType.EXPENSE, 2_000, source = accountA.id),
            transaction(TransactionType.TRANSFER, 3_000, source = accountA.id, destination = accountB.id),
        )
        assertEquals(10_000, calculator.calculate(accountA, transactions).minorUnits)
        assertEquals(3_000, calculator.calculate(accountB, transactions).minorUnits)
    }

    @Test fun `balance may become negative without domain rejection`() {
        val balance = calculator.calculate(
            accountA.copy(openingBalance = AccountBalance(0, cny)),
            listOf(transaction(TransactionType.EXPENSE, 1_000, source = accountA.id)),
        )
        assertEquals(-1_000, balance.minorUnits)
    }

    @Test fun `transaction in another currency is rejected for involved account`() {
        val usd = Currency.getInstance("USD")
        assertThrows(IllegalArgumentException::class.java) {
            calculator.calculate(
                accountA,
                listOf(transaction(TransactionType.EXPENSE, 1_000, source = accountA.id, currency = usd)),
            )
        }
    }

    private fun account(opening: Long) = Account(
        AccountId(UUID.randomUUID()),
        "Account",
        AccountKind.BANK,
        AccountBalance(opening, cny),
        0,
        null,
    )

    private fun transaction(
        type: TransactionType,
        amount: Long,
        source: AccountId? = null,
        destination: AccountId? = null,
        currency: Currency = cny,
    ) = Transaction(
        TransactionId(UUID.randomUUID()),
        type,
        Money(amount, currency),
        Instant.EPOCH,
        source,
        destination,
        if (type == TransactionType.TRANSFER) null else CategoryId(UUID.randomUUID()),
        null,
        Instant.EPOCH,
        Instant.EPOCH,
    )
}

