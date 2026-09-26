package me.polynom.polycloud.apps.calendar.service

import io.hypersistence.utils.hibernate.type.range.Range
import me.polynom.polycloud.apps.calendar.api.dto.CalendarCreationRequestDto
import me.polynom.polycloud.apps.calendar.api.dto.CalendarDto
import me.polynom.polycloud.apps.calendar.api.dto.CalendarEventListingRequestDto
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
import org.springframework.transaction.support.TransactionTemplate
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID

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
        val entity =
            Calendar(
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
            calendarMapper.calendarToCalendarDto(calendar.first!!),
        )
    }

    /**
     * Create a single event in a calendar.
     *
     * @param calendarId            The ID of the calendar this event belongs to.
     * @param eventCreationRequest  The request by the client.
     * @return A {@link EventDto}
     */
    fun createEvent(
        calendarId: UUID,
        eventCreationRequest: EventCreationRequestDto,
    ): ResponseEntity<EventDto> {
        // Deny the request if there is anything wrong with the user's request.
        val calendar = getCalendarByIdWithAccessCheck(calendarId)
        if (calendar.first == null) {
            return ResponseEntity.status(calendar.second).build()
        }

        val entity =
            Event(
                calendar = calendarId,
                title = eventCreationRequest.title,
                description = eventCreationRequest.description,
                timeframe = Range.closed(eventCreationRequest.start, eventCreationRequest.end),
                allDay = eventCreationRequest.allDay,
                place = eventCreationRequest.place,
            )
        eventRepository.save(entity)
        entity.color = calendar.first!!.color
        return ResponseEntity.ok(eventMapper.eventToEventDto(entity))
    }

    /**
     * Gets all events associated with a calendar.
     *
     * @param calendarId    The ID of the calendar.
     * @return A {@link ResponseEntity} that may or may not contain the event list.
     */
    fun getEvents(
        calendarId: UUID,
        start: Long?,
        end: Long?,
        timezone: String?,
    ): ResponseEntity<List<EventDto>> {
        if (start == null && end == null && timezone == null) {
            // Query all events
            val calendar = getCalendarByIdWithAccessCheck(calendarId)
            if (calendar.first == null) {
                return ResponseEntity.status(calendar.second).build()
            }

            return ResponseEntity.ok(
                eventRepository
                    .findAllByCalendar(calendarId)
                    .map(eventMapper::eventToEventDto),
            )
        } else if (start != null && end != null && timezone != null) {
            // Query only events between start and end
            val calendar = getCalendarByIdWithAccessCheck(calendarId)
            if (calendar.first == null) {
                return ResponseEntity.status(calendar.second).build()
            }

            val zone = ZoneId.of(timezone)
            val startZdt = ZonedDateTime.ofInstant(Instant.ofEpochSecond(start), zone)
            val endZdt = ZonedDateTime.ofInstant(Instant.ofEpochSecond(end), zone)
            val range = Range.closed(startZdt, endZdt)

            return ResponseEntity.ok(
                eventRepository
                    .findAllByCalenderIdsAndTimeframeOverlapWithTimeframe(listOf(calendarId), range)
                    .map(eventMapper::eventToEventDto),
            )
        }

        return ResponseEntity.status(400).build()
    }

    /**
     * Gets all events associated with a list of calendars.
     *
     * @param request   The request by the client.
     * @param start     The start date of events to query.
     * @param end       The end date of events to query.
     * @param timezone  The timezone to use for the query.
     * @return A {@link ResponseEntity} that may or may not contain the event list.
     */
    fun getEventsForMultipleCalendars(
        request: CalendarEventListingRequestDto,
        start: Long,
        end: Long,
        timezone: String,
    ): ResponseEntity<List<EventDto>> {
        val zone = ZoneId.of(timezone)
        val events = eventRepository.findAllByCalenderIdsAndTimeframeOverlapWithTimeframe(
            request.calendars.map(UUID::fromString),
            Range.closed(
                ZonedDateTime.ofInstant(Instant.ofEpochMilli(start), zone),
                ZonedDateTime.ofInstant(Instant.ofEpochMilli(end), zone),
            )
        ).map(eventMapper::eventToEventDto)

        return ResponseEntity.ok(events)
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
    fun deleteEvent(
        calendarId: UUID,
        eventId: UUID,
    ): ResponseEntity<Void> {
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
