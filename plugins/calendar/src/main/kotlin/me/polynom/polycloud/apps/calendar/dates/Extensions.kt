package me.polynom.polycloud.apps.calendar.dates

import java.time.ZonedDateTime

fun ZonedDateTime.isAfterInclusive(other: ZonedDateTime): Boolean = this.isAfter(other) || this.isEqual(other)

fun ZonedDateTime.isBeforeInclusive(other: ZonedDateTime): Boolean = this.isBefore(other) || this.isEqual(other)

fun ZonedDateTime.isInsideRange(
    start: ZonedDateTime,
    end: ZonedDateTime,
): Boolean = this.isBeforeInclusive(end) && this.isAfterInclusive(start)

fun ZonedDateTime.weekStart(): ZonedDateTime = this.minusDays(this.dayOfWeek.value.toLong() - 1)
