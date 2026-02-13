package com.insidehealthgt.hms.repository

import com.insidehealthgt.hms.TestcontainersConfiguration
import com.insidehealthgt.hms.config.JpaConfig
import com.insidehealthgt.hms.entity.PhoneType
import com.insidehealthgt.hms.entity.User
import com.insidehealthgt.hms.entity.UserPhoneNumber
import com.insidehealthgt.hms.entity.UserStatus
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageRequest
import org.springframework.jdbc.core.JdbcTemplate
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

    @Autowired
    private lateinit var roleRepository: RoleRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    private lateinit var testUser: User

    @BeforeEach
    fun setUp() {
        // Clean all data in FK dependency order using jdbcTemplate.
        // Must include the full chain because integration tests sharing
        // the same Testcontainers DB may leave behind data with FK references to users.
        jdbcTemplate.execute("DELETE FROM medication_administrations")
        jdbcTemplate.execute("DELETE FROM patient_charges")
        jdbcTemplate.execute("DELETE FROM invoices")
        jdbcTemplate.execute("DELETE FROM inventory_movements")
        jdbcTemplate.execute("DELETE FROM inventory_items")
        jdbcTemplate.execute("DELETE FROM inventory_categories WHERE created_by IS NOT NULL")
        jdbcTemplate.execute("DELETE FROM triage_codes WHERE created_by IS NOT NULL")
        jdbcTemplate.execute("DELETE FROM psychotherapy_categories WHERE created_by IS NOT NULL")
        jdbcTemplate.execute("DELETE FROM nursing_notes")
        jdbcTemplate.execute("DELETE FROM vital_signs")
        jdbcTemplate.execute("DELETE FROM psychotherapy_activities")
        jdbcTemplate.execute("DELETE FROM medical_orders")
        jdbcTemplate.execute("DELETE FROM progress_notes")
        jdbcTemplate.execute("DELETE FROM clinical_histories")
        jdbcTemplate.execute("DELETE FROM admission_consulting_physicians")
        jdbcTemplate.execute("DELETE FROM admission_consent_documents")
        jdbcTemplate.execute("DELETE FROM admission_documents")
        jdbcTemplate.execute("DELETE FROM admissions")
        jdbcTemplate.execute("DELETE FROM emergency_contacts")
        jdbcTemplate.execute("DELETE FROM patient_id_documents")
        jdbcTemplate.execute("DELETE FROM patients")
        jdbcTemplate.execute("DELETE FROM rooms")
        jdbcTemplate.execute("DELETE FROM password_reset_tokens")
        jdbcTemplate.execute("DELETE FROM refresh_tokens")
        jdbcTemplate.execute("DELETE FROM audit_logs")
        jdbcTemplate.execute("DELETE FROM user_phone_numbers")
        jdbcTemplate.execute("DELETE FROM user_roles")
        jdbcTemplate.execute("DELETE FROM users")

        testUser = userRepository.saveAndFlush(
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

    // ============ findByEmail ============

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

    // ============ existsByEmail ============

    @Test
    fun `existsByEmail should return true when email exists`() {
        assertTrue(userRepository.existsByEmail("test@example.com"))
    }

    @Test
    fun `existsByEmail should return false when email does not exist`() {
        assertFalse(userRepository.existsByEmail("nonexistent@example.com"))
    }

    // ============ findByUsername / existsByUsername ============

    @Test
    fun `findByUsername should return user when username exists`() {
        val found = userRepository.findByUsername("testuser")

        assertNotNull(found)
        assertEquals("testuser", found.username)
        assertEquals("test@example.com", found.email)
    }

    @Test
    fun `findByUsername should return null when username does not exist`() {
        assertNull(userRepository.findByUsername("nonexistent"))
    }

    @Test
    fun `existsByUsername should return true when username exists`() {
        assertTrue(userRepository.existsByUsername("testuser"))
    }

    @Test
    fun `existsByUsername should return false when username does not exist`() {
        assertFalse(userRepository.existsByUsername("nonexistent"))
    }

    // ============ findByEmailOrUsername ============

    @Test
    fun `findByEmailOrUsername should find by email`() {
        val found = userRepository.findByEmailOrUsername("test@example.com", "nomatch")

        assertNotNull(found)
        assertEquals("testuser", found.username)
    }

    @Test
    fun `findByEmailOrUsername should find by username`() {
        val found = userRepository.findByEmailOrUsername("nomatch@example.com", "testuser")

        assertNotNull(found)
        assertEquals("test@example.com", found.email)
    }

    // ============ Soft deletes ============

    @Test
    fun `soft deleted users should be excluded from queries`() {
        testUser.deletedAt = LocalDateTime.now()
        userRepository.saveAndFlush(testUser)

        assertNull(userRepository.findByEmail("test@example.com"))
        assertFalse(userRepository.existsByEmail("test@example.com"))
    }

    @Test
    fun `should auto-populate audit fields on save`() {
        val newUser = userRepository.saveAndFlush(
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

    // ============ findByIdWithRolesAndPermissions ============

    @Test
    fun `findByIdWithRolesAndPermissions should eagerly load roles and permissions`() {
        val adminRole = roleRepository.findByCode("ADMIN")!!
        testUser.roles.add(adminRole)
        userRepository.saveAndFlush(testUser)
        entityManager.clear()

        val found = userRepository.findByIdWithRolesAndPermissions(testUser.id!!)

        assertNotNull(found)
        assertTrue(found.roles.isNotEmpty())
        assertEquals("ADMIN", found.roles.first().code)
        assertTrue(found.roles.first().permissions.isNotEmpty())
    }

    @Test
    fun `findByIdWithRolesAndPermissions should return null for non-existent id`() {
        assertNull(userRepository.findByIdWithRolesAndPermissions(999999L))
    }

    // ============ findByIdentifierWithRolesAndPermissions ============

    @Test
    fun `findByIdentifierWithRolesAndPermissions should find by email with roles`() {
        val adminRole = roleRepository.findByCode("ADMIN")!!
        testUser.roles.add(adminRole)
        userRepository.saveAndFlush(testUser)
        entityManager.clear()

        val found = userRepository.findByIdentifierWithRolesAndPermissions("test@example.com")

        assertNotNull(found)
        assertEquals("testuser", found.username)
        assertTrue(found.roles.isNotEmpty())
    }

    @Test
    fun `findByIdentifierWithRolesAndPermissions should find by username with roles`() {
        val adminRole = roleRepository.findByCode("ADMIN")!!
        testUser.roles.add(adminRole)
        userRepository.saveAndFlush(testUser)
        entityManager.clear()

        val found = userRepository.findByIdentifierWithRolesAndPermissions("testuser")

        assertNotNull(found)
        assertEquals("test@example.com", found.email)
        assertTrue(found.roles.isNotEmpty())
    }

    @Test
    fun `findByIdentifierWithRolesAndPermissions should return null for unknown identifier`() {
        assertNull(userRepository.findByIdentifierWithRolesAndPermissions("unknown@example.com"))
    }

    // ============ findWithFilters (native query) ============

    @Test
    fun `findWithFilters should filter by status only`() {
        userRepository.saveAndFlush(
            User(
                username = "inactive",
                email = "inactive@example.com",
                passwordHash = "hash",
                status = UserStatus.INACTIVE,
            ),
        )

        val page = userRepository.findWithFilters("ACTIVE", null, null, PageRequest.of(0, 10))

        assertTrue(page.content.all { it.status == UserStatus.ACTIVE })
        assertEquals(1, page.totalElements)
    }

    @Test
    fun `findWithFilters should filter by roleCode only`() {
        val doctorRole = roleRepository.findByCode("DOCTOR")!!
        testUser.roles.add(doctorRole)
        userRepository.saveAndFlush(testUser)
        entityManager.flush()

        val page = userRepository.findWithFilters(null, "DOCTOR", null, PageRequest.of(0, 10))

        assertEquals(1, page.totalElements)
        assertEquals("testuser", page.content[0].username)
    }

    @Test
    fun `findWithFilters should filter by search term`() {
        userRepository.saveAndFlush(
            User(
                username = "other",
                email = "other@example.com",
                passwordHash = "hash",
                firstName = "Other",
                lastName = "Person",
            ),
        )

        val page = userRepository.findWithFilters(null, null, "Test", PageRequest.of(0, 10))

        assertEquals(1, page.totalElements)
        assertEquals("testuser", page.content[0].username)
    }

    @Test
    fun `findWithFilters should combine all filters`() {
        val doctorRole = roleRepository.findByCode("DOCTOR")!!
        testUser.roles.add(doctorRole)
        userRepository.saveAndFlush(testUser)

        val otherUser = User(
            username = "otherdoctor",
            email = "other@example.com",
            passwordHash = "hash",
            firstName = "Other",
            status = UserStatus.ACTIVE,
        )
        otherUser.roles.add(doctorRole)
        userRepository.saveAndFlush(otherUser)
        entityManager.flush()

        val page = userRepository.findWithFilters("ACTIVE", "DOCTOR", "Test", PageRequest.of(0, 10))

        assertEquals(1, page.totalElements)
        assertEquals("testuser", page.content[0].username)
    }

    @Test
    fun `findWithFilters should return all when no filters applied`() {
        userRepository.saveAndFlush(
            User(username = "second", email = "second@example.com", passwordHash = "hash"),
        )
        entityManager.flush()

        val page = userRepository.findWithFilters(null, null, null, PageRequest.of(0, 10))

        assertEquals(2, page.totalElements)
    }

    // ============ findAllDeleted / findDeletedById (native queries) ============

    @Test
    fun `findAllDeleted should return only soft-deleted users`() {
        testUser.deletedAt = LocalDateTime.now()
        userRepository.saveAndFlush(testUser)
        entityManager.flush()

        val page = userRepository.findAllDeleted(PageRequest.of(0, 10))

        assertEquals(1, page.totalElements)
        assertEquals("testuser", page.content[0].username)
    }

    @Test
    fun `findAllDeleted should return empty when no deleted users exist`() {
        entityManager.flush()

        val page = userRepository.findAllDeleted(PageRequest.of(0, 10))

        assertEquals(0, page.totalElements)
    }

    @Test
    fun `findDeletedById should return soft-deleted user`() {
        testUser.deletedAt = LocalDateTime.now()
        userRepository.saveAndFlush(testUser)
        entityManager.flush()

        val found = userRepository.findDeletedById(testUser.id!!)

        assertNotNull(found)
        assertEquals("testuser", found.username)
        assertNotNull(found.deletedAt)
    }

    @Test
    fun `findDeletedById should return null for active user`() {
        entityManager.flush()

        assertNull(userRepository.findDeletedById(testUser.id!!))
    }

    // ============ findByIdWithPhoneNumbers ============

    @Test
    fun `findByIdWithPhoneNumbers should eagerly load phone numbers`() {
        testUser.addPhoneNumber(
            UserPhoneNumber(phoneNumber = "555-1234", phoneType = PhoneType.MOBILE, isPrimary = true),
        )
        userRepository.saveAndFlush(testUser)
        entityManager.clear()

        val found = userRepository.findByIdWithPhoneNumbers(testUser.id!!)

        assertNotNull(found)
        assertEquals(1, found.phoneNumbers.size)
        assertEquals("555-1234", found.phoneNumbers[0].phoneNumber)
    }

    // ============ findByRoleCode ============

    @Test
    fun `findByRoleCode should return users with the specified role`() {
        val doctorRole = roleRepository.findByCode("DOCTOR")!!
        testUser.roles.add(doctorRole)
        userRepository.saveAndFlush(testUser)

        val secondDoctor = User(
            username = "doctor2",
            email = "doctor2@example.com",
            passwordHash = "hash",
            firstName = "Doc",
            lastName = "Two",
        )
        secondDoctor.roles.add(doctorRole)
        userRepository.saveAndFlush(secondDoctor)

        val doctors = userRepository.findByRoleCode("DOCTOR")

        assertEquals(2, doctors.size)
    }

    @Test
    fun `findByRoleCode should return empty list when no users have the role`() {
        assertTrue(userRepository.findByRoleCode("PSYCHOLOGIST").isEmpty())
    }
}
