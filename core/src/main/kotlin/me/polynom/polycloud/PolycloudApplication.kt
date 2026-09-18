package me.polynom.polycloud

import me.polynom.polycloud.config.JwtConfig
import me.polynom.polycloud.database.MigrationManager
import me.polynom.polycloud.plugin.PolycloudPlugin
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.event.EventListener

@SpringBootApplication
@EnableConfigurationProperties(JwtConfig::class)
class PolycloudApplication(
    /** List of active plugins. */
    private val plugins: List<PolycloudPlugin>,
    /** Migration manager. */
    private val migrationManager: MigrationManager,
) {
    @EventListener(ApplicationReadyEvent::class)
    fun applicationReadyEvent() {
        plugins.forEach(PolycloudPlugin::register)
        migrationManager.runMigrations()
    }
}

fun main(args: Array<String>) {
    runApplication<PolycloudApplication>(*args)
}
