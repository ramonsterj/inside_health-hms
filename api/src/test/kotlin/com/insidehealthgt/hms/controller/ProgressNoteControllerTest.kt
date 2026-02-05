package com.insidehealthgt.hms.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.insidehealthgt.hms.TestcontainersConfiguration
import com.insidehealthgt.hms.dto.request.CreateAdmissionRequest
import com.insidehealthgt.hms.dto.request.CreatePatientRequest
import com.insidehealthgt.hms.dto.request.CreateProgressNoteRequest
import com.insidehealthgt.hms.dto.request.EmergencyContactRequest
import com.insidehealthgt.hms.dto.request.UpdateProgressNoteRequest
import com.insidehealthgt.hms.dto.response.ApiResponse
import com.insidehealthgt.hms.dto.response.AuthResponse
import com.insidehealthgt.hms.entity.AdmissionType
import com.insidehealthgt.hms.entity.EducationLevel
import com.insidehealthgt.hms.entity.MaritalStatus
import com.insidehealthgt.hms.entity.Salutation
import com.insidehealthgt.hms.entity.Sex
import com.insidehealthgt.hms.entity.User
import com.insidehealthgt.hms.repository.AdmissionRepository
import com.insidehealthgt.hms.repository.PatientRepository
import com.insidehealthgt.hms.repository.ProgressNoteRepository
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
class ProgressNoteControllerTest {

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
    private lateinit var progressNoteRepository: ProgressNoteRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    private lateinit var adminToken: String
    private lateinit var doctorToken: String
    private lateinit var nurseToken: String
    private lateinit var doctorUser: User
    private var admissionId: Long = 0

    @BeforeEach
    fun setUp() {
        progressNoteRepository.deleteAll()
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

    // ============ LIST PROGRESS NOTES TESTS ============

    @Test
    fun `list progress notes returns empty page when none exist`() {
        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/progress-notes")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content").isEmpty)
    }

    @Test
    fun `list progress notes returns paginated results`() {
        createProgressNote("First note")
        createProgressNote("Second note")

        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/progress-notes")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content.length()").value(2))
    }

    // ============ CREATE PROGRESS NOTE TESTS ============

    @Test
    fun `doctor can create progress note`() {
        val request = CreateProgressNoteRequest(
            subjectiveData = "Patient reports feeling anxious",
            objectiveData = "Vital signs stable, BP 120/80",
            analysis = "Anxiety symptoms appear controlled",
            actionPlans = "Continue current medication",
        )

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/progress-notes")
                .header("Authorization", "Bearer $doctorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.subjectiveData").value("Patient reports feeling anxious"))
            .andExpect(jsonPath("$.data.createdBy.firstName").value("Dr. Maria"))
    }

    @Test
    fun `nurse can create progress note`() {
        val request = CreateProgressNoteRequest(
            subjectiveData = "Patient reports sleeping well",
            objectiveData = "Temperature 36.5C",
            analysis = "No concerns",
            actionPlans = "Continue monitoring",
        )

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/progress-notes")
                .header("Authorization", "Bearer $nurseToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.createdBy.firstName").value("Nurse"))
    }

    @Test
    fun `can create progress note with optional fields`() {
        val request = CreateProgressNoteRequest(
            subjectiveData = "Only subjective filled in",
            objectiveData = null,
            analysis = null,
            actionPlans = null,
        )

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/progress-notes")
                .header("Authorization", "Bearer $doctorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.subjectiveData").value("Only subjective filled in"))
            .andExpect(jsonPath("$.data.objectiveData").doesNotExist())
    }

    @Test
    fun `multiple notes per day allowed`() {
        createProgressNote("Morning note")
        createProgressNote("Afternoon note")
        createProgressNote("Evening note")

        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/progress-notes")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content.length()").value(3))
    }

    // ============ GET SINGLE PROGRESS NOTE TESTS ============

    @Test
    fun `get single progress note returns note details`() {
        val noteId = createProgressNoteAndGetId("Test note")

        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/progress-notes/$noteId")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(noteId))
            .andExpect(jsonPath("$.data.subjectiveData").value("Test note"))
    }

    @Test
    fun `get non-existent progress note returns 404`() {
        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/progress-notes/99999")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isNotFound)
    }

    // ============ UPDATE PROGRESS NOTE TESTS ============

    @Test
    fun `admin can update progress note`() {
        val noteId = createProgressNoteAndGetId("Original note")

        val updateRequest = UpdateProgressNoteRequest(
            subjectiveData = "Updated subjective data",
            objectiveData = "Updated objective data",
            analysis = "Updated analysis",
            actionPlans = "Updated action plans",
        )

        mockMvc.perform(
            put("/api/v1/admissions/$admissionId/progress-notes/$noteId")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.subjectiveData").value("Updated subjective data"))
    }

    @Test
    fun `doctor cannot update progress note`() {
        val noteId = createProgressNoteAndGetId("Original note")

        val updateRequest = UpdateProgressNoteRequest(
            subjectiveData = "Should fail",
            objectiveData = "Should fail",
            analysis = "Should fail",
            actionPlans = "Should fail",
        )

        mockMvc.perform(
            put("/api/v1/admissions/$admissionId/progress-notes/$noteId")
                .header("Authorization", "Bearer $doctorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `nurse cannot update progress note`() {
        val noteId = createProgressNoteAndGetId("Original note")

        val updateRequest = UpdateProgressNoteRequest(
            subjectiveData = "Should fail",
            objectiveData = "Should fail",
            analysis = "Should fail",
            actionPlans = "Should fail",
        )

        mockMvc.perform(
            put("/api/v1/admissions/$admissionId/progress-notes/$noteId")
                .header("Authorization", "Bearer $nurseToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `progress note includes audit fields`() {
        val noteId = createProgressNoteAndGetId("Audit test")

        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/progress-notes/$noteId")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.createdAt").exists())
            .andExpect(jsonPath("$.data.updatedAt").exists())
            .andExpect(jsonPath("$.data.createdBy.firstName").value("Dr. Maria"))
    }

    private fun createProgressNote(subjectiveData: String) {
        val request = CreateProgressNoteRequest(
            subjectiveData = subjectiveData,
            objectiveData = "Vital signs stable",
            analysis = "No concerns",
            actionPlans = "Continue monitoring",
        )

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/progress-notes")
                .header("Authorization", "Bearer $doctorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
    }

    private fun createProgressNoteAndGetId(subjectiveData: String): Long {
        val request = CreateProgressNoteRequest(
            subjectiveData = subjectiveData,
            objectiveData = "Vital signs stable",
            analysis = "No concerns",
            actionPlans = "Continue monitoring",
        )

        val result = mockMvc.perform(
            post("/api/v1/admissions/$admissionId/progress-notes")
                .header("Authorization", "Bearer $doctorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andReturn()

        return objectMapper.readTree(result.response.contentAsString)
            .get("data").get("id").asLong()
    }
}
