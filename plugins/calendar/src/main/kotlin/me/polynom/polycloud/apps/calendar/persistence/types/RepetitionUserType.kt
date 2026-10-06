package me.polynom.polycloud.apps.calendar.persistence.types

import com.fasterxml.jackson.core.JsonProcessingException
import me.polynom.polycloud.apps.calendar.persistence.entities.RepetitionConfig
import org.hibernate.type.SqlTypes
import org.hibernate.type.descriptor.WrapperOptions
import org.hibernate.usertype.UserType
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue
import java.sql.PreparedStatement
import java.sql.ResultSet

/**
 * User type for the repetition JSON config.
 */
class RepetitionUserType : UserType<RepetitionConfig> {
    companion object {
        private val objectMapper = jacksonObjectMapper()
    }

    override fun getSqlType(): Int = SqlTypes.OTHER

    override fun returnedClass(): Class<RepetitionConfig> = RepetitionConfig::class.java

    override fun deepCopy(value: RepetitionConfig?): RepetitionConfig? = value

    override fun isMutable(): Boolean = false

    override fun nullSafeGet(rs: ResultSet, position: Int, options: WrapperOptions): RepetitionConfig? {
        val value = rs.getString(position) ?: return null

        try {
            val config = objectMapper.readValue<RepetitionConfig>(value)
            return config
        } catch (e: JsonProcessingException) {
            return null
        }
    }

    override fun nullSafeSet(
        st: PreparedStatement,
        value: RepetitionConfig?,
        position: Int,
        options: WrapperOptions
    ) {
        if (value == null) {
            st.setNull(position, SqlTypes.OTHER)
        } else {
            st.setString(position, objectMapper.writeValueAsString(value))
        }
    }
}