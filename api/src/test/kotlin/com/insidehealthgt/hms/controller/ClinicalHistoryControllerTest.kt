package com.insidehealthgt.hms.controller

import com.insidehealthgt.hms.dto.request.CreateClinicalHistoryRequest
import com.insidehealthgt.hms.dto.request.UpdateClinicalHistoryRequest
import com.insidehealthgt.hms.entity.User
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class ClinicalHistoryControllerTest : AbstractIntegrationTest() {

    private lateinit var adminToken: String
    private lateinit var doctorToken: String
    private lateinit var nurseToken: String
    private lateinit var doctorUser: User
    private var admissionId: Long = 0

    @BeforeEach
    fun setUp() {
        val (_, adminTkn) = createAdminUser()
        adminToken = adminTkn

        val (doctorUsr, doctorTkn) = createDoctorUser()
        doctorUser = doctorUsr
        doctorToken = doctorTkn

        val (_, nurseTkn) = createNurseUser()
        nurseToken = nurseTkn

        val patientId = createPatient(adminToken)
        admissionId = createAdmission(adminToken, patientId, doctorUser.id!!)
    }

    // ============ GET CLINICAL HISTORY TESTS ============

    @Test
    fun `get clinical history returns null when not exists`() {
        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/clinical-history")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `doctor can read clinical history`() {
        createClinicalHistory()

        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/clinical-history")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.reasonForAdmission").value("Anxiety disorder"))
    }

    @Test
    fun `nurse can read clinical history`() {
        createClinicalHistory()

        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/clinical-history")
                .header("Authorization", "Bearer $nurseToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.reasonForAdmission").value("Anxiety disorder"))
    }

    @Test
    fun `admin can read clinical history`() {
        createClinicalHistory()

        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/clinical-history")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.reasonForAdmission").value("Anxiety disorder"))
    }

    // ============ CREATE CLINICAL HISTORY TESTS ============

    @Test
    fun `doctor can create clinical history`() {
        val request = CreateClinicalHistoryRequest(
            reasonForAdmission = "Anxiety disorder",
            historyOfPresentIllness = "Patient reports increasing anxiety",
            psychiatricHistory = "No prior psychiatric history",
        )

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/clinical-history")
                .header("Authorization", "Bearer $doctorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.reasonForAdmission").value("Anxiety disorder"))
            .andExpect(jsonPath("$.data.createdBy").exists())
    }

    @Test
    fun `nurse cannot create clinical history`() {
        val request = CreateClinicalHistoryRequest(
            reasonForAdmission = "Should fail",
        )

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/clinical-history")
                .header("Authorization", "Bearer $nurseToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `duplicate creation fails with 400`() {
        createClinicalHistory()

        val request = CreateClinicalHistoryRequest(
            reasonForAdmission = "Duplicate",
        )

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/clinical-history")
                .header("Authorization", "Bearer $doctorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.message").value("Clinical history already exists for this admission"))
    }

    // ============ UPDATE CLINICAL HISTORY TESTS ============

    @Test
    fun `admin can update clinical history`() {
        createClinicalHistory()

        val updateRequest = UpdateClinicalHistoryRequest(
            reasonForAdmission = "Updated: Severe anxiety disorder",
            treatmentPlan = "Medication and therapy",
        )

        mockMvc.perform(
            put("/api/v1/admissions/$admissionId/clinical-history")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.reasonForAdmission").value("Updated: Severe anxiety disorder"))
            .andExpect(jsonPath("$.data.treatmentPlan").value("Medication and therapy"))
    }

    @Test
    fun `doctor cannot update clinical history`() {
        createClinicalHistory()

        val updateRequest = UpdateClinicalHistoryRequest(
            reasonForAdmission = "Should fail",
        )

        mockMvc.perform(
            put("/api/v1/admissions/$admissionId/clinical-history")
                .header("Authorization", "Bearer $doctorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `nurse cannot update clinical history`() {
        createClinicalHistory()

        val updateRequest = UpdateClinicalHistoryRequest(
            reasonForAdmission = "Should fail",
        )

        mockMvc.perform(
            put("/api/v1/admissions/$admissionId/clinical-history")
                .header("Authorization", "Bearer $nurseToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `update returns 404 when clinical history not exists`() {
        val updateRequest = UpdateClinicalHistoryRequest(
            reasonForAdmission = "Should fail",
        )

        mockMvc.perform(
            put("/api/v1/admissions/$admissionId/clinical-history")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)),
        )
            .andExpect(status().isNotFound)
    }

    // ============ UNAUTHENTICATED / NON-EXISTENT ADMISSION TESTS ============

    @Test
    fun `get clinical history fails without authentication`() {
        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/clinical-history"),
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `create clinical history fails without authentication`() {
        val request = CreateClinicalHistoryRequest(
            reasonForAdmission = "Test",
        )

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/clinical-history")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `get clinical history for non-existent admission returns 404`() {
        mockMvc.perform(
            get("/api/v1/admissions/99999/clinical-history")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `create clinical history for non-existent admission returns 404`() {
        val request = CreateClinicalHistoryRequest(
            reasonForAdmission = "Test",
        )

        mockMvc.perform(
            post("/api/v1/admissions/99999/clinical-history")
                .header("Authorization", "Bearer $doctorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isNotFound)
    }

    // ============ AUDIT TESTS ============

    @Test
    fun `clinical history includes audit fields`() {
        createClinicalHistory()

        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/clinical-history")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.createdAt").exists())
            .andExpect(jsonPath("$.data.updatedAt").exists())
            .andExpect(jsonPath("$.data.createdBy.firstName").value("Dr. Maria"))
    }

    private fun createClinicalHistory() {
        val request = CreateClinicalHistoryRequest(
            reasonForAdmission = "Anxiety disorder",
            historyOfPresentIllness = "Patient reports increasing anxiety",
        )

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/clinical-history")
                .header("Authorization", "Bearer $doctorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andExpect(status().isCreated)
    }
}
