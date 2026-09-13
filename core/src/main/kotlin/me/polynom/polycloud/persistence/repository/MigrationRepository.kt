package me.polynom.polycloud.persistence.repository

import me.polynom.polycloud.persistence.entities.Migration
import org.springframework.data.repository.CrudRepository

interface MigrationRepository : CrudRepository<Migration, String>
