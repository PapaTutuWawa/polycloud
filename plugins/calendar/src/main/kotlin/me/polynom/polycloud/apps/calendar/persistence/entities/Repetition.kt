package me.polynom.polycloud.apps.calendar.persistence.entities

import jakarta.persistence.Column
import jakarta.validation.constraints.NotNull
import java.time.ZonedDateTime

/**
 * How should the repetition be handled.
 */
enum class RepeatMode {
    /** Event repeats daily. */
    daily,

    /** Event repeats weekly. */
    weekly,

    /** Event repeats monthly. */
    monthly,

    /** Event repeats yearly. */
    yearly,
}

/**
 * A repetition of an event.
 */
data class Repetition(
    /** Repeat mode. */
    @Column("repeat_mode")
    @NotNull
    var repeatMode: RepeatMode? = null,

    /** Repeat after n units. */
    @Column("repeat_after")
    @NotNull
    var repeatAfter: Int? = null,

    /** Repeat n times. */
    @Column("repeat_times")
    var repeatTimes: Int? = null,

    /** Repeat until this date. */
    @Column("end_date")
    var repeatUntil: ZonedDateTime? = null,
)