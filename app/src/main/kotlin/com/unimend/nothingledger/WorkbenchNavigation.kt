package com.unimend.nothingledger

/** Developer-shell navigation. It never enters the product page stack. */
internal enum class RootSurface { WORKBENCH, PRODUCT_PREVIEW }

internal data class RootNavigationState(
    val surface: RootSurface = RootSurface.WORKBENCH,
)

internal sealed interface RootNavigationAction {
    data object OpenProductPreview : RootNavigationAction
    data object CloseProductPreview : RootNavigationAction
}

internal object RootNavigationReducer {
    fun reduce(state: RootNavigationState, action: RootNavigationAction): RootNavigationState = when (action) {
        RootNavigationAction.OpenProductPreview -> state.copy(surface = RootSurface.PRODUCT_PREVIEW)
        RootNavigationAction.CloseProductPreview -> state.copy(surface = RootSurface.WORKBENCH)
    }
}

/** Local navigation inside the database-free component laboratory. */
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
