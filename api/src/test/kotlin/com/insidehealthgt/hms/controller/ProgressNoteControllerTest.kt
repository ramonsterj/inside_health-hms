package com.insidehealthgt.hms.controller

import com.insidehealthgt.hms.dto.request.CreateProgressNoteRequest
import com.insidehealthgt.hms.dto.request.UpdateProgressNoteRequest
import com.insidehealthgt.hms.entity.User
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class ProgressNoteControllerTest : AbstractIntegrationTest() {

    private lateinit var adminToken: String
    private lateinit var doctorToken: String
    private lateinit var nurseToken: String
    private lateinit var doctorUser: User
    private var admissionId: Long = 0

    @BeforeEach
    fun setUp() {
        val (_, adminTkn) = createAdminUser()
        adminToken = adminTkn

        val (docUsr, docTkn) = createDoctorUser()
        doctorUser = docUsr
        doctorToken = docTkn

        val (_, nurseTkn) = createNurseUser()
        nurseToken = nurseTkn

        // Create admission for tests
        val patientId = createPatient(adminToken)
        admissionId = createAdmission(adminToken, patientId, doctorUser.id!!)
    }

    // ============ LIST PROGRESS NOTES TESTS ============

    @Test
    fun `list progress notes returns empty page when none exist`() {
        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/progress-notes")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content").isEmpty)
    }

    @Test
    fun `list progress notes returns paginated results`() {
        createProgressNote("First note")
        createProgressNote("Second note")

        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/progress-notes")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content.length()").value(2))
    }

    // ============ CREATE PROGRESS NOTE TESTS ============

    @Test
    fun `doctor can create progress note`() {
        val request = CreateProgressNoteRequest(
            subjectiveData = "Patient reports feeling anxious",
            objectiveData = "Vital signs stable, BP 120/80",
            analysis = "Anxiety symptoms appear controlled",
            actionPlans = "Continue current medication",
        )

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/progress-notes")
                .header("Authorization", "Bearer $doctorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.subjectiveData").value("Patient reports feeling anxious"))
            .andExpect(jsonPath("$.data.createdBy.firstName").value("Dr. Maria"))
    }

    @Test
    fun `nurse can create progress note`() {
        val request = CreateProgressNoteRequest(
            subjectiveData = "Patient reports sleeping well",
            objectiveData = "Temperature 36.5C",
            analysis = "No concerns",
            actionPlans = "Continue monitoring",
        )

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/progress-notes")
                .header("Authorization", "Bearer $nurseToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.createdBy.firstName").value("Nurse"))
    }

    @Test
    fun `can create progress note with optional fields`() {
        val request = CreateProgressNoteRequest(
            subjectiveData = "Only subjective filled in",
            objectiveData = null,
            analysis = null,
            actionPlans = null,
        )

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/progress-notes")
                .header("Authorization", "Bearer $doctorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.subjectiveData").value("Only subjective filled in"))
            .andExpect(jsonPath("$.data.objectiveData").doesNotExist())
    }

    @Test
    fun `multiple notes per day allowed`() {
        createProgressNote("Morning note")
        createProgressNote("Afternoon note")
        createProgressNote("Evening note")

        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/progress-notes")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content.length()").value(3))
    }

    // ============ GET SINGLE PROGRESS NOTE TESTS ============

    @Test
    fun `get single progress note returns note details`() {
        val noteId = createProgressNoteAndGetId("Test note")

        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/progress-notes/$noteId")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(noteId))
            .andExpect(jsonPath("$.data.subjectiveData").value("Test note"))
    }

    @Test
    fun `get non-existent progress note returns 404`() {
        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/progress-notes/99999")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isNotFound)
    }

    // ============ UPDATE PROGRESS NOTE TESTS ============

    @Test
    fun `admin can update progress note`() {
        val noteId = createProgressNoteAndGetId("Original note")

        val updateRequest = UpdateProgressNoteRequest(
            subjectiveData = "Updated subjective data",
            objectiveData = "Updated objective data",
            analysis = "Updated analysis",
            actionPlans = "Updated action plans",
        )

        mockMvc.perform(
            put("/api/v1/admissions/$admissionId/progress-notes/$noteId")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.subjectiveData").value("Updated subjective data"))
    }

    @Test
    fun `doctor cannot update progress note`() {
        val noteId = createProgressNoteAndGetId("Original note")

        val updateRequest = UpdateProgressNoteRequest(
            subjectiveData = "Should fail",
            objectiveData = "Should fail",
            analysis = "Should fail",
            actionPlans = "Should fail",
        )

        mockMvc.perform(
            put("/api/v1/admissions/$admissionId/progress-notes/$noteId")
                .header("Authorization", "Bearer $doctorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `nurse cannot update progress note`() {
        val noteId = createProgressNoteAndGetId("Original note")

        val updateRequest = UpdateProgressNoteRequest(
            subjectiveData = "Should fail",
            objectiveData = "Should fail",
            analysis = "Should fail",
            actionPlans = "Should fail",
        )

        mockMvc.perform(
            put("/api/v1/admissions/$admissionId/progress-notes/$noteId")
                .header("Authorization", "Bearer $nurseToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)),
        )
            .andExpect(status().isForbidden)
    }

    // ============ UNAUTHENTICATED / NON-EXISTENT ADMISSION TESTS ============

    @Test
    fun `list progress notes fails without authentication`() {
        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/progress-notes"),
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `create progress note fails without authentication`() {
        val request = CreateProgressNoteRequest(
            subjectiveData = "Test",
            objectiveData = null,
            analysis = null,
            actionPlans = null,
        )

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/progress-notes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `list progress notes for non-existent admission returns 404`() {
        mockMvc.perform(
            get("/api/v1/admissions/99999/progress-notes")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `create progress note for non-existent admission returns 404`() {
        val request = CreateProgressNoteRequest(
            subjectiveData = "Test",
            objectiveData = null,
            analysis = null,
            actionPlans = null,
        )

        mockMvc.perform(
            post("/api/v1/admissions/99999/progress-notes")
                .header("Authorization", "Bearer $doctorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isNotFound)
    }

    // ============ AUDIT TESTS ============

    @Test
    fun `progress note includes audit fields`() {
        val noteId = createProgressNoteAndGetId("Audit test")

        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/progress-notes/$noteId")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.createdAt").exists())
            .andExpect(jsonPath("$.data.updatedAt").exists())
            .andExpect(jsonPath("$.data.createdBy.firstName").value("Dr. Maria"))
    }

    private fun createProgressNote(subjectiveData: String) {
        val request = CreateProgressNoteRequest(
            subjectiveData = subjectiveData,
            objectiveData = "Vital signs stable",
            analysis = "No concerns",
            actionPlans = "Continue monitoring",
        )

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/progress-notes")
                .header("Authorization", "Bearer $doctorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andExpect(status().isCreated)
    }

    private fun createProgressNoteAndGetId(subjectiveData: String): Long {
        val request = CreateProgressNoteRequest(
            subjectiveData = subjectiveData,
            objectiveData = "Vital signs stable",
            analysis = "No concerns",
            actionPlans = "Continue monitoring",
        )

        val result = mockMvc.perform(
            post("/api/v1/admissions/$admissionId/progress-notes")
                .header("Authorization", "Bearer $doctorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andReturn()

        return objectMapper.readTree(result.response.contentAsString)
            .get("data").get("id").asLong()
    }
}
