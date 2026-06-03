package com.insidehealthgt.hms.controller

import com.insidehealthgt.hms.dto.request.CreateMedicalOrderRequest
import com.insidehealthgt.hms.dto.request.CreateMedicationAdministrationRequest
import com.insidehealthgt.hms.dto.request.CreateNursingNoteRequest
import com.insidehealthgt.hms.dto.request.CreateProgressNoteRequest
import com.insidehealthgt.hms.dto.request.CreateVitalSignRequest
import com.insidehealthgt.hms.entity.AdministrationStatus
import com.insidehealthgt.hms.entity.MedicalOrderCategory
import com.insidehealthgt.hms.entity.User
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Service-layer guard for AUXILIARY_NURSE: even when the underlying permission is granted
 * (here via an additional custom role), the three restricted actions — administer medication,
 * mark order in progress, upload result document — return 403 with error.nursing.auxiliary.denied
 * for a user whose only nursing-or-better role is AUXILIARY_NURSE (AC-3/4/5). A plain auxiliary
 * can still write nursing notes and vital signs (AC-8) but not progress notes (AC-9). NURSE and
 * stacked NURSE+AUXILIARY_NURSE users are unaffected (AC-10/AC-11).
 *
 * Spec: docs/features/nursing-roles-split.md.
 */
class AuxiliaryNurseRestrictionTest : AbstractIntegrationTest() {

    private lateinit var adminToken: String
    private lateinit var doctorToken: String
    private lateinit var doctorUser: User

    // Auxiliary nurse that ALSO holds a custom role granting the three guarded permissions —
    // models AC-3's "even if a custom role accidentally grants the permission". @PreAuthorize
    // passes for this user; only the service-layer guard blocks it.
    private lateinit var auxWithPermsToken: String

    // Plain auxiliary nurse with only the default AUXILIARY_NURSE grants.
    private lateinit var auxPlainToken: String

    private lateinit var nurseToken: String
    private lateinit var stackedNurseAuxToken: String

    private var admissionId: Long = 0

    @BeforeEach
    fun setUp() {
        val (_, adminTkn) = createAdminUser()
        adminToken = adminTkn

        val (docUsr, docTkn) = createDoctorUser()
        doctorUser = docUsr
        doctorToken = docTkn

        // Custom role that grants the three guarded permissions to its holders.
        jdbcTemplate.update(
            """
            INSERT INTO roles (code, name, description, is_system, created_at, updated_at)
            VALUES ('CUSTOM_NURSE_PERMS', 'Custom Nurse Perms', 'test-only custom role',
                    FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            ON CONFLICT (code) DO NOTHING
            """.trimIndent(),
        )
        jdbcTemplate.update(
            """
            INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
            SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
            FROM roles r, permissions p
            WHERE r.code = 'CUSTOM_NURSE_PERMS'
              AND p.code IN (
                'medication-administration:create',
                'medical-order:mark-in-progress',
                'medical-order:upload-document'
              )
            ON CONFLICT DO NOTHING
            """.trimIndent(),
        )

        val (_, auxPermsTkn) = createUserWithRole(
            roleCode = "AUXILIARY_NURSE",
            username = "aux_with_perms",
            email = "aux_with_perms@example.com",
            password = "password123",
            extraRoleCodes = listOf("CUSTOM_NURSE_PERMS"),
        )
        auxWithPermsToken = auxPermsTkn

        val (_, auxPlainTkn) = createUserWithRole(
            roleCode = "AUXILIARY_NURSE",
            username = "aux_plain",
            email = "aux_plain@example.com",
            password = "password123",
        )
        auxPlainToken = auxPlainTkn

        val (_, nurseTkn) = createNurseUser()
        nurseToken = nurseTkn

        val (_, stackedTkn) = createUserWithRole(
            roleCode = "NURSE",
            username = "graduate_covering_aux",
            email = "graduate_covering_aux@example.com",
            password = "password123",
            extraRoleCodes = listOf("AUXILIARY_NURSE"),
        )
        stackedNurseAuxToken = stackedTkn

        val patientId = createPatient(adminToken)
        admissionId = createAdmission(adminToken, patientId, doctorUser.id!!)
    }

    // ============ AC-3/4/5: service guard blocks the three actions ============

