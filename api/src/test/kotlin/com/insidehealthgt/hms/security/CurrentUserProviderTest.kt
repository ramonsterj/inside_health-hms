package com.insidehealthgt.hms.security

import com.insidehealthgt.hms.entity.User
import com.insidehealthgt.hms.entity.UserStatus
import com.insidehealthgt.hms.exception.UnauthorizedException
import com.insidehealthgt.hms.service.MessageService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CurrentUserProviderTest {

    private lateinit var messageService: MessageService
    private lateinit var provider: CurrentUserProvider

    @BeforeEach
    fun setUp() {
        SecurityContextHolder.clearContext()
        messageService = mock {
            on { errorNotAuthenticated() } doReturn "Not authenticated"
        }
        provider = CurrentUserProvider(messageService)
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `returns null when context has no authentication`() {
        assertNull(provider.currentUserDetails())
        assertNull(provider.currentUserId())
        assertThrows<UnauthorizedException> { provider.currentUserDetailsOrThrow() }
        assertThrows<UnauthorizedException> { provider.currentUserIdOrThrow() }
    }

    @Test
    fun `returns null when authentication is not authenticated`() {
        val token = UsernamePasswordAuthenticationToken("user", "credentials")
        // UsernamePasswordAuthenticationToken(principal, credentials) is unauthenticated by default.
        SecurityContextHolder.getContext().authentication = token

        assertNull(provider.currentUserDetails())
        assertNull(provider.currentUserId())
        assertThrows<UnauthorizedException> { provider.currentUserDetailsOrThrow() }
        assertThrows<UnauthorizedException> { provider.currentUserIdOrThrow() }
    }

    @Test
    fun `returns null for anonymous principal string`() {
        val token = AnonymousAuthenticationToken(
            "key",
            "anonymousUser",
            listOf(SimpleGrantedAuthority("ROLE_ANONYMOUS")),
        )
        SecurityContextHolder.getContext().authentication = token

        assertNull(provider.currentUserDetails())
        assertNull(provider.currentUserId())
        assertThrows<UnauthorizedException> { provider.currentUserDetailsOrThrow() }
        assertThrows<UnauthorizedException> { provider.currentUserIdOrThrow() }
    }

    @Test
    fun `returns details and id for CustomUserDetails principal`() {
        val user = User(
            username = "alice",
            email = "alice@example.com",
            passwordHash = "hash",
            status = UserStatus.ACTIVE,
        ).apply { id = 42L }
        val details = CustomUserDetails(user)
        val token = UsernamePasswordAuthenticationToken(details, null, details.authorities)
        SecurityContextHolder.getContext().authentication = token

        assertEquals(details, provider.currentUserDetails())
        assertEquals(42L, provider.currentUserId())
        assertEquals(details, provider.currentUserDetailsOrThrow())
        assertEquals(42L, provider.currentUserIdOrThrow())
    }
}
