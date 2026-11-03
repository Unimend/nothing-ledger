package com.unimend.nothingledger.database

import com.unimend.nothingledger.domain.TransactionRepository
import com.unimend.nothingledger.model.AccountId
import com.unimend.nothingledger.model.CategoryId
import com.unimend.nothingledger.model.Money
import com.unimend.nothingledger.model.Transaction
import com.unimend.nothingledger.model.TransactionId
import com.unimend.nothingledger.model.TransactionType
import java.time.Clock
import java.time.Instant
import java.util.Currency
import java.util.UUID

public class RoomTransactionRepository internal constructor(
    private val dao: TransactionDao,
    private val clock: Clock = Clock.systemUTC(),
) : TransactionRepository {
    override fun save(transaction: Transaction): Unit = dao.insert(transaction.toEntity())

    override fun findById(id: TransactionId): Transaction? =
        dao.findActiveById(id.value.toString())?.toDomain()

    override fun softDelete(id: TransactionId) {
        dao.softDelete(id.value.toString(), clock.millis())
    }
}

private fun Transaction.toEntity(): TransactionEntity = TransactionEntity(
    id = id.value.toString(),
    type = type.name,
    amountMinor = money.minorUnits,
    currencyCode = money.currency.currencyCode,
    occurredAtEpochMillis = occurredAt.toEpochMilli(),
    sourceAccountId = sourceAccountId?.value?.toString(),
    destinationAccountId = destinationAccountId?.value?.toString(),
    categoryId = categoryId?.value?.toString(),
    note = note,
    importFingerprint = null,
    createdAtEpochMillis = createdAt.toEpochMilli(),
    updatedAtEpochMillis = updatedAt.toEpochMilli(),
    deletedAtEpochMillis = null,
)

private fun TransactionEntity.toDomain(): Transaction = Transaction(
    id = TransactionId(UUID.fromString(id)),
    type = TransactionType.valueOf(type),
    money = Money(amountMinor, Currency.getInstance(currencyCode)),
    occurredAt = Instant.ofEpochMilli(occurredAtEpochMillis),
    sourceAccountId = sourceAccountId?.let { AccountId(UUID.fromString(it)) },
    destinationAccountId = destinationAccountId?.let { AccountId(UUID.fromString(it)) },
    categoryId = categoryId?.let { CategoryId(UUID.fromString(it)) },
    note = note,
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
    updatedAt = Instant.ofEpochMilli(updatedAtEpochMillis),
)

