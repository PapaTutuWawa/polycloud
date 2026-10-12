package me.polynom.polycloud.apps.calendar.persistence.entities

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.jacksonObjectMapper

class RepetitionTests {
    @Test
    fun testPolymorphicSerializationWeekly() {
        val mapper = ObjectMapper()
        val entity = WeeklyRepetitionConfig(listOf(1, 2, 3))
        val result = mapper.writeValueAsString(entity)

        assertThat(result).isEqualTo("{\"type\":\"repeat.config.weekly\",\"weekdays\":[1,2,3]}")
    }

    @Test
    fun testPolymorphicSerializationMonthly() {
        val mapper = ObjectMapper()
        val entity = MonthlyRepetitionConfig(true)
        val result = mapper.writeValueAsString(entity)

        assertThat(result).isEqualTo("{\"type\":\"repeat.config.monthly\",\"sameDay\":true}")
    }

    @Test
    fun testPolymorphicDeserializationWeekly() {
        val mapper = jacksonObjectMapper()
        val result = mapper.readValue(
            "{\"type\":\"repeat.config.weekly\",\"weekdays\":[1,2,3]}",
            RepetitionConfig::class.java,
        )

        assertThat(result).isNotNull()
        assertThat(result).isInstanceOf(WeeklyRepetitionConfig::class.java)
        assertThat((result as WeeklyRepetitionConfig).weekdays).containsExactly(1, 2, 3)
    }

    @Test
    fun testPolymorphicDeserializationMonthly() {
        val mapper = jacksonObjectMapper()
        val result = mapper.readValue(
            "{\"type\":\"repeat.config.monthly\",\"sameDay\":true}",
            RepetitionConfig::class.java,
        )

        assertThat(result).isNotNull()
        assertThat(result).isInstanceOf(MonthlyRepetitionConfig::class.java)
        assertThat((result as MonthlyRepetitionConfig).sameDay).isTrue()
    }
}