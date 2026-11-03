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
import com.unimend.nothingledger.design.LedgerAmountText
import com.unimend.nothingledger.design.LedgerControl
import com.unimend.nothingledger.design.LedgerScreen
import com.unimend.nothingledger.design.LedgerSurface
import com.unimend.nothingledger.design.NothingLedgerTheme
import com.unimend.nothingledger.model.Transaction
import com.unimend.nothingledger.model.TransactionType
import com.unimend.nothingledger.timeline.TransactionDetailPresentation
import com.unimend.nothingledger.timeline.TransactionDetailActionState
import com.unimend.nothingledger.timeline.TransactionDetailPhase
import com.unimend.nothingledger.timeline.TransactionDetailState
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
internal fun TransactionDetailScreen(
    state: TransactionDetailState,
    actionState: TransactionDetailActionState,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onUndoDelete: () -> Unit,
) {
    if (actionState.phase == TransactionDetailPhase.DELETED) {
        DeletedDetailScreen(onBack, onUndoDelete)
        return
    }
    when (state) {
        is TransactionDetailState.Ready -> TransactionDetailScreen(state.detail, actionState, onBack, onDelete)
        is TransactionDetailState.Loading -> DetailStatusScreen("READING / DETAIL", onBack)
        is TransactionDetailState.Missing -> DetailStatusScreen("ENTRY / NOT FOUND", onBack)
        is TransactionDetailState.Failure -> DetailStatusScreen("ERROR / ${state.message.take(36)}", onBack)
    }
}

@Composable
private fun TransactionDetailScreen(
    detail: TransactionDetailPresentation,
    actionState: TransactionDetailActionState,
    onBack: () -> Unit,
    onDelete: () -> Unit,
) {
    val transaction = detail.transaction
    LedgerScreen {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("账单详情", color = Color.White, style = MaterialTheme.typography.titleLarge)
            LedgerControl("返回", onBack)
        }
        Spacer(Modifier.height(28.dp))
        LedgerAmountText(transaction.detailAmount(), Modifier.fillMaxWidth(), accent = transaction.type == TransactionType.EXPENSE)
        Spacer(Modifier.height(20.dp))
        LedgerSurface(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                DetailRow("类型", transaction.detailType())
                DetailRow("日期", transaction.occurredAt.atZone(ZoneId.systemDefault()).format(dateTimeFormatter))
                if (transaction.sourceAccountId != null) {
                    DetailRow("来源账户", detail.sourceAccountName ?: "账户不可用")
                }
                if (transaction.destinationAccountId != null) {
                    DetailRow("目标账户", detail.destinationAccountName ?: "账户不可用")
                }
                if (transaction.categoryId != null) {
                    DetailRow("分类", detail.categoryLabel ?: "分类不可用")
                }
                DetailRow("备注", transaction.note?.takeIf(String::isNotBlank) ?: "无")
                DetailRow("币种", transaction.money.currency.currencyCode)
            }
        }
        actionState.errorMessage?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, color = NothingLedgerTheme.colors.accent, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(16.dp))
        LedgerControl(
            label = if (actionState.phase == TransactionDetailPhase.WORKING) "处理中" else "删除账单",
            onClick = onDelete,
            modifier = Modifier.fillMaxWidth(),
            accent = true,
            enabled = actionState.phase != TransactionDetailPhase.WORKING,
        )
    }
}

@Composable
private fun DeletedDetailScreen(onBack: () -> Unit, onUndoDelete: () -> Unit) {
    LedgerScreen {
        Text("账单已删除", color = Color.White, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(24.dp))
        LedgerControl("撤回删除", onUndoDelete, Modifier.fillMaxWidth(), accent = true)
        Spacer(Modifier.height(8.dp))
        LedgerControl("返回流水", onBack, Modifier.fillMaxWidth())
    }
}

@Composable
private fun DetailStatusScreen(message: String, onBack: () -> Unit) {
    LedgerScreen {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("账单详情", color = Color.White, style = MaterialTheme.typography.titleLarge)
            LedgerControl("返回", onBack)
        }
        Spacer(Modifier.height(40.dp))
        Text(message, color = NothingLedgerTheme.colors.contentMuted, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = NothingLedgerTheme.colors.contentMuted, style = MaterialTheme.typography.bodyMedium)
        Text(value, color = Color.White, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun Transaction.detailType(): String = when (type) {
    TransactionType.EXPENSE -> "支出"
    TransactionType.INCOME -> "收入"
    TransactionType.TRANSFER -> "转账"
}

private fun Transaction.detailAmount(): String {
    val prefix = when (type) {
        TransactionType.EXPENSE -> "−"
        TransactionType.INCOME -> "+"
        TransactionType.TRANSFER -> "→"
    }
    return "$prefix${money.currency.symbol}%,d.%02d".format(money.minorUnits / 100, money.minorUnits % 100)
}

private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日  HH:mm")
