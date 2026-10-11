package me.polynom.polycloud.auth.filter

import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import me.polynom.polycloud.auth.AuthenticationData
import me.polynom.polycloud.auth.RouteAuthenticationEvaluator
import me.polynom.polycloud.plugin.auth.PolycloudAuthPlugin
import me.polynom.polycloud.plugin.auth.dto.AuthVerificationResult
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.annotation.Order
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component

/**
 * Filter that returns a 401 if the user is not authenticated.
 */
@Component
@Order(2)
class PostAuthenticationFilter(
    /** The route evaluator. */
    private val evaluator: RouteAuthenticationEvaluator,
    /** Used to advertise every registered scheme via `WWW-Authenticate` on a 401. */
    private val authPlugins: List<PolycloudAuthPlugin>,
) : Filter {
    /** Logging. */
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    @Suppress("ReturnCount")
    override fun doFilter(
        request: ServletRequest,
        response: ServletResponse,
        filterChain: FilterChain,
    ) {
        if (request !is HttpServletRequest) {
            logger.debug("Request is not a HttpServletRequest. Skipping.")
            filterChain.doFilter(request, response)
            return
        }
        val httpResponse = response as HttpServletResponse

        // Do not apply this work if the path is not authenticated.
        if (!evaluator.isPathAuthenticated(request.servletPath)) {
            logger.debug("Skipping request to [{}] as it is not marked as authenticated", request.servletPath)
            filterChain.doFilter(request, response)
            return
        }

        val auth = request.getAttribute(AuthenticationData.REQUEST_ATTRIBUTE_RESULT) as AuthVerificationResult?
        logger.debug("Checking authentication using [{}]", auth)
        if (auth == null) {
            logger.debug("Rejecting request to [{}] because there is no authentication data", request.servletPath)

            // Return a 401, if the client did not provide authentication at all, and a 403 if it was wrong.
            if (!(request.getAttribute(AuthenticationData.REQUEST_ATTRIBUTE_PRESENT) as Boolean)) {
                httpResponse.status = HttpStatus.UNAUTHORIZED.value()
                addAuthenticationChallenge(httpResponse)
            } else {
                httpResponse.status = HttpStatus.FORBIDDEN.value()
            }
            return
        }

        filterChain.doFilter(request, response)
    }

    /**
     * Without a `WWW-Authenticate` challenge, clients that rely on it (e.g. native WebDAV
     * clients using Basic auth) have no signal to prompt for credentials and just fail silently.
     */
    private fun addAuthenticationChallenge(response: HttpServletResponse) {
        authPlugins
            .mapNotNull { it.getData().scheme }
            .distinct()
            .forEach { scheme -> response.addHeader(HttpHeaders.WWW_AUTHENTICATE, "$scheme realm=\"PolyCloud\"") }
    }
}
