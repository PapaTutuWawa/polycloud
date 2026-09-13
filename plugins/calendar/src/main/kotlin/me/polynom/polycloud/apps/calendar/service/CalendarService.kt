package me.polynom.polycloud.apps.calendar.service

import me.polynom.polycloud.apps.calendar.api.dto.CalendarCreationRequestDto
import me.polynom.polycloud.apps.calendar.api.dto.CalendarDto
import me.polynom.polycloud.apps.calendar.autoconfigure.PluginEnabled
import me.polynom.polycloud.apps.calendar.mappers.CalendarMapper
import me.polynom.polycloud.apps.calendar.persistence.entities.Calendar
import me.polynom.polycloud.apps.calendar.persistence.repository.CalendarRepository
import me.polynom.polycloud.plugin.auth.UserContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * Service holding the API logic.
 */
@Service
@PluginEnabled
class CalendarService(
    /** The calendar CRUD repository. */
    private val repo: CalendarRepository,
    /** The user context. */
    private val userContext: UserContext,
    /** The mapper for calendar entities. */
    private val calendarMapper: CalendarMapper,
) {
    /** Logging. */
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    /**
     * Lists all calendars that the user is the owner of.
     *
     * @return List of {@link CalendarDto}
     */
    fun getCalendars(): List<CalendarDto> {
        // TODO: Better exception.
        val user = userContext.getUser() ?: throw IllegalArgumentException("User doesn't exist")
        val calendars = repo.findByOwner(user.username)
        return calendars.map(calendarMapper::calendarToCalendarDto)
    }

    /**
     * Creates a calendar.
     *
     * @param creationRequest   The data of the calendar to be created.
     * @return The created {@link CalendarDto}.
     */
    fun createCalendar(creationRequest: CalendarCreationRequestDto): CalendarDto {
        // TODO: Better exception.
        val user = userContext.getUser() ?: throw IllegalArgumentException("User doesn't exist")
        val entity = Calendar(
            name = creationRequest.name,
            description = creationRequest.description,
            owner = user.username,
            color = creationRequest.color,
            public = creationRequest.public,
        )
        repo.save(entity)
        return calendarMapper.calendarToCalendarDto(entity)
    }

    /**
     * Gets a specific calendar by its ID. Returns the calendar with a 200 if
     * - the user is authenticated and the calendar is owned by the user.
     * - the calendar is public.
     *
     * 403s are used when
     * - the user is authenticated and the calendar is not public and not owned by the user.
     * - the user is unauthenticated and the calendar does not exist or is not public.
     *
     * 404s are used when
     * - the calendar does not exist.
     * - the calendar exists, is not public, and is not owned by the requesting user.
     *
     * @param id    The ID of the calendar.
     * @return A response entity that wraps the payload.
     */
    fun getCalendarById(id: UUID): ResponseEntity<CalendarDto> {
        val calendar = repo.findByIdOrNull(id)
        logger.debug("Found calendar [{}] with public status [{}]", id, calendar?.public)
        val user = userContext.getUser()
        if (calendar == null) {
            return if (user == null) {
                ResponseEntity.status(403).build()
            } else {
                ResponseEntity.notFound().build()
            }
        }

        if (user != null && !calendar.public!!) {
            // TODO: Sharing calendars?
            return if (calendar.owner == user.username) {
                ResponseEntity.ok(calendarMapper.calendarToCalendarDto(calendar))
            } else {
                ResponseEntity.notFound().build()
            }
        }

        // Unauthenticated and calendar does exist
        if (!calendar.public!!) {
            logger.debug("Returning 403 for non-public calendar")
            return ResponseEntity.status(403).build()
        }
        return ResponseEntity.ok(calendarMapper.calendarToCalendarDto(calendar))
    }
}