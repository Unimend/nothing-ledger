package com.unimend.nothingledger.domain

import com.unimend.nothingledger.model.Category
import com.unimend.nothingledger.model.CategoryId

public interface CategoryRepository {
    public fun findById(id: CategoryId): Category?
    public fun save(category: Category)
}

