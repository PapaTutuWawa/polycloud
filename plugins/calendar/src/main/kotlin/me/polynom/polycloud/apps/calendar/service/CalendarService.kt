package me.polynom.polycloud.apps.calendar.service

import me.polynom.polycloud.apps.calendar.api.dto.CalendarCreationRequestDto
import me.polynom.polycloud.apps.calendar.api.dto.CalendarDto
import me.polynom.polycloud.apps.calendar.api.dto.EventCreationRequestDto
import me.polynom.polycloud.apps.calendar.api.dto.EventDto
import me.polynom.polycloud.apps.calendar.autoconfigure.PluginEnabled
import me.polynom.polycloud.apps.calendar.mappers.CalendarMapper
import me.polynom.polycloud.apps.calendar.mappers.EventMapper
import me.polynom.polycloud.apps.calendar.persistence.entities.Calendar
import me.polynom.polycloud.apps.calendar.persistence.entities.Event
import me.polynom.polycloud.apps.calendar.persistence.repository.CalendarRepository
import me.polynom.polycloud.apps.calendar.persistence.repository.EventRepository
import me.polynom.polycloud.plugin.auth.UserContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.TransactionCallback
import org.springframework.transaction.support.TransactionTemplate
import java.util.*

/**
 * Service holding the API logic.
 */
@Service
@PluginEnabled
class CalendarService(
    /** The calendar CRUD repository. */
    private val calendarRepository: CalendarRepository,
    /** The event CRUD repository. */
    private val eventRepository: EventRepository,
    /** The user context. */
    private val userContext: UserContext,
    /** The mapper for calendar entities. */
    private val calendarMapper: CalendarMapper,
    /** The mapper for event entities. */
    private val eventMapper: EventMapper,
    /** Transaction management. */
    private val transactionTemplate: TransactionTemplate,
) {
    /** Logging. */
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    /**
     * Lists all calendars that the user is the owner of.
     *
     * @return List of {@link CalendarDto}
     */
    fun getCalendars(): List<CalendarDto> {
        val user = userContext.getUser()!!
        val calendars = calendarRepository.findByOwner(user.username)
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
        calendarRepository.save(entity)
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
        val calendar = getCalendarByIdWithAccessCheck(id)
        if (calendar.first == null) {
            return ResponseEntity.status(calendar.second).build()
        }

        return ResponseEntity.ok(
            calendarMapper.calendarToCalendarDto(calendar.first!!)
        )
    }

    /**
     * Create a single event in a calendar.
     *
     * @param calendarId            The ID of the calendar this event belongs to.
     * @param eventCreationRequest  The request by the client.
     * @return A {@link EventDto}
     */
    fun createEvent(calendarId: UUID, eventCreationRequest: EventCreationRequestDto): ResponseEntity<EventDto> {
        // Deny the request if there is anything wrong with the user's request.
        val calendar = getCalendarByIdWithAccessCheck(calendarId)
        if (calendar.first == null) {
            return ResponseEntity.status(calendar.second).build()
        }

        val entity = Event(
            calendar = calendarId,
            title = eventCreationRequest.title,
            description = eventCreationRequest.description,
            datetime = eventCreationRequest.datetime,
        )
        eventRepository.save(entity)
        return ResponseEntity.ok(eventMapper.eventToEventDto(entity))
    }

    /**
     * Gets all events associated with a calendar.
     *
     * @param calendarId    The ID of the calendar.
     * @return A {@link ResponseEntity} that may or may not contain the event list.
     */
    fun getEvents(calendarId: UUID): ResponseEntity<List<EventDto>> {
        val calendar = getCalendarByIdWithAccessCheck(calendarId)
        if (calendar.first == null) {
            return ResponseEntity.status(calendar.second).build()
        }

        return ResponseEntity.ok(
            eventRepository
                .findAllByCalendar(calendarId)
                .map(eventMapper::eventToEventDto)
        )
    }

    /**
     * Deletes a calendar.
     *
     * @param calendarId    The ID of the calendar.
     * @return A {@link ResponseEntity} that only sets the status code.
     */
    fun deleteCalendar(calendarId: UUID): ResponseEntity<Void> {
        // This endpoint must be authenticated.
        if (userContext.getUser() == null) {
            return ResponseEntity(HttpStatus.FORBIDDEN)
        }

        // Get the calendar.
        val calendar = getCalendarByIdWithAccessCheck(calendarId)
        if (calendar.first == null) {
            return ResponseEntity.status(calendar.second).build()
        }

        // Only the owner can delete the calendar.
        if (calendar.first!!.owner != userContext.getUser()!!.username) {
            return ResponseEntity(HttpStatus.FORBIDDEN)
        }

        calendarRepository.deleteById(calendarId)
        return ResponseEntity.ok().build()
    }

    /**
     * Deletes a single event.
     *
     * @param calendarId    The ID of the calendar.
     * @param eventId       The ID of the event.
     * @return A {@link ResponseEntity} that only sets the status code.
     */
    fun deleteEvent(calendarId: UUID, eventId: UUID): ResponseEntity<Void> {
        // TODO: No idea why we need a transaction here but not in the other functions
        return transactionTemplate.execute<ResponseEntity<Void>> ret@{
            val calendar = getCalendarByIdWithAccessCheck(calendarId)
            if (calendar.first == null) {
                return@ret ResponseEntity.status(calendar.second).build()
            }

            eventRepository.deleteByCalendarAndId(calendarId, eventId)
            ResponseEntity.ok().build()
        }

    }

    /**
     * Safe access method to fetch a calendar. Safe in that it checks access permissions of the requesting user.
     *
     * @param calendarId    The ID of the calendar to request.
     * @return A pair of the calendar entity (or null if we cannot find it or the user cannot see it) and the
     *         suggested HTTP status code.
     */
    fun getCalendarByIdWithAccessCheck(calendarId: UUID): Pair<Calendar?, Int> {
        logger.debug("Querying calendar with id {}", calendarId)
        val calendar = calendarRepository.findByIdOrNull(calendarId)
        logger.debug("Found calendar [{}] with public status [{}]", calendarId, calendar?.public)
        val user = userContext.getUser()
        if (calendar == null) {
            return if (user == null) {
                Pair(null, 403)
            } else {
                Pair(null, 404)
            }
        }

        if (user != null && !calendar.public!!) {
            // TODO: Sharing calendars?
            return if (calendar.owner == user.username) {
                Pair(calendar, 200)
            } else {
                Pair(null, 404)
            }
        }

        // Unauthenticated and calendar does exist
        if (!calendar.public!!) {
            logger.debug("Returning 403 for non-public calendar")
            return Pair(null, 403)
        }
        return Pair(calendar, 200)
    }
}