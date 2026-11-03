package com.unimend.nothingledger.categories

import com.unimend.nothingledger.model.Category
import com.unimend.nothingledger.model.CategoryId
import com.unimend.nothingledger.model.CategoryType

public data class CategoryQuery(
    public val type: CategoryType = CategoryType.EXPENSE,
    public val includeArchived: Boolean = false,
)

public data class CategoryRow(public val category: Category, public val depth: Int)

public sealed interface CategoryListState {
    public data class Loading(public val query: CategoryQuery) : CategoryListState
    public data class Empty(public val query: CategoryQuery) : CategoryListState
    public data class Content(public val query: CategoryQuery, public val rows: List<CategoryRow>) : CategoryListState
    public data class Failure(public val query: CategoryQuery, public val message: String) : CategoryListState
}

public sealed interface CategoryAction {
    public data class SelectType(public val type: CategoryType) : CategoryAction
    public data class SetArchivedVisible(public val visible: Boolean) : CategoryAction
    public data class Create(public val name: String, public val parentId: CategoryId? = null) : CategoryAction
    public data class Rename(public val id: CategoryId, public val name: String) : CategoryAction
    public data class Move(public val id: CategoryId, public val direction: Direction) : CategoryAction
    public data class Archive(public val id: CategoryId) : CategoryAction

    public enum class Direction { UP, DOWN }
}
