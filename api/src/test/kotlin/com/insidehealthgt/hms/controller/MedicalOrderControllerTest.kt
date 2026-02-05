package com.insidehealthgt.hms.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.insidehealthgt.hms.TestcontainersConfiguration
import com.insidehealthgt.hms.dto.request.CreateAdmissionRequest
import com.insidehealthgt.hms.dto.request.CreateMedicalOrderRequest
import com.insidehealthgt.hms.dto.request.CreatePatientRequest
import com.insidehealthgt.hms.dto.request.EmergencyContactRequest
import com.insidehealthgt.hms.dto.request.UpdateMedicalOrderRequest
import com.insidehealthgt.hms.dto.response.ApiResponse
import com.insidehealthgt.hms.dto.response.AuthResponse
import com.insidehealthgt.hms.entity.AdministrationRoute
import com.insidehealthgt.hms.entity.AdmissionType
import com.insidehealthgt.hms.entity.EducationLevel
import com.insidehealthgt.hms.entity.MaritalStatus
import com.insidehealthgt.hms.entity.MedicalOrderCategory
import com.insidehealthgt.hms.entity.Salutation
import com.insidehealthgt.hms.entity.Sex
import com.insidehealthgt.hms.entity.User
import com.insidehealthgt.hms.repository.AdmissionRepository
import com.insidehealthgt.hms.repository.MedicalOrderRepository
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
import java.time.LocalDate
import java.time.LocalDateTime

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Suppress("LargeClass", "LongMethod")
class MedicalOrderControllerTest {

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
    private lateinit var medicalOrderRepository: MedicalOrderRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    private lateinit var adminToken: String
    private lateinit var doctorToken: String
    private lateinit var nurseToken: String
    private lateinit var doctorUser: User
    private var admissionId: Long = 0

