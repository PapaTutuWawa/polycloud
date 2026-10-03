package me.polynom.polycloud.apps.calendar.persistence.entities

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.hypersistence.utils.hibernate.type.json.JsonType
import jakarta.persistence.Column
import jakarta.validation.constraints.NotNull
import org.hibernate.annotations.Type
import java.io.Serializable
import java.time.Month
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
abstract class RepetitionConfig : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
    abstract fun getType(): String
}

/**
 * Extra config for monthly event repeats.
 */
class MonthlyRepetitionConfig(
    /** Should the repetition happen every month on the same day of the month? */
    var sameDay: Boolean,
    /**
     * If sameDay is false, this value must indicate that the n-th weekday of the month
     * the event should occur.
     */
    var weekdayOfTheMonth: Int?,
) : RepetitionConfig() {
    init {
        assert((weekdayOfTheMonth == null).xor(sameDay))
    }

    companion object {
        private const val DISCRIMIATOR = "repeat.config.monthly"
    }

    override fun getType(): String = DISCRIMIATOR
}

/**
 * Extra config for weekly event repeats.
 */
@Suppress("MagicNumber")
class WeeklyRepetitionConfig(
    /** List of weekdays that this event shoud repeat (0 - 6). */
    var weekdays: List<Int>,
) : RepetitionConfig() {
    init {
        assert(weekdays.all { it in 0..6 })
    }

    companion object {
        private const val DISCRIMIATOR = "repeat.config.weekly"
    }

    override fun getType(): String = DISCRIMIATOR
}

/**
 * A repetition of an event.
 */
data class Repetition(
    /** Repeat mode. */
    @Column("repeat_mode")
    @NotNull
    var repeatMode: RepeatMode? = null,
    /** Repeat n times. */
    @Column("repeat_times")
    var repeatTimes: Int? = null,
    /** Repeat until this date. */
    @Column("end_date")
    var repeatUntil: ZonedDateTime? = null,
    /** Datetime the event should start on the first day. */
    @Column("repeat_start")
    var repeatEventStart: ZonedDateTime? = null,
    /** Datetime the event should end on the first day. */
    @Column("repeat_end")
    var repeatEventEnd: ZonedDateTime? = null,
    /** Extra config for the repetition. */
    @Type(JsonType::class)
    @Column("repeat_config")
    var repeatConfig: RepetitionConfig? = null,
)
