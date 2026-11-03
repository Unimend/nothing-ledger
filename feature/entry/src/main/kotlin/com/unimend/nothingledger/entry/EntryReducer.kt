package com.unimend.nothingledger.entry

public object EntryReducer {
    public fun reduce(state: EntryState, action: EntryAction): EntryTransition = when (action) {
        is EntryAction.EnterDigit -> transition(state.copy(amount = state.amount.append(action.digit)))
        EntryAction.EnterDecimalSeparator -> transition(state.copy(amount = state.amount.appendDecimalSeparator()))
        EntryAction.BackspaceAmount -> transition(state.copy(amount = state.amount.backspace()))
        EntryAction.ClearAmount -> transition(state.copy(amount = AmountInput()))
        is EntryAction.ChangeMode -> transition(changeMode(state, action.mode))
        is EntryAction.SelectSourceAccount -> transition(state.copy(sourceAccountId = action.id))
        is EntryAction.SelectDestinationAccount -> transition(state.copy(destinationAccountId = action.id))
        is EntryAction.SelectCategory -> transition(state.copy(categoryId = action.id))
        is EntryAction.ChangeNote -> transition(state.copy(note = action.note))
        is EntryAction.ChangeOccurredAt -> transition(state.copy(occurredAt = action.instant))
        EntryAction.RequestSave -> requestSave(state)
        EntryAction.ClearDraft -> transition(clearDraft(state))
        EntryAction.RestoreDraft -> transition(restoreDraft(state))
    }

    private fun transition(state: EntryState): EntryTransition =
        EntryTransition(state.copy(validationErrors = emptySet()))

    private fun changeMode(state: EntryState, mode: EntryMode): EntryState = when (mode) {
        EntryMode.EXPENSE -> state.copy(
            mode = mode,
            destinationAccountId = null,
        )
        EntryMode.INCOME -> state.copy(
            mode = mode,
            sourceAccountId = null,
        )
        EntryMode.TRANSFER -> state.copy(
            mode = mode,
            categoryId = null,
        )
    }

    private fun requestSave(state: EntryState): EntryTransition {
        val errors = buildSet {
            if (state.amount.minorUnits == 0L) add(EntryValidationError.AMOUNT_REQUIRED)
            if (state.note.length > MAX_NOTE_LENGTH) add(EntryValidationError.NOTE_TOO_LONG)
            when (state.mode) {
                EntryMode.EXPENSE -> {
                    if (state.sourceAccountId == null) add(EntryValidationError.SOURCE_ACCOUNT_REQUIRED)
                    if (state.categoryId == null) add(EntryValidationError.CATEGORY_REQUIRED)
                }
                EntryMode.INCOME -> {
                    if (state.destinationAccountId == null) add(EntryValidationError.DESTINATION_ACCOUNT_REQUIRED)
                    if (state.categoryId == null) add(EntryValidationError.CATEGORY_REQUIRED)
                }
                EntryMode.TRANSFER -> {
                    if (state.sourceAccountId == null) add(EntryValidationError.SOURCE_ACCOUNT_REQUIRED)
                    if (state.destinationAccountId == null) add(EntryValidationError.DESTINATION_ACCOUNT_REQUIRED)
                    if (state.sourceAccountId != null && state.sourceAccountId == state.destinationAccountId) {
                        add(EntryValidationError.SAME_TRANSFER_ACCOUNT)
                    }
                }
            }
        }
        if (errors.isNotEmpty()) return EntryTransition(state.copy(validationErrors = errors))
        val submission = EntrySubmission(
            mode = state.mode,
            amountMinorUnits = state.amount.minorUnits,
            currencyCode = state.currency.currencyCode,
            sourceAccountId = state.sourceAccountId,
            destinationAccountId = state.destinationAccountId,
            categoryId = state.categoryId,
            note = state.note.trim().ifEmpty { null },
            occurredAt = state.occurredAt,
        )
        return EntryTransition(state.copy(validationErrors = emptySet()), EntryEffect.Submit(submission))
    }

    private fun clearDraft(state: EntryState): EntryState = EntryState.initial(
        now = state.occurredAt,
        currency = state.currency,
    ).copy(recoverableDraft = state.snapshot())

    private fun restoreDraft(state: EntryState): EntryState {
        val draft = state.recoverableDraft ?: return state
        return state.copy(
            mode = draft.mode,
            amount = draft.amount,
            sourceAccountId = draft.sourceAccountId,
            destinationAccountId = draft.destinationAccountId,
            categoryId = draft.categoryId,
            note = draft.note,
            occurredAt = draft.occurredAt,
            validationErrors = emptySet(),
            recoverableDraft = null,
        )
    }

    private const val MAX_NOTE_LENGTH: Int = 500
}

