package com.unimend.nothingledger.shell

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ShellReducerTest {
    @Test
    fun `timeline is the stable default`() {
        assertEquals(MainDestination.TIMELINE, ShellState().selectedMain)
        assertNull(ShellState().overlay)
    }

    @Test
    fun `main destinations have one stable pager and bottom bar order`() {
        assertEquals(
            listOf(
                MainDestination.TIMELINE,
                MainDestination.CALENDAR,
                MainDestination.STATISTICS,
                MainDestination.ACCOUNTS,
            ),
            MainDestination.entries,
        )
    }

    @Test
    fun `selecting a main destination also closes an overlay`() {
        val transition = ShellReducer.reduce(
            ShellState(overlay = OverlayDestination.QUICK_ENTRY),
            ShellAction.SelectMain(MainDestination.CALENDAR),
        )

        assertEquals(MainDestination.CALENDAR, transition.state.selectedMain)
        assertNull(transition.state.overlay)
        assertNull(transition.effect)
    }

    @Test
    fun `pager and bottom bar selection share the same transition for every page`() {
        MainDestination.entries.forEach { destination ->
            val transition = ShellReducer.reduce(
                ShellState(
                    selectedMain = MainDestination.TIMELINE,
                    overlay = OverlayDestination.CATEGORIES,
                ),
                ShellAction.SelectMain(destination),
            )

            assertEquals(destination, transition.state.selectedMain)
            assertNull(transition.state.overlay)
            assertNull(transition.effect)
        }
    }

    @Test
    fun `entry opens above and returns to the same main destination`() {
        val origin = ShellState(selectedMain = MainDestination.CALENDAR)
        val opened = ShellReducer.reduce(origin, ShellAction.OpenOverlay(OverlayDestination.QUICK_ENTRY))
        val closed = ShellReducer.reduce(opened.state, ShellAction.NavigateBack)

        assertEquals(OverlayDestination.QUICK_ENTRY, opened.state.overlay)
        assertEquals(origin, closed.state)
        assertNull(closed.effect)
    }

    @Test
    fun `every overlay closes back to every originating main page`() {
        MainDestination.entries.forEach { origin ->
            OverlayDestination.entries.forEach { overlay ->
                val before = ShellState(selectedMain = origin)
                val opened = ShellReducer.reduce(before, ShellAction.OpenOverlay(overlay))
                val closed = ShellReducer.reduce(opened.state, ShellAction.CloseOverlay)

                assertEquals(origin, opened.state.selectedMain)
                assertEquals(overlay, opened.state.overlay)
                assertEquals(before, closed.state)
                assertNull(closed.effect)
            }
        }
    }

    @Test
    fun `back from a main destination requests host exit`() {
        MainDestination.entries.forEach { destination ->
            val state = ShellState(selectedMain = destination)
            val transition = ShellReducer.reduce(state, ShellAction.NavigateBack)
            assertEquals(state, transition.state)
            assertEquals(ShellEffect.ExitRequested, transition.effect)
        }
    }
}
