package com.unimend.nothingledger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.unimend.nothingledger.design.DitherField
import com.unimend.nothingledger.design.LedgerAmountText
import com.unimend.nothingledger.design.LedgerControl
import com.unimend.nothingledger.design.LedgerScreen
import com.unimend.nothingledger.design.NothingLedgerTheme
import com.unimend.nothingledger.entry.EntryAction
import com.unimend.nothingledger.entry.EntryMode
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = (application as LedgerApplication).database
        setContent {
            val viewModel: EntryViewModel = viewModel(factory = EntryViewModel.factory(database))
            val uiState by viewModel.uiState.collectAsState()
            val timelineViewModel: TimelineViewModel = viewModel(factory = TimelineViewModel.factory(database))
            val timelineState by timelineViewModel.uiState.collectAsState()
            NothingLedgerTheme { EntryScreen(uiState, timelineState, viewModel::dispatch) }
        }
    }
}

@Composable
private fun EntryScreen(
    uiState: EntryUiState,
    timelineState: com.unimend.nothingledger.timeline.TimelineState,
    dispatch: (EntryAction) -> Unit,
) {
    val state = uiState.entry
    val status = uiState.errorMessage?.let { "ERROR / ${it.take(18)}" } ?: uiState.status.name

    LedgerScreen {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("NOTHING\nLEDGER", color = Color.White, style = MaterialTheme.typography.titleLarge)
                Text(status, color = Color(0xFFFF2D2D), style = MaterialTheme.typography.labelMedium)
            }
            Spacer(Modifier.height(24.dp))
            ModeRow(state.mode) { mode ->
                dispatch(EntryAction.ChangeMode(mode))
                when (mode) {
                    EntryMode.EXPENSE -> {
                        dispatch(EntryAction.SelectSourceAccount(LedgerDefaults.walletId))
                        dispatch(EntryAction.SelectCategory(LedgerDefaults.expenseCategoryId))
                    }
                    EntryMode.INCOME -> {
                        dispatch(EntryAction.SelectDestinationAccount(LedgerDefaults.walletId))
                        dispatch(EntryAction.SelectCategory(LedgerDefaults.incomeCategoryId))
                    }
                    EntryMode.TRANSFER -> dispatch(EntryAction.SelectSourceAccount(LedgerDefaults.walletId))
                }
            }
            Spacer(Modifier.height(24.dp))
            LedgerAmountText(formatAmount(state.amount.minorUnits), Modifier.fillMaxWidth())
            Text(
                when (state.mode) {
                    EntryMode.EXPENSE -> "微信钱包  /  日常支出"
                    EntryMode.INCOME -> "日常收入  /  微信钱包"
                    EntryMode.TRANSFER -> "微信钱包  →  选择目标账户"
                },
                Modifier.fillMaxWidth(), Color(0xFF8A8A8A),
                style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.End,
            )
            Spacer(Modifier.height(20.dp))
            Keypad { dispatch(EntryAction.EnterDigit(it)) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Key("DEL", Modifier.weight(1f)) { dispatch(EntryAction.BackspaceAmount) }
                Key("CLEAR", Modifier.weight(1f)) { dispatch(EntryAction.ClearAmount) }
                Key("SAVE", Modifier.weight(1f), true) { dispatch(EntryAction.RequestSave) }
            }
        state.validationErrors.firstOrNull()?.let {
            Text("CHECK / ${it.name}", color = Color(0xFFFF2D2D), style = MaterialTheme.typography.labelMedium)
        }
        Spacer(Modifier.height(32.dp))
        Box(Modifier.fillMaxWidth().height(72.dp)) {
            DitherField(Modifier.align(Alignment.CenterEnd).size(160.dp, 72.dp), color = Color(0xFF333333))
        }
        TimelineSection(timelineState)
    }
}

@Composable
private fun ModeRow(selected: EntryMode, onSelect: (EntryMode) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        EntryMode.entries.forEach { mode ->
            LedgerControl(mode.name, { onSelect(mode) }, Modifier.weight(1f), accent = mode == selected)
        }
    }
}

@Composable
private fun Keypad(onDigit: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9)).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { digit -> Key(digit.toString(), Modifier.weight(1f)) { onDigit(digit) } }
            }
        }
        Key("0", Modifier.fillMaxWidth()) { onDigit(0) }
    }
}

@Composable
private fun Key(label: String, modifier: Modifier, accent: Boolean = false, onClick: () -> Unit) {
    LedgerControl(label, onClick, modifier.height(52.dp), accent = accent)
}

private fun formatAmount(minorUnits: Long): String = "¥ %,d.%02d".format(minorUnits / 100, minorUnits % 100)

