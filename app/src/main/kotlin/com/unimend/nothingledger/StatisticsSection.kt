package com.unimend.nothingledger

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.unimend.nothingledger.design.LedgerSurface
import com.unimend.nothingledger.design.NothingLedgerTheme
import com.unimend.nothingledger.timeline.CurrencyMonthStatistics
import com.unimend.nothingledger.timeline.TimelineState
import com.unimend.nothingledger.timeline.TimelineStatisticsState
import com.unimend.nothingledger.timeline.toStatistics
import java.time.ZoneId
import kotlin.math.max

@Composable
internal fun StatisticsSection(state: TimelineState, modifier: Modifier = Modifier) {
    val statistics = remember(state) { state.toStatistics(ZoneId.systemDefault()) }
    Column(modifier.fillMaxWidth()) {
        Text("本月统计", color = Color.White, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        when (statistics) {
            is TimelineStatisticsState.Loading -> StatisticsMessage("正在整理账单")
            is TimelineStatisticsState.Failure -> StatisticsMessage("统计读取失败")
            is TimelineStatisticsState.Ready -> if (statistics.value.currencies.isEmpty()) {
                StatisticsMessage("本月还没有账单")
            } else {
                statistics.value.currencies.forEach { currency ->
                    CurrencyStatistics(currency)
                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun CurrencyStatistics(statistics: CurrencyMonthStatistics) {
    Text(statistics.currencyCode, color = NothingLedgerTheme.colors.contentMuted, style = MaterialTheme.typography.labelMedium)
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatisticCard("支出", statistics.overview.expenseMinor, Modifier.weight(1f), true)
        StatisticCard("收入", statistics.overview.incomeMinor, Modifier.weight(1f), false)
        StatisticCard("结余", statistics.overview.netMinor, Modifier.weight(1f), false)
    }
    Spacer(Modifier.height(20.dp))
    Text("每日支出", color = Color.White, style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(10.dp))
    DailyExpenseBars(statistics)
}

@Composable
private fun StatisticCard(label: String, minor: Long, modifier: Modifier, accent: Boolean) {
    LedgerSurface(modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(label, color = NothingLedgerTheme.colors.contentMuted, style = MaterialTheme.typography.labelMedium)
            Text(
                formatCompactStatisticsAmount(minor),
                color = if (accent) NothingLedgerTheme.colors.accent else Color.White,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun DailyExpenseBars(statistics: CurrencyMonthStatistics) {
    val expenses = remember(statistics.dailyTrend) {
        statistics.dailyTrend
            .filter { it.expenseMinor > 0 }
            .sortedByDescending { it.date }
            .take(10)
    }
    if (expenses.isEmpty()) {
        StatisticsMessage("本月还没有支出")
        return
    }
    val largest = max(1L, expenses.maxOf { it.expenseMinor })
    LedgerSurface(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            expenses.forEach { day ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${day.date.dayOfMonth}日", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                    Text(formatStatisticsAmount(day.expenseMinor), color = NothingLedgerTheme.colors.accent, style = MaterialTheme.typography.bodyMedium)
                }
                Box(Modifier.fillMaxWidth().height(6.dp).background(NothingLedgerTheme.colors.border)) {
                    Box(
                        Modifier
                            .fillMaxWidth((day.expenseMinor.toFloat() / largest.toFloat()).coerceIn(0.04f, 1f))
                            .height(6.dp)
                            .background(NothingLedgerTheme.colors.accent),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatisticsMessage(message: String) {
    LedgerSurface(Modifier.fillMaxWidth()) {
        Text(message, Modifier.padding(20.dp), color = NothingLedgerTheme.colors.contentMuted)
    }
}

private fun formatStatisticsAmount(minor: Long): String {
    val sign = if (minor < 0) "−" else ""
    val absolute = kotlin.math.abs(minor)
    return "$sign¥%,d.%02d".format(absolute / 100, absolute % 100)
}

private fun formatCompactStatisticsAmount(minor: Long): String {
    val sign = if (minor < 0) "−" else ""
    val major = kotlin.math.abs(minor).toDouble() / 100.0
    val compact = when {
        major >= 1_000_000_000 -> "%.2fB".format(major / 1_000_000_000)
        major >= 1_000_000 -> "%.2fM".format(major / 1_000_000)
        major >= 100_000 -> "%.1fK".format(major / 1_000)
        else -> return formatStatisticsAmount(minor)
    }
    return "$sign¥$compact"
}
