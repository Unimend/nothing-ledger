package com.unimend.nothingledger

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.unimend.nothingledger.accounts.AccountListItem
import com.unimend.nothingledger.accounts.AccountsState
import com.unimend.nothingledger.design.LedgerControl
import com.unimend.nothingledger.design.LedgerSurface
import com.unimend.nothingledger.design.NothingLedgerTheme

@Composable
internal fun AccountsSection(
    accounts: AccountsState,
    editor: AccountEditorState,
    onBeginCreate: () -> Unit,
    onBeginRename: (AccountListItem) -> Unit,
    onNameChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onArchive: (AccountListItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("ACCOUNTS", color = Color.White, style = MaterialTheme.typography.titleMedium)
            if (editor.editor == AccountEditor.Closed) {
                LedgerControl("NEW", onBeginCreate, Modifier)
            }
        }
        Spacer(Modifier.height(10.dp))
        when (accounts) {
            AccountsState.Loading -> Hint("READING / ACCOUNTS")
            is AccountsState.Failure -> Hint("ERROR / ${accounts.message.take(22)}", true)
            is AccountsState.Content -> {
                if (accounts.active.isEmpty()) Hint("NO ACTIVE ACCOUNTS")
                accounts.active.forEach { item ->
                    AccountRow(item, onBeginRename, onArchive)
                    Spacer(Modifier.height(6.dp))
                }
                if (accounts.archived.isNotEmpty()) {
                    Text("ARCHIVED / ${accounts.archived.size}", color = Color(0xFF666666), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        if (editor.editor != AccountEditor.Closed) {
            Spacer(Modifier.height(10.dp))
            AccountEditorCard(editor, onNameChange, onSave, onCancel)
        }
    }
}

@Composable
private fun AccountRow(
    item: AccountListItem,
    onRename: (AccountListItem) -> Unit,
    onArchive: (AccountListItem) -> Unit,
) {
    LedgerSurface(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(item.account.name, color = Color.White, style = MaterialTheme.typography.bodyLarge)
                    Text(item.account.kind.name, color = Color(0xFF777777), style = MaterialTheme.typography.labelMedium)
                }
                Text(formatBalance(item), color = Color.White, style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LedgerControl("RENAME", { onRename(item) }, Modifier.weight(1f))
                LedgerControl("ARCHIVE", { onArchive(item) }, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun AccountEditorCard(
    state: AccountEditorState,
    onNameChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    val name = when (val editor = state.editor) {
        AccountEditor.Closed -> ""
        is AccountEditor.Create -> editor.name
        is AccountEditor.Rename -> editor.name
    }
    LedgerSurface(Modifier.fillMaxWidth(), elevated = true) {
        Column(Modifier.padding(14.dp)) {
            Text(
                if (state.editor is AccountEditor.Create) "NEW ACCOUNT" else "RENAME ACCOUNT",
                color = Color.White, style = MaterialTheme.typography.labelLarge,
            )
            Spacer(Modifier.height(10.dp))
            BasicTextField(
                value = name,
                onValueChange = onNameChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NothingLedgerTheme.colors.background)
                    .border(1.dp, NothingLedgerTheme.colors.border)
                    .padding(14.dp),
                textStyle = TextStyle(color = Color.White, fontSize = MaterialTheme.typography.bodyLarge.fontSize),
                singleLine = true,
                decorationBox = { field ->
                    Box {
                        if (name.isEmpty()) Text("账户名称", color = Color(0xFF666666), style = MaterialTheme.typography.bodyLarge)
                        field()
                    }
                },
            )
            state.error?.let { Text(it, color = Color(0xFFFF2D2D), style = MaterialTheme.typography.bodyMedium) }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LedgerControl("CANCEL", onCancel, Modifier.weight(1f), enabled = !state.busy)
                LedgerControl("SAVE", onSave, Modifier.weight(1f), accent = true, enabled = !state.busy && name.isNotBlank())
            }
        }
    }
}

@Composable
private fun Hint(text: String, alert: Boolean = false) {
    Text(
        text, Modifier.fillMaxWidth().padding(18.dp),
        color = if (alert) Color(0xFFFF2D2D) else Color(0xFF777777),
        style = MaterialTheme.typography.labelMedium,
    )
}

private fun formatBalance(item: AccountListItem): String {
    val value = item.currentBalance.minorUnits
    val sign = if (value < 0) "−" else ""
    val absolute = kotlin.math.abs(value)
    val major = absolute.toDouble() / 100.0
    val compact = when {
        major >= 1_000_000_000 -> "%.2fB".format(major / 1_000_000_000)
        major >= 1_000_000 -> "%.2fM".format(major / 1_000_000)
        major >= 100_000 -> "%.1fK".format(major / 1_000)
        else -> null
    }
    if (compact != null) return "$sign¥$compact"
    return "$sign¥%,d.%02d".format(absolute / 100, absolute % 100)
}
