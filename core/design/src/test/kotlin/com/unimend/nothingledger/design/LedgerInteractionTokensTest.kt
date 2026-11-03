package com.unimend.nothingledger.design

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LedgerInteractionTokensTest {
    @Test
    fun `motion durations follow semantic order`() {
        assertTrue(LedgerMotionTokens.ImmediateMillis < LedgerMotionTokens.FeedbackMillis)
        assertTrue(LedgerMotionTokens.FeedbackMillis < LedgerMotionTokens.NavigationMillis)
        assertTrue(LedgerMotionTokens.NavigationMillis < LedgerMotionTokens.DotRippleMillis)
    }

    @Test
    fun `local gesture thresholds have one stable source`() {
        assertEquals(64f, LedgerGestureTokens.LocalSwipeActionDistance.value)
        assertEquals(1.35f, LedgerGestureTokens.HorizontalDominanceRatio)
    }
}
