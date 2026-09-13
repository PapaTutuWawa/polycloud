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
    /** The start time of the event. */
    val start: ZonedDateTime,
    /** The end time of the event */
    val end: ZonedDateTime,
    /** Place where the event takes place. */
    val place: String?,
)
