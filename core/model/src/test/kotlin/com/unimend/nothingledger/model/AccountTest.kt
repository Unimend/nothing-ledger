package com.unimend.nothingledger.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.Currency
import java.util.UUID

class AccountTest {
    private val cny = Currency.getInstance("CNY")

    @Test fun `negative opening balance is valid for liability accounts`() {
        val account = account(AccountKind.CREDIT, -50_000)
        assertEquals(-50_000, account.openingBalance.minorUnits)
    }

    @Test fun `account archive and restore are explicit state transitions`() {
        val active = account(AccountKind.BANK, 0)
        val archived = active.archive(Instant.EPOCH)
        assertTrue(archived.isArchived)
        assertFalse(archived.restore().isArchived)
        assertThrows(IllegalArgumentException::class.java) { archived.archive(Instant.EPOCH) }
        assertThrows(IllegalArgumentException::class.java) { active.restore() }
    }

    private fun account(kind: AccountKind, opening: Long) = Account(
        id = AccountId(UUID.randomUUID()),
        name = "Main",
        kind = kind,
        openingBalance = AccountBalance(opening, cny),
        displayOrder = 0,
        archivedAt = null,
    )
}

