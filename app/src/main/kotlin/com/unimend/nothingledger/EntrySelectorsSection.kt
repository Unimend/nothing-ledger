package com.unimend.nothingledger

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.unimend.nothingledger.design.LedgerControl
import com.unimend.nothingledger.design.LedgerSpacing
import com.unimend.nothingledger.design.NothingLedgerTheme
import com.unimend.nothingledger.entry.EntryAction
import com.unimend.nothingledger.entry.EntryMode
import com.unimend.nothingledger.entry.EntryOptionsState
import com.unimend.nothingledger.entry.EntryState
import com.unimend.nothingledger.model.Account
import com.unimend.nothingledger.model.Category

@Composable
internal fun EntrySelectorsSection(
    entry: EntryState,
    optionsState: EntryOptionsState,
    onAction: (EntryAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        Text("选择账户与分类", color = Color.White, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(LedgerSpacing.xs))
        when (optionsState) {
            EntryOptionsState.Loading -> SelectorMessage("正在读取账户和分类")
            is EntryOptionsState.Failure -> SelectorMessage("读取失败 / ${optionsState.message.take(36)}", alert = true)
            is EntryOptionsState.Ready -> when (entry.mode) {
                EntryMode.EXPENSE -> {
                    AccountSelector("付款账户", optionsState.options.sourceAccounts, entry.sourceAccountId) { account ->
                        onAction(EntryAction.SelectSourceAccount(account.id))
                    }
                    CategorySelector(optionsState.options.categories, entry) { category ->
                        onAction(EntryAction.SelectCategory(category.id))
                    }
                }
                EntryMode.INCOME -> {
                    AccountSelector("收款账户", optionsState.options.destinationAccounts, entry.destinationAccountId) { account ->
                        onAction(EntryAction.SelectDestinationAccount(account.id))
                    }
                    CategorySelector(optionsState.options.categories, entry) { category ->
                        onAction(EntryAction.SelectCategory(category.id))
                    }
                }
                EntryMode.TRANSFER -> {
                    AccountSelector("转出账户", optionsState.options.sourceAccounts, entry.sourceAccountId) { account ->
                        onAction(EntryAction.SelectSourceAccount(account.id))
                    }
                    AccountSelector("转入账户", optionsState.options.destinationAccounts, entry.destinationAccountId) { account ->
                        onAction(EntryAction.SelectDestinationAccount(account.id))
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountSelector(
    label: String,
    accounts: List<Account>,
    selectedId: com.unimend.nothingledger.model.AccountId?,
    onSelect: (Account) -> Unit,
) {
    SelectorGroup(
        label = label,
        items = accounts,
        itemLabel = Account::name,
        isSelected = { it.id == selectedId },
        onSelect = onSelect,
    )
}

@Composable
private fun CategorySelector(
    categories: List<Category>,
    entry: EntryState,
    onSelect: (Category) -> Unit,
) {
    val parents = categories.associateBy(Category::id)
    SelectorGroup(
        label = "分类",
        items = categories,
        itemLabel = { category ->
            category.parentId?.let { parentId ->
                parents[parentId]?.let { parent -> "${parent.name} / ${category.name}" }
            } ?: category.name
        },
        isSelected = { it.id == entry.categoryId },
        onSelect = onSelect,
    )
}

@Composable
private fun <T> SelectorGroup(
    label: String,
    items: List<T>,
    itemLabel: (T) -> String,
    isSelected: (T) -> Boolean,
    onSelect: (T) -> Unit,
) {
    Text(
        text = label,
        color = NothingLedgerTheme.colors.contentMuted,
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier.padding(top = LedgerSpacing.sm, bottom = 4.dp),
    )
    if (items.isEmpty()) {
        SelectorMessage("没有可用的$label")
        return
    }
    items.chunked(2).forEach { rowItems ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            rowItems.forEach { item ->
                LedgerControl(
                    label = itemLabel(item),
                    onClick = { onSelect(item) },
                    modifier = Modifier.weight(1f),
                    accent = isSelected(item),
                )
            }
            if (rowItems.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun SelectorMessage(message: String, alert: Boolean = false) {
    Text(
        text = message,
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        color = if (alert) NothingLedgerTheme.colors.accent else NothingLedgerTheme.colors.contentMuted,
        style = MaterialTheme.typography.labelMedium,
    )
}
