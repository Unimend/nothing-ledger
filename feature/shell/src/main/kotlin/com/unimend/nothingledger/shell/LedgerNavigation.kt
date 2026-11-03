package com.unimend.nothingledger.shell

/** Stable top-level destinations. UI labels and Android routes must not be used as state. */
public enum class MainDestination {
    TIMELINE,
    CALENDAR,
    STATISTICS,
    ACCOUNTS,
}

/** Opaque entity key. The navigation layer never imports database or domain model types. */
public data class NavigationKey(public val value: String) {
    init {
        require(value.isNotBlank())
    }
}

/** Full-screen pages placed above the selected main page. */
public sealed interface PageDestination {
    public data object QuickEntry : PageDestination
    public data class TransactionDetail(public val transactionId: NavigationKey) : PageDestination
    public data class TransactionEdit(public val transactionId: NavigationKey) : PageDestination
    public data class DayTransactions(public val epochDay: Long) : PageDestination
    public data object Search : PageDestination
    public data class AccountDetail(public val accountId: NavigationKey) : PageDestination
    public data object CategoryManagement : PageDestination
    public data object Settings : PageDestination
    public data object DataManagement : PageDestination
}

/** Temporary choices and confirmations that sit above a full-screen page. */
public sealed interface ModalDestination {
    public data object AccountPicker : ModalDestination
    public data object CategoryPicker : ModalDestination
    public data object DateTimePicker : ModalDestination
    public data class DeleteTransactionConfirmation(
        public val transactionId: NavigationKey,
    ) : ModalDestination
}

/**
 * Complete user-visible navigation state.
 *
 * Main pages remain in the pager. Secondary pages use a real stack, and temporary pickers use a
 * single modal slot. No screen may create its own nullable "selected item" navigation state.
 */
public data class LedgerNavigationState(
    public val selectedMain: MainDestination = MainDestination.TIMELINE,
    public val pageStack: List<PageDestination> = emptyList(),
    public val modal: ModalDestination? = null,
) {
    public val visiblePage: PageDestination?
        get() = pageStack.lastOrNull()
}

public sealed interface LedgerNavigationAction {
    public data class SelectMain(public val destination: MainDestination) : LedgerNavigationAction
    public data class PushPage(public val destination: PageDestination) : LedgerNavigationAction
    public data class ReplacePage(public val destination: PageDestination) : LedgerNavigationAction
    public data object PopPage : LedgerNavigationAction
    public data class ShowModal(public val destination: ModalDestination) : LedgerNavigationAction
    public data object CloseModal : LedgerNavigationAction
    public data object NavigateBack : LedgerNavigationAction
}

public sealed interface LedgerNavigationEffect {
    public data object ExitRequested : LedgerNavigationEffect
}

public data class LedgerNavigationTransition(
    public val state: LedgerNavigationState,
    public val effect: LedgerNavigationEffect? = null,
)

/** Pure navigation policy. Rendering and ViewModel creation are consumers of this state. */
public object LedgerNavigationReducer {
    public fun reduce(
        state: LedgerNavigationState,
        action: LedgerNavigationAction,
    ): LedgerNavigationTransition = when (action) {
        is LedgerNavigationAction.SelectMain -> LedgerNavigationTransition(
            state.copy(selectedMain = action.destination, pageStack = emptyList(), modal = null),
        )
        is LedgerNavigationAction.PushPage -> LedgerNavigationTransition(
            state.copy(pageStack = state.pageStack + action.destination, modal = null),
        )
        is LedgerNavigationAction.ReplacePage -> LedgerNavigationTransition(
            state.copy(pageStack = state.pageStack.dropLast(1) + action.destination, modal = null),
        )
        LedgerNavigationAction.PopPage -> LedgerNavigationTransition(
            state.copy(pageStack = state.pageStack.dropLast(1), modal = null),
        )
        is LedgerNavigationAction.ShowModal -> LedgerNavigationTransition(
            state.copy(modal = action.destination),
        )
        LedgerNavigationAction.CloseModal -> LedgerNavigationTransition(state.copy(modal = null))
        LedgerNavigationAction.NavigateBack -> when {
            state.modal != null -> LedgerNavigationTransition(state.copy(modal = null))
            state.pageStack.isNotEmpty() -> LedgerNavigationTransition(
                state.copy(pageStack = state.pageStack.dropLast(1)),
            )
            else -> LedgerNavigationTransition(state, LedgerNavigationEffect.ExitRequested)
        }
    }
}
