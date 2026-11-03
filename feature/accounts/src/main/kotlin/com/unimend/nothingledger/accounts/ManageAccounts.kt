package com.unimend.nothingledger.accounts

import com.unimend.nothingledger.domain.AccountRepository
import com.unimend.nothingledger.model.Account
import com.unimend.nothingledger.model.AccountBalance
import com.unimend.nothingledger.model.AccountId
import java.time.Clock
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** The only write boundary of the account feature. Current balances cannot be written. */
public class ManageAccounts(
    private val repository: AccountRepository,
    private val clock: Clock,
    private val newId: () -> AccountId = { AccountId(UUID.randomUUID()) },
) {
    private val writes = Mutex()

    public suspend fun create(command: CreateAccountCommand): Account = writes.withLock {
        val existing = repository.observeAll().first()
        val name = normalizeName(command.name)
        require(existing.none { it.name.equals(name, ignoreCase = true) && !it.isArchived }) {
            "An active account with this name already exists"
        }
        val nextOrder = existing.maxOfOrNull(Account::displayOrder)?.let(Math::incrementExact) ?: 0
        return Account(
            newId(), name, command.kind,
            AccountBalance(command.openingBalanceMinor, command.currency), nextOrder, null,
        ).also { repository.save(it) }
    }

    public suspend fun rename(id: AccountId, rawName: String): Account = writes.withLock {
        val account = requireNotNull(repository.findById(id)) { "Account does not exist" }
        val name = normalizeName(rawName)
        val duplicate = repository.observeAll().first().any {
            it.id != id && !it.isArchived && it.name.equals(name, ignoreCase = true)
        }
        require(!duplicate) { "An active account with this name already exists" }
        return account.copy(name = name).also { repository.save(it) }
    }

    public suspend fun archive(id: AccountId): Account = writes.withLock {
        val account = requireNotNull(repository.findById(id)) { "Account does not exist" }
        if (account.isArchived) return account
        val activeCount = repository.observeAll().first().count { !it.isArchived }
        require(activeCount > 1) { "At least one active account must remain" }
        return account.archive(clock.instant()).also { repository.save(it) }
    }

    private fun normalizeName(rawName: String): String = rawName.trim().also {
        require(it.isNotEmpty()) { "Account name must not be blank" }
    }
}
