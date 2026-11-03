package com.unimend.nothingledger.domain

import com.unimend.nothingledger.model.Category
import com.unimend.nothingledger.model.CategoryId
import kotlinx.coroutines.flow.Flow

public interface CategoryRepository {
    public suspend fun findById(id: CategoryId): Category?
    public suspend fun save(category: Category)
    public fun observeAll(): Flow<List<Category>>
}

