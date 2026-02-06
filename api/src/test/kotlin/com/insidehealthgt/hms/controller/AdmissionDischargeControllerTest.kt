package com.insidehealthgt.hms.controller

import com.insidehealthgt.hms.dto.request.CreateAdmissionRequest
import com.insidehealthgt.hms.entity.AdmissionType
import com.insidehealthgt.hms.entity.Room
import com.insidehealthgt.hms.entity.RoomType
import com.insidehealthgt.hms.entity.User
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

class AdmissionDischargeControllerTest : AbstractIntegrationTest() {

    private lateinit var administrativeStaffToken: String
    private lateinit var doctorUser: User
    private var patientId: Long = 0
    private var triageCodeId: Long = 0
    private var roomId: Long = 0

    @BeforeEach
    fun setUp() {
        val (_, staffTkn) = createAdminStaffUser()
        administrativeStaffToken = staffTkn

        val (doctorUsr, _) = createDoctorUser()
        doctorUser = doctorUsr

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

    @Test
    fun `discharge patient should set status to DISCHARGED`() {
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
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.status").value("DISCHARGED"))
            .andExpect(jsonPath("$.data.dischargeDate").exists())
    }

    @Test
    fun `discharge patient should fail when already discharged`() {
        val request = createValidAdmissionRequest()
        val createResult = mockMvc.perform(
            post("/api/v1/admissions")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andReturn()

        val admissionId = objectMapper.readTree(createResult.response.contentAsString)
            .get("data").get("id").asLong()

        // First discharge
        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/discharge")
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )

        // Second discharge should fail
        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/discharge")
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.message").value("Patient is already discharged"))
    }

    @Test
    fun `discharge should free room capacity`() {
        // Fill the room (capacity is 2)
        val request1 = createValidAdmissionRequest()
        val createResult = mockMvc.perform(
            post("/api/v1/admissions")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)),
        ).andReturn()

        val admissionId = objectMapper.readTree(createResult.response.contentAsString)
            .get("data").get("id").asLong()

        val patient2Id = createSecondPatient(administrativeStaffToken)
        val request2 = createValidAdmissionRequest().copy(patientId = patient2Id)
        mockMvc.perform(
            post("/api/v1/admissions")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)),
        ).andExpect(status().isCreated)

        // Room is now full, third admission should fail
        val patient3Id = createThirdPatient(administrativeStaffToken)
        val request3 = createValidAdmissionRequest().copy(patientId = patient3Id)
        mockMvc.perform(
            post("/api/v1/admissions")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request3)),
        ).andExpect(status().isBadRequest)

        // Discharge first patient
        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/discharge")
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )

        // Now third admission should succeed
        mockMvc.perform(
            post("/api/v1/admissions")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request3)),
        ).andExpect(status().isCreated)
    }
}
