package me.polynom.polycloud.apps.calendar.api.dto

import java.time.ZonedDateTime

/**
 * DTO for a single event.
 */
data class EventDto(
    /** The ID of the event. */
    val id: String,
    /** The name of the event. */
    val title: String,
    /** The description of the event. */
    val description: String?,
    /** The UUID of the calendar this event belongs to. */
    val calendar: String,
    /** The time of the event. */
    val datetime: ZonedDateTime,
)
