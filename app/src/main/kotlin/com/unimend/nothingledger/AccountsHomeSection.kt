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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.unimend.nothingledger.accounts.AccountListItem
import com.unimend.nothingledger.accounts.AccountsState
import com.unimend.nothingledger.design.LedgerAmountText
import com.unimend.nothingledger.design.LedgerControl
import com.unimend.nothingledger.design.LedgerSurface
import java.math.BigDecimal
import java.util.Currency

/** Mature account landing surface. It consumes feature state and emits intents only. */
@Composable
internal fun AccountsHomeSection(
    accounts: AccountsState,
    editor: AccountEditorState,
    onBeginCreate: () -> Unit,
    onBeginRename: (AccountListItem) -> Unit,
    onNameChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onArchive: (AccountListItem) -> Unit,
    onOpenCategories: () -> Unit,
    onOpenDataManagement: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        AccountPortfolioSummary(accounts)
        Spacer(Modifier.height(24.dp))
        AccountsSection(
            accounts = accounts,
            editor = editor,
            onBeginCreate = onBeginCreate,
            onBeginRename = onBeginRename,
            onNameChange = onNameChange,
            onSave = onSave,
            onCancel = onCancel,
            onArchive = onArchive,
        )
        Spacer(Modifier.height(18.dp))
        Text("MANAGE", color = Color.White, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        LedgerControl(
            label = "分类管理",
            onClick = onOpenCategories,
            modifier = Modifier.fillMaxWidth(),
            accessibilityLabel = "打开分类管理",
        )
        Spacer(Modifier.height(8.dp))
        LedgerControl(
            label = "数据管理",
            onClick = onOpenDataManagement,
            modifier = Modifier.fillMaxWidth(),
            accessibilityLabel = "打开数据管理",
        )
    }
}

@Composable
private fun AccountPortfolioSummary(accounts: AccountsState) {
    val content = accounts as? AccountsState.Content
    val totals = content?.active.orEmpty()
        .groupBy { it.currentBalance.currency }
        .mapValues { (_, items) -> items.fold(0L) { total, item -> Math.addExact(total, item.currentBalance.minorUnits) } }
        .toList()
        .sortedBy { (currency, _) -> currency.currencyCode }

    LedgerSurface(Modifier.fillMaxWidth(), elevated = true) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("NET ASSETS", color = Color.White, style = MaterialTheme.typography.titleMedium)
                Text(
                    when (accounts) {
                        AccountsState.Loading -> "读取中"
                        is AccountsState.Failure -> "不可用"
                        is AccountsState.Content -> "${accounts.active.size} 个账户"
                    },
                    color = if (accounts is AccountsState.Failure) Color(0xFFFF2D2D) else Color(0xFF777777),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Spacer(Modifier.height(16.dp))
            when {
                accounts is AccountsState.Loading -> SummaryHint("正在汇总账户")
                accounts is AccountsState.Failure -> SummaryHint(accounts.message.take(36), alert = true)
                totals.isEmpty() -> LedgerAmountText("¥ 0.00", Modifier.fillMaxWidth())
                totals.size == 1 -> {
                    val (currency, value) = totals.single()
                    LedgerAmountText(formatCurrency(value, currency), Modifier.fillMaxWidth())
                }
                else -> totals.forEach { (currency, value) ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(currency.currencyCode, color = Color(0xFF777777), style = MaterialTheme.typography.labelMedium)
                        Text(formatCurrency(value, currency), color = Color.White, style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
            if (content != null && content.archived.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "另有 ${content.archived.size} 个已归档账户，不计入上方合计",
                    color = Color(0xFF666666),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun SummaryHint(message: String, alert: Boolean = false) {
    Text(
        message,
        color = if (alert) Color(0xFFFF2D2D) else Color(0xFF777777),
        style = MaterialTheme.typography.bodyMedium,
    )
}

private fun formatCurrency(minorUnits: Long, currency: Currency): String {
    val fractionDigits = currency.defaultFractionDigits.coerceAtLeast(0)
    val value = BigDecimal.valueOf(minorUnits, fractionDigits).toPlainString()
    return "${currency.symbol} $value"
}
