package com.insidehealthgt.hms.controller

import com.insidehealthgt.hms.dto.request.CreatePsychotherapyActivityRequest
import com.insidehealthgt.hms.entity.AdmissionType
import com.insidehealthgt.hms.entity.Room
import com.insidehealthgt.hms.entity.RoomType
import com.insidehealthgt.hms.entity.User
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class PsychotherapyActivityControllerTest : AbstractIntegrationTest() {

    private lateinit var adminToken: String
    private lateinit var psychologistToken: String
    private lateinit var doctorToken: String
    private lateinit var nurseToken: String
    private lateinit var adminStaffToken: String
    private lateinit var doctorUser: User

    private var hospitalizationAdmissionId: Long = 0
    private var ambulatoryAdmissionId: Long = 0
    private var categoryId: Long = 0

    @BeforeEach
    fun setUp() {
        val (_, adminTkn) = createAdminUser()
        adminToken = adminTkn

        val (_, psychTkn) = createPsychologistUser()
        psychologistToken = psychTkn

        val (docUsr, docTkn) = createDoctorUser()
        doctorUser = docUsr
        doctorToken = docTkn

        val (_, nurseTkn) = createNurseUser()
        nurseToken = nurseTkn

        val (_, staffTkn) = createAdminStaffUser()
        adminStaffToken = staffTkn

        categoryId = psychotherapyCategoryRepository.findAll().first().id!!

        // Create hospitalization admission (requires room + triage code)
        val room = Room(
            number = "Room-${System.nanoTime()}",
            type = RoomType.PRIVATE,
            capacity = 1,
        )
        roomRepository.save(room)
        val triageCode = triageCodeRepository.findAll().first()
        val patient1Id = createPatient(adminStaffToken)
        hospitalizationAdmissionId = createAdmission(
            adminStaffToken,
            patient1Id,
            doctorUser.id!!,
            AdmissionType.HOSPITALIZATION,
            room.id!!,
            triageCode.id!!,
        )

        // Create ambulatory admission (different patient)
        val patient2Id = createSecondPatient(adminStaffToken)
        ambulatoryAdmissionId = createAdmission(
            adminStaffToken,
            patient2Id,
            doctorUser.id!!,
            AdmissionType.AMBULATORY,
        )
    }

    // ============ CREATE ACTIVITY TESTS ============

    @Test
    fun `create activity with psychologist should return 201`() {
        val request = CreatePsychotherapyActivityRequest(
            categoryId = categoryId,
            description = "Patient participated in art therapy workshop",
        )

        mockMvc.perform(
            post("/api/v1/admissions/$hospitalizationAdmissionId/psychotherapy-activities")
                .header("Authorization", "Bearer $psychologistToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.description").value("Patient participated in art therapy workshop"))
            .andExpect(jsonPath("$.data.category.id").value(categoryId))
            .andExpect(jsonPath("$.data.admissionId").value(hospitalizationAdmissionId))
            .andExpect(jsonPath("$.data.createdBy.firstName").value("Psych"))
    }

    @Test
    fun `create activity should fail for doctor`() {
        val request = CreatePsychotherapyActivityRequest(
            categoryId = categoryId,
            description = "Test activity",
        )

        mockMvc.perform(
            post("/api/v1/admissions/$hospitalizationAdmissionId/psychotherapy-activities")
                .header("Authorization", "Bearer $doctorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `create activity should fail for nurse`() {
        val request = CreatePsychotherapyActivityRequest(
            categoryId = categoryId,
            description = "Test activity",
        )

        mockMvc.perform(
            post("/api/v1/admissions/$hospitalizationAdmissionId/psychotherapy-activities")
                .header("Authorization", "Bearer $nurseToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `create activity should fail for admin`() {
        val request = CreatePsychotherapyActivityRequest(
            categoryId = categoryId,
            description = "Test activity",
        )

        mockMvc.perform(
            post("/api/v1/admissions/$hospitalizationAdmissionId/psychotherapy-activities")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `create activity should fail for ambulatory admission`() {
        val request = CreatePsychotherapyActivityRequest(
            categoryId = categoryId,
            description = "Test activity",
        )

        mockMvc.perform(
            post("/api/v1/admissions/$ambulatoryAdmissionId/psychotherapy-activities")
                .header("Authorization", "Bearer $psychologistToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `create activity should fail with blank description`() {
        val request = mapOf(
            "categoryId" to categoryId,
            "description" to "",
        )

        mockMvc.perform(
            post("/api/v1/admissions/$hospitalizationAdmissionId/psychotherapy-activities")
                .header("Authorization", "Bearer $psychologistToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `create activity should fail with non-existent category`() {
        val request = CreatePsychotherapyActivityRequest(
            categoryId = 99999,
            description = "Test activity",
        )

        mockMvc.perform(
            post("/api/v1/admissions/$hospitalizationAdmissionId/psychotherapy-activities")
                .header("Authorization", "Bearer $psychologistToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isNotFound)
    }

    // ============ LIST ACTIVITIES TESTS ============

    @Test
    fun `list activities should return activities`() {
        // Create an activity first
        val request = CreatePsychotherapyActivityRequest(
            categoryId = categoryId,
            description = "Test activity",
        )
        mockMvc.perform(
            post("/api/v1/admissions/$hospitalizationAdmissionId/psychotherapy-activities")
                .header("Authorization", "Bearer $psychologistToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andExpect(status().isCreated)

        // List activities
        mockMvc.perform(
            get("/api/v1/admissions/$hospitalizationAdmissionId/psychotherapy-activities")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data[0].description").value("Test activity"))
    }

    @Test
    fun `list activities should be accessible by psychologist`() {
        mockMvc.perform(
            get("/api/v1/admissions/$hospitalizationAdmissionId/psychotherapy-activities")
                .header("Authorization", "Bearer $psychologistToken"),
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `list activities should be accessible by doctor`() {
        mockMvc.perform(
            get("/api/v1/admissions/$hospitalizationAdmissionId/psychotherapy-activities")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `list activities should be accessible by nurse`() {
        mockMvc.perform(
            get("/api/v1/admissions/$hospitalizationAdmissionId/psychotherapy-activities")
                .header("Authorization", "Bearer $nurseToken"),
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `list activities with sort asc should sort oldest first`() {
        // Create first activity
        mockMvc.perform(
            post("/api/v1/admissions/$hospitalizationAdmissionId/psychotherapy-activities")
                .header("Authorization", "Bearer $psychologistToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        CreatePsychotherapyActivityRequest(categoryId, "First activity"),
                    ),
                ),
        ).andExpect(status().isCreated)

        // Create second activity
        mockMvc.perform(
            post("/api/v1/admissions/$hospitalizationAdmissionId/psychotherapy-activities")
                .header("Authorization", "Bearer $psychologistToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        CreatePsychotherapyActivityRequest(categoryId, "Second activity"),
                    ),
                ),
        ).andExpect(status().isCreated)

        // List with asc sort - oldest first
        mockMvc.perform(
            get("/api/v1/admissions/$hospitalizationAdmissionId/psychotherapy-activities?sort=asc")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].description").value("First activity"))
            .andExpect(jsonPath("$.data[1].description").value("Second activity"))
    }

    @Test
    fun `list activities with sort desc should sort newest first`() {
        // Create first activity
        mockMvc.perform(
            post("/api/v1/admissions/$hospitalizationAdmissionId/psychotherapy-activities")
                .header("Authorization", "Bearer $psychologistToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        CreatePsychotherapyActivityRequest(categoryId, "First activity"),
                    ),
                ),
        ).andExpect(status().isCreated)

        // Create second activity
        mockMvc.perform(
            post("/api/v1/admissions/$hospitalizationAdmissionId/psychotherapy-activities")
                .header("Authorization", "Bearer $psychologistToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        CreatePsychotherapyActivityRequest(categoryId, "Second activity"),
                    ),
                ),
        ).andExpect(status().isCreated)

        // List with desc sort - newest first (default)
        mockMvc.perform(
            get("/api/v1/admissions/$hospitalizationAdmissionId/psychotherapy-activities?sort=desc")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].description").value("Second activity"))
            .andExpect(jsonPath("$.data[1].description").value("First activity"))
    }

    // ============ GET ACTIVITY TESTS ============

    @Test
    fun `get activity should return activity details`() {
        // Create an activity first
        val request = CreatePsychotherapyActivityRequest(
            categoryId = categoryId,
            description = "Test activity",
        )
        val createResult = mockMvc.perform(
            post("/api/v1/admissions/$hospitalizationAdmissionId/psychotherapy-activities")
                .header("Authorization", "Bearer $psychologistToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andReturn()

        val activityId = objectMapper.readTree(createResult.response.contentAsString)
            .get("data").get("id").asLong()

        // Get activity
        mockMvc.perform(
            get("/api/v1/admissions/$hospitalizationAdmissionId/psychotherapy-activities/$activityId")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.description").value("Test activity"))
    }

    @Test
    fun `get activity should return 404 for non-existent`() {
        mockMvc.perform(
            get("/api/v1/admissions/$hospitalizationAdmissionId/psychotherapy-activities/99999")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isNotFound)
    }

    // ============ DELETE ACTIVITY TESTS ============

    @Test
    fun `delete activity should work for admin`() {
        // Create an activity first
        val request = CreatePsychotherapyActivityRequest(
            categoryId = categoryId,
            description = "Test activity",
        )
        val createResult = mockMvc.perform(
            post("/api/v1/admissions/$hospitalizationAdmissionId/psychotherapy-activities")
                .header("Authorization", "Bearer $psychologistToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andReturn()

        val activityId = objectMapper.readTree(createResult.response.contentAsString)
            .get("data").get("id").asLong()

        // Delete activity
        mockMvc.perform(
            delete("/api/v1/admissions/$hospitalizationAdmissionId/psychotherapy-activities/$activityId")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isNoContent)

        // Should not be found after deletion
        mockMvc.perform(
            get("/api/v1/admissions/$hospitalizationAdmissionId/psychotherapy-activities/$activityId")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `delete activity should fail for psychologist`() {
        // Create an activity first
        val request = CreatePsychotherapyActivityRequest(
            categoryId = categoryId,
            description = "Test activity",
        )
        val createResult = mockMvc.perform(
            post("/api/v1/admissions/$hospitalizationAdmissionId/psychotherapy-activities")
                .header("Authorization", "Bearer $psychologistToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andReturn()

        val activityId = objectMapper.readTree(createResult.response.contentAsString)
            .get("data").get("id").asLong()

        // Try to delete as psychologist
        mockMvc.perform(
            delete("/api/v1/admissions/$hospitalizationAdmissionId/psychotherapy-activities/$activityId")
                .header("Authorization", "Bearer $psychologistToken"),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `delete activity should fail for doctor`() {
        // Create an activity first
        val request = CreatePsychotherapyActivityRequest(
            categoryId = categoryId,
            description = "Test activity",
        )
        val createResult = mockMvc.perform(
            post("/api/v1/admissions/$hospitalizationAdmissionId/psychotherapy-activities")
                .header("Authorization", "Bearer $psychologistToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andReturn()

        val activityId = objectMapper.readTree(createResult.response.contentAsString)
            .get("data").get("id").asLong()

        // Try to delete as doctor
        mockMvc.perform(
            delete("/api/v1/admissions/$hospitalizationAdmissionId/psychotherapy-activities/$activityId")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isForbidden)
    }

    // ============ UNAUTHENTICATED / NON-EXISTENT ADMISSION TESTS ============

    @Test
    fun `create activity fails without authentication`() {
        val request = CreatePsychotherapyActivityRequest(
            categoryId = categoryId,
            description = "Test activity",
        )

        mockMvc.perform(
            post("/api/v1/admissions/$hospitalizationAdmissionId/psychotherapy-activities")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `list activities fails without authentication`() {
        mockMvc.perform(
            get("/api/v1/admissions/$hospitalizationAdmissionId/psychotherapy-activities"),
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `create activity for non-existent admission returns 404`() {
        val request = CreatePsychotherapyActivityRequest(
            categoryId = categoryId,
            description = "Test activity",
        )

        mockMvc.perform(
            post("/api/v1/admissions/99999/psychotherapy-activities")
                .header("Authorization", "Bearer $psychologistToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `list activities for non-existent admission returns 404`() {
        mockMvc.perform(
            get("/api/v1/admissions/99999/psychotherapy-activities")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isNotFound)
    }

    // ============ CATEGORY IN USE CHECK TEST ============

    @Test
    fun `delete category should fail when in use by activity`() {
        // Create an activity first
        val request = CreatePsychotherapyActivityRequest(
            categoryId = categoryId,
            description = "Test activity",
        )
        mockMvc.perform(
            post("/api/v1/admissions/$hospitalizationAdmissionId/psychotherapy-activities")
                .header("Authorization", "Bearer $psychologistToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andExpect(status().isCreated)

        // Try to delete the category
        mockMvc.perform(
            delete("/api/v1/admin/psychotherapy-categories/$categoryId")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isBadRequest)
    }
}
