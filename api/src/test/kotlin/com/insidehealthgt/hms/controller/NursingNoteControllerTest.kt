package com.insidehealthgt.hms.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.insidehealthgt.hms.TestcontainersConfiguration
import com.insidehealthgt.hms.dto.request.CreateAdmissionRequest
import com.insidehealthgt.hms.dto.request.CreateNursingNoteRequest
import com.insidehealthgt.hms.dto.request.CreatePatientRequest
import com.insidehealthgt.hms.dto.request.EmergencyContactRequest
import com.insidehealthgt.hms.dto.response.ApiResponse
import com.insidehealthgt.hms.dto.response.AuthResponse
import com.insidehealthgt.hms.entity.AdmissionType
import com.insidehealthgt.hms.entity.EducationLevel
import com.insidehealthgt.hms.entity.MaritalStatus
import com.insidehealthgt.hms.entity.Salutation
import com.insidehealthgt.hms.entity.Sex
import com.insidehealthgt.hms.entity.User
import com.insidehealthgt.hms.repository.AdmissionRepository
import com.insidehealthgt.hms.repository.NursingNoteRepository
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
import org.springframework.jdbc.core.JdbcTemplate
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
class NursingNoteControllerTest {

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
    private lateinit var nursingNoteRepository: NursingNoteRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    private lateinit var adminToken: String
    private lateinit var doctorToken: String
    private lateinit var nurseToken: String
    private lateinit var nurseUser: User
    private lateinit var doctorUser: User
    private var admissionId: Long = 0

    @BeforeEach
    fun setUp() {
        nursingNoteRepository.deleteAll()
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
        nurseUser = User(
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

    private fun dischargeAdmission(id: Long) {
        mockMvc.perform(
            post("/api/v1/admissions/$id/discharge")
                .header("Authorization", "Bearer $adminToken"),
        ).andExpect(status().isOk)
    }

    // ============ LIST NURSING NOTES TESTS ============

    @Test
    fun `list nursing notes returns empty page when none exist`() {
        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/nursing-notes")
                .header("Authorization", "Bearer $nurseToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content").isEmpty)
    }

    @Test
    fun `list nursing notes returns paginated results`() {
        createNursingNote("First note")
        createNursingNote("Second note")

        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/nursing-notes")
                .header("Authorization", "Bearer $nurseToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content.length()").value(2))
    }

    // ============ CREATE NURSING NOTE TESTS ============

