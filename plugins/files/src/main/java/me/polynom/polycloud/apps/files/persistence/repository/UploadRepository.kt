package me.polynom.polycloud.apps.files.persistence.repository

import me.polynom.polycloud.apps.files.persistence.entities.Upload
import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface UploadRepository : CrudRepository<Upload, UUID>
