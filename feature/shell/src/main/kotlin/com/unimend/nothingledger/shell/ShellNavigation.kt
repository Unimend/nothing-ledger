package com.unimend.nothingledger.shell

/** Stable top-level destinations. UI labels and Android routes must not be used as state. */
public enum class ShellDestination {
    QUICK_ENTRY,
    TIMELINE,
    MANAGE,
}

public data class ShellState(
    val destination: ShellDestination = ShellDestination.QUICK_ENTRY,
)

public sealed interface ShellAction {
    public data class SelectDestination(val destination: ShellDestination) : ShellAction
    public data object NavigateBack : ShellAction
}

public sealed interface ShellEffect {
    /** The shell is already at its root; the Android host may finish or defer to the system. */
    public data object ExitRequested : ShellEffect
}

public data class ShellTransition(
    val state: ShellState,
    val effect: ShellEffect? = null,
)

/** Pure navigation policy shared by every shell surface. */
public object ShellReducer {
    public fun reduce(state: ShellState, action: ShellAction): ShellTransition = when (action) {
        is ShellAction.SelectDestination -> ShellTransition(
            state = state.copy(destination = action.destination),
        )
        ShellAction.NavigateBack -> when (state.destination) {
            ShellDestination.QUICK_ENTRY -> ShellTransition(state, ShellEffect.ExitRequested)
            ShellDestination.TIMELINE,
            ShellDestination.MANAGE,
            -> ShellTransition(state.copy(destination = ShellDestination.QUICK_ENTRY))
        }
    }
}
