package me.polynom.polycloud.apps.calendar.persistence.repository

import io.hypersistence.utils.hibernate.type.range.Range
import me.polynom.polycloud.apps.calendar.persistence.entities.Event
import org.springframework.data.jpa.repository.NativeQuery
import org.springframework.data.repository.CrudRepository
import java.time.ZonedDateTime
import java.util.UUID

/**
 * Repository for event entities.
 */
interface EventRepository : CrudRepository<Event, UUID> {
    /**
     * Finds all events inside a calendar.
     *
     * @param calendar  The UUID of the calendar.
     */
    fun findAllByCalendar(calendar: UUID): List<Event>

    /**
     * Deletes an event based on the event ID and the calendar.
     *
     * @param calendar  The calendar's UUID.
     * @param id        The event's UUID.
     */
    fun deleteByCalendarAndId(
        calendar: UUID,
        id: UUID,
    )

    /**
     * Finds all events that somehow overlap with the specified timeframe.
     *
     * @param calendarIds   The UUIDs of the calendars.
     * @param timeframe     The timeframe that has to overlap the event's duration.
     */
    @NativeQuery(
        """
        SELECT
            event.*,
            calendar.color
        FROM
            event
        JOIN calendar ON event.calendar = calendar.id
        WHERE
            event.calendar IN ?1 AND
            event.timeframe && ?2
        """,
    )
    fun findAllByCalenderIdsAndTimeframeOverlapWithTimeframe(
        calendarIds: List<UUID>,
        timeframe: Range<ZonedDateTime>,
    ): List<Event>
}
