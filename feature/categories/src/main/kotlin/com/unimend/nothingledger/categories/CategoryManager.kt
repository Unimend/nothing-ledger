package com.unimend.nothingledger.categories

import com.unimend.nothingledger.domain.ArchiveCategory
import com.unimend.nothingledger.domain.CategoryDraft
import com.unimend.nothingledger.domain.CategoryRepository
import com.unimend.nothingledger.domain.CreateCategory
import com.unimend.nothingledger.model.Category
import com.unimend.nothingledger.model.CategoryId
import java.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Pure Kotlin application layer for category list state and commands. */
public class CategoryManager(
    private val repository: CategoryRepository,
    clock: Clock,
    newId: () -> CategoryId,
    initialQuery: CategoryQuery = CategoryQuery(),
) {
    private val query = MutableStateFlow(initialQuery)
    private val create = CreateCategory(repository, newId)
    private val archive = ArchiveCategory(repository, clock)
    private val writes = Mutex()

    public val states: Flow<CategoryListState> = repository.observeAll()
        .combine(query) { categories, selectedQuery -> buildCategoryState(selectedQuery, categories) }
        .onStart { emit(CategoryListState.Loading(query.value)) }
        .catch { error -> emit(CategoryListState.Failure(query.value, error.message ?: "Categories unavailable")) }

    public suspend fun dispatch(action: CategoryAction) {
        when (action) {
            is CategoryAction.SelectType -> query.value = query.value.copy(type = action.type)
            is CategoryAction.SetArchivedVisible -> query.value = query.value.copy(includeArchived = action.visible)
            is CategoryAction.Create -> writes.withLock { createCategory(action) }
            is CategoryAction.Rename -> writes.withLock { rename(action.id, action.name) }
            is CategoryAction.Move -> writes.withLock { move(action.id, action.direction) }
            is CategoryAction.Archive -> writes.withLock { archive(action.id) }
        }
    }

    private suspend fun createCategory(action: CategoryAction.Create) {
        val all = repository.observeAll().first()
        val parent = action.parentId?.let { id ->
            requireNotNull(all.find { it.id == id }) { "Parent category does not exist" }
        }
        val type = parent?.type ?: query.value.type
        val nextOrder = all.asSequence()
            .filter { it.type == type && it.parentId == action.parentId }
            .maxOfOrNull(Category::displayOrder)
            ?.plus(1) ?: 0
        create(CategoryDraft(action.parentId, action.name, type, nextOrder))
    }

    private suspend fun rename(id: CategoryId, name: String) {
        val existing = requireNotNull(repository.findById(id)) { "Category does not exist" }
        repository.save(existing.copy(name = name.trim()))
    }

    private suspend fun move(id: CategoryId, direction: CategoryAction.Direction) {
        val all = repository.observeAll().first()
        val moving = requireNotNull(all.find { it.id == id }) { "Category does not exist" }
        val siblings = all.filter { it.type == moving.type && it.parentId == moving.parentId }
            .sortedWith(categoryOrder)
        val fromIndex = siblings.indexOfFirst { it.id == id }
        val targetIndex = when (direction) {
            CategoryAction.Direction.UP -> fromIndex - 1
            CategoryAction.Direction.DOWN -> fromIndex + 1
        }
        if (fromIndex < 0 || targetIndex !in siblings.indices) return
        val reordered = siblings.toMutableList().apply { add(targetIndex, removeAt(fromIndex)) }
        reordered.forEachIndexed { index, category ->
            if (category.displayOrder != index) repository.save(category.copy(displayOrder = index))
        }
    }
}

public fun buildCategoryState(query: CategoryQuery, categories: List<Category>): CategoryListState {
    val matching = categories.filter { category ->
        category.type == query.type && (query.includeArchived || !category.isArchived)
    }
    if (matching.isEmpty()) return CategoryListState.Empty(query)

    val visibleIds = matching.mapTo(mutableSetOf(), Category::id)
    val roots = matching.filter { it.parentId == null || it.parentId !in visibleIds }.sortedWith(categoryOrder)
    val children = matching.filter { it.parentId != null }.groupBy(Category::parentId)
    val rows = buildList {
        roots.forEach { root ->
            add(CategoryRow(root, depth = 0))
            children[root.id].orEmpty().sortedWith(categoryOrder).forEach { child ->
                add(CategoryRow(child, depth = 1))
            }
        }
    }
    return CategoryListState.Content(query, rows)
}

private val categoryOrder: Comparator<Category> =
    compareBy<Category>(Category::displayOrder)
        .thenBy(String.CASE_INSENSITIVE_ORDER, Category::name)
        .thenBy { it.id.value.toString() }
