package com.unimend.nothingledger.workbench

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.unimend.nothingledger.design.LedgerAmountText
import com.unimend.nothingledger.design.LedgerControl
import com.unimend.nothingledger.design.LedgerSpacing
import com.unimend.nothingledger.design.LedgerSurface
import com.unimend.nothingledger.design.NothingLedgerTheme
import com.unimend.nothingledger.timeline.CalendarDayCell
import com.unimend.nothingledger.timeline.CalendarDayCellState
import com.unimend.nothingledger.timeline.TransactionList
import java.time.LocalDate
import java.time.ZoneId

/**
 * Database-free layout sketches. They deliberately compose production primitives and business
 * components, but own no navigation or feature state. A selected sketch may later become a page.
 */
@Composable
internal fun WorkbenchLayoutDrafts(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Text("大界面草图", color = Color.White, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(LedgerSpacing.xxs))
        Text(
            "先比较整体信息层级与排布，不在这里接入真实功能。",
            color = NothingLedgerTheme.colors.contentMuted,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(LedgerSpacing.md))
        DraftFrame("草图 01 / 流水首页") { TimelineDraft() }
        DraftGap()
        DraftFrame("草图 02 / 日历") { CalendarDraft() }
        DraftGap()
        DraftFrame("草图 03 / 统计") { StatisticsDraft() }
        DraftGap()
        DraftFrame("草图 04 / 账户") { AccountsDraft() }
        DraftGap()
        DraftFrame("草图 05 / 账单详情") { DetailDraft() }
        DraftGap()
        DraftFrame("草图 06 / 记账编辑") { EditorDraft() }
    }
}

@Composable
private fun DraftFrame(title: String, content: @Composable () -> Unit) {
    LedgerSurface(Modifier.fillMaxWidth(), elevated = true) {
        Column(Modifier.fillMaxWidth().padding(LedgerSpacing.md)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, color = Color.White, style = MaterialTheme.typography.titleMedium)
                Text("仅供布局预览", color = NothingLedgerTheme.colors.accent, style = MaterialTheme.typography.labelMedium)
            }
            Spacer(Modifier.height(LedgerSpacing.md))
            content()
        }
    }
}

@Composable
private fun TimelineDraft() {
    DraftHeader("十一月流水", "本月")
    Spacer(Modifier.height(LedgerSpacing.sm))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.xs)) {
        DraftMetric("收入", "¥8,650", Modifier.weight(1f))
        DraftMetric("支出", "¥1,234", Modifier.weight(1f), alert = true)
        DraftMetric("结余", "+¥7,416", Modifier.weight(1f))
    }
    Spacer(Modifier.height(LedgerSpacing.md))
    Text("11 月 03 日", color = NothingLedgerTheme.colors.contentMuted, style = MaterialTheme.typography.labelMedium)
    Spacer(Modifier.height(LedgerSpacing.xs))
    TransactionList(
        transactions = TransactionSamples.standardList,
        onTransactionClick = {},
        zoneId = DRAFT_ZONE,
    )
}

@Composable
private fun CalendarDraft() {
    DraftHeader("十一月", "左右滑动切月")
    Spacer(Modifier.height(LedgerSpacing.sm))
    Row(Modifier.fillMaxWidth()) {
        listOf("一", "二", "三", "四", "五", "六", "日").forEach { weekday ->
            Text(
                weekday,
                modifier = Modifier.weight(1f),
                color = NothingLedgerTheme.colors.contentMuted,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
    CALENDAR_DRAFT_DAYS.chunked(7).forEach { week ->
        Row(Modifier.fillMaxWidth()) {
            week.forEach { state ->
                CalendarDayCell(state = state, onClick = {}, modifier = Modifier.weight(1f))
            }
        }
    }
    Spacer(Modifier.height(LedgerSpacing.md))
    Text("选中日期的流水", color = Color.White, style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(LedgerSpacing.xs))
    TransactionList(
        transactions = TransactionSamples.standardList.take(2),
        onTransactionClick = {},
        zoneId = DRAFT_ZONE,
    )
}

@Composable
private fun StatisticsDraft() {
    DraftHeader("本月统计", "人民币")
    Spacer(Modifier.height(LedgerSpacing.sm))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.xs)) {
        DraftMetric("支出", "¥1,234", Modifier.weight(1f), alert = true)
        DraftMetric("收入", "¥8,650", Modifier.weight(1f))
    }
    Spacer(Modifier.height(LedgerSpacing.md))
    listOf("餐饮" to .72f, "交通" to .46f, "居家" to .28f).forEach { (name, ratio) ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(name, color = Color.White)
            Text("${(ratio * 100).toInt()}%", color = NothingLedgerTheme.colors.contentMuted)
        }
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth().height(6.dp).background(NothingLedgerTheme.colors.border)) {
            Box(Modifier.fillMaxWidth(ratio).height(6.dp).background(NothingLedgerTheme.colors.accent))
        }
        Spacer(Modifier.height(LedgerSpacing.xs))
    }
}

