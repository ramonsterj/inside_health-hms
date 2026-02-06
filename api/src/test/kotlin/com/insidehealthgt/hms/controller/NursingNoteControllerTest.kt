package com.insidehealthgt.hms.controller

import com.insidehealthgt.hms.dto.request.CreateNursingNoteRequest
import com.insidehealthgt.hms.entity.User
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

class NursingNoteControllerTest : AbstractIntegrationTest() {

    private lateinit var adminToken: String
    private lateinit var doctorToken: String
    private lateinit var nurseToken: String
    private lateinit var nurseUser: User
    private lateinit var doctorUser: User
    private var admissionId: Long = 0

    @BeforeEach
    fun setUp() {
        val (_, adminTkn) = createAdminUser()
        adminToken = adminTkn

        val (doctorUsr, doctorTkn) = createDoctorUser()
        doctorUser = doctorUsr
        doctorToken = doctorTkn

        val (nurseUsr, nurseTkn) = createNurseUser()
        nurseUser = nurseUsr
        nurseToken = nurseTkn

        val patientId = createPatient(adminToken)
        admissionId = createAdmission(adminToken, patientId, doctorUser.id!!)
    }

    // ============ LIST NURSING NOTES TESTS ============

    @Test
    fun `list nursing notes returns empty page when none exist`() {
        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/nursing-notes")
                .header("Authorization", "Bearer $nurseToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content").isEmpty)
    }

    @Test
    fun `list nursing notes returns paginated results`() {
        createNursingNote("First note")
        createNursingNote("Second note")

        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/nursing-notes")
                .header("Authorization", "Bearer $nurseToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content.length()").value(2))
    }

    // ============ CREATE NURSING NOTE TESTS ============

    @Test
    fun `doctor can create nursing note`() {
        val request = CreateNursingNoteRequest(
            description = "Patient vital signs stable. No complaints.",
        )

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/nursing-notes")
                .header("Authorization", "Bearer $doctorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.description").value("Patient vital signs stable. No complaints."))
            .andExpect(jsonPath("$.data.createdBy.firstName").value("Dr. Maria"))
    }

    @Test
    fun `nurse can create nursing note`() {
        val request = CreateNursingNoteRequest(
            description = "Administered medication as prescribed.",
        )

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/nursing-notes")
                .header("Authorization", "Bearer $nurseToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.createdBy.firstName").value("Nurse"))
    }

    @Test
    fun `create nursing note returns 201 with audit fields`() {
        val request = CreateNursingNoteRequest(
            description = "Test nursing note for audit",
        )

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/nursing-notes")
                .header("Authorization", "Bearer $nurseToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.id").exists())
            .andExpect(jsonPath("$.data.admissionId").value(admissionId))
            .andExpect(jsonPath("$.data.createdAt").exists())
            .andExpect(jsonPath("$.data.updatedAt").exists())
            .andExpect(jsonPath("$.data.createdBy").exists())
            .andExpect(jsonPath("$.data.canEdit").value(true))
    }

    // ============ GET SINGLE NURSING NOTE TESTS ============

    @Test
    fun `get nursing note by id returns correct data`() {
        val noteId = createNursingNoteAndGetId("Test note content")

        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/nursing-notes/$noteId")
                .header("Authorization", "Bearer $nurseToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(noteId))
            .andExpect(jsonPath("$.data.description").value("Test note content"))
    }

    @Test
    fun `get non-existent nursing note returns 404`() {
        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/nursing-notes/99999")
                .header("Authorization", "Bearer $nurseToken"),
        )
            .andExpect(status().isNotFound)
    }

    // ============ UPDATE NURSING NOTE TESTS ============

    @Test
    fun `creator can update nursing note within 24 hours`() {
        val noteId = createNursingNoteAndGetId("Original note", nurseToken)

        val updateRequest = CreateNursingNoteRequest(
            description = "Updated note content",
        )

        mockMvc.perform(
            put("/api/v1/admissions/$admissionId/nursing-notes/$noteId")
                .header("Authorization", "Bearer $nurseToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.description").value("Updated note content"))
    }

    @Test
    fun `admin can update any nursing note`() {
        val noteId = createNursingNoteAndGetId("Original note", nurseToken)

        val updateRequest = CreateNursingNoteRequest(
            description = "Admin updated this note",
        )

        mockMvc.perform(
            put("/api/v1/admissions/$admissionId/nursing-notes/$noteId")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.description").value("Admin updated this note"))
    }

    // ============ VALIDATION TESTS ============

    @Test
    fun `create nursing note fails with empty description`() {
        val request = mapOf("description" to "")

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/nursing-notes")
                .header("Authorization", "Bearer $nurseToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `create nursing note fails with description over 5000 chars`() {
        val request = mapOf("description" to "X".repeat(5001))

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/nursing-notes")
                .header("Authorization", "Bearer $nurseToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)
    }

    // ============ AUTHORIZATION / BUSINESS RULES TESTS ============

    @Test
    fun `unauthenticated request returns 401`() {
        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/nursing-notes"),
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `list nursing notes for non-existent admission returns 404`() {
        mockMvc.perform(
            get("/api/v1/admissions/99999/nursing-notes")
                .header("Authorization", "Bearer $nurseToken"),
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `create nursing note for non-existent admission returns 404`() {
        val request = CreateNursingNoteRequest(
            description = "Test",
        )

        mockMvc.perform(
            post("/api/v1/admissions/99999/nursing-notes")
                .header("Authorization", "Bearer $nurseToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `non-creator cannot update nursing note`() {
        val noteId = createNursingNoteAndGetId("Nurse's note", nurseToken)

        val updateRequest = CreateNursingNoteRequest(
            description = "Should fail",
        )

        mockMvc.perform(
            put("/api/v1/admissions/$admissionId/nursing-notes/$noteId")
                .header("Authorization", "Bearer $doctorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `edit after 24 hours denied for non-admin`() {
        val noteId = createNursingNoteAndGetId("Old note", nurseToken)

        jdbcTemplate.update(
            "UPDATE nursing_notes SET created_at = ? WHERE id = ?",
            java.sql.Timestamp.valueOf(LocalDateTime.now().minusHours(25)),
            noteId,
        )

        val updateRequest = CreateNursingNoteRequest(
            description = "Should fail - too late",
        )

        mockMvc.perform(
            put("/api/v1/admissions/$admissionId/nursing-notes/$noteId")
                .header("Authorization", "Bearer $nurseToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `create nursing note fails for discharged admission`() {
        dischargeAdmission(admissionId, adminToken)

        val request = CreateNursingNoteRequest(
            description = "Should fail - discharged",
        )

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/nursing-notes")
                .header("Authorization", "Bearer $nurseToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `update nursing note fails for discharged admission`() {
        val noteId = createNursingNoteAndGetId("Note before discharge", nurseToken)
        dischargeAdmission(admissionId, adminToken)

        val updateRequest = CreateNursingNoteRequest(
            description = "Should fail - discharged",
        )

        mockMvc.perform(
            put("/api/v1/admissions/$admissionId/nursing-notes/$noteId")
                .header("Authorization", "Bearer $nurseToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)),
        )
            .andExpect(status().isBadRequest)
    }

    // ============ AUDIT TESTS ============

    @Test
    fun `nursing note includes createdBy and updatedBy audit fields`() {
        val noteId = createNursingNoteAndGetId("Audit test", nurseToken)

        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/nursing-notes/$noteId")
                .header("Authorization", "Bearer $nurseToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.createdAt").exists())
            .andExpect(jsonPath("$.data.updatedAt").exists())
            .andExpect(jsonPath("$.data.createdBy.firstName").value("Nurse"))
    }

    @Test
    fun `canEdit is true for creator within 24h and false otherwise`() {
        val noteId = createNursingNoteAndGetId("Edit window test", nurseToken)

        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/nursing-notes/$noteId")
                .header("Authorization", "Bearer $nurseToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.canEdit").value(true))

        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/nursing-notes/$noteId")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.canEdit").value(false))
    }

    // ============ HELPER METHODS ============

    private fun createNursingNote(description: String, token: String = nurseToken) {
        val request = CreateNursingNoteRequest(description = description)

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/nursing-notes")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andExpect(status().isCreated)
    }

    private fun createNursingNoteAndGetId(description: String, token: String = nurseToken): Long {
        val request = CreateNursingNoteRequest(description = description)

        val result = mockMvc.perform(
            post("/api/v1/admissions/$admissionId/nursing-notes")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andReturn()

        return objectMapper.readTree(result.response.contentAsString)
            .get("data").get("id").asLong()
    }
}
