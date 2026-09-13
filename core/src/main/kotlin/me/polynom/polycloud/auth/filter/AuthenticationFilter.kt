package me.polynom.polycloud.auth.filter

import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import me.polynom.polycloud.auth.AuthenticationData
import me.polynom.polycloud.auth.AuthenticationManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder

/**
 * Filter that finds out what authentication mechanism should be used.
 */
@Component
@Order(1)
class AuthenticationFilter(
    /** The authentication manager. */
    private val manager: AuthenticationManager,
) : Filter {
    /** Logger. */
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

        val header = request.getHeader(AuthenticationData.HEADER)
        request.setAttribute(
            AuthenticationData.REQUEST_ATTRIBUTE_PRESENT,
            header != null,
        )
        if (header?.isEmpty() ?: true) {
            logger.debug("No Authorization header, skipping")
            filterChain.doFilter(request, response)
            return
        }

        val result = manager.authenticate(header)
        if (result == null) {
            logger.debug("Auth manager returned null")
            filterChain.doFilter(request, response)
            return
        }

        logger.debug("Adding [{}] as authentication", result)
        request.setAttribute(AuthenticationData.REQUEST_ATTRIBUTE_RESULT, result)
        RequestContextHolder.currentRequestAttributes().setAttribute(
            AuthenticationData.REQUEST_ATTRIBUTE_RESULT,
            result,
            0,
        )
        filterChain.doFilter(request, response)
    }
}
