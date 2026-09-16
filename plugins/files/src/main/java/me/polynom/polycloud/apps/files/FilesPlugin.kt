package me.polynom.polycloud.apps.files

import me.polynom.polycloud.apps.files.autoconfigure.PluginEnabled
import me.polynom.polycloud.plugin.PolycloudPlugin
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@PluginEnabled
@Component
class FilesPlugin : PolycloudPlugin {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    override fun register() {
        logger.info("Files plugin loaded")
    }
}
