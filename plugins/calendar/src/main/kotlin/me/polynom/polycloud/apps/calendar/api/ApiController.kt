package me.polynom.polycloud.apps.calendar.api

import io.swagger.v3.oas.annotations.Operation
import me.polynom.polycloud.apps.calendar.api.dto.CalendarCreationRequestDto
import me.polynom.polycloud.apps.calendar.api.dto.CalendarDto
import me.polynom.polycloud.apps.calendar.autoconfigure.PluginEnabled
import me.polynom.polycloud.apps.calendar.service.CalendarService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/**
 * Rest controller for the main API.
 */
@RestController
@PluginEnabled
@RequestMapping("/api/apps/calendar")
class ApiController(
    /** The logic implementation of the API. */
    private val service: CalendarService,
) {
    @Operation(summary = "Returns a list of all calendars that this user owns.")
    @GetMapping("/calendars", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getCalendars(): List<CalendarDto> = service.getCalendars()

    @Operation(summary = "Creates a calendar.")
    @PostMapping("/calendar")
    @ResponseStatus(HttpStatus.CREATED)
    fun postCalendar(@RequestBody calendar: CalendarCreationRequestDto): CalendarDto
        = service.createCalendar(calendar)

    @Operation(summary = "Gets a calendar by its ID.")
    @GetMapping("/calendar/{id}")
    fun getCalendarById(@PathVariable id: UUID): ResponseEntity<CalendarDto>
        = service.getCalendarById(id)
}