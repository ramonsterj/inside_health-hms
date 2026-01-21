package com.insidehealthgt.hms.service

import com.insidehealthgt.hms.entity.PasswordResetToken
import com.insidehealthgt.hms.exception.InvalidTokenException
import com.insidehealthgt.hms.repository.PasswordResetTokenRepository
import com.insidehealthgt.hms.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.LocalDateTime

@Service
class PasswordResetService(
    private val passwordResetTokenRepository: PasswordResetTokenRepository,
    private val userRepository: UserRepository,
    private val emailService: EmailService,
    private val passwordEncoder: PasswordEncoder,
) {

    companion object {
        private const val TOKEN_EXPIRATION_HOURS = 1L
        private const val TOKEN_BYTE_LENGTH = 32
    }

    @Transactional
    fun initiatePasswordReset(email: String) {
        val user = userRepository.findByEmail(email) ?: return

        passwordResetTokenRepository.deleteByUser(user)

        val token = generateSecureToken()
        val expiresAt = LocalDateTime.now().plusHours(TOKEN_EXPIRATION_HOURS)

        val resetToken = PasswordResetToken(
            token = token,
            expiresAt = expiresAt,
            user = user,
        )

        passwordResetTokenRepository.save(resetToken)

        emailService.sendPasswordResetEmail(email, token)
    }

    @Transactional
    fun resetPassword(token: String, newPassword: String) {
        val resetToken = passwordResetTokenRepository.findByToken(token)
            ?: throw InvalidTokenException("Invalid or expired password reset token")

        validateToken(resetToken)

        val user = resetToken.user
        user.passwordHash = passwordEncoder.encode(newPassword)!!
        userRepository.save(user)

        passwordResetTokenRepository.delete(resetToken)
    }

    @Transactional
    fun cleanupExpiredTokens() {
        passwordResetTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now())
    }

    private fun validateToken(resetToken: PasswordResetToken) {
        if (resetToken.expiresAt.isBefore(LocalDateTime.now())) {
            throw InvalidTokenException("Password reset token has expired")
        }
        if (resetToken.deletedAt != null) {
            throw InvalidTokenException("Password reset token has been used")
        }
    }

    private fun generateSecureToken(): String {
        val random = SecureRandom()
        val bytes = ByteArray(TOKEN_BYTE_LENGTH)
        random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
