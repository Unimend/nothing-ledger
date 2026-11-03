package com.unimend.nothingledger.design

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class HorizontalSwipeTest {
    private val config = HorizontalSwipeConfig(
        directionLockDistance = 12f,
        actionDistance = 48f,
        horizontalDominanceRatio = 1.35f,
    )

    @Test
    fun `right swipe requests previous`() {
        val state = HorizontalSwipeState().move(60f, 8f, config)

        assertEquals(GestureAxisLock.Horizontal, state.axisLock)
        assertEquals(HorizontalSwipeAction.Previous, state.finish(config))
    }

    @Test
    fun `left swipe requests next`() {
        val state = HorizontalSwipeState().move(-60f, 4f, config)

        assertEquals(HorizontalSwipeAction.Next, state.finish(config))
    }

    @Test
    fun `short horizontal drag does not trigger action`() {
        val state = HorizontalSwipeState().move(30f, 2f, config)

        assertEquals(GestureAxisLock.Horizontal, state.axisLock)
        assertEquals(HorizontalSwipeAction.None, state.finish(config))
    }

    @Test
    fun `mostly vertical gesture locks vertical and stays locked`() {
        val vertical = HorizontalSwipeState().move(5f, 20f, config)
        val curved = vertical.move(80f, 0f, config)

        assertEquals(GestureAxisLock.Vertical, curved.axisLock)
        assertEquals(HorizontalSwipeAction.None, curved.finish(config))
    }

    @Test
    fun `ambiguous diagonal remains undecided until one axis dominates`() {
        val diagonal = HorizontalSwipeState().move(20f, 18f, config)
        val horizontal = diagonal.move(20f, 0f, config)

        assertEquals(GestureAxisLock.Undecided, diagonal.axisLock)
        assertEquals(GestureAxisLock.Horizontal, horizontal.axisLock)
    }

    @Test
    fun `horizontal lock survives curved finish`() {
        val locked = HorizontalSwipeState().move(18f, 2f, config)
        val curved = locked.move(36f, 80f, config)

        assertEquals(GestureAxisLock.Horizontal, curved.axisLock)
        assertEquals(HorizontalSwipeAction.Previous, curved.finish(config))
    }
}
