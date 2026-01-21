package com.insidehealthgt.hms.service

import com.insidehealthgt.hms.entity.PasswordResetToken
import com.insidehealthgt.hms.entity.User
import com.insidehealthgt.hms.exception.InvalidTokenException
import com.insidehealthgt.hms.repository.PasswordResetTokenRepository
import com.insidehealthgt.hms.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PasswordResetServiceTest {

    private lateinit var passwordResetTokenRepository: PasswordResetTokenRepository
    private lateinit var userRepository: UserRepository
    private lateinit var emailService: EmailService
    private lateinit var passwordEncoder: PasswordEncoder
    private lateinit var passwordResetService: PasswordResetService

    private lateinit var testUser: User

    @BeforeEach
    fun setUp() {
        passwordResetTokenRepository = mock()
        userRepository = mock()
        emailService = mock()
        passwordEncoder = mock()

        passwordResetService = PasswordResetService(
            passwordResetTokenRepository,
            userRepository,
            emailService,
            passwordEncoder,
        )

        testUser = User(
            username = "testuser",
            email = "test@example.com",
            passwordHash = "hashedpassword",
        ).apply { id = 1L }
    }

    @Test
    fun `initiatePasswordReset should create token and send email when user exists`() {
        whenever(userRepository.findByEmail("test@example.com")).thenReturn(testUser)
        whenever(passwordResetTokenRepository.save(any<PasswordResetToken>()))
            .thenAnswer { it.arguments[0] }

        passwordResetService.initiatePasswordReset("test@example.com")

        verify(passwordResetTokenRepository).deleteByUser(testUser)
        verify(passwordResetTokenRepository).save(any<PasswordResetToken>())
        verify(emailService).sendPasswordResetEmail(any(), any())
    }

    @Test
    fun `initiatePasswordReset should do nothing when user not found`() {
        whenever(userRepository.findByEmail("nonexistent@example.com")).thenReturn(null)

        passwordResetService.initiatePasswordReset("nonexistent@example.com")

        verify(passwordResetTokenRepository, never()).save(any())
        verify(emailService, never()).sendPasswordResetEmail(any(), any())
    }

    @Test
    fun `initiatePasswordReset should invalidate existing tokens before creating new one`() {
        whenever(userRepository.findByEmail("test@example.com")).thenReturn(testUser)
        whenever(passwordResetTokenRepository.save(any<PasswordResetToken>()))
            .thenAnswer { it.arguments[0] }

        passwordResetService.initiatePasswordReset("test@example.com")

        verify(passwordResetTokenRepository).deleteByUser(testUser)
    }

    @Test
    fun `resetPassword should update password and delete token`() {
        val token = "valid-token"
        val resetToken = PasswordResetToken(
            token = token,
            expiresAt = LocalDateTime.now().plusHours(1),
            user = testUser,
        ).apply { id = 1L }

        whenever(passwordResetTokenRepository.findByToken(token)).thenReturn(resetToken)
        whenever(passwordEncoder.encode("newpassword123")).thenReturn("encodedhash")
        whenever(userRepository.save(any<User>())).thenReturn(testUser)

        passwordResetService.resetPassword(token, "newpassword123")

        verify(userRepository).save(testUser)
        verify(passwordResetTokenRepository).delete(resetToken)
        assertEquals("encodedhash", testUser.passwordHash)
    }

    @Test
    fun `resetPassword should throw exception for invalid token`() {
        whenever(passwordResetTokenRepository.findByToken("invalid-token")).thenReturn(null)

        val exception = assertThrows<InvalidTokenException> {
            passwordResetService.resetPassword("invalid-token", "newpassword")
        }

        assertTrue(exception.message!!.contains("Invalid"))
    }

    @Test
    fun `resetPassword should throw exception for expired token`() {
        val expiredToken = PasswordResetToken(
            token = "expired-token",
            expiresAt = LocalDateTime.now().minusHours(1),
            user = testUser,
        )
        whenever(passwordResetTokenRepository.findByToken("expired-token")).thenReturn(expiredToken)

        val exception = assertThrows<InvalidTokenException> {
            passwordResetService.resetPassword("expired-token", "newpassword")
        }

        assertTrue(exception.message!!.contains("expired"))
    }

    @Test
    fun `generated token should be 64 characters hex string`() {
        whenever(userRepository.findByEmail("test@example.com")).thenReturn(testUser)

        val tokenCaptor = argumentCaptor<PasswordResetToken>()
        whenever(passwordResetTokenRepository.save(tokenCaptor.capture()))
            .thenAnswer { it.arguments[0] }

        passwordResetService.initiatePasswordReset("test@example.com")

        val capturedToken = tokenCaptor.firstValue.token
        assertEquals(64, capturedToken.length)
        assertTrue(capturedToken.matches(Regex("[0-9a-f]+")))
    }

    @Test
    fun `token expiration should be set to 1 hour from now`() {
        whenever(userRepository.findByEmail("test@example.com")).thenReturn(testUser)

        val tokenCaptor = argumentCaptor<PasswordResetToken>()
        whenever(passwordResetTokenRepository.save(tokenCaptor.capture()))
            .thenAnswer { it.arguments[0] }

        val beforeCall = LocalDateTime.now()
        passwordResetService.initiatePasswordReset("test@example.com")
        val afterCall = LocalDateTime.now()

        val expiresAt = tokenCaptor.firstValue.expiresAt
        assertTrue(expiresAt.isAfter(beforeCall.plusMinutes(59)))
        assertTrue(expiresAt.isBefore(afterCall.plusMinutes(61)))
    }

    @Test
    fun `cleanupExpiredTokens should delete expired tokens`() {
        passwordResetService.cleanupExpiredTokens()

        verify(passwordResetTokenRepository).deleteByExpiresAtBefore(any())
    }
}
