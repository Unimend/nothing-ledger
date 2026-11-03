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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.unimend.nothingledger.design.DitherField
import com.unimend.nothingledger.design.LedgerAmountText
import com.unimend.nothingledger.design.LedgerColorTokens
import com.unimend.nothingledger.design.LedgerControl
import com.unimend.nothingledger.design.LedgerRadius
import com.unimend.nothingledger.design.LedgerSpacing
import com.unimend.nothingledger.design.LedgerStatus
import com.unimend.nothingledger.design.LedgerStatusMarker
import com.unimend.nothingledger.design.LedgerSurface
import com.unimend.nothingledger.design.LedgerTextStyles
import com.unimend.nothingledger.design.NothingLedgerTheme

/**
 * The first, database-free component catalogue.
 *
 * Every production primitive must be proven here in its normal and edge states before a feature
 * screen may compose it. Samples are deliberately static so failures belong to design code rather
 * than repositories, clocks, navigation, or asynchronous state.
 */
@Composable
internal fun WorkbenchComponentCatalog(
    onAction: (WorkbenchSampleAction) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        CatalogHeader()
        CatalogSection("01 / 颜色令牌", "所有界面只能从这一组语义颜色读取。") {
            ColorTokenGrid()
        }
        CatalogSection("02 / 字体", "中文正文使用系统字形，数字与英文标签使用 Doto。") {
            TypographySamples()
        }
        CatalogSection("03 / 间距与圆角", "间距和圆角用同一套刻度，不在页面写临时数值。") {
            SpacingSamples()
        }
        CatalogSection("04 / 按钮与操作", "默认、强调、不可用与长文字必须同时成立。") {
            ControlSamples(onAction)
        }
        CatalogSection("05 / 内容容器", "普通层和抬高层只表达层级，不承担业务状态。") {
            SurfaceSamples()
        }
        CatalogSection("06 / 金额", "金额组件必须覆盖常规、负数、超长与强调状态。") {
            AmountSamples()
        }
        CatalogSection("07 / 状态", "加载、空白、成功和错误使用同一状态语言。") {
            StatusSamples()
        }
        CatalogSection("08 / 文字压力测试", "组件必须在真实中文、英文和超长内容下保持稳定。") {
            TextStressSamples()
        }
        Spacer(Modifier.height(LedgerSpacing.xxl))
    }
}

internal enum class WorkbenchSampleAction {
    NORMAL_CONTROL,
    ACCENT_CONTROL,
    LONG_CONTROL,
}

