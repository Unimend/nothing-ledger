package com.unimend.nothingledger.categories

import com.unimend.nothingledger.domain.CategoryRepository
import com.unimend.nothingledger.model.Category
import com.unimend.nothingledger.model.CategoryId
import com.unimend.nothingledger.model.CategoryType
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CategoryManagerTest {
    private val now = Instant.parse("2026-11-03T08:00:00Z")
    private val repository = MemoryCategoryRepository()
    private var nextId = 10L
    private val manager = CategoryManager(
        repository,
        Clock.fixed(now, ZoneOffset.UTC),
        newId = { id(nextId++) },
    )

    @Test fun `list separates expense and income and preserves parent child hierarchy`() = runBlocking {
        val expenseRoot = category(1, "Food", CategoryType.EXPENSE, order = 1)
        val expenseChild = category(2, "Lunch", CategoryType.EXPENSE, parentId = expenseRoot.id)
        val income = category(3, "Salary", CategoryType.INCOME)
        repository.save(expenseChild)
        repository.save(income)
        repository.save(expenseRoot)

        val expenseState = manager.content()
        assertEquals(listOf(expenseRoot.id, expenseChild.id), expenseState.rows.map { it.category.id })
        assertEquals(listOf(0, 1), expenseState.rows.map(CategoryRow::depth))

        manager.dispatch(CategoryAction.SelectType(CategoryType.INCOME))
        assertEquals(listOf(income.id), manager.content().rows.map { it.category.id })
    }

    @Test fun `create appends to selected sibling list and child inherits parent type`() = runBlocking {
        val root = category(1, "Food", CategoryType.EXPENSE, order = 0)
        val child = category(2, "Breakfast", CategoryType.EXPENSE, parentId = root.id, order = 0)
        repository.save(root)
        repository.save(child)
        manager.dispatch(CategoryAction.SelectType(CategoryType.INCOME))

        manager.dispatch(CategoryAction.Create(" Lunch ", root.id))

        val created = requireNotNull(repository.findById(id(10)))
        assertEquals("Lunch", created.name)
        assertEquals(CategoryType.EXPENSE, created.type)
        assertEquals(root.id, created.parentId)
        assertEquals(1, created.displayOrder)
    }

    @Test fun `rename normalizes text and archive hides item until requested`() = runBlocking {
        val food = category(1, "Food", CategoryType.EXPENSE)
        repository.save(food)

        manager.dispatch(CategoryAction.Rename(food.id, " Daily food "))
        assertEquals("Daily food", repository.findById(food.id)?.name)

        manager.dispatch(CategoryAction.Archive(food.id))
        assertTrue(repository.findById(food.id)?.isArchived == true)
        assertTrue(manager.states.first() is CategoryListState.Loading)
        val hidden = manager.states.first { it !is CategoryListState.Loading }
        assertTrue(hidden is CategoryListState.Empty)

        manager.dispatch(CategoryAction.SetArchivedVisible(true))
        val visible = manager.content().rows.single().category
        assertEquals(food.id, visible.id)
        assertEquals(now, visible.archivedAt)
    }

    @Test fun `move swaps only categories in the same sibling group`() = runBlocking {
        val food = category(1, "Food", CategoryType.EXPENSE, order = 0)
        val travel = category(2, "Travel", CategoryType.EXPENSE, order = 1)
        val salary = category(3, "Salary", CategoryType.INCOME, order = 0)
        repository.save(food)
        repository.save(travel)
        repository.save(salary)

        manager.dispatch(CategoryAction.Move(travel.id, CategoryAction.Direction.UP))

        assertEquals(listOf(travel.id, food.id), manager.content().rows.map { it.category.id })
        assertEquals(0, repository.findById(travel.id)?.displayOrder)
        assertEquals(1, repository.findById(food.id)?.displayOrder)
        assertEquals(0, repository.findById(salary.id)?.displayOrder)
    }

    @Test fun `archived filtering does not lose active child of archived parent`() {
        val parent = category(1, "Old", CategoryType.EXPENSE, archivedAt = Instant.EPOCH)
        val child = category(2, "Still active", CategoryType.EXPENSE, parentId = parent.id)

        val state = buildCategoryState(CategoryQuery(), listOf(parent, child)) as CategoryListState.Content

        assertEquals(listOf(child.id), state.rows.map { it.category.id })
        assertEquals(0, state.rows.single().depth)
        assertFalse(state.rows.single().category.isArchived)
    }

    @Test fun `concurrent creates receive distinct stable display orders`() = runBlocking {
        val delayedRepository = MemoryCategoryRepository(saveDelayMillis = 25)
        var generated = 100L
        val concurrentManager = CategoryManager(
            delayedRepository,
            Clock.fixed(now, ZoneOffset.UTC),
            newId = { id(generated++) },
        )

        listOf("Food", "Travel").map { name ->
            async { concurrentManager.dispatch(CategoryAction.Create(name)) }
        }.forEach { it.await() }

        assertEquals(listOf(0, 1), delayedRepository.snapshot().map(Category::displayOrder).sorted())
    }

    private suspend fun CategoryManager.content(): CategoryListState.Content =
        states.first { it is CategoryListState.Content } as CategoryListState.Content

    private fun category(
        number: Long,
        name: String,
        type: CategoryType,
        parentId: CategoryId? = null,
        order: Int = 0,
        archivedAt: Instant? = null,
    ): Category = Category(id(number), parentId, name, type, order, archivedAt)

    private fun id(number: Long): CategoryId = CategoryId(UUID(0, number))

    private class MemoryCategoryRepository(
        private val saveDelayMillis: Long = 0,
    ) : CategoryRepository {
        private val state = MutableStateFlow<List<Category>>(emptyList())

        override suspend fun findById(id: CategoryId): Category? = state.value.find { it.id == id }

        override suspend fun save(category: Category) {
            if (saveDelayMillis > 0) delay(saveDelayMillis)
            state.value = state.value.filterNot { it.id == category.id } + category
        }

        override fun observeAll(): Flow<List<Category>> = state
        fun snapshot(): List<Category> = state.value
    }
}
