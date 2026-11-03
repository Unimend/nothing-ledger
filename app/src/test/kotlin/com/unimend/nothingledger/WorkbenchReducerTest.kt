package com.unimend.nothingledger

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class WorkbenchReducerTest {
    @Test
    fun `developer shell has one reducer between workbench and product preview`() {
        val preview = RootNavigationReducer.reduce(
            RootNavigationState(),
            RootNavigationAction.OpenProductPreview,
        )
        val workbench = RootNavigationReducer.reduce(
            preview,
            RootNavigationAction.CloseProductPreview,
        )

        assertEquals(RootSurface.PRODUCT_PREVIEW, preview.surface)
        assertEquals(RootNavigationState(), workbench)
    }

    @Test
    fun `focus has one owner and close always returns to the long catalog`() {
        val focused = WorkbenchReducer.reduce(
            WorkbenchState(),
            WorkbenchAction.Focus(WorkbenchSection.PAGE_FRAMES),
        )
        val closed = WorkbenchReducer.reduce(focused, WorkbenchAction.CloseFocus)

        assertEquals(WorkbenchSection.PAGE_FRAMES, focused.focusedSection)
        assertNull(closed.focusedSection)
    }
}