@Composable
private fun CatalogHeader() {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column {
            Text("NOTHING", color = Color.White, style = MaterialTheme.typography.titleLarge)
            Text("LEDGER / LAB", color = NothingLedgerTheme.colors.accent, style = MaterialTheme.typography.labelLarge)
        }
        LedgerStatusMarker("基础已建立", LedgerStatus.ACTIVE)
    }
    Spacer(Modifier.height(LedgerSpacing.sm))
    Text(
        "这里不是产品页面。它是所有基础组件唯一的设计、极端状态和交互检验入口。",
        color = NothingLedgerTheme.colors.contentSecondary,
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun CatalogSection(
    title: String,
    description: String,
    content: @Composable () -> Unit,
) {
    Spacer(Modifier.height(LedgerSpacing.xl))
    Text(title, color = Color.White, style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(LedgerSpacing.xxs))
    Text(description, color = NothingLedgerTheme.colors.contentMuted, style = MaterialTheme.typography.bodyMedium)
    Spacer(Modifier.height(LedgerSpacing.sm))
    content()
}

@Composable
private fun ColorTokenGrid() {
    val colors = LedgerColorTokens.Dark
    val samples = listOf(
        "背景" to colors.background,
        "普通容器" to colors.surface,
        "抬高容器" to colors.surfaceRaised,
        "主要文字" to colors.contentPrimary,
        "次要文字" to colors.contentSecondary,
        "弱化文字" to colors.contentMuted,
        "边框" to colors.border,
        "强调色" to colors.accent,
    )
    Column(verticalArrangement = Arrangement.spacedBy(LedgerSpacing.xxs)) {
        samples.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.xxs)) {
                row.forEach { (label, color) -> ColorToken(label, color, Modifier.weight(1f)) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ColorToken(label: String, color: Color, modifier: Modifier = Modifier) {
    LedgerSurface(modifier.height(72.dp)) {
        Box(Modifier.fillMaxWidth().height(72.dp).background(color)) {
            Text(
                label,
                modifier = Modifier.align(Alignment.BottomStart).background(Color.Black.copy(alpha = 0.7f)).padding(6.dp),
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun TypographySamples() {
    LedgerSurface(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(LedgerSpacing.md), verticalArrangement = Arrangement.spacedBy(LedgerSpacing.xs)) {
            Text("56.00", color = Color.White, style = MaterialTheme.typography.displayLarge)
            Text("记一笔真实生活", color = Color.White, style = MaterialTheme.typography.headlineMedium)
            Text("账户与账单使用清晰的中文正文。", color = Color.White, style = MaterialTheme.typography.bodyLarge)
            Text("DOTO / 2026 / 11 / 03", color = NothingLedgerTheme.colors.accent, style = LedgerTextStyles.latinLabel)
            Text("辅助信息 / SECONDARY", color = NothingLedgerTheme.colors.contentMuted, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun SpacingSamples() {
    LedgerSurface(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(LedgerSpacing.md), verticalArrangement = Arrangement.spacedBy(LedgerSpacing.xs)) {
            listOf(
                "XXS / 4" to LedgerSpacing.xxs,
                "XS / 8" to LedgerSpacing.xs,
                "SM / 12" to LedgerSpacing.sm,
                "MD / 16" to LedgerSpacing.md,
                "LG / 24" to LedgerSpacing.lg,
                "XL / 32" to LedgerSpacing.xl,
            ).forEach { (label, size) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.height(8.dp).fillMaxWidth(size.value / 48f).background(NothingLedgerTheme.colors.accent))
                    Spacer(Modifier.weight(1f))
                    Text(label, color = NothingLedgerTheme.colors.contentSecondary, style = MaterialTheme.typography.labelMedium)
                }
            }
            Text(
                "圆角 / ${LedgerRadius.control.value.toInt()} · ${LedgerRadius.surface.value.toInt()} · ${LedgerRadius.prominent.value.toInt()}",
                color = NothingLedgerTheme.colors.contentMuted,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun ControlSamples(onAction: (WorkbenchSampleAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(LedgerSpacing.xs)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.xs)) {
            LedgerControl("普通", { onAction(WorkbenchSampleAction.NORMAL_CONTROL) }, Modifier.weight(1f))
            LedgerControl("强调", { onAction(WorkbenchSampleAction.ACCENT_CONTROL) }, Modifier.weight(1f), accent = true)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.xs)) {
            LedgerControl("不可用", {}, Modifier.weight(1f), enabled = false)
            LedgerControl("返回", {}, Modifier.weight(1f))
        }
        LedgerControl(
            "一个需要验证不会溢出的很长按钮名称",
            { onAction(WorkbenchSampleAction.LONG_CONTROL) },
            Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SurfaceSamples() {
    Column(verticalArrangement = Arrangement.spacedBy(LedgerSpacing.xs)) {
        LedgerSurface(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(LedgerSpacing.md)) {
                Text("普通容器", color = Color.White, style = MaterialTheme.typography.labelLarge)
                Text("列表、分组和普通信息容器。", color = NothingLedgerTheme.colors.contentMuted)
            }
        }
        LedgerSurface(Modifier.fillMaxWidth(), elevated = true) {
            Box(Modifier.fillMaxWidth().height(96.dp)) {
                DitherField(Modifier.fillMaxWidth().height(96.dp), color = NothingLedgerTheme.colors.border)
                Text(
                    "抬高容器",
                    modifier = Modifier.align(Alignment.Center).background(Color.Black).padding(8.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun AmountSamples() {
    LedgerSurface(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(LedgerSpacing.md), verticalArrangement = Arrangement.spacedBy(LedgerSpacing.xs)) {
            LedgerAmountText("¥ 128.00", Modifier.fillMaxWidth())
            LedgerAmountText("−¥ 6,449.18", Modifier.fillMaxWidth(), accent = true)
            LedgerAmountText("¥ 12,345,678,901.23", Modifier.fillMaxWidth())
            LedgerAmountText("¥ 0.00", Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
        }
    }
}

@Composable
private fun StatusSamples() {
    LedgerSurface(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(LedgerSpacing.md), verticalArrangement = Arrangement.spacedBy(LedgerSpacing.sm)) {
            LedgerStatusMarker("正常 / ACTIVE", LedgerStatus.ACTIVE)
            LedgerStatusMarker("读取中 / LOADING", LedgerStatus.MUTED)
            LedgerStatusMarker("暂无数据 / EMPTY", LedgerStatus.MUTED)
            LedgerStatusMarker("读取失败 / RETRY", LedgerStatus.ALERT)
        }
    }
}

@Composable
private fun TextStressSamples() {
    LedgerSurface(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(LedgerSpacing.md), verticalArrangement = Arrangement.spacedBy(LedgerSpacing.sm)) {
            Text("日常支出", color = Color.White, style = MaterialTheme.typography.bodyLarge)
            Text("DINING / TRANSPORT / HOME", color = Color.White, style = LedgerTextStyles.latinLabel)
            Text(
                "这是一个故意写得很长的账户或者分类名称，用来检查换行、边距以及内容不会覆盖右侧金额。",
                color = NothingLedgerTheme.colors.contentSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text("¥ 9,223,372,036,854,775.80", color = NothingLedgerTheme.colors.accent, style = LedgerTextStyles.amountCompact)
        }
    }
}
