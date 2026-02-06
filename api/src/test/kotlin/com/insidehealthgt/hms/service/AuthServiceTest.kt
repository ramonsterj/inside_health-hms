package com.insidehealthgt.hms.service

import com.insidehealthgt.hms.dto.request.LoginRequest
import com.insidehealthgt.hms.dto.request.RefreshTokenRequest
import com.insidehealthgt.hms.entity.RefreshToken
import com.insidehealthgt.hms.entity.User
import com.insidehealthgt.hms.entity.UserStatus
import com.insidehealthgt.hms.exception.AccountDisabledException
import com.insidehealthgt.hms.exception.InvalidCredentialsException
import com.insidehealthgt.hms.repository.RoleRepository
import com.insidehealthgt.hms.repository.UserRepository
import com.insidehealthgt.hms.security.CustomUserDetails
import com.insidehealthgt.hms.security.JwtTokenProvider
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AuthServiceTest {

    private lateinit var userRepository: UserRepository
    private lateinit var roleRepository: RoleRepository
    private lateinit var passwordEncoder: PasswordEncoder
    private lateinit var jwtTokenProvider: JwtTokenProvider
    private lateinit var refreshTokenService: RefreshTokenService
    private lateinit var authService: AuthService

    private lateinit var activeUser: User

    @BeforeEach
    fun setUp() {
        SecurityContextHolder.clearContext()
        userRepository = mock()
        roleRepository = mock()
        passwordEncoder = mock()
        jwtTokenProvider = mock()
        refreshTokenService = mock()

        authService = AuthService(
            userRepository,
            roleRepository,
            passwordEncoder,
            jwtTokenProvider,
            refreshTokenService,
        )

        activeUser = User(
            username = "testuser",
            email = "test@example.com",
            passwordHash = "hashedpassword",
            status = UserStatus.ACTIVE,
        ).apply { id = 1L }
    }

    @Test
    fun `login should return auth response for valid credentials`() {
        whenever(userRepository.findByIdentifierWithRolesAndPermissions("test@example.com"))
            .thenReturn(activeUser)
        whenever(passwordEncoder.matches("password123", "hashedpassword")).thenReturn(true)
        whenever(jwtTokenProvider.generateAccessToken(1L, "testuser")).thenReturn("access-token")
        whenever(jwtTokenProvider.getAccessTokenExpiration()).thenReturn(900_000L)
        whenever(refreshTokenService.createRefreshToken(activeUser))
            .thenReturn(
                RefreshToken(
                    token = "refresh-token",
                    expiresAt = LocalDateTime.now().plusDays(7),
                    user = activeUser,
                ),
            )

        val response = authService.login(LoginRequest(identifier = "test@example.com", password = "password123"))

        assertEquals("access-token", response.accessToken)
        assertEquals("refresh-token", response.refreshToken)
        assertEquals(900L, response.expiresIn)
        assertNotNull(response.user)
    }

    @Test
    fun `login should throw InvalidCredentialsException when user not found`() {
        whenever(userRepository.findByIdentifierWithRolesAndPermissions("unknown@example.com"))
            .thenReturn(null)

        assertThrows<InvalidCredentialsException> {
            authService.login(LoginRequest(identifier = "unknown@example.com", password = "password123"))
        }
    }

    @Test
    fun `login should throw InvalidCredentialsException when password is wrong`() {
        whenever(userRepository.findByIdentifierWithRolesAndPermissions("test@example.com"))
            .thenReturn(activeUser)
        whenever(passwordEncoder.matches("wrongpassword", "hashedpassword")).thenReturn(false)

        assertThrows<InvalidCredentialsException> {
            authService.login(LoginRequest(identifier = "test@example.com", password = "wrongpassword"))
        }
    }

    @Test
    fun `login should throw AccountDisabledException when user is inactive`() {
        val inactiveUser = User(
            username = "inactive",
            email = "inactive@example.com",
            passwordHash = "hashedpassword",
            status = UserStatus.INACTIVE,
        ).apply { id = 2L }

        whenever(userRepository.findByIdentifierWithRolesAndPermissions("inactive@example.com"))
            .thenReturn(inactiveUser)
        whenever(passwordEncoder.matches("password123", "hashedpassword")).thenReturn(true)

        val exception = assertThrows<AccountDisabledException> {
            authService.login(LoginRequest(identifier = "inactive@example.com", password = "password123"))
        }
        assertEquals("Your account is inactive. Please contact support.", exception.message)
    }

    @Test
    fun `login should throw AccountDisabledException when user is suspended`() {
        val suspendedUser = User(
            username = "suspended",
            email = "suspended@example.com",
            passwordHash = "hashedpassword",
            status = UserStatus.SUSPENDED,
        ).apply { id = 3L }

        whenever(userRepository.findByIdentifierWithRolesAndPermissions("suspended@example.com"))
            .thenReturn(suspendedUser)
        whenever(passwordEncoder.matches("password123", "hashedpassword")).thenReturn(true)

        val exception = assertThrows<AccountDisabledException> {
            authService.login(LoginRequest(identifier = "suspended@example.com", password = "password123"))
        }
        assertEquals("Your account is suspended. Please contact support.", exception.message)
    }

    @Test
    fun `refresh should return new auth response for valid refresh token`() {
        val refreshToken = RefreshToken(
            token = "valid-refresh",
            expiresAt = LocalDateTime.now().plusDays(7),
            user = activeUser,
        )
        whenever(refreshTokenService.validateRefreshToken("valid-refresh")).thenReturn(refreshToken)
        whenever(jwtTokenProvider.generateAccessToken(1L, "testuser")).thenReturn("new-access-token")
        whenever(jwtTokenProvider.getAccessTokenExpiration()).thenReturn(900_000L)
        whenever(refreshTokenService.createRefreshToken(activeUser))
            .thenReturn(
                RefreshToken(
                    token = "new-refresh-token",
                    expiresAt = LocalDateTime.now().plusDays(7),
                    user = activeUser,
                ),
            )

        val response = authService.refresh(RefreshTokenRequest(refreshToken = "valid-refresh"))

        assertEquals("new-access-token", response.accessToken)
        assertEquals("new-refresh-token", response.refreshToken)
        verify(refreshTokenService).revokeToken("valid-refresh")
    }

    @Test
    fun `refresh should revoke token and throw when user is disabled`() {
        val disabledUser = User(
            username = "disabled",
            email = "disabled@example.com",
            passwordHash = "hashedpassword",
            status = UserStatus.INACTIVE,
        ).apply { id = 4L }

        val refreshToken = RefreshToken(
            token = "disabled-user-refresh",
            expiresAt = LocalDateTime.now().plusDays(7),
            user = disabledUser,
        )
        whenever(refreshTokenService.validateRefreshToken("disabled-user-refresh")).thenReturn(refreshToken)

        assertThrows<AccountDisabledException> {
            authService.refresh(RefreshTokenRequest(refreshToken = "disabled-user-refresh"))
        }
        verify(refreshTokenService).revokeToken("disabled-user-refresh")
    }

    @Test
    fun `refresh should propagate InvalidTokenException for expired token`() {
        whenever(refreshTokenService.validateRefreshToken("expired-refresh"))
            .thenThrow(com.insidehealthgt.hms.exception.InvalidTokenException("Refresh token has expired"))

        assertThrows<com.insidehealthgt.hms.exception.InvalidTokenException> {
            authService.refresh(RefreshTokenRequest(refreshToken = "expired-refresh"))
        }
    }

    @Test
    fun `logout should revoke all user tokens`() {
        val securityContext: SecurityContext = mock()
        val authentication: Authentication = mock()
        val userDetails = CustomUserDetails(activeUser)

        whenever(securityContext.authentication).thenReturn(authentication)
        whenever(authentication.principal).thenReturn(userDetails)
        SecurityContextHolder.setContext(securityContext)

        try {
            authService.logout()
            verify(refreshTokenService).revokeUserTokens(activeUser)
        } finally {
            SecurityContextHolder.clearContext()
        }
    }
}
