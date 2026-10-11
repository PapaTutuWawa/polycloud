package me.polynom.polycloud.apps.webdav.persistence.repository

import me.polynom.polycloud.apps.webdav.persistence.entities.AppPasswordEntity
import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface AppPasswordRepository : CrudRepository<AppPasswordEntity, String> {
    fun findAllByUsername(username: String): List<AppPasswordEntity>

    fun findByAppPasswordIdAndUsername(
        appPasswordId: UUID,
        username: String,
    ): AppPasswordEntity?
}
