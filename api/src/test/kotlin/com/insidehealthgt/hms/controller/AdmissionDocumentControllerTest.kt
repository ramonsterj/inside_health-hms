package com.insidehealthgt.hms.controller

import com.insidehealthgt.hms.dto.request.CreateAdmissionRequest
import com.insidehealthgt.hms.entity.AdmissionType
import com.insidehealthgt.hms.entity.Room
import com.insidehealthgt.hms.entity.RoomType
import com.insidehealthgt.hms.entity.User
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

class AdmissionDocumentControllerTest : AbstractIntegrationTest() {

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

    private fun createAdmissionAndGetId(): Long {
        val request = CreateAdmissionRequest(
            patientId = patientId,
            triageCodeId = triageCodeId,
            roomId = roomId,
            treatingPhysicianId = doctorUser.id!!,
            admissionDate = LocalDateTime.now(),
            type = AdmissionType.HOSPITALIZATION,
            inventory = "Wallet, phone, glasses",
        )
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
    fun `upload consent document should work for administrative staff`() {
        val admissionId = createAdmissionAndGetId()

        val mockFile = MockMultipartFile(
            "file",
            "consent.pdf",
            MediaType.APPLICATION_PDF_VALUE,
            "fake-pdf-content".toByteArray(),
        )

        mockMvc.perform(
            multipart("/api/v1/admissions/$admissionId/consent")
                .file(mockFile)
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.hasConsentDocument").value(true))
    }

    @Test
    fun `upload consent document should fail for invalid file type`() {
        val admissionId = createAdmissionAndGetId()

        val mockFile = MockMultipartFile(
            "file",
            "consent.txt",
            MediaType.TEXT_PLAIN_VALUE,
            "text content".toByteArray(),
        )

        mockMvc.perform(
            multipart("/api/v1/admissions/$admissionId/consent")
                .file(mockFile)
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `download consent document should work after upload`() {
        val admissionId = createAdmissionAndGetId()

        val mockFile = MockMultipartFile(
            "file",
            "consent.pdf",
            MediaType.APPLICATION_PDF_VALUE,
            "fake-pdf-content".toByteArray(),
        )

        mockMvc.perform(
            multipart("/api/v1/admissions/$admissionId/consent")
                .file(mockFile)
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )

        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/consent")
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `download consent document should return 404 when no document uploaded`() {
        val admissionId = createAdmissionAndGetId()

        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/consent")
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )
            .andExpect(status().isNotFound)
    }
}
