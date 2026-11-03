package com.unimend.nothingledger

import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.unimend.nothingledger.design.DitherField
import com.unimend.nothingledger.design.LedgerControl
import com.unimend.nothingledger.design.LedgerSpacing
import com.unimend.nothingledger.design.LedgerSurface
import com.unimend.nothingledger.design.LedgerScreen
import com.unimend.nothingledger.design.NothingLedgerTheme
import com.unimend.nothingledger.workbench.WorkbenchBusinessCatalog
import com.unimend.nothingledger.workbench.WorkbenchComponentCatalog

/** Database-free catalog. Product pages may consume its components; the catalog never consumes product state. */
@Composable
internal fun WorkbenchScreen(
    onOpenProductPreview: () -> Unit,
    onExit: () -> Unit,
) {
    var state by remember { mutableStateOf(WorkbenchState()) }
    val dispatch: (WorkbenchAction) -> Unit = { state = WorkbenchReducer.reduce(state, it) }
    BackHandler {
        if (state.focusedSection != null) dispatch(WorkbenchAction.CloseFocus) else onExit()
    }

    val focused = state.focusedSection
    if (focused != null) {
        FocusedWorkbenchSection(focused, onBack = { dispatch(WorkbenchAction.CloseFocus) })
        return
    }

    LedgerScreen {
        WorkbenchHeader(onOpenProductPreview)
        Spacer(Modifier.height(LedgerSpacing.xl))
        WorkbenchComponentCatalog()
        WorkbenchGap()
        WorkbenchBusinessCatalog(onTransactionClick = {})
        WorkbenchGap()
        PageFramesSection { dispatch(WorkbenchAction.Focus(WorkbenchSection.PAGE_FRAMES)) }
        WorkbenchGap()
        InteractionTestsSection { dispatch(WorkbenchAction.Focus(WorkbenchSection.INTERACTION_TESTS)) }
        Spacer(Modifier.height(64.dp))
    }
}

@Composable
private fun WorkbenchHeader(onOpenProductPreview: () -> Unit) {
    Text("NOTHING LEDGER", color = Color.White, style = MaterialTheme.typography.titleLarge)
    Text("COMPONENT WORKBENCH / 00", color = NothingLedgerTheme.colors.accent, style = MaterialTheme.typography.labelLarge)
    Spacer(Modifier.height(12.dp))
    Text(
        "所有基础组件、业务组件、页面骨架与交互状态必须先在这里通过，再进入正式产品。",
        color = NothingLedgerTheme.colors.contentSecondary,
        style = MaterialTheme.typography.bodyLarge,
    )
    Spacer(Modifier.height(16.dp))
    LedgerControl("打开 PRODUCT PREVIEW", onOpenProductPreview, Modifier.fillMaxWidth())
}

@Composable
private fun PageFramesSection(onFocus: () -> Unit) {
    WorkbenchSectionHeader("04", "PAGE FRAMES", onFocus)
    listOf("流水", "日历", "统计", "账户", "账单详情", "账单编辑", "搜索", "设置与数据").chunked(2).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEach { title -> WorkbenchFrame(title, "PAGE / READY", Modifier.weight(1f)) }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun InteractionTestsSection(onFocus: () -> Unit) {
    var count by remember { mutableIntStateOf(0) }
    WorkbenchSectionHeader("05", "INTERACTION TESTS", onFocus)
    LedgerSurface(Modifier.fillMaxWidth(), elevated = true) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("TAP COUNT / $count", color = Color.White, style = MaterialTheme.typography.titleMedium)
            LedgerControl("点击反馈", { count += 1 }, Modifier.fillMaxWidth(), accent = true)
            DitherField(Modifier.fillMaxWidth().height(64.dp), color = Color(0xFF444444))
        }
    }
}

@Composable
private fun FocusedWorkbenchSection(section: WorkbenchSection, onBack: () -> Unit) {
    LedgerScreen {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(section.name.replace('_', ' '), color = Color.White, style = MaterialTheme.typography.titleLarge)
            LedgerControl("返回总览", onBack)
        }
        Spacer(Modifier.height(24.dp))
        when (section) {
            WorkbenchSection.DESIGN_TOKENS,
            WorkbenchSection.BASE_COMPONENTS -> WorkbenchComponentCatalog()
            WorkbenchSection.BUSINESS_COMPONENTS -> WorkbenchBusinessCatalog(onTransactionClick = {})
            WorkbenchSection.PAGE_FRAMES -> PageFramesSection(onFocus = {})
            WorkbenchSection.INTERACTION_TESTS -> InteractionTestsSection(onFocus = {})
        }
    }
}

@Composable
private fun WorkbenchSectionHeader(index: String, title: String, onFocus: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text(index, color = NothingLedgerTheme.colors.accent, style = MaterialTheme.typography.labelMedium)
            Text(title, color = Color.White, style = MaterialTheme.typography.titleMedium)
        }
        LedgerControl("FOCUS", onFocus)
    }
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun WorkbenchFrame(title: String, subtitle: String, modifier: Modifier = Modifier) {
    LedgerSurface(modifier) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text(title, color = Color.White, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(6.dp))
            Text(subtitle, color = NothingLedgerTheme.colors.contentMuted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun WorkbenchGap() {
    Spacer(Modifier.height(40.dp))
}
