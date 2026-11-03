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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.unimend.nothingledger.model.TransactionId
import com.unimend.nothingledger.shell.ShellAction
import com.unimend.nothingledger.shell.MainDestination
import com.unimend.nothingledger.shell.OverlayDestination
import com.unimend.nothingledger.shell.ShellEffect
import com.unimend.nothingledger.shell.ShellReducer
import com.unimend.nothingledger.shell.ShellState
import com.unimend.nothingledger.timeline.TimelineHomePage
import com.unimend.nothingledger.timeline.TransactionDetailState
import com.unimend.nothingledger.timeline.TransactionDetailActionState
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
            val calendarViewModel: CalendarViewModel = viewModel(factory = CalendarViewModel.factory(database))
            val calendarState by calendarViewModel.state.collectAsState()
            val accountsViewModel: AccountsViewModel = viewModel(factory = AccountsViewModel.factory(database))
            val accountsState by accountsViewModel.accounts.collectAsState()
            val accountEditor by accountsViewModel.editor.collectAsState()
            val categoriesViewModel: CategoriesViewModel = viewModel(factory = CategoriesViewModel.factory(database))
            val categoriesState by categoriesViewModel.state.collectAsState()
            val categoryError by categoriesViewModel.commandError.collectAsState()
            var selectedTransactionId by remember { mutableStateOf<TransactionId?>(null) }
            val detailActionsViewModel: TransactionDetailActionsViewModel = viewModel(
                factory = TransactionDetailActionsViewModel.factory(database),
            )
            val transactionDetailActions by detailActionsViewModel.state.collectAsState()
            val transactionDetail = selectedTransactionId?.let { id ->
                val detailViewModel: TransactionDetailViewModel = viewModel(
                    key = "transaction-detail-${id.value}",
                    factory = TransactionDetailViewModel.factory(database, id),
                )
                val state by detailViewModel.state.collectAsState()
                state
            }
            LaunchedEffect(transactionDetail) {
                val ready = transactionDetail as? TransactionDetailState.Ready ?: return@LaunchedEffect
                detailActionsViewModel.open(ready.detail.transaction)
            }
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
                    timelineActions = TimelineActions(
                        previousMonth = timelineViewModel::showPreviousMonth,
                        currentMonth = timelineViewModel::showCurrentMonth,
                        nextMonth = timelineViewModel::showNextMonth,
                    ),
                    calendarState = calendarState,
                    manageState = ManageUiState(accountsState, accountEditor, categoriesState, categoryError),
                    entryAction = viewModel::dispatch,
                    calendarActions = CalendarActions(
                        previousMonth = calendarViewModel::previousMonth,
                        nextMonth = calendarViewModel::nextMonth,
                        selectDate = calendarViewModel::selectDate,
                    ),
                    transactionDetail = transactionDetail,
                    transactionDetailActions = transactionDetailActions,
                    onSelectTransaction = { selectedTransactionId = it },
                    onCloseTransaction = {
                        detailActionsViewModel.close()
                        selectedTransactionId = null
                    },
                    onDeleteTransaction = detailActionsViewModel::delete,
                    onUndoDelete = detailActionsViewModel::undoDelete,
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
    timelineActions: TimelineActions,
    calendarState: com.unimend.nothingledger.timeline.CalendarState,
    manageState: ManageUiState,
    entryAction: (EntryAction) -> Unit,
    calendarActions: CalendarActions,
    transactionDetail: TransactionDetailState?,
    transactionDetailActions: TransactionDetailActionState,
    onSelectTransaction: (TransactionId) -> Unit,
    onCloseTransaction: () -> Unit,
    onDeleteTransaction: () -> Unit,
    onUndoDelete: () -> Unit,
    manageActions: ManageActions,
    onExit: () -> Unit,
) {
    var shellState by remember { mutableStateOf(ShellState()) }
    fun navigate(action: ShellAction) {
        val transition = ShellReducer.reduce(shellState, action)
        shellState = transition.state
        if (transition.effect == ShellEffect.ExitRequested) onExit()
    }
    BackHandler {
        if (transactionDetail != null) onCloseTransaction() else navigate(ShellAction.NavigateBack)
    }

    transactionDetail?.let { detail ->
        TransactionDetailScreen(
            state = detail,
            actionState = transactionDetailActions,
            onBack = onCloseTransaction,
            onDelete = onDeleteTransaction,
            onUndoDelete = onUndoDelete,
        )
        return
    }

    when (shellState.overlay) {
        OverlayDestination.QUICK_ENTRY -> {
            LedgerScreen {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("新增记账", color = Color.White, style = MaterialTheme.typography.titleLarge)
                    LedgerControl("返回", { navigate(ShellAction.CloseOverlay) })
                }
                Spacer(Modifier.height(24.dp))
                EntryContent(uiState, entryOptions, entryAction)
            }
        }
        OverlayDestination.CATEGORIES -> {
            LedgerScreen {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("分类管理", color = Color.White, style = MaterialTheme.typography.titleLarge)
                    LedgerControl("返回", { navigate(ShellAction.CloseOverlay) })
                }
                Spacer(Modifier.height(24.dp))
                CategoriesSection(manageState.categories, manageState.categoryError, manageActions.categoryAction)
            }
        }
        null -> AppScaffold(
            selected = shellState.selectedMain,
            onSelect = { navigate(ShellAction.SelectMain(it)) },
            onAdd = { navigate(ShellAction.OpenOverlay(OverlayDestination.QUICK_ENTRY)) },
        ) { destination ->
            AppHeader(uiState, destination)
            Spacer(Modifier.height(24.dp))
            when (destination) {
                MainDestination.TIMELINE -> TimelineHomePage(
                    state = timelineState,
                    onTransactionClick = { onSelectTransaction(it.id) },
                    onPreviousMonth = timelineActions.previousMonth,
                    onCurrentMonth = timelineActions.currentMonth,
                    onNextMonth = timelineActions.nextMonth,
                )
                MainDestination.CALENDAR -> CalendarSection(
                    state = calendarState,
                    onPreviousMonth = calendarActions.previousMonth,
                    onNextMonth = calendarActions.nextMonth,
                    onSelectDate = calendarActions.selectDate,
                )
                MainDestination.STATISTICS -> StatisticsSection(timelineState, manageState.categories)
                MainDestination.ACCOUNTS -> AccountsContent(
                    manageState,
                    manageActions,
                    onCategories = { navigate(ShellAction.OpenOverlay(OverlayDestination.CATEGORIES)) },
                )
            }
        }
    }
}

