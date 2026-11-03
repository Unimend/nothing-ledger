package com.unimend.nothingledger.database

import com.unimend.nothingledger.domain.AccountRepository
import com.unimend.nothingledger.model.Account
import com.unimend.nothingledger.model.AccountBalance
import com.unimend.nothingledger.model.AccountId
import com.unimend.nothingledger.model.AccountKind
import java.time.Instant
import java.util.Currency
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

public class RoomAccountRepository internal constructor(private val dao: AccountDao) : AccountRepository {
    override suspend fun findById(id: AccountId): Account? = dao.findById(id.value.toString())?.toDomain()
    override suspend fun save(account: Account): Unit = dao.save(account.toEntity())
    override fun observeAll(): Flow<List<Account>> =
        dao.observeAll().map { entities -> entities.map(AccountEntity::toDomain) }
}

private fun Account.toEntity(): AccountEntity = AccountEntity(
    id.value.toString(), name, kind.name, openingBalance.currency.currencyCode,
    openingBalance.minorUnits, displayOrder, archivedAt?.toEpochMilli(),
)

private fun AccountEntity.toDomain(): Account = Account(
    AccountId(UUID.fromString(id)), name, AccountKind.valueOf(kind),
    AccountBalance(openingBalanceMinor, Currency.getInstance(currencyCode)),
    displayOrder, archivedAtEpochMillis?.let(Instant::ofEpochMilli),
)

