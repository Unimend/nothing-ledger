package com.unimend.nothingledger.timeline

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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.unimend.nothingledger.design.LedgerSurface
import com.unimend.nothingledger.design.NothingLedgerTheme
import com.unimend.nothingledger.design.ledgerClickable
import com.unimend.nothingledger.model.Transaction
import com.unimend.nothingledger.model.TransactionType
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Currency
import kotlin.math.abs

@Composable
public fun TimelineHomePage(
    state: TimelineState,
    onTransactionClick: (Transaction) -> Unit,
    modifier: Modifier = Modifier,
    zoneId: ZoneId = ZoneId.systemDefault(),
) {
    Column(modifier = modifier.fillMaxWidth()) {
        TimelineHeader(state)
        Spacer(Modifier.height(20.dp))
        when (state) {
            is TimelineState.Loading -> TimelineNotice("READING / LEDGER")
            is TimelineState.Failure -> TimelineNotice("ERROR / ${state.message.take(32)}", alert = true)
            is TimelineState.Empty -> {
                TimelineSummary(state.summary)
                TimelineNotice("NO ENTRIES / YET")
            }
            is TimelineState.Content -> {
                TimelineSummary(state.summary)
                Spacer(Modifier.height(16.dp))
                state.days.forEach { day ->
                    Text(
                        day.date.format(dayFormatter).uppercase(),
                        modifier = Modifier.padding(vertical = 10.dp),
                        color = NothingLedgerTheme.colors.contentMuted,
                        style = MaterialTheme.typography.labelMedium,
                    )
                    day.transactions.forEach { transaction ->
                        TransactionRow(transaction, zoneId) { onTransactionClick(transaction) }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineHeader(state: TimelineState) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("本月流水", color = Color.White, style = MaterialTheme.typography.titleLarge)
        Text(
            state.query().month.toString().replace("-", " / "),
            color = NothingLedgerTheme.colors.contentMuted,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun TimelineSummary(summary: MonthSummary) {
    if (summary.totals.isEmpty()) return
    summary.totals.forEach { subtotal ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryCell("IN", formatCompactMinor(subtotal.incomeMinor, subtotal.currencyCode), Modifier.weight(1f))
            SummaryCell("OUT", formatCompactMinor(subtotal.expenseMinor, subtotal.currencyCode), Modifier.weight(1f), true)
            SummaryCell("NET", formatCompactSigned(subtotal.netMinor, subtotal.currencyCode), Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SummaryCell(label: String, value: String, modifier: Modifier, alert: Boolean = false) {
    LedgerSurface(modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(label, color = NothingLedgerTheme.colors.contentMuted, style = MaterialTheme.typography.labelMedium)
            Text(
                value,
                color = if (alert) NothingLedgerTheme.colors.accent else NothingLedgerTheme.colors.contentPrimary,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun TransactionRow(transaction: Transaction, zoneId: ZoneId, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 2.dp)
            .background(NothingLedgerTheme.colors.surface)
            .ledgerClickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(transaction.timelineTitle(), color = Color.White, style = MaterialTheme.typography.bodyLarge)
            Text(
                transaction.note?.takeIf(String::isNotBlank)
                    ?: transaction.occurredAt.atZone(zoneId).format(timeFormatter),
                color = NothingLedgerTheme.colors.contentMuted,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
            )
        }
        Text(
            transaction.timelineAmount(),
            color = if (transaction.type == TransactionType.EXPENSE) {
                NothingLedgerTheme.colors.accent
            } else {
                NothingLedgerTheme.colors.contentPrimary
            },
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.End,
            maxLines = 1,
        )
    }
}

@Composable
private fun TimelineNotice(message: String, alert: Boolean = false) {
    Text(
        message,
        Modifier.fillMaxWidth().padding(vertical = 40.dp),
        color = if (alert) NothingLedgerTheme.colors.accent else NothingLedgerTheme.colors.contentMuted,
        style = MaterialTheme.typography.labelMedium,
        textAlign = TextAlign.Center,
    )
}

internal fun TimelineState.query(): TimelineQuery = when (this) {
    is TimelineState.Loading -> query
    is TimelineState.Empty -> query
    is TimelineState.Content -> query
    is TimelineState.Failure -> query
}

internal fun Transaction.timelineTitle(): String = when (type) {
    TransactionType.EXPENSE -> "支出"
    TransactionType.INCOME -> "收入"
    TransactionType.TRANSFER -> "账户转账"
}

internal fun Transaction.timelineAmount(): String {
    val prefix = when (type) {
        TransactionType.EXPENSE -> "−"
        TransactionType.INCOME -> "+"
        TransactionType.TRANSFER -> "→"
    }
    return prefix + formatCompactMinor(money.minorUnits, money.currency.currencyCode)
}

internal fun formatSigned(value: Long, currencyCode: String): String =
    (if (value >= 0) "+" else "−") + formatMinor(abs(value), currencyCode)

internal fun formatCompactSigned(value: Long, currencyCode: String): String =
    (if (value >= 0) "+" else "−") + formatCompactMinor(abs(value), currencyCode)

internal fun formatCompactMinor(value: Long, currencyCode: String): String {
    val currency = Currency.getInstance(currencyCode)
    val scale = currency.defaultFractionDigits.coerceAtLeast(0)
    val major = value.toDouble() / powerOfTen(scale).toDouble()
    val compact = when {
        major >= 1_000_000_000 -> "%.2fB".format(major / 1_000_000_000)
        major >= 1_000_000 -> "%.2fM".format(major / 1_000_000)
        major >= 100_000 -> "%.1fK".format(major / 1_000)
        else -> return formatMinor(value, currencyCode)
    }
    return currency.symbol + compact
}

internal fun formatMinor(value: Long, currencyCode: String): String {
    val currency = Currency.getInstance(currencyCode)
    val scale = currency.defaultFractionDigits.coerceAtLeast(0)
    val divisor = powerOfTen(scale)
    return if (scale == 0) {
        "${currency.symbol}%,d".format(value)
    } else {
        "${currency.symbol}%,d.%0${scale}d".format(value / divisor, value % divisor)
    }
}

private fun powerOfTen(scale: Int): Long = generateSequence(1L) { it * 10L }.drop(scale).first()
private val dayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MM / dd   EEE")
private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
