package com.insidehealthgt.hms.service

import com.insidehealthgt.hms.dto.request.LoginRequest
import com.insidehealthgt.hms.dto.request.RefreshTokenRequest
import com.insidehealthgt.hms.dto.response.AuthResponse
import com.insidehealthgt.hms.dto.response.UserResponse
import com.insidehealthgt.hms.entity.User
import com.insidehealthgt.hms.entity.UserStatus
import com.insidehealthgt.hms.exception.AccountDisabledException
import com.insidehealthgt.hms.exception.InvalidCredentialsException
import com.insidehealthgt.hms.repository.RoleRepository
import com.insidehealthgt.hms.repository.UserRepository
import com.insidehealthgt.hms.security.CustomUserDetails
import com.insidehealthgt.hms.security.JwtTokenProvider
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider,
    private val refreshTokenService: RefreshTokenService,
) {

    @Transactional
    fun login(request: LoginRequest): AuthResponse {
        val user = findAndValidateUser(request.identifier, request.password)
        validateUserStatus(user)
        return generateAuthResponse(user)
    }

    private fun findAndValidateUser(identifier: String, password: String): User {
        val user = userRepository.findByIdentifierWithRolesAndPermissions(identifier)
            ?: throw InvalidCredentialsException()

        if (!passwordEncoder.matches(password, user.passwordHash)) {
            throw InvalidCredentialsException()
        }

        return user
    }

    private fun validateUserStatus(user: User) {
        if (user.status != UserStatus.ACTIVE) {
            throw AccountDisabledException("Your account is ${getStatusMessage(user.status)}. Please contact support.")
        }
    }

    private fun getStatusMessage(status: UserStatus): String = when (status) {
        UserStatus.INACTIVE -> "inactive"
        UserStatus.SUSPENDED -> "suspended"
        UserStatus.DELETED -> "deleted"
        UserStatus.ACTIVE -> "active"
    }

    @Transactional
    fun refresh(request: RefreshTokenRequest): AuthResponse {
        val refreshToken = refreshTokenService.validateRefreshToken(request.refreshToken)
        val user = refreshToken.user

        // Check user status before issuing new tokens
        if (user.status != UserStatus.ACTIVE) {
            // Revoke the refresh token since account is no longer active
            refreshTokenService.revokeToken(request.refreshToken)
            throw AccountDisabledException("Your account is ${getStatusMessage(user.status)}. Please contact support.")
        }

        // Revoke the old refresh token
        refreshTokenService.revokeToken(request.refreshToken)

        return generateAuthResponse(user)
    }

    @Transactional
    fun logout() {
        val authentication = SecurityContextHolder.getContext().authentication
        if (authentication != null && authentication.principal is CustomUserDetails) {
            val userDetails = authentication.principal as CustomUserDetails
            val user = userDetails.getUser()
            refreshTokenService.revokeUserTokens(user)
        }
    }

    private fun generateAuthResponse(user: User): AuthResponse {
        val accessToken = jwtTokenProvider.generateAccessToken(user.id!!, user.username)
        val refreshToken = refreshTokenService.createRefreshToken(user)

        return AuthResponse(
            accessToken = accessToken,
            refreshToken = refreshToken.token,
            expiresIn = jwtTokenProvider.getAccessTokenExpiration() / 1000,
            user = UserResponse.from(user),
        )
    }
}
