package com.insidehealthgt.hms.controller

import com.insidehealthgt.hms.dto.request.CreateAdmissionRequest
import com.insidehealthgt.hms.dto.request.UpdateAdmissionRequest
import com.insidehealthgt.hms.entity.AdmissionType
import com.insidehealthgt.hms.entity.Room
import com.insidehealthgt.hms.entity.RoomGender
import com.insidehealthgt.hms.entity.RoomType
import com.insidehealthgt.hms.entity.User
import org.hamcrest.Matchers.containsInAnyOrder
import org.hamcrest.Matchers.hasItem
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

@Suppress("LargeClass")
class AdmissionControllerTest : AbstractIntegrationTest() {

    private lateinit var adminToken: String
    private lateinit var administrativeStaffToken: String
    private lateinit var doctorToken: String
    private lateinit var doctorUser: User
    private lateinit var residentToken: String
    private lateinit var residentUser: User
    private lateinit var psychologistToken: String
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

        // Only a resident (auto-bound to self) or an admin (who picks a resident)
        // may register an admission. Most creation POSTs below go through this
        // resident; admin-staff keeps driving the read/update flows. Discharge is
        // restricted to ADMINISTRADOR / MEDICO_RESIDENTE (see admission:discharge).
        val (residentUsr, residentTkn) = createResidentUser()
        residentUser = residentUsr
        residentToken = residentTkn

        val (_, psychTkn) = createPsychologistUser()
        psychologistToken = psychTkn

        patientId = createPatient(administrativeStaffToken)

        val triageCode = triageCodeRepository.findAll().first()
        triageCodeId = triageCode.id!!

        val room = Room(
            number = "TEST-101",
            type = RoomType.PRIVATE,
            gender = RoomGender.FEMALE,
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
                .header("Authorization", "Bearer $residentToken")
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
                .header("Authorization", "Bearer $residentToken")
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
                .header("Authorization", "Bearer $residentToken")
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
                .header("Authorization", "Bearer $residentToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `create admission should fail with non-doctor treating physician`() {
        val adminStaffRole = roleRepository.findByCode("PERSONAL_ADMINISTRATIVO")!!
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
                .header("Authorization", "Bearer $residentToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.message").value("Treating physician must have the MEDICO role"))
    }

