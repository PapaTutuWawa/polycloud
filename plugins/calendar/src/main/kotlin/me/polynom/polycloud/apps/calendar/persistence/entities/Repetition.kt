package me.polynom.polycloud.apps.calendar.persistence.entities

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.validation.constraints.NotNull
import me.polynom.polycloud.apps.calendar.persistence.types.RepetitionUserType
import org.hibernate.annotations.Type
import java.time.ZonedDateTime

/**
 * How should the repetition be handled.
 */
enum class RepeatMode {
    /** Event repeats daily. */
    DAILY,

    /** Event repeats weekly. */
    WEEKLY,

    /** Event repeats monthly. */
    MONTHLY,

    /** Event repeats yearly. */
    YEARLY,
}

/**
 * Base class for extra data needed by the repetitions.
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type",
)
@JsonSubTypes(
    JsonSubTypes.Type(
        name = "repeat.config.monthly",
        value = MonthlyRepetitionConfig::class,
    ),
    JsonSubTypes.Type(
        name = "repeat.config.weekly",
        value = WeeklyRepetitionConfig::class,
    ),
)
open class RepetitionConfig { }

/**
 * Extra config for monthly event repeats.
 */
class MonthlyRepetitionConfig(
    /** Should the repetition happen every month on the same day of the month? */
    var sameDay: Boolean,
) : RepetitionConfig() {}

/**
 * Extra config for weekly event repeats.
 */
@OptIn(ExperimentalStdlibApi::class)
@Suppress("MagicNumber")
data class WeeklyRepetitionConfig(
    /** List of weekdays that this event should repeat (0 - 6). */
    var weekdays: List<Int>,
) : RepetitionConfig() {
    init {
        assert(weekdays.all { it in 1..7 })
        assert(weekdays.allDistinct())
    }
}

/**
 * A repetition of an event.
 */
@Embeddable
data class Repetition(
    /** Repeat mode. */
    @Column("repeat_mode")
    @NotNull
    @Enumerated(EnumType.STRING)
    var repeatMode: RepeatMode? = null,

    /** Repeat n times. */
    @Column("repeat_times")
    var repeatTimes: Int? = null,

    /** Repeat until this date. */
    @Column("repeat_until")
    var repeatUntil: ZonedDateTime? = null,

    /** Rough query for until when the repeat is valid (computed either from repeatUntil or repeatTimes) */
    @Column("repeat_valid_until")
    @NotNull
    var repeatTimeframeEnd: ZonedDateTime? = null,

    /** Extra config for the repetition. */
    @Type(RepetitionUserType::class)
    @Column("repeat_config")
    var repeatConfig: RepetitionConfig? = null,
)
