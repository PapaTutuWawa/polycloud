package me.polynom.polycloud.apps.calendar.dates

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime

class DailyRecurrenceTests {
    @Test
    fun testDailyRecurrenceUntil() {
        val recurrence =
            DailyRecurrence(
                ZonedDateTime.of(2026, 9, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                ZonedDateTime.of(2026, 9, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                ZonedDateTime.of(2026, 9, 6, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                ZonedDateTime.of(2026, 9, 17, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                null,
            )

        val events = mutableListOf<ZonedDateTime>()
        while (recurrence.hasNext()) {
            val (_, date) = recurrence.next()
            events.addLast(date)
        }

        assertThat(events).hasSize(6)
        assertThat(events).contains(
            ZonedDateTime.of(2026, 9, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
            ZonedDateTime.of(2026, 9, 2, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
            ZonedDateTime.of(2026, 9, 3, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
            ZonedDateTime.of(2026, 9, 4, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
            ZonedDateTime.of(2026, 9, 5, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
            ZonedDateTime.of(2026, 9, 6, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
        )
    }

    @Test
    fun testDailyRecurrenceUntilOutsideWindow() {
        val recurrence =
            DailyRecurrence(
                ZonedDateTime.of(2026, 9, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                ZonedDateTime.of(2026, 9, 21, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                ZonedDateTime.of(2026, 9, 27, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                ZonedDateTime.of(2026, 9, 17, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                null,
            )

        val events = mutableListOf<ZonedDateTime>()
        while (recurrence.hasNext()) {
            val (_, date) = recurrence.next()
            events.addLast(date)
        }

        assertThat(events).hasSize(0)
    }

    @Test
    fun testDailyRecurrenceCount() {
        val recurrence =
            DailyRecurrence(
                ZonedDateTime.of(2026, 9, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                ZonedDateTime.of(2026, 9, 7, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                ZonedDateTime.of(2026, 9, 13, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                null,
                100,
            )

        val events = mutableListOf<ZonedDateTime>()
        while (recurrence.hasNext()) {
            val (_, date) = recurrence.next()
            events.addLast(date)
        }

        assertThat(events).hasSize(7)
        assertThat(events).contains(
            ZonedDateTime.of(2026, 9, 7, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
            ZonedDateTime.of(2026, 9, 8, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
            ZonedDateTime.of(2026, 9, 9, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
            ZonedDateTime.of(2026, 9, 10, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
            ZonedDateTime.of(2026, 9, 11, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
            ZonedDateTime.of(2026, 9, 12, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
            ZonedDateTime.of(2026, 9, 13, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
        )
    }

    @Test
    fun testDailyRecurrenceCountOutsideWindow() {
        val recurrence =
            DailyRecurrence(
                ZonedDateTime.of(2026, 9, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                ZonedDateTime.of(2026, 9, 14, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                ZonedDateTime.of(2026, 9, 21, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                null,
                12,
            )

        val events = mutableListOf<ZonedDateTime>()
        while (recurrence.hasNext()) {
            val (_, date) = recurrence.next()
            events.addLast(date)
        }

        assertThat(events).hasSize(0)
    }
}

class WeeklyRecurrenceTests {
    @Test
    fun testWeeklyRecurrenceMultipleWeekdaysUntil() {
        val recurrence =
            WeeklyRecurrence(
                ZonedDateTime.of(2026, 9, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                ZonedDateTime.of(2026, 9, 7, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                ZonedDateTime.of(2026, 9, 20, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                ZonedDateTime.of(2031, 9, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                null,
                listOf(DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            )

        val events = mutableListOf<ZonedDateTime>()
        while (recurrence.hasNext()) {
            val (_, date) = recurrence.next()
            events.addLast(date)
        }

        assertThat(events).hasSize(4)
        assertThat(events).contains(
            ZonedDateTime.of(2026, 9, 9, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
            ZonedDateTime.of(2026, 9, 11, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
            ZonedDateTime.of(2026, 9, 16, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
            ZonedDateTime.of(2026, 9, 18, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
        )
    }

    @Test
    fun testWeeklyRecurrenceMultipleWeekdaysUntilViewBeforeEventStart() {
        val recurrence =
            WeeklyRecurrence(
                ZonedDateTime.of(2026, 9, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                ZonedDateTime.of(2026, 8, 7, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                ZonedDateTime.of(2026, 9, 20, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                ZonedDateTime.of(2031, 9, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                null,
                listOf(DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            )

        val events = mutableListOf<ZonedDateTime>()
        while (recurrence.hasNext()) {
            val (_, date) = recurrence.next()
            events.addLast(date)
        }

        assertThat(events).hasSize(7)
        assertThat(events).contains(
            ZonedDateTime.of(2026, 9, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
            ZonedDateTime.of(2026, 9, 2, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
            ZonedDateTime.of(2026, 9, 4, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
            ZonedDateTime.of(2026, 9, 9, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
            ZonedDateTime.of(2026, 9, 11, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
            ZonedDateTime.of(2026, 9, 16, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
            ZonedDateTime.of(2026, 9, 18, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
        )
    }

    @Test
    fun testWeeklyRecurrenceMultipleWeekdaysUntilViewAfterEventStart() {
        val recurrence =
            WeeklyRecurrence(
                ZonedDateTime.of(2026, 9, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                ZonedDateTime.of(2026, 9, 7, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                ZonedDateTime.of(2026, 9, 20, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                ZonedDateTime.of(2031, 9, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                null,
                listOf(DayOfWeek.TUESDAY),
            )

        val events = mutableListOf<ZonedDateTime>()
        while (recurrence.hasNext()) {
            val (_, date) = recurrence.next()
            events.addLast(date)
        }

        assertThat(events).hasSize(2)
        assertThat(events).contains(
            ZonedDateTime.of(2026, 9, 8, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
            ZonedDateTime.of(2026, 9, 15, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
        )
    }

    @Test
    fun testWeeklyRecurrenceMultipleWeekdaysUntilViewBeforeEventStartWithMonthStartingOnWeekday() {
        val recurrence =
            WeeklyRecurrence(
                ZonedDateTime.of(2026, 9, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                ZonedDateTime.of(2026, 8, 7, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                ZonedDateTime.of(2026, 9, 20, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                ZonedDateTime.of(2031, 9, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                null,
                listOf(DayOfWeek.TUESDAY),
            )

        val events = mutableListOf<ZonedDateTime>()
        while (recurrence.hasNext()) {
            val (_, date) = recurrence.next()
            events.addLast(date)
        }

        assertThat(events).hasSize(3)
        assertThat(events).contains(
            ZonedDateTime.of(2026, 9, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
            ZonedDateTime.of(2026, 9, 8, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
            ZonedDateTime.of(2026, 9, 15, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
        )
    }

    @Test
    fun testWeeklyRecurrenceMultipleWeekdaysUntilOutsideWindow() {
        val recurrence =
            WeeklyRecurrence(
                ZonedDateTime.of(2026, 9, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                ZonedDateTime.of(2026, 9, 7, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                ZonedDateTime.of(2026, 9, 20, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                ZonedDateTime.of(2026, 9, 11, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                null,
                listOf(DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            )

        val events = mutableListOf<ZonedDateTime>()
        while (recurrence.hasNext()) {
            val (_, date) = recurrence.next()
            events.addLast(date)
        }

        assertThat(events).hasSize(2)
        assertThat(events).contains(
            ZonedDateTime.of(2026, 9, 9, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
            ZonedDateTime.of(2026, 9, 11, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
        )
    }

    @Test
    fun testWeeklyRecurrenceMultipleWeekdaysCount() {
        val recurrence =
            WeeklyRecurrence(
                ZonedDateTime.of(2026, 9, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                ZonedDateTime.of(2026, 9, 7, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                ZonedDateTime.of(2026, 9, 27, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                null,
                8,
                listOf(DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            )

        val events = mutableListOf<ZonedDateTime>()
        while (recurrence.hasNext()) {
            val (_, date) = recurrence.next()
            events.addLast(date)
        }

        assertThat(events).hasSize(6)
        assertThat(events).contains(
            ZonedDateTime.of(2026, 9, 9, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
            ZonedDateTime.of(2026, 9, 11, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
            ZonedDateTime.of(2026, 9, 16, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
            ZonedDateTime.of(2026, 9, 18, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
            ZonedDateTime.of(2026, 9, 23, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
            ZonedDateTime.of(2026, 9, 25, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
        )
    }
}

class MonthlyRepetitionTests {
    @Nested
    inner class DayOfTheMonth {
        @Test
        fun testMonthlyRecurrenceUntil() {
            val recurrence =
                MonthlyRecurrence(
                    ZonedDateTime.of(2026, 9, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                    ZonedDateTime.of(2026, 8, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                    ZonedDateTime.of(2026, 12, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                    ZonedDateTime.of(2031, 9, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                    null,
                    MonthlyRecurrence.MonthlyRecurrenceMode.DAY_OF_THE_MONTH,
                )

            val events = mutableListOf<ZonedDateTime>()
            while (recurrence.hasNext()) {
                val (_, date) = recurrence.next()
                events.addLast(date)
            }

            assertThat(events).hasSize(4)
            assertThat(events).contains(
                ZonedDateTime.of(2026, 9, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                ZonedDateTime.of(2026, 10, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                ZonedDateTime.of(2026, 11, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                ZonedDateTime.of(2026, 12, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
            )
        }

        @Test
        fun testMonthlyRecurrenceUntilViewStartsAfterEvent() {
            val recurrence =
                MonthlyRecurrence(
                    ZonedDateTime.of(2026, 9, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                    ZonedDateTime.of(2026, 10, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                    ZonedDateTime.of(2026, 10, 30, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                    ZonedDateTime.of(2031, 9, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                    null,
                    MonthlyRecurrence.MonthlyRecurrenceMode.DAY_OF_THE_MONTH,
                )

            val events = mutableListOf<ZonedDateTime>()
            while (recurrence.hasNext()) {
                val (_, date) = recurrence.next()
                events.addLast(date)
            }

            assertThat(events).hasSize(1)
            assertThat(events).contains(
                ZonedDateTime.of(2026, 10, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
            )
        }

        @Test
        fun testMonthlyRecurrenceUntilOutsideWindow() {
            val recurrence =
                MonthlyRecurrence(
                    ZonedDateTime.of(2026, 9, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                    ZonedDateTime.of(2026, 8, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                    ZonedDateTime.of(2027, 1, 30, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                    ZonedDateTime.of(2026, 12, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                    null,
                    MonthlyRecurrence.MonthlyRecurrenceMode.DAY_OF_THE_MONTH,
                )

            val events = mutableListOf<ZonedDateTime>()
            while (recurrence.hasNext()) {
                val (_, date) = recurrence.next()
                events.addLast(date)
            }

            assertThat(events).hasSize(4)
            assertThat(events).contains(
                ZonedDateTime.of(2026, 9, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                ZonedDateTime.of(2026, 10, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                ZonedDateTime.of(2026, 11, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                ZonedDateTime.of(2026, 12, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
            )
        }
    }

    @Nested
    inner class NthDayOfTheMonth {
        @Test
        fun testMonthlyRecurrenceUntil() {
            val recurrence =
                MonthlyRecurrence(
                    ZonedDateTime.of(2026, 9, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                    ZonedDateTime.of(2026, 8, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                    ZonedDateTime.of(2026, 12, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                    ZonedDateTime.of(2031, 9, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                    null,
                    MonthlyRecurrence.MonthlyRecurrenceMode.NTH_DAY_OF_THE_MONTH,
                )

            val events = mutableListOf<ZonedDateTime>()
            while (recurrence.hasNext()) {
                val (_, date) = recurrence.next()
                events.addLast(date)
            }

            assertThat(events).hasSize(4)
            assertThat(events).contains(
                ZonedDateTime.of(2026, 9, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                ZonedDateTime.of(2026, 10, 6, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                ZonedDateTime.of(2026, 11, 3, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                ZonedDateTime.of(2026, 12, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
            )
        }

        @Test
        fun testMonthlyRecurrenceUntilViewAfterEventStart() {
            val recurrence =
                MonthlyRecurrence(
                    ZonedDateTime.of(2026, 9, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                    ZonedDateTime.of(2026, 10, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                    ZonedDateTime.of(2026, 12, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                    ZonedDateTime.of(2031, 9, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                    null,
                    MonthlyRecurrence.MonthlyRecurrenceMode.NTH_DAY_OF_THE_MONTH,
                )

            val events = mutableListOf<ZonedDateTime>()
            while (recurrence.hasNext()) {
                val (_, date) = recurrence.next()
                events.addLast(date)
            }

            assertThat(events).hasSize(3)
            assertThat(events).contains(
                ZonedDateTime.of(2026, 10, 6, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                ZonedDateTime.of(2026, 11, 3, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                ZonedDateTime.of(2026, 12, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
            )
        }

        @Test
        fun testMonthlyRecurrenceUntilMiddleOfTheMonth() {
            val recurrence =
                MonthlyRecurrence(
                    ZonedDateTime.of(2026, 9, 17, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                    ZonedDateTime.of(2026, 8, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                    ZonedDateTime.of(2026, 12, 30, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                    ZonedDateTime.of(2031, 9, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                    null,
                    MonthlyRecurrence.MonthlyRecurrenceMode.NTH_DAY_OF_THE_MONTH,
                )

            val events = mutableListOf<ZonedDateTime>()
            while (recurrence.hasNext()) {
                val (_, date) = recurrence.next()
                events.addLast(date)
            }

            assertThat(events).hasSize(4)
            assertThat(events).contains(
                ZonedDateTime.of(2026, 9, 17, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                ZonedDateTime.of(2026, 10, 15, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                ZonedDateTime.of(2026, 11, 19, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                ZonedDateTime.of(2026, 12, 17, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
            )
        }

        @Test
        fun testMonthlyRecurrenceUntilOutsideWindow() {
            val recurrence =
                MonthlyRecurrence(
                    ZonedDateTime.of(2026, 9, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                    ZonedDateTime.of(2026, 8, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                    ZonedDateTime.of(2027, 1, 30, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                    ZonedDateTime.of(2026, 12, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                    null,
                    MonthlyRecurrence.MonthlyRecurrenceMode.NTH_DAY_OF_THE_MONTH,
                )

            val events = mutableListOf<ZonedDateTime>()
            while (recurrence.hasNext()) {
                val (_, date) = recurrence.next()
                events.addLast(date)
            }

            assertThat(events).hasSize(4)
            assertThat(events).contains(
                ZonedDateTime.of(2026, 9, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                ZonedDateTime.of(2026, 10, 6, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                ZonedDateTime.of(2026, 11, 3, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                ZonedDateTime.of(2026, 12, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
            )
        }
    }
}

class YearlyRecurrenceTests {
    @Test
    fun testYearlyRecurrenceUntil() {
        val recurrence =
            YearlyRecurrence(
                ZonedDateTime.of(2026, 9, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                ZonedDateTime.of(2030, 1, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                ZonedDateTime.of(2031, 9, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                null,
            )

        val events = mutableListOf<ZonedDateTime>()
        while (recurrence.hasNext()) {
            val (_, date) = recurrence.next()
            events.addLast(date)
        }

        assertThat(events).hasSize(3)
        assertThat(events).contains(
            ZonedDateTime.of(2027, 9, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
            ZonedDateTime.of(2028, 9, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
            ZonedDateTime.of(2029, 9, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
        )
    }

    @Test
    fun testYearlyRecurrenceUntilViewBeforeEventStart() {
        val recurrence =
            YearlyRecurrence(
                ZonedDateTime.of(2026, 9, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                ZonedDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                ZonedDateTime.of(2030, 1, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                ZonedDateTime.of(2031, 9, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                null,
            )

        val events = mutableListOf<ZonedDateTime>()
        while (recurrence.hasNext()) {
            val (_, date) = recurrence.next()
            events.addLast(date)
        }

        assertThat(events).hasSize(4)
        assertThat(events).contains(
            ZonedDateTime.of(2026, 9, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
            ZonedDateTime.of(2027, 9, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
            ZonedDateTime.of(2028, 9, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
            ZonedDateTime.of(2029, 9, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
        )
    }

    @Test
    fun testYearlyRecurrenceUntilOutsideWindow() {
        val recurrence =
            YearlyRecurrence(
                ZonedDateTime.of(2026, 9, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                ZonedDateTime.of(2032, 1, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                ZonedDateTime.of(2038, 1, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                ZonedDateTime.of(2031, 9, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                null,
            )

        val events = mutableListOf<ZonedDateTime>()
        while (recurrence.hasNext()) {
            val (_, date) = recurrence.next()
            events.addLast(date)
        }

        assertThat(events).hasSize(0)
    }

    @Test
    fun testYearlyRecurrenceCount() {
        val recurrence =
            YearlyRecurrence(
                ZonedDateTime.of(2026, 9, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                ZonedDateTime.of(2030, 1, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                null,
                4,
            )

        val events = mutableListOf<ZonedDateTime>()
        while (recurrence.hasNext()) {
            val (_, date) = recurrence.next()
            events.addLast(date)
        }

        assertThat(events).hasSize(3)
        assertThat(events).contains(
            ZonedDateTime.of(2027, 9, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
            ZonedDateTime.of(2028, 9, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
            ZonedDateTime.of(2029, 9, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
        )
    }

    @Test
    fun testYearlyRecurrenceCountOutsideWindow() {
        val recurrence =
            YearlyRecurrence(
                ZonedDateTime.of(2026, 9, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                ZonedDateTime.of(2032, 1, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                ZonedDateTime.of(2038, 1, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")),
                null,
                4,
            )

        val events = mutableListOf<ZonedDateTime>()
        while (recurrence.hasNext()) {
            val (_, date) = recurrence.next()
            events.addLast(date)
        }

        assertThat(events).hasSize(0)
    }
}
