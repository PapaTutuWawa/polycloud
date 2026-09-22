package me.polynom.polycloud.apps.calendar.api.dto

import kotlinx.serialization.Serializable

/**
 * DTO for requests listing events in multiple calendars at once.
 */
@Serializable
data class CalendarEventListingRequestDto(
    /** Calendars to list from. */
    // TODO: Deserialize this to UUID
    val calendars: List<String>,
)
