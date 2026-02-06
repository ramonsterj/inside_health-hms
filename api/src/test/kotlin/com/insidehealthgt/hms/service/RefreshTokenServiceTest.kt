package com.insidehealthgt.hms.service

import com.insidehealthgt.hms.entity.RefreshToken
import com.insidehealthgt.hms.entity.User
import com.insidehealthgt.hms.exception.InvalidTokenException
import com.insidehealthgt.hms.repository.RefreshTokenRepository
import com.insidehealthgt.hms.security.JwtTokenProvider
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RefreshTokenServiceTest {

    private lateinit var refreshTokenRepository: RefreshTokenRepository
    private lateinit var jwtTokenProvider: JwtTokenProvider
    private lateinit var refreshTokenService: RefreshTokenService

    private lateinit var testUser: User

    @BeforeEach
    fun setUp() {
        refreshTokenRepository = mock()
        jwtTokenProvider = mock()

        refreshTokenService = RefreshTokenService(
            refreshTokenRepository,
            jwtTokenProvider,
        )

        testUser = User(
            username = "testuser",
            email = "test@example.com",
            passwordHash = "hashedpassword",
        ).apply { id = 1L }
    }

    @Test
    fun `createRefreshToken should generate and save token`() {
        whenever(jwtTokenProvider.generateRefreshToken(1L)).thenReturn("jwt-refresh-token")
        whenever(jwtTokenProvider.getRefreshTokenExpiration()).thenReturn(604_800_000L) // 7 days in ms
        whenever(refreshTokenRepository.save(any<RefreshToken>())).thenAnswer { it.arguments[0] }

        val before = LocalDateTime.now().plusDays(6)
        val result = refreshTokenService.createRefreshToken(testUser)
        val after = LocalDateTime.now().plusDays(8)

        assertEquals("jwt-refresh-token", result.token)
        assertEquals(testUser, result.user)
        assertNotNull(result.expiresAt)
        assertTrue(result.expiresAt.isAfter(before))
        assertTrue(result.expiresAt.isBefore(after))
        verify(refreshTokenRepository).save(any<RefreshToken>())
    }

    @Test
    fun `validateRefreshToken should return token when valid`() {
        val validToken = RefreshToken(
            token = "valid-token",
            expiresAt = LocalDateTime.now().plusDays(7),
            user = testUser,
        ).apply { id = 1L }

        whenever(refreshTokenRepository.findByToken("valid-token")).thenReturn(validToken)

        val result = refreshTokenService.validateRefreshToken("valid-token")

        assertEquals(validToken, result)
    }

    @Test
    fun `validateRefreshToken should throw when token not found`() {
        whenever(refreshTokenRepository.findByToken("nonexistent")).thenReturn(null)

        val exception = assertThrows<InvalidTokenException> {
            refreshTokenService.validateRefreshToken("nonexistent")
        }
        assertTrue(exception.message!!.contains("not found"))
    }

    @Test
    fun `validateRefreshToken should throw when token is expired`() {
        val expiredToken = RefreshToken(
            token = "expired-token",
            expiresAt = LocalDateTime.now().minusHours(1),
            user = testUser,
        ).apply { id = 2L }

        whenever(refreshTokenRepository.findByToken("expired-token")).thenReturn(expiredToken)

        val exception = assertThrows<InvalidTokenException> {
            refreshTokenService.validateRefreshToken("expired-token")
        }
        assertTrue(exception.message!!.contains("expired"))
    }

    @Test
    fun `revokeUserTokens should delete all tokens for user`() {
        refreshTokenService.revokeUserTokens(testUser)

        verify(refreshTokenRepository).deleteByUser(testUser)
    }

    @Test
    fun `revokeToken should delete specific token`() {
        val token = RefreshToken(
            token = "token-to-revoke",
            expiresAt = LocalDateTime.now().plusDays(7),
            user = testUser,
        ).apply { id = 4L }

        whenever(refreshTokenRepository.findByToken("token-to-revoke")).thenReturn(token)

        refreshTokenService.revokeToken("token-to-revoke")

        verify(refreshTokenRepository).delete(token)
    }

    @Test
    fun `cleanupExpiredTokens should delete expired tokens`() {
        refreshTokenService.cleanupExpiredTokens()

        verify(refreshTokenRepository).deleteByExpiresAtBefore(any())
    }
}
