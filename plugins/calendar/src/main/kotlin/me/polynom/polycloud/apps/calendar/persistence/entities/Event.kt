package me.polynom.polycloud.apps.calendar.persistence.entities

import jakarta.annotation.Nullable
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import java.time.ZonedDateTime
import java.util.UUID

/**
 * Database model for a single event in a calendar.
 */
@Entity
@Table(name = "event")
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

    /** Time of the event. */
    var datetime: ZonedDateTime? = null,
)