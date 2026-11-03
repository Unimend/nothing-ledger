package com.unimend.nothingledger.workbench

import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.unimend.nothingledger.design.LedgerAmountText
import com.unimend.nothingledger.design.LedgerControl
import com.unimend.nothingledger.design.LedgerSpacing
import com.unimend.nothingledger.design.LedgerSurface
import com.unimend.nothingledger.design.NothingLedgerTheme

@Composable
internal fun IncomeExpenseStatsDraft() {
    var period by remember { mutableStateOf("月") }
    ChartTitle("收支统计", "从总量到时间分布", listOf("日", "月", "年"), period) { period = it }
    ChartSummary("收入", "+¥ 8,650", "支出", "−¥ 3,284")
    ChartCard("${period}度收支分布") {
        PairedBarChart(
            income = listOf(.36f, .62f, .48f, .84f, .55f, .72f, .92f),
            expense = listOf(.52f, .31f, .76f, .43f, .68f, .38f, .61f),
        )
        AxisLabels(if (period == "日") listOf("08", "10", "12", "14", "16", "18", "20") else listOf("1", "2", "3", "4", "5", "6", "7"))
        ChartLegend("收入", NothingLedgerTheme.colors.income, "支出", NothingLedgerTheme.colors.expense)
    }
}

@Composable
internal fun AssetTrendDraft() {
    var period by remember { mutableStateOf("月") }
    ChartTitle("资产走势", "净资产变化与阶段高低点", listOf("月", "年"), period) { period = it }
    LedgerAmountText("¥ 128,420.68", Modifier.fillMaxWidth())
    Text("较上个${period}度  +¥ 6,280.40", color = NothingLedgerTheme.colors.income, style = MaterialTheme.typography.labelLarge)
    Spacer(Modifier.height(LedgerSpacing.md))
    ChartCard("净资产 / $period") {
        TrendLineChart(listOf(.20f, .34f, .29f, .46f, .52f, .49f, .68f, .76f, .71f, .88f))
        AxisLabels(if (period == "月") listOf("01", "05", "10", "15", "20", "25", "30") else listOf("20", "21", "22", "23", "24", "25", "26"))
        ChartLegend("资产", NothingLedgerTheme.colors.asset, "当前", NothingLedgerTheme.colors.warning)
    }
}

@Composable
internal fun IncomeExpenseCompareDraft() {
    var period by remember { mutableStateOf("月") }
    ChartTitle("收支对比", "相同周期中的收入与支出", listOf("月", "年"), period) { period = it }
    ChartCard("最近 6 个${period}度") {
        val labels = if (period == "月") listOf("06", "07", "08", "09", "10", "11") else listOf("21", "22", "23", "24", "25", "26")
        labels.forEachIndexed { index, label ->
            ComparisonRow(label, .48f + (index % 3) * .14f, .30f + ((index + 1) % 4) * .11f)
            if (index != labels.lastIndex) Spacer(Modifier.height(LedgerSpacing.sm))
        }
        Spacer(Modifier.height(LedgerSpacing.md))
        ChartLegend("收入", NothingLedgerTheme.colors.income, "支出", NothingLedgerTheme.colors.expense)
    }
}

@Composable
internal fun ExpenseShareDraft() {
    var period by remember { mutableStateOf("月") }
    ChartTitle("支出占比", "支出分类结构", listOf("月", "年"), period) { period = it }
    ChartCard("${period}度支出  ¥ 3,284") {
        ExpenseDonutChart()
        Spacer(Modifier.height(LedgerSpacing.md))
        SHARE_DATA.forEachIndexed { index, item ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                val colors = listOf(
                    NothingLedgerTheme.colors.expense,
                    NothingLedgerTheme.colors.categoryA,
                    NothingLedgerTheme.colors.categoryB,
                    NothingLedgerTheme.colors.categoryC,
                    NothingLedgerTheme.colors.transfer,
                )
                Text("■  ${item.first}", color = colors[index])
                Text("${item.second}%", color = NothingLedgerTheme.colors.contentMuted, style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.height(LedgerSpacing.xs))
        }
    }
}

