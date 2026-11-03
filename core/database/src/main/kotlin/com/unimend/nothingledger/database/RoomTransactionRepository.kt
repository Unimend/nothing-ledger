package com.unimend.nothingledger.database

import com.unimend.nothingledger.domain.TransactionRepository
import com.unimend.nothingledger.model.AccountId
import com.unimend.nothingledger.model.CategoryId
import com.unimend.nothingledger.model.Money
import com.unimend.nothingledger.model.Transaction
import com.unimend.nothingledger.model.TransactionId
import com.unimend.nothingledger.model.TransactionType
import java.time.Instant
import java.util.Currency
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

public class RoomTransactionRepository internal constructor(
    private val dao: TransactionDao,
) : TransactionRepository {
    override suspend fun insert(transaction: Transaction): Unit = dao.insert(transaction.toEntity())

    override suspend fun findById(id: TransactionId): Transaction? =
        dao.findActiveById(id.value.toString())?.toDomain()

    override suspend fun update(transaction: Transaction, expectedUpdatedAt: Instant): Boolean =
        transaction.toEntity().let { entity ->
            dao.updateActive(
                entity.id, entity.type, entity.amountMinor, entity.currencyCode,
                entity.occurredAtEpochMillis, entity.sourceAccountId, entity.destinationAccountId,
                entity.categoryId, entity.note, entity.updatedAtEpochMillis, expectedUpdatedAt.toEpochMilli(),
            ) == 1
        }

    override suspend fun softDelete(
        id: TransactionId,
        deletedAt: Instant,
        expectedUpdatedAt: Instant,
    ): Boolean = dao.softDelete(
        id.value.toString(), deletedAt.toEpochMilli(), expectedUpdatedAt.toEpochMilli(),
    ) == 1

    override suspend fun restore(id: TransactionId, deletedAt: Instant, restoredAt: Instant): Boolean =
        dao.restore(id.value.toString(), deletedAt.toEpochMilli(), restoredAt.toEpochMilli()) == 1

    override fun observeActive(): Flow<List<Transaction>> =
        dao.observeActive().map { entities -> entities.map(TransactionEntity::toDomain) }
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

