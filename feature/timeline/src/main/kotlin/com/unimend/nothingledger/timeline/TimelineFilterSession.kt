package com.unimend.nothingledger.timeline

import com.unimend.nothingledger.model.AccountId
import com.unimend.nothingledger.model.CategoryId
import java.time.YearMonth

/**
 * One predictable source of truth for the timeline search surface. The initial month is retained so
 * "clear filters" and "reset session" can remain distinct operations.
 */
public data class TimelineFilterSession(
    public val initialMonth: YearMonth,
    public val query: TimelineQuery = TimelineQuery(initialMonth),
) {
    public val activeFilterCount: Int
        get() = listOf(
            query.normalizedText.takeIf(String::isNotEmpty),
            query.accountId,
            query.categoryId,
        ).count { it != null }

    public val hasActiveFilters: Boolean get() = activeFilterCount > 0

    public companion object {
        public fun initial(month: YearMonth): TimelineFilterSession = TimelineFilterSession(month)
    }
}

public sealed interface TimelineFilterAction {
    public data class SelectMonth(public val month: YearMonth) : TimelineFilterAction
    public data object PreviousMonth : TimelineFilterAction
    public data object NextMonth : TimelineFilterAction
    public data class ChangeText(public val text: String) : TimelineFilterAction
    public data class SelectAccount(public val id: AccountId) : TimelineFilterAction
    public data class SelectCategory(public val id: CategoryId) : TimelineFilterAction
    public data object ClearText : TimelineFilterAction
    public data object ClearAccount : TimelineFilterAction
    public data object ClearCategory : TimelineFilterAction

    /** Clears text/account/category while deliberately preserving the currently viewed month. */
    public data object ClearFilters : TimelineFilterAction

    /** Returns to the month where the session began and clears every search filter. */
    public data object ResetSession : TimelineFilterAction
}

public object TimelineFilterReducer {
    public fun reduce(
        state: TimelineFilterSession,
        action: TimelineFilterAction,
    ): TimelineFilterSession = when (action) {
        is TimelineFilterAction.SelectMonth -> state.withQuery(state.query.copy(month = action.month))
        TimelineFilterAction.PreviousMonth -> state.withQuery(state.query.copy(month = state.query.month.minusMonths(1)))
        TimelineFilterAction.NextMonth -> state.withQuery(state.query.copy(month = state.query.month.plusMonths(1)))
        is TimelineFilterAction.ChangeText -> state.withQuery(state.query.copy(text = action.text))
        is TimelineFilterAction.SelectAccount -> state.withQuery(state.query.copy(accountId = action.id))
        is TimelineFilterAction.SelectCategory -> state.withQuery(state.query.copy(categoryId = action.id))
        TimelineFilterAction.ClearText -> state.withQuery(state.query.copy(text = ""))
        TimelineFilterAction.ClearAccount -> state.withQuery(state.query.copy(accountId = null))
        TimelineFilterAction.ClearCategory -> state.withQuery(state.query.copy(categoryId = null))
        TimelineFilterAction.ClearFilters -> state.withQuery(
            state.query.copy(text = "", accountId = null, categoryId = null),
        )
        TimelineFilterAction.ResetSession -> TimelineFilterSession.initial(state.initialMonth)
    }

    private fun TimelineFilterSession.withQuery(next: TimelineQuery): TimelineFilterSession =
        copy(query = next)
}
