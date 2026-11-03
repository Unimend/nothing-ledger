package com.unimend.nothingledger.domain

import com.unimend.nothingledger.model.Account
import com.unimend.nothingledger.model.AccountId

public interface AccountRepository {
    public fun findById(id: AccountId): Account?
    public fun save(account: Account)
}

