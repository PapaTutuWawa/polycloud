package me.polynom.polycloud.security

import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

/**
 * Request filter that adds CORS headers. Specifically, Access-Control-Allow-Origin. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class CorsFilter : Filter {
    /** Allowed HTTP methods. */
    private val allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS").joinToString(",")

    override fun doFilter(
        request: ServletRequest,
        response: ServletResponse,
        filterChain: FilterChain
    ) {
        if (request !is HttpServletRequest || response !is HttpServletResponse) {
            filterChain.doFilter(request, response)
            return
        }

        response.setHeader("Access-Control-Allow-Origin", "*")
        response.setHeader("Access-Control-Allow-Headers", "*")
        response.setHeader("Access-Control-Allow-Methods", allowedMethods)

        // If we have a CORS preflight request, then just terminate here and do not run anything else.
        if (request.method == "OPTIONS") {
            response.status = 200;
            return;
        }

        filterChain.doFilter(request, response)
    }
}