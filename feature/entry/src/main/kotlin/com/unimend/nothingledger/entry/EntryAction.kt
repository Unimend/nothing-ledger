package com.unimend.nothingledger.entry

import com.unimend.nothingledger.model.AccountId
import com.unimend.nothingledger.model.CategoryId
import java.time.Instant

public sealed interface EntryAction {
    public data class EnterDigit(public val digit: Int) : EntryAction
    public data object EnterDecimalSeparator : EntryAction
    public data object BackspaceAmount : EntryAction
    public data object ClearAmount : EntryAction
    public data class ChangeMode(public val mode: EntryMode) : EntryAction
    public data class SelectSourceAccount(public val id: AccountId?) : EntryAction
    public data class SelectDestinationAccount(public val id: AccountId?) : EntryAction
    public data class SelectCategory(public val id: CategoryId?) : EntryAction
    public data class ChangeNote(public val note: String) : EntryAction
    public data class ChangeOccurredAt(public val instant: Instant) : EntryAction
    public data object RequestSave : EntryAction
    public data object ClearDraft : EntryAction
    public data object RestoreDraft : EntryAction
}

public data class EntrySubmission(
    public val mode: EntryMode,
    public val amountMinorUnits: Long,
    public val currencyCode: String,
    public val sourceAccountId: AccountId?,
    public val destinationAccountId: AccountId?,
    public val categoryId: CategoryId?,
    public val note: String?,
    public val occurredAt: Instant,
)

public sealed interface EntryEffect {
    public data class Submit(public val value: EntrySubmission) : EntryEffect
}

public data class EntryTransition(
    public val state: EntryState,
    public val effect: EntryEffect? = null,
)

