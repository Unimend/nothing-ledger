package com.unimend.nothingledger.entry

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId

public data class EntryDateGrid(
    public val month: YearMonth,
    /** Monday-first complete weeks; null cells are outside [month]. */
    public val weeks: List<List<LocalDate?>>,
)

public fun buildEntryDateGrid(month: YearMonth): EntryDateGrid {
    val leading = month.atDay(1).dayOfWeek.value - 1
    val cells = buildList<LocalDate?> {
        repeat(leading) { add(null) }
        repeat(month.lengthOfMonth()) { offset -> add(month.atDay(offset + 1)) }
        while (size % DAYS_PER_WEEK != 0) add(null)
    }
    return EntryDateGrid(month, cells.chunked(DAYS_PER_WEEK))
}

/** Replaces the local calendar date while preserving the locally displayed time. */
public fun replaceEntryDate(
    value: Instant,
    date: LocalDate,
    zoneId: ZoneId,
): Instant {
    val current = value.atZone(zoneId)
    return date.atTime(current.toLocalTime()).atZone(zoneId).toInstant()
}

public fun replaceEntryTime(
    value: Instant,
    time: LocalTime,
    zoneId: ZoneId,
): Instant {
    val current = value.atZone(zoneId)
    return current.toLocalDate().atTime(time).atZone(zoneId).toInstant()
}

/** Selects today/yesterday by local date, not by subtracting a fixed 24-hour duration. */
public fun selectRelativeEntryDate(
    value: Instant,
    now: Instant,
    dayOffset: Long,
    zoneId: ZoneId,
): Instant = replaceEntryDate(value, now.atZone(zoneId).toLocalDate().plusDays(dayOffset), zoneId)

public fun shiftEntryMinutes(
    value: Instant,
    minutes: Long,
    zoneId: ZoneId,
): Instant = value.atZone(zoneId).plusMinutes(minutes).toInstant()

private const val DAYS_PER_WEEK: Int = 7
