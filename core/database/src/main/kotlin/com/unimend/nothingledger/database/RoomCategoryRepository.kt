package com.unimend.nothingledger.database

import com.unimend.nothingledger.domain.CategoryRepository
import com.unimend.nothingledger.model.Category
import com.unimend.nothingledger.model.CategoryId
import com.unimend.nothingledger.model.CategoryType
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

public class RoomCategoryRepository internal constructor(private val dao: CategoryDao) : CategoryRepository {
    override suspend fun findById(id: CategoryId): Category? = dao.findById(id.value.toString())?.toDomain()
    override suspend fun save(category: Category): Unit = dao.save(category.toEntity())
    override fun observeAll(): Flow<List<Category>> =
        dao.observeAll().map { entities -> entities.map(CategoryEntity::toDomain) }
}

private fun Category.toEntity(): CategoryEntity = CategoryEntity(
    id.value.toString(), parentId?.value?.toString(), name, type.name,
    displayOrder, archivedAt?.toEpochMilli(),
)

private fun CategoryEntity.toDomain(): Category = Category(
    CategoryId(UUID.fromString(id)), parentId?.let { CategoryId(UUID.fromString(it)) },
    name, CategoryType.valueOf(transactionType), displayOrder,
    archivedAtEpochMillis?.let(Instant::ofEpochMilli),
)

