package me.polynom.polycloud.apps.calendar.persistence.entities

import io.hypersistence.utils.hibernate.type.range.PostgreSQLRangeType
import io.hypersistence.utils.hibernate.type.range.Range
import jakarta.annotation.Nullable
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SecondaryTable
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import org.hibernate.annotations.Type
import java.time.ZonedDateTime
import java.util.UUID

@Embeddable
class EventParent {
    /** The recurring event this one belongs to. */
    @Column(name = "parent_id")
    var parentId: UUID? = null

    /** The counter in the sequence that this event replaces. */
    @Column(name = "parent_counter")
    var parentCounter: Long? = null
}

/**
 * Database model for a single event in a calendar.
 *
 * Modeling information: The way an event is supposed to be represented is that
 * the timeframe itself describes the occurrence of the event itself, i.e. if an
 * appointment starts at 11:30 and ends at 12:00, then that's the timeframe. If that
 * appointment repeats daily, then the timeframe is still the same but the information
 * about the repetition is in the repeat config.
 *
 * If an event has a parent, then it is supposed to replace a virtual event in the recurring
 * sequence of the specified parent.
 */
@Entity
@Table(name = "event")
@SecondaryTable(name = "calendar")
class Event(
    /** The ID of the event. */
    @Id
    @GeneratedValue(GenerationType.UUID)
    var id: UUID? = null,

    /** The calendar this event belongs to. */
    var calendar: UUID? = null,

    /** Title of the event. */
    @NotNull
    var title: String? = null,

    /** The description of the event. */
    @Nullable
    var description: String? = null,

    /** Timerange of the event. */
    @NotNull
    @Column(name = "timeframe")
    @Type(PostgreSQLRangeType::class)
    var timeframe: Range<ZonedDateTime>? = null,

    /** Is the event an all-day event? */
    @NotNull
    @Column(name = "all_day")
    var allDay: Boolean? = null,

    /** The place where the event occurs. */
    @Nullable
    var place: String? = null,

    /** Parent event, if set. Only applicable for repeats. */
    @Nullable
    @Column(name = "repeat_parent")
    @Embedded
    var parent: EventParent? = null,

    /**
     * The color of the parent calendar. Defined by the calendar entity itself
     * and the result of a join, so transient.
     */
    @Column(name = "color", table = "calendar")
    var color: String? = null,

    /** Repetition config. */
    @Column(name = "repeat")
    @Embedded
    var repeat: Repetition? = null,
)
