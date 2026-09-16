package me.polynom.polycloud.apps.calendar.mappers

import me.polynom.polycloud.apps.calendar.api.dto.CalendarDto
import me.polynom.polycloud.apps.calendar.persistence.entities.Calendar
import org.mapstruct.Mapper
import org.mapstruct.MapperConfig
import org.mapstruct.Mapping
import org.mapstruct.MappingConstants
import org.mapstruct.ReportingPolicy

/**
 * Mapper for converting a calendar entity to its DTO.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
@MapperConfig(unmappedSourcePolicy = ReportingPolicy.WARN)
interface CalendarMapper {
    @Mapping(source = "public", target = "isPublic")
    fun calendarToCalendarDto(calendar: Calendar): CalendarDto
}
