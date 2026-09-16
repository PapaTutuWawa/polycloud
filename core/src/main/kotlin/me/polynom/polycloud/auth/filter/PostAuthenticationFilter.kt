package me.polynom.polycloud.auth.filter

import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import me.polynom.polycloud.auth.AuthenticationData
import me.polynom.polycloud.auth.RouteAuthenticationEvaluator
import me.polynom.polycloud.plugin.auth.dto.AuthVerificationResult
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

/**
 * Filter that returns a 401 if the user is not authenticated.
 */
@Component
@Order(2)
class PostAuthenticationFilter(
    /** The route evaluator. */
    private val evaluator: RouteAuthenticationEvaluator,
) : Filter {
    /** Logging. */
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

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
            httpResponse.status =
                if (!(request.getAttribute(AuthenticationData.REQUEST_ATTRIBUTE_PRESENT) as Boolean)) {
                    401
                } else {
                    403
                }
            return
        }

        filterChain.doFilter(request, response)
    }
}
