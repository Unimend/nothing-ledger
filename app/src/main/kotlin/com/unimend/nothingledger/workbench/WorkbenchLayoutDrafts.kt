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
import com.unimend.nothingledger.design.LedgerScreen
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
internal enum class WorkbenchLayoutDraft(
    val index: String,
    val title: String,
    val description: String,
) {
    TIMELINE("01", "流水首页", "月度概览、日期分组与流水列表"),
    CALENDAR("02", "日历", "完整月份与选中日期流水"),
    STATISTICS("03", "统计", "收支汇总与分类比例"),
    ACCOUNTS("04", "账户", "净资产与账户列表"),
    DETAIL("05", "账单详情", "单条账单的完整信息"),
    EDITOR("06", "记账编辑", "金额、分类、账户与数字键盘"),
    TAGS("07", "标签系统", "标签管理、颜色与使用状态"),
    MINI_KEYPAD("08", "迷你数字键盘", "金额输入与快捷运算"),
    INCOME_EXPENSE_STATS("09", "收支统计", "按日、月、年查看收入与支出"),
    ASSET_TREND("10", "资产走势", "净资产随月份或年份的变化"),
    INCOME_EXPENSE_COMPARE("11", "收支对比", "同一周期内收入与支出的差距"),
    EXPENSE_SHARE("12", "支出占比", "不同支出分类的构成比例"),
    INCOME_EXPENSE_HEATMAP("13", "收支热力", "以日期密度观察收支活跃程度"),
}

@Composable
internal fun WorkbenchLayoutDrafts(
    onOpenDraft: (WorkbenchLayoutDraft) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        Text("大界面草图", color = Color.White, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(LedgerSpacing.xxs))
        Text(
            "每个草图都以完整手机页面单独预览，不在目录中压缩展示。",
            color = NothingLedgerTheme.colors.contentMuted,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(LedgerSpacing.md))
        WorkbenchLayoutDraft.entries.forEach { draft ->
            DraftEntry(draft = draft, onOpen = { onOpenDraft(draft) })
            Spacer(Modifier.height(LedgerSpacing.xs))
        }
    }
}

@Composable
private fun DraftEntry(draft: WorkbenchLayoutDraft, onOpen: () -> Unit) {
    LedgerSurface(Modifier.fillMaxWidth(), elevated = true) {
        Row(
            Modifier.fillMaxWidth().padding(LedgerSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(draft.index, color = NothingLedgerTheme.colors.accent, style = MaterialTheme.typography.titleLarge)
            Column(Modifier.weight(1f)) {
                Text(draft.title, color = Color.White, style = MaterialTheme.typography.titleMedium)
                Text(
                    draft.description,
                    color = NothingLedgerTheme.colors.contentMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            LedgerControl("全屏查看", onOpen)
        }
    }
}

@Composable
internal fun WorkbenchLayoutDraftScreen(
    draft: WorkbenchLayoutDraft,
    onBack: () -> Unit,
) {
    LedgerScreen {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("草图 ${draft.index}", color = NothingLedgerTheme.colors.accent, style = MaterialTheme.typography.labelLarge)
                Text(draft.title, color = Color.White, style = MaterialTheme.typography.titleLarge)
            }
            LedgerControl("返回目录", onBack)
        }
        Spacer(Modifier.height(LedgerSpacing.lg))
        when (draft) {
            WorkbenchLayoutDraft.TIMELINE -> TimelineDraft()
            WorkbenchLayoutDraft.CALENDAR -> CalendarDraft()
            WorkbenchLayoutDraft.STATISTICS -> StatisticsDraft()
            WorkbenchLayoutDraft.ACCOUNTS -> AccountsDraft()
            WorkbenchLayoutDraft.DETAIL -> DetailDraft()
            WorkbenchLayoutDraft.EDITOR -> EntryEditorDraft()
            WorkbenchLayoutDraft.TAGS -> TagsDraft()
            WorkbenchLayoutDraft.MINI_KEYPAD -> MiniNumericKeypadDraft()
            WorkbenchLayoutDraft.INCOME_EXPENSE_STATS -> IncomeExpenseStatsDraft()
            WorkbenchLayoutDraft.ASSET_TREND -> AssetTrendDraft()
            WorkbenchLayoutDraft.INCOME_EXPENSE_COMPARE -> IncomeExpenseCompareDraft()
            WorkbenchLayoutDraft.EXPENSE_SHARE -> ExpenseShareDraft()
            WorkbenchLayoutDraft.INCOME_EXPENSE_HEATMAP -> IncomeExpenseHeatmapDraft()
        }
        Spacer(Modifier.height(64.dp))
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
