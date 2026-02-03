package com.insidehealthgt.hms.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.insidehealthgt.hms.TestcontainersConfiguration
import com.insidehealthgt.hms.dto.request.CreateAdmissionRequest
import com.insidehealthgt.hms.dto.request.CreatePatientRequest
import com.insidehealthgt.hms.dto.request.CreateTriageCodeRequest
import com.insidehealthgt.hms.dto.request.EmergencyContactRequest
import com.insidehealthgt.hms.dto.request.UpdateTriageCodeRequest
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class TriageCodeControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var roleRepository: RoleRepository

    @Autowired
    private lateinit var triageCodeRepository: TriageCodeRepository

    @Autowired
    private lateinit var admissionRepository: AdmissionRepository

    @Autowired
    private lateinit var patientRepository: PatientRepository

    @Autowired
    private lateinit var roomRepository: RoomRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    private lateinit var adminToken: String
    private lateinit var administrativeStaffToken: String
    private lateinit var doctorToken: String

    @BeforeEach
    fun setUp() {
        admissionRepository.deleteAll()
        roomRepository.deleteAll()
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

        // Create administrative staff user
        val adminStaffRole = roleRepository.findByCode("ADMINISTRATIVE_STAFF")!!
        val adminStaffUser = User(
            username = "receptionist",
            email = "receptionist@example.com",
            passwordHash = passwordEncoder.encode("password123")!!,
            firstName = "Reception",
            lastName = "Staff",
        )
        adminStaffUser.roles.add(adminStaffRole)
        userRepository.save(adminStaffUser)
        administrativeStaffToken = loginAndGetToken("receptionist@example.com", "password123")

        // Create doctor user
        val doctorRole = roleRepository.findByCode("DOCTOR")!!
        val doctorUser = User(
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

    private fun createValidTriageCodeRequest(): CreateTriageCodeRequest = CreateTriageCodeRequest(
        code = "X",
        color = "#FF00FF",
        description = "Test triage code",
        displayOrder = 99,
    )

    // ============ CREATE TRIAGE CODE TESTS ============

    @Test
    fun `create triage code with valid data should return 201`() {
        val request = createValidTriageCodeRequest()

        mockMvc.perform(
            post("/api/v1/triage-codes")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.code").value("X"))
            .andExpect(jsonPath("$.data.color").value("#FF00FF"))
            .andExpect(jsonPath("$.data.description").value("Test triage code"))
            .andExpect(jsonPath("$.data.displayOrder").value(99))
    }

    @Test
    fun `create triage code should fail with duplicate code`() {
        val request = createValidTriageCodeRequest()

        // Create first triage code
        mockMvc.perform(
            post("/api/v1/triage-codes")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andExpect(status().isCreated)

        // Try to create duplicate
        mockMvc.perform(
            post("/api/v1/triage-codes")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `create triage code should fail for administrative staff`() {
        val request = createValidTriageCodeRequest()

        mockMvc.perform(
            post("/api/v1/triage-codes")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `create triage code should fail with invalid color format`() {
        val request = mapOf(
            "code" to "X",
            "color" to "invalid",
            "description" to "Test",
            "displayOrder" to 99,
        )

        mockMvc.perform(
            post("/api/v1/triage-codes")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `create triage code should fail with blank code`() {
        val request = mapOf(
            "code" to "",
            "color" to "#FF00FF",
            "description" to "Test",
            "displayOrder" to 99,
        )

        mockMvc.perform(
            post("/api/v1/triage-codes")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `create triage code should fail without authentication`() {
        val request = createValidTriageCodeRequest()

        mockMvc.perform(
            post("/api/v1/triage-codes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isUnauthorized)
    }

    // ============ LIST TRIAGE CODES TESTS ============

    @Test
    fun `list triage codes should return all codes sorted by display order`() {
        // Seeded triage codes from V021 migration should be present
        mockMvc.perform(
            get("/api/v1/triage-codes")
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data[0].code").value("A")) // First by display order
    }

    @Test
    fun `list triage codes should be accessible by administrative staff`() {
        mockMvc.perform(
            get("/api/v1/triage-codes")
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `list triage codes should be accessible by doctor`() {
        mockMvc.perform(
            get("/api/v1/triage-codes")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isOk)
    }

    // ============ GET TRIAGE CODE TESTS ============

    @Test
    fun `get triage code should return triage code details`() {
        val triageCode = triageCodeRepository.findAll().first()

        mockMvc.perform(
            get("/api/v1/triage-codes/${triageCode.id}")
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.code").exists())
            .andExpect(jsonPath("$.data.color").exists())
    }

    @Test
    fun `get triage code should return 404 for non-existent code`() {
        mockMvc.perform(
            get("/api/v1/triage-codes/99999")
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )
            .andExpect(status().isNotFound)
    }

    // ============ UPDATE TRIAGE CODE TESTS ============

    @Test
    fun `update triage code should update triage code data`() {
        val request = createValidTriageCodeRequest()
        val createResult = mockMvc.perform(
            post("/api/v1/triage-codes")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andReturn()

        val triageCodeId = objectMapper.readTree(createResult.response.contentAsString)
            .get("data").get("id").asLong()

        val updateRequest = UpdateTriageCodeRequest(
            code = "X-Updated",
            color = "#00FF00",
            description = "Updated description",
            displayOrder = 100,
        )

        mockMvc.perform(
            put("/api/v1/triage-codes/$triageCodeId")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.code").value("X-Updated"))
            .andExpect(jsonPath("$.data.color").value("#00FF00"))
            .andExpect(jsonPath("$.data.description").value("Updated description"))
            .andExpect(jsonPath("$.data.displayOrder").value(100))
    }

    @Test
    fun `update triage code should fail for administrative staff`() {
        val request = createValidTriageCodeRequest()
        val createResult = mockMvc.perform(
            post("/api/v1/triage-codes")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andReturn()

        val triageCodeId = objectMapper.readTree(createResult.response.contentAsString)
            .get("data").get("id").asLong()

        val updateRequest = UpdateTriageCodeRequest(
            code = "X-Updated",
            color = "#00FF00",
            description = "Updated",
            displayOrder = 100,
        )

        mockMvc.perform(
            put("/api/v1/triage-codes/$triageCodeId")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `update triage code should return 404 for non-existent code`() {
        val updateRequest = UpdateTriageCodeRequest(
            code = "X-Updated",
            color = "#00FF00",
            description = "Updated",
            displayOrder = 100,
        )

        mockMvc.perform(
            put("/api/v1/triage-codes/99999")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)),
        )
            .andExpect(status().isNotFound)
    }

    // ============ DELETE TRIAGE CODE TESTS ============

    @Test
    fun `delete triage code should soft delete code`() {
        val request = createValidTriageCodeRequest()
        val createResult = mockMvc.perform(
            post("/api/v1/triage-codes")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andReturn()

        val triageCodeId = objectMapper.readTree(createResult.response.contentAsString)
            .get("data").get("id").asLong()

        mockMvc.perform(
            delete("/api/v1/triage-codes/$triageCodeId")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)

        // Should not be found after deletion
        mockMvc.perform(
            get("/api/v1/triage-codes/$triageCodeId")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `delete triage code should fail when in use by active admission`() {
        // Get existing triage code from seeded data
        val triageCode = triageCodeRepository.findAll().first()

        // Create room, patient, and doctor for admission
        val room = Room(
            number = "101",
            type = RoomType.PRIVATE,
            capacity = 1,
        )
        roomRepository.save(room)

        val patientId = createPatient()
        val doctorId = createDoctor()

        // Create admission using the triage code
        val admissionRequest = CreateAdmissionRequest(
            patientId = patientId,
            triageCodeId = triageCode.id!!,
            roomId = room.id!!,
            treatingPhysicianId = doctorId,
            admissionDate = LocalDateTime.now(),
            type = AdmissionType.HOSPITALIZATION,
            inventory = null,
        )

        mockMvc.perform(
            post("/api/v1/admissions")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(admissionRequest)),
        ).andExpect(status().isCreated)

        // Try to delete triage code - should fail
        mockMvc.perform(
            delete("/api/v1/triage-codes/${triageCode.id}")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `delete triage code should fail for administrative staff`() {
        val request = createValidTriageCodeRequest()
        val createResult = mockMvc.perform(
            post("/api/v1/triage-codes")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andReturn()

        val triageCodeId = objectMapper.readTree(createResult.response.contentAsString)
            .get("data").get("id").asLong()

        mockMvc.perform(
            delete("/api/v1/triage-codes/$triageCodeId")
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )
            .andExpect(status().isForbidden)
    }

    // ============ HELPER METHODS ============

    private fun createPatient(): Long {
        val request = CreatePatientRequest(
            firstName = "Juan",
            lastName = "Pérez García",
            age = 45,
            sex = Sex.MALE,
            gender = "Masculino",
            maritalStatus = MaritalStatus.MARRIED,
            religion = "Católica",
            educationLevel = EducationLevel.UNIVERSITY,
            occupation = "Ingeniero",
            address = "4a Calle 5-67 Zona 1, Guatemala",
            email = "juan.perez@email.com",
            emergencyContacts = listOf(
                EmergencyContactRequest(
                    name = "María de Pérez",
                    relationship = "Esposa",
                    phone = "+502 5555-1234",
                ),
            ),
        )

        val result = mockMvc.perform(
            post("/api/v1/patients")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andReturn()

        return objectMapper.readTree(result.response.contentAsString)
            .get("data").get("id").asLong()
    }

    private fun createDoctor(): Long {
        val doctorRole = roleRepository.findByCode("DOCTOR")!!
        val doctor = User(
            username = "doctor2",
            email = "doctor2@example.com",
            passwordHash = passwordEncoder.encode("password123")!!,
            firstName = "Dr. Carlos",
            lastName = "Lopez",
            salutation = Salutation.DR,
        )
        doctor.roles.add(doctorRole)
        userRepository.save(doctor)
        return doctor.id!!
    }
}
