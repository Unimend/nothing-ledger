package com.unimend.nothingledger.domain

import com.unimend.nothingledger.model.Account
import com.unimend.nothingledger.model.AccountBalance
import com.unimend.nothingledger.model.AccountId
import com.unimend.nothingledger.model.AccountKind
import com.unimend.nothingledger.model.CategoryId
import com.unimend.nothingledger.model.Money
import com.unimend.nothingledger.model.TransactionType
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.Currency
import java.util.UUID

class AccountTransactionPolicyTest {
    private val id = AccountId(UUID.randomUUID())
    private val active = Account(id, "Active", AccountKind.BANK, AccountBalance(0, Currency.getInstance("CNY")), 0, null)
    private val draft = TransactionDraft(
        TransactionType.EXPENSE,
        Money(100, Currency.getInstance("CNY")),
        Instant.EPOCH,
        sourceAccountId = id,
        categoryId = CategoryId(UUID.randomUUID()),
    )

    @Test fun `active account can be used for new entry`() {
        assertDoesNotThrow { AccountTransactionPolicy().validate(draft, mapOf(id to active)) }
    }

    @Test fun `archived account is retained but rejected for new entry`() {
        assertThrows(IllegalArgumentException::class.java) {
            AccountTransactionPolicy().validate(draft, mapOf(id to active.archive(Instant.EPOCH)))
        }
    }
}

