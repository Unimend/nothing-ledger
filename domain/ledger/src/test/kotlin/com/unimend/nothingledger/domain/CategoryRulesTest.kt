package com.unimend.nothingledger.domain

import com.unimend.nothingledger.model.Category
import com.unimend.nothingledger.model.CategoryId
import com.unimend.nothingledger.model.CategoryType
import com.unimend.nothingledger.model.TransactionType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking

class CategoryRulesTest {
    private val repository = MemoryCategoryRepository()

    @Test fun `child requires an existing parent`() {
        val create = CreateCategory(repository)
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { create(CategoryDraft(CategoryId(UUID.randomUUID()), "Lunch", CategoryType.EXPENSE, 0)) }
        }
    }

    @Test fun `parent and child must share transaction type`() {
        val parent = category(type = CategoryType.INCOME)
        runBlocking { repository.save(parent) }
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { CreateCategory(repository)(CategoryDraft(parent.id, "Lunch", CategoryType.EXPENSE, 0)) }
        }
    }

    @Test fun `only two category levels are allowed`() {
        val root = category()
        val child = category(parentId = root.id)
        runBlocking { repository.save(root); repository.save(child) }
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { CreateCategory(repository)(CategoryDraft(child.id, "Third", CategoryType.EXPENSE, 0)) }
        }
    }

    @Test fun `archived category cannot receive a child`() {
        val parent = category(archivedAt = Instant.EPOCH)
        runBlocking { repository.save(parent) }
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { CreateCategory(repository)(CategoryDraft(parent.id, "Child", CategoryType.EXPENSE, 0)) }
        }
    }

    @Test fun `category can be archived idempotently`() = runBlocking {
        val now = Instant.parse("2026-11-03T08:00:00Z")
        val category = category()
        repository.save(category)
        val archive = ArchiveCategory(repository, Clock.fixed(now, ZoneOffset.UTC))
        assertEquals(now, archive(category.id).archivedAt)
        assertEquals(now, archive(category.id).archivedAt)
    }

    @Test fun `archived category cannot be used by new transaction`() {
        val category = category(archivedAt = Instant.EPOCH)
        runBlocking { repository.save(category) }
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { RequireUsableCategory(repository)(category.id, TransactionType.EXPENSE) }
        }
    }

    @Test fun `category and transaction types must match`() {
        val category = category(type = CategoryType.INCOME)
        runBlocking { repository.save(category) }
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { RequireUsableCategory(repository)(category.id, TransactionType.EXPENSE) }
        }
    }

    @Test fun `display order can be changed through the category system`() = runBlocking {
        val category = category(displayOrder = 2)
        repository.save(category)
        assertEquals(7, ReorderCategory(repository)(category.id, 7).displayOrder)
    }

    private fun category(
        parentId: CategoryId? = null,
        type: CategoryType = CategoryType.EXPENSE,
        displayOrder: Int = 0,
        archivedAt: Instant? = null,
    ): Category = Category(
        id = CategoryId(UUID.randomUUID()),
        parentId = parentId,
        name = "Food",
        type = type,
        displayOrder = displayOrder,
        archivedAt = archivedAt,
    )

    private class MemoryCategoryRepository : CategoryRepository {
        private val categories = mutableMapOf<CategoryId, Category>()
        override suspend fun findById(id: CategoryId): Category? = categories[id]
        override suspend fun save(category: Category) { categories[category.id] = category }
        override fun observeAll(): Flow<List<Category>> = flowOf(categories.values.toList())
    }
}

