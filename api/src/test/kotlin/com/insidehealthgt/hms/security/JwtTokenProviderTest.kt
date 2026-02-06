package com.insidehealthgt.hms.security

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JwtTokenProviderTest {

    private lateinit var jwtTokenProvider: JwtTokenProvider

    @BeforeEach
    fun setUp() {
        // Use a test secret that meets minimum length requirement (256 bits = 32 bytes)
        jwtTokenProvider = JwtTokenProvider(
            jwtSecret = "test-secret-key-that-is-at-least-256-bits-long-for-hmac-sha256",
            accessTokenExpiration = 900000L, // 15 minutes
            refreshTokenExpiration = 604800000L, // 7 days
        )
    }

    @Test
    fun `generateAccessToken should create valid token`() {
        val token = jwtTokenProvider.generateAccessToken(1L, "test@example.com")

        assertTrue(token.isNotEmpty())
        assertTrue(jwtTokenProvider.validateToken(token))
        assertTrue(jwtTokenProvider.isAccessToken(token))
    }

    @Test
    fun `generateRefreshToken should create valid token`() {
        val token = jwtTokenProvider.generateRefreshToken(1L)

        assertTrue(token.isNotEmpty())
        assertTrue(jwtTokenProvider.validateToken(token))
        assertFalse(jwtTokenProvider.isAccessToken(token))
    }

    @Test
    fun `getUserIdFromToken should extract user ID`() {
        val token = jwtTokenProvider.generateAccessToken(42L, "test@example.com")

        val userId = jwtTokenProvider.getUserIdFromToken(token)

        assertEquals(42L, userId)
    }

    @Test
    fun `getEmailFromToken should extract email`() {
        val token = jwtTokenProvider.generateAccessToken(1L, "test@example.com")

        val email = jwtTokenProvider.getEmailFromToken(token)

        assertEquals("test@example.com", email)
    }

    @Test
    fun `validateToken should return false for invalid token`() {
        val result = jwtTokenProvider.validateToken("invalid.token.here")

        assertFalse(result)
    }

    @Test
    fun `validateToken should return false for tampered token`() {
        val token = jwtTokenProvider.generateAccessToken(1L, "test@example.com")
        val tamperedToken = token.dropLast(5) + "xxxxx"

        val result = jwtTokenProvider.validateToken(tamperedToken)

        assertFalse(result)
    }

    @Test
    fun `validateToken should return false for expired token`() {
        // Create provider with 0ms expiration
        val shortLivedProvider = JwtTokenProvider(
            jwtSecret = "test-secret-key-that-is-at-least-256-bits-long-for-hmac-sha256",
            accessTokenExpiration = 0L,
            refreshTokenExpiration = 0L,
        )

        val token = shortLivedProvider.generateAccessToken(1L, "test@example.com")

        // Token should be invalid immediately (expired)
        val result = shortLivedProvider.validateToken(token)
        assertFalse(result)
    }

    @Test
    fun `getEmailFromToken should return null for refresh token`() {
        val token = jwtTokenProvider.generateRefreshToken(1L)

        val email = jwtTokenProvider.getEmailFromToken(token)

        assertEquals(null, email)
    }

    @Test
    fun `validateToken should return false for empty string`() {
        val result = jwtTokenProvider.validateToken("")

        assertFalse(result)
    }

    @Test
    fun `getAccessTokenExpiration should return configured value`() {
        assertEquals(900000L, jwtTokenProvider.getAccessTokenExpiration())
    }

    @Test
    fun `getRefreshTokenExpiration should return configured value`() {
        assertEquals(604800000L, jwtTokenProvider.getRefreshTokenExpiration())
    }
}
