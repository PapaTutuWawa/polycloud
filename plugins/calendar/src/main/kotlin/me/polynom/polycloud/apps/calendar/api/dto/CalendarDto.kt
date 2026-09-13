package me.polynom.polycloud.apps.calendar.api.dto

/**
 * DTO describing the API response of an entire calendar.
 */
data class CalendarDto(
    /** The id of the calendar. */
    val id: String,
    /** The name of the calendar. */
    val name: String,
    /** The description of the calendar. */
    val description: String?,
    /** The username of the owner. */
    val owner: String,
    /** The color of the calendar (hex code). */
    val color: String,
)
