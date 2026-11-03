package com.unimend.nothingledger.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unimend.nothingledger.design.NothingLedgerTheme
import com.unimend.nothingledger.design.ledgerClickable
import java.time.LocalDate
import kotlin.math.abs

/** Complete, testable visual state for the only calendar date-cell component. */
public sealed interface CalendarDayCellState {
    public data object Empty : CalendarDayCellState

    public data class Day(
        public val date: LocalDate,
        public val selected: Boolean,
        public val today: Boolean,
        public val incomeLabel: String? = null,
        public val expenseLabel: String? = null,
        public val hasTransfer: Boolean = false,
        public val hasMultipleCurrencies: Boolean = false,
    ) : CalendarDayCellState
}

/** Converts business calendar data into the single presentation model consumed by every host. */
public fun CalendarDay.toCellState(
    selected: Boolean,
    today: LocalDate,
): CalendarDayCellState.Day {
    val total = totals.singleOrNull()
    return CalendarDayCellState.Day(
        date = date,
        selected = selected,
        today = date == today,
        incomeLabel = total?.incomeMinor?.takeIf { it > 0 }?.let(::compactCalendarMinor),
        expenseLabel = total?.expenseMinor?.takeIf { it > 0 }?.let(::compactCalendarMinor),
        hasTransfer = hasTransfer,
        hasMultipleCurrencies = transactionCount > 0 && total == null,
    )
}

@Composable
public fun CalendarDayCell(
    state: CalendarDayCellState,
    onClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        CalendarDayCellState.Empty -> Box(modifier.height(CalendarDayCellHeight))
        is CalendarDayCellState.Day -> Box(
            modifier = modifier
                .height(CalendarDayCellHeight)
                .padding(1.dp)
                .background(if (state.selected) NothingLedgerTheme.colors.surfaceRaised else Color.Transparent)
                .ledgerClickable { onClick(state.date) },
        ) {
            if (state.today) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 5.dp, end = 5.dp)
                        .fillMaxWidth(0.18f)
                        .height(3.dp)
                        .background(NothingLedgerTheme.colors.accent),
                )
            }
            Column(Modifier.padding(horizontal = 4.dp, vertical = 5.dp)) {
                Text(
                    state.date.dayOfMonth.toString(),
                    color = if (state.selected) NothingLedgerTheme.colors.accent else Color.White,
                    fontSize = 13.sp,
                )
                when {
                    state.hasMultipleCurrencies -> CellLabel("多币种", NothingLedgerTheme.colors.contentMuted)
                    else -> {
                        state.incomeLabel?.let { CellLabel("+$it", Color.White) }
                        state.expenseLabel?.let { CellLabel("−$it", NothingLedgerTheme.colors.accent) }
                    }
                }
                if (state.hasTransfer) CellLabel("转账", NothingLedgerTheme.colors.contentMuted)
            }
        }
    }
}

@Composable
private fun CellLabel(text: String, color: Color) {
    Text(text = text, color = color, fontSize = 8.sp, maxLines = 1)
}

internal fun compactCalendarMinor(minor: Long): String {
    val absolute = if (minor == Long.MIN_VALUE) Long.MAX_VALUE else abs(minor)
    return when {
        absolute >= 100_000_000_000L -> "%.1fB".format(absolute / 100_000_000_000.0)
        absolute >= 100_000_000L -> "%.1fM".format(absolute / 100_000_000.0)
        absolute >= 100_000L -> "%.1fK".format(absolute / 100_000.0)
        else -> (absolute / 100L).toString()
    }
}

public val CalendarDayCellHeight = 58.dp
