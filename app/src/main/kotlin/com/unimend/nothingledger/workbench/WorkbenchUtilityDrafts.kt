package com.unimend.nothingledger.workbench

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
import androidx.compose.ui.graphics.Color
import com.unimend.nothingledger.design.LedgerAmountText
import com.unimend.nothingledger.design.LedgerControl
import com.unimend.nothingledger.design.LedgerSpacing
import com.unimend.nothingledger.design.LedgerSurface
import com.unimend.nothingledger.design.NothingLedgerTheme

@Composable
internal fun TagsDraft() {
    Text("标签", color = Color.White, style = MaterialTheme.typography.titleLarge)
    Text(
        "标签用于补充分类：一笔账可以有多个标签，分类仍然只有一个。",
        color = NothingLedgerTheme.colors.contentMuted,
        style = MaterialTheme.typography.bodyMedium,
    )
    Spacer(Modifier.height(LedgerSpacing.lg))
    LedgerControl("＋ 新建标签", {}, Modifier.fillMaxWidth(), accent = true)
    Spacer(Modifier.height(LedgerSpacing.lg))
    Text("常用标签", color = Color.White, style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(LedgerSpacing.sm))
    TAG_SAMPLES.chunked(2).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.xs)) {
            row.forEach { tag -> TagSample(tag, Modifier.weight(1f)) }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
        Spacer(Modifier.height(LedgerSpacing.xs))
    }
    Spacer(Modifier.height(LedgerSpacing.lg))
    Text("账单中的组合效果", color = Color.White, style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(LedgerSpacing.sm))
    LedgerSurface(Modifier.fillMaxWidth(), elevated = true) {
        Column(Modifier.padding(LedgerSpacing.md), verticalArrangement = Arrangement.spacedBy(LedgerSpacing.xs)) {
            Text("午餐", color = Color.White, style = MaterialTheme.typography.titleMedium)
            Text("餐饮 · 微信钱包", color = NothingLedgerTheme.colors.contentMuted)
            Text("# 工作日   # 公司附近", color = NothingLedgerTheme.colors.accent, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun TagSample(tag: TagSample, modifier: Modifier) {
    LedgerSurface(modifier) {
        Column(Modifier.fillMaxWidth().padding(LedgerSpacing.sm)) {
            Text("■  ${tag.name}", color = if (tag.accent) NothingLedgerTheme.colors.accent else Color.White)
            Text("${tag.usage} 笔", color = NothingLedgerTheme.colors.contentMuted, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
internal fun MiniNumericKeypadDraft() {
    var amount by remember { mutableStateOf("128.00") }
    Text("迷你数字键盘", color = Color.White, style = MaterialTheme.typography.titleLarge)
    Text(
        "用于记账页底部的金额输入，不唤起系统键盘。",
        color = NothingLedgerTheme.colors.contentMuted,
        style = MaterialTheme.typography.bodyMedium,
    )
    Spacer(Modifier.height(LedgerSpacing.lg))
    LedgerSurface(Modifier.fillMaxWidth(), elevated = true) {
        Column(Modifier.fillMaxWidth().padding(LedgerSpacing.md)) {
            Text("支出金额", color = NothingLedgerTheme.colors.contentMuted)
            Spacer(Modifier.height(LedgerSpacing.xs))
            LedgerAmountText("¥ $amount", Modifier.fillMaxWidth(), accent = true)
        }
    }
    Spacer(Modifier.height(LedgerSpacing.md))
    KEYPAD_ROWS.forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.xs)) {
            row.forEach { label ->
                LedgerControl(
                    label = label,
                    onClick = { amount = updateDraftAmount(amount, label) },
                    modifier = Modifier.weight(1f),
                    accent = label == "完成",
                )
            }
        }
        Spacer(Modifier.height(LedgerSpacing.xs))
    }
    Text(
        "预览规则：数字直接追加；删除只影响当前金额；完成暂不提交数据。",
        color = NothingLedgerTheme.colors.contentMuted,
        style = MaterialTheme.typography.bodySmall,
    )
}

private fun updateDraftAmount(current: String, key: String): String = when (key) {
    "删除" -> current.dropLast(1).ifEmpty { "0" }
    "清空" -> "0"
    "." -> if ('.' in current) current else "$current."
    in listOf("＋", "－", "×", "÷", "完成") -> current
    else -> if (current == "0") key else (current + key).take(10)
}

private data class TagSample(val name: String, val usage: Int, val accent: Boolean = false)

private val TAG_SAMPLES = listOf(
    TagSample("工作日", 28, true),
    TagSample("公司附近", 16),
    TagSample("可报销", 9, true),
    TagSample("家庭共同", 21),
    TagSample("旅行", 12),
    TagSample("冲动消费", 7, true),
)

private val KEYPAD_ROWS = listOf(
    listOf("7", "8", "9", "删除"),
    listOf("4", "5", "6", "＋"),
    listOf("1", "2", "3", "－"),
    listOf("清空", "0", ".", "完成"),
)
