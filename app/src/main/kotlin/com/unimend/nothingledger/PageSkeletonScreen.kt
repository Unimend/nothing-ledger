package com.unimend.nothingledger

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
import com.unimend.nothingledger.design.DitherField
import com.unimend.nothingledger.design.LedgerControl
import com.unimend.nothingledger.design.LedgerScreen
import com.unimend.nothingledger.design.NothingLedgerTheme
import com.unimend.nothingledger.shell.PageDestination

/** Shared placeholder for a route whose business implementation has not been connected yet. */
@Composable
internal fun PageSkeletonScreen(destination: PageDestination, onBack: () -> Unit) {
    val copy = destination.skeletonCopy()
    LedgerScreen {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(copy.title, color = Color.White, style = MaterialTheme.typography.titleLarge)
            LedgerControl("返回", onBack)
        }
        Spacer(Modifier.height(28.dp))
        Text("PAGE / READY", color = NothingLedgerTheme.colors.accent, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(10.dp))
        Text(copy.description, color = NothingLedgerTheme.colors.contentMuted, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(28.dp))
        DitherField(Modifier.fillMaxWidth().height(96.dp), color = NothingLedgerTheme.colors.border)
    }
}

private data class SkeletonCopy(val title: String, val description: String)

private fun PageDestination.skeletonCopy(): SkeletonCopy = when (this) {
    PageDestination.Search -> SkeletonCopy("搜索流水", "页面入口和返回路径已经接通，筛选与搜索功能将在后续阶段实现。")
    is PageDestination.TransactionEdit -> SkeletonCopy("编辑账单", "编辑页面骨架已经建立，后续会复用新增记账的字段组件。")
    is PageDestination.DayTransactions -> SkeletonCopy("当日流水", "日期参数已经进入页面路由，后续接入对应日期的统一流水列表。")
    is PageDestination.AccountDetail -> SkeletonCopy("账户详情", "账户参数已经进入页面路由，后续接入余额和账户流水。")
    PageDestination.Settings -> SkeletonCopy("设置", "设置页面入口已接通，功能选项将在后续逐项接入。")
    PageDestination.DataManagement -> SkeletonCopy("数据管理", "导入、导出和备份将在这里统一管理。")
    PageDestination.QuickEntry -> SkeletonCopy("新增记账", "新增记账页面正在加载。")
    is PageDestination.TransactionDetail -> SkeletonCopy("账单详情", "账单详情页面正在加载。")
    PageDestination.CategoryManagement -> SkeletonCopy("分类管理", "分类管理页面正在加载。")
}
