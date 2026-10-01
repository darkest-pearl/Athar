package dev.elm.prototype

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class StreakRulesTest {
    private fun credits(vararg dates: String) = dates.map {
        StudyDayCredit(it, 0, "Asia/Dubai")
    }

    @Test fun yesterdayRemainsCurrentAcrossMonthAndYearUntilMissed() {
        val days = credits("2025-12-31", "2026-01-01", "2026-01-02")
        assertEquals(StreakStats(3, 3, 3), streakStats(days, LocalDate.parse("2026-01-02")))
        assertEquals(StreakStats(3, 3, 3), streakStats(days, LocalDate.parse("2026-01-03")))
        assertEquals(StreakStats(0, 3, 3), streakStats(days, LocalDate.parse("2026-01-04")))
    }

    @Test fun gapsAndRepeatedDayKeepLongestAndUniqueTotal() {
        val days = credits("2026-01-01", "2026-01-01", "2026-01-03", "2026-01-04")
        assertEquals(StreakStats(2, 2, 3), streakStats(days, LocalDate.parse("2026-01-05")))
        assertEquals(StreakStats(0, 0, 0), streakStats(emptyList(), LocalDate.parse("2026-01-05")))
        assertEquals(StreakStats(1, 2, 3), streakStats(days, LocalDate.parse("2026-01-03")))
    }

    @Test fun pinnedZoneDeterminesStudyDayAtMidnight() {
        val zone = ZoneId.of("Asia/Dubai")
        assertEquals("2026-01-01", studyDay(Instant.parse("2025-12-31T20:00:00Z"), zone))
        assertEquals("2025-12-31", studyDay(Instant.parse("2025-12-31T19:59:59Z"), zone))
    }
}
