package com.insidehealthgt.hms.controller

import com.insidehealthgt.hms.dto.request.CreateAdmissionRequest
import com.insidehealthgt.hms.dto.request.CreateTriageCodeRequest
import com.insidehealthgt.hms.dto.request.UpdateTriageCodeRequest
import com.insidehealthgt.hms.entity.AdmissionType
import com.insidehealthgt.hms.entity.Room
import com.insidehealthgt.hms.entity.RoomGender
import com.insidehealthgt.hms.entity.RoomType
import com.insidehealthgt.hms.entity.User
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

class TriageCodeControllerTest : AbstractIntegrationTest() {

    private lateinit var adminToken: String
    private lateinit var administrativeStaffToken: String
    private lateinit var doctorToken: String
    private lateinit var doctorUser: User

    @BeforeEach
    fun setUp() {
        val (_, adminTkn) = createAdminUser()
        adminToken = adminTkn

        val (_, staffTkn) = createAdminStaffUser()
        administrativeStaffToken = staffTkn

        val (docUsr, docTkn) = createDoctorUser()
        doctorUser = docUsr
        doctorToken = docTkn
    }

    private fun createValidTriageCodeRequest(): CreateTriageCodeRequest = CreateTriageCodeRequest(
        code = "X",
        color = "#FF00FF",
        description = "Test triage code",
        displayOrder = 99,
    )

    // ============ CREATE TRIAGE CODE TESTS ============

