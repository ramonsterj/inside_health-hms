package com.insidehealthgt.hms.controller

import com.insidehealthgt.hms.dto.request.CreateAdmissionRequest
import com.insidehealthgt.hms.dto.request.UpdateAdmissionRequest
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

class AdmissionControllerTest : AbstractIntegrationTest() {

    private lateinit var adminToken: String
    private lateinit var administrativeStaffToken: String
    private lateinit var doctorToken: String
    private lateinit var doctorUser: User
    private var patientId: Long = 0
    private var triageCodeId: Long = 0
    private var roomId: Long = 0

    @BeforeEach
    fun setUp() {
        val (_, adminTkn) = createAdminUser()
        adminToken = adminTkn

        val (_, staffTkn) = createAdminStaffUser()
        administrativeStaffToken = staffTkn

        val (doctorUsr, doctorTkn) = createDoctorUser()
        doctorUser = doctorUsr
        doctorToken = doctorTkn

        patientId = createPatient(administrativeStaffToken)

        val triageCode = triageCodeRepository.findAll().first()
        triageCodeId = triageCode.id!!

        val room = Room(
            number = "TEST-101",
            type = RoomType.PRIVATE,
            capacity = 2,
        )
        roomRepository.save(room)
        roomId = room.id!!
    }

    private fun createValidAdmissionRequest(): CreateAdmissionRequest = CreateAdmissionRequest(
        patientId = patientId,
        triageCodeId = triageCodeId,
        roomId = roomId,
        treatingPhysicianId = doctorUser.id!!,
        admissionDate = LocalDateTime.now(),
        type = AdmissionType.HOSPITALIZATION,
        inventory = "Wallet, phone, glasses",
    )

    // ============ CREATE ADMISSION TESTS ============

