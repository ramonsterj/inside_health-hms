package com.insidehealthgt.hms.audit

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

private const val FILTER_ORDER_OFFSET = 10

/**
 * Filter that captures request context (IP address, user agent) for audit logging.
 * Runs early in the filter chain to ensure context is available for all operations.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + FILTER_ORDER_OFFSET)
class AuditContextFilter : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        try {
            val ipAddress = extractClientIp(request)
            val userAgent = request.getHeader("User-Agent")

            AuditContext.set(
                AuditContextData(
                    ipAddress = ipAddress,
                    userAgent = userAgent,
                ),
            )

            filterChain.doFilter(request, response)
        } finally {
            AuditContext.clear()
        }
    }

    /**
     * Extracts the client IP address, handling proxies and load balancers.
     * Checks X-Forwarded-For header first, then falls back to remoteAddr.
     */
    private fun extractClientIp(request: HttpServletRequest): String? {
        val xForwardedFor = request.getHeader("X-Forwarded-For")
        return if (!xForwardedFor.isNullOrBlank()) {
            // X-Forwarded-For can contain multiple IPs: client, proxy1, proxy2
            // The first one is the original client IP
            xForwardedFor.split(",").firstOrNull()?.trim()
        } else {
            request.remoteAddr
        }
    }
}
