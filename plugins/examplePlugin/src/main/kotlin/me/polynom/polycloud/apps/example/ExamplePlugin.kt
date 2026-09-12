package me.polynom.polycloud.apps.example

import me.polynom.polycloud.apps.example.autoconfigure.PluginEnabled
import me.polynom.polycloud.apps.example.dto.ExampleResponseDto
import me.polynom.polycloud.plugin.PolycloudPlugin
import me.polynom.polycloud.plugin.auth.UserContext
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Example plugin that returns a simple JSON response on /api/apps/example/test
 */
@PluginEnabled
@RestController
@RequestMapping("/api/apps/example")
class ExamplePlugin(
    private val userContext: UserContext,
) : PolycloudPlugin {
    /** Logger. */
    private val logger = LoggerFactory.getLogger(this.javaClass)

    override fun register() {
        logger.info("Example plugin loaded!")
    }

    @GetMapping("/test")
    fun test() = ExampleResponseDto("Hello World!")

    @GetMapping("/authenticated")
    fun authenticated() = ExampleResponseDto("Hello ${userContext.getUser()?.username}")
}