package com.unimend.nothingledger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.unimend.nothingledger.design.DitherField
import com.unimend.nothingledger.design.LedgerAmountText
import com.unimend.nothingledger.design.LedgerControl
import com.unimend.nothingledger.design.LedgerScreen
import com.unimend.nothingledger.design.NothingLedgerTheme
import com.unimend.nothingledger.accounts.AccountListItem
import com.unimend.nothingledger.accounts.AccountsState
import com.unimend.nothingledger.categories.CategoryAction
import com.unimend.nothingledger.categories.CategoryListState
import com.unimend.nothingledger.entry.EntryAction
import com.unimend.nothingledger.entry.EntryMode
import com.unimend.nothingledger.entry.EntryOptionsState
import com.unimend.nothingledger.shell.ShellAction
import com.unimend.nothingledger.shell.ShellDestination
import com.unimend.nothingledger.shell.ShellEffect
import com.unimend.nothingledger.shell.ShellReducer
import com.unimend.nothingledger.shell.ShellState
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = (application as LedgerApplication).database
        setContent {
            val viewModel: EntryViewModel = viewModel(factory = EntryViewModel.factory(database))
            val uiState by viewModel.uiState.collectAsState()
            val selectorsViewModel: EntrySelectorsViewModel = viewModel(factory = EntrySelectorsViewModel.factory(database))
            val entryOptions by selectorsViewModel.state.collectAsState()
            LaunchedEffect(uiState.entry) { selectorsViewModel.updateContext(uiState.entry) }
            val timelineViewModel: TimelineViewModel = viewModel(factory = TimelineViewModel.factory(database))
            val timelineState by timelineViewModel.uiState.collectAsState()
            val accountsViewModel: AccountsViewModel = viewModel(factory = AccountsViewModel.factory(database))
            val accountsState by accountsViewModel.accounts.collectAsState()
            val accountEditor by accountsViewModel.editor.collectAsState()
            val categoriesViewModel: CategoriesViewModel = viewModel(factory = CategoriesViewModel.factory(database))
            val categoriesState by categoriesViewModel.state.collectAsState()
            val categoryError by categoriesViewModel.commandError.collectAsState()
            val manageActions = remember(accountsViewModel, categoriesViewModel) {
                ManageActions(
                    beginAccountCreate = accountsViewModel::beginCreate,
                    beginAccountRename = { accountsViewModel.beginRename(it.account.id, it.account.name) },
                    changeAccountName = accountsViewModel::changeName,
                    saveAccount = accountsViewModel::save,
                    cancelAccount = accountsViewModel::cancel,
                    archiveAccount = { accountsViewModel.archive(it.account.id) },
                    categoryAction = categoriesViewModel::dispatch,
                )
            }
            NothingLedgerTheme {
                LedgerApp(
                    uiState = uiState,
                    entryOptions = entryOptions,
                    timelineState = timelineState,
                    manageState = ManageUiState(accountsState, accountEditor, categoriesState, categoryError),
                    entryAction = viewModel::dispatch,
                    manageActions = manageActions,
                    onExit = ::finish,
                )
            }
        }
    }
}

@Composable
private fun LedgerApp(
    uiState: EntryUiState,
    entryOptions: EntryOptionsState,
    timelineState: com.unimend.nothingledger.timeline.TimelineState,
    manageState: ManageUiState,
    entryAction: (EntryAction) -> Unit,
    manageActions: ManageActions,
    onExit: () -> Unit,
) {
    var destination by rememberSaveable { mutableStateOf(ShellDestination.QUICK_ENTRY) }
    fun navigate(action: ShellAction) {
        val transition = ShellReducer.reduce(ShellState(destination), action)
        destination = transition.state.destination
        if (transition.effect == ShellEffect.ExitRequested) onExit()
    }
    BackHandler { navigate(ShellAction.NavigateBack) }

    key(destination) {
        LedgerScreen {
            AppHeader(uiState, destination)
            Spacer(Modifier.height(16.dp))
            ShellNavigation(destination) {
                navigate(ShellAction.SelectDestination(it))
            }
            Spacer(Modifier.height(24.dp))
            when (destination) {
                ShellDestination.QUICK_ENTRY -> EntryContent(uiState, entryOptions, entryAction)
                ShellDestination.TIMELINE -> TimelineSection(timelineState)
                ShellDestination.MANAGE -> ManageContent(manageState, manageActions)
            }
        }
    }
}

