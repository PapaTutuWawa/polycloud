package me.polynom.polycloud.apps.calendar.persistence.repository

import me.polynom.polycloud.apps.calendar.api.dto.EventDto
import me.polynom.polycloud.apps.calendar.persistence.entities.Event
import org.springframework.data.repository.CrudRepository
import java.util.UUID

/**
 * Repository for event entities.
 */
interface EventRepository : CrudRepository<Event, UUID> {
    fun findAllByCalendar(calendarId: UUID): List<Event>

    fun deleteByCalendarAndId(calendar: UUID, id: UUID)
}