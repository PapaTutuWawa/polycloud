package me.polynom.polycloud.api

import me.polynom.polycloud.api.dto.AuthMechanismDto
import me.polynom.polycloud.api.dto.AuthMechanismsDto
import me.polynom.polycloud.api.dto.EnabledAppDto
import me.polynom.polycloud.api.dto.EnabledAppsDto
import me.polynom.polycloud.api.dto.HealthDto
import me.polynom.polycloud.plugin.PolycloudPlugin
import me.polynom.polycloud.plugin.auth.PolycloudAuthPlugin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * RestController that provides core-specific APIs.
 */
@RestController
@RequestMapping("/api/v1")
class CoreApiController(
    /** List of active auth plugins. */
    private val authPlugins: List<PolycloudAuthPlugin>,
    /** List of active plugins. */
    private val plugins: List<PolycloudPlugin>,
) {
    /**
     * Endpoint that indicates that the server is up.
     */
    @GetMapping("/public/health", produces = ["application/json"])
    fun health() =
        HealthDto(
            status = "OK",
        )

    @GetMapping("/public/auth/mechanisms")
    fun authMechanisms() =
        AuthMechanismsDto(
            mechanisms =
                authPlugins.map {
                    val data = it.getData()
                    AuthMechanismDto(
                        id = it.javaClass.name,
                        displayName = data.displayName,
                    )
                },
        )

    @GetMapping("/apps")
    fun apps() =
        EnabledAppsDto(
            apps =
                plugins
                    .filter {
                        it !is PolycloudAuthPlugin
                    }.map {
                        EnabledAppDto(
                            id = it.javaClass.name,
                        )
                    },
        )
}
