package me.polynom.polycloud.apps.calendar.api.dto

import kotlinx.serialization.Serializable
import me.polynom.polycloud.apps.calendar.serialization.UuidSerializer
import me.polynom.polycloud.apps.calendar.serialization.ZonedDateTimeSerializer
import java.time.ZonedDateTime
import java.util.UUID

/**
 * DTO for a single event.
 */
@Serializable
data class EventDto(
    /** The ID of the event. */
    val id: String,
    /** The name of the event. */
    val title: String,
    /** The description of the event. */
    val description: String? = null,
    /** The start time of the event. */
    @Serializable(with = ZonedDateTimeSerializer::class)
    val start: ZonedDateTime,
    /** The end time of the event */
    @Serializable(with = ZonedDateTimeSerializer::class)
    val end: ZonedDateTime,
    /** Flag indicating an all-day event. */
    val allDay: Boolean,
    /** Place where the event takes place. */
    val place: String? = null,
    /** The owning calendar's UUID. */
    @Serializable(with = UuidSerializer::class)
    val calendar: UUID,
    /** The color of the calendar. */
    val color: String? = null,
)
