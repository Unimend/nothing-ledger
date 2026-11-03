package com.unimend.nothingledger.entry

import com.unimend.nothingledger.model.AccountId
import com.unimend.nothingledger.model.CategoryId
import java.time.Instant
import java.util.Currency
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class EntrySelectionPolicyTest {
    private val firstAccount = AccountId(UUID(0, 1))
    private val secondAccount = AccountId(UUID(0, 2))
    private val expenseCategory = CategoryId(UUID(0, 3))
    private val incomeCategory = CategoryId(UUID(0, 4))
    private val options = EntrySelectionOptions(
        accountIds = listOf(firstAccount, secondAccount),
        expenseCategoryIds = listOf(expenseCategory),
        incomeCategoryIds = listOf(incomeCategory),
    )
    private val initial = EntryState.initial(Instant.EPOCH, Currency.getInstance("CNY"))

    @Test fun `expense receives active account and expense category defaults`() {
        val result = EntrySelectionPolicy.apply(initial, options)

        assertEquals(firstAccount, result.sourceAccountId)
        assertEquals(expenseCategory, result.categoryId)
        assertNull(result.destinationAccountId)
    }

    @Test fun `income replaces incompatible selections in one policy step`() {
        val result = EntrySelectionPolicy.apply(
            initial.copy(
                mode = EntryMode.INCOME,
                sourceAccountId = firstAccount,
                categoryId = expenseCategory,
            ),
            options,
        )

        assertNull(result.sourceAccountId)
        assertEquals(firstAccount, result.destinationAccountId)
        assertEquals(incomeCategory, result.categoryId)
    }

    @Test fun `transfer chooses two different accounts and removes category`() {
        val result = EntrySelectionPolicy.apply(
            initial.copy(mode = EntryMode.TRANSFER, categoryId = expenseCategory),
            options,
        )

        assertEquals(firstAccount, result.sourceAccountId)
        assertEquals(secondAccount, result.destinationAccountId)
        assertNull(result.categoryId)
    }

    @Test fun `valid user choices survive option refresh`() {
        val result = EntrySelectionPolicy.apply(
            initial.copy(sourceAccountId = secondAccount, categoryId = expenseCategory),
            options,
        )

        assertEquals(secondAccount, result.sourceAccountId)
        assertEquals(expenseCategory, result.categoryId)
    }

    @Test fun `transfer remains incomplete when only one account exists`() {
        val result = EntrySelectionPolicy.apply(
            initial.copy(mode = EntryMode.TRANSFER),
            options.copy(accountIds = listOf(firstAccount)),
        )

        assertEquals(firstAccount, result.sourceAccountId)
        assertNull(result.destinationAccountId)
    }
}
