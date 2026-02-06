package com.insidehealthgt.hms.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.insidehealthgt.hms.TestcontainersConfiguration
import com.insidehealthgt.hms.dto.request.CreateAdmissionRequest
import com.insidehealthgt.hms.dto.request.CreatePatientRequest
import com.insidehealthgt.hms.dto.request.CreateVitalSignRequest
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
import com.insidehealthgt.hms.repository.PatientRepository
import com.insidehealthgt.hms.repository.RoleRepository
import com.insidehealthgt.hms.repository.UserRepository
import com.insidehealthgt.hms.repository.VitalSignRepository
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
import java.math.BigDecimal
import java.time.LocalDateTime

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Suppress("LargeClass", "LongMethod")
class VitalSignControllerTest {

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
    private lateinit var vitalSignRepository: VitalSignRepository

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
        vitalSignRepository.deleteAll()
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
            admissionDate = LocalDateTime.now().minusDays(1),
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

    private fun createDefaultVitalSignRequest(): CreateVitalSignRequest = CreateVitalSignRequest(
        systolicBp = 120,
        diastolicBp = 80,
        heartRate = 72,
        respiratoryRate = 16,
        temperature = BigDecimal("36.5"),
        oxygenSaturation = 98,
    )

    // ============ LIST VITAL SIGNS TESTS ============

