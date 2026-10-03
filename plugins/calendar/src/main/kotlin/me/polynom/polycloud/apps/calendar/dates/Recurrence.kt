package me.polynom.polycloud.apps.calendar.dates

import java.lang.Math.clamp
import java.time.DayOfWeek
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/**
 * Base class for recurrence rules.
 */
abstract class BaseRecurrence(
    /** The date to start on. This is implicitly occurrence 0. */
    protected val startDate: ZonedDateTime,
    /** The start of the view. */
    protected val viewStartDate: ZonedDateTime,
    /** The end of the view. */
    protected val viewEndDate: ZonedDateTime,
    /** The end date of the event. */
    protected val endDate: ZonedDateTime?,
    /** The number of occurrences this event should have. */
    protected val occurrences: Int?,
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
    private val weekdays: List<DayOfWeek>,
) : BaseRecurrence(startDate, viewStartDate, viewEndDate, endDate, occurrences) {
    /** Last event we have processed. */
    private lateinit var lastOccurrence: ZonedDateTime

    init {
        assert((endDate != null).xor(occurrences != null))
        assert(weekdays.isNotEmpty())

        if (viewStartDate.isAfter(startDate)) {
            val weekStart = startDate.weekStart()
            val viewWeekStart = viewStartDate.weekStart()
            if (weekStart.isEqual(viewWeekStart)) {
                counter = 0
            } else {
                val weeksBetween =
                    ChronoUnit.WEEKS.between(
                        weekStart,
                        viewWeekStart,
                    )

                if (weeksBetween == 0L) {
                    // View starts in the same week as the event but after it.
                } else {
                    // View starts in another week after the event. So set the last occurrence to the last event
                    // in the previous week.
                    val maxWeekDay = weekdays.maxBy { it.value }
                    lastOccurrence = viewWeekStart.minusWeeks(1).plusDays(maxWeekDay.value - 1L)
                    counter = weeksBetween * weekdays.size
                }
            }
        } else {
            counter = -1
            lastOccurrence = startDate
        }
    }

    override fun nextOccurrence(n: Long): ZonedDateTime {
        if (n == 0L) {
            return startDate
        } else if (n == 1L) {
            return startDate.toNextWeekday(
                weekdays.minBy { it.value },
            )
        }

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

        val nextWeekDay =
            weekdays
                .filter {
                    it.value > lastOccurrence.dayOfWeek.value
                }.minByOrNull { it.value } ?: weekdays.minBy { it.value }
        return lastOccurrence.toNextWeekday(nextWeekDay)
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
class MonthlyRecurrence(
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
    /** The repeat mode. */
    private val recurrenceMode: MonthlyRecurrenceMode,
) : BaseRecurrence(startDate, viewStartDate, viewEndDate, endDate, occurrences) {
    /** Last event we have processed. */
    private var lastOccurrence: ZonedDateTime

    init {
        assert((endDate != null).xor(occurrences != null))

        val months =
            clamp(
                ChronoUnit.MONTHS.between(
                    startDate.truncatedTo(ChronoUnit.DAYS),
                    viewStartDate.truncatedTo(ChronoUnit.DAYS),
                ),
                0,
                Long.MAX_VALUE,
            )
        when (recurrenceMode) {
            MonthlyRecurrenceMode.DAY_OF_THE_MONTH -> {
                if (viewStartDate.isAfter(startDate)) {
                    counter = months
                    lastOccurrence = startDate.plusDays(months)
                } else {
                    counter = 0
                    lastOccurrence = startDate.minusMonths(1)
                }
            }

            MonthlyRecurrenceMode.NTH_DAY_OF_THE_MONTH -> {
                if (viewStartDate.isAfter(startDate)) {
                    counter = months
                    lastOccurrence = viewStartDate.minusMonths(1)
                } else {
                    lastOccurrence = startDate.minusMonths(1)
                    counter = 0
                }
            }
        }
    }

    private fun buildNthWeekdayOfMonth(
        month: ZonedDateTime,
        nthDay: Int,
    ): ZonedDateTime {
        val monthStart = month.withDayOfMonth(1)
        if (nthDay == 1 && monthStart.dayOfWeek == startDate.dayOfWeek) {
            return monthStart
        }
        val day = monthStart.toNextWeekday(startDate.dayOfWeek)
        // If we wrap and the first day of the month is already the day we are looking for, we only have
        // to move forward nth-2, because we (due to toNextWeekday) already moved forward one.
        val subtract = if (monthStart.dayOfWeek == startDate.dayOfWeek) 2 else 1
        return day.plusDays((nthDay - subtract).times(7L))
    }

    override fun nextOccurrence(n: Long): ZonedDateTime {
        when (recurrenceMode) {
            MonthlyRecurrenceMode.NTH_DAY_OF_THE_MONTH -> {
                if (n == 1L) {
                    return startDate
                }
                val nextMonth = lastOccurrence.plusMonths(1)
                return buildNthWeekdayOfMonth(
                    nextMonth,
                    startDate.nthWeekday(),
                )
            }

            MonthlyRecurrenceMode.DAY_OF_THE_MONTH -> {
                return lastOccurrence
                    .plusMonths(1)
                    .withDayOfMonth(startDate.dayOfMonth)
            }
        }
    }

    override fun next(): Pair<Long, ZonedDateTime> {
        val value = super.next()
        lastOccurrence = value.second
        return value
    }

    enum class MonthlyRecurrenceMode {
        /** Repeat every n-th weekday in the month. */
        NTH_DAY_OF_THE_MONTH,

        /** Repeat on the say day of the month. */
        DAY_OF_THE_MONTH,
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
