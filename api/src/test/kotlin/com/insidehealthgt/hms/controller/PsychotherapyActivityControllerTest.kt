package com.insidehealthgt.hms.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.insidehealthgt.hms.TestcontainersConfiguration
import com.insidehealthgt.hms.dto.request.CreateAdmissionRequest
import com.insidehealthgt.hms.dto.request.CreatePatientRequest
import com.insidehealthgt.hms.dto.request.CreatePsychotherapyActivityRequest
import com.insidehealthgt.hms.dto.request.EmergencyContactRequest
import com.insidehealthgt.hms.dto.response.ApiResponse
import com.insidehealthgt.hms.dto.response.AuthResponse
import com.insidehealthgt.hms.entity.AdmissionType
import com.insidehealthgt.hms.entity.EducationLevel
import com.insidehealthgt.hms.entity.MaritalStatus
import com.insidehealthgt.hms.entity.Room
import com.insidehealthgt.hms.entity.RoomType
import com.insidehealthgt.hms.entity.Salutation
import com.insidehealthgt.hms.entity.Sex
import com.insidehealthgt.hms.entity.User
import com.insidehealthgt.hms.repository.AdmissionRepository
import com.insidehealthgt.hms.repository.PatientRepository
import com.insidehealthgt.hms.repository.PsychotherapyActivityRepository
import com.insidehealthgt.hms.repository.PsychotherapyCategoryRepository
import com.insidehealthgt.hms.repository.RoleRepository
import com.insidehealthgt.hms.repository.RoomRepository
import com.insidehealthgt.hms.repository.TriageCodeRepository
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class PsychotherapyActivityControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var roleRepository: RoleRepository

    @Autowired
    private lateinit var categoryRepository: PsychotherapyCategoryRepository

    @Autowired
    private lateinit var activityRepository: PsychotherapyActivityRepository

    @Autowired
    private lateinit var admissionRepository: AdmissionRepository

    @Autowired
    private lateinit var patientRepository: PatientRepository

    @Autowired
    private lateinit var roomRepository: RoomRepository

    @Autowired
    private lateinit var triageCodeRepository: TriageCodeRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    private lateinit var adminToken: String
    private lateinit var psychologistToken: String
    private lateinit var doctorToken: String
    private lateinit var nurseToken: String
    private lateinit var adminStaffToken: String

    private var hospitalizationAdmissionId: Long = 0
    private var ambulatoryAdmissionId: Long = 0
    private var categoryId: Long = 0

    @BeforeEach
    fun setUp() {
        activityRepository.deleteAllHard()
        admissionRepository.deleteAll()
        roomRepository.deleteAll()
        patientRepository.deleteAll()
        userRepository.deleteAll()

        adminToken = createUserAndGetToken("ADMIN", "admin", "admin@example.com", "admin123")
        psychologistToken = createUserAndGetToken("PSYCHOLOGIST", "psychologist", "psychologist@example.com")
        doctorToken = createUserAndGetToken("DOCTOR", "doctor", "doctor@example.com")
        nurseToken = createUserAndGetToken("NURSE", "nurse", "nurse@example.com")
        adminStaffToken = createUserAndGetToken("ADMINISTRATIVE_STAFF", "receptionist", "receptionist@example.com")

        categoryId = categoryRepository.findAll().first().id!!
        hospitalizationAdmissionId = createAdmission(AdmissionType.HOSPITALIZATION)
        ambulatoryAdmissionId = createAdmission(AdmissionType.AMBULATORY)
    }

    private fun createUserAndGetToken(
        roleCode: String,
        username: String,
        email: String,
        password: String = "password123",
    ): String {
        val role = roleRepository.findByCode(roleCode)!!
        val user = User(
            username = username,
            email = email,
            passwordHash = passwordEncoder.encode(password)!!,
            firstName = username.replaceFirstChar { it.uppercase() },
            lastName = "User",
        )
        user.roles.add(role)
        userRepository.save(user)
        return loginAndGetToken(email, password)
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

    private fun createAdmission(type: AdmissionType): Long {
        val patientId = createPatient()
        val doctorId = createDoctor()

        val room = Room(
            number = "Room-${System.nanoTime()}",
            type = RoomType.PRIVATE,
            capacity = 1,
        )
        roomRepository.save(room)

        val triageCode = triageCodeRepository.findAll().first()

        val admissionRequest = CreateAdmissionRequest(
            patientId = patientId,
            triageCodeId = if (type.requiresTriageCode()) triageCode.id!! else null,
            roomId = if (type.requiresRoom()) room.id!! else null,
            treatingPhysicianId = doctorId,
            admissionDate = LocalDateTime.now(),
            type = type,
            inventory = null,
        )

        val result = mockMvc.perform(
            post("/api/v1/admissions")
                .header("Authorization", "Bearer $adminStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(admissionRequest)),
        ).andReturn()

        return objectMapper.readTree(result.response.contentAsString)
            .get("data").get("id").asLong()
    }

    private fun createPatient(): Long {
        val request = CreatePatientRequest(
            firstName = "Test",
            lastName = "Patient ${System.nanoTime()}",
            age = 30,
            sex = Sex.MALE,
            gender = "Male",
            maritalStatus = MaritalStatus.SINGLE,
            religion = "None",
            educationLevel = EducationLevel.UNIVERSITY,
            occupation = "Engineer",
            address = "Test Address",
            email = "patient${System.nanoTime()}@test.com",
            emergencyContacts = listOf(
                EmergencyContactRequest(
                    name = "Emergency Contact",
                    relationship = "Family",
                    phone = "+502 5555-1234",
                ),
            ),
        )

        val result = mockMvc.perform(
            post("/api/v1/patients")
                .header("Authorization", "Bearer $adminStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andReturn()

        return objectMapper.readTree(result.response.contentAsString)
            .get("data").get("id").asLong()
    }

    private fun createDoctor(): Long {
        val doctorRole = roleRepository.findByCode("DOCTOR")!!
        val doctor = User(
            username = "doctor${System.nanoTime()}",
            email = "doctor${System.nanoTime()}@example.com",
            passwordHash = passwordEncoder.encode("password123")!!,
            firstName = "Dr. Test",
            lastName = "Doctor",
            salutation = Salutation.DR,
        )
        doctor.roles.add(doctorRole)
        userRepository.save(doctor)
        return doctor.id!!
    }

    // ============ CREATE ACTIVITY TESTS ============

    @Test
    fun `create activity with psychologist should return 201`() {
        val request = CreatePsychotherapyActivityRequest(
            categoryId = categoryId,
            description = "Patient participated in art therapy workshop",
        )

        mockMvc.perform(
            post("/api/v1/admissions/$hospitalizationAdmissionId/psychotherapy-activities")
                .header("Authorization", "Bearer $psychologistToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.description").value("Patient participated in art therapy workshop"))
            .andExpect(jsonPath("$.data.category.id").value(categoryId))
            .andExpect(jsonPath("$.data.admissionId").value(hospitalizationAdmissionId))
            .andExpect(jsonPath("$.data.createdBy.firstName").value("Psychologist"))
    }

    @Test
    fun `create activity should fail for doctor`() {
        val request = CreatePsychotherapyActivityRequest(
            categoryId = categoryId,
            description = "Test activity",
        )

        mockMvc.perform(
            post("/api/v1/admissions/$hospitalizationAdmissionId/psychotherapy-activities")
                .header("Authorization", "Bearer $doctorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `create activity should fail for nurse`() {
        val request = CreatePsychotherapyActivityRequest(
            categoryId = categoryId,
            description = "Test activity",
        )

        mockMvc.perform(
            post("/api/v1/admissions/$hospitalizationAdmissionId/psychotherapy-activities")
                .header("Authorization", "Bearer $nurseToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `create activity should fail for admin`() {
        val request = CreatePsychotherapyActivityRequest(
            categoryId = categoryId,
            description = "Test activity",
        )

        mockMvc.perform(
            post("/api/v1/admissions/$hospitalizationAdmissionId/psychotherapy-activities")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `create activity should fail for ambulatory admission`() {
        val request = CreatePsychotherapyActivityRequest(
            categoryId = categoryId,
            description = "Test activity",
        )

        mockMvc.perform(
            post("/api/v1/admissions/$ambulatoryAdmissionId/psychotherapy-activities")
                .header("Authorization", "Bearer $psychologistToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `create activity should fail with blank description`() {
        val request = mapOf(
            "categoryId" to categoryId,
            "description" to "",
        )

        mockMvc.perform(
            post("/api/v1/admissions/$hospitalizationAdmissionId/psychotherapy-activities")
                .header("Authorization", "Bearer $psychologistToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `create activity should fail with non-existent category`() {
        val request = CreatePsychotherapyActivityRequest(
            categoryId = 99999,
            description = "Test activity",
        )

        mockMvc.perform(
            post("/api/v1/admissions/$hospitalizationAdmissionId/psychotherapy-activities")
                .header("Authorization", "Bearer $psychologistToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isNotFound)
    }

    // ============ LIST ACTIVITIES TESTS ============

    @Test
    fun `list activities should return activities`() {
        // Create an activity first
        val request = CreatePsychotherapyActivityRequest(
            categoryId = categoryId,
            description = "Test activity",
        )
        mockMvc.perform(
            post("/api/v1/admissions/$hospitalizationAdmissionId/psychotherapy-activities")
                .header("Authorization", "Bearer $psychologistToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andExpect(status().isCreated)

        // List activities
        mockMvc.perform(
            get("/api/v1/admissions/$hospitalizationAdmissionId/psychotherapy-activities")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data[0].description").value("Test activity"))
    }

    @Test
    fun `list activities should be accessible by psychologist`() {
        mockMvc.perform(
            get("/api/v1/admissions/$hospitalizationAdmissionId/psychotherapy-activities")
                .header("Authorization", "Bearer $psychologistToken"),
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `list activities should be accessible by doctor`() {
        mockMvc.perform(
            get("/api/v1/admissions/$hospitalizationAdmissionId/psychotherapy-activities")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `list activities should be accessible by nurse`() {
        mockMvc.perform(
            get("/api/v1/admissions/$hospitalizationAdmissionId/psychotherapy-activities")
                .header("Authorization", "Bearer $nurseToken"),
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `list activities with sort asc should sort oldest first`() {
        // Create first activity
        mockMvc.perform(
            post("/api/v1/admissions/$hospitalizationAdmissionId/psychotherapy-activities")
                .header("Authorization", "Bearer $psychologistToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        CreatePsychotherapyActivityRequest(categoryId, "First activity"),
                    ),
                ),
        ).andExpect(status().isCreated)

        // Create second activity
        mockMvc.perform(
            post("/api/v1/admissions/$hospitalizationAdmissionId/psychotherapy-activities")
                .header("Authorization", "Bearer $psychologistToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        CreatePsychotherapyActivityRequest(categoryId, "Second activity"),
                    ),
                ),
        ).andExpect(status().isCreated)

        // List with asc sort - oldest first
        mockMvc.perform(
            get("/api/v1/admissions/$hospitalizationAdmissionId/psychotherapy-activities?sort=asc")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].description").value("First activity"))
            .andExpect(jsonPath("$.data[1].description").value("Second activity"))
    }

    @Test
    fun `list activities with sort desc should sort newest first`() {
        // Create first activity
        mockMvc.perform(
            post("/api/v1/admissions/$hospitalizationAdmissionId/psychotherapy-activities")
                .header("Authorization", "Bearer $psychologistToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        CreatePsychotherapyActivityRequest(categoryId, "First activity"),
                    ),
                ),
        ).andExpect(status().isCreated)

        // Create second activity
        mockMvc.perform(
            post("/api/v1/admissions/$hospitalizationAdmissionId/psychotherapy-activities")
                .header("Authorization", "Bearer $psychologistToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        CreatePsychotherapyActivityRequest(categoryId, "Second activity"),
                    ),
                ),
        ).andExpect(status().isCreated)

        // List with desc sort - newest first (default)
        mockMvc.perform(
            get("/api/v1/admissions/$hospitalizationAdmissionId/psychotherapy-activities?sort=desc")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].description").value("Second activity"))
            .andExpect(jsonPath("$.data[1].description").value("First activity"))
    }

    // ============ GET ACTIVITY TESTS ============

    @Test
    fun `get activity should return activity details`() {
        // Create an activity first
        val request = CreatePsychotherapyActivityRequest(
            categoryId = categoryId,
            description = "Test activity",
        )
        val createResult = mockMvc.perform(
            post("/api/v1/admissions/$hospitalizationAdmissionId/psychotherapy-activities")
                .header("Authorization", "Bearer $psychologistToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andReturn()

        val activityId = objectMapper.readTree(createResult.response.contentAsString)
            .get("data").get("id").asLong()

        // Get activity
        mockMvc.perform(
            get("/api/v1/admissions/$hospitalizationAdmissionId/psychotherapy-activities/$activityId")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.description").value("Test activity"))
    }

    @Test
    fun `get activity should return 404 for non-existent`() {
        mockMvc.perform(
            get("/api/v1/admissions/$hospitalizationAdmissionId/psychotherapy-activities/99999")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isNotFound)
    }

    // ============ DELETE ACTIVITY TESTS ============

    @Test
    fun `delete activity should work for admin`() {
        // Create an activity first
        val request = CreatePsychotherapyActivityRequest(
            categoryId = categoryId,
            description = "Test activity",
        )
        val createResult = mockMvc.perform(
            post("/api/v1/admissions/$hospitalizationAdmissionId/psychotherapy-activities")
                .header("Authorization", "Bearer $psychologistToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andReturn()

        val activityId = objectMapper.readTree(createResult.response.contentAsString)
            .get("data").get("id").asLong()

        // Delete activity
        mockMvc.perform(
            delete("/api/v1/admissions/$hospitalizationAdmissionId/psychotherapy-activities/$activityId")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isNoContent)

        // Should not be found after deletion
        mockMvc.perform(
            get("/api/v1/admissions/$hospitalizationAdmissionId/psychotherapy-activities/$activityId")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `delete activity should fail for psychologist`() {
        // Create an activity first
        val request = CreatePsychotherapyActivityRequest(
            categoryId = categoryId,
            description = "Test activity",
        )
        val createResult = mockMvc.perform(
            post("/api/v1/admissions/$hospitalizationAdmissionId/psychotherapy-activities")
                .header("Authorization", "Bearer $psychologistToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andReturn()

        val activityId = objectMapper.readTree(createResult.response.contentAsString)
            .get("data").get("id").asLong()

        // Try to delete as psychologist
        mockMvc.perform(
            delete("/api/v1/admissions/$hospitalizationAdmissionId/psychotherapy-activities/$activityId")
                .header("Authorization", "Bearer $psychologistToken"),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `delete activity should fail for doctor`() {
        // Create an activity first
        val request = CreatePsychotherapyActivityRequest(
            categoryId = categoryId,
            description = "Test activity",
        )
        val createResult = mockMvc.perform(
            post("/api/v1/admissions/$hospitalizationAdmissionId/psychotherapy-activities")
                .header("Authorization", "Bearer $psychologistToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andReturn()

        val activityId = objectMapper.readTree(createResult.response.contentAsString)
            .get("data").get("id").asLong()

        // Try to delete as doctor
        mockMvc.perform(
            delete("/api/v1/admissions/$hospitalizationAdmissionId/psychotherapy-activities/$activityId")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isForbidden)
    }

    // ============ CATEGORY IN USE CHECK TEST ============

    @Test
    fun `delete category should fail when in use by activity`() {
        // Create an activity first
        val request = CreatePsychotherapyActivityRequest(
            categoryId = categoryId,
            description = "Test activity",
        )
        mockMvc.perform(
            post("/api/v1/admissions/$hospitalizationAdmissionId/psychotherapy-activities")
                .header("Authorization", "Bearer $psychologistToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andExpect(status().isCreated)

        // Try to delete the category
        mockMvc.perform(
            delete("/api/v1/admin/psychotherapy-categories/$categoryId")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isBadRequest)
    }
}
