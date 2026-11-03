package com.unimend.nothingledger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.unimend.nothingledger.database.LedgerDatabaseHandle
import com.unimend.nothingledger.timeline.ObserveTimeline
import com.unimend.nothingledger.timeline.TimelineFilterAction
import com.unimend.nothingledger.timeline.TimelineFilterReducer
import com.unimend.nothingledger.timeline.TimelineFilterSession
import com.unimend.nothingledger.timeline.TimelineState
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

internal class TimelineViewModel(
    database: LedgerDatabaseHandle,
    zoneId: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {
    private val currentMonth = YearMonth.now(zoneId)
    private val observer = ObserveTimeline(database.transactions(), zoneId)
    private val _filterSession = MutableStateFlow(TimelineFilterSession.initial(currentMonth))
    val filterSession: StateFlow<TimelineFilterSession> = _filterSession.asStateFlow()
    val uiState: StateFlow<TimelineState> = _filterSession
        .flatMapLatest { session -> observer(session.query) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            TimelineState.Loading(_filterSession.value.query),
        )

    fun dispatch(action: TimelineFilterAction) {
        _filterSession.value = TimelineFilterReducer.reduce(_filterSession.value, action)
    }

    fun showPreviousMonth() { dispatch(TimelineFilterAction.PreviousMonth) }
    fun showNextMonth() { dispatch(TimelineFilterAction.NextMonth) }
    fun showCurrentMonth() { dispatch(TimelineFilterAction.SelectMonth(currentMonth)) }

    companion object {
        fun factory(database: LedgerDatabaseHandle): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                    require(modelClass.isAssignableFrom(TimelineViewModel::class.java))
                    @Suppress("UNCHECKED_CAST")
                    return TimelineViewModel(database) as T
                }
            }
    }
}
