package me.polynom.polycloud.apps.calendar.api.dto

import java.time.ZonedDateTime

/**
 * Additional information about repeats.
 */
data class RecurringInfoDto(
    /** The frequency at which the event repeats. */
    val frequency: RecurringFrequency,

    /** Configuration for the repeat. */
    val config: RepetitionConfigDto? = null,
) {
    /**
     * Repeat configuration.
     */
    data class RepetitionConfigDto(
        /** For MONTHLY only: Is the repeat always on a fixed date within the month? */
        val sameDay: Boolean? = null,

        /** For WEEKLY only: On which weekdays is the repeat happening. */
        val weekdays: List<Int>? = null,

        /** When does the repeat start? */
        val repeatStarts: ZonedDateTime,
    ) {
        init {
            assert((sameDay != null).xor(weekdays != null))
        }
    }

    /**
     * Recurrence frequency.
     */
    enum class RecurringFrequency {
        /** Event repeats daily. */
        DAILY,

        /** Event repeats weekly. */
        WEEKLY,

        /** Event repeats monthly. */
        MONTHLY,

        /** Event repeats yearly. */
        YEARLY,
    }
}