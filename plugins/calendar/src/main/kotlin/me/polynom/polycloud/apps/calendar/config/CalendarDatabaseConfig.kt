package me.polynom.polycloud.apps.calendar.config

import me.polynom.polycloud.apps.calendar.autoconfigure.PluginEnabled
import me.polynom.polycloud.plugin.database.DatabaseMigration
import org.springframework.context.annotation.Configuration

@Configuration
@PluginEnabled
open class CalendarDatabaseConfig : DatabaseMigration {

}