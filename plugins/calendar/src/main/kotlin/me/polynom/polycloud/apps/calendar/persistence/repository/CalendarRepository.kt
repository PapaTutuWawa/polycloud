package me.polynom.polycloud.apps.calendar.persistence.repository

import me.polynom.polycloud.apps.calendar.persistence.entities.Calendar
import org.springframework.data.repository.CrudRepository
import java.util.UUID

/**
 * Repository for calendar queries.
 */
interface CalendarRepository : CrudRepository<Calendar, UUID> {
    /**
     * Find calendars that are owned by a given user.
     */
    fun findByOwner(owner: String): List<Calendar>
}