    @Test
    fun `auxiliary nurse cannot administer medication even with the permission granted`() {
        val orderId = createResultsBearingOrder()
        val request = CreateMedicationAdministrationRequest(status = AdministrationStatus.GIVEN)

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/medical-orders/$orderId/administrations")
                .header("Authorization", "Bearer $auxWithPermsToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.error.message").value(containsString("Auxiliary nurses cannot")))
    }

    @Test
    fun `auxiliary nurse cannot mark order in progress even with the permission granted`() {
        val orderId = createResultsBearingOrder()
        authorize(orderId)

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/medical-orders/$orderId/mark-in-progress")
                .header("Authorization", "Bearer $auxWithPermsToken"),
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.error.message").value(containsString("Auxiliary nurses cannot")))
    }

    @Test
    fun `auxiliary nurse cannot upload result document even with the permission granted`() {
        val orderId = createResultsBearingOrder()
        val mockFile = MockMultipartFile(
            "file",
            "result.pdf",
            MediaType.APPLICATION_PDF_VALUE,
            "fake-pdf-content".toByteArray(),
        )

        mockMvc.perform(
            multipart("/api/v1/admissions/$admissionId/medical-orders/$orderId/documents")
                .file(mockFile)
                .header("Authorization", "Bearer $auxWithPermsToken"),
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.error.message").value(containsString("Auxiliary nurses cannot")))
    }

    // ============ AC-8: auxiliary can write notes and vitals ============

    @Test
    fun `auxiliary nurse can create a nursing note`() {
        val request = CreateNursingNoteRequest(description = "Patient resting comfortably.")

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/nursing-notes")
                .header("Authorization", "Bearer $auxPlainToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isCreated)
    }

    @Test
    fun `auxiliary nurse can record a vital sign`() {
        val request = CreateVitalSignRequest(
            systolicBp = 120,
            diastolicBp = 80,
            heartRate = 72,
            respiratoryRate = 16,
            temperature = BigDecimal("36.6"),
            oxygenSaturation = 98,
        )

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/vital-signs")
                .header("Authorization", "Bearer $auxPlainToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isCreated)
    }

    // ============ auxiliary cannot discharge or edit admissions ============

    @Test
    fun `auxiliary nurse cannot discharge a patient`() {
        // admission:update is not granted to AUXILIARY_NURSE, so @PreAuthorize blocks the
        // discharge endpoint (which is gated by admission:update). Notes/vitals-only scope.
        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/discharge")
                .header("Authorization", "Bearer $auxPlainToken"),
        )
            .andExpect(status().isForbidden)
    }

    // ============ AC-9: auxiliary cannot author progress notes ============

    @Test
    fun `auxiliary nurse cannot create a progress note`() {
        val request = CreateProgressNoteRequest(subjectiveData = "Should be rejected")

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/progress-notes")
                .header("Authorization", "Bearer $auxPlainToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isForbidden)
    }

    // ============ AC-10 / AC-11: NURSE and stacked roles unaffected ============

    @Test
    fun `graduate nurse can mark an authorized order in progress`() {
        val orderId = createResultsBearingOrder()
        authorize(orderId)

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/medical-orders/$orderId/mark-in-progress")
                .header("Authorization", "Bearer $nurseToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.status").value("EN_PROCESO"))
    }

    @Test
    fun `nurse stacked with auxiliary role can mark an authorized order in progress`() {
        val orderId = createResultsBearingOrder()
        authorize(orderId)

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/medical-orders/$orderId/mark-in-progress")
                .header("Authorization", "Bearer $stackedNurseAuxToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.status").value("EN_PROCESO"))
    }

    // ============ helpers ============

    private fun createResultsBearingOrder(): Long {
        val request = CreateMedicalOrderRequest(
            category = MedicalOrderCategory.REFERENCIAS_MEDICAS,
            startDate = LocalDate.now(),
            medication = "Referencia",
        )
        val result = mockMvc.perform(
            post("/api/v1/admissions/$admissionId/medical-orders")
                .header("Authorization", "Bearer $doctorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andExpect(status().isCreated).andReturn()

        return objectMapper.readTree(result.response.contentAsString)
            .get("data").get("id").asLong()
    }

    private fun authorize(orderId: Long) {
        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/medical-orders/$orderId/authorize")
                .header("Authorization", "Bearer $adminToken"),
        ).andExpect(status().isOk)
    }
}
