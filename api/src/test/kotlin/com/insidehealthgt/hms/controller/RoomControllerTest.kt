package com.insidehealthgt.hms.controller

import com.insidehealthgt.hms.dto.request.CreateAdmissionRequest
import com.insidehealthgt.hms.dto.request.CreateRoomRequest
import com.insidehealthgt.hms.dto.request.UpdateRoomRequest
import com.insidehealthgt.hms.entity.AdmissionType
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

class RoomControllerTest : AbstractIntegrationTest() {

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

    private fun createValidRoomRequest(): CreateRoomRequest = CreateRoomRequest(
        number = "101",
        type = RoomType.PRIVATE,
        capacity = 1,
    )

    // ============ CREATE ROOM TESTS ============

    @Test
    fun `create room with valid data should return 201`() {
        val request = createValidRoomRequest()

        mockMvc.perform(
            post("/api/v1/rooms")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.number").value("101"))
            .andExpect(jsonPath("$.data.type").value("PRIVATE"))
            .andExpect(jsonPath("$.data.capacity").value(1))
    }

    @Test
    fun `create room should fail with duplicate name`() {
        val request = createValidRoomRequest()

        // Create first room
        mockMvc.perform(
            post("/api/v1/rooms")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andExpect(status().isCreated)

        // Try to create duplicate
        mockMvc.perform(
            post("/api/v1/rooms")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `create room should fail for administrative staff`() {
        val request = createValidRoomRequest()

        mockMvc.perform(
            post("/api/v1/rooms")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `create room should fail with invalid capacity`() {
        val request = createValidRoomRequest().copy(capacity = 0)

        mockMvc.perform(
            post("/api/v1/rooms")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `create room should fail with blank number`() {
        val request = mapOf(
            "number" to "",
            "type" to "PRIVATE",
            "capacity" to 1,
        )

        mockMvc.perform(
            post("/api/v1/rooms")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `create room should fail without authentication`() {
        val request = createValidRoomRequest()

        mockMvc.perform(
            post("/api/v1/rooms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isUnauthorized)
    }

    // ============ LIST ROOMS TESTS ============

    @Test
    fun `list rooms should return all rooms`() {
        // Create rooms
        val room1 = createValidRoomRequest()
        mockMvc.perform(
            post("/api/v1/rooms")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(room1)),
        )

        val room2 = CreateRoomRequest(
            number = "201",
            type = RoomType.SHARED,
            capacity = 4,
        )
        mockMvc.perform(
            post("/api/v1/rooms")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(room2)),
        )

        mockMvc.perform(
            get("/api/v1/rooms")
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data.length()").value(2))
    }

    @Test
    fun `list rooms should be accessible by administrative staff`() {
        mockMvc.perform(
            get("/api/v1/rooms")
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `list rooms should be accessible by doctor`() {
        mockMvc.perform(
            get("/api/v1/rooms")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isOk)
    }

    // ============ GET AVAILABLE ROOMS TESTS ============

    @Test
    fun `list available rooms should return rooms with availability`() {
        val room = createValidRoomRequest().copy(capacity = 2)
        mockMvc.perform(
            post("/api/v1/rooms")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(room)),
        )

        mockMvc.perform(
            get("/api/v1/rooms/available")
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].number").value("101"))
            .andExpect(jsonPath("$.data[0].capacity").value(2))
            .andExpect(jsonPath("$.data[0].availableBeds").value(2))
    }

    @Test
    fun `available rooms should decrease after admission`() {
        // Create room with capacity 2
        val roomRequest = createValidRoomRequest().copy(capacity = 2)
        val roomResult = mockMvc.perform(
            post("/api/v1/rooms")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(roomRequest)),
        ).andReturn()

        val roomId = objectMapper.readTree(roomResult.response.contentAsString)
            .get("data").get("id").asLong()

        // Create patient for admission
        val patientId = createPatient(administrativeStaffToken)
        val triageCodeId = triageCodeRepository.findAll().first().id!!

        // Create admission
        val admissionRequest = CreateAdmissionRequest(
            patientId = patientId,
            triageCodeId = triageCodeId,
            roomId = roomId,
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

        // Check available beds decreased
        mockMvc.perform(
            get("/api/v1/rooms/available")
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].availableBeds").value(1))
    }

    @Test
    fun `full rooms should not appear in available list`() {
        // Create room with capacity 1
        val roomRequest = createValidRoomRequest().copy(capacity = 1)
        val roomResult = mockMvc.perform(
            post("/api/v1/rooms")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(roomRequest)),
        ).andReturn()

        val roomId = objectMapper.readTree(roomResult.response.contentAsString)
            .get("data").get("id").asLong()

        // Create patient for admission
        val patientId = createPatient(administrativeStaffToken)
        val triageCodeId = triageCodeRepository.findAll().first().id!!

        // Create admission to fill the room
        val admissionRequest = CreateAdmissionRequest(
            patientId = patientId,
            triageCodeId = triageCodeId,
            roomId = roomId,
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

        // Room should not appear in available list
        mockMvc.perform(
            get("/api/v1/rooms/available")
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").isEmpty)
    }

    // ============ GET ROOM TESTS ============

    @Test
    fun `get room should return room details`() {
        val request = createValidRoomRequest()
        val createResult = mockMvc.perform(
            post("/api/v1/rooms")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andReturn()

        val roomId = objectMapper.readTree(createResult.response.contentAsString)
            .get("data").get("id").asLong()

        mockMvc.perform(
            get("/api/v1/rooms/$roomId")
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.number").value("101"))
    }

    @Test
    fun `get room should return 404 for non-existent room`() {
        mockMvc.perform(
            get("/api/v1/rooms/99999")
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `get room availability should return room with availability info`() {
        val request = createValidRoomRequest().copy(capacity = 3)
        val createResult = mockMvc.perform(
            post("/api/v1/rooms")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andReturn()

        val roomId = objectMapper.readTree(createResult.response.contentAsString)
            .get("data").get("id").asLong()

        mockMvc.perform(
            get("/api/v1/rooms/$roomId/availability")
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.capacity").value(3))
            .andExpect(jsonPath("$.data.availableBeds").value(3))
    }

    // ============ UPDATE ROOM TESTS ============

    @Test
    fun `update room should update room data`() {
        val createRequest = createValidRoomRequest()
        val createResult = mockMvc.perform(
            post("/api/v1/rooms")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)),
        ).andReturn()

        val roomId = objectMapper.readTree(createResult.response.contentAsString)
            .get("data").get("id").asLong()

        val updateRequest = UpdateRoomRequest(
            number = "101-Updated",
            type = RoomType.SHARED,
            capacity = 4,
        )

        mockMvc.perform(
            put("/api/v1/rooms/$roomId")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.number").value("101-Updated"))
            .andExpect(jsonPath("$.data.type").value("SHARED"))
            .andExpect(jsonPath("$.data.capacity").value(4))
    }

    @Test
    fun `update room should fail for administrative staff`() {
        val createRequest = createValidRoomRequest()
        val createResult = mockMvc.perform(
            post("/api/v1/rooms")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)),
        ).andReturn()

        val roomId = objectMapper.readTree(createResult.response.contentAsString)
            .get("data").get("id").asLong()

        val updateRequest = UpdateRoomRequest(
            number = "101-Updated",
            type = RoomType.SHARED,
            capacity = 4,
        )

        mockMvc.perform(
            put("/api/v1/rooms/$roomId")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `update room should return 404 for non-existent room`() {
        val updateRequest = UpdateRoomRequest(
            number = "101-Updated",
            type = RoomType.SHARED,
            capacity = 4,
        )

        mockMvc.perform(
            put("/api/v1/rooms/99999")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)),
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `update room should fail with duplicate number`() {
        // Create first room
        val room1 = createValidRoomRequest()
        mockMvc.perform(
            post("/api/v1/rooms")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(room1)),
        ).andExpect(status().isCreated)

        // Create second room
        val room2 = CreateRoomRequest(number = "202", type = RoomType.SHARED, capacity = 2)
        val room2Result = mockMvc.perform(
            post("/api/v1/rooms")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(room2)),
        ).andReturn()

        val room2Id = objectMapper.readTree(room2Result.response.contentAsString)
            .get("data").get("id").asLong()

        // Try to update room2 with room1's number
        val updateRequest = UpdateRoomRequest(number = "101", type = RoomType.SHARED, capacity = 2)

        mockMvc.perform(
            put("/api/v1/rooms/$room2Id")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.message").value("Room with number '101' already exists"))
    }

    @Test
    fun `update room should fail when reducing capacity below active admissions`() {
        // Create room with capacity 2
        val roomRequest = createValidRoomRequest().copy(capacity = 2)
        val roomResult = mockMvc.perform(
            post("/api/v1/rooms")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(roomRequest)),
        ).andReturn()

        val roomId = objectMapper.readTree(roomResult.response.contentAsString)
            .get("data").get("id").asLong()

        val triageCodeId = triageCodeRepository.findAll().first().id!!

        // Create first patient and admission
        val patient1Id = createPatient(administrativeStaffToken)
        val admissionRequest1 = CreateAdmissionRequest(
            patientId = patient1Id,
            triageCodeId = triageCodeId,
            roomId = roomId,
            treatingPhysicianId = doctorUser.id!!,
            admissionDate = LocalDateTime.now(),
            type = AdmissionType.HOSPITALIZATION,
            inventory = null,
        )
        mockMvc.perform(
            post("/api/v1/admissions")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(admissionRequest1)),
        ).andExpect(status().isCreated)

        // Create second patient and admission to fill the room
        val patient2Id = createSecondPatient(administrativeStaffToken)
        val admissionRequest2 = admissionRequest1.copy(patientId = patient2Id)
        mockMvc.perform(
            post("/api/v1/admissions")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(admissionRequest2)),
        ).andExpect(status().isCreated)

        // Try to reduce capacity to 1 (below the 2 active admissions)
        val updateRequest = UpdateRoomRequest(number = "101", type = RoomType.PRIVATE, capacity = 1)

        mockMvc.perform(
            put("/api/v1/rooms/$roomId")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.message").value("Cannot reduce capacity to 1. Room has 2 active admissions."))
    }

    // ============ DELETE ROOM TESTS ============

    @Test
    fun `delete room should soft delete room`() {
        val request = createValidRoomRequest()
        val createResult = mockMvc.perform(
            post("/api/v1/rooms")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andReturn()

        val roomId = objectMapper.readTree(createResult.response.contentAsString)
            .get("data").get("id").asLong()

        mockMvc.perform(
            delete("/api/v1/rooms/$roomId")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)

        // Should not be found after deletion
        mockMvc.perform(
            get("/api/v1/rooms/$roomId")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `delete room should fail when room has active admissions`() {
        // Create room
        val roomRequest = createValidRoomRequest()
        val roomResult = mockMvc.perform(
            post("/api/v1/rooms")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(roomRequest)),
        ).andReturn()

        val roomId = objectMapper.readTree(roomResult.response.contentAsString)
            .get("data").get("id").asLong()

        // Create patient for admission
        val patientId = createPatient(administrativeStaffToken)
        val triageCodeId = triageCodeRepository.findAll().first().id!!

        // Create admission
        val admissionRequest = CreateAdmissionRequest(
            patientId = patientId,
            triageCodeId = triageCodeId,
            roomId = roomId,
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

        // Try to delete room - should fail
        mockMvc.perform(
            delete("/api/v1/rooms/$roomId")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `delete room should fail for administrative staff`() {
        val request = createValidRoomRequest()
        val createResult = mockMvc.perform(
            post("/api/v1/rooms")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andReturn()

        val roomId = objectMapper.readTree(createResult.response.contentAsString)
            .get("data").get("id").asLong()

        mockMvc.perform(
            delete("/api/v1/rooms/$roomId")
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )
            .andExpect(status().isForbidden)
    }
}
