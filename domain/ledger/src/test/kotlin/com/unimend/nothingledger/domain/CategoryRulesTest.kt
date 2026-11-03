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

class CategoryRulesTest {
    private val repository = MemoryCategoryRepository()

    @Test fun `child requires an existing parent`() {
        val create = CreateCategory(repository)
        assertThrows(IllegalArgumentException::class.java) {
            create(CategoryDraft(CategoryId(UUID.randomUUID()), "Lunch", CategoryType.EXPENSE, 0))
        }
    }

    @Test fun `parent and child must share transaction type`() {
        val parent = category(type = CategoryType.INCOME)
        repository.save(parent)
        assertThrows(IllegalArgumentException::class.java) {
            CreateCategory(repository)(CategoryDraft(parent.id, "Lunch", CategoryType.EXPENSE, 0))
        }
    }

    @Test fun `only two category levels are allowed`() {
        val root = category()
        val child = category(parentId = root.id)
        repository.save(root)
        repository.save(child)
        assertThrows(IllegalArgumentException::class.java) {
            CreateCategory(repository)(CategoryDraft(child.id, "Third", CategoryType.EXPENSE, 0))
        }
    }

    @Test fun `archived category cannot receive a child`() {
        val parent = category(archivedAt = Instant.EPOCH)
        repository.save(parent)
        assertThrows(IllegalArgumentException::class.java) {
            CreateCategory(repository)(CategoryDraft(parent.id, "Child", CategoryType.EXPENSE, 0))
        }
    }

    @Test fun `category can be archived idempotently`() {
        val now = Instant.parse("2026-11-03T08:00:00Z")
        val category = category()
        repository.save(category)
        val archive = ArchiveCategory(repository, Clock.fixed(now, ZoneOffset.UTC))
        assertEquals(now, archive(category.id).archivedAt)
        assertEquals(now, archive(category.id).archivedAt)
    }

    @Test fun `archived category cannot be used by new transaction`() {
        val category = category(archivedAt = Instant.EPOCH)
        repository.save(category)
        assertThrows(IllegalArgumentException::class.java) {
            RequireUsableCategory(repository)(category.id, TransactionType.EXPENSE)
        }
    }

    @Test fun `category and transaction types must match`() {
        val category = category(type = CategoryType.INCOME)
        repository.save(category)
        assertThrows(IllegalArgumentException::class.java) {
            RequireUsableCategory(repository)(category.id, TransactionType.EXPENSE)
        }
    }

    @Test fun `display order can be changed through the category system`() {
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
        override fun findById(id: CategoryId): Category? = categories[id]
        override fun save(category: Category) { categories[category.id] = category }
    }
}

