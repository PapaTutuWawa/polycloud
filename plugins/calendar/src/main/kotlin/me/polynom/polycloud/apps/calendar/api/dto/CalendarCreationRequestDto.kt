package me.polynom.polycloud.apps.calendar.api.dto

import kotlinx.serialization.Serializable

/**
 * DTO describing a calendar creation request.
 */
@Serializable
data class CalendarCreationRequestDto(
    /** Name of the calendar. */
    val name: String,
    /** Optional description of the calendar. */
    val description: String?,
    /** The color of the calendar. */
    val color: String,
    /** Is the calendar public? */
    val public: Boolean,
)