package com.insidehealthgt.hms.repository

import com.insidehealthgt.hms.TestcontainersConfiguration
import com.insidehealthgt.hms.config.JpaConfig
import com.insidehealthgt.hms.entity.User
import com.insidehealthgt.hms.entity.UserStatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@DataJpaTest
@Import(TestcontainersConfiguration::class, JpaConfig::class)
class UserRepositoryTest {

    @Autowired
    private lateinit var userRepository: UserRepository

    private lateinit var testUser: User

    @BeforeEach
    fun setUp() {
        userRepository.deleteAll()
        testUser = userRepository.save(
            User(
                username = "testuser",
                email = "test@example.com",
                passwordHash = "hashedpassword123",
                firstName = "Test",
                lastName = "User",
                status = UserStatus.ACTIVE,
            ),
        )
    }

    @Test
    fun `findByEmail should return user when email exists`() {
        val found = userRepository.findByEmail("test@example.com")

        assertNotNull(found)
        assertEquals("test@example.com", found.email)
        assertEquals("Test", found.firstName)
    }

    @Test
    fun `findByEmail should return null when email does not exist`() {
        val found = userRepository.findByEmail("nonexistent@example.com")

        assertNull(found)
    }

    @Test
    fun `existsByEmail should return true when email exists`() {
        val exists = userRepository.existsByEmail("test@example.com")

        assertTrue(exists)
    }

    @Test
    fun `existsByEmail should return false when email does not exist`() {
        val exists = userRepository.existsByEmail("nonexistent@example.com")

        assertFalse(exists)
    }

    @Test
    fun `soft deleted users should be excluded from queries`() {
        // Soft delete the user
        testUser.deletedAt = LocalDateTime.now()
        userRepository.save(testUser)

        // Should not find the soft-deleted user
        val found = userRepository.findByEmail("test@example.com")
        assertNull(found)

        val exists = userRepository.existsByEmail("test@example.com")
        assertFalse(exists)
    }

    @Test
    fun `should auto-populate audit fields on save`() {
        val newUser = userRepository.save(
            User(
                username = "newuser",
                email = "new@example.com",
                passwordHash = "hashedpassword",
            ),
        )

        assertNotNull(newUser.id)
        assertNotNull(newUser.createdAt)
        assertNotNull(newUser.updatedAt)
    }
}
