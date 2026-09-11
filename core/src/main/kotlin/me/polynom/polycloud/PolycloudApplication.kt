package me.polynom.polycloud

import me.polynom.polycloud.plugin.PolycloudPlugin
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.event.EventListener

@SpringBootApplication
@EnableConfigurationProperties
class PolycloudApplication(
	/** List of active plugins. */
	private val plugins: List<PolycloudPlugin>
) {
	@EventListener(ApplicationReadyEvent::class)
	fun applicationReadyEvent() {
		plugins.forEach(PolycloudPlugin::register)
	}
}

fun main(args: Array<String>) {
	runApplication<PolycloudApplication>(*args)
}