@Composable
private fun AppHeader(uiState: EntryUiState, destination: MainDestination) {
    val status = uiState.errorMessage?.let { "出错 / ${it.take(18)}" } ?: when (destination) {
        MainDestination.TIMELINE -> "全部流水"
        MainDestination.CALENDAR -> "按日期查看"
        MainDestination.STATISTICS -> "收支概览"
        MainDestination.ACCOUNTS -> "资产与账户"
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("NOTHING\nLEDGER", color = Color.White, style = MaterialTheme.typography.titleLarge)
        Text(status, color = Color(0xFFFF2D2D), style = MaterialTheme.typography.labelMedium)
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
    EntrySelectorsSection(state, entryOptions, dispatch)
    Spacer(Modifier.height(16.dp))
    EntryDateTimeEditor(state.occurredAt, dispatch)
    Spacer(Modifier.height(16.dp))
    EntryNoteField(state.note) { dispatch(EntryAction.ChangeNote(it)) }
    Spacer(Modifier.height(16.dp))
    LedgerAmountText("¥ ${state.amount.displayText}", Modifier.fillMaxWidth())
    Spacer(Modifier.height(12.dp))
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

@Composable
private fun EntryNoteField(value: String, onValueChange: (String) -> Unit) {
    BasicTextField(
        value = value,
        onValueChange = { onValueChange(it.take(500)) },
        modifier = Modifier
            .fillMaxWidth()
            .background(NothingLedgerTheme.colors.surfaceRaised)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
        decorationBox = { field ->
            Box {
                if (value.isEmpty()) {
                    Text("备注（可选）", color = NothingLedgerTheme.colors.contentMuted, style = MaterialTheme.typography.bodyLarge)
                }
                field()
            }
        },
    )
}

private data class ManageUiState(
    val accounts: AccountsState,
    val accountEditor: AccountEditorState,
    val categories: CategoryListState,
    val categoryError: String?,
)

private data class CalendarActions(
    val previousMonth: () -> Unit,
    val nextMonth: () -> Unit,
    val selectDate: (java.time.LocalDate) -> Unit,
)

private data class TimelineActions(
    val previousMonth: () -> Unit,
    val currentMonth: () -> Unit,
    val nextMonth: () -> Unit,
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
private fun AccountsContent(
    state: ManageUiState,
    actions: ManageActions,
    onCategories: () -> Unit,
) {
    AccountsHomeSection(
        accounts = state.accounts,
        editor = state.accountEditor,
        onBeginCreate = actions.beginAccountCreate,
        onBeginRename = actions.beginAccountRename,
        onNameChange = actions.changeAccountName,
        onSave = actions.saveAccount,
        onCancel = actions.cancelAccount,
        onArchive = actions.archiveAccount,
        onOpenCategories = onCategories,
    )
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

