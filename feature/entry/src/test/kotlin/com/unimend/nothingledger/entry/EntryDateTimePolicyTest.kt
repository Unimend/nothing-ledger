package com.unimend.nothingledger.entry

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EntryDateTimePolicyTest {
    @Test fun `selecting date preserves local time`() {
        val zone = ZoneId.of("Asia/Shanghai")
        val value = Instant.parse("2026-11-03T05:45:00Z")

        val changed = replaceEntryDate(value, LocalDate.of(2026, 10, 28), zone).atZone(zone)

        assertEquals(LocalDate.of(2026, 10, 28), changed.toLocalDate())
        assertEquals(LocalTime.of(13, 45), changed.toLocalTime())
    }

    @Test fun `yesterday uses local calendar date across daylight saving boundary`() {
        val zone = ZoneId.of("America/New_York")
        val now = Instant.parse("2026-03-09T16:00:00Z")
        val value = Instant.parse("2026-03-09T13:30:00Z")

        val yesterday = selectRelativeEntryDate(value, now, -1, zone).atZone(zone)

        assertEquals(LocalDate.of(2026, 3, 8), yesterday.toLocalDate())
        assertEquals(LocalTime.of(9, 30), yesterday.toLocalTime())
    }

    @Test fun `time replacement and minute shifts are explicit`() {
        val zone = ZoneId.of("Asia/Shanghai")
        val value = Instant.parse("2026-11-03T15:50:00Z")

        val replaced = replaceEntryTime(value, LocalTime.of(8, 5), zone)
        assertEquals(LocalTime.of(8, 5), replaced.atZone(zone).toLocalTime())
        val shifted = shiftEntryMinutes(replaced, -15, zone).atZone(zone)
        assertEquals(LocalTime.of(7, 50), shifted.toLocalTime())
    }

    @Test fun `date grid is Monday first and contains complete weeks`() {
        val grid = buildEntryDateGrid(YearMonth.of(2026, 11))

        assertTrue(grid.weeks.all { it.size == 7 })
        assertEquals(6, grid.weeks.first().indexOfFirst { it?.dayOfMonth == 1 })
        assertEquals(30, grid.weeks.flatten().filterNotNull().size)
    }
}
