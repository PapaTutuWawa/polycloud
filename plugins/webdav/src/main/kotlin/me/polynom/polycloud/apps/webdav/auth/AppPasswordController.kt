package me.polynom.polycloud.apps.webdav.auth

import me.polynom.polycloud.apps.webdav.auth.dto.AppPasswordSummaryDto
import me.polynom.polycloud.apps.webdav.auth.dto.CreateAppPasswordRequest
import me.polynom.polycloud.apps.webdav.auth.dto.CreateAppPasswordResponse
import me.polynom.polycloud.apps.webdav.autoconfigure.PluginEnabled
import me.polynom.polycloud.plugin.auth.UserContext
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/** Lets an already-logged-in user manage app passwords for their own WebDAV clients. */
@PluginEnabled
@RestController
@RequestMapping("/api/apps/webdav/app-passwords")
class AppPasswordController(
    private val appPasswordService: AppPasswordService,
    private val userContext: UserContext,
) {
    @PostMapping
    fun create(
        @RequestBody request: CreateAppPasswordRequest,
    ): CreateAppPasswordResponse {
        val user = userContext.getUser()!!
        val (appPassword, secret) = appPasswordService.create(user.username, request.name, user.roles)
        return CreateAppPasswordResponse(appPassword.id.toString(), appPassword.name, secret)
    }

    @GetMapping
    fun list(): List<AppPasswordSummaryDto> {
        val user = userContext.getUser()!!
        return appPasswordService.list(user.username).map {
            AppPasswordSummaryDto(it.id.toString(), it.name, it.createdAt.toString())
        }
    }

    @DeleteMapping("/{id}")
    fun revoke(
        @PathVariable id: UUID,
    ): ResponseEntity<Unit> {
        val user = userContext.getUser()!!
        return if (appPasswordService.revoke(user.username, id)) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }
}
