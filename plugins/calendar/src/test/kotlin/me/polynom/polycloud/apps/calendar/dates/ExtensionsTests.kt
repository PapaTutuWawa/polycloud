package me.polynom.polycloud.apps.calendar.dates

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class ExtensionsTests {
    @Test
    fun isAfterInclusive() {
        val date = ZonedDateTime.of(2026, 10, 3, 0, 0, 0, 0, ZoneId.of("Europe/Berlin"))
        val dateAfter = ZonedDateTime.of(2026, 10, 4, 0, 0, 0, 0, ZoneId.of("Europe/Berlin"))
        val dateBefore = ZonedDateTime.of(2026, 10, 2, 0, 0, 0, 0, ZoneId.of("Europe/Berlin"))

        assertThat(date.isAfterInclusive(dateBefore)).isTrue()
        assertThat(date.isAfterInclusive(date)).isTrue()
        assertThat(date.isAfterInclusive(dateAfter)).isFalse()
    }

    @Test
    fun isBeforeInclusive() {
        val date = ZonedDateTime.of(2026, 10, 3, 0, 0, 0, 0, ZoneId.of("Europe/Berlin"))
        val dateAfter = ZonedDateTime.of(2026, 10, 4, 0, 0, 0, 0, ZoneId.of("Europe/Berlin"))
        val dateBefore = ZonedDateTime.of(2026, 10, 2, 0, 0, 0, 0, ZoneId.of("Europe/Berlin"))

        assertThat(date.isBeforeInclusive(dateBefore)).isFalse()
        assertThat(date.isBeforeInclusive(date)).isTrue()
        assertThat(date.isBeforeInclusive(dateAfter)).isTrue()
    }

    @Test
    fun isInsideRange() {
        val outsideBefore = ZonedDateTime.of(2026, 10, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin"))
        val outsideAfter = ZonedDateTime.of(2026, 10, 5, 0, 0, 0, 0, ZoneId.of("Europe/Berlin"))
        val date = ZonedDateTime.of(2026, 10, 3, 0, 0, 0, 0, ZoneId.of("Europe/Berlin"))
        val start = ZonedDateTime.of(2026, 10, 2, 0, 0, 0, 0, ZoneId.of("Europe/Berlin"))
        val end = ZonedDateTime.of(2026, 10, 4, 0, 0, 0, 0, ZoneId.of("Europe/Berlin"))

        assertThat(date.isInsideRange(start, end)).isTrue()
        assertThat(start.isInsideRange(start, end)).isTrue()
        assertThat(end.isInsideRange(start, end)).isTrue()

        assertThat(outsideAfter.isInsideRange(start, end)).isFalse()
        assertThat(outsideBefore.isInsideRange(start, end)).isFalse()
    }

    @Test
    fun weekStartMiddleOfTheMonth() {
        val date = ZonedDateTime.of(2026, 10, 15, 0, 0, 0, 0, ZoneId.of("Europe/Berlin"))
        val weekStart = date.weekStart()
        assertThat(weekStart.dayOfMonth).isEqualTo(12)
        assertThat(weekStart.monthValue).isEqualTo(10)
    }

    @Test
    fun weekStartMiddleOfTheMonday() {
        val date = ZonedDateTime.of(2026, 10, 12, 0, 0, 0, 0, ZoneId.of("Europe/Berlin"))
        val weekStart = date.weekStart()
        assertThat(weekStart.dayOfMonth).isEqualTo(12)
        assertThat(weekStart.monthValue).isEqualTo(10)
    }

    @Test
    fun weekStartStartOfMonth() {
        val date = ZonedDateTime.of(2026, 10, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin"))
        val weekStart = date.weekStart()
        assertThat(weekStart.dayOfMonth).isEqualTo(28)
        assertThat(weekStart.monthValue).isEqualTo(9)
    }
}