    @Test
    fun `create triage code with valid data should return 201`() {
        val request = createValidTriageCodeRequest()

        mockMvc.perform(
            post("/api/v1/triage-codes")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.code").value("X"))
            .andExpect(jsonPath("$.data.color").value("#FF00FF"))
            .andExpect(jsonPath("$.data.description").value("Test triage code"))
            .andExpect(jsonPath("$.data.displayOrder").value(99))
    }

    @Test
    fun `create triage code should fail with duplicate code`() {
        val request = createValidTriageCodeRequest()

        // Create first triage code
        mockMvc.perform(
            post("/api/v1/triage-codes")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andExpect(status().isCreated)

        // Try to create duplicate
        mockMvc.perform(
            post("/api/v1/triage-codes")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `create triage code should fail for administrative staff`() {
        val request = createValidTriageCodeRequest()

        mockMvc.perform(
            post("/api/v1/triage-codes")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `create triage code should fail with invalid color format`() {
        val request = mapOf(
            "code" to "X",
            "color" to "invalid",
            "description" to "Test",
            "displayOrder" to 99,
        )

        mockMvc.perform(
            post("/api/v1/triage-codes")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `create triage code should fail with blank code`() {
        val request = mapOf(
            "code" to "",
            "color" to "#FF00FF",
            "description" to "Test",
            "displayOrder" to 99,
        )

        mockMvc.perform(
            post("/api/v1/triage-codes")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `create triage code should fail without authentication`() {
        val request = createValidTriageCodeRequest()

        mockMvc.perform(
            post("/api/v1/triage-codes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isUnauthorized)
    }

    // ============ LIST TRIAGE CODES TESTS ============

    @Test
    fun `list triage codes should return all codes sorted by display order`() {
        // Seeded triage codes from V021 migration should be present
        mockMvc.perform(
            get("/api/v1/triage-codes")
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data[0].code").value("A")) // First by display order
    }

    @Test
    fun `list triage codes should be accessible by administrative staff`() {
        mockMvc.perform(
            get("/api/v1/triage-codes")
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `list triage codes should be accessible by doctor`() {
        mockMvc.perform(
            get("/api/v1/triage-codes")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isOk)
    }

    // ============ GET TRIAGE CODE TESTS ============

    @Test
    fun `get triage code should return triage code details`() {
        val triageCode = triageCodeRepository.findAll().first()

        mockMvc.perform(
            get("/api/v1/triage-codes/${triageCode.id}")
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.code").exists())
            .andExpect(jsonPath("$.data.color").exists())
    }

    @Test
    fun `get triage code should return 404 for non-existent code`() {
        mockMvc.perform(
            get("/api/v1/triage-codes/99999")
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )
            .andExpect(status().isNotFound)
    }

    // ============ UPDATE TRIAGE CODE TESTS ============

    @Test
    fun `update triage code should update triage code data`() {
        val request = createValidTriageCodeRequest()
        val createResult = mockMvc.perform(
            post("/api/v1/triage-codes")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andReturn()

        val triageCodeId = objectMapper.readTree(createResult.response.contentAsString)
            .get("data").get("id").asLong()

        val updateRequest = UpdateTriageCodeRequest(
            code = "X-Updated",
            color = "#00FF00",
            description = "Updated description",
            displayOrder = 100,
        )

        mockMvc.perform(
            put("/api/v1/triage-codes/$triageCodeId")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.code").value("X-Updated"))
            .andExpect(jsonPath("$.data.color").value("#00FF00"))
            .andExpect(jsonPath("$.data.description").value("Updated description"))
            .andExpect(jsonPath("$.data.displayOrder").value(100))
    }

    @Test
    fun `update triage code should fail for administrative staff`() {
        val request = createValidTriageCodeRequest()
        val createResult = mockMvc.perform(
            post("/api/v1/triage-codes")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andReturn()

        val triageCodeId = objectMapper.readTree(createResult.response.contentAsString)
            .get("data").get("id").asLong()

        val updateRequest = UpdateTriageCodeRequest(
            code = "X-Updated",
            color = "#00FF00",
            description = "Updated",
            displayOrder = 100,
        )

        mockMvc.perform(
            put("/api/v1/triage-codes/$triageCodeId")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `update triage code should return 404 for non-existent code`() {
        val updateRequest = UpdateTriageCodeRequest(
            code = "X-Updated",
            color = "#00FF00",
            description = "Updated",
            displayOrder = 100,
        )

        mockMvc.perform(
            put("/api/v1/triage-codes/99999")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)),
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `update triage code should fail with duplicate code`() {
        // Create first triage code
        val request1 = createValidTriageCodeRequest()
        mockMvc.perform(
            post("/api/v1/triage-codes")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)),
        ).andExpect(status().isCreated)

        // Create second triage code
        val request2 = CreateTriageCodeRequest(
            code = "Y",
            color = "#00FF00",
            description = "Second code",
            displayOrder = 100,
        )
        val result2 = mockMvc.perform(
            post("/api/v1/triage-codes")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)),
        ).andReturn()

        val code2Id = objectMapper.readTree(result2.response.contentAsString)
            .get("data").get("id").asLong()

        // Try to update second code to have same code as first
        val updateRequest = UpdateTriageCodeRequest(
            code = "X",
            color = "#00FF00",
            description = "Duplicate",
            displayOrder = 100,
        )

        mockMvc.perform(
            put("/api/v1/triage-codes/$code2Id")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.message").value("Triage code with code 'X' already exists"))
    }

    // ============ DELETE TRIAGE CODE TESTS ============

    @Test
    fun `delete triage code should soft delete code`() {
        val request = createValidTriageCodeRequest()
        val createResult = mockMvc.perform(
            post("/api/v1/triage-codes")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andReturn()

        val triageCodeId = objectMapper.readTree(createResult.response.contentAsString)
            .get("data").get("id").asLong()

        mockMvc.perform(
            delete("/api/v1/triage-codes/$triageCodeId")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)

        // Should not be found after deletion
        mockMvc.perform(
            get("/api/v1/triage-codes/$triageCodeId")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `delete triage code should fail when in use by active admission`() {
        // Get existing triage code from seeded data
        val triageCode = triageCodeRepository.findAll().first()

        // Create room for admission
        val room = Room(
            number = "101",
            type = RoomType.PRIVATE,
            gender = RoomGender.FEMALE,
            capacity = 1,
        )
        roomRepository.save(room)

        val patientId = createPatient(administrativeStaffToken)

        // Create admission using the triage code
        val admissionRequest = CreateAdmissionRequest(
            patientId = patientId,
            triageCodeId = triageCode.id!!,
            roomId = room.id!!,
            treatingPhysicianId = doctorUser.id!!,
            admissionDate = LocalDateTime.now(),
            type = AdmissionType.HOSPITALIZATION,
            inventory = null,
        )

        mockMvc.perform(
            post("/api/v1/admissions")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(admissionRequest)),
        ).andExpect(status().isCreated)

        // Try to delete triage code - should fail
        mockMvc.perform(
            delete("/api/v1/triage-codes/${triageCode.id}")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `delete triage code should fail for administrative staff`() {
        val request = createValidTriageCodeRequest()
        val createResult = mockMvc.perform(
            post("/api/v1/triage-codes")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andReturn()

        val triageCodeId = objectMapper.readTree(createResult.response.contentAsString)
            .get("data").get("id").asLong()

        mockMvc.perform(
            delete("/api/v1/triage-codes/$triageCodeId")
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )
            .andExpect(status().isForbidden)
    }
}