    @Test
    fun `create admission with valid data should return 201`() {
        val request = createValidAdmissionRequest()

        mockMvc.perform(
            post("/api/v1/admissions")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.patient.firstName").value("Juan"))
            .andExpect(jsonPath("$.data.status").value("ACTIVE"))
            .andExpect(jsonPath("$.data.inventory").value("Wallet, phone, glasses"))
            .andExpect(jsonPath("$.data.treatingPhysician.firstName").value("Dr. Maria"))
            .andExpect(jsonPath("$.data.createdBy").exists())
    }

    @Test
    fun `create admission should fail with non-existent patient`() {
        val request = createValidAdmissionRequest().copy(patientId = 99999)

        mockMvc.perform(
            post("/api/v1/admissions")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `create admission should fail with non-existent triage code`() {
        val request = createValidAdmissionRequest().copy(triageCodeId = 99999)

        mockMvc.perform(
            post("/api/v1/admissions")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `create admission should fail with non-existent room`() {
        val request = createValidAdmissionRequest().copy(roomId = 99999)

        mockMvc.perform(
            post("/api/v1/admissions")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `create admission should fail with non-doctor treating physician`() {
        val adminStaffRole = roleRepository.findByCode("ADMINISTRATIVE_STAFF")!!
        val nonDoctorUser = User(
            username = "nondoctor",
            email = "nondoctor@example.com",
            passwordHash = passwordEncoder.encode("password123")!!,
            firstName = "Nurse",
            lastName = "Jones",
        )
        nonDoctorUser.roles.add(adminStaffRole)
        userRepository.save(nonDoctorUser)

        val request = createValidAdmissionRequest().copy(treatingPhysicianId = nonDoctorUser.id!!)

        mockMvc.perform(
            post("/api/v1/admissions")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.message").value("Treating physician must have the DOCTOR role"))
    }

    @Test
    fun `create admission should fail when room is full`() {
        val request1 = createValidAdmissionRequest()
        mockMvc.perform(
            post("/api/v1/admissions")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)),
        ).andExpect(status().isCreated)

        val patient2Id = createSecondPatient(administrativeStaffToken)
        val request2 = createValidAdmissionRequest().copy(patientId = patient2Id)
        mockMvc.perform(
            post("/api/v1/admissions")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)),
        ).andExpect(status().isCreated)

        val patient3Id = createThirdPatient(administrativeStaffToken)
        val request3 = createValidAdmissionRequest().copy(patientId = patient3Id)
        mockMvc.perform(
            post("/api/v1/admissions")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request3)),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.message").value("Room 'TEST-101' is full. No available beds."))
    }

    @Test
    fun `create admission should fail for doctor role`() {
        val request = createValidAdmissionRequest()

        mockMvc.perform(
            post("/api/v1/admissions")
                .header("Authorization", "Bearer $doctorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `create admission should fail without authentication`() {
        val request = createValidAdmissionRequest()

        mockMvc.perform(
            post("/api/v1/admissions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `create admission should fail when patient already has active admission`() {
        val request = createValidAdmissionRequest()
        mockMvc.perform(
            post("/api/v1/admissions")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andExpect(status().isCreated)

        mockMvc.perform(
            post("/api/v1/admissions")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.message").value("Patient already has an active admission"))
    }

    // ============ LIST ADMISSIONS TESTS ============

    @Test
    fun `list admissions should return paginated results`() {
        val request = createValidAdmissionRequest()
        mockMvc.perform(
            post("/api/v1/admissions")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )

        mockMvc.perform(
            get("/api/v1/admissions")
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content").isArray)
            .andExpect(jsonPath("$.data.content[0].patient.firstName").value("Juan"))
    }

    @Test
    fun `list admissions should filter by status`() {
        val request = createValidAdmissionRequest()
        val createResult = mockMvc.perform(
            post("/api/v1/admissions")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andReturn()

        val admissionId = objectMapper.readTree(createResult.response.contentAsString)
            .get("data").get("id").asLong()

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/discharge")
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )

        mockMvc.perform(
            get("/api/v1/admissions")
                .param("status", "ACTIVE")
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content").isEmpty)

        mockMvc.perform(
            get("/api/v1/admissions")
                .param("status", "DISCHARGED")
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content[0].status").value("DISCHARGED"))
    }

    // ============ GET ADMISSION TESTS ============

    @Test
    fun `get admission should return admission details`() {
        val request = createValidAdmissionRequest()
        val createResult = mockMvc.perform(
            post("/api/v1/admissions")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andReturn()

        val admissionId = objectMapper.readTree(createResult.response.contentAsString)
            .get("data").get("id").asLong()

        mockMvc.perform(
            get("/api/v1/admissions/$admissionId")
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.patient.firstName").value("Juan"))
            .andExpect(jsonPath("$.data.triageCode").exists())
            .andExpect(jsonPath("$.data.room").exists())
            .andExpect(jsonPath("$.data.treatingPhysician").exists())
    }

    @Test
    fun `get admission should return 404 for non-existent admission`() {
        mockMvc.perform(
            get("/api/v1/admissions/99999")
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )
            .andExpect(status().isNotFound)
    }

    // ============ UPDATE ADMISSION TESTS ============

    @Test
    fun `update admission should update admission data`() {
        val createRequest = createValidAdmissionRequest()
        val createResult = mockMvc.perform(
            post("/api/v1/admissions")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)),
        ).andReturn()

        val admissionId = objectMapper.readTree(createResult.response.contentAsString)
            .get("data").get("id").asLong()

        val updateRequest = UpdateAdmissionRequest(
            triageCodeId = triageCodeId,
            roomId = roomId,
            treatingPhysicianId = doctorUser.id!!,
            inventory = "Updated inventory: Wallet, phone, keys",
        )

        mockMvc.perform(
            put("/api/v1/admissions/$admissionId")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.inventory").value("Updated inventory: Wallet, phone, keys"))
    }

    @Test
    fun `update admission should fail for discharged admission`() {
        val createRequest = createValidAdmissionRequest()
        val createResult = mockMvc.perform(
            post("/api/v1/admissions")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)),
        ).andReturn()

        val admissionId = objectMapper.readTree(createResult.response.contentAsString)
            .get("data").get("id").asLong()

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/discharge")
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )

        val updateRequest = UpdateAdmissionRequest(
            triageCodeId = triageCodeId,
            roomId = roomId,
            treatingPhysicianId = doctorUser.id!!,
            inventory = "Should fail",
        )

        mockMvc.perform(
            put("/api/v1/admissions/$admissionId")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.message").value("Cannot update a discharged admission"))
    }

    // ============ DELETE ADMISSION TESTS ============

    @Test
    fun `delete admission should soft delete admission`() {
        val request = createValidAdmissionRequest()
        val createResult = mockMvc.perform(
            post("/api/v1/admissions")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andReturn()

        val admissionId = objectMapper.readTree(createResult.response.contentAsString)
            .get("data").get("id").asLong()

        mockMvc.perform(
            delete("/api/v1/admissions/$admissionId")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)

        mockMvc.perform(
            get("/api/v1/admissions/$admissionId")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `delete admission should fail for administrative staff`() {
        val request = createValidAdmissionRequest()
        val createResult = mockMvc.perform(
            post("/api/v1/admissions")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andReturn()

        val admissionId = objectMapper.readTree(createResult.response.contentAsString)
            .get("data").get("id").asLong()

        mockMvc.perform(
            delete("/api/v1/admissions/$admissionId")
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )
            .andExpect(status().isForbidden)
    }

    // ============ SEARCH PATIENTS TESTS ============

    @Test
    fun `search patients should return matching patients`() {
        mockMvc.perform(
            get("/api/v1/admissions/patients/search")
                .param("q", "Juan")
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].firstName").value("Juan"))
    }

    @Test
    fun `search patients should return empty for no matches`() {
        mockMvc.perform(
            get("/api/v1/admissions/patients/search")
                .param("q", "NonExistent")
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").isEmpty)
    }

    // ============ LIST DOCTORS TESTS ============

    @Test
    fun `list doctors should return doctors only`() {
        mockMvc.perform(
            get("/api/v1/admissions/doctors")
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].firstName").value("Dr. Maria"))
            .andExpect(jsonPath("$.data[0].salutation").value("DR"))
    }
}