@Composable
internal fun IncomeExpenseHeatmapDraft() {
    var period by remember { mutableStateOf("月") }
    ChartTitle("收支热力", "颜色表示每天的收支活跃程度", listOf("月", "年"), period) { period = it }
    ChartSummary("活跃天数", if (period == "月") "23 天" else "286 天", "最高支出", "11 / 02")
    ChartCard("${period}度日历热力") {
        HeatmapChart(if (period == "月") 7 else 18)
        Spacer(Modifier.height(LedgerSpacing.md))
        ChartLegend("收入较多", NothingLedgerTheme.colors.income, "支出较多", NothingLedgerTheme.colors.expense)
        Text("颜色越亮，表示当天金额或频率越高。", color = NothingLedgerTheme.colors.contentMuted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ChartTitle(
    title: String,
    subtitle: String,
    periods: List<String>,
    selected: String,
    onSelected: (String) -> Unit,
) {
    Text(title, color = Color.White, style = MaterialTheme.typography.titleLarge)
    Text(subtitle, color = NothingLedgerTheme.colors.contentMuted, style = MaterialTheme.typography.bodyMedium)
    Spacer(Modifier.height(LedgerSpacing.md))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.xs)) {
        periods.forEach { period ->
            LedgerControl(period, { onSelected(period) }, Modifier.weight(1f), accent = period == selected)
        }
    }
    Spacer(Modifier.height(LedgerSpacing.lg))
}

@Composable
private fun ChartSummary(leftLabel: String, leftValue: String, rightLabel: String, rightValue: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.xs)) {
        SummaryCell(leftLabel, leftValue, Modifier.weight(1f))
        SummaryCell(rightLabel, rightValue, Modifier.weight(1f), accent = true)
    }
    Spacer(Modifier.height(LedgerSpacing.md))
}

