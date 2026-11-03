package com.unimend.nothingledger.domain

import com.unimend.nothingledger.model.Account
import com.unimend.nothingledger.model.AccountId
import kotlinx.coroutines.flow.Flow

public interface AccountRepository {
    public suspend fun findById(id: AccountId): Account?
    public suspend fun save(account: Account)
    public fun observeAll(): Flow<List<Account>>
}

