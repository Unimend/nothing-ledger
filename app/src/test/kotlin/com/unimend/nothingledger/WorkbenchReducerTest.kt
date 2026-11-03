package com.unimend.nothingledger

import com.unimend.nothingledger.workbench.WorkbenchLayoutDraft
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

    @Test
    fun `each layout draft opens full screen and closes back to its catalog`() {
        val opened = WorkbenchReducer.reduce(
            WorkbenchState(),
            WorkbenchAction.OpenDraft(WorkbenchLayoutDraft.CALENDAR),
        )
        val closed = WorkbenchReducer.reduce(opened, WorkbenchAction.CloseDraft)

        assertEquals(WorkbenchSection.PAGE_FRAMES, opened.focusedSection)
        assertEquals(WorkbenchLayoutDraft.CALENDAR, opened.focusedDraft)
        assertEquals(WorkbenchSection.PAGE_FRAMES, closed.focusedSection)
        assertNull(closed.focusedDraft)
    }
}
