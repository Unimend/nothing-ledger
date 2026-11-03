package com.unimend.nothingledger.workbench

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.unimend.nothingledger.design.LedgerSpacing
import com.unimend.nothingledger.design.LedgerSurface
import com.unimend.nothingledger.design.NothingLedgerTheme
import com.unimend.nothingledger.timeline.CalendarDay
import com.unimend.nothingledger.timeline.CalendarDayCell
import com.unimend.nothingledger.timeline.CalendarDayCellState
import com.unimend.nothingledger.timeline.CalendarDayTotal
import com.unimend.nothingledger.timeline.toCellState
import java.time.LocalDate

/** 中文日历日期格状态矩阵。每个样本直接调用正式 CalendarDayCell。 */
@Composable
internal fun WorkbenchCalendarCatalog(modifier: Modifier = Modifier) {
    val today = LocalDate.of(2026, 11, 3)
    val samples = listOf(
        "空占位" to CalendarDayCellState.Empty,
        "普通日期" to sampleDay(2).toCellState(selected = false, today = today),
        "今天" to sampleDay(3).toCellState(selected = false, today = today),
        "已选中" to sampleDay(4).toCellState(selected = true, today = today),
        "有收入" to sampleDay(5, income = 865_000).toCellState(false, today),
        "有支出" to sampleDay(6, expense = 12_345).toCellState(false, today),
        "收入与支出" to sampleDay(7, income = 50_000, expense = 26_800).toCellState(false, today),
        "包含转账" to sampleDay(8, transfer = true).toCellState(false, today),
        "多币种" to sampleDay(9, multipleCurrencies = true).toCellState(false, today),
        "极端金额" to sampleDay(10, income = Long.MAX_VALUE).toCellState(false, today),
    )

    Column(modifier.fillMaxWidth()) {
        Text("业务组件 / 日历日期格", color = Color.White, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(LedgerSpacing.xxs))
        Text(
            "普通、选中、今天、收支、转账、多币种、空白与极端金额共用同一个组件。",
            color = NothingLedgerTheme.colors.contentMuted,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(LedgerSpacing.sm))
        samples.chunked(5).forEach { row ->
            LedgerSurface(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    row.forEach { (_, state) ->
                        CalendarDayCell(state = state, onClick = {}, modifier = Modifier.weight(1f))
                    }
                    repeat(5 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            Row(Modifier.fillMaxWidth()) {
                row.forEach { (label, _) ->
                    Text(
                        label,
                        modifier = Modifier.weight(1f),
                        color = NothingLedgerTheme.colors.contentMuted,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                repeat(5 - row.size) { Spacer(Modifier.weight(1f)) }
            }
            Spacer(Modifier.height(LedgerSpacing.sm))
        }
    }
}

private fun sampleDay(
    day: Int,
    income: Long = 0,
    expense: Long = 0,
    transfer: Boolean = false,
    multipleCurrencies: Boolean = false,
): CalendarDay {
    val totals = when {
        multipleCurrencies -> listOf(
            CalendarDayTotal("CNY", 10_000, 0),
            CalendarDayTotal("USD", 500, 0),
        )
        income > 0 || expense > 0 -> listOf(CalendarDayTotal("CNY", income, expense))
        else -> emptyList()
    }
    return CalendarDay(
        date = LocalDate.of(2026, 11, day),
        totals = totals,
        transactionCount = when {
            multipleCurrencies -> 2
            income > 0 || expense > 0 || transfer -> 1
            else -> 0
        },
        hasTransfer = transfer,
    )
}
