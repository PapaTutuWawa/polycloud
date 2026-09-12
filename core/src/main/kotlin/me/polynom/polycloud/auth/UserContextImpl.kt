package me.polynom.polycloud.auth

import me.polynom.polycloud.plugin.auth.UserContext
import me.polynom.polycloud.plugin.auth.dto.AuthVerificationResult
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder

/**
 * Implementation of the {@link UserContext} interface to allow plugins to
 * get information about the currently requesting user.
 */
@Component
class UserContextImpl : UserContext {
    override fun getUser(): AuthVerificationResult? {
        return RequestContextHolder.currentRequestAttributes().getAttribute(
            AuthenticationData.REQUEST_ATTRIBUTE,
            0,
        ) as AuthVerificationResult?
    }
}
