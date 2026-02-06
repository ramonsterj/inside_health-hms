package com.insidehealthgt.hms.controller

import com.insidehealthgt.hms.dto.request.AddConsultingPhysicianRequest
import com.insidehealthgt.hms.dto.request.CreateAdmissionRequest
import com.insidehealthgt.hms.entity.AdmissionType
import com.insidehealthgt.hms.entity.Room
import com.insidehealthgt.hms.entity.RoomType
import com.insidehealthgt.hms.entity.Salutation
import com.insidehealthgt.hms.entity.User
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate
import java.time.LocalDateTime

class AdmissionConsultingPhysicianControllerTest : AbstractIntegrationTest() {

    private lateinit var administrativeStaffToken: String
    private lateinit var doctorToken: String
    private lateinit var doctorUser: User
    private lateinit var secondDoctorUser: User
    private var patientId: Long = 0
    private var triageCodeId: Long = 0
    private var roomId: Long = 0

    @BeforeEach
    fun setUp() {
        val (_, staffTkn) = createAdminStaffUser()
        administrativeStaffToken = staffTkn

        val (doctorUsr, doctorTkn) = createDoctorUser()
        doctorUser = doctorUsr
        doctorToken = doctorTkn

        val (secondDoctor, _) = createUserWithRole(
            roleCode = "DOCTOR",
            username = "doctor2",
            email = "doctor2@example.com",
            password = "password123",
            firstName = "Dr. Carlos",
            lastName = "Rodriguez",
            salutation = Salutation.DR,
        )
        secondDoctorUser = secondDoctor

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

    private fun createAdmissionAndGetId(): Long {
        val request = createValidAdmissionRequest()
        val createResult = mockMvc.perform(
            post("/api/v1/admissions")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andExpect(status().isCreated)
            .andReturn()

        return objectMapper.readTree(createResult.response.contentAsString)
            .get("data").get("id").asLong()
    }

    @Test
    fun `list consulting physicians should return empty list for new admission`() {
        val admissionId = createAdmissionAndGetId()

        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/consulting-physicians")
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data").isEmpty)
    }

    @Test
    fun `add consulting physician should return 201`() {
        val admissionId = createAdmissionAndGetId()

        val consultingRequest = AddConsultingPhysicianRequest(
            physicianId = secondDoctorUser.id!!,
            reason = "Cardiology consultation",
            requestedDate = LocalDate.now(),
        )

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/consulting-physicians")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(consultingRequest)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.physician.firstName").value("Dr. Carlos"))
            .andExpect(jsonPath("$.data.reason").value("Cardiology consultation"))
    }

    @Test
    fun `add consulting physician should fail when adding treating physician`() {
        val admissionId = createAdmissionAndGetId()

        val consultingRequest = AddConsultingPhysicianRequest(
            physicianId = doctorUser.id!!,
            reason = "Should fail",
        )

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/consulting-physicians")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(consultingRequest)),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.message").value("Cannot add treating physician as a consulting physician"))
    }

    @Test
    fun `add consulting physician should fail when user is not a doctor`() {
        val admissionId = createAdmissionAndGetId()

        val (_, adminTkn) = createAdminUser()
        val adminUser = userRepository.findByEmail("admin@example.com")!!

        val consultingRequest = AddConsultingPhysicianRequest(
            physicianId = adminUser.id!!,
            reason = "Should fail",
        )

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/consulting-physicians")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(consultingRequest)),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.message").value("Consulting physician must have the DOCTOR role"))
    }

    @Test
    fun `add consulting physician should fail when physician already assigned`() {
        val admissionId = createAdmissionAndGetId()

        val consultingRequest = AddConsultingPhysicianRequest(
            physicianId = secondDoctorUser.id!!,
            reason = "First assignment",
        )

        // First assignment should succeed
        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/consulting-physicians")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(consultingRequest)),
        )
            .andExpect(status().isCreated)

        // Second assignment should fail
        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/consulting-physicians")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(consultingRequest)),
        )
            .andExpect(status().isBadRequest)
            .andExpect(
                jsonPath("$.error.message")
                    .value("Physician is already assigned as a consultant for this admission"),
            )
    }

    @Test
    fun `remove consulting physician should return 204`() {
        val admissionId = createAdmissionAndGetId()

        val consultingRequest = AddConsultingPhysicianRequest(
            physicianId = secondDoctorUser.id!!,
            reason = "To be removed",
        )

        val addResult = mockMvc.perform(
            post("/api/v1/admissions/$admissionId/consulting-physicians")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(consultingRequest)),
        ).andReturn()

        val consultingPhysicianId = objectMapper.readTree(addResult.response.contentAsString)
            .get("data").get("id").asLong()

        mockMvc.perform(
            delete("/api/v1/admissions/$admissionId/consulting-physicians/$consultingPhysicianId")
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )
            .andExpect(status().isNoContent)

        // Verify it's removed
        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/consulting-physicians")
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").isEmpty)
    }

    @Test
    fun `remove consulting physician should return 404 for non-existent record`() {
        val admissionId = createAdmissionAndGetId()

        mockMvc.perform(
            delete("/api/v1/admissions/$admissionId/consulting-physicians/99999")
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `add consulting physician should fail for doctor role`() {
        val admissionId = createAdmissionAndGetId()

        val consultingRequest = AddConsultingPhysicianRequest(
            physicianId = secondDoctorUser.id!!,
            reason = "Should fail - no permission",
        )

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/consulting-physicians")
                .header("Authorization", "Bearer $doctorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(consultingRequest)),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `get admission should include consulting physicians`() {
        val admissionId = createAdmissionAndGetId()

        val consultingRequest = AddConsultingPhysicianRequest(
            physicianId = secondDoctorUser.id!!,
            reason = "Cardiology consultation",
        )

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/consulting-physicians")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(consultingRequest)),
        )

        mockMvc.perform(
            get("/api/v1/admissions/$admissionId")
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.consultingPhysicians").isArray)
            .andExpect(jsonPath("$.data.consultingPhysicians[0].physician.firstName").value("Dr. Carlos"))
            .andExpect(jsonPath("$.data.consultingPhysicians[0].reason").value("Cardiology consultation"))
    }
}
