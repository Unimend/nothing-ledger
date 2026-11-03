package com.unimend.nothingledger

internal enum class WorkbenchSection {
    DESIGN_TOKENS,
    BASE_COMPONENTS,
    BUSINESS_COMPONENTS,
    PAGE_FRAMES,
    INTERACTION_TESTS,
}

internal data class WorkbenchState(
    val focusedSection: WorkbenchSection? = null,
)

internal sealed interface WorkbenchAction {
    data class Focus(val section: WorkbenchSection) : WorkbenchAction
    data object CloseFocus : WorkbenchAction
}

internal object WorkbenchReducer {
    fun reduce(state: WorkbenchState, action: WorkbenchAction): WorkbenchState = when (action) {
        is WorkbenchAction.Focus -> state.copy(focusedSection = action.section)
        WorkbenchAction.CloseFocus -> state.copy(focusedSection = null)
    }
}
