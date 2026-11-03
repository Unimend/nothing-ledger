package com.unimend.nothingledger.entry

import com.unimend.nothingledger.model.AccountId
import com.unimend.nothingledger.model.CategoryId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.Currency
import java.util.UUID

class EntryReducerTest {
    private val now = Instant.parse("2026-11-03T08:00:00Z")
    private val cny = Currency.getInstance("CNY")
    private val account = AccountId(UUID.randomUUID())
    private val category = CategoryId(UUID.randomUUID())

    @Test fun `keypad enters whole yuan by default without floating point`() {
        var state = EntryState.initial(now, cny)
        listOf(1, 2, 3).forEach { state = EntryReducer.reduce(state, EntryAction.EnterDigit(it)).state }
        assertEquals("123", state.amount.digits)
        assertEquals(12_300L, state.amount.minorUnits)
        state = EntryReducer.reduce(state, EntryAction.BackspaceAmount).state
        assertEquals(1_200L, state.amount.minorUnits)
    }

    @Test fun `decimal separator accepts at most two fraction digits`() {
        var state = EntryState.initial(now, cny)
        state = EntryReducer.reduce(state, EntryAction.EnterDigit(1)).state
        state = EntryReducer.reduce(state, EntryAction.EnterDecimalSeparator).state
        listOf(2, 3, 4).forEach { state = EntryReducer.reduce(state, EntryAction.EnterDigit(it)).state }
        assertEquals("1.23", state.amount.digits)
        assertEquals(123L, state.amount.minorUnits)
    }

    @Test fun `amount is capped instead of overflowing`() {
        var state = EntryState.initial(now, cny)
        repeat(AmountInput.MAX_WHOLE_DIGITS + 4) {
            state = EntryReducer.reduce(state, EntryAction.EnterDigit(9)).state
        }
        assertEquals(AmountInput.MAX_WHOLE_DIGITS, state.amount.digits.length)
    }

    @Test fun `expense requires amount source and category`() {
        val result = EntryReducer.reduce(EntryState.initial(now, cny), EntryAction.RequestSave)
        assertEquals(
            setOf(
                EntryValidationError.AMOUNT_REQUIRED,
                EntryValidationError.SOURCE_ACCOUNT_REQUIRED,
                EntryValidationError.CATEGORY_REQUIRED,
            ),
            result.state.validationErrors,
        )
        assertNull(result.effect)
    }

    @Test fun `valid expense emits immutable submission`() {
        val state = EntryState.initial(now, cny).copy(
            amount = AmountInput("12.50"),
            sourceAccountId = account,
            categoryId = category,
            note = "  lunch  ",
        )
        val result = EntryReducer.reduce(state, EntryAction.RequestSave)
        val effect = assertInstanceOf(EntryEffect.Submit::class.java, result.effect)
        assertEquals(1250L, effect.value.amountMinorUnits)
        assertEquals("lunch", effect.value.note)
        assertEquals("CNY", effect.value.currencyCode)
    }

    @Test fun `transfer clears category and rejects same account`() {
        val state = EntryReducer.reduce(
            EntryState.initial(now, cny).copy(categoryId = category),
            EntryAction.ChangeMode(EntryMode.TRANSFER),
        ).state.copy(
            amount = AmountInput("1"),
            sourceAccountId = account,
            destinationAccountId = account,
        )
        assertNull(state.categoryId)
        val result = EntryReducer.reduce(state, EntryAction.RequestSave)
        assertTrue(EntryValidationError.SAME_TRANSFER_ACCOUNT in result.state.validationErrors)
    }

    @Test fun `clear stores one recoverable draft and restore is exact`() {
        val original = EntryState.initial(now, cny).copy(
            amount = AmountInput("99.88"),
            sourceAccountId = account,
            categoryId = category,
            note = "Dinner",
        )
        val cleared = EntryReducer.reduce(original, EntryAction.ClearDraft).state
        assertEquals(0L, cleared.amount.minorUnits)
        val restored = EntryReducer.reduce(cleared, EntryAction.RestoreDraft).state
        assertEquals(original.snapshot(), restored.snapshot())
        assertNull(restored.recoverableDraft)
    }

    @Test fun `editing after validation clears stale errors`() {
        val invalid = EntryReducer.reduce(EntryState.initial(now, cny), EntryAction.RequestSave).state
        assertTrue(invalid.validationErrors.isNotEmpty())
        val edited = EntryReducer.reduce(invalid, EntryAction.EnterDigit(1)).state
        assertTrue(edited.validationErrors.isEmpty())
    }
}

