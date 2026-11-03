package com.unimend.nothingledger.shell

/** Stable top-level destinations. UI labels and Android routes must not be used as state. */
public enum class MainDestination {
    TIMELINE,
    CALENDAR,
    STATISTICS,
    ACCOUNTS,
}

/** Full-screen secondary destinations presented above the selected main destination. */
public enum class OverlayDestination {
    QUICK_ENTRY,
    CATEGORIES,
}

public data class ShellState(
    public val selectedMain: MainDestination = MainDestination.TIMELINE,
    public val overlay: OverlayDestination? = null,
)

public sealed interface ShellAction {
    public data class SelectMain(public val destination: MainDestination) : ShellAction
    public data class OpenOverlay(public val destination: OverlayDestination) : ShellAction
    public data object CloseOverlay : ShellAction
    public data object NavigateBack : ShellAction
}

public sealed interface ShellEffect {
    public data object ExitRequested : ShellEffect
}

public data class ShellTransition(
    public val state: ShellState,
    public val effect: ShellEffect? = null,
)

/** Pure navigation policy shared by every shell surface. */
public object ShellReducer {
    public fun reduce(state: ShellState, action: ShellAction): ShellTransition = when (action) {
        is ShellAction.SelectMain -> ShellTransition(
            state = state.copy(selectedMain = action.destination, overlay = null),
        )
        is ShellAction.OpenOverlay -> ShellTransition(state.copy(overlay = action.destination))
        ShellAction.CloseOverlay -> ShellTransition(state.copy(overlay = null))
        ShellAction.NavigateBack -> if (state.overlay != null) {
            ShellTransition(state.copy(overlay = null))
        } else {
            ShellTransition(state, ShellEffect.ExitRequested)
        }
    }
}