    @BeforeEach
    fun setUp() {
        medicalOrderRepository.deleteAll()
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

    // ============ LIST MEDICAL ORDERS TESTS ============

    @Test
    fun `list medical orders returns empty grouped response when none exist`() {
        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/medical-orders")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.orders").isEmpty)
    }

    @Test
    fun `list medical orders returns grouped by category`() {
        createMedicalOrder(MedicalOrderCategory.MEDICAMENTOS, "Medication 1")
        createMedicalOrder(MedicalOrderCategory.MEDICAMENTOS, "Medication 2")
        createMedicalOrder(MedicalOrderCategory.LABORATORIOS, "Lab test")

        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/medical-orders")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.orders.MEDICAMENTOS.length()").value(2))
            .andExpect(jsonPath("$.data.orders.LABORATORIOS.length()").value(1))
    }

    // ============ CREATE MEDICAL ORDER TESTS ============

    @Test
    fun `doctor can create medical order`() {
        val request = CreateMedicalOrderRequest(
            category = MedicalOrderCategory.MEDICAMENTOS,
            startDate = LocalDate.now(),
            medication = "Lorazepam",
            dosage = "1mg",
            route = AdministrationRoute.ORAL,
            frequency = "Every 8 hours",
            observations = "For anxiety",
        )

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/medical-orders")
                .header("Authorization", "Bearer $doctorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.medication").value("Lorazepam"))
            .andExpect(jsonPath("$.data.status").value("ACTIVE"))
            .andExpect(jsonPath("$.data.createdBy.firstName").value("Dr. Maria"))
    }

    @Test
    fun `nurse cannot create medical order`() {
        val request = CreateMedicalOrderRequest(
            category = MedicalOrderCategory.MEDICAMENTOS,
            startDate = LocalDate.now(),
            medication = "Should fail",
        )

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/medical-orders")
                .header("Authorization", "Bearer $nurseToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `validation requires category and start date`() {
        // Missing category (will fail Jackson deserialization or validation)
        val requestJson = """
            {
                "startDate": "${LocalDate.now()}",
                "medication": "Test"
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/medical-orders")
                .header("Authorization", "Bearer $doctorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson),
        )
            .andExpect(status().isBadRequest)
    }

    // ============ GET SINGLE MEDICAL ORDER TESTS ============

    @Test
    fun `get single medical order returns order details`() {
        val orderId = createMedicalOrderAndGetId(MedicalOrderCategory.MEDICAMENTOS, "Test medication")

        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/medical-orders/$orderId")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(orderId))
            .andExpect(jsonPath("$.data.medication").value("Test medication"))
    }

    @Test
    fun `nurse can read medical order`() {
        val orderId = createMedicalOrderAndGetId(MedicalOrderCategory.MEDICAMENTOS, "Nurse can read")

        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/medical-orders/$orderId")
                .header("Authorization", "Bearer $nurseToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
    }

    @Test
    fun `get non-existent medical order returns 404`() {
        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/medical-orders/99999")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isNotFound)
    }

    // ============ UPDATE MEDICAL ORDER TESTS ============

    @Test
    fun `admin can update medical order`() {
        val orderId = createMedicalOrderAndGetId(MedicalOrderCategory.MEDICAMENTOS, "Original")

        val updateRequest = UpdateMedicalOrderRequest(
            category = MedicalOrderCategory.MEDICAMENTOS,
            startDate = LocalDate.now(),
            medication = "Updated medication",
            dosage = "2mg",
            route = AdministrationRoute.IV,
        )

        mockMvc.perform(
            put("/api/v1/admissions/$admissionId/medical-orders/$orderId")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.medication").value("Updated medication"))
            .andExpect(jsonPath("$.data.dosage").value("2mg"))
    }

    @Test
    fun `doctor cannot update medical order`() {
        val orderId = createMedicalOrderAndGetId(MedicalOrderCategory.MEDICAMENTOS, "Original")

        val updateRequest = UpdateMedicalOrderRequest(
            category = MedicalOrderCategory.MEDICAMENTOS,
            startDate = LocalDate.now(),
            medication = "Should fail",
        )

        mockMvc.perform(
            put("/api/v1/admissions/$admissionId/medical-orders/$orderId")
                .header("Authorization", "Bearer $doctorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)),
        )
            .andExpect(status().isForbidden)
    }

    // ============ DISCONTINUE MEDICAL ORDER TESTS ============

    @Test
    fun `doctor can discontinue medical order`() {
        val orderId = createMedicalOrderAndGetId(MedicalOrderCategory.MEDICAMENTOS, "To be discontinued")

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/medical-orders/$orderId/discontinue")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("DISCONTINUED"))
            .andExpect(jsonPath("$.data.discontinuedAt").exists())
            .andExpect(jsonPath("$.data.discontinuedBy.firstName").value("Dr. Maria"))
    }

    @Test
    fun `cannot discontinue already discontinued order`() {
        val orderId = createMedicalOrderAndGetId(MedicalOrderCategory.MEDICAMENTOS, "Discontinue me")

        // First discontinue
        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/medical-orders/$orderId/discontinue")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isOk)

        // Second discontinue should fail
        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/medical-orders/$orderId/discontinue")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.message").value("Medical order is already discontinued"))
    }

    @Test
    fun `nurse cannot discontinue medical order`() {
        val orderId = createMedicalOrderAndGetId(MedicalOrderCategory.MEDICAMENTOS, "Nurse cannot discontinue")

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/medical-orders/$orderId/discontinue")
                .header("Authorization", "Bearer $nurseToken"),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `medical order includes audit fields`() {
        val orderId = createMedicalOrderAndGetId(MedicalOrderCategory.LABORATORIOS, "Audit test")

        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/medical-orders/$orderId")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.createdAt").exists())
            .andExpect(jsonPath("$.data.updatedAt").exists())
            .andExpect(jsonPath("$.data.createdBy.firstName").value("Dr. Maria"))
    }

    private fun createMedicalOrder(category: MedicalOrderCategory, medication: String?) {
        val request = CreateMedicalOrderRequest(
            category = category,
            startDate = LocalDate.now(),
            medication = medication,
        )

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/medical-orders")
                .header("Authorization", "Bearer $doctorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
    }

    private fun createMedicalOrderAndGetId(category: MedicalOrderCategory, medication: String?): Long {
        val request = CreateMedicalOrderRequest(
            category = category,
            startDate = LocalDate.now(),
            medication = medication,
        )

        val result = mockMvc.perform(
            post("/api/v1/admissions/$admissionId/medical-orders")
                .header("Authorization", "Bearer $doctorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andReturn()

        return objectMapper.readTree(result.response.contentAsString)
            .get("data").get("id").asLong()
    }
}
