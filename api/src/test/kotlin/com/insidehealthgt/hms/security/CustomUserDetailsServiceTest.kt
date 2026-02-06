package com.insidehealthgt.hms.security

import com.insidehealthgt.hms.entity.User
import com.insidehealthgt.hms.entity.UserStatus
import com.insidehealthgt.hms.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.security.core.userdetails.UsernameNotFoundException
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class CustomUserDetailsServiceTest {

    private lateinit var userRepository: UserRepository
    private lateinit var customUserDetailsService: CustomUserDetailsService

    private lateinit var testUser: User

    @BeforeEach
    fun setUp() {
        userRepository = mock()
        customUserDetailsService = CustomUserDetailsService(userRepository)

        testUser = User(
            username = "testuser",
            email = "test@example.com",
            passwordHash = "hashedpassword",
            status = UserStatus.ACTIVE,
        ).apply { id = 1L }
    }

    @Test
    fun `loadUserByUsername should return UserDetails when found by email`() {
        whenever(userRepository.findByIdentifierWithRolesAndPermissions("test@example.com"))
            .thenReturn(testUser)

        val result = customUserDetailsService.loadUserByUsername("test@example.com")

        assertNotNull(result)
        assertEquals("test@example.com", result.username)
    }

    @Test
    fun `loadUserByUsername should return UserDetails when found by username`() {
        whenever(userRepository.findByIdentifierWithRolesAndPermissions("testuser"))
            .thenReturn(testUser)

        val result = customUserDetailsService.loadUserByUsername("testuser")

        assertNotNull(result)
        assertEquals("test@example.com", result.username)
    }

    @Test
    fun `loadUserByUsername should throw UsernameNotFoundException when not found`() {
        whenever(userRepository.findByIdentifierWithRolesAndPermissions("unknown"))
            .thenReturn(null)

        val exception = assertThrows<UsernameNotFoundException> {
            customUserDetailsService.loadUserByUsername("unknown")
        }
        assertNotNull(exception.message)
    }

    @Test
    fun `loadUserById should return UserDetails when found`() {
        whenever(userRepository.findByIdWithRolesAndPermissions(1L))
            .thenReturn(testUser)

        val result = customUserDetailsService.loadUserById(1L)

        assertNotNull(result)
        assertEquals("test@example.com", result.username)
    }

    @Test
    fun `loadUserById should throw UsernameNotFoundException when not found`() {
        whenever(userRepository.findByIdWithRolesAndPermissions(999L))
            .thenReturn(null)

        assertThrows<UsernameNotFoundException> {
            customUserDetailsService.loadUserById(999L)
        }
    }
}
