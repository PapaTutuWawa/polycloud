package me.polynom.polycloud.config

import me.polynom.polycloud.plugin.database.DatabaseMigration
import org.springframework.context.annotation.Configuration

/**
 * Config class for database related configuration.
 */
@Configuration
class CoreDatabaseConfig : DatabaseMigration
