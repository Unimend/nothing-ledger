package com.unimend.nothingledger.timeline

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.unimend.nothingledger.design.NothingLedgerTheme
import com.unimend.nothingledger.design.LedgerDotRippleSurface
import com.unimend.nothingledger.model.Transaction
import com.unimend.nothingledger.model.TransactionType
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Shared flat transaction collection. A host that needs date/account grouping renders its own
 * group header and invokes this once per group; search and account detail can pass one flat list.
 */
@Composable
public fun TransactionList(
    transactions: List<Transaction>,
    onTransactionClick: (Transaction) -> Unit,
    modifier: Modifier = Modifier,
    emptyMessage: String? = null,
    zoneId: ZoneId = ZoneId.systemDefault(),
) {
    Column(modifier.fillMaxWidth()) {
        if (transactions.isEmpty()) {
            emptyMessage?.let { message ->
                Text(
                    text = message,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
                    color = NothingLedgerTheme.colors.contentMuted,
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            transactions.forEach { transaction ->
                TransactionListItem(
                    transaction = transaction,
                    onClick = onTransactionClick,
                    zoneId = zoneId,
                )
            }
        }
    }
}

/**
 * The single transaction-row surface used by timeline, calendar and future search results.
 * Navigation stays outside this component; every host supplies the same transaction callback.
 */
@Composable
public fun TransactionListItem(
    transaction: Transaction,
    onClick: (Transaction) -> Unit,
    modifier: Modifier = Modifier,
    zoneId: ZoneId = ZoneId.systemDefault(),
) {
    LedgerDotRippleSurface(
        onClick = { onClick(transaction) },
        modifier = modifier.fillMaxWidth().padding(bottom = 4.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text(transaction.timelineTitle(), color = Color.White, style = MaterialTheme.typography.bodyLarge)
                Text(
                    transaction.note?.takeIf(String::isNotBlank)
                        ?: transaction.occurredAt.atZone(zoneId).format(LIST_TIME_FORMATTER),
                    color = NothingLedgerTheme.colors.contentMuted,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                )
            }
            Text(
                transaction.timelineAmount(),
                color = if (transaction.type == TransactionType.EXPENSE) {
                    NothingLedgerTheme.colors.expense
                } else {
                    NothingLedgerTheme.colors.income
                },
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.End,
                maxLines = 1,
            )
        }
    }
}

private val LIST_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
