package me.polynom.polycloud.apps.calendar.mappers

import me.polynom.polycloud.apps.calendar.api.dto.EventDto
import me.polynom.polycloud.apps.calendar.api.dto.RecurringInfoDto
import me.polynom.polycloud.apps.calendar.persistence.entities.Calendar
import me.polynom.polycloud.apps.calendar.persistence.entities.Event
import me.polynom.polycloud.apps.calendar.persistence.entities.MonthlyRepetitionConfig
import me.polynom.polycloud.apps.calendar.persistence.entities.RepeatMode
import me.polynom.polycloud.apps.calendar.persistence.entities.Repetition
import me.polynom.polycloud.apps.calendar.persistence.entities.RepetitionConfig
import me.polynom.polycloud.apps.calendar.persistence.entities.WeeklyRepetitionConfig
import org.mapstruct.EnumMapping
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingConstants
import java.time.ZonedDateTime

/**
 * Mapper for events.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
interface EventMapper {
    @Mapping(target = "start", expression = "java(event.getTimeframe().lower())")
    @Mapping(target = "end", expression = "java(event.getTimeframe().upper())")
    fun eventToEventDto(event: Event): EventDto

    @EnumMapping(nameTransformationStrategy = MappingConstants.CASE_TRANSFORMATION, configuration = "upper")
    fun repeatModeToRecurringFrequency(mode: RepeatMode): RecurringInfoDto.RecurringFrequency

    fun repetitionConfigToRecurringInfoRepetitionConfig(repeat: RepetitionConfig?, repeatStart: ZonedDateTime): RecurringInfoDto.RepetitionConfigDto? {
        if (repeat == null) return null

        if (repeat is WeeklyRepetitionConfig) {
            return RecurringInfoDto.RepetitionConfigDto(
                sameDay = null,
                weekdays = repeat.weekdays,
                repeatStarts = repeatStart,
            )
        } else if (repeat is MonthlyRepetitionConfig) {
            return RecurringInfoDto.RepetitionConfigDto(
                sameDay = repeat.sameDay,
                weekdays = null,
                repeatStarts = repeatStart,
            )
        }

        throw IllegalArgumentException()
    }

    fun repetitionToRecurringInfoDto(repetition: Repetition, repeatStart: ZonedDateTime): RecurringInfoDto {
        return RecurringInfoDto(
            this.repeatModeToRecurringFrequency(repetition.repeatMode!!),
            this.repetitionConfigToRecurringInfoRepetitionConfig(repetition.repeatConfig, repeatStart),
        )
    }
}
