package com.unimend.nothingledger.timeline

import com.unimend.nothingledger.domain.DeleteReceipt
import com.unimend.nothingledger.domain.TransactionDraft
import com.unimend.nothingledger.domain.TransactionEdit
import com.unimend.nothingledger.domain.TransactionSnapshot
import com.unimend.nothingledger.domain.UpdateTransactionCommand
import com.unimend.nothingledger.model.AccountId
import com.unimend.nothingledger.model.CategoryId
import com.unimend.nothingledger.model.Money
import com.unimend.nothingledger.model.Transaction
import com.unimend.nothingledger.model.TransactionId
import com.unimend.nothingledger.model.TransactionType
import java.time.Instant
import java.util.Currency
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TransactionDetailSessionTest {
    @Test fun `update uses visible revision and replaces detail state`() = runBlocking {
        val original = transaction()
        val gateway = FakeCommands(original)
        val session = TransactionDetailSession(gateway)
        session.open(original)

        session.update(draft(250, " dinner "))

        assertEquals(original.updatedAt, gateway.lastUpdate?.expectedUpdatedAt)
        assertEquals(250, session.state.value.transaction?.money?.minorUnits)
        assertEquals(" dinner ", gateway.lastUpdate?.draft?.note)
        assertEquals(TransactionDetailPhase.READY, session.state.value.phase)
    }

    @Test fun `delete exposes one undo and restored state consumes it`() = runBlocking {
        val original = transaction()
        val session = TransactionDetailSession(FakeCommands(original))
        session.open(original)

        session.delete()
        assertEquals(TransactionDetailPhase.DELETED, session.state.value.phase)
        assertTrue(session.state.value.canUndoDelete)

        session.undoDelete()
        assertEquals(TransactionDetailPhase.READY, session.state.value.phase)
        assertFalse(session.state.value.canUndoDelete)
    }

    @Test fun `command failure stays visible without losing selected transaction`() = runBlocking {
        val original = transaction()
        val session = TransactionDetailSession(FakeCommands(original, failUpdate = true))
        session.open(original)

        session.update(draft(500))

        assertEquals(TransactionDetailPhase.ERROR, session.state.value.phase)
        assertEquals(original, session.state.value.transaction)
        assertEquals("stale", session.state.value.errorMessage)
    }

    private fun draft(amount: Long, note: String? = null): TransactionDraft = TransactionDraft(
        TransactionType.EXPENSE,
        Money(amount, Currency.getInstance("CNY")),
        Instant.parse("2026-11-03T09:00:00Z"),
        AccountId(UUID(0, 2)),
        categoryId = CategoryId(UUID(0, 3)),
        note = note,
    )

    private fun transaction(): Transaction = Transaction(
        TransactionId(UUID(0, 1)),
        TransactionType.EXPENSE,
        Money(100, Currency.getInstance("CNY")),
        Instant.parse("2026-11-03T08:00:00Z"),
        AccountId(UUID(0, 2)),
        null,
        CategoryId(UUID(0, 3)),
        null,
        Instant.parse("2026-11-03T08:00:00Z"),
        Instant.parse("2026-11-03T08:00:00Z"),
    )
}

private class FakeCommands(
    private var current: Transaction,
    private val failUpdate: Boolean = false,
) : TransactionDetailCommands {
    var lastUpdate: UpdateTransactionCommand? = null
    private var deleted: Transaction? = null

    override suspend fun update(command: UpdateTransactionCommand): TransactionEdit {
        if (failUpdate) error("stale")
        lastUpdate = command
        val before = current
        current = before.copy(
            money = command.draft.money,
            note = command.draft.note?.trim(),
            updatedAt = before.updatedAt.plusSeconds(1),
        )
        return TransactionEdit(TransactionSnapshot(before), current)
    }

    override suspend fun delete(transaction: Transaction): DeleteReceipt {
        deleted = transaction
        return DeleteReceipt(TransactionSnapshot(transaction), Instant.parse("2026-11-03T10:00:00Z"))
    }

    override suspend fun undoLastDelete(): Transaction? = deleted?.copy(
        updatedAt = deleted!!.updatedAt.plusSeconds(2),
    ).also { deleted = null }
}
