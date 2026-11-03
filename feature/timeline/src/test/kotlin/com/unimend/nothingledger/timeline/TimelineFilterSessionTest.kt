package com.unimend.nothingledger.timeline

import com.unimend.nothingledger.model.AccountId
import com.unimend.nothingledger.model.CategoryId
import java.time.YearMonth
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TimelineFilterSessionTest {
    private val initialMonth = YearMonth.of(2026, 11)
    private val accountId = AccountId(UUID.randomUUID())
    private val categoryId = CategoryId(UUID.randomUUID())

    @Test fun `each filter action preserves every unrelated field`() {
        val selected = reduce(
            TimelineFilterAction.ChangeText(" coffee "),
            TimelineFilterAction.SelectAccount(accountId),
            TimelineFilterAction.SelectCategory(categoryId),
            TimelineFilterAction.NextMonth,
        )

        assertEquals(YearMonth.of(2026, 12), selected.query.month)
        assertEquals(" coffee ", selected.query.text)
        assertEquals(accountId, selected.query.accountId)
        assertEquals(categoryId, selected.query.categoryId)
        assertEquals(3, selected.activeFilterCount)
    }

    @Test fun `single clear operations never erase sibling filters`() {
        val populated = reduce(
            TimelineFilterAction.ChangeText("coffee"),
            TimelineFilterAction.SelectAccount(accountId),
            TimelineFilterAction.SelectCategory(categoryId),
        )

        val withoutAccount = TimelineFilterReducer.reduce(populated, TimelineFilterAction.ClearAccount)
        assertEquals("coffee", withoutAccount.query.text)
        assertEquals(null, withoutAccount.query.accountId)
        assertEquals(categoryId, withoutAccount.query.categoryId)

        val withoutText = TimelineFilterReducer.reduce(withoutAccount, TimelineFilterAction.ClearText)
        assertEquals("", withoutText.query.text)
        assertEquals(categoryId, withoutText.query.categoryId)
    }

    @Test fun `clear filters preserves currently selected month`() {
        val populated = reduce(
            TimelineFilterAction.SelectMonth(YearMonth.of(2027, 2)),
            TimelineFilterAction.ChangeText("rent"),
            TimelineFilterAction.SelectAccount(accountId),
            TimelineFilterAction.SelectCategory(categoryId),
        )

        val cleared = TimelineFilterReducer.reduce(populated, TimelineFilterAction.ClearFilters)

        assertEquals(YearMonth.of(2027, 2), cleared.query.month)
        assertEquals(TimelineQuery(YearMonth.of(2027, 2)), cleared.query)
        assertFalse(cleared.hasActiveFilters)
    }

    @Test fun `reset session restores initial month and clears all filters`() {
        val changed = reduce(
            TimelineFilterAction.PreviousMonth,
            TimelineFilterAction.ChangeText("salary"),
            TimelineFilterAction.SelectAccount(accountId),
        )

        val reset = TimelineFilterReducer.reduce(changed, TimelineFilterAction.ResetSession)

        assertEquals(TimelineFilterSession.initial(initialMonth), reset)
    }

    @Test fun `returning to current month preserves active filters`() {
        val changed = reduce(
            TimelineFilterAction.PreviousMonth,
            TimelineFilterAction.ChangeText("salary"),
            TimelineFilterAction.SelectAccount(accountId),
        )

        val current = TimelineFilterReducer.reduce(
            changed,
            TimelineFilterAction.SelectMonth(initialMonth),
        )

        assertEquals(initialMonth, current.query.month)
        assertEquals("salary", current.query.text)
        assertEquals(accountId, current.query.accountId)
    }

    @Test fun `blank search is retained for editing but does not count as active`() {
        val state = reduce(TimelineFilterAction.ChangeText("   "))

        assertEquals("   ", state.query.text)
        assertEquals(0, state.activeFilterCount)
        assertFalse(state.hasActiveFilters)

        val selected = TimelineFilterReducer.reduce(state, TimelineFilterAction.SelectAccount(accountId))
        assertTrue(selected.hasActiveFilters)
        assertEquals(1, selected.activeFilterCount)
    }

    private fun reduce(vararg actions: TimelineFilterAction): TimelineFilterSession =
        actions.fold(TimelineFilterSession.initial(initialMonth), TimelineFilterReducer::reduce)
}
