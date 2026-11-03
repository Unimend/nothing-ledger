package com.unimend.nothingledger

import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unimend.nothingledger.design.LedgerControl
import com.unimend.nothingledger.design.LedgerSpacing
import com.unimend.nothingledger.design.LedgerSurface
import com.unimend.nothingledger.design.NothingLedgerTheme
import com.unimend.nothingledger.design.GestureAxisLock
import com.unimend.nothingledger.design.HorizontalSwipeAction
import com.unimend.nothingledger.design.HorizontalSwipeConfig
import com.unimend.nothingledger.design.HorizontalSwipeState
import com.unimend.nothingledger.design.ledgerClickable
import com.unimend.nothingledger.model.Transaction
import com.unimend.nothingledger.model.TransactionType
import com.unimend.nothingledger.timeline.CalendarDay
import com.unimend.nothingledger.timeline.CalendarMonth
import com.unimend.nothingledger.timeline.CalendarState
import java.time.LocalDate

@Composable
internal fun CalendarSection(
    state: CalendarState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
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
) {
    val touchSlop = LocalViewConfiguration.current.touchSlop
    val swipeDistance = with(LocalDensity.current) { 64.dp.toPx() }
    val swipeConfig = HorizontalSwipeConfig(
        directionLockDistance = touchSlop,
        actionDistance = swipeDistance,
    )
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
                    if (day == null) {
                        Spacer(Modifier.weight(1f).height(58.dp))
                    } else {
                        CalendarCell(
                            day = day,
                            selected = day.date == month.query.selectedDate,
                            onClick = { onSelectDate(day.date) },
                            modifier = Modifier.weight(1f),
                        )
                    }
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
    if (selected != null && month.selectedTransactions.isEmpty()) {
        CalendarMessage("NO ENTRIES / THIS DAY")
    } else {
        month.selectedTransactions.forEach { transaction -> CalendarTransactionRow(transaction) }
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
private fun CalendarCell(
    day: CalendarDay,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val total = day.totals.singleOrNull()
    Box(
        modifier = modifier
            .height(58.dp)
            .padding(1.dp)
            .background(if (selected) NothingLedgerTheme.colors.surfaceRaised else Color.Transparent)
            .ledgerClickable(onClick = onClick),
    ) {
        Column(Modifier.padding(horizontal = 4.dp, vertical = 5.dp)) {
            Text(
                day.date.dayOfMonth.toString(),
                color = if (selected) NothingLedgerTheme.colors.accent else Color.White,
                fontSize = 13.sp,
            )
            if (day.transactionCount > 0) {
                if (total == null) {
                    Text("MULTI", color = NothingLedgerTheme.colors.contentMuted, fontSize = 8.sp)
                } else {
                    if (total.incomeMinor > 0) Text("+${compactMinor(total.incomeMinor)}", color = Color.White, fontSize = 8.sp)
                    if (total.expenseMinor > 0) Text("-${compactMinor(total.expenseMinor)}", color = NothingLedgerTheme.colors.accent, fontSize = 8.sp)
                }
                if (day.hasTransfer) Text("MOVE", color = NothingLedgerTheme.colors.contentMuted, fontSize = 8.sp)
            }
        }
    }
}

@Composable
private fun CalendarTransactionRow(transaction: Transaction) {
    LedgerSurface(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text(transaction.kindLabel(), color = Color.White, style = MaterialTheme.typography.bodyLarge)
                transaction.note?.takeIf(String::isNotBlank)?.let { note ->
                    Text(note, color = NothingLedgerTheme.colors.contentMuted, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Text(
                transaction.amountLabel(),
                color = if (transaction.type == TransactionType.EXPENSE) NothingLedgerTheme.colors.accent else Color.White,
                style = MaterialTheme.typography.labelLarge,
            )
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

private fun Transaction.kindLabel(): String = when (type) {
    TransactionType.EXPENSE -> "EXPENSE"
    TransactionType.INCOME -> "INCOME"
    TransactionType.TRANSFER -> "TRANSFER"
}

private fun Transaction.amountLabel(): String {
    val prefix = when (type) {
        TransactionType.EXPENSE -> "−"
        TransactionType.INCOME -> "+"
        TransactionType.TRANSFER -> "→"
    }
    return "$prefix${money.currency.symbol}${money.minorUnits / 100}.${(money.minorUnits % 100).toString().padStart(2, '0')}"
}

private fun compactMinor(minor: Long): String = when {
    minor >= 100_000_000 -> "${minor / 100_000_000}m"
    minor >= 100_000 -> "${minor / 100_000}k"
    else -> "${minor / 100}"
}

private val WEEKDAY_LABELS = listOf("MO", "TU", "WE", "TH", "FR", "SA", "SU")