    @Test
    fun `create admission should fail when room is full`() {
        val request1 = createValidAdmissionRequest()
        mockMvc.perform(
            post("/api/v1/admissions")
                .header("Authorization", "Bearer $residentToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)),
        ).andExpect(status().isCreated)

        val patient2Id = createSecondPatient(administrativeStaffToken)
        val request2 = createValidAdmissionRequest().copy(patientId = patient2Id)
        mockMvc.perform(
            post("/api/v1/admissions")
                .header("Authorization", "Bearer $residentToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)),
        ).andExpect(status().isCreated)

        val patient3Id = createThirdPatient(administrativeStaffToken)
        val request3 = createValidAdmissionRequest().copy(patientId = patient3Id)
        mockMvc.perform(
            post("/api/v1/admissions")
                .header("Authorization", "Bearer $residentToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request3)),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.message").value("Room 'TEST-101' is full. No available beds."))
    }

    @Test
    fun `create hospitalization should ignore non-hospitalization admissions when checking room capacity`() {
        val singleBedRoom = roomRepository.save(
            Room(
                number = "TEST-102",
                type = RoomType.PRIVATE,
                gender = RoomGender.FEMALE,
                capacity = 1,
            ),
        )

        val ambulatoryRequest = createValidAdmissionRequest().copy(
            roomId = singleBedRoom.id!!,
            type = AdmissionType.AMBULATORY,
        )
        mockMvc.perform(
            post("/api/v1/admissions")
                .header("Authorization", "Bearer $residentToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ambulatoryRequest)),
        ).andExpect(status().isCreated)

        val hospitalizationRequest = createValidAdmissionRequest().copy(
            patientId = createSecondPatient(administrativeStaffToken),
            roomId = singleBedRoom.id!!,
            type = AdmissionType.HOSPITALIZATION,
        )
        mockMvc.perform(
            post("/api/v1/admissions")
                .header("Authorization", "Bearer $residentToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(hospitalizationRequest)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.room.id").value(singleBedRoom.id))
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
    fun `create admission as resident sets resident to current user`() {
        val request = createValidAdmissionRequest()

        mockMvc.perform(
            post("/api/v1/admissions")
                .header("Authorization", "Bearer $residentToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.resident.id").value(residentUser.id))
            .andExpect(jsonPath("$.data.resident.firstName").value(residentUser.firstName))
            .andExpect(jsonPath("$.data.treatingPhysician.id").value(doctorUser.id))
    }

    @Test
    fun `create admission as admin with residentId sets that resident`() {
        val request = createValidAdmissionRequest().copy(residentId = residentUser.id)

        mockMvc.perform(
            post("/api/v1/admissions")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.resident.id").value(residentUser.id))
            .andExpect(jsonPath("$.data.treatingPhysician.id").value(doctorUser.id))
    }

    @Test
    fun `create admission as admin without residentId returns 400`() {
        val request = createValidAdmissionRequest()

        mockMvc.perform(
            post("/api/v1/admissions")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)
            .andExpect(
                jsonPath("$.error.message")
                    .value("An administrator must select the resident doctor for the admission"),
            )
    }

    @Test
    fun `create admission as admin with non-resident residentId returns 400`() {
        // doctorUser carries DOCTOR but not RESIDENT_DOCTOR.
        val request = createValidAdmissionRequest().copy(residentId = doctorUser.id)

        mockMvc.perform(
            post("/api/v1/admissions")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)
            .andExpect(
                jsonPath("$.error.message")
                    .value("The selected user does not have the resident doctor role"),
            )
    }

    @Test
    fun `create admission as doctor without resident role returns 400`() {
        val (_, plainStaffToken) = createUserWithRole(
            roleCode = "PERSONAL_ADMINISTRATIVO",
            username = "plainstaff",
            email = "plainstaff@example.com",
            password = "password123",
        )
        val request = createValidAdmissionRequest()

        mockMvc.perform(
            post("/api/v1/admissions")
                .header("Authorization", "Bearer $plainStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)
            .andExpect(
                jsonPath("$.error.message")
                    .value("Only users with the MEDICO_RESIDENTE role can register admissions"),
            )
    }

    @Test
    fun `create admission should fail when patient already has active admission`() {
        val request = createValidAdmissionRequest()
        mockMvc.perform(
            post("/api/v1/admissions")
                .header("Authorization", "Bearer $residentToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andExpect(status().isCreated)

        mockMvc.perform(
            post("/api/v1/admissions")
                .header("Authorization", "Bearer $residentToken")
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
                .header("Authorization", "Bearer $residentToken")
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
                .header("Authorization", "Bearer $residentToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andReturn()

        val admissionId = objectMapper.readTree(createResult.response.contentAsString)
            .get("data").get("id").asLong()

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/discharge")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"dischargeNote": "Discharged (test)"}"""),
        ).andExpect(status().isOk)

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

    @Test
    fun `list admissions should only return active admissions for psychologist`() {
        val request = createValidAdmissionRequest()
        val createResult = mockMvc.perform(
            post("/api/v1/admissions")
                .header("Authorization", "Bearer $residentToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andReturn()

        val admissionId = objectMapper.readTree(createResult.response.contentAsString)
            .get("data").get("id").asLong()

        // Psychologist should see the active admission
        mockMvc.perform(
            get("/api/v1/admissions")
                .header("Authorization", "Bearer $psychologistToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content[0].status").value("ACTIVE"))

        // Discharge the patient
        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/discharge")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"dischargeNote": "Discharged (test)"}"""),
        ).andExpect(status().isOk)

        // Psychologist should no longer see the discharged admission
        mockMvc.perform(
            get("/api/v1/admissions")
                .header("Authorization", "Bearer $psychologistToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content").isEmpty)
    }

    @Test
    fun `list admissions for resident returns all admissions, not only their own`() {
        // Admission registered by the setUp resident (resident_id = that resident).
        val request = createValidAdmissionRequest()
        mockMvc.perform(
            post("/api/v1/admissions")
                .header("Authorization", "Bearer $residentToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andExpect(status().isCreated)

        val (_, otherResidentToken) = createUserWithRole(
            roleCode = "MEDICO_RESIDENTE",
            username = "resident2",
            email = "resident2@example.com",
            password = "password123",
        )

        // A different resident sees this admission even though they didn't admit it
        mockMvc.perform(
            get("/api/v1/admissions")
                .header("Authorization", "Bearer $otherResidentToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content").isNotEmpty)
            .andExpect(jsonPath("$.data.content[0].patient.firstName").value("Juan"))
    }

    @Test
    fun `get admission should deny psychologist access to discharged admission`() {
        val request = createValidAdmissionRequest()
        val createResult = mockMvc.perform(
            post("/api/v1/admissions")
                .header("Authorization", "Bearer $residentToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andReturn()

        val admissionId = objectMapper.readTree(createResult.response.contentAsString)
            .get("data").get("id").asLong()

        // Psychologist can access active admission
        mockMvc.perform(
            get("/api/v1/admissions/$admissionId")
                .header("Authorization", "Bearer $psychologistToken"),
        )
            .andExpect(status().isOk)

        // Discharge the patient
        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/discharge")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"dischargeNote": "Discharged (test)"}"""),
        ).andExpect(status().isOk)

        // Psychologist cannot access discharged admission
        mockMvc.perform(
            get("/api/v1/admissions/$admissionId")
                .header("Authorization", "Bearer $psychologistToken"),
        )
            .andExpect(status().isForbidden)
    }

    // ============ GET ADMISSION TESTS ============

    @Test
    fun `get admission should return admission details`() {
        val request = createValidAdmissionRequest()
        val createResult = mockMvc.perform(
            post("/api/v1/admissions")
                .header("Authorization", "Bearer $residentToken")
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
                .header("Authorization", "Bearer $residentToken")
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
    fun `resident discharge permission should not allow admission update`() {
        val createRequest = createValidAdmissionRequest()
        val createResult = mockMvc.perform(
            post("/api/v1/admissions")
                .header("Authorization", "Bearer $residentToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)),
        ).andReturn()

        val admissionId = objectMapper.readTree(createResult.response.contentAsString)
            .get("data").get("id").asLong()

        val updateRequest = UpdateAdmissionRequest(
            triageCodeId = triageCodeId,
            roomId = roomId,
            treatingPhysicianId = doctorUser.id!!,
            inventory = "Resident should not update metadata",
        )

        mockMvc.perform(
            put("/api/v1/admissions/$admissionId")
                .header("Authorization", "Bearer $residentToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `update admission should fail for discharged admission`() {
        val createRequest = createValidAdmissionRequest()
        val createResult = mockMvc.perform(
            post("/api/v1/admissions")
                .header("Authorization", "Bearer $residentToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)),
        ).andReturn()

        val admissionId = objectMapper.readTree(createResult.response.contentAsString)
            .get("data").get("id").asLong()

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/discharge")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"dischargeNote": "Discharged (test)"}"""),
        ).andExpect(status().isOk)

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
                .header("Authorization", "Bearer $residentToken")
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
                .header("Authorization", "Bearer $residentToken")
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

    // ============ LIST RESIDENTS TESTS ============

    @Test
    fun `list residents should return resident doctors only`() {
        mockMvc.perform(
            get("/api/v1/admissions/residents")
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].id").value(residentUser.id))
            .andExpect(jsonPath("$.data[0].firstName").value(residentUser.firstName))
    }

    // ============ PATIENT ADMISSIONS HISTORY TESTS ============

    private fun patientHistoryUrl(id: Long) = "/api/v1/admissions/patients/$id/admissions"

    @Test
    fun `patient admissions history returns all admissions most-recent-first`() {
        val now = LocalDateTime.now()
        val oldest = createAdmission(
            token = residentToken,
            patientId = patientId,
            doctorId = doctorUser.id!!,
            type = AdmissionType.AMBULATORY,
            admissionDate = now.minusDays(10),
        )
        dischargeAdmission(oldest, adminToken)
        val middle = createAdmission(
            token = residentToken,
            patientId = patientId,
            doctorId = doctorUser.id!!,
            type = AdmissionType.AMBULATORY,
            admissionDate = now.minusDays(5),
        )
        dischargeAdmission(middle, adminToken)
        // Newest, still active (HOSPITALIZATION needs a room + triage code).
        val newest = createAdmission(
            token = residentToken,
            patientId = patientId,
            doctorId = doctorUser.id!!,
            type = AdmissionType.HOSPITALIZATION,
            roomId = roomId,
            triageCodeId = triageCodeId,
            admissionDate = now,
        )

        mockMvc.perform(
            get(patientHistoryUrl(patientId)).header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content.length()").value(3))
            .andExpect(jsonPath("$.data.page.totalElements").value(3))
            // Most-recent-first: active newest, then discharged middle, then discharged oldest.
            .andExpect(jsonPath("$.data.content[0].id").value(newest))
            .andExpect(jsonPath("$.data.content[0].status").value("ACTIVE"))
            .andExpect(jsonPath("$.data.content[1].id").value(middle))
            .andExpect(jsonPath("$.data.content[1].status").value("DISCHARGED"))
            .andExpect(jsonPath("$.data.content[2].id").value(oldest))
    }

    @Test
    fun `patient admissions history returns all admission types without filtering`() {
        val now = LocalDateTime.now()
        val electroshock = createAdmission(
            token = residentToken,
            patientId = patientId,
            doctorId = doctorUser.id!!,
            type = AdmissionType.ELECTROSHOCK_THERAPY,
            admissionDate = now.minusDays(2),
        )
        dischargeAdmission(electroshock, adminToken)
        createAdmission(
            token = residentToken,
            patientId = patientId,
            doctorId = doctorUser.id!!,
            type = AdmissionType.HOSPITALIZATION,
            roomId = roomId,
            triageCodeId = triageCodeId,
            admissionDate = now,
        )

        mockMvc.perform(
            get(patientHistoryUrl(patientId)).header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content.length()").value(2))
            .andExpect(
                jsonPath(
                    "$.data.content[*].type",
                    containsInAnyOrder("HOSPITALIZATION", "ELECTROSHOCK_THERAPY"),
                ),
            )
    }

    @Test
    fun `patient admissions history returns empty page for patient with no admissions`() {
        val freshPatient = createSecondPatient(administrativeStaffToken)

        mockMvc.perform(
            get(patientHistoryUrl(freshPatient)).header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content").isEmpty)
            .andExpect(jsonPath("$.data.page.totalElements").value(0))
    }

    @Test
    fun `patient admissions history returns 404 for unknown patient`() {
        mockMvc.perform(
            get(patientHistoryUrl(99999)).header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `patient admissions history honors pagination parameters`() {
        val now = LocalDateTime.now()
        val a1 = createAdmission(
            token = residentToken,
            patientId = patientId,
            doctorId = doctorUser.id!!,
            type = AdmissionType.AMBULATORY,
            admissionDate = now.minusDays(3),
        )
        dischargeAdmission(a1, adminToken)
        val a2 = createAdmission(
            token = residentToken,
            patientId = patientId,
            doctorId = doctorUser.id!!,
            type = AdmissionType.AMBULATORY,
            admissionDate = now.minusDays(2),
        )
        dischargeAdmission(a2, adminToken)
        createAdmission(
            token = residentToken,
            patientId = patientId,
            doctorId = doctorUser.id!!,
            type = AdmissionType.AMBULATORY,
            admissionDate = now,
        )

        mockMvc.perform(
            get(patientHistoryUrl(patientId))
                .param("page", "0").param("size", "2")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content.length()").value(2))
            .andExpect(jsonPath("$.data.page.totalElements").value(3))

        mockMvc.perform(
            get(patientHistoryUrl(patientId))
                .param("page", "1").param("size", "2")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content.length()").value(1))
    }

    @Test
    fun `patient admissions history allows assigned doctor full history but denies unassigned doctor`() {
        val now = LocalDateTime.now()
        val discharged = createAdmission(
            token = residentToken,
            patientId = patientId,
            doctorId = doctorUser.id!!,
            type = AdmissionType.AMBULATORY,
            admissionDate = now.minusDays(3),
        )
        dischargeAdmission(discharged, adminToken)
        // Active admission with doctorUser as treating physician → doctorUser is "assigned".
        createAdmission(
            token = residentToken,
            patientId = patientId,
            doctorId = doctorUser.id!!,
            type = AdmissionType.HOSPITALIZATION,
            roomId = roomId,
            triageCodeId = triageCodeId,
            admissionDate = now,
        )

        // Assigned standalone doctor: gate passes, sees ALL admissions incl. the discharged one.
        mockMvc.perform(
            get(patientHistoryUrl(patientId)).header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content.length()").value(2))
            .andExpect(jsonPath("$.data.content[*].status", hasItem("DISCHARGED")))

        // A different standalone doctor, not assigned to the patient, is blocked.
        val (_, otherDoctorToken) = createUserWithRole(
            roleCode = "MEDICO",
            username = "doctor2",
            email = "doctor2@example.com",
            password = "password123",
        )
        mockMvc.perform(
            get(patientHistoryUrl(patientId)).header("Authorization", "Bearer $otherDoctorToken"),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `patient admissions history applies the psychologist active-admission gate`() {
        val now = LocalDateTime.now()
        val discharged = createAdmission(
            token = residentToken,
            patientId = patientId,
            doctorId = doctorUser.id!!,
            type = AdmissionType.AMBULATORY,
            admissionDate = now.minusDays(4),
        )
        dischargeAdmission(discharged, adminToken)

        // No active admission → psychologist is denied, exactly like the patient detail page.
        mockMvc.perform(
            get(patientHistoryUrl(patientId)).header("Authorization", "Bearer $psychologistToken"),
        )
            .andExpect(status().isForbidden)

        // Give the patient an active admission again.
        createAdmission(
            token = residentToken,
            patientId = patientId,
            doctorId = doctorUser.id!!,
            type = AdmissionType.HOSPITALIZATION,
            roomId = roomId,
            triageCodeId = triageCodeId,
            admissionDate = now,
        )

        // Now the psychologist is allowed and sees the FULL history, incl. the prior discharge.
        mockMvc.perform(
            get(patientHistoryUrl(patientId)).header("Authorization", "Bearer $psychologistToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content.length()").value(2))
            .andExpect(jsonPath("$.data.content[*].status", hasItem("DISCHARGED")))
    }

    @Test
    fun `patient admissions history forbids a user without patient read`() {
        // The seeded USER role only holds user:read.
        val (_, userToken) = createUserWithRole(
            roleCode = "USUARIO",
            username = "plainuser",
            email = "plainuser@example.com",
            password = "password123",
        )

        mockMvc.perform(
            get(patientHistoryUrl(patientId)).header("Authorization", "Bearer $userToken"),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `patient admissions history excludes soft-deleted admissions`() {
        val now = LocalDateTime.now()
        val kept = createAdmission(
            token = residentToken,
            patientId = patientId,
            doctorId = doctorUser.id!!,
            type = AdmissionType.AMBULATORY,
            admissionDate = now.minusDays(2),
        )
        dischargeAdmission(kept, adminToken)
        val deleted = createAdmission(
            token = residentToken,
            patientId = patientId,
            doctorId = doctorUser.id!!,
            type = AdmissionType.AMBULATORY,
            admissionDate = now,
        )
        mockMvc.perform(
            delete("/api/v1/admissions/$deleted").header("Authorization", "Bearer $adminToken"),
        ).andExpect(status().isOk)

        mockMvc.perform(
            get(patientHistoryUrl(patientId)).header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content.length()").value(1))
            .andExpect(jsonPath("$.data.content[0].id").value(kept))
    }

    @Test
    fun `patient summary and admissions history routes both resolve without collision`() {
        createAdmission(
            token = residentToken,
            patientId = patientId,
            doctorId = doctorUser.id!!,
            type = AdmissionType.AMBULATORY,
            admissionDate = LocalDateTime.now(),
        )

        // Summary route returns a single patient object.
        mockMvc.perform(
            get("/api/v1/admissions/patients/$patientId")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.id").value(patientId))
            .andExpect(jsonPath("$.data.firstName").value("Juan"))

        // History route returns a page of admissions.
        mockMvc.perform(
            get(patientHistoryUrl(patientId)).header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content").isArray)
            .andExpect(jsonPath("$.data.content.length()").value(1))
    }
}
