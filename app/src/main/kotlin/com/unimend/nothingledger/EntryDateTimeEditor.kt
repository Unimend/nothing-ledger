package com.unimend.nothingledger

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unimend.nothingledger.design.LedgerControl
import com.unimend.nothingledger.design.LedgerSpacing
import com.unimend.nothingledger.design.LedgerSurface
import com.unimend.nothingledger.design.NothingLedgerTheme
import com.unimend.nothingledger.design.ledgerClickable
import com.unimend.nothingledger.entry.EntryAction
import com.unimend.nothingledger.entry.buildEntryDateGrid
import com.unimend.nothingledger.entry.replaceEntryDate
import com.unimend.nothingledger.entry.selectRelativeEntryDate
import com.unimend.nothingledger.entry.shiftEntryMinutes
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
internal fun EntryDateTimeEditor(
    occurredAt: Instant,
    onAction: (EntryAction) -> Unit,
    modifier: Modifier = Modifier,
    clock: Clock = Clock.systemDefaultZone(),
) {
    val zoneId = clock.zone
    val now = remember(clock) { clock.instant() }
    val localValue = occurredAt.atZone(zoneId)
    var pickerVisible by remember { mutableStateOf(false) }
    var visibleMonth by remember { mutableStateOf(YearMonth.from(localValue)) }
    LaunchedEffect(occurredAt, zoneId) {
        if (!pickerVisible) visibleMonth = YearMonth.from(occurredAt.atZone(zoneId))
    }

    fun change(value: Instant) {
        onAction(EntryAction.ChangeOccurredAt(value))
    }

    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("DATE / TIME", color = NothingLedgerTheme.colors.contentMuted, style = MaterialTheme.typography.labelMedium)
                Text(localValue.format(DISPLAY_FORMAT), color = Color.White, style = MaterialTheme.typography.bodyLarge)
            }
            LedgerControl(
                label = if (pickerVisible) "CLOSE" else "SELECT DATE",
                onClick = { pickerVisible = !pickerVisible },
            )
        }
        if (pickerVisible) {
            Spacer(Modifier.height(LedgerSpacing.xs))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                LedgerControl(
                    label = "TODAY",
                    onClick = { change(selectRelativeEntryDate(occurredAt, now, 0, zoneId)) },
                    modifier = Modifier.weight(1f),
                    accent = localValue.toLocalDate() == now.atZone(zoneId).toLocalDate(),
                )
                LedgerControl(
                    label = "YESTERDAY",
                    onClick = { change(selectRelativeEntryDate(occurredAt, now, -1, zoneId)) },
                    modifier = Modifier.weight(1f),
                    accent = localValue.toLocalDate() == now.atZone(zoneId).toLocalDate().minusDays(1),
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                LedgerControl("−15 MIN", { change(shiftEntryMinutes(occurredAt, -15, zoneId)) }, Modifier.weight(1f))
                LedgerControl("+15 MIN", { change(shiftEntryMinutes(occurredAt, 15, zoneId)) }, Modifier.weight(1f))
            }
            Spacer(Modifier.height(LedgerSpacing.sm))
            EntryDatePicker(
                month = visibleMonth,
                selectedDate = localValue.toLocalDate(),
                onPreviousMonth = { visibleMonth = visibleMonth.minusMonths(1) },
                onNextMonth = { visibleMonth = visibleMonth.plusMonths(1) },
                onSelectDate = { date ->
                    change(replaceEntryDate(occurredAt, date, zoneId))
                    pickerVisible = false
                },
            )
        }
    }
}

@Composable
private fun EntryDatePicker(
    month: YearMonth,
    selectedDate: LocalDate,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
) {
    LedgerSurface(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(10.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LedgerControl("PREV", onPreviousMonth, Modifier.weight(1f))
                Text(
                    month.toString().replace("-", " / "),
                    modifier = Modifier.weight(1.4f),
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                )
                LedgerControl("NEXT", onNextMonth, Modifier.weight(1f))
            }
            Spacer(Modifier.height(LedgerSpacing.xs))
            Row(Modifier.fillMaxWidth()) {
                WEEKDAYS.forEach { label ->
                    Text(
                        label,
                        modifier = Modifier.weight(1f),
                        color = NothingLedgerTheme.colors.contentMuted,
                        fontSize = 9.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            buildEntryDateGrid(month).weeks.forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    week.forEach { date ->
                        if (date == null) {
                            Spacer(Modifier.weight(1f).height(48.dp))
                        } else {
                            Text(
                                text = date.dayOfMonth.toString(),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .ledgerClickable { onSelectDate(date) }
                                    .padding(top = 15.dp),
                                color = if (date == selectedDate) NothingLedgerTheme.colors.accent else Color.White,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}

private val DISPLAY_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy / MM / dd   HH:mm")
private val WEEKDAYS: List<String> = listOf("MO", "TU", "WE", "TH", "FR", "SA", "SU")
