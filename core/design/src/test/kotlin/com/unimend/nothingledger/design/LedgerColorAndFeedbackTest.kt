package com.unimend.nothingledger.design

import androidx.compose.ui.geometry.Offset
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class LedgerColorAndFeedbackTest {
    @Test
    fun `rapid taps remain independent feedback requests`() {
        val controller = LedgerFeedbackController()

        controller.showDotRipple(Offset(10f, 20f))
        controller.showDotRipple(Offset(30f, 40f))

        assertEquals(2, controller.dotRippleRequests.size)
        assertNotEquals(controller.dotRippleRequests[0].id, controller.dotRippleRequests[1].id)
    }

    @Test
    fun `semantic finance colors are visually distinct`() {
        val colors = LedgerColorTokens.Dark
        val semantic = setOf(
            colors.income,
            colors.expense,
            colors.asset,
            colors.transfer,
            colors.warning,
            colors.categoryA,
            colors.categoryB,
            colors.categoryC,
        )

        assertEquals(8, semantic.size)
    }
}
