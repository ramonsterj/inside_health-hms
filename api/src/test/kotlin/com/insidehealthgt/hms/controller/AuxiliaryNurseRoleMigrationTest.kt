package com.insidehealthgt.hms.controller

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Verifies the V117 migration: the AUXILIAR_ENFERMERIA role exists (AC-1) and its
 * granted permission set matches the spec exactly (AC-2) — both the present
 * permissions and the deliberate exclusions. Spec: docs/features/nursing-roles-split.md.
 */
class AuxiliaryNurseRoleMigrationTest : AbstractIntegrationTest() {

    @Test
    fun `AUXILIAR_ENFERMERIA role exists after migration`() {
        val count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM roles WHERE code = 'AUXILIAR_ENFERMERIA'",
            Int::class.java,
        )
        assertEquals(1, count)
    }

    @Test
    fun `AUXILIAR_ENFERMERIA has exactly the expected permission grants`() {
        val granted = jdbcTemplate.queryForList(
            """
            SELECT p.code
            FROM role_permissions rp
            JOIN roles r ON r.id = rp.role_id
            JOIN permissions p ON p.id = rp.permission_id
            WHERE r.code = 'AUXILIAR_ENFERMERIA'
            """.trimIndent(),
            String::class.java,
        ).toSet()

        val expected = setOf(
            "nursing-note:read", "nursing-note:create",
            "vital-sign:read", "vital-sign:create",
            "medication-administration:read",
            "medical-order:read",
            "progress-note:read",
            "clinical-history:read",
            "patient:read",
            "admission:read",
            "room:occupancy-view",
            // V119 warehouse grants: read its (ENFERMERIA) warehouse + read transfer history.
            "warehouse:read",
            "warehouse-transfer:read",
        )

        assertEquals(expected, granted, "AUXILIAR_ENFERMERIA grant set must match AC-2 exactly")

        // Explicit exclusions (AC-2): the three guarded actions, progress-note authoring, and
        // admission:update (which gates discharge / admission edit / consulting-physician changes —
        // out of the notes/vitals-only scope).
        val forbidden = setOf(
            "medication-administration:create",
            "medical-order:mark-in-progress",
            "medical-order:upload-document",
            "progress-note:create",
            "admission:update",
        )
        forbidden.forEach { code ->
            assertTrue(code !in granted, "AUXILIAR_ENFERMERIA must NOT hold $code")
        }
    }
}
