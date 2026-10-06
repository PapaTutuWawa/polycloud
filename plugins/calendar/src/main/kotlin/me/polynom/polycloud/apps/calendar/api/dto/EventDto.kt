package me.polynom.polycloud.apps.calendar.api.dto

import java.time.ZonedDateTime
import java.util.UUID

/**
 * DTO for a single event.
 */
data class EventDto(
    /** The ID of the event. */
    val id: String,

    /** The name of the event. */
    val title: String,

    /** The description of the event. */
    val description: String? = null,

    /** The start time of the event. */
    val start: ZonedDateTime,

    /** The end time of the event */
    val end: ZonedDateTime,

    /** Flag indicating an all-day event. */
    val allDay: Boolean,

    /** Place where the event takes place. */
    val place: String? = null,

    /** The owning calendar's UUID. */
    val calendar: UUID,

    /** The color of the calendar. */
    val color: String? = null,

    /** Is the event a virtual event, i.e. it is not materialized in the database. */
    val virtual: Boolean = false,

    /** The ID of the event that this event belongs to, if it is a virtual ID. */
    val virtualParentId: String? = null,

    /** The index of the recurring event that identifies it in the sequence. */
    val virtualId: Long? = null,

    /** The type of virtual event this is. */
    val virtualEventType: VirtualEventType? = null,
)
