package com.insidehealthgt.hms.service

import com.insidehealthgt.hms.dto.request.ChangePasswordRequest
import com.insidehealthgt.hms.dto.request.CreateUserRequest
import com.insidehealthgt.hms.dto.request.PhoneNumberRequest
import com.insidehealthgt.hms.dto.request.UpdateUserRequest
import com.insidehealthgt.hms.entity.PhoneType
import com.insidehealthgt.hms.entity.Role
import com.insidehealthgt.hms.entity.User
import com.insidehealthgt.hms.entity.UserStatus
import com.insidehealthgt.hms.exception.BadRequestException
import com.insidehealthgt.hms.exception.ConflictException
import com.insidehealthgt.hms.exception.ResourceNotFoundException
import com.insidehealthgt.hms.repository.RoleRepository
import com.insidehealthgt.hms.repository.UserRepository
import com.insidehealthgt.hms.security.CustomUserDetails
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UserServiceTest {

    private lateinit var userRepository: UserRepository
    private lateinit var roleRepository: RoleRepository
    private lateinit var passwordEncoder: PasswordEncoder
    private lateinit var messageService: MessageService
    private lateinit var userService: UserService

    private lateinit var testUser: User
    private lateinit var adminRole: Role

    @BeforeEach
    fun setUp() {
        userRepository = mock()
        roleRepository = mock()
        passwordEncoder = mock()
        messageService = mock()

        userService = UserService(
            userRepository,
            roleRepository,
            passwordEncoder,
            messageService,
        )

        adminRole = Role(
            code = "ADMIN",
            name = "Administrator",
        ).apply { id = 1L }

        testUser = User(
            username = "testuser",
            email = "test@example.com",
            passwordHash = "hashedpassword",
            firstName = "Test",
            lastName = "User",
            status = UserStatus.ACTIVE,
        ).apply { id = 1L }

        SecurityContextHolder.clearContext()
    }

    private fun setUpAuthentication(user: User) {
        val securityContext: SecurityContext = mock()
        val authentication: Authentication = mock()
        val userDetails = CustomUserDetails(user)
        whenever(securityContext.authentication).thenReturn(authentication)
        whenever(authentication.principal).thenReturn(userDetails)
        SecurityContextHolder.setContext(securityContext)
    }

    // === createUser tests ===

    @Test
    fun `createUser should create user with roles and phone numbers`() {
        val request = CreateUserRequest(
            username = "newuser",
            email = "new@example.com",
            password = "password123",
            firstName = "New",
            lastName = "User",
            roleCodes = listOf("ADMIN"),
            phoneNumbers = listOf(
                PhoneNumberRequest(phoneNumber = "12345678", phoneType = PhoneType.MOBILE, isPrimary = true),
            ),
        )

        whenever(userRepository.existsByEmail("new@example.com")).thenReturn(false)
        whenever(userRepository.existsByUsername("newuser")).thenReturn(false)
        whenever(passwordEncoder.encode("password123")).thenReturn("encodedpassword")
        whenever(roleRepository.findAllByCodeIn(listOf("ADMIN"))).thenReturn(listOf(adminRole))
        whenever(userRepository.save(any<User>())).thenAnswer { invocation ->
            (invocation.arguments[0] as User).apply {
                id = 2L
                phoneNumbers.forEachIndexed { index, phone -> phone.id = (index + 1).toLong() }
            }
        }

        val result = userService.createUser(request)

        assertEquals("newuser", result.username)
        assertEquals("new@example.com", result.email)
        verify(userRepository).save(any<User>())
    }

    @Test
    fun `createUser should throw ConflictException for duplicate email`() {
        val request = CreateUserRequest(
            username = "newuser",
            email = "existing@example.com",
            password = "password123",
            phoneNumbers = listOf(
                PhoneNumberRequest(phoneNumber = "12345678", phoneType = PhoneType.MOBILE, isPrimary = true),
            ),
        )

        whenever(userRepository.existsByEmail("existing@example.com")).thenReturn(true)

        val exception = assertThrows<ConflictException> {
            userService.createUser(request)
        }
        assertTrue(exception.message!!.contains("Email"))
    }

    @Test
    fun `createUser should throw ConflictException for duplicate username`() {
        val request = CreateUserRequest(
            username = "existinguser",
            email = "new@example.com",
            password = "password123",
            phoneNumbers = listOf(
                PhoneNumberRequest(phoneNumber = "12345678", phoneType = PhoneType.MOBILE, isPrimary = true),
            ),
        )

        whenever(userRepository.existsByEmail("new@example.com")).thenReturn(false)
        whenever(userRepository.existsByUsername("existinguser")).thenReturn(true)

        val exception = assertThrows<ConflictException> {
            userService.createUser(request)
        }
        assertTrue(exception.message!!.contains("Username"))
    }

    // === updateProfile tests ===

    @Test
    fun `updateProfile should update first and last name`() {
        setUpAuthentication(testUser)
        whenever(userRepository.findByIdWithRolesAndPermissions(1L)).thenReturn(testUser)
        whenever(userRepository.save(any<User>())).thenAnswer { it.arguments[0] }

        val request = UpdateUserRequest(firstName = "Updated", lastName = "Name")
        val result = userService.updateProfile(request)

        assertEquals("Updated", result.firstName)
        assertEquals("Name", result.lastName)
    }

    // === changePassword tests ===

    @Test
    fun `changePassword should update password when current password is correct`() {
        setUpAuthentication(testUser)
        whenever(userRepository.findById(1L)).thenReturn(Optional.of(testUser))
        whenever(passwordEncoder.matches("currentpass", "hashedpassword")).thenReturn(true)
        whenever(passwordEncoder.encode("newpassword123")).thenReturn("newhashedpassword")

        userService.changePassword(
            ChangePasswordRequest(currentPassword = "currentpass", newPassword = "newpassword123"),
        )

        assertEquals("newhashedpassword", testUser.passwordHash)
        assertFalse(testUser.mustChangePassword)
        verify(userRepository).save(testUser)
    }

    @Test
    fun `changePassword should throw BadRequestException when current password is wrong`() {
        setUpAuthentication(testUser)
        whenever(userRepository.findById(1L)).thenReturn(Optional.of(testUser))
        whenever(passwordEncoder.matches("wrongpass", "hashedpassword")).thenReturn(false)

        assertThrows<BadRequestException> {
            userService.changePassword(
                ChangePasswordRequest(currentPassword = "wrongpass", newPassword = "newpassword123"),
            )
        }
    }

    // === softDelete tests ===

    @Test
    fun `softDelete should set status to DELETED and deletedAt`() {
        whenever(userRepository.findById(1L)).thenReturn(Optional.of(testUser))
        whenever(userRepository.save(any<User>())).thenAnswer { it.arguments[0] }

        userService.softDelete(1L)

        assertEquals(UserStatus.DELETED, testUser.status)
        assertTrue(testUser.deletedAt != null)
        verify(userRepository).save(testUser)
    }

    // === restore tests ===

    @Test
    fun `restore should set status to ACTIVE and clear deletedAt`() {
        testUser.status = UserStatus.DELETED
        whenever(userRepository.findDeletedById(1L)).thenReturn(testUser)
        whenever(userRepository.save(any<User>())).thenAnswer { it.arguments[0] }

        val result = userService.restore(1L)

        assertEquals(UserStatus.ACTIVE, testUser.status)
        assertEquals(null, testUser.deletedAt)
        assertEquals("testuser", result.username)
    }

    @Test
    fun `restore should throw ResourceNotFoundException when deleted user not found`() {
        whenever(userRepository.findDeletedById(999L)).thenReturn(null)

        assertThrows<ResourceNotFoundException> {
            userService.restore(999L)
        }
    }

    // === assignRoles tests ===

    @Test
    fun `assignRoles should replace user roles`() {
        val doctorRole = Role(code = "DOCTOR", name = "Doctor").apply { id = 2L }
        testUser.roles.add(adminRole)

        whenever(userRepository.findByIdWithRolesAndPermissions(1L)).thenReturn(testUser)
        whenever(roleRepository.findAllByCodeIn(listOf("DOCTOR"))).thenReturn(listOf(doctorRole))
        whenever(userRepository.save(any<User>())).thenAnswer { it.arguments[0] }

        val result = userService.assignRoles(1L, listOf("DOCTOR"))

        assertEquals(listOf("DOCTOR"), result.roles)
    }

    @Test
    fun `assignRoles should throw BadRequestException for invalid role codes`() {
        whenever(userRepository.findByIdWithRolesAndPermissions(1L)).thenReturn(testUser)
        whenever(roleRepository.findAllByCodeIn(listOf("NONEXISTENT"))).thenReturn(emptyList())

        val exception = assertThrows<BadRequestException> {
            userService.assignRoles(1L, listOf("NONEXISTENT"))
        }
        assertTrue(exception.message!!.contains("Invalid role codes"))
    }

    // === updateLocalePreference tests ===

    @Test
    fun `updateLocalePreference should update locale for supported locale`() {
        setUpAuthentication(testUser)
        whenever(userRepository.findById(1L)).thenReturn(Optional.of(testUser))
        whenever(userRepository.save(any<User>())).thenAnswer { it.arguments[0] }

        userService.updateLocalePreference("es")

        assertEquals("es", testUser.localePreference)
        verify(userRepository).save(testUser)
    }

    @Test
    fun `updateLocalePreference should throw BadRequestException for unsupported locale`() {
        setUpAuthentication(testUser)
        whenever(messageService.errorLocaleUnsupported(any(), any())).thenReturn("Unsupported locale: fr")

        assertThrows<BadRequestException> {
            userService.updateLocalePreference("fr")
        }
    }

    // === findAll tests ===

    @Test
    fun `findAll with no filters should call findAll on repository`() {
        val pageable = PageRequest.of(0, 10)
        whenever(userRepository.findAll(pageable)).thenReturn(Page.empty())

        userService.findAll(pageable)

        verify(userRepository).findAll(pageable)
    }

    @Test
    fun `findAll with filters should call findWithFilters on repository`() {
        val pageable = PageRequest.of(0, 10)
        whenever(userRepository.findWithFilters("ACTIVE", null, null, pageable))
            .thenReturn(Page.empty())

        userService.findAll(pageable, status = UserStatus.ACTIVE)

        verify(userRepository).findWithFilters("ACTIVE", null, null, pageable)
    }
}
