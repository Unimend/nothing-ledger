package com.unimend.nothingledger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.unimend.nothingledger.database.LedgerDatabaseFactory
import com.unimend.nothingledger.database.LedgerDatabaseHandle
import com.unimend.nothingledger.design.DitherField
import com.unimend.nothingledger.design.LedgerAmountText
import com.unimend.nothingledger.design.NothingLedgerTheme
import com.unimend.nothingledger.domain.CreateTransaction
import com.unimend.nothingledger.domain.TransactionDraft
import com.unimend.nothingledger.entry.EntryAction
import com.unimend.nothingledger.entry.EntryEffect
import com.unimend.nothingledger.entry.EntryMode
import com.unimend.nothingledger.entry.EntryReducer
import com.unimend.nothingledger.entry.EntryState
import com.unimend.nothingledger.model.Account
import com.unimend.nothingledger.model.AccountBalance
import com.unimend.nothingledger.model.AccountId
import com.unimend.nothingledger.model.AccountKind
import com.unimend.nothingledger.model.Category
import com.unimend.nothingledger.model.CategoryId
import com.unimend.nothingledger.model.CategoryType
import com.unimend.nothingledger.model.Money
import com.unimend.nothingledger.model.TransactionType
import java.time.Clock
import java.time.Instant
import java.util.Currency
import java.util.UUID
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val database by lazy { LedgerDatabaseFactory.create(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NothingLedgerTheme { EntryScreen(database) } }
    }

    override fun onDestroy() {
        database.close()
        super.onDestroy()
    }
}

private val walletId = AccountId(UUID.fromString("10000000-0000-0000-0000-000000000001"))
private val expenseCategoryId = CategoryId(UUID.fromString("20000000-0000-0000-0000-000000000001"))
private val incomeCategoryId = CategoryId(UUID.fromString("20000000-0000-0000-0000-000000000002"))
private val cny: Currency = Currency.getInstance("CNY")

@Composable
private fun EntryScreen(database: LedgerDatabaseHandle) {
    val accounts = remember(database) { database.accounts() }
    val categories = remember(database) { database.categories() }
    val create = remember(database) {
        CreateTransaction(database.transactions(), accounts, categories, Clock.systemUTC())
    }
    var state by remember {
        mutableStateOf(
            EntryState.initial(Instant.now(), cny).copy(
                sourceAccountId = walletId,
                categoryId = expenseCategoryId,
            ),
        )
    }
    var status by remember { mutableStateOf("READY") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(database) {
        accounts.save(Account(walletId, "微信钱包", AccountKind.PAYMENT, AccountBalance(0, cny), 0, null))
        categories.save(Category(expenseCategoryId, null, "日常支出", CategoryType.EXPENSE, 0, null))
        categories.save(Category(incomeCategoryId, null, "日常收入", CategoryType.INCOME, 1, null))
    }

    fun dispatch(action: EntryAction) {
        val transition = EntryReducer.reduce(state, action)
        state = transition.state
        val effect = transition.effect
        if (effect is EntryEffect.Submit) {
            status = "SAVING"
            scope.launch {
                runCatching {
                    val value = effect.value
                    create(
                        TransactionDraft(
                            type = when (value.mode) {
                                EntryMode.EXPENSE -> TransactionType.EXPENSE
                                EntryMode.INCOME -> TransactionType.INCOME
                                EntryMode.TRANSFER -> TransactionType.TRANSFER
                            },
                            money = Money(value.amountMinorUnits, Currency.getInstance(value.currencyCode)),
                            occurredAt = value.occurredAt,
                            sourceAccountId = value.sourceAccountId,
                            destinationAccountId = value.destinationAccountId,
                            categoryId = value.categoryId,
                            note = value.note,
                        ),
                    )
                }.onSuccess {
                    status = "SAVED"
                    state = EntryState.initial(Instant.now(), cny).copy(
                        sourceAccountId = walletId,
                        categoryId = expenseCategoryId,
                    )
                }.onFailure { status = "ERROR / ${it.message.orEmpty().take(18)}" }
            }
        }
    }

    Box(
        Modifier.fillMaxSize().background(Color.Black).padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        DitherField(
            Modifier.align(Alignment.BottomEnd).size(160.dp, 72.dp),
            color = Color(0xFF333333),
        )
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("NOTHING\nLEDGER", color = Color.White, style = MaterialTheme.typography.titleLarge)
                Text(status, color = Color(0xFFFF2D2D), style = MaterialTheme.typography.labelMedium)
            }
            Spacer(Modifier.height(24.dp))
            ModeRow(state.mode) { mode ->
                var next = EntryReducer.reduce(state, EntryAction.ChangeMode(mode)).state
                next = when (mode) {
                    EntryMode.EXPENSE -> next.copy(sourceAccountId = walletId, categoryId = expenseCategoryId)
                    EntryMode.INCOME -> next.copy(destinationAccountId = walletId, categoryId = incomeCategoryId)
                    EntryMode.TRANSFER -> next.copy(sourceAccountId = walletId, destinationAccountId = null)
                }
                state = next
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
                Text("CHECK / ${it.name}", color = Color(0xFFFF2D2D), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 10.dp))
            }
        }
    }
}

@Composable
private fun ModeRow(selected: EntryMode, onSelect: (EntryMode) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        EntryMode.entries.forEach { mode ->
            val active = mode == selected
            Text(
                mode.name,
                Modifier.weight(1f).background(if (active) Color.White else Color(0xFF101010), RoundedCornerShape(8.dp)).clickable { onSelect(mode) }.padding(vertical = 11.dp),
                if (active) Color.Black else Color(0xFF777777),
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
            )
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
    Box(
        modifier.height(52.dp).background(if (accent) Color(0xFFFF2D2D) else Color(0xFF101010), RoundedCornerShape(8.dp)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = Color.White, style = MaterialTheme.typography.titleMedium) }
}

private fun formatAmount(minorUnits: Long): String = "¥ %,d.%02d".format(minorUnits / 100, minorUnits % 100)

