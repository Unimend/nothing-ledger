package com.unimend.nothingledger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.unimend.nothingledger.database.LedgerDatabaseHandle
import com.unimend.nothingledger.timeline.ObserveTimeline
import com.unimend.nothingledger.timeline.TimelineQuery
import com.unimend.nothingledger.timeline.TimelineState
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

internal class TimelineViewModel(
    database: LedgerDatabaseHandle,
    zoneId: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {
    private val query = TimelineQuery(YearMonth.now(zoneId))
    val uiState: StateFlow<TimelineState> = ObserveTimeline(database.transactions(), zoneId)(query)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TimelineState.Loading(query))

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
