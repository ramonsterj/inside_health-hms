package com.insidehealthgt.hms.i18n

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.insidehealthgt.hms.TestcontainersConfiguration
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import java.io.File
import kotlin.test.assertTrue

/**
 * Backend half of the reference-data i18n guard (see CLAUDE.md → i18n /
 * Reference-Data Labels and docs/architecture/I18N.md).
 *
 * The DB is the source of truth for the stable codes (role codes, permission
 * codes/resources, warehouse codes live only in SQL — there is no Kotlin enum).
 * After Flyway migrates the Testcontainers database, this test reads the live
 * code sets and asserts that the web ES locale bundle has EXACTLY the required
 * i18n key for each — no missing keys (a new role/permission cannot ship without
 * a translation) and no stale keys (a removed code cannot leave a dangling one).
 *
 * Combined with the frontend guards — `catalogs.spec.ts` (catalog ⇄ bundle) and
 * `locales.parity.spec.ts` (en ⇄ es) — this closes the DB ↔ catalog ↔ i18n loop.
 *
 * The bundle is read from the sibling `web` module via a project-root-relative
 * path (precedent: `locales.parity.spec.ts` reads the backend `.properties`
 * bundles from the web module — this is the reverse cross-module read).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(TestcontainersConfiguration::class)
class I18nReferenceDataCoverageTest {

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    private val mapper = ObjectMapper()

    private val esBundle: JsonNode by lazy {
        mapper.readTree(locateRepoFile("web/src/i18n/locales/es.json"))
    }

    @Test
    fun `every system role has a roleNames and roleDescriptions key`() {
        // Only Flyway-seeded SYSTEM roles need keys; admin/test-created custom roles
        // (is_system = FALSE) fall back to their DB value per the standard.
        val codes = distinct("SELECT code FROM roles WHERE is_system = TRUE")
        assertKeysEqual("roleNames", keysOf(esBundle.path("roleNames")), codes)
        assertKeysEqual("roleDescriptions", keysOf(esBundle.path("roleDescriptions")), codes)
        codes.forEach {
            assertNonBlank("roleNames.$it", esBundle.path("roleNames").path(it))
            assertNonBlank("roleDescriptions.$it", esBundle.path("roleDescriptions").path(it))
        }
    }

    @Test
    fun `every permission has a name and description key`() {
        val codes = distinct("SELECT code FROM permissions WHERE created_by IS NULL")
        val permissions = esBundle.path("permissions")
        assertKeysEqual("permissions", keysOf(permissions), codes)
        codes.forEach {
            assertNonBlank("permissions.$it.name", permissions.path(it).path("name"))
            assertNonBlank("permissions.$it.description", permissions.path(it).path("description"))
        }
    }

    @Test
    fun `every permission resource has a permissionGroups label`() {
        val resources = distinct(
            "SELECT DISTINCT resource FROM permissions WHERE created_by IS NULL",
        )
        val groups = esBundle.path("roles").path("permissionGroups")
        assertKeysEqual("roles.permissionGroups", keysOf(groups), resources)
        resources.forEach { assertNonBlank("roles.permissionGroups.$it", groups.path(it)) }
    }

    @Test
    fun `every warehouse has a names and descriptions key`() {
        val codes = distinct("SELECT code FROM warehouses WHERE created_by IS NULL")
        val names = esBundle.path("warehouse").path("names")
        val descriptions = esBundle.path("warehouse").path("descriptions")
        assertKeysEqual("warehouse.names", keysOf(names), codes)
        assertKeysEqual("warehouse.descriptions", keysOf(descriptions), codes)
        codes.forEach {
            assertNonBlank("warehouse.names.$it", names.path(it))
            assertNonBlank("warehouse.descriptions.$it", descriptions.path(it))
        }
    }

    @Test
    fun `every document type has a name and description key`() {
        val codes = distinct("SELECT code FROM document_types WHERE created_by IS NULL")
        val names = esBundle.path("document").path("types")
        val descriptions = esBundle.path("document").path("typeDescriptions")
        assertKeysEqual("document.types", keysOf(names), codes)
        assertKeysEqual("document.typeDescriptions", keysOf(descriptions), codes)
        codes.forEach {
            assertNonBlank("document.types.$it", names.path(it))
            assertNonBlank("document.typeDescriptions.$it", descriptions.path(it))
        }
    }

    @Test
    fun `every triage code has a label key`() {
        val codes = distinct("SELECT code FROM triage_codes WHERE created_by IS NULL")
        val labels = esBundle.path("triageCode").path("codes")
        assertKeysEqual("triageCode.codes", keysOf(labels), codes)
        codes.forEach { assertNonBlank("triageCode.codes.$it", labels.path(it)) }
    }

    // ---- helpers ----

    private fun distinct(sql: String): Set<String> =
        jdbcTemplate.queryForList(sql, String::class.java).filterNotNull().toSet()

    private fun keysOf(node: JsonNode): Set<String> =
        if (node.isObject) node.fieldNames().asSequence().toSet() else emptySet()

    private fun assertKeysEqual(section: String, bundleKeys: Set<String>, dbCodes: Set<String>) {
        val missing = (dbCodes - bundleKeys).sorted()
        val stale = (bundleKeys - dbCodes).sorted()
        assertTrue(
            missing.isEmpty() && stale.isEmpty(),
            "i18n section '$section' drifted from the DB code set.\n" +
                "  Missing keys (in DB, not in es.json): ${missing.ifEmpty { listOf("(none)") }}\n" +
                "  Stale keys (in es.json, not in DB): ${stale.ifEmpty { listOf("(none)") }}",
        )
    }

    private fun assertNonBlank(path: String, node: JsonNode) {
        assertTrue(node.isTextual && node.asText().isNotBlank(), "i18n key '$path' is missing or blank")
    }

    private fun locateRepoFile(relativePath: String): File {
        var dir: File? = File(System.getProperty("user.dir")).absoluteFile
        while (dir != null) {
            val candidate = File(dir, relativePath)
            if (candidate.isFile) return candidate
            dir = dir.parentFile
        }
        error("Could not locate '$relativePath' walking up from ${System.getProperty("user.dir")}")
    }
}
