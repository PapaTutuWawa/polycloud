package me.polynom.polycloud.apps.calendar.api.dto

/**
 * DTO for requests listing events in multiple calendars at once.
 */
data class CalendarEventListingRequestDto(
    /** Calendars to list from. TODO: Deserialize this to UUID */
    val calendars: List<String>,
)