    @Test
    fun `doctor can create nursing note`() {
        val request = CreateNursingNoteRequest(
            description = "Patient vital signs stable. No complaints.",
        )

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/nursing-notes")
                .header("Authorization", "Bearer $doctorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.description").value("Patient vital signs stable. No complaints."))
            .andExpect(jsonPath("$.data.createdBy.firstName").value("Dr. Maria"))
    }

    @Test
    fun `nurse can create nursing note`() {
        val request = CreateNursingNoteRequest(
            description = "Administered medication as prescribed.",
        )

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/nursing-notes")
                .header("Authorization", "Bearer $nurseToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.createdBy.firstName").value("Nurse"))
    }

    @Test
    fun `create nursing note returns 201 with audit fields`() {
        val request = CreateNursingNoteRequest(
            description = "Test nursing note for audit",
        )

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/nursing-notes")
                .header("Authorization", "Bearer $nurseToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.id").exists())
            .andExpect(jsonPath("$.data.admissionId").value(admissionId))
            .andExpect(jsonPath("$.data.createdAt").exists())
            .andExpect(jsonPath("$.data.updatedAt").exists())
            .andExpect(jsonPath("$.data.createdBy").exists())
            .andExpect(jsonPath("$.data.canEdit").value(true))
    }

    // ============ GET SINGLE NURSING NOTE TESTS ============

    @Test
    fun `get nursing note by id returns correct data`() {
        val noteId = createNursingNoteAndGetId("Test note content")

        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/nursing-notes/$noteId")
                .header("Authorization", "Bearer $nurseToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(noteId))
            .andExpect(jsonPath("$.data.description").value("Test note content"))
    }

    @Test
    fun `get non-existent nursing note returns 404`() {
        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/nursing-notes/99999")
                .header("Authorization", "Bearer $nurseToken"),
        )
            .andExpect(status().isNotFound)
    }

    // ============ UPDATE NURSING NOTE TESTS ============

    @Test
    fun `creator can update nursing note within 24 hours`() {
        val noteId = createNursingNoteAndGetId("Original note", nurseToken)

        val updateRequest = CreateNursingNoteRequest(
            description = "Updated note content",
        )

        mockMvc.perform(
            put("/api/v1/admissions/$admissionId/nursing-notes/$noteId")
                .header("Authorization", "Bearer $nurseToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.description").value("Updated note content"))
    }

    @Test
    fun `admin can update any nursing note`() {
        val noteId = createNursingNoteAndGetId("Original note", nurseToken)

        val updateRequest = CreateNursingNoteRequest(
            description = "Admin updated this note",
        )

        mockMvc.perform(
            put("/api/v1/admissions/$admissionId/nursing-notes/$noteId")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.description").value("Admin updated this note"))
    }

    // ============ VALIDATION TESTS ============

    @Test
    fun `create nursing note fails with empty description`() {
        val request = mapOf("description" to "")

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/nursing-notes")
                .header("Authorization", "Bearer $nurseToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `create nursing note fails with description over 5000 chars`() {
        val request = mapOf("description" to "X".repeat(5001))

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/nursing-notes")
                .header("Authorization", "Bearer $nurseToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)
    }

    // ============ AUTHORIZATION / BUSINESS RULES TESTS ============

    @Test
    fun `unauthenticated request returns 401`() {
        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/nursing-notes"),
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `non-creator cannot update nursing note`() {
        // Nurse creates a note
        val noteId = createNursingNoteAndGetId("Nurse's note", nurseToken)

        // Doctor tries to update it
        val updateRequest = CreateNursingNoteRequest(
            description = "Should fail",
        )

        mockMvc.perform(
            put("/api/v1/admissions/$admissionId/nursing-notes/$noteId")
                .header("Authorization", "Bearer $doctorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `edit after 24 hours denied for non-admin`() {
        val noteId = createNursingNoteAndGetId("Old note", nurseToken)

        // Use native SQL to bypass @Column(updatable = false) on createdAt
        jdbcTemplate.update(
            "UPDATE nursing_notes SET created_at = ? WHERE id = ?",
            java.sql.Timestamp.valueOf(LocalDateTime.now().minusHours(25)),
            noteId,
        )

        val updateRequest = CreateNursingNoteRequest(
            description = "Should fail - too late",
        )

        mockMvc.perform(
            put("/api/v1/admissions/$admissionId/nursing-notes/$noteId")
                .header("Authorization", "Bearer $nurseToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `create nursing note fails for discharged admission`() {
        dischargeAdmission(admissionId)

        val request = CreateNursingNoteRequest(
            description = "Should fail - discharged",
        )

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/nursing-notes")
                .header("Authorization", "Bearer $nurseToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `update nursing note fails for discharged admission`() {
        val noteId = createNursingNoteAndGetId("Note before discharge", nurseToken)
        dischargeAdmission(admissionId)

        val updateRequest = CreateNursingNoteRequest(
            description = "Should fail - discharged",
        )

        mockMvc.perform(
            put("/api/v1/admissions/$admissionId/nursing-notes/$noteId")
                .header("Authorization", "Bearer $nurseToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)),
        )
            .andExpect(status().isBadRequest)
    }

    // ============ AUDIT TESTS ============

    @Test
    fun `nursing note includes createdBy and updatedBy audit fields`() {
        val noteId = createNursingNoteAndGetId("Audit test", nurseToken)

        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/nursing-notes/$noteId")
                .header("Authorization", "Bearer $nurseToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.createdAt").exists())
            .andExpect(jsonPath("$.data.updatedAt").exists())
            .andExpect(jsonPath("$.data.createdBy.firstName").value("Nurse"))
    }

    @Test
    fun `canEdit is true for creator within 24h and false otherwise`() {
        val noteId = createNursingNoteAndGetId("Edit window test", nurseToken)

        // Creator within 24h - canEdit should be true
        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/nursing-notes/$noteId")
                .header("Authorization", "Bearer $nurseToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.canEdit").value(true))

        // Non-creator - canEdit should be false
        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/nursing-notes/$noteId")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.canEdit").value(false))
    }

    // ============ HELPER METHODS ============

    private fun createNursingNote(description: String, token: String = nurseToken) {
        val request = CreateNursingNoteRequest(description = description)

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/nursing-notes")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
    }

    private fun createNursingNoteAndGetId(description: String, token: String = nurseToken): Long {
        val request = CreateNursingNoteRequest(description = description)

        val result = mockMvc.perform(
            post("/api/v1/admissions/$admissionId/nursing-notes")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andReturn()

        return objectMapper.readTree(result.response.contentAsString)
            .get("data").get("id").asLong()
    }
}
