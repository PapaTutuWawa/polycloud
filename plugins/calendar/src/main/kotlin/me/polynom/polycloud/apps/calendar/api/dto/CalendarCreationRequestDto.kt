package me.polynom.polycloud.apps.calendar.api.dto

/**
 * DTO describing a calendar creation request.
 */
data class CalendarCreationRequestDto(
    /** Name of the calendar. */
    val name: String,
    /** Optional description of the calendar. */
    val description: String? = null,
    /** The color of the calendar. */
    val color: String,
    /** Is the calendar public? */
    val public: Boolean,
)
