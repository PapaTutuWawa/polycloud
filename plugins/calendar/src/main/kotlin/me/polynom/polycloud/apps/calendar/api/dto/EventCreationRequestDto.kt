package me.polynom.polycloud.apps.calendar.api.dto

import java.time.ZonedDateTime

/**
 * DTO for creating a single event.
 */
data class EventCreationRequestDto(
    /** The name of the event. */
    val title: String,
    /** The description of the event. */
    val description: String? = null,
    /** The start time of the event. */
    val start: ZonedDateTime,
    /** The end time of the event. */
    val end: ZonedDateTime,
    /** Flag indicating an all-day event. */
    val allDay: Boolean,
    /** Place where the event takes place. */
    val place: String? = null,
)
