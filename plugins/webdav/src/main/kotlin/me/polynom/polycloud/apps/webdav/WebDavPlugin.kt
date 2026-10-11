package me.polynom.polycloud.apps.webdav

import me.polynom.polycloud.apps.webdav.autoconfigure.PluginEnabled
import me.polynom.polycloud.plugin.app.PolycloudPlugin
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@PluginEnabled
@Component
class WebDavPlugin : PolycloudPlugin {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    override fun register() {
        logger.info("WebDAV plugin loaded, serving at /dav/**")
    }
}
