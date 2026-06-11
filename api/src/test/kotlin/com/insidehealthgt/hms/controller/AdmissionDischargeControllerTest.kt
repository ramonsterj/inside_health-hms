package com.insidehealthgt.hms.controller

import com.insidehealthgt.hms.dto.request.CreateAdmissionRequest
import com.insidehealthgt.hms.entity.AdmissionType
import com.insidehealthgt.hms.entity.Room
import com.insidehealthgt.hms.entity.RoomGender
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
    private lateinit var residentToken: String
    private lateinit var adminToken: String
    private lateinit var doctorUser: User
    private var patientId: Long = 0
    private var triageCodeId: Long = 0
    private var roomId: Long = 0

    @BeforeEach
    fun setUp() {
        val (_, staffTkn) = createAdminStaffUser()
        administrativeStaffToken = staffTkn

        // Admissions are registered through a resident (auto-bound to self).
        val (_, residentTkn) = createResidentUser()
        residentToken = residentTkn

        // Only ADMINISTRADOR and MEDICO_RESIDENTE may discharge.
        val (_, adminTkn) = createAdminUser()
        adminToken = adminTkn

        val (doctorUsr, _) = createDoctorUser()
        doctorUser = doctorUsr

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

    @Test
    fun `discharge patient should set status to DISCHARGED`() {
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
                .header("Authorization", "Bearer $residentToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"dischargeNote": "Stable, discharged home"}"""),
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
                .header("Authorization", "Bearer $residentToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andReturn()

        val admissionId = objectMapper.readTree(createResult.response.contentAsString)
            .get("data").get("id").asLong()

        // First discharge
        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/discharge")
                .header("Authorization", "Bearer $residentToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"dischargeNote": "Stable, discharged home"}"""),
        ).andExpect(status().isOk)

        // Second discharge should fail
        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/discharge")
                .header("Authorization", "Bearer $residentToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"dischargeNote": "Stable, discharged home"}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.message").value("Patient is already discharged"))
    }

    @Test
    fun `resident discharge with blank note should fail with 400`() {
        val admissionId = createAdmission()

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/discharge")
                .header("Authorization", "Bearer $residentToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"dischargeNote": "   "}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(
                jsonPath("$.error.message")
                    .value("A comment is required when discharging the patient"),
            )
    }

    @Test
    fun `resident discharge with no body should fail with 400`() {
        val admissionId = createAdmission()

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/discharge")
                .header("Authorization", "Bearer $residentToken"),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `resident discharge with note should set status DISCHARGED and persist the note`() {
        val admissionId = createAdmission()

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/discharge")
                .header("Authorization", "Bearer $residentToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"dischargeNote": "  Stable, follow up in 2 weeks  "}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.status").value("DISCHARGED"))
            .andExpect(jsonPath("$.data.dischargeNote").value("Stable, follow up in 2 weeks"))
    }

    @Test
    fun `admin discharge with note should set status DISCHARGED and persist the note`() {
        val admissionId = createAdmission()

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/discharge")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"dischargeNote": "  Cleared by attending  "}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.status").value("DISCHARGED"))
            .andExpect(jsonPath("$.data.dischargeNote").value("Cleared by attending"))
    }

    @Test
    fun `admin discharge with blank note should fail with 400`() {
        val admissionId = createAdmission()

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/discharge")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"dischargeNote": "   "}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(
                jsonPath("$.error.message")
                    .value("A comment is required when discharging the patient"),
            )
    }

    @Test
    fun `administrative staff cannot discharge and gets 403`() {
        val admissionId = createAdmission()

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/discharge")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"dischargeNote": "Attempt by admin staff"}"""),
        )
            .andExpect(status().isForbidden)
    }

    private fun createAdmission(): Long {
        val createResult = mockMvc.perform(
            post("/api/v1/admissions")
                .header("Authorization", "Bearer $residentToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createValidAdmissionRequest())),
        ).andReturn()
        return objectMapper.readTree(createResult.response.contentAsString)
            .get("data").get("id").asLong()
    }

    @Test
    fun `discharge should free room capacity`() {
        // Fill the room (capacity is 2)
        val request1 = createValidAdmissionRequest()
        val createResult = mockMvc.perform(
            post("/api/v1/admissions")
                .header("Authorization", "Bearer $residentToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)),
        ).andReturn()

        val admissionId = objectMapper.readTree(createResult.response.contentAsString)
            .get("data").get("id").asLong()

        val patient2Id = createSecondPatient(administrativeStaffToken)
        val request2 = createValidAdmissionRequest().copy(patientId = patient2Id)
        mockMvc.perform(
            post("/api/v1/admissions")
                .header("Authorization", "Bearer $residentToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)),
        ).andExpect(status().isCreated)

        // Room is now full, third admission should fail
        val patient3Id = createThirdPatient(administrativeStaffToken)
        val request3 = createValidAdmissionRequest().copy(patientId = patient3Id)
        mockMvc.perform(
            post("/api/v1/admissions")
                .header("Authorization", "Bearer $residentToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request3)),
        ).andExpect(status().isBadRequest)

        // Discharge first patient
        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/discharge")
                .header("Authorization", "Bearer $residentToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"dischargeNote": "Stable, discharged home"}"""),
        ).andExpect(status().isOk)

        // Now third admission should succeed
        mockMvc.perform(
            post("/api/v1/admissions")
                .header("Authorization", "Bearer $residentToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request3)),
        ).andExpect(status().isCreated)
    }
}
