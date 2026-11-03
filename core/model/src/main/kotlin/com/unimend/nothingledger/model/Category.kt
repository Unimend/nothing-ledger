package com.unimend.nothingledger.model

import java.time.Instant

public enum class CategoryType {
    EXPENSE,
    INCOME,
}

public data class Category(
    public val id: CategoryId,
    public val parentId: CategoryId?,
    public val name: String,
    public val type: CategoryType,
    public val displayOrder: Int,
    public val archivedAt: Instant?,
) {
    init {
        require(name.isNotBlank()) { "Category name must not be blank" }
        require(name == name.trim()) { "Category name must be normalized" }
        require(name.length <= MAX_NAME_LENGTH) { "Category name is too long" }
        require(displayOrder >= 0) { "Category display order must not be negative" }
        require(parentId != id) { "Category cannot be its own parent" }
    }

    public val isArchived: Boolean
        get() = archivedAt != null

    public companion object {
        public const val MAX_NAME_LENGTH: Int = 40
    }
}

