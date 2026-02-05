package com.insidehealthgt.hms.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.insidehealthgt.hms.TestcontainersConfiguration
import com.insidehealthgt.hms.dto.request.CreateAdmissionRequest
import com.insidehealthgt.hms.dto.request.CreateClinicalHistoryRequest
import com.insidehealthgt.hms.dto.request.CreatePatientRequest
import com.insidehealthgt.hms.dto.request.EmergencyContactRequest
import com.insidehealthgt.hms.dto.request.UpdateClinicalHistoryRequest
import com.insidehealthgt.hms.dto.response.ApiResponse
import com.insidehealthgt.hms.dto.response.AuthResponse
import com.insidehealthgt.hms.entity.AdmissionType
import com.insidehealthgt.hms.entity.EducationLevel
import com.insidehealthgt.hms.entity.MaritalStatus
import com.insidehealthgt.hms.entity.Salutation
import com.insidehealthgt.hms.entity.Sex
import com.insidehealthgt.hms.entity.User
import com.insidehealthgt.hms.repository.AdmissionRepository
import com.insidehealthgt.hms.repository.ClinicalHistoryRepository
import com.insidehealthgt.hms.repository.PatientRepository
import com.insidehealthgt.hms.repository.RoleRepository
import com.insidehealthgt.hms.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Suppress("LargeClass", "LongMethod")
class ClinicalHistoryControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var roleRepository: RoleRepository

    @Autowired
    private lateinit var patientRepository: PatientRepository

    @Autowired
    private lateinit var admissionRepository: AdmissionRepository

    @Autowired
    private lateinit var clinicalHistoryRepository: ClinicalHistoryRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    private lateinit var adminToken: String
    private lateinit var doctorToken: String
    private lateinit var nurseToken: String
    private lateinit var doctorUser: User
    private var admissionId: Long = 0

    @BeforeEach
    fun setUp() {
        clinicalHistoryRepository.deleteAll()
        admissionRepository.deleteAll()
        patientRepository.deleteAll()
        userRepository.deleteAll()

        // Create admin user
        val adminRole = roleRepository.findByCode("ADMIN")!!
        val adminUser = User(
            username = "admin",
            email = "admin@example.com",
            passwordHash = passwordEncoder.encode("admin123")!!,
            firstName = "Admin",
            lastName = "User",
        )
        adminUser.roles.add(adminRole)
        userRepository.save(adminUser)
        adminToken = loginAndGetToken("admin@example.com", "admin123")

        // Create doctor user
        val doctorRole = roleRepository.findByCode("DOCTOR")!!
        doctorUser = User(
            username = "doctor",
            email = "doctor@example.com",
            passwordHash = passwordEncoder.encode("password123")!!,
            firstName = "Dr. Maria",
            lastName = "Garcia",
            salutation = Salutation.DR,
        )
        doctorUser.roles.add(doctorRole)
        userRepository.save(doctorUser)
        doctorToken = loginAndGetToken("doctor@example.com", "password123")

        // Create nurse user
        val nurseRole = roleRepository.findByCode("NURSE")!!
        val nurseUser = User(
            username = "nurse",
            email = "nurse@example.com",
            passwordHash = passwordEncoder.encode("password123")!!,
            firstName = "Nurse",
            lastName = "Johnson",
        )
        nurseUser.roles.add(nurseRole)
        userRepository.save(nurseUser)
        nurseToken = loginAndGetToken("nurse@example.com", "password123")

        // Create admission for tests
        admissionId = createAdmission()
    }

    private fun loginAndGetToken(email: String, password: String): String {
        val request = mapOf("identifier" to email, "password" to password)
        val result = mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andReturn()

        val responseType = objectMapper.typeFactory.constructParametricType(
            ApiResponse::class.java,
            AuthResponse::class.java,
        )
        val response: ApiResponse<AuthResponse> = objectMapper.readValue(
            result.response.contentAsString,
            responseType,
        )
        return response.data!!.accessToken
    }

    private fun createAdmission(): Long {
        // Create patient first
        val patientRequest = CreatePatientRequest(
            firstName = "Juan",
            lastName = "Perez",
            age = 45,
            sex = Sex.MALE,
            gender = "Masculino",
            maritalStatus = MaritalStatus.MARRIED,
            religion = "Catolica",
            educationLevel = EducationLevel.UNIVERSITY,
            occupation = "Ingeniero",
            address = "Guatemala City",
            email = "juan.perez@example.com",
            emergencyContacts = listOf(
                EmergencyContactRequest(name = "Maria", relationship = "Esposa", phone = "555-1234"),
            ),
        )

        val patientResult = mockMvc.perform(
            post("/api/v1/patients")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patientRequest)),
        ).andReturn()

        val patientId = objectMapper.readTree(patientResult.response.contentAsString)
            .get("data").get("id").asLong()

        // Create admission
        val admissionRequest = CreateAdmissionRequest(
            patientId = patientId,
            triageCodeId = null,
            roomId = null,
            treatingPhysicianId = doctorUser.id!!,
            admissionDate = LocalDateTime.now(),
            type = AdmissionType.AMBULATORY,
        )

        val admissionResult = mockMvc.perform(
            post("/api/v1/admissions")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(admissionRequest)),
        ).andReturn()

        return objectMapper.readTree(admissionResult.response.contentAsString)
            .get("data").get("id").asLong()
    }

    // ============ GET CLINICAL HISTORY TESTS ============

    @Test
    fun `get clinical history returns null when not exists`() {
        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/clinical-history")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `doctor can read clinical history`() {
        // Create clinical history first
        createClinicalHistory()

        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/clinical-history")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.reasonForAdmission").value("Anxiety disorder"))
    }

    @Test
    fun `nurse can read clinical history`() {
        createClinicalHistory()

        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/clinical-history")
                .header("Authorization", "Bearer $nurseToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.reasonForAdmission").value("Anxiety disorder"))
    }

    @Test
    fun `admin can read clinical history`() {
        createClinicalHistory()

        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/clinical-history")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.reasonForAdmission").value("Anxiety disorder"))
    }

    // ============ CREATE CLINICAL HISTORY TESTS ============

    @Test
    fun `doctor can create clinical history`() {
        val request = CreateClinicalHistoryRequest(
            reasonForAdmission = "Anxiety disorder",
            historyOfPresentIllness = "Patient reports increasing anxiety",
            psychiatricHistory = "No prior psychiatric history",
        )

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/clinical-history")
                .header("Authorization", "Bearer $doctorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.reasonForAdmission").value("Anxiety disorder"))
            .andExpect(jsonPath("$.data.createdBy").exists())
    }

    @Test
    fun `nurse cannot create clinical history`() {
        val request = CreateClinicalHistoryRequest(
            reasonForAdmission = "Should fail",
        )

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/clinical-history")
                .header("Authorization", "Bearer $nurseToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `duplicate creation fails with 400`() {
        createClinicalHistory()

        val request = CreateClinicalHistoryRequest(
            reasonForAdmission = "Duplicate",
        )

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/clinical-history")
                .header("Authorization", "Bearer $doctorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.message").value("Clinical history already exists for this admission"))
    }

    // ============ UPDATE CLINICAL HISTORY TESTS ============

    @Test
    fun `admin can update clinical history`() {
        createClinicalHistory()

        val updateRequest = UpdateClinicalHistoryRequest(
            reasonForAdmission = "Updated: Severe anxiety disorder",
            treatmentPlan = "Medication and therapy",
        )

        mockMvc.perform(
            put("/api/v1/admissions/$admissionId/clinical-history")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.reasonForAdmission").value("Updated: Severe anxiety disorder"))
            .andExpect(jsonPath("$.data.treatmentPlan").value("Medication and therapy"))
    }

    @Test
    fun `doctor cannot update clinical history`() {
        createClinicalHistory()

        val updateRequest = UpdateClinicalHistoryRequest(
            reasonForAdmission = "Should fail",
        )

        mockMvc.perform(
            put("/api/v1/admissions/$admissionId/clinical-history")
                .header("Authorization", "Bearer $doctorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `nurse cannot update clinical history`() {
        createClinicalHistory()

        val updateRequest = UpdateClinicalHistoryRequest(
            reasonForAdmission = "Should fail",
        )

        mockMvc.perform(
            put("/api/v1/admissions/$admissionId/clinical-history")
                .header("Authorization", "Bearer $nurseToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `update returns 404 when clinical history not exists`() {
        val updateRequest = UpdateClinicalHistoryRequest(
            reasonForAdmission = "Should fail",
        )

        mockMvc.perform(
            put("/api/v1/admissions/$admissionId/clinical-history")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)),
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `clinical history includes audit fields`() {
        createClinicalHistory()

        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/clinical-history")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.createdAt").exists())
            .andExpect(jsonPath("$.data.updatedAt").exists())
            .andExpect(jsonPath("$.data.createdBy.firstName").value("Dr. Maria"))
    }

    private fun createClinicalHistory() {
        val request = CreateClinicalHistoryRequest(
            reasonForAdmission = "Anxiety disorder",
            historyOfPresentIllness = "Patient reports increasing anxiety",
        )

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/clinical-history")
                .header("Authorization", "Bearer $doctorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
    }
}
