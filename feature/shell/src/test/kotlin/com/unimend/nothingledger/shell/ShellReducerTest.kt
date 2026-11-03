package com.unimend.nothingledger.shell

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ShellReducerTest {
    @Test
    fun `quick entry is the stable default`() {
        assertEquals(ShellDestination.QUICK_ENTRY, ShellState().destination)
    }

    @Test
    fun `selecting a destination changes only navigation state`() {
        val transition = ShellReducer.reduce(
            ShellState(),
            ShellAction.SelectDestination(ShellDestination.TIMELINE),
        )

        assertEquals(ShellDestination.TIMELINE, transition.state.destination)
        assertNull(transition.effect)
    }

    @Test
    fun `back from secondary destination returns to quick entry`() {
        ShellDestination.entries
            .filterNot { it == ShellDestination.QUICK_ENTRY }
            .forEach { destination ->
                val transition = ShellReducer.reduce(ShellState(destination), ShellAction.NavigateBack)
                assertEquals(ShellDestination.QUICK_ENTRY, transition.state.destination)
                assertNull(transition.effect)
            }
    }

    @Test
    fun `back from root requests host exit without changing state`() {
        val state = ShellState()
        val transition = ShellReducer.reduce(state, ShellAction.NavigateBack)

        assertEquals(state, transition.state)
        assertEquals(ShellEffect.ExitRequested, transition.effect)
    }
}