    @Test
    fun `list vital signs returns empty page when none exist`() {
        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/vital-signs")
                .header("Authorization", "Bearer $nurseToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content").isEmpty)
    }

    @Test
    fun `list vital signs returns paginated results`() {
        createVitalSign()
        createVitalSign()

        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/vital-signs")
                .header("Authorization", "Bearer $nurseToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content.length()").value(2))
    }

    // ============ CREATE VITAL SIGN TESTS ============

    @Test
    fun `doctor can create vital signs`() {
        val request = createDefaultVitalSignRequest()

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/vital-signs")
                .header("Authorization", "Bearer $doctorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.createdBy.firstName").value("Dr. Maria"))
    }

    @Test
    fun `nurse can create vital signs`() {
        val request = createDefaultVitalSignRequest()

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/vital-signs")
                .header("Authorization", "Bearer $nurseToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.createdBy.firstName").value("Nurse"))
    }

    @Test
    fun `create vital signs returns 201 with all fields`() {
        val request = CreateVitalSignRequest(
            systolicBp = 130,
            diastolicBp = 85,
            heartRate = 78,
            respiratoryRate = 18,
            temperature = BigDecimal("37.2"),
            oxygenSaturation = 96,
            other = "Post-exercise reading",
        )

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/vital-signs")
                .header("Authorization", "Bearer $nurseToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.id").exists())
            .andExpect(jsonPath("$.data.admissionId").value(admissionId))
            .andExpect(jsonPath("$.data.systolicBp").value(130))
            .andExpect(jsonPath("$.data.diastolicBp").value(85))
            .andExpect(jsonPath("$.data.heartRate").value(78))
            .andExpect(jsonPath("$.data.respiratoryRate").value(18))
            .andExpect(jsonPath("$.data.temperature").value(37.2))
            .andExpect(jsonPath("$.data.oxygenSaturation").value(96))
            .andExpect(jsonPath("$.data.other").value("Post-exercise reading"))
            .andExpect(jsonPath("$.data.recordedAt").exists())
            .andExpect(jsonPath("$.data.canEdit").value(true))
    }

    @Test
    fun `create vital signs without recordedAt defaults to now`() {
        val request = createDefaultVitalSignRequest()

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/vital-signs")
                .header("Authorization", "Bearer $nurseToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.recordedAt").exists())
    }

    @Test
    fun `create vital signs with specific recordedAt stores correctly`() {
        val recordedAt = LocalDateTime.now().minusHours(2)
        val request = CreateVitalSignRequest(
            recordedAt = recordedAt,
            systolicBp = 120,
            diastolicBp = 80,
            heartRate = 72,
            respiratoryRate = 16,
            temperature = BigDecimal("36.5"),
            oxygenSaturation = 98,
        )

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/vital-signs")
                .header("Authorization", "Bearer $nurseToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.recordedAt").exists())
    }

    // ============ GET SINGLE VITAL SIGN TESTS ============

    @Test
    fun `get vital sign by id returns correct data`() {
        val vitalSignId = createVitalSignAndGetId()

        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/vital-signs/$vitalSignId")
                .header("Authorization", "Bearer $nurseToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(vitalSignId))
            .andExpect(jsonPath("$.data.systolicBp").value(120))
    }

    @Test
    fun `get non-existent vital sign returns 404`() {
        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/vital-signs/99999")
                .header("Authorization", "Bearer $nurseToken"),
        )
            .andExpect(status().isNotFound)
    }

    // ============ UPDATE VITAL SIGN TESTS ============

    @Test
    fun `creator can update vital sign within 24 hours`() {
        val vitalSignId = createVitalSignAndGetId(nurseToken)

        val updateRequest = CreateVitalSignRequest(
            systolicBp = 125,
            diastolicBp = 82,
            heartRate = 75,
            respiratoryRate = 17,
            temperature = BigDecimal("36.8"),
            oxygenSaturation = 97,
        )

        mockMvc.perform(
            put("/api/v1/admissions/$admissionId/vital-signs/$vitalSignId")
                .header("Authorization", "Bearer $nurseToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.systolicBp").value(125))
    }

    @Test
    fun `admin can update any vital sign`() {
        val vitalSignId = createVitalSignAndGetId(nurseToken)

        val updateRequest = CreateVitalSignRequest(
            systolicBp = 130,
            diastolicBp = 85,
            heartRate = 78,
            respiratoryRate = 18,
            temperature = BigDecimal("37.0"),
            oxygenSaturation = 96,
        )

        mockMvc.perform(
            put("/api/v1/admissions/$admissionId/vital-signs/$vitalSignId")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.systolicBp").value(130))
    }

    // ============ CHART ENDPOINT TESTS ============

    @Test
    fun `chart endpoint returns non-paginated list`() {
        createVitalSign()
        createVitalSign()

        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/vital-signs/chart")
                .header("Authorization", "Bearer $nurseToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data.length()").value(2))
    }

    @Test
    fun `chart endpoint returns empty list when no data`() {
        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/vital-signs/chart")
                .header("Authorization", "Bearer $nurseToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data").isEmpty)
    }

    @Test
    fun `chart endpoint respects date range filters`() {
        createVitalSign()

        // Query with a date range that excludes the data
        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/vital-signs/chart")
                .param("fromDate", "2025-01-01")
                .param("toDate", "2025-01-02")
                .header("Authorization", "Bearer $nurseToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").isEmpty)
    }

    // ============ DATE FILTERING TESTS ============

    @Test
    fun `list vital signs with fromDate filter`() {
        createVitalSign()

        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/vital-signs")
                .param("fromDate", "2025-01-01")
                .header("Authorization", "Bearer $nurseToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
    }

    @Test
    fun `list vital signs with toDate filter`() {
        createVitalSign()

        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/vital-signs")
                .param("toDate", "2027-12-31")
                .header("Authorization", "Bearer $nurseToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
    }

    @Test
    fun `list vital signs with both fromDate and toDate`() {
        createVitalSign()

        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/vital-signs")
                .param("fromDate", "2025-01-01")
                .param("toDate", "2027-12-31")
                .header("Authorization", "Bearer $nurseToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content.length()").value(1))
    }

    // ============ VALIDATION TESTS ============

    @Test
    fun `create fails with systolic BP outside 60-250 range`() {
        val request = CreateVitalSignRequest(
            systolicBp = 50,
            diastolicBp = 80,
            heartRate = 72,
            respiratoryRate = 16,
            temperature = BigDecimal("36.5"),
            oxygenSaturation = 98,
        )

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/vital-signs")
                .header("Authorization", "Bearer $nurseToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `create fails with diastolic BP outside 30-150 range`() {
        val request = CreateVitalSignRequest(
            systolicBp = 120,
            diastolicBp = 20,
            heartRate = 72,
            respiratoryRate = 16,
            temperature = BigDecimal("36.5"),
            oxygenSaturation = 98,
        )

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/vital-signs")
                .header("Authorization", "Bearer $nurseToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `create fails with systolic less than or equal to diastolic`() {
        val request = CreateVitalSignRequest(
            systolicBp = 80,
            diastolicBp = 80,
            heartRate = 72,
            respiratoryRate = 16,
            temperature = BigDecimal("36.5"),
            oxygenSaturation = 98,
        )

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/vital-signs")
                .header("Authorization", "Bearer $nurseToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `create fails with heart rate outside 20-250 range`() {
        val request = CreateVitalSignRequest(
            systolicBp = 120,
            diastolicBp = 80,
            heartRate = 10,
            respiratoryRate = 16,
            temperature = BigDecimal("36.5"),
            oxygenSaturation = 98,
        )

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/vital-signs")
                .header("Authorization", "Bearer $nurseToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `create fails with temperature outside 30-45 range`() {
        val request = CreateVitalSignRequest(
            systolicBp = 120,
            diastolicBp = 80,
            heartRate = 72,
            respiratoryRate = 16,
            temperature = BigDecimal("29.0"),
            oxygenSaturation = 98,
        )

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/vital-signs")
                .header("Authorization", "Bearer $nurseToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `create fails with oxygen saturation outside 50-100 range`() {
        val request = CreateVitalSignRequest(
            systolicBp = 120,
            diastolicBp = 80,
            heartRate = 72,
            respiratoryRate = 16,
            temperature = BigDecimal("36.5"),
            oxygenSaturation = 40,
        )

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/vital-signs")
                .header("Authorization", "Bearer $nurseToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `create fails with recordedAt in the future`() {
        val request = CreateVitalSignRequest(
            recordedAt = LocalDateTime.now().plusHours(2),
            systolicBp = 120,
            diastolicBp = 80,
            heartRate = 72,
            respiratoryRate = 16,
            temperature = BigDecimal("36.5"),
            oxygenSaturation = 98,
        )

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/vital-signs")
                .header("Authorization", "Bearer $nurseToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)
    }

    // ============ AUTHORIZATION / BUSINESS RULES TESTS ============

    @Test
    fun `non-creator cannot update vital sign`() {
        val vitalSignId = createVitalSignAndGetId(nurseToken)

        val updateRequest = createDefaultVitalSignRequest()

        mockMvc.perform(
            put("/api/v1/admissions/$admissionId/vital-signs/$vitalSignId")
                .header("Authorization", "Bearer $doctorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `edit after 24 hours denied for non-admin`() {
        val vitalSignId = createVitalSignAndGetId(nurseToken)

        // Use native SQL to bypass @Column(updatable = false) on createdAt
        jdbcTemplate.update(
            "UPDATE vital_signs SET created_at = ? WHERE id = ?",
            java.sql.Timestamp.valueOf(LocalDateTime.now().minusHours(25)),
            vitalSignId,
        )

        val updateRequest = createDefaultVitalSignRequest()

        mockMvc.perform(
            put("/api/v1/admissions/$admissionId/vital-signs/$vitalSignId")
                .header("Authorization", "Bearer $nurseToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `create vital sign fails for discharged admission`() {
        dischargeAdmission(admissionId)

        val request = createDefaultVitalSignRequest()

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/vital-signs")
                .header("Authorization", "Bearer $nurseToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `update vital sign fails for discharged admission`() {
        val vitalSignId = createVitalSignAndGetId(nurseToken)
        dischargeAdmission(admissionId)

        val updateRequest = createDefaultVitalSignRequest()

        mockMvc.perform(
            put("/api/v1/admissions/$admissionId/vital-signs/$vitalSignId")
                .header("Authorization", "Bearer $nurseToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)),
        )
            .andExpect(status().isBadRequest)
    }

    // ============ AUDIT TESTS ============

    @Test
    fun `vital sign includes audit fields and canEdit`() {
        val vitalSignId = createVitalSignAndGetId(nurseToken)

        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/vital-signs/$vitalSignId")
                .header("Authorization", "Bearer $nurseToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.createdAt").exists())
            .andExpect(jsonPath("$.data.updatedAt").exists())
            .andExpect(jsonPath("$.data.createdBy.firstName").value("Nurse"))
            .andExpect(jsonPath("$.data.canEdit").value(true))
    }

    // ============ HELPER METHODS ============

    private fun createVitalSign(token: String = nurseToken) {
        val request = createDefaultVitalSignRequest()

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/vital-signs")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
    }

    private fun createVitalSignAndGetId(token: String = nurseToken): Long {
        val request = createDefaultVitalSignRequest()

        val result = mockMvc.perform(
            post("/api/v1/admissions/$admissionId/vital-signs")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andReturn()

        return objectMapper.readTree(result.response.contentAsString)
            .get("data").get("id").asLong()
    }
}
