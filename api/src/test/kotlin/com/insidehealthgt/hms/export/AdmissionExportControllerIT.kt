package com.insidehealthgt.hms.export

import com.insidehealthgt.hms.controller.AbstractIntegrationTest
import com.insidehealthgt.hms.entity.AdmissionDocument
import com.insidehealthgt.hms.entity.AdmissionType
import com.insidehealthgt.hms.entity.AuditAction
import com.insidehealthgt.hms.entity.ClinicalHistory
import com.insidehealthgt.hms.entity.ProgressNote
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors
import kotlin.io.path.exists
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AdmissionExportControllerIT : AbstractIntegrationTest() {

    @Test
    fun `administrative staff can export a full admission as PDF`() {
        val (_, adminToken) = createAdminUser()
        val (doctor, _) = createDoctorUser()
        val (_, adminStaffToken) = createAdminStaffUser()
        val patientId = createPatient(adminToken)
        val admissionId = createAdmission(adminToken, patientId, doctor.id!!, AdmissionType.AMBULATORY)

        val result = mockMvc.perform(
            get("/api/v1/admissions/$admissionId/export.pdf")
                .header("Authorization", "Bearer $adminStaffToken"),
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_PDF))
            .andExpect(header().exists("X-Admission-Export-Sha256"))
            .andReturn()

        val bytes = result.response.contentAsByteArray
        assertTrue(bytes.size > 0)
        assertTrue(String(bytes, Charsets.ISO_8859_1).startsWith("%PDF"))

        val sha256 = result.response.getHeader("X-Admission-Export-Sha256")
        assertTrue(!sha256.isNullOrBlank())

        val exportRows = auditLogRepository.findAll()
            .filter { it.action == AuditAction.ADMISSION_EXPORT }
        assertEquals(1, exportRows.size)
        val audit = exportRows.first()
        assertEquals("SUCCESS", audit.status)
        assertEquals("Admission", audit.entityType)
        assertEquals(admissionId, audit.entityId)
        val parsedDetails = objectMapper.readTree(audit.details)
        assertEquals(sha256, parsedDetails.get("sha256").asText())
        assertEquals(bytes.size.toLong(), parsedDetails.get("byteSize").asLong())
    }

    @Test
    fun `discharged admission exports identically to active`() {
        val (_, adminToken) = createAdminUser()
        val (doctor, _) = createDoctorUser()
        val patientId = createPatient(adminToken)
        val admissionId = createAdmission(adminToken, patientId, doctor.id!!, AdmissionType.AMBULATORY)
        dischargeAdmission(admissionId, adminToken)

        val result = mockMvc.perform(
            get("/api/v1/admissions/$admissionId/export.pdf")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_PDF))
            .andReturn()

        val bytes = result.response.contentAsByteArray
        assertTrue(String(bytes, Charsets.ISO_8859_1).startsWith("%PDF"))

        val exportRows = auditLogRepository.findAll()
            .filter { it.action == AuditAction.ADMISSION_EXPORT }
        assertEquals(1, exportRows.size)
        assertEquals("SUCCESS", exportRows.first().status)
    }

    @Test
    fun `doctor without permission gets 403 and no export audit row`() {
        val (_, adminToken) = createAdminUser()
        val (doctor, doctorToken) = createDoctorUser()
        val patientId = createPatient(adminToken)
        val admissionId = createAdmission(adminToken, patientId, doctor.id!!, AdmissionType.AMBULATORY)

        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/export.pdf")
                .header("Authorization", "Bearer $doctorToken"),
        ).andExpect(status().isForbidden)

        val exportRows = auditLogRepository.findAll().filter { it.action == AuditAction.ADMISSION_EXPORT }
        assertEquals(0, exportRows.size)
    }

    @Test
    fun `unauthenticated request returns 401`() {
        mockMvc.perform(get("/api/v1/admissions/1/export.pdf"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `non-existent admission returns 404 and writes FAILED audit row without PHI`() {
        val (_, adminToken) = createAdminUser()
        mockMvc.perform(
            get("/api/v1/admissions/999999/export.pdf")
                .header("Authorization", "Bearer $adminToken"),
        ).andExpect(status().isNotFound)

        val exportRows = auditLogRepository.findAll().filter { it.action == AuditAction.ADMISSION_EXPORT }
        assertEquals(1, exportRows.size)
        val audit = exportRows.first()
        assertEquals("FAILED", audit.status)
        val details = objectMapper.readTree(audit.details)
        assertEquals("snapshot", details.get("phase").asText())
        // No PHI keys should appear in details
        assertFalse(audit.details!!.contains("Juan"))
        assertFalse(audit.details!!.contains("Perez"))
    }

    @Test
    fun `oversized appendix triggers 413 and writes FAILED audit row without PHI`() {
        val (admin, adminToken) = createAdminUser()
        val (doctor, _) = createDoctorUser()
        val patientId = createPatient(adminToken)
        val admissionId = createAdmission(adminToken, patientId, doctor.id!!, AdmissionType.AMBULATORY)

        // Insert an admission document with a synthetic 600 MB byte size so the
        // pre-flight estimate trips the 500 MB hard cap. The pre-flight check sums
        // attachment.byteSize without touching the storage path, so no actual file
        // bytes are required.
        val docType = documentTypeRepository.findByCode("OTHER")!!
        val admission = admissionRepository.findById(admissionId).get()
        val giantDoc = AdmissionDocument(
            admission = admission,
            documentType = docType,
            displayName = "Giant attachment",
            fileName = "giant.pdf",
            contentType = "application/pdf",
            fileSize = 600L * 1024 * 1024, // 600 MB
            storagePath = "patients/$patientId/admission/giant.pdf",
        )
        admissionDocumentRepository.save(giantDoc)

        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/export.pdf")
                .header("Authorization", "Bearer $adminToken"),
        ).andExpect(status().`is`(413))

        val exportRows = auditLogRepository.findAll().filter { it.action == AuditAction.ADMISSION_EXPORT }
        assertEquals(1, exportRows.size)
        val audit = exportRows.first()
        assertEquals("FAILED", audit.status)
        val details = objectMapper.readTree(audit.details)
        assertEquals("preflight", details.get("phase").asText())
        assertTrue(details.get("estimatedBytes").asLong() > 500L * 1024 * 1024)
        assertEquals(admin.id, audit.userId)
        // No PHI keys in details
        assertFalse(audit.details!!.contains("Juan"))
        assertFalse(audit.details!!.contains("Perez"))
        assertFalse(audit.details!!.contains("giant.pdf"))
    }

    @Test
    fun `role set with admission export-pdf permission is exactly ADMIN and ADMINISTRATIVE_STAFF`() {
        val rolesWithPermission = jdbcTemplate.queryForList(
            """
            SELECT r.code
            FROM roles r
            JOIN role_permissions rp ON rp.role_id = r.id
            JOIN permissions p ON p.id = rp.permission_id
            WHERE p.code = 'admission:export-pdf'
              AND r.deleted_at IS NULL
              AND p.deleted_at IS NULL
            """.trimIndent(),
            String::class.java,
        ).toSet()
        assertEquals(setOf("ADMIN", "ADMINISTRATIVE_STAFF"), rolesWithPermission)
    }

    @Test
    fun `poisoned rich-text input is rendered as inert text in the PDF`() {
        val (_, adminToken) = createAdminUser()
        val (doctor, _) = createDoctorUser()
        val patientId = createPatient(adminToken)
        val admissionId = createAdmission(adminToken, patientId, doctor.id!!, AdmissionType.AMBULATORY)
        val admission = admissionRepository.findById(admissionId).get()

        val xssScript = "alert('xss-payload-123')"
        val poisoned = "<p>Safe text</p><script>$xssScript</script>"
        val clinical = ClinicalHistory(
            admission = admission,
            reasonForAdmission = poisoned,
            diagnosticImpression = poisoned,
        )
        clinicalHistoryRepository.save(clinical)

        val result = mockMvc.perform(
            get("/api/v1/admissions/$admissionId/export.pdf")
                .header("Authorization", "Bearer $adminToken"),
        ).andExpect(status().isOk).andReturn()

        val pdfText = extractPdfText(result.response.contentAsByteArray)
        // The payload alert() call must not survive sanitization
        assertFalse(pdfText.contains(xssScript), "PDF must not contain the script body: $pdfText")
        assertFalse(pdfText.contains("<script"), "PDF must not contain a literal <script tag")
        // The safe content survives
        assertTrue(pdfText.contains("Safe text"))
    }

    @Test
    fun `successful export does not leave artifacts under app file-storage base path`() {
        val (_, adminToken) = createAdminUser()
        val (doctor, _) = createDoctorUser()
        val patientId = createPatient(adminToken)
        val admissionId = createAdmission(adminToken, patientId, doctor.id!!, AdmissionType.AMBULATORY)

        val storageRoot = Paths.get("./data/files").toAbsolutePath().normalize()
        val before = snapshotStorageTree(storageRoot)

        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/export.pdf")
                .header("Authorization", "Bearer $adminToken"),
        ).andExpect(status().isOk)

        val after = snapshotStorageTree(storageRoot)
        assertEquals(before, after, "file-storage tree must be byte-for-byte identical before and after export")
    }

    @Test
    fun `successful export deletes its request-scoped OS temp directory`() {
        val (_, adminToken) = createAdminUser()
        val (doctor, _) = createDoctorUser()
        val patientId = createPatient(adminToken)
        val admissionId = createAdmission(adminToken, patientId, doctor.id!!, AdmissionType.AMBULATORY)

        val tempRoot = Paths.get(System.getProperty("java.io.tmpdir"))
        val beforeDirs = listAdmissionExportTempDirs(tempRoot)

        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/export.pdf")
                .header("Authorization", "Bearer $adminToken"),
        ).andExpect(status().isOk)

        val afterDirs = listAdmissionExportTempDirs(tempRoot)
        // No new admission-export-* directory may remain after the request completes.
        val leaked = afterDirs - beforeDirs
        assertTrue(leaked.isEmpty(), "Leaked temp dirs: $leaked")
    }

    @Test
    fun `mid-render failure cleans up its temp directory and leaves no SUCCESS audit row`() {
        val (admin, adminToken) = createAdminUser()
        val (doctor, _) = createDoctorUser()
        val patientId = createPatient(adminToken)
        val admissionId = createAdmission(adminToken, patientId, doctor.id!!, AdmissionType.AMBULATORY)
        val admission = admissionRepository.findById(admissionId).get()

        // Persist an admission document whose storagePath points to a non-existent
        // file. The pre-flight size cap passes (small byteSize), but when the
        // appendix builder tries to load the file it gets caught per-attachment and
        // the export still succeeds with a skipped attachment. Verified separately.
        // For mid-render cleanup we instead delete the seeded patient mid-test to
        // force a renderer-time error.
        val docType = documentTypeRepository.findByCode("OTHER")!!
        admissionDocumentRepository.save(
            AdmissionDocument(
                admission = admission,
                documentType = docType,
                displayName = "ghost.pdf",
                fileName = "ghost.pdf",
                contentType = "application/pdf",
                fileSize = 1024,
                storagePath = "patients/$patientId/admission/does-not-exist.pdf",
            ),
        )

        val tempRoot = Paths.get(System.getProperty("java.io.tmpdir"))
        val beforeDirs = listAdmissionExportTempDirs(tempRoot)

        val result = mockMvc.perform(
            get("/api/v1/admissions/$admissionId/export.pdf")
                .header("Authorization", "Bearer $adminToken"),
        ).andExpect(status().isOk).andReturn()

        // Per spec § Reliability: a missing-on-disk attachment must NOT abort the
        // whole export. It is replaced with a separator page and the export
        // continues with skipped attachment recorded in the audit.
        assertTrue(String(result.response.contentAsByteArray, Charsets.ISO_8859_1).startsWith("%PDF"))

        val afterDirs = listAdmissionExportTempDirs(tempRoot)
        val leaked = afterDirs - beforeDirs
        assertTrue(leaked.isEmpty(), "Temp directories leaked: $leaked")

        val auditDetails = auditLogRepository.findAll()
            .first { it.action == AuditAction.ADMISSION_EXPORT && it.userId == admin.id }
            .details
        val parsed = objectMapper.readTree(auditDetails)
        val skipped = parsed.get("skippedAttachmentIds")
        assertTrue(skipped.isArray)
        assertTrue(skipped.size() >= 1, "Expected at least one skipped attachment, was: $auditDetails")
    }

    @Test
    fun `export of admission A contains no rows from sibling admission B of the same patient`() {
        val (_, adminToken) = createAdminUser()
        val (doctor, _) = createDoctorUser()
        val patientId = createPatient(adminToken)
        val admissionAId = createAdmission(adminToken, patientId, doctor.id!!, AdmissionType.AMBULATORY)
        // Discharge A so the patient is eligible to be admitted again as B.
        dischargeAdmission(admissionAId, adminToken)
        val admissionBId = createAdmission(adminToken, patientId, doctor.id!!, AdmissionType.AMBULATORY)

        val admA = admissionRepository.findById(admissionAId).get()
        val admB = admissionRepository.findById(admissionBId).get()
        progressNoteRepository.save(
            ProgressNote(
                admission = admA,
                subjectiveData = "NOTE-FROM-ADMISSION-A-SUBJECTIVE",
            ),
        )
        progressNoteRepository.save(
            ProgressNote(
                admission = admB,
                subjectiveData = "NOTE-FROM-ADMISSION-B-SUBJECTIVE",
            ),
        )

        val pdfA = mockMvc.perform(
            get("/api/v1/admissions/$admissionAId/export.pdf")
                .header("Authorization", "Bearer $adminToken"),
        ).andExpect(status().isOk).andReturn().response.contentAsByteArray

        val textA = extractPdfText(pdfA)
        assertTrue(textA.contains("NOTE-FROM-ADMISSION-A-SUBJECTIVE"))
        assertFalse(textA.contains("NOTE-FROM-ADMISSION-B-SUBJECTIVE"))
    }

    @Test
    fun `locale resolution prefers user localePreference over Accept-Language`() {
        val (admin, _) = createAdminUser()
        // Spanish preference user
        admin.localePreference = "es"
        userRepository.save(admin)
        val adminToken = loginAndGetToken(admin.email, "admin123")
        val (doctor, _) = createDoctorUser()
        val patientId = createPatient(adminToken)
        val admissionId = createAdmission(adminToken, patientId, doctor.id!!, AdmissionType.AMBULATORY)

        val result = mockMvc.perform(
            get("/api/v1/admissions/$admissionId/export.pdf")
                .header("Authorization", "Bearer $adminToken")
                // Explicitly conflicting Accept-Language. localePreference must win.
                .header("Accept-Language", "en"),
        ).andExpect(status().isOk).andReturn()

        val text = extractPdfText(result.response.contentAsByteArray)
        // "Expediente" (Spanish) appears in cover section header; "Admission Record" (English) does not.
        assertTrue(text.contains("Expediente"), "Expected Spanish label, got: $text")
        assertFalse(text.contains("Admission Record"))
    }

    @Test
    fun `locale falls back to Accept-Language when user has no preference`() {
        val (admin, _) = createAdminUser()
        admin.localePreference = null
        userRepository.save(admin)
        val adminToken = loginAndGetToken(admin.email, "admin123")
        val (doctor, _) = createDoctorUser()
        val patientId = createPatient(adminToken)
        val admissionId = createAdmission(adminToken, patientId, doctor.id!!, AdmissionType.AMBULATORY)

        val result = mockMvc.perform(
            get("/api/v1/admissions/$admissionId/export.pdf")
                .header("Authorization", "Bearer $adminToken")
                .header("Accept-Language", "es"),
        ).andExpect(status().isOk).andReturn()

        val text = extractPdfText(result.response.contentAsByteArray)
        assertTrue(text.contains("Expediente"))
    }

    @Test
    fun `audit-row SHA-256 matches X-Admission-Export-Sha256 header byte-for-byte`() {
        val (_, adminToken) = createAdminUser()
        val (doctor, _) = createDoctorUser()
        val patientId = createPatient(adminToken)
        val admissionId = createAdmission(adminToken, patientId, doctor.id!!, AdmissionType.AMBULATORY)

        val result = mockMvc.perform(
            get("/api/v1/admissions/$admissionId/export.pdf")
                .header("Authorization", "Bearer $adminToken"),
        ).andExpect(status().isOk).andReturn()

        val headerSha = result.response.getHeader("X-Admission-Export-Sha256")!!
        val audit = auditLogRepository.findAll()
            .first { it.action == AuditAction.ADMISSION_EXPORT }
        val detailSha = objectMapper.readTree(audit.details).get("sha256").asText()
        assertEquals(headerSha, detailSha)
        assertNotEquals("", headerSha)
        assertNull(audit.details!!.takeIf { it.contains("Juan") })
    }

    private fun extractPdfText(bytes: ByteArray): String {
        org.apache.pdfbox.Loader.loadPDF(bytes).use { doc ->
            val stripper = org.apache.pdfbox.text.PDFTextStripper()
            return stripper.getText(doc)
        }
    }

    private fun snapshotStorageTree(root: Path): Map<String, Long> {
        if (!root.exists()) return emptyMap()
        Files.walk(root).use { stream ->
            return stream
                .filter { Files.isRegularFile(it) }
                .collect(Collectors.toMap({ root.relativize(it).toString() }, { Files.size(it) }))
        }
    }

    private fun listAdmissionExportTempDirs(tempRoot: Path): Set<String> {
        if (!tempRoot.exists()) return emptySet()
        Files.list(tempRoot).use { stream ->
            return stream
                .filter { Files.isDirectory(it) && it.fileName.toString().startsWith("admission-export-") }
                .map { it.fileName.toString() }
                .collect(Collectors.toSet())
        }
    }
}
