package com.insidehealthgt.hms.repository

import com.insidehealthgt.hms.TestcontainersConfiguration
import com.insidehealthgt.hms.config.JpaConfig
import com.insidehealthgt.hms.entity.PasswordResetToken
import com.insidehealthgt.hms.entity.User
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@DataJpaTest
@Import(TestcontainersConfiguration::class, JpaConfig::class)
class PasswordResetTokenRepositoryTest {

    @Autowired
    private lateinit var passwordResetTokenRepository: PasswordResetTokenRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    private lateinit var testUser: User

    @BeforeEach
    fun setUp() {
        // Clean all data in FK dependency order using jdbcTemplate
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

        testUser = userRepository.save(
            User(
                username = "testuser",
                email = "test@example.com",
                passwordHash = "hashedpassword",
            ),
        )
    }

    @Test
    fun `findByToken should return token when exists`() {
        val resetToken = passwordResetTokenRepository.save(
            PasswordResetToken(
                token = "test-token-123",
                expiresAt = LocalDateTime.now().plusHours(1),
                user = testUser,
            ),
        )

        val found = passwordResetTokenRepository.findByToken("test-token-123")

        assertNotNull(found)
        assertEquals(resetToken.id, found.id)
        assertEquals("test-token-123", found.token)
    }

    @Test
    fun `findByToken should return null when token does not exist`() {
        val found = passwordResetTokenRepository.findByToken("nonexistent")

        assertNull(found)
    }

    @Test
    fun `deleteByUser should remove all tokens for user`() {
        passwordResetTokenRepository.save(
            PasswordResetToken(
                token = "token-1",
                expiresAt = LocalDateTime.now().plusHours(1),
                user = testUser,
            ),
        )
        passwordResetTokenRepository.save(
            PasswordResetToken(
                token = "token-2",
                expiresAt = LocalDateTime.now().plusHours(1),
                user = testUser,
            ),
        )

        assertEquals(2, passwordResetTokenRepository.count())

        passwordResetTokenRepository.deleteByUser(testUser)

        assertEquals(0, passwordResetTokenRepository.count())
    }

    @Test
    fun `deleteByExpiresAtBefore should remove expired tokens`() {
        passwordResetTokenRepository.save(
            PasswordResetToken(
                token = "expired-token",
                expiresAt = LocalDateTime.now().minusHours(1),
                user = testUser,
            ),
        )
        passwordResetTokenRepository.save(
            PasswordResetToken(
                token = "valid-token",
                expiresAt = LocalDateTime.now().plusHours(1),
                user = testUser,
            ),
        )

        assertEquals(2, passwordResetTokenRepository.count())

        passwordResetTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now())

        assertEquals(1, passwordResetTokenRepository.count())
        assertNotNull(passwordResetTokenRepository.findByToken("valid-token"))
        assertNull(passwordResetTokenRepository.findByToken("expired-token"))
    }

    @Test
    fun `should auto-populate audit fields on save`() {
        val saved = passwordResetTokenRepository.save(
            PasswordResetToken(
                token = "audit-test-token",
                expiresAt = LocalDateTime.now().plusHours(1),
                user = testUser,
            ),
        )

        assertNotNull(saved.id)
        assertNotNull(saved.createdAt)
        assertNotNull(saved.updatedAt)
    }

    @Test
    fun `soft deleted tokens should be excluded from queries`() {
        val token = passwordResetTokenRepository.save(
            PasswordResetToken(
                token = "soft-delete-test",
                expiresAt = LocalDateTime.now().plusHours(1),
                user = testUser,
            ),
        )

        token.deletedAt = LocalDateTime.now()
        passwordResetTokenRepository.save(token)

        val found = passwordResetTokenRepository.findByToken("soft-delete-test")
        assertNull(found)
    }
}