@Composable
private fun AccountsDraft() {
    DraftHeader("账户", "净资产")
    Spacer(Modifier.height(LedgerSpacing.sm))
    LedgerAmountText("¥ 12,345.67", Modifier.fillMaxWidth())
    Spacer(Modifier.height(LedgerSpacing.md))
    DraftAccount("微信钱包", "支付账户", "¥ 328.40")
    Spacer(Modifier.height(LedgerSpacing.xs))
    DraftAccount("储蓄卡", "银行账户", "¥ 12,017.27")
}

@Composable
private fun DetailDraft() {
    DraftHeader("账单详情", "支出")
    Spacer(Modifier.height(LedgerSpacing.sm))
    LedgerAmountText("−¥ 26.80", Modifier.fillMaxWidth(), accent = true)
    Spacer(Modifier.height(LedgerSpacing.md))
    DraftDetailRow("分类", "餐饮 / 午餐")
    DraftDetailRow("账户", "微信钱包")
    DraftDetailRow("时间", "2026 / 11 / 03  12:30")
    DraftDetailRow("备注", "午餐")
    Spacer(Modifier.height(LedgerSpacing.md))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.xs)) {
        LedgerControl("编辑", {}, Modifier.weight(1f))
        LedgerControl("删除", {}, Modifier.weight(1f), accent = true)
    }
}

@Composable
private fun EditorDraft() {
    DraftHeader("记一笔", "支出")
    Spacer(Modifier.height(LedgerSpacing.sm))
    LedgerAmountText("¥ 128.00", Modifier.fillMaxWidth())
    Spacer(Modifier.height(LedgerSpacing.md))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.xs)) {
        LedgerControl("餐饮", {}, Modifier.weight(1f))
        LedgerControl("微信钱包", {}, Modifier.weight(1f))
    }
    Spacer(Modifier.height(LedgerSpacing.sm))
    listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9")).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.xs)) {
            row.forEach { label -> LedgerControl(label, {}, Modifier.weight(1f)) }
        }
        Spacer(Modifier.height(LedgerSpacing.xs))
    }
    LedgerControl("保存账单", {}, Modifier.fillMaxWidth(), accent = true)
}

@Composable
private fun DraftHeader(title: String, hint: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = Color.White, style = MaterialTheme.typography.titleMedium)
        Text(hint, color = NothingLedgerTheme.colors.contentMuted, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun DraftMetric(label: String, value: String, modifier: Modifier, alert: Boolean = false) {
    LedgerSurface(modifier) {
        Column(Modifier.fillMaxWidth().padding(LedgerSpacing.sm)) {
            Text(label, color = NothingLedgerTheme.colors.contentMuted, style = MaterialTheme.typography.labelMedium)
            Text(value, color = if (alert) NothingLedgerTheme.colors.accent else Color.White, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun DraftAccount(name: String, type: String, balance: String) {
    LedgerSurface(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(LedgerSpacing.sm), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(name, color = Color.White, style = MaterialTheme.typography.bodyLarge)
                Text(type, color = NothingLedgerTheme.colors.contentMuted, style = MaterialTheme.typography.bodySmall)
            }
            Text(balance, color = Color.White, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun DraftDetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = LedgerSpacing.xs), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = NothingLedgerTheme.colors.contentMuted)
        Spacer(Modifier.width(LedgerSpacing.md))
        Text(value, color = Color.White)
    }
}

@Composable
private fun DraftGap() {
    Spacer(Modifier.height(LedgerSpacing.lg))
}

private val DRAFT_ZONE: ZoneId = ZoneId.of("Asia/Shanghai")

private val CALENDAR_DRAFT_DAYS: List<CalendarDayCellState> = buildList {
    repeat(6) { add(CalendarDayCellState.Empty) }
    (1..8).forEach { day ->
        add(
            CalendarDayCellState.Day(
                date = LocalDate.of(2026, 11, day),
                selected = day == 3,
                today = day == 3,
                incomeLabel = if (day == 2) "8650" else null,
                expenseLabel = if (day == 3) "26.8" else null,
                hasTransfer = day == 5,
            ),
        )
    }
}
