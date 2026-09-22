package me.polynom.polycloud.apps.calendar.api.dto

import kotlinx.serialization.Serializable
import me.polynom.polycloud.apps.calendar.serialization.ZonedDateTimeSerializer
import java.time.ZonedDateTime

/**
 * DTO for creating a single event.
 */
@Serializable
data class EventCreationRequestDto(
    /** The name of the event. */
    val title: String,
    /** The description of the event. */
    val description: String? = null,
    /** The start time of the event. */
    @Serializable(with = ZonedDateTimeSerializer::class)
    val start: ZonedDateTime,
    /** The end time of the event. */
    @Serializable(with = ZonedDateTimeSerializer::class)
    val end: ZonedDateTime,
    /** Flag indicating an all-day event. */
    val allDay: Boolean,
    /** Place where the event takes place. */
    val place: String? = null,
)
