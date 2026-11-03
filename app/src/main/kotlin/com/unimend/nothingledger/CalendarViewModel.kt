package com.unimend.nothingledger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.unimend.nothingledger.database.LedgerDatabaseHandle
import com.unimend.nothingledger.timeline.CalendarQuery
import com.unimend.nothingledger.timeline.CalendarState
import com.unimend.nothingledger.timeline.ObserveCalendar
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
internal class CalendarViewModel(
    database: LedgerDatabaseHandle,
    clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {
    private val zoneId: ZoneId = clock.zone
    private val query = MutableStateFlow(CalendarQuery(YearMonth.now(clock)))
    private val observeCalendar = ObserveCalendar(database.transactions(), zoneId)

    val state: StateFlow<CalendarState> = query
        .flatMapLatest(observeCalendar::invoke)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CalendarState.Loading(query.value),
        )

    fun previousMonth() {
        query.value = CalendarQuery(query.value.month.minusMonths(1))
    }

    fun nextMonth() {
        query.value = CalendarQuery(query.value.month.plusMonths(1))
    }

    fun selectDate(date: LocalDate) {
        if (YearMonth.from(date) != query.value.month) return
        query.value = query.value.copy(
            selectedDate = date.takeUnless { it == query.value.selectedDate },
        )
    }

    companion object {
        fun factory(database: LedgerDatabaseHandle): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                    require(modelClass.isAssignableFrom(CalendarViewModel::class.java))
                    @Suppress("UNCHECKED_CAST")
                    return CalendarViewModel(database) as T
                }
            }
    }
}
