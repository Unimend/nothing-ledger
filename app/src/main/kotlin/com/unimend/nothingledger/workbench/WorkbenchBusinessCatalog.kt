package com.unimend.nothingledger.workbench

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.unimend.nothingledger.design.LedgerSurface
import com.unimend.nothingledger.design.NothingLedgerTheme
import com.unimend.nothingledger.model.AccountId
import com.unimend.nothingledger.model.CategoryId
import com.unimend.nothingledger.model.Money
import com.unimend.nothingledger.model.Transaction
import com.unimend.nothingledger.model.TransactionId
import com.unimend.nothingledger.model.TransactionType
import com.unimend.nothingledger.timeline.TransactionList
import com.unimend.nothingledger.timeline.TransactionListItem
import java.time.Instant
import java.time.ZoneId
import java.util.Currency
import java.util.UUID

/**
 * Pure sample data for the component workbench. Nothing in this catalog reads a database or owns
 * product navigation. It deliberately exercises the same production components used by screens.
 */
@Composable
internal fun WorkbenchBusinessCatalog(
    onTransactionClick: (Transaction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        CatalogHeading("业务组件 / 单条流水", "同一个组件的完整状态矩阵")
        TransactionSamples.all.forEach { sample ->
            CatalogLabel(sample.label)
            TransactionListItem(
                transaction = sample.transaction,
                onClick = onTransactionClick,
                zoneId = WORKBENCH_ZONE,
            )
            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(22.dp))
        CatalogHeading("业务组件 / 流水列表", "页面只能提供数据，不能重新绘制流水")
        CatalogLabel("有数据 / 混合类型")
        TransactionList(
            transactions = TransactionSamples.standardList,
            onTransactionClick = onTransactionClick,
            zoneId = WORKBENCH_ZONE,
        )
        Spacer(Modifier.height(12.dp))
        CatalogLabel("空列表")
        LedgerSurface(Modifier.fillMaxWidth()) {
            TransactionList(
                transactions = emptyList(),
                onTransactionClick = onTransactionClick,
                emptyMessage = "暂无流水 / 样本",
                zoneId = WORKBENCH_ZONE,
            )
        }
    }
}

@Composable
private fun CatalogHeading(title: String, description: String) {
    Text(title, color = Color.White, style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(5.dp))
    Text(
        description,
        color = NothingLedgerTheme.colors.contentMuted,
        style = MaterialTheme.typography.bodyMedium,
    )
    Spacer(Modifier.height(14.dp))
}

@Composable
private fun CatalogLabel(label: String) {
    Text(
        label,
        color = NothingLedgerTheme.colors.accent,
        style = MaterialTheme.typography.labelMedium,
    )
    Spacer(Modifier.height(6.dp))
}

internal data class TransactionSample(
    val label: String,
    val transaction: Transaction,
)

/** Stable fixtures make screenshots and interaction tests deterministic. */
internal object TransactionSamples {
    private val expense = sampleTransaction(
        serial = 1,
        type = TransactionType.EXPENSE,
        minorUnits = 2_680,
        note = "午餐",
    )
    private val income = sampleTransaction(
        serial = 2,
        type = TransactionType.INCOME,
        minorUnits = 865_000,
        note = "工资到账",
    )
    private val transfer = sampleTransaction(
        serial = 3,
        type = TransactionType.TRANSFER,
        minorUnits = 100_000,
        note = null,
    )
    private val longText = sampleTransaction(
        serial = 4,
        type = TransactionType.EXPENSE,
        minorUnits = 12_345,
        note = "这是一条故意非常长的备注，用来确认流水组件在狭窄屏幕中仍然只占一行并且不会挤压右侧金额",
    )
    private val extremeAmount = sampleTransaction(
        serial = 5,
        type = TransactionType.INCOME,
        minorUnits = 9_223_372_036_854_000L,
        note = "极端金额边界",
    )

    val all: List<TransactionSample> = listOf(
        TransactionSample("支出 / 有备注", expense),
        TransactionSample("收入 / 有备注", income),
        TransactionSample("转账 / 无备注", transfer),
        TransactionSample("超长备注", longText),
        TransactionSample("极端金额", extremeAmount),
    )

    val standardList: List<Transaction> = listOf(expense, income, transfer)
}

private fun sampleTransaction(
    serial: Int,
    type: TransactionType,
    minorUnits: Long,
    note: String?,
): Transaction {
    val source = AccountId(stableUuid(100))
    val destination = AccountId(stableUuid(101))
    val category = CategoryId(stableUuid(200))
    val instant = Instant.parse("2026-11-03T08:${serial.toString().padStart(2, '0')}:00Z")
    return Transaction(
        id = TransactionId(stableUuid(serial)),
        type = type,
        money = Money(minorUnits, Currency.getInstance("CNY")),
        occurredAt = instant,
        sourceAccountId = when (type) {
            TransactionType.EXPENSE, TransactionType.TRANSFER -> source
            TransactionType.INCOME -> null
        },
        destinationAccountId = when (type) {
            TransactionType.INCOME, TransactionType.TRANSFER -> destination
            TransactionType.EXPENSE -> null
        },
        categoryId = when (type) {
            TransactionType.EXPENSE, TransactionType.INCOME -> category
            TransactionType.TRANSFER -> null
        },
        note = note,
        createdAt = instant,
        updatedAt = instant,
    )
}

private fun stableUuid(serial: Int): UUID =
    UUID.fromString("00000000-0000-0000-0000-${serial.toString().padStart(12, '0')}")

private val WORKBENCH_ZONE: ZoneId = ZoneId.of("Asia/Shanghai")
