package com.insidehealthgt.hms.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class JwtAuthenticationFilterTest {

    private val jwtTokenProvider: JwtTokenProvider = mock()
    private val userDetailsService: CustomUserDetailsService = mock()
    private val filter = JwtAuthenticationFilter(jwtTokenProvider, userDetailsService)

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    // Regression guard for the StreamingResponseBody / admission PDF export bug:
    // OncePerRequestFilter skips async and error dispatches by default, which would
    // leave SecurityContext empty during the body write and cause AuthorizationFilter
    // to deny access after the response is already committed.
    @Test
    fun `filter runs on async dispatch so stateless JWT auth survives StreamingResponseBody`() {
        assertFalse(filter.shouldNotFilterAsyncDispatch())
    }

    @Test
    fun `filter runs on error dispatch so error pages see the same authentication`() {
        assertFalse(filter.shouldNotFilterErrorDispatch())
    }

    @Test
    fun `valid bearer token populates SecurityContext`() {
        val token = "valid-token"
        val userDetails = mock<CustomUserDetails>()
        whenever(jwtTokenProvider.validateToken(token)).thenReturn(true)
        whenever(jwtTokenProvider.isAccessToken(token)).thenReturn(true)
        whenever(jwtTokenProvider.getUserIdFromToken(token)).thenReturn(42L)
        whenever(userDetailsService.loadUserById(42L)).thenReturn(userDetails)
        whenever(userDetails.isEnabled).thenReturn(true)
        whenever(userDetails.authorities).thenReturn(emptyList())

        val request = MockHttpServletRequest().apply {
            addHeader("Authorization", "Bearer $token")
        }
        val response = MockHttpServletResponse()
        val chain = MockFilterChain()

        filter.doFilter(request, response, chain)

        val auth = SecurityContextHolder.getContext().authentication
        assertNotNull(auth)
        assertEquals(userDetails, auth.principal)
    }

    @Test
    fun `missing authorization header leaves SecurityContext empty and continues chain`() {
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        val chain: FilterChain = mock()

        filter.doFilter(request, response, chain)

        verify(chain).doFilter(any<HttpServletRequest>(), any<HttpServletResponse>())
        assertEquals(null, SecurityContextHolder.getContext().authentication)
    }
}
