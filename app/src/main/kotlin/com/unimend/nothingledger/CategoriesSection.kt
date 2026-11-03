package com.unimend.nothingledger

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.unimend.nothingledger.categories.CategoryAction
import com.unimend.nothingledger.categories.CategoryListState
import com.unimend.nothingledger.categories.CategoryRow
import com.unimend.nothingledger.design.LedgerControl
import com.unimend.nothingledger.design.LedgerSpacing
import com.unimend.nothingledger.design.LedgerSurface
import com.unimend.nothingledger.design.NothingLedgerTheme
import com.unimend.nothingledger.model.CategoryId
import com.unimend.nothingledger.model.CategoryType

@Composable
internal fun CategoriesSection(
    state: CategoryListState,
    commandError: String?,
    onAction: (CategoryAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var draftName by remember { mutableStateOf("") }
    var draftParentId by remember { mutableStateOf<CategoryId?>(null) }
    var editingId by remember { mutableStateOf<CategoryId?>(null) }
    var editingName by remember { mutableStateOf("") }
    val query = state.selectedQuery()

    Column(modifier.fillMaxWidth()) {
        Text("CATEGORIES", color = Color.White, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(LedgerSpacing.xs))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.xs)) {
            LedgerControl(
                label = "EXPENSE",
                onClick = { onAction(CategoryAction.SelectType(CategoryType.EXPENSE)) },
                modifier = Modifier.weight(1f),
                accent = query.type == CategoryType.EXPENSE,
            )
            LedgerControl(
                label = "INCOME",
                onClick = { onAction(CategoryAction.SelectType(CategoryType.INCOME)) },
                modifier = Modifier.weight(1f),
                accent = query.type == CategoryType.INCOME,
            )
        }
        Spacer(Modifier.height(LedgerSpacing.xs))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            CategoryTextInput(
                value = draftName,
                onValueChange = { draftName = it.take(40) },
                placeholder = if (draftParentId == null) "NEW CATEGORY" else "NEW SUBCATEGORY",
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(LedgerSpacing.xs))
            LedgerControl(
                label = "ADD",
                enabled = draftName.isNotBlank(),
                onClick = {
                    onAction(CategoryAction.Create(draftName, draftParentId))
                    draftName = ""
                    draftParentId = null
                },
            )
        }
        if (draftParentId != null) {
            LedgerControl(
                label = "CANCEL SUBCATEGORY",
                onClick = { draftParentId = null },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            )
        }
        commandError?.let { message ->
            Text(
                text = "ERROR / $message",
                color = NothingLedgerTheme.colors.accent,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(vertical = LedgerSpacing.xs),
            )
        }
        Spacer(Modifier.height(LedgerSpacing.sm))
        when (state) {
            is CategoryListState.Loading -> CategoryMessage("READING / CATEGORIES")
            is CategoryListState.Empty -> CategoryMessage("NO CATEGORIES / YET")
            is CategoryListState.Failure -> CategoryMessage("ERROR / ${state.message.take(36)}", alert = true)
            is CategoryListState.Content -> state.rows.forEach { row ->
                if (editingId == row.category.id) {
                    RenameRow(
                        value = editingName,
                        onValueChange = { editingName = it.take(40) },
                        onSave = {
                            onAction(CategoryAction.Rename(row.category.id, editingName))
                            editingId = null
                        },
                        onCancel = { editingId = null },
                    )
                } else {
                    CategoryItem(
                        row = row,
                        onSubcategory = {
                            draftParentId = row.category.id
                            draftName = ""
                        },
                        onRename = {
                            editingId = row.category.id
                            editingName = row.category.name
                        },
                        onAction = onAction,
                    )
                }
                Spacer(Modifier.height(4.dp))
            }
        }
        LedgerControl(
            label = if (query.includeArchived) "HIDE INACTIVE" else "SHOW INACTIVE",
            onClick = { onAction(CategoryAction.SetArchivedVisible(!query.includeArchived)) },
            modifier = Modifier.fillMaxWidth().padding(top = LedgerSpacing.xs),
        )
    }
}

@Composable
private fun CategoryItem(
    row: CategoryRow,
    onSubcategory: () -> Unit,
    onRename: () -> Unit,
    onAction: (CategoryAction) -> Unit,
) {
    LedgerSurface(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = (if (row.depth == 1) "·  " else "") + row.category.name,
                    color = if (row.category.isArchived) NothingLedgerTheme.colors.contentMuted else Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = if (row.category.isArchived) "INACTIVE" else row.category.type.name,
                    color = NothingLedgerTheme.colors.contentMuted,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                MiniControl("UP", Modifier.weight(1f)) {
                    onAction(CategoryAction.Move(row.category.id, CategoryAction.Direction.UP))
                }
                MiniControl("DOWN", Modifier.weight(1f)) {
                    onAction(CategoryAction.Move(row.category.id, CategoryAction.Direction.DOWN))
                }
                MiniControl("RENAME", Modifier.weight(1.35f), onClick = onRename)
                if (row.depth == 0 && !row.category.isArchived) {
                    MiniControl("SUB", Modifier.weight(1f), onClick = onSubcategory)
                }
                if (!row.category.isArchived) {
                    MiniControl("OFF", Modifier.weight(0.8f), accent = true) {
                        onAction(CategoryAction.Archive(row.category.id))
                    }
                }
            }
        }
    }
}

@Composable
private fun RenameRow(
    value: String,
    onValueChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        CategoryTextInput(value, onValueChange, "CATEGORY NAME", Modifier.weight(1f))
        Spacer(Modifier.width(4.dp))
        MiniControl("SAVE", Modifier.weight(0.55f), enabled = value.isNotBlank(), onClick = onSave)
        Spacer(Modifier.width(4.dp))
        MiniControl("CANCEL", Modifier.weight(0.65f), onClick = onCancel)
    }
}

@Composable
private fun MiniControl(
    label: String,
    modifier: Modifier,
    accent: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    LedgerControl(label, onClick, modifier.height(40.dp), accent, enabled)
}

@Composable
private fun CategoryTextInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .height(48.dp)
            .background(NothingLedgerTheme.colors.surfaceRaised)
            .padding(horizontal = 12.dp),
        textStyle = TextStyle(color = Color.White, fontSize = MaterialTheme.typography.bodyLarge.fontSize),
        singleLine = true,
        decorationBox = { inner ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (value.isEmpty()) {
                    Text(placeholder, color = NothingLedgerTheme.colors.contentMuted, style = MaterialTheme.typography.labelMedium)
                }
                inner()
            }
        },
    )
}

@Composable
private fun CategoryMessage(message: String, alert: Boolean = false) {
    Text(
        message,
        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
        color = if (alert) NothingLedgerTheme.colors.accent else NothingLedgerTheme.colors.contentMuted,
        style = MaterialTheme.typography.labelMedium,
    )
}

private fun CategoryListState.selectedQuery() = when (this) {
    is CategoryListState.Loading -> query
    is CategoryListState.Empty -> query
    is CategoryListState.Content -> query
    is CategoryListState.Failure -> query
}
