package me.polynom.polycloud.apps.calendar.dates

import java.time.DayOfWeek
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/**
 * Base class for recurrence rules.
 */
abstract class BaseRecurrence(
    /** The date to start on. This is implicitly occurrence 0. */
    val startDate: ZonedDateTime,
    /** The start of the view. */
    val viewStartDate: ZonedDateTime,
    /** The end of the view. */
    val viewEndDate: ZonedDateTime,
    /** The end date of the event. */
    val endDate: ZonedDateTime?,
    /** The number of occurrences this event should have. */
    val occurrences: Int?,
) {
    /** The repetition counter. */
    protected var counter: Long = 0L

    /**
     * Computes the next occurrence of the event.
     *
     * @param n  The next occurrence counter.
     * @return The date the next event will occur.
     */
    protected abstract fun nextOccurrence(n: Long): ZonedDateTime

    /**
     * Computes if there is a next recurring event.
     *
     * @return True, if there is one. False, if not.
     */
    @Suppress("ReturnCount")
    fun hasNext(): Boolean {
        if (counter < 0) {
            return false
        }
        val nextOccurrence = nextOccurrence(counter + 1)
        if (occurrences != null) {
            return counter + 1 <= occurrences && nextOccurrence.isInsideRange(viewStartDate, viewEndDate)
        }

        // Until date has to be non-null
        var condition = nextOccurrence.isInsideRange(viewStartDate, viewEndDate)
        if (endDate != null) {
            condition = condition && nextOccurrence.isBeforeInclusive(endDate)
        }
        return condition
    }

    /**
     * Returns the next event.
     *
     * @return A pair of the event index and the next date the event will occur on.
     */
    open fun next(): Pair<Long, ZonedDateTime> {
        val nextOccurrence = nextOccurrence(++counter)
        return Pair(counter, nextOccurrence)
    }
}

/**
 * Recurrence rules for daily events.
 */
class DailyRecurrence(
    /** The date to start on. This is implicitly occurrence 0. */
    startDate: ZonedDateTime,
    /** The start of the view. */
    viewStartDate: ZonedDateTime,
    /** The end of the view. */
    viewEndDate: ZonedDateTime,
    /** The end date of the event. */
    endDate: ZonedDateTime?,
    /** The number of occurrences this event should have. */
    occurrences: Int?,
) : BaseRecurrence(startDate, viewStartDate, viewEndDate, endDate, occurrences) {
    init {
        assert((endDate != null).xor(occurrences != null))
        counter = ChronoUnit.DAYS.between(
            startDate.truncatedTo(ChronoUnit.DAYS),
            viewStartDate.truncatedTo(ChronoUnit.DAYS),
        ) - 1
    }

    override fun nextOccurrence(n: Long): ZonedDateTime =
        startDate
            .truncatedTo(ChronoUnit.DAYS)
            .plusDays(n)
}

/**
 * Recurrence processor for weekly repeating events
 */
class WeeklyRecurrence(
    /** The date to start on. This is implicitly occurrence 0. */
    startDate: ZonedDateTime,
    /** The start of the view. */
    viewStartDate: ZonedDateTime,
    /** The end of the view. */
    viewEndDate: ZonedDateTime,
    /** The end date of the event. */
    endDate: ZonedDateTime?,
    /** The number of occurrences this event should have. */
    occurrences: Int?,
    /** Weekdays that the event should occur on. */
    val weekdays: List<DayOfWeek>,
) : BaseRecurrence(startDate, viewStartDate, viewEndDate, endDate, occurrences) {
    /** Last event we have processed. */
    private lateinit var lastOccurrence: ZonedDateTime

    init {
        assert((endDate != null).xor(occurrences != null))
        assert(weekdays.isNotEmpty())

        val weekStart = startDate.weekStart()
        val viewWeekStart = viewStartDate.weekStart()
        if (weekStart.isEqual(viewWeekStart)) {
            counter = 0
        } else {
            // Special handling for the week of the event.
            val weekdayValues = weekdays.filter { it.value > startDate.dayOfWeek.value }
            val weeksBetween =
                ChronoUnit.WEEKS.between(
                    weekStart.truncatedTo(ChronoUnit.DAYS),
                    viewWeekStart.truncatedTo(ChronoUnit.DAYS),
                )
            counter = weeksBetween * weekdays.size + weekdayValues.size
            if (weeksBetween == 0L) {
                if (weekdayValues.isEmpty()) {
                    lastOccurrence = startDate
                } else {
                    val maxWeekdayValue = weekdayValues.maxBy { it.value }.value.toLong()
                    lastOccurrence = startDate.plusDays(maxWeekdayValue - startDate.dayOfWeek.value)
                }
            } else {
                val maxWeekdayValue = weekdayValues.maxBy { it.value }.value.toLong()
                lastOccurrence = viewWeekStart.minusWeeks(1L).plusDays(maxWeekdayValue)
            }
        }
    }

    override fun nextOccurrence(n: Long): ZonedDateTime {
        val nextWeekday =
            weekdays
                .filter { it.value > lastOccurrence.dayOfWeek.value }
                .minByOrNull { it.value }
                ?.value
                ?.toLong()
        if (nextWeekday != null) {
            return lastOccurrence
                .truncatedTo(ChronoUnit.DAYS)
                .plusDays(nextWeekday - startDate.dayOfWeek.value - 1)
        }

        val sundayDistance = DayOfWeek.SUNDAY.value.toLong() - lastOccurrence.dayOfWeek.value
        val nextWeekDay = weekdays.minBy { it.value }.value
        return lastOccurrence
            .truncatedTo(ChronoUnit.DAYS)
            .plusDays(sundayDistance + nextWeekDay)
    }

    override fun next(): Pair<Long, ZonedDateTime> {
        val value = super.next()
        lastOccurrence = value.second
        return value
    }
}

/**
 * Recurrence rules for yearly events.
 */
class YearlyRecurrence(
    /** The date to start on. This is implicitly occurrence 0. */
    startDate: ZonedDateTime,
    /** The start of the view. */
    viewStartDate: ZonedDateTime,
    /** The end of the view. */
    viewEndDate: ZonedDateTime,
    /** The end date of the event. */
    endDate: ZonedDateTime?,
    /** The number of occurrences this event should have. */
    occurrences: Int?,
) : BaseRecurrence(startDate, viewStartDate, viewEndDate, endDate, occurrences) {
    init {
        assert((endDate != null).xor(occurrences != null))
        counter =
            ChronoUnit.YEARS.between(
                startDate.truncatedTo(ChronoUnit.DAYS),
                viewStartDate.truncatedTo(ChronoUnit.DAYS),
            )
    }

    override fun nextOccurrence(n: Long): ZonedDateTime =
        startDate
            .truncatedTo(ChronoUnit.DAYS)
            .plusYears(n)
}
