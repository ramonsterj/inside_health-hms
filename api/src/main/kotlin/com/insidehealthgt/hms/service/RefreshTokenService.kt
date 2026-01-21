package com.insidehealthgt.hms.service

import com.insidehealthgt.hms.entity.RefreshToken
import com.insidehealthgt.hms.entity.User
import com.insidehealthgt.hms.exception.InvalidTokenException
import com.insidehealthgt.hms.repository.RefreshTokenRepository
import com.insidehealthgt.hms.security.JwtTokenProvider
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class RefreshTokenService(
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jwtTokenProvider: JwtTokenProvider,
) {

    companion object {
        private const val MILLIS_PER_SECOND = 1000L
    }

    @Transactional
    fun createRefreshToken(user: User): RefreshToken {
        val tokenString = jwtTokenProvider.generateRefreshToken(user.id!!)
        val expiresAt = LocalDateTime.now().plusSeconds(
            jwtTokenProvider.getRefreshTokenExpiration() / MILLIS_PER_SECOND,
        )

        val refreshToken = RefreshToken(
            token = tokenString,
            expiresAt = expiresAt,
            user = user,
        )

        return refreshTokenRepository.save(refreshToken)
    }

    @Transactional(readOnly = true)
    fun validateRefreshToken(token: String): RefreshToken {
        val refreshToken = refreshTokenRepository.findByToken(token)
            ?: throw InvalidTokenException("Refresh token not found")

        val errorMessage = getValidationError(refreshToken)
        if (errorMessage != null) {
            throw InvalidTokenException(errorMessage)
        }

        return refreshToken
    }

    private fun getValidationError(refreshToken: RefreshToken): String? = when {
        refreshToken.expiresAt.isBefore(LocalDateTime.now()) -> "Refresh token has expired"
        refreshToken.deletedAt != null -> "Refresh token has been revoked"
        else -> null
    }

    @Transactional
    fun revokeUserTokens(user: User) {
        refreshTokenRepository.deleteByUser(user)
    }

    @Transactional
    fun revokeToken(token: String) {
        val refreshToken = refreshTokenRepository.findByToken(token)
        if (refreshToken != null) {
            // Hard delete to allow token string reuse (JWT is deterministic)
            refreshTokenRepository.delete(refreshToken)
        }
    }

    @Transactional
    fun cleanupExpiredTokens() {
        refreshTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now())
    }
}
