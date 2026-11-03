package com.unimend.nothingledger.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
internal data class AccountEntity(
    @PrimaryKey val id: String,
    val name: String,
    val kind: String,
    val currencyCode: String,
    val openingBalanceMinor: Long,
    val displayOrder: Int,
    val archivedAtEpochMillis: Long?,
)

@Entity(
    tableName = "categories",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("parentId")],
)
internal data class CategoryEntity(
    @PrimaryKey val id: String,
    val parentId: String?,
    val name: String,
    val transactionType: String,
    val displayOrder: Int,
    val archivedAtEpochMillis: Long?,
)

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(entity = AccountEntity::class, parentColumns = ["id"], childColumns = ["sourceAccountId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = AccountEntity::class, parentColumns = ["id"], childColumns = ["destinationAccountId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = CategoryEntity::class, parentColumns = ["id"], childColumns = ["categoryId"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [
        Index("occurredAtEpochMillis"),
        Index("sourceAccountId"),
        Index("destinationAccountId"),
        Index("categoryId"),
        Index(value = ["importFingerprint"], unique = true),
    ],
)
internal data class TransactionEntity(
    @PrimaryKey val id: String,
    val type: String,
    val amountMinor: Long,
    val currencyCode: String,
    val occurredAtEpochMillis: Long,
    val sourceAccountId: String?,
    val destinationAccountId: String?,
    val categoryId: String?,
    val note: String?,
    val importFingerprint: String?,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val deletedAtEpochMillis: Long?,
)

