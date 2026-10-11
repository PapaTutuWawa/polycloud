package me.polynom.polycloud.apps.calendar.service

import me.polynom.polycloud.apps.calendar.api.dto.EventDto
import me.polynom.polycloud.apps.calendar.api.dto.RecurringInfoDto
import me.polynom.polycloud.apps.calendar.api.dto.VirtualEventType
import me.polynom.polycloud.apps.calendar.autoconfigure.PluginEnabled
import me.polynom.polycloud.apps.calendar.dates.DailyRecurrence
import me.polynom.polycloud.apps.calendar.dates.MonthlyRecurrence
import me.polynom.polycloud.apps.calendar.dates.WeeklyRecurrence
import me.polynom.polycloud.apps.calendar.dates.YearlyRecurrence
import me.polynom.polycloud.apps.calendar.mappers.EventMapper
import me.polynom.polycloud.apps.calendar.persistence.entities.Event
import me.polynom.polycloud.apps.calendar.persistence.entities.MonthlyRepetitionConfig
import me.polynom.polycloud.apps.calendar.persistence.entities.RepeatMode
import me.polynom.polycloud.apps.calendar.persistence.entities.WeeklyRepetitionConfig
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.DayOfWeek
import java.time.ZonedDateTime
import java.util.UUID

/**
 * Service that processes virtual events.
 */
@PluginEnabled
@Service
class VirtualEventService(
    private val eventMapper: EventMapper,
) {
    /** Logging. */
    private val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    fun processEvent(
        event: Event,
        viewStart: ZonedDateTime,
        viewEnd: ZonedDateTime,
        overrides: Map<UUID, List<Event>>,
    ): List<EventDto> {
        val recurrence = when (event.repeat!!.repeatMode!!) {
            RepeatMode.YEARLY -> YearlyRecurrence(
                event.timeframe!!.lower(),
                viewStart,
                viewEnd,
                event.repeat!!.repeatUntil,
                event.repeat!!.repeatTimes,
            )

            RepeatMode.MONTHLY -> MonthlyRecurrence(
                event.timeframe!!.lower(),
                viewStart,
                viewEnd,
                event.repeat!!.repeatUntil,
                event.repeat!!.repeatTimes,
                if ((event.repeat!!.repeatConfig as MonthlyRepetitionConfig?)?.sameDay
                        ?: true
                ) MonthlyRecurrence.MonthlyRecurrenceMode.DAY_OF_THE_MONTH else MonthlyRecurrence.MonthlyRecurrenceMode.NTH_DAY_OF_THE_MONTH,
            )

            RepeatMode.WEEKLY -> WeeklyRecurrence(
                event.timeframe!!.lower(),
                viewStart,
                viewEnd,
                event.repeat!!.repeatUntil,
                event.repeat!!.repeatTimes,
                ((event.repeat!!.repeatConfig as WeeklyRepetitionConfig?)?.weekdays
                    ?: listOf(event.timeframe!!.lower().dayOfWeek.value))
                    .map { DayOfWeek.of(it) }
            )

            RepeatMode.DAILY -> DailyRecurrence(
                event.timeframe!!.lower(),
                viewStart,
                viewEnd,
                event.repeat!!.repeatUntil,
                event.repeat!!.repeatTimes,
            )
        }

        logger.debug(
            "Using parameters for recurrence: [{}] [{}] [{}] [{}] [{}]",
            event.timeframe!!.lower(),
            viewStart,
            viewEnd,
            event.repeat!!.repeatUntil,
            event.repeat!!.repeatTimes
        )

        val events = mutableListOf<EventDto>()
        var count = 0
        val hasOverride = overrides.containsKey(event.id!!)
        while (recurrence.hasNext()) {
            val (idx, recurringEvent) = recurrence.next()
            val start = recurringEvent
                .withHour(event.timeframe!!.lower().hour)
                .withMinute(event.timeframe!!.lower().minute)
            val end = recurringEvent
                .withHour(event.timeframe!!.upper().hour)
                .withMinute(event.timeframe!!.upper().minute)

            // Check if there is an override.
            if (hasOverride) {
                val override = overrides[event.id]?.firstOrNull { it.parent?.parentCounter == idx }
                if (override != null) {
                    events.addLast(
                        EventDto(
                            "${event.id}-${idx}",
                            override.title!!,
                            override.description,
                            override.timeframe!!.lower(),
                            override.timeframe!!.upper(),
                            override.allDay!!,
                            override.place,
                            override.calendar!!,
                            override.color,
                            true,
                            event.id.toString(),
                            idx,
                            VirtualEventType.RECURRING,
                            eventMapper.repetitionToRecurringInfoDto(event.repeat!!, event.timeframe!!.lower()),
                        ),
                    )
                    continue
                }
            }

            events.addLast(
                EventDto(
                    "${event.id}-${idx}",
                    event.title!!,
                    event.description,
                    start,
                    end,
                    event.allDay!!,
                    event.place,
                    event.calendar!!,
                    event.color,
                    true,
                    event.id.toString(),
                    idx,
                    VirtualEventType.RECURRING,
                    eventMapper.repetitionToRecurringInfoDto(event.repeat!!, event.timeframe!!.lower()),
                ),
            )
            count++
        }
        logger.debug("Generated [{}] events", count)
        return events
    }
}