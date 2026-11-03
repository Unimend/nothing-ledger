package com.unimend.nothingledger.domain

import com.unimend.nothingledger.model.Category
import com.unimend.nothingledger.model.CategoryId
import com.unimend.nothingledger.model.CategoryType
import com.unimend.nothingledger.model.TransactionType
import java.time.Clock
import java.util.UUID

public data class CategoryDraft(
    public val parentId: CategoryId? = null,
    public val name: String,
    public val type: CategoryType,
    public val displayOrder: Int,
)

public class CreateCategory(
    private val repository: CategoryRepository,
    private val newId: () -> CategoryId = { CategoryId(UUID.randomUUID()) },
) {
    public operator fun invoke(draft: CategoryDraft): Category {
        val parent = draft.parentId?.let { parentId ->
            requireNotNull(repository.findById(parentId)) { "Parent category does not exist" }
        }
        if (parent != null) {
            require(!parent.isArchived) { "Archived category cannot receive new children" }
            require(parent.parentId == null) { "Categories support at most two levels" }
            require(parent.type == draft.type) { "Parent and child category types must match" }
        }
        val category = Category(
            id = newId(),
            parentId = parent?.id,
            name = draft.name.trim(),
            type = draft.type,
            displayOrder = draft.displayOrder,
            archivedAt = null,
        )
        repository.save(category)
        return category
    }
}

public class ArchiveCategory(
    private val repository: CategoryRepository,
    private val clock: Clock,
) {
    public operator fun invoke(id: CategoryId): Category {
        val category = requireNotNull(repository.findById(id)) { "Category does not exist" }
        if (category.isArchived) return category
        return category.copy(archivedAt = clock.instant()).also(repository::save)
    }
}

public class ReorderCategory(
    private val repository: CategoryRepository,
) {
    public operator fun invoke(id: CategoryId, displayOrder: Int): Category {
        require(displayOrder >= 0) { "Category display order must not be negative" }
        val category = requireNotNull(repository.findById(id)) { "Category does not exist" }
        return category.copy(displayOrder = displayOrder).also(repository::save)
    }
}

public class RequireUsableCategory(
    private val repository: CategoryRepository,
) {
    public operator fun invoke(id: CategoryId, transactionType: TransactionType): Category {
        require(transactionType != TransactionType.TRANSFER) { "Transfer must not have a category" }
        val category = requireNotNull(repository.findById(id)) { "Category does not exist" }
        require(!category.isArchived) { "Archived category cannot be used for a new transaction" }
        val requiredType = when (transactionType) {
            TransactionType.EXPENSE -> CategoryType.EXPENSE
            TransactionType.INCOME -> CategoryType.INCOME
            TransactionType.TRANSFER -> error("Transfer category was rejected above")
        }
        require(category.type == requiredType) { "Category type does not match transaction type" }
        return category
    }
}

