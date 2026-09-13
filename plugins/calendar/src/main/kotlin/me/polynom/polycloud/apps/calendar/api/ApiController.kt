package me.polynom.polycloud.apps.calendar.api

import io.swagger.v3.oas.annotations.Operation
import me.polynom.polycloud.apps.calendar.api.dto.CalendarDto
import me.polynom.polycloud.apps.calendar.autoconfigure.PluginEnabled
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Rest controller for the main API.
 */
@RestController
@PluginEnabled
@RequestMapping("/api/apps/calendar")
class ApiController {
    @Operation(summary = "Returns a list of all calendars that this user owns.")
    @GetMapping("/calendars", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getCalendars(): List<CalendarDto> = emptyList()
}