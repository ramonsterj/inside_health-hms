package com.insidehealthgt.hms.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider,
    private val userDetailsService: CustomUserDetailsService,
) : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(JwtAuthenticationFilter::class.java)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        trySetAuthentication(request)
        filterChain.doFilter(request, response)
    }

    private fun trySetAuthentication(request: HttpServletRequest) {
        val token = extractTokenFromRequest(request) ?: return
        if (!jwtTokenProvider.validateToken(token) || !jwtTokenProvider.isAccessToken(token)) return

        try {
            val userId = jwtTokenProvider.getUserIdFromToken(token)
            val userDetails = userDetailsService.loadUserById(userId)

            // Only set authentication if user is still active
            if (userDetails.isEnabled) {
                val authentication = UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.authorities,
                )
                authentication.details = WebAuthenticationDetailsSource().buildDetails(request)

                SecurityContextHolder.getContext().authentication = authentication
                log.debug("Set authentication for user: {}", userId)
            } else {
                log.debug("User {} is not active, rejecting authentication", userId)
            }
        } catch (ex: UsernameNotFoundException) {
            log.debug("User not found for token: {}", ex.message)
        } catch (ex: NumberFormatException) {
            log.debug("Invalid user ID in token: {}", ex.message)
        }
    }

    private fun extractTokenFromRequest(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader("Authorization")
        return if (bearerToken != null && bearerToken.startsWith(BEARER_PREFIX)) {
            bearerToken.substring(BEARER_PREFIX.length)
        } else {
            null
        }
    }

    companion object {
        private const val BEARER_PREFIX = "Bearer "
    }
}
