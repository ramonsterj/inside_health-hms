package com.insidehealthgt.hms.controller

import com.insidehealthgt.hms.dto.request.CreateMedicalOrderRequest
import com.insidehealthgt.hms.dto.request.CreateNursingNoteRequest
import com.insidehealthgt.hms.dto.request.CreateVitalSignRequest
import com.insidehealthgt.hms.entity.AdministrationRoute
import com.insidehealthgt.hms.entity.AdmissionType
import com.insidehealthgt.hms.entity.MedicalOrderCategory
import com.insidehealthgt.hms.entity.Room
import com.insidehealthgt.hms.entity.RoomGender
import com.insidehealthgt.hms.entity.RoomType
import com.insidehealthgt.hms.entity.User
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class NursingKardexControllerTest : AbstractIntegrationTest() {

    private lateinit var adminToken: String
    private lateinit var doctorUser: User
    private lateinit var doctorToken: String
    private lateinit var nurseUser: User
    private lateinit var nurseToken: String
    private var admissionId1: Long = 0
    private var admissionId2: Long = 0
    private var patientId1: Long = 0
    private var patientId2: Long = 0

    @BeforeEach
    fun setUp() {
        val (_, adminTkn) = createAdminUser()
        adminToken = adminTkn
        val (docUser, docTkn) = createDoctorUser()
        doctorUser = docUser
        doctorToken = docTkn
        val (nrsUser, nrsTkn) = createNurseUser()
        nurseUser = nrsUser
        nurseToken = nrsTkn

        // Create room and get seeded triage code for HOSPITALIZATION
        val room = roomRepository.save(
            Room(
                number = "101",
                type = RoomType.PRIVATE,
                gender = RoomGender.MALE,
                capacity = 4,
            ),
        )
        val triageCode = triageCodeRepository.findAll().first()

        patientId1 = createPatient(adminToken)
        patientId2 = createSecondPatient(adminToken)

        admissionId1 = createAdmission(
            adminToken,
            patientId1,
            doctorUser.id!!,
            type = AdmissionType.HOSPITALIZATION,
            roomId = room.id!!,
            triageCodeId = triageCode.id!!,
            admissionDate = LocalDateTime.now().minusDays(3),
        )
        admissionId2 = createAdmission(
            adminToken,
            patientId2,
            doctorUser.id!!,
            type = AdmissionType.AMBULATORY,
            admissionDate = LocalDateTime.now().minusDays(1),
        )
    }

    // ========== LIST ENDPOINT ==========

    @Test
    fun `list kardex should return active admissions`() {
        mockMvc.perform(
            get("/api/v1/nursing-kardex")
                .header("Authorization", "Bearer $nurseToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content").isArray)
            .andExpect(jsonPath("$.data.content.length()").value(2))
            .andExpect(jsonPath("$.data.content[0].admissionId").isNumber)
            .andExpect(jsonPath("$.data.content[0].patientName").isString)
            .andExpect(jsonPath("$.data.content[0].treatingPhysicianName").isString)
            .andExpect(jsonPath("$.data.content[0].activeMedicationCount").value(0))
            .andExpect(jsonPath("$.data.content[0].activeCareInstructionCount").value(0))
            .andExpect(jsonPath("$.data.content[0].medications").isArray)
            .andExpect(jsonPath("$.data.content[0].careInstructions").isArray)
    }

    @Test
    fun `list kardex should filter by admission type`() {
        mockMvc.perform(
            get("/api/v1/nursing-kardex")
                .param("type", "HOSPITALIZATION")
                .header("Authorization", "Bearer $nurseToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content.length()").value(1))
            .andExpect(jsonPath("$.data.content[0].admissionType").value("HOSPITALIZATION"))
    }

    @Test
    fun `list kardex should filter by patient name search`() {
        mockMvc.perform(
            get("/api/v1/nursing-kardex")
                .param("search", "Juan")
                .header("Authorization", "Bearer $nurseToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content.length()").value(1))
            .andExpect(jsonPath("$.data.content[0].patientName").value("Juan Perez"))
    }

    @Test
    fun `list kardex should return empty for non-matching search`() {
        mockMvc.perform(
            get("/api/v1/nursing-kardex")
                .param("search", "NonExistentPatient")
                .header("Authorization", "Bearer $nurseToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content.length()").value(0))
    }

    @Test
    fun `list kardex should support pagination`() {
        mockMvc.perform(
            get("/api/v1/nursing-kardex")
                .param("size", "1")
                .param("page", "0")
                .header("Authorization", "Bearer $nurseToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content.length()").value(1))
            .andExpect(jsonPath("$.data.page.totalElements").value(2))
            .andExpect(jsonPath("$.data.page.totalPages").value(2))
    }

    @Test
    fun `list kardex should exclude discharged admissions`() {
        dischargeAdmission(admissionId1, adminToken)

        mockMvc.perform(
            get("/api/v1/nursing-kardex")
                .header("Authorization", "Bearer $nurseToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content.length()").value(1))
    }

    @Test
    fun `list kardex should include medication orders`() {
        createMedicalOrder(admissionId1, doctorToken)

        mockMvc.perform(
            get("/api/v1/nursing-kardex")
                .param("search", "Juan")
                .header("Authorization", "Bearer $nurseToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content[0].activeMedicationCount").value(1))
            .andExpect(jsonPath("$.data.content[0].medications.length()").value(1))
            .andExpect(jsonPath("$.data.content[0].medications[0].medication").value("Acetaminophen"))
    }

    @Test
    fun `list kardex should include care instructions`() {
        createCareInstruction(admissionId1, doctorToken)

        mockMvc.perform(
            get("/api/v1/nursing-kardex")
                .param("search", "Juan")
                .header("Authorization", "Bearer $nurseToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content[0].activeCareInstructionCount").value(1))
            .andExpect(jsonPath("$.data.content[0].careInstructions.length()").value(1))
    }

    @Test
    fun `list kardex should include latest vital signs`() {
        createVitalSigns(admissionId1, nurseToken)

        mockMvc.perform(
            get("/api/v1/nursing-kardex")
                .param("search", "Juan")
                .header("Authorization", "Bearer $nurseToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content[0].latestVitalSigns").isNotEmpty)
            .andExpect(jsonPath("$.data.content[0].latestVitalSigns.systolicBp").value(120))
            .andExpect(jsonPath("$.data.content[0].hoursSinceLastVitals").isNumber)
    }

    @Test
    fun `list kardex should include latest nursing note preview`() {
        createNursingNote(admissionId1, nurseToken)

        mockMvc.perform(
            get("/api/v1/nursing-kardex")
                .param("search", "Juan")
                .header("Authorization", "Bearer $nurseToken"),
        )
            .andExpect(status().isOk)
            .andExpect(
                jsonPath("$.data.content[0].lastNursingNotePreview")
                    .value("Patient is stable and resting comfortably"),
            )
            .andExpect(jsonPath("$.data.content[0].lastNursingNoteAt").isNotEmpty)
    }

    // ========== SINGLE ENDPOINT ==========

    @Test
    fun `get single kardex should return admission summary`() {
        mockMvc.perform(
            get("/api/v1/nursing-kardex/$admissionId1")
                .header("Authorization", "Bearer $nurseToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.admissionId").value(admissionId1))
            .andExpect(jsonPath("$.data.patientName").value("Juan Perez"))
            .andExpect(jsonPath("$.data.admissionType").value("HOSPITALIZATION"))
    }

    @Test
    fun `get single kardex should return 404 for non-existent admission`() {
        mockMvc.perform(
            get("/api/v1/nursing-kardex/99999")
                .header("Authorization", "Bearer $nurseToken"),
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `get single kardex should return 404 for discharged admission`() {
        dischargeAdmission(admissionId1, adminToken)

        mockMvc.perform(
            get("/api/v1/nursing-kardex/$admissionId1")
                .header("Authorization", "Bearer $nurseToken"),
        )
            .andExpect(status().isNotFound)
    }

    // ========== AUTHORIZATION ==========

    @Test
    fun `list kardex should fail without authentication`() {
        mockMvc.perform(
            get("/api/v1/nursing-kardex"),
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `list kardex should be accessible by admin`() {
        mockMvc.perform(
            get("/api/v1/nursing-kardex")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `list kardex should be accessible by doctor`() {
        mockMvc.perform(
            get("/api/v1/nursing-kardex")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isOk)
    }

    // ========== HELPERS ==========

    private fun createMedicalOrder(admissionId: Long, token: String) {
        val request = CreateMedicalOrderRequest(
            category = MedicalOrderCategory.MEDICAMENTOS,
            startDate = LocalDate.now(),
            medication = "Acetaminophen",
            dosage = "500mg",
            route = AdministrationRoute.ORAL,
            frequency = "Every 6 hours",
            schedule = "06:00, 12:00, 18:00, 00:00",
        )
        val createdId = createOrderAndGetId(admissionId, token, request)
        authorize(admissionId, createdId)
    }

    private fun createCareInstruction(admissionId: Long, token: String) {
        val request = CreateMedicalOrderRequest(
            category = MedicalOrderCategory.DIETA,
            startDate = LocalDate.now(),
            observations = "Low sodium diet",
        )
        // Directive categories (DIETA, etc.) are created directly in ACTIVA — they
        // don't have an authorize step. The kardex query now matches both ACTIVA and AUTORIZADO.
        createOrderAndGetId(admissionId, token, request)
    }

    private fun createOrderAndGetId(admissionId: Long, token: String, request: CreateMedicalOrderRequest): Long {
        val result = mockMvc.perform(
            post("/api/v1/admissions/$admissionId/medical-orders")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andExpect(status().isCreated).andReturn()
        return objectMapper.readTree(result.response.contentAsString).get("data").get("id").asLong()
    }

    // Kardex queries filter by AUTORIZADO. Tests must authorize after creation.
    private fun authorize(admissionId: Long, orderId: Long) {
        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/medical-orders/$orderId/authorize")
                .header("Authorization", "Bearer $adminToken"),
        ).andExpect(status().isOk)
    }

    private fun createVitalSigns(admissionId: Long, token: String) {
        val request = CreateVitalSignRequest(
            systolicBp = 120,
            diastolicBp = 80,
            heartRate = 72,
            respiratoryRate = 16,
            temperature = BigDecimal("36.5"),
            oxygenSaturation = 98,
        )
        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/vital-signs")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andExpect(status().isCreated)
    }

    private fun createNursingNote(admissionId: Long, token: String) {
        val request = CreateNursingNoteRequest(
            description = "Patient is stable and resting comfortably",
        )
        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/nursing-notes")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andExpect(status().isCreated)
    }
}