@Composable
private fun SummaryCell(label: String, value: String, modifier: Modifier, accent: Boolean = false) {
    LedgerSurface(modifier) {
        Column(Modifier.padding(LedgerSpacing.sm)) {
            Text(label, color = NothingLedgerTheme.colors.contentMuted, style = MaterialTheme.typography.labelMedium)
            Text(value, color = if (accent) NothingLedgerTheme.colors.expense else NothingLedgerTheme.colors.income, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun ChartCard(title: String, content: @Composable () -> Unit) {
    LedgerSurface(Modifier.fillMaxWidth(), elevated = true) {
        Column(Modifier.fillMaxWidth().padding(LedgerSpacing.md)) {
            Text(title, color = Color.White, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(LedgerSpacing.md))
            content()
        }
    }
}

@Composable
private fun PairedBarChart(income: List<Float>, expense: List<Float>) {
    val incomeColor = NothingLedgerTheme.colors.income
    val expenseColor = NothingLedgerTheme.colors.expense
    val grid = NothingLedgerTheme.colors.border
    Canvas(Modifier.fillMaxWidth().height(220.dp)) {
        repeat(4) { line ->
            val y = size.height * line / 3f
            drawLine(grid, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
        }
        val slot = size.width / income.size
        val barWidth = slot * .23f
        income.indices.forEach { index ->
            val center = slot * (index + .5f)
            drawRect(incomeColor, Offset(center - barWidth - 2.dp.toPx(), size.height * (1f - income[index])), Size(barWidth, size.height * income[index]))
            drawRect(expenseColor, Offset(center + 2.dp.toPx(), size.height * (1f - expense[index])), Size(barWidth, size.height * expense[index]))
        }
    }
}

@Composable
private fun TrendLineChart(values: List<Float>) {
    val asset = NothingLedgerTheme.colors.asset
    val current = NothingLedgerTheme.colors.warning
    val grid = NothingLedgerTheme.colors.border
    Canvas(Modifier.fillMaxWidth().height(240.dp)) {
        repeat(4) { line ->
            val y = size.height * line / 3f
            drawLine(grid, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
        }
        val path = Path()
        values.forEachIndexed { index, value ->
            val point = Offset(size.width * index / values.lastIndex, size.height * (1f - value))
            if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
        }
        drawPath(path, asset, style = Stroke(width = 3.dp.toPx()))
        values.forEachIndexed { index, value ->
            val point = Offset(size.width * index / values.lastIndex, size.height * (1f - value))
            drawRect(if (index == values.lastIndex) current else asset, point - Offset(4.dp.toPx(), 4.dp.toPx()), Size(8.dp.toPx(), 8.dp.toPx()))
        }
    }
}

@Composable
private fun ComparisonRow(label: String, incomeRatio: Float, expenseRatio: Float) {
    val incomeColor = NothingLedgerTheme.colors.income
    val expenseColor = NothingLedgerTheme.colors.expense
    val border = NothingLedgerTheme.colors.border
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.sm)) {
        Text(label, color = NothingLedgerTheme.colors.contentMuted, style = MaterialTheme.typography.labelMedium)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Canvas(Modifier.fillMaxWidth().height(8.dp)) {
                drawRect(border)
                drawRect(incomeColor, size = Size(size.width * incomeRatio, size.height))
            }
            Canvas(Modifier.fillMaxWidth().height(8.dp)) {
                drawRect(border)
                drawRect(expenseColor, size = Size(size.width * expenseRatio, size.height))
            }
        }
    }
}

@Composable
private fun ExpenseDonutChart() {
    val colors = listOf(
        NothingLedgerTheme.colors.expense,
        NothingLedgerTheme.colors.categoryA,
        NothingLedgerTheme.colors.categoryB,
        NothingLedgerTheme.colors.categoryC,
        NothingLedgerTheme.colors.transfer,
    )
    Canvas(Modifier.fillMaxWidth().height(240.dp)) {
        val diameter = minOf(size.width, size.height) * .82f
        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        var start = -90f
        SHARE_DATA.forEachIndexed { index, item ->
            val sweep = item.second * 3.6f
            drawArc(colors[index], start, sweep - 2f, false, topLeft, Size(diameter, diameter), style = Stroke(26.dp.toPx()))
            start += sweep
        }
    }
}

@Composable
private fun HeatmapChart(columns: Int) {
    val expenseColor = NothingLedgerTheme.colors.expense
    val incomeColor = NothingLedgerTheme.colors.income
    val border = NothingLedgerTheme.colors.border
    Canvas(Modifier.fillMaxWidth().height(if (columns == 7) 240.dp else 180.dp)) {
        val rows = 7
        val gap = 4.dp.toPx()
        val cell = minOf((size.width - gap * (columns - 1)) / columns, (size.height - gap * (rows - 1)) / rows)
        repeat(columns) { column ->
            repeat(rows) { row ->
                val seed = (column * 11 + row * 7 + 3) % 9
                val active = seed > 1
                val isIncome = seed % 4 == 0
                val alpha = if (active) .24f + seed / 12f else 1f
                drawRect(
                    color = if (!active) border else if (isIncome) incomeColor.copy(alpha = alpha) else expenseColor.copy(alpha = alpha),
                    topLeft = Offset(column * (cell + gap), row * (cell + gap)),
                    size = Size(cell, cell),
                )
            }
        }
    }
}

@Composable
private fun AxisLabels(labels: List<String>) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        labels.forEach { Text(it, color = NothingLedgerTheme.colors.contentMuted, style = MaterialTheme.typography.labelSmall) }
    }
    Spacer(Modifier.height(LedgerSpacing.md))
}

@Composable
private fun ChartLegend(left: String, leftColor: Color, right: String, rightColor: Color) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.lg)) {
        Text("■  $left", color = leftColor, style = MaterialTheme.typography.labelMedium)
        Text("■  $right", color = rightColor, style = MaterialTheme.typography.labelMedium)
    }
    Spacer(Modifier.height(LedgerSpacing.xs))
}

private val SHARE_DATA = listOf("餐饮" to 38, "居家" to 24, "交通" to 17, "娱乐" to 13, "其他" to 8)
