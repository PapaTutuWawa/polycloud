package me.polynom.polycloud.apps.calendar.dates

import java.time.DayOfWeek
import java.time.ZonedDateTime
import kotlin.math.ceil

/**
 * Like isAfter but includes the date as well.
 *
 * @param other The date to check if we are after.
 * @return True, if we are after or equal to @param other.
 */
fun ZonedDateTime.isAfterInclusive(other: ZonedDateTime): Boolean = this.isAfter(other) || this.isEqual(other)

/**
 * Like isBefore but includes the date as well.
 *
 * @param other The date to check if we are before.
 * @return True, if we are before or equal to @param other.
 */
fun ZonedDateTime.isBeforeInclusive(other: ZonedDateTime): Boolean = this.isBefore(other) || this.isEqual(other)

/**
 * Checks if the date lies within a range.
 *
 * @param start     The start date of the range (inclusive).
 * @param end       The end date of the rnage (inclusive).
 */
fun ZonedDateTime.isInsideRange(
    start: ZonedDateTime,
    end: ZonedDateTime,
): Boolean = this.isBeforeInclusive(end) && this.isAfterInclusive(start)

/**
 * Computes the start day of the week the date is in.
 *
 * @return The ZonedDateTime.
 */
fun ZonedDateTime.weekStart(): ZonedDateTime = this.minusDays(this.dayOfWeek.value.toLong() - 1)

/**
 * Computes the n-th occurrence of the date's weekday in the month.
 *
 * If the day is the second Saturday of the month, then the return value is 2.
 *
 * @return The "n" of n-th weekday of the month.
 */
@Suppress("MagicNumber")
fun ZonedDateTime.nthWeekday(): Int = (this.dayOfMonth - 1).floorDiv(7) + 1

/**
 * Computes the next time after the current date that is of the specified weekday.
 *
 * @param day   The desired weekday.
 * @return The next {@link ZonedDateTime} that represents that weekday.
 */
fun ZonedDateTime.toNextWeekday(day: DayOfWeek): ZonedDateTime {
    if (this.dayOfWeek.value > day.value) {
        val offsetToSunday = 7 - this.dayOfWeek.value
        return this.plusDays((offsetToSunday + day.value).toLong())
    } else if (this.dayOfWeek == day) {
        return this.plusDays(7)
    }

    return this.plusDays((day.value - this.dayOfWeek.value).toLong())
}
