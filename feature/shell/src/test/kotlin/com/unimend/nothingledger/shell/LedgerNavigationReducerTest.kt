package com.unimend.nothingledger.shell

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class LedgerNavigationReducerTest {
    private val transaction = NavigationKey("transaction-1")

    @Test
    fun `page stack preserves the path from list to detail to edit`() {
        val detail = PageDestination.TransactionDetail(transaction)
        val edit = PageDestination.TransactionEdit(transaction)

        val openedDetail = reduce(LedgerNavigationState(), LedgerNavigationAction.PushPage(detail))
        val openedEdit = reduce(openedDetail, LedgerNavigationAction.PushPage(edit))
        val backToDetail = reduce(openedEdit, LedgerNavigationAction.NavigateBack)

        assertEquals(listOf(detail, edit), openedEdit.pageStack)
        assertEquals(detail, backToDetail.visiblePage)
    }

    @Test
    fun `modal closes before page stack is popped`() {
        val detail = PageDestination.TransactionDetail(transaction)
        val modal = ModalDestination.DeleteTransactionConfirmation(transaction)
        val state = LedgerNavigationState(pageStack = listOf(detail), modal = modal)

        val firstBack = reduce(state, LedgerNavigationAction.NavigateBack)
        val secondBack = reduce(firstBack, LedgerNavigationAction.NavigateBack)

        assertNull(firstBack.modal)
        assertEquals(detail, firstBack.visiblePage)
        assertNull(secondBack.visiblePage)
    }

    @Test
    fun `selecting a main page clears all transient navigation`() {
        val state = LedgerNavigationState(
            pageStack = listOf(PageDestination.QuickEntry),
            modal = ModalDestination.CategoryPicker,
        )

        val result = reduce(state, LedgerNavigationAction.SelectMain(MainDestination.ACCOUNTS))

        assertEquals(MainDestination.ACCOUNTS, result.selectedMain)
        assertEquals(emptyList<PageDestination>(), result.pageStack)
        assertNull(result.modal)
    }

    @Test
    fun `opening a page closes a stale modal`() {
        val state = LedgerNavigationState(modal = ModalDestination.AccountPicker)
        val result = reduce(state, LedgerNavigationAction.PushPage(PageDestination.Search))

        assertEquals(PageDestination.Search, result.visiblePage)
        assertNull(result.modal)
    }

    @Test
    fun `back at root requests host exit`() {
        val state = LedgerNavigationState(selectedMain = MainDestination.CALENDAR)
        val transition = LedgerNavigationReducer.reduce(state, LedgerNavigationAction.NavigateBack)

        assertEquals(state, transition.state)
        assertEquals(LedgerNavigationEffect.ExitRequested, transition.effect)
    }

    private fun reduce(
        state: LedgerNavigationState,
        action: LedgerNavigationAction,
    ): LedgerNavigationState = LedgerNavigationReducer.reduce(state, action).state
}