@Composable
private fun AppHeader(uiState: EntryUiState, destination: ShellDestination) {
    val status = uiState.errorMessage?.let { "出错 / ${it.take(18)}" } ?: when (destination) {
        ShellDestination.QUICK_ENTRY -> when (uiState.status) {
            SaveStatus.INITIALIZING -> "准备中"
            SaveStatus.READY -> "可以记账"
            SaveStatus.SAVING -> "保存中"
            SaveStatus.SAVED -> "已记下"
            SaveStatus.ERROR -> "保存失败"
        }
        ShellDestination.TIMELINE -> "全部流水"
        ShellDestination.MANAGE -> "账户与分类"
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("NOTHING\nLEDGER", color = Color.White, style = MaterialTheme.typography.titleLarge)
        Text(status, color = Color(0xFFFF2D2D), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun ShellNavigation(selected: ShellDestination, onSelect: (ShellDestination) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            ShellDestination.QUICK_ENTRY to "记账",
            ShellDestination.TIMELINE to "流水",
            ShellDestination.MANAGE to "管理",
        ).forEach { (destination, label) ->
            LedgerControl(label, { onSelect(destination) }, Modifier.weight(1f), accent = destination == selected)
        }
    }
}

@Composable
private fun EntryContent(
    uiState: EntryUiState,
    entryOptions: EntryOptionsState,
    dispatch: (EntryAction) -> Unit,
) {
    val state = uiState.entry
    ModeRow(state.mode) { mode ->
        dispatch(EntryAction.ChangeMode(mode))
    }
    Spacer(Modifier.height(24.dp))
    LedgerAmountText("¥ ${state.amount.displayText}", Modifier.fillMaxWidth())
    EntrySelectorsSection(state, entryOptions, dispatch)
    Spacer(Modifier.height(20.dp))
    Keypad(
        onDigit = { dispatch(EntryAction.EnterDigit(it)) },
        onDecimal = { dispatch(EntryAction.EnterDecimalSeparator) },
        onBackspace = { dispatch(EntryAction.BackspaceAmount) },
        onClear = { dispatch(EntryAction.ClearAmount) },
        onSave = { dispatch(EntryAction.RequestSave) },
    )
    state.validationErrors.firstOrNull()?.let {
        Text("CHECK / ${it.name}", color = Color(0xFFFF2D2D), style = MaterialTheme.typography.labelMedium)
    }
    Spacer(Modifier.height(32.dp))
    Box(Modifier.fillMaxWidth().height(72.dp)) {
        DitherField(Modifier.align(Alignment.CenterEnd).size(160.dp, 72.dp), color = Color(0xFF333333))
    }
}

private data class ManageUiState(
    val accounts: AccountsState,
    val accountEditor: AccountEditorState,
    val categories: CategoryListState,
    val categoryError: String?,
)

private data class ManageActions(
    val beginAccountCreate: () -> Unit,
    val beginAccountRename: (AccountListItem) -> Unit,
    val changeAccountName: (String) -> Unit,
    val saveAccount: () -> Unit,
    val cancelAccount: () -> Unit,
    val archiveAccount: (AccountListItem) -> Unit,
    val categoryAction: (CategoryAction) -> Unit,
)

@Composable
private fun ManageContent(state: ManageUiState, actions: ManageActions) {
    AccountsSection(
        accounts = state.accounts,
        editor = state.accountEditor,
        onBeginCreate = actions.beginAccountCreate,
        onBeginRename = actions.beginAccountRename,
        onNameChange = actions.changeAccountName,
        onSave = actions.saveAccount,
        onCancel = actions.cancelAccount,
        onArchive = actions.archiveAccount,
    )
    Spacer(Modifier.height(32.dp))
    CategoriesSection(state.categories, state.categoryError, actions.categoryAction)
}

@Composable
private fun ModeRow(selected: EntryMode, onSelect: (EntryMode) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        EntryMode.entries.forEach { mode ->
            val label = when (mode) {
                EntryMode.EXPENSE -> "支出"
                EntryMode.INCOME -> "收入"
                EntryMode.TRANSFER -> "转账"
            }
            LedgerControl(label, { onSelect(mode) }, Modifier.weight(1f), accent = mode == selected)
        }
    }
}

@Composable
private fun Keypad(
    onDigit: (Int) -> Unit,
    onDecimal: () -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onSave: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(1, 2, 3).forEach { digit -> Key(digit.toString(), Modifier.weight(1f)) { onDigit(digit) } }
            Key("删除", Modifier.weight(1f), onClick = onBackspace)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(4, 5, 6).forEach { digit -> Key(digit.toString(), Modifier.weight(1f)) { onDigit(digit) } }
            Key("清空", Modifier.weight(1f), onClick = onClear)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(7, 8, 9).forEach { digit -> Key(digit.toString(), Modifier.weight(1f)) { onDigit(digit) } }
            Key(".", Modifier.weight(1f), onClick = onDecimal)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Key("0", Modifier.weight(2f)) { onDigit(0) }
            Key("记下", Modifier.weight(2f), accent = true, onClick = onSave)
        }
    }
}

@Composable
private fun Key(label: String, modifier: Modifier, accent: Boolean = false, onClick: () -> Unit) {
    LedgerControl(label, onClick, modifier.height(52.dp), accent = accent)
}

