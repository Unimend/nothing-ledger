package com.unimend.nothingledger

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unimend.nothingledger.design.LedgerControl
import com.unimend.nothingledger.design.LedgerSpacing
import com.unimend.nothingledger.design.NothingLedgerTheme
import com.unimend.nothingledger.design.GestureAxisLock
import com.unimend.nothingledger.design.HorizontalSwipeAction
import com.unimend.nothingledger.design.HorizontalSwipeConfig
import com.unimend.nothingledger.design.HorizontalSwipeState
import com.unimend.nothingledger.design.rememberLedgerLocalSwipeConfig
import com.unimend.nothingledger.model.Transaction
import com.unimend.nothingledger.timeline.CalendarDayCell
import com.unimend.nothingledger.timeline.CalendarDayCellState
import com.unimend.nothingledger.timeline.CalendarMonth
import com.unimend.nothingledger.timeline.CalendarState
import com.unimend.nothingledger.timeline.TransactionList
import com.unimend.nothingledger.timeline.toCellState
import java.time.LocalDate

@Composable
internal fun CalendarSection(
    state: CalendarState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onTransactionClick: (Transaction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LedgerControl("PREV", onPreviousMonth, Modifier.weight(1f))
            Text(
                text = state.monthLabel(),
                modifier = Modifier.weight(1.4f),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            LedgerControl("NEXT", onNextMonth, Modifier.weight(1f))
        }
        Spacer(Modifier.height(LedgerSpacing.sm))
        when (state) {
            is CalendarState.Loading -> CalendarMessage("READING / CALENDAR")
            is CalendarState.Failure -> CalendarMessage("ERROR / ${state.message.take(36)}", alert = true)
            is CalendarState.Ready -> CalendarGrid(
                month = state.month,
                onSelectDate = onSelectDate,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth,
                onTransactionClick = onTransactionClick,
            )
        }
    }
}

@Composable
private fun CalendarGrid(
    month: CalendarMonth,
    onSelectDate: (LocalDate) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onTransactionClick: (Transaction) -> Unit,
) {
    val swipeConfig = rememberLedgerLocalSwipeConfig()
    Column(
        Modifier
            .fillMaxWidth()
            .calendarMonthSwipe(swipeConfig, onPreviousMonth, onNextMonth),
    ) {
        Row(Modifier.fillMaxWidth()) {
            WEEKDAY_LABELS.forEach { label ->
                Text(
                    label,
                    modifier = Modifier.weight(1f).padding(bottom = 6.dp),
                    color = NothingLedgerTheme.colors.contentMuted,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
        month.weeks.forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    CalendarDayCell(
                        state = day?.toCellState(
                            selected = day.date == month.query.selectedDate,
                            today = LocalDate.now(),
                        ) ?: CalendarDayCellState.Empty,
                        onClick = onSelectDate,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
    Spacer(Modifier.height(LedgerSpacing.md))
    val selected = month.query.selectedDate
    Text(
        text = selected?.let { "${it.monthValue} / ${it.dayOfMonth}  ENTRIES" } ?: "SELECT A DAY",
        color = if (selected == null) NothingLedgerTheme.colors.contentMuted else Color.White,
        style = MaterialTheme.typography.labelMedium,
    )
    Spacer(Modifier.height(LedgerSpacing.xs))
    if (selected != null) {
        TransactionList(
            transactions = month.selectedTransactions,
            onTransactionClick = onTransactionClick,
            emptyMessage = "NO ENTRIES / THIS DAY",
        )
    }
}

private fun Modifier.calendarMonthSwipe(
    config: HorizontalSwipeConfig,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
): Modifier = pointerInput(config, onPreviousMonth, onNextMonth) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        var swipe = HorizontalSwipeState()
        var pressed = true
        while (pressed) {
            val event = awaitPointerEvent()
            val change: PointerInputChange = event.changes.firstOrNull() ?: break
            val movement: Offset = change.positionChange()
            swipe = swipe.move(movement.x, movement.y, config)
            if (swipe.axisLock == GestureAxisLock.Horizontal) change.consume()
            pressed = change.pressed
        }
        when (swipe.finish(config)) {
            HorizontalSwipeAction.Previous -> onPreviousMonth()
            HorizontalSwipeAction.Next -> onNextMonth()
            HorizontalSwipeAction.None -> Unit
        }
    }
}

@Composable
private fun CalendarMessage(message: String, alert: Boolean = false) {
    Box(Modifier.fillMaxWidth().padding(vertical = 18.dp), contentAlignment = Alignment.Center) {
        Text(
            message,
            color = if (alert) NothingLedgerTheme.colors.accent else NothingLedgerTheme.colors.contentMuted,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

private fun CalendarState.monthLabel(): String = when (this) {
    is CalendarState.Loading -> query.month
    is CalendarState.Ready -> month.query.month
    is CalendarState.Failure -> query.month
}.toString().replace("-", " / ")

private val WEEKDAY_LABELS = listOf("MO", "TU", "WE", "TH", "FR", "SA", "SU")
