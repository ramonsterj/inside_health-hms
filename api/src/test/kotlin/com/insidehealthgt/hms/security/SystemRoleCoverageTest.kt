package com.insidehealthgt.hms.security

import com.insidehealthgt.hms.TestcontainersConfiguration
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import kotlin.test.assertTrue

/**
 * The rename/typo safety net for [SystemRole]. Mirrors `I18nReferenceDataCoverageTest`: after
 * Flyway migrates the Testcontainers database, it reads the live role/permission rows and asserts
 * the Kotlin constants line up with the DB.
 *
 * Two invariants:
 *  1. [SystemRole.ALL] == `roles WHERE is_system = TRUE`, both directions. Catches a future code
 *     rename done in a migration without the constant (or vice-versa) and a typo'd constant.
 *  2. ADMINISTRADOR holds every permission. This is the invariant that makes the permission-cleanup
 *     (services gating on the permission rather than `hasRole("ADMINISTRADOR")`) behavior-preserving
 *     and that the frontend relies on after removing its admin bypass.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(TestcontainersConfiguration::class)
class SystemRoleCoverageTest {

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Test
    fun `SystemRole ALL matches the seeded system roles in the database`() {
        val dbCodes = distinct("SELECT code FROM roles WHERE is_system = TRUE AND deleted_at IS NULL")
        val missing = (dbCodes - SystemRole.ALL).sorted()
        val stale = (SystemRole.ALL - dbCodes).sorted()
        assertTrue(
            missing.isEmpty() && stale.isEmpty(),
            "SystemRole.ALL drifted from the system roles in the DB.\n" +
                "  Missing (in DB, not in SystemRole.ALL): ${missing.ifEmpty { listOf("(none)") }}\n" +
                "  Stale (in SystemRole.ALL, not in DB): ${stale.ifEmpty { listOf("(none)") }}",
        )
    }

    @Test
    fun `ADMINISTRADOR holds every permission`() {
        val allPermissions = distinct("SELECT code FROM permissions WHERE deleted_at IS NULL")
        val adminPermissions = distinct(
            """
            SELECT p.code
            FROM permissions p
            JOIN role_permissions rp ON rp.permission_id = p.id AND rp.deleted_at IS NULL
            JOIN roles r ON r.id = rp.role_id AND r.deleted_at IS NULL
            WHERE r.code = ? AND p.deleted_at IS NULL
            """.trimIndent(),
            SystemRole.ADMINISTRADOR,
        )
        val missing = (allPermissions - adminPermissions).sorted()
        assertTrue(
            missing.isEmpty(),
            "ADMINISTRADOR is expected to hold every permission, but is missing grants for: $missing.\n" +
                "  Grant them to ADMINISTRADOR in a new migration " +
                "(idempotent INSERT…SELECT … ON CONFLICT DO NOTHING, like the V005…V131 grant migrations).",
        )
    }

    private fun distinct(sql: String, vararg args: Any): Set<String> =
        jdbcTemplate.queryForList(sql, String::class.java, *args).filterNotNull().toSet()
}
