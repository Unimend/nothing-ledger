package com.unimend.nothingledger.entry

import com.unimend.nothingledger.domain.AccountRepository
import com.unimend.nothingledger.domain.CategoryRepository
import com.unimend.nothingledger.model.Account
import com.unimend.nothingledger.model.AccountBalance
import com.unimend.nothingledger.model.AccountId
import com.unimend.nothingledger.model.AccountKind
import com.unimend.nothingledger.model.Category
import com.unimend.nothingledger.model.CategoryId
import com.unimend.nothingledger.model.CategoryType
import java.time.Instant
import java.util.Currency
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EntryOptionsTest {
    private val cny = Currency.getInstance("CNY")
    private val usd = Currency.getInstance("USD")
    private val a = account("00000000-0000-0000-0000-000000000001", "现金", 2)
    private val b = account("00000000-0000-0000-0000-000000000002", "微信", 1)
    private val archived = account("00000000-0000-0000-0000-000000000003", "旧账户", 0, true)
    private val foreign = account("00000000-0000-0000-0000-000000000004", "USD", 0, currency = usd)
    private val expense = category("00000000-0000-0000-0000-000000000011", "餐饮", CategoryType.EXPENSE, 2)
    private val income = category("00000000-0000-0000-0000-000000000012", "工资", CategoryType.INCOME, 1)
    private val oldCategory = category("00000000-0000-0000-0000-000000000013", "旧分类", CategoryType.EXPENSE, 0, true)

    @Test
    fun `expense exposes active matching-currency sources and expense categories`() = runBlocking {
        val states = observer()(EntryOptionsQuery(EntryMode.EXPENSE, cny)).toList()
        assertInstanceOf(EntryOptionsState.Loading::class.java, states.first())
        val ready = states.last() as EntryOptionsState.Ready

        assertEquals(listOf(b.id, a.id), ready.options.sourceAccounts.map(Account::id))
        assertTrue(ready.options.destinationAccounts.isEmpty())
        assertEquals(listOf(expense.id), ready.options.categories.map(Category::id))
    }

    @Test
    fun `income exposes only destinations and income categories`() = runBlocking {
        val ready = observer()(EntryOptionsQuery(EntryMode.INCOME, cny)).toList().last() as EntryOptionsState.Ready

        assertTrue(ready.options.sourceAccounts.isEmpty())
        assertEquals(listOf(b.id, a.id), ready.options.destinationAccounts.map(Account::id))
        assertEquals(listOf(income.id), ready.options.categories.map(Category::id))
    }

    @Test
    fun `transfer has no categories and prevents same account choices`() = runBlocking {
        val ready = observer()(
            EntryOptionsQuery(EntryMode.TRANSFER, cny, selectedSourceAccountId = a.id, selectedDestinationAccountId = b.id),
        ).toList().last() as EntryOptionsState.Ready

        assertEquals(listOf(a.id), ready.options.sourceAccounts.map(Account::id))
        assertEquals(listOf(b.id), ready.options.destinationAccounts.map(Account::id))
        assertTrue(ready.options.categories.isEmpty())
    }

    private fun observer(): ObserveEntryOptions = ObserveEntryOptions(
        FakeAccountRepository(listOf(a, b, archived, foreign)),
        FakeCategoryRepository(listOf(expense, income, oldCategory)),
    )

    private fun account(
        rawId: String,
        name: String,
        order: Int,
        archived: Boolean = false,
        currency: Currency = cny,
    ): Account = Account(
        AccountId(UUID.fromString(rawId)), name, AccountKind.PAYMENT, AccountBalance(0, currency), order,
        if (archived) Instant.EPOCH else null,
    )

    private fun category(
        rawId: String,
        name: String,
        type: CategoryType,
        order: Int,
        archived: Boolean = false,
    ): Category = Category(
        CategoryId(UUID.fromString(rawId)), null, name, type, order,
        if (archived) Instant.EPOCH else null,
    )
}

private class FakeAccountRepository(private val values: List<Account>) : AccountRepository {
    override suspend fun findById(id: AccountId): Account? = values.find { it.id == id }
    override suspend fun save(account: Account): Unit = error("Read-only fake")
    override fun observeAll(): Flow<List<Account>> = flowOf(values)
}

private class FakeCategoryRepository(private val values: List<Category>) : CategoryRepository {
    override suspend fun findById(id: CategoryId): Category? = values.find { it.id == id }
    override suspend fun save(category: Category): Unit = error("Read-only fake")
    override fun observeAll(): Flow<List<Category>> = flowOf(values)
}
