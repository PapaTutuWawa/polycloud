package me.polynom.polycloud.apps.calendar.mappers

import me.polynom.polycloud.apps.calendar.api.dto.EventDto
import me.polynom.polycloud.apps.calendar.persistence.entities.Event
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingConstants

/**
 * Mapper for events.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
interface EventMapper {
    @Mapping(target = "start", expression = "java(event.getTimeframe().lower())")
    @Mapping(target = "end", expression = "java(event.getTimeframe().upper())")
    fun eventToEventDto(event: Event): EventDto
}
