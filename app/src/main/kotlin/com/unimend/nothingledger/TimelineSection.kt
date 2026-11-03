package com.unimend.nothingledger

import androidx.compose.foundation.background
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.unimend.nothingledger.design.LedgerSurface
import com.unimend.nothingledger.design.NothingLedgerTheme
import com.unimend.nothingledger.model.Transaction
import com.unimend.nothingledger.model.TransactionType
import com.unimend.nothingledger.timeline.MonthSummary
import com.unimend.nothingledger.timeline.TimelineState
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
internal fun TimelineSection(state: TimelineState, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("THIS MONTH", color = Color.White, style = MaterialTheme.typography.titleMedium)
            Text(state.monthLabel(), color = Color(0xFF8A8A8A), style = MaterialTheme.typography.labelMedium)
        }
        Spacer(Modifier.height(10.dp))
        when (state) {
            is TimelineState.Loading -> TimelineMessage("READING / LEDGER")
            is TimelineState.Failure -> TimelineMessage("ERROR / ${state.message.take(24)}", alert = true)
            is TimelineState.Empty -> {
                Summary(state.summary)
                TimelineMessage("NO ENTRIES / YET")
            }
            is TimelineState.Content -> {
                Summary(state.summary)
                Spacer(Modifier.height(8.dp))
                state.days.forEach { day ->
                    Text(
                        day.date.format(dayFormatter).uppercase(),
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = Color(0xFF8A8A8A),
                        style = MaterialTheme.typography.labelMedium,
                    )
                    day.transactions.forEach { TimelineItem(it) }
                }
            }
        }
    }
}

@Composable
private fun Summary(summary: MonthSummary) {
    val total = summary.totals.firstOrNull()
    if (total == null) return
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SummaryCell("IN", formatMinor(total.incomeMinor), Modifier.weight(1f), Color.White)
        SummaryCell("OUT", formatMinor(total.expenseMinor), Modifier.weight(1f), Color(0xFFFF2D2D))
        SummaryCell("NET", formatSigned(total.netMinor), Modifier.weight(1f), Color.White)
    }
}

@Composable
private fun SummaryCell(label: String, value: String, modifier: Modifier, color: Color) {
    LedgerSurface(modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(label, color = Color(0xFF777777), style = MaterialTheme.typography.labelMedium)
            Text(value, color = color, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

@Composable
private fun TimelineItem(transaction: Transaction) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
            .background(NothingLedgerTheme.colors.surface)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(transaction.title(), color = Color.White, style = MaterialTheme.typography.bodyLarge)
            Text(
                transaction.note?.takeIf(String::isNotBlank) ?: transaction.occurredAt.atZone(ZoneId.systemDefault()).format(timeFormatter),
                color = Color(0xFF777777), style = MaterialTheme.typography.bodyMedium,
            )
        }
        Text(
            transaction.displayAmount(),
            color = if (transaction.type == TransactionType.EXPENSE) Color(0xFFFF2D2D) else Color.White,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun TimelineMessage(message: String, alert: Boolean = false) {
    Text(
        message,
        Modifier.fillMaxWidth().padding(vertical = 20.dp),
        color = if (alert) Color(0xFFFF2D2D) else Color(0xFF777777),
        style = MaterialTheme.typography.labelMedium,
        textAlign = TextAlign.Center,
    )
}

private fun TimelineState.monthLabel(): String = when (this) {
    is TimelineState.Loading -> query.month
    is TimelineState.Empty -> query.month
    is TimelineState.Content -> query.month
    is TimelineState.Failure -> query.month
}.toString()

private fun Transaction.title(): String = when (type) {
    TransactionType.EXPENSE -> "日常支出"
    TransactionType.INCOME -> "日常收入"
    TransactionType.TRANSFER -> "账户转账"
}

private fun Transaction.displayAmount(): String {
    val prefix = when (type) {
        TransactionType.EXPENSE -> "−"
        TransactionType.INCOME -> "+"
        TransactionType.TRANSFER -> "→"
    }
    return "$prefix${formatMinor(money.minorUnits)}"
}

private fun formatSigned(value: Long): String = (if (value >= 0) "+" else "−") + formatMinor(kotlin.math.abs(value))
private fun formatMinor(value: Long): String = "¥%,d.%02d".format(value / 100, value % 100)
private val dayFormatter = DateTimeFormatter.ofPattern("MM / dd   EEE")
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
