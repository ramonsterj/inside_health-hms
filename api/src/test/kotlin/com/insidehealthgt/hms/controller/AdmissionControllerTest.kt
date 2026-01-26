package com.insidehealthgt.hms.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.insidehealthgt.hms.TestcontainersConfiguration
import com.insidehealthgt.hms.dto.request.AddConsultingPhysicianRequest
import com.insidehealthgt.hms.dto.request.CreateAdmissionRequest
import com.insidehealthgt.hms.dto.request.CreatePatientRequest
import com.insidehealthgt.hms.dto.request.EmergencyContactRequest
import com.insidehealthgt.hms.dto.request.UpdateAdmissionRequest
import com.insidehealthgt.hms.dto.response.ApiResponse
import com.insidehealthgt.hms.dto.response.AuthResponse
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
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
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
class AdmissionControllerTest {

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
    private lateinit var roomRepository: RoomRepository

    @Autowired
    private lateinit var triageCodeRepository: TriageCodeRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    private lateinit var adminToken: String
    private lateinit var administrativeStaffToken: String
    private lateinit var doctorToken: String
    private lateinit var doctorUser: User
    private lateinit var secondDoctorUser: User
    private var patientId: Long = 0
    private var triageCodeId: Long = 0
    private var roomId: Long = 0

    @BeforeEach
    fun setUp() {
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

        // Create second doctor user for consulting physician tests
        secondDoctorUser = User(
            username = "doctor2",
            email = "doctor2@example.com",
            passwordHash = passwordEncoder.encode("password123")!!,
            firstName = "Dr. Carlos",
            lastName = "Rodriguez",
            salutation = Salutation.DR,
        )
        secondDoctorUser.roles.add(doctorRole)
        userRepository.save(secondDoctorUser)

        // Create a patient for admission tests
        patientId = createPatient()

        // Get a triage code (seeded by migration V021)
        val triageCode = triageCodeRepository.findAll().first()
        triageCodeId = triageCode.id!!

        // Create a room for admission tests (use unique number to avoid conflict with seeded data)
        val room = Room(
            number = "TEST-101",
            type = RoomType.PRIVATE,
            capacity = 2,
        )
        roomRepository.save(room)
        roomId = room.id!!
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
            idDocumentNumber = "1234567890101",
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

    private fun createValidAdmissionRequest(): CreateAdmissionRequest = CreateAdmissionRequest(
        patientId = patientId,
        triageCodeId = triageCodeId,
        roomId = roomId,
        treatingPhysicianId = doctorUser.id!!,
        admissionDate = LocalDateTime.now(),
        inventory = "Wallet, phone, glasses",
    )

    // ============ CREATE ADMISSION TESTS ============

    @Test
    fun `create admission with valid data should return 201`() {
        val request = createValidAdmissionRequest()

        mockMvc.perform(
            post("/api/v1/admissions")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.patient.firstName").value("Juan"))
            .andExpect(jsonPath("$.data.status").value("ACTIVE"))
            .andExpect(jsonPath("$.data.inventory").value("Wallet, phone, glasses"))
            .andExpect(jsonPath("$.data.treatingPhysician.firstName").value("Dr. Maria"))
            .andExpect(jsonPath("$.data.createdBy").exists())
    }

    @Test
    fun `create admission should fail with non-existent patient`() {
        val request = createValidAdmissionRequest().copy(patientId = 99999)

        mockMvc.perform(
            post("/api/v1/admissions")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `create admission should fail with non-existent triage code`() {
        val request = createValidAdmissionRequest().copy(triageCodeId = 99999)

        mockMvc.perform(
            post("/api/v1/admissions")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `create admission should fail with non-existent room`() {
        val request = createValidAdmissionRequest().copy(roomId = 99999)

        mockMvc.perform(
            post("/api/v1/admissions")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `create admission should fail with non-doctor treating physician`() {
        // Create non-doctor user
        val adminStaffRole = roleRepository.findByCode("ADMINISTRATIVE_STAFF")!!
        val nonDoctorUser = User(
            username = "nurse",
            email = "nurse@example.com",
            passwordHash = passwordEncoder.encode("password123")!!,
            firstName = "Nurse",
            lastName = "Jones",
        )
        nonDoctorUser.roles.add(adminStaffRole)
        userRepository.save(nonDoctorUser)

        val request = createValidAdmissionRequest().copy(treatingPhysicianId = nonDoctorUser.id!!)

        mockMvc.perform(
            post("/api/v1/admissions")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.message").value("Treating physician must have the DOCTOR role"))
    }

    @Test
    fun `create admission should fail when room is full`() {
        // Fill the room (capacity is 2)
        val request1 = createValidAdmissionRequest()
        mockMvc.perform(
            post("/api/v1/admissions")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)),
        ).andExpect(status().isCreated)

        // Create second patient
        val patient2Id = createSecondPatient()
        val request2 = createValidAdmissionRequest().copy(patientId = patient2Id)
        mockMvc.perform(
            post("/api/v1/admissions")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)),
        ).andExpect(status().isCreated)

        // Create third patient - should fail
        val patient3Id = createThirdPatient()
        val request3 = createValidAdmissionRequest().copy(patientId = patient3Id)
        mockMvc.perform(
            post("/api/v1/admissions")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request3)),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.message").value("Room 'TEST-101' is full. No available beds."))
    }

    @Test
    fun `create admission should fail for doctor role`() {
        val request = createValidAdmissionRequest()

        mockMvc.perform(
            post("/api/v1/admissions")
                .header("Authorization", "Bearer $doctorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `create admission should fail without authentication`() {
        val request = createValidAdmissionRequest()

        mockMvc.perform(
            post("/api/v1/admissions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isUnauthorized)
    }

    // ============ LIST ADMISSIONS TESTS ============

    @Test
    fun `list admissions should return paginated results`() {
        // Create an admission first
        val request = createValidAdmissionRequest()
        mockMvc.perform(
            post("/api/v1/admissions")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )

        mockMvc.perform(
            get("/api/v1/admissions")
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content").isArray)
            .andExpect(jsonPath("$.data.content[0].patient.firstName").value("Juan"))
    }

    @Test
    fun `list admissions should filter by status`() {
        // Create an admission
        val request = createValidAdmissionRequest()
        val createResult = mockMvc.perform(
            post("/api/v1/admissions")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andReturn()

        val admissionId = objectMapper.readTree(createResult.response.contentAsString)
            .get("data").get("id").asLong()

        // Discharge the patient
        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/discharge")
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )

        // Filter by ACTIVE - should be empty
        mockMvc.perform(
            get("/api/v1/admissions")
                .param("status", "ACTIVE")
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content").isEmpty)

        // Filter by DISCHARGED - should have one
        mockMvc.perform(
            get("/api/v1/admissions")
                .param("status", "DISCHARGED")
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content[0].status").value("DISCHARGED"))
    }

    // ============ GET ADMISSION TESTS ============

    @Test
    fun `get admission should return admission details`() {
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
            get("/api/v1/admissions/$admissionId")
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.patient.firstName").value("Juan"))
            .andExpect(jsonPath("$.data.triageCode").exists())
            .andExpect(jsonPath("$.data.room").exists())
            .andExpect(jsonPath("$.data.treatingPhysician").exists())
    }

    @Test
    fun `get admission should return 404 for non-existent admission`() {
        mockMvc.perform(
            get("/api/v1/admissions/99999")
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )
            .andExpect(status().isNotFound)
    }

    // ============ UPDATE ADMISSION TESTS ============

    @Test
    fun `update admission should update admission data`() {
        val createRequest = createValidAdmissionRequest()
        val createResult = mockMvc.perform(
            post("/api/v1/admissions")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)),
        ).andReturn()

        val admissionId = objectMapper.readTree(createResult.response.contentAsString)
            .get("data").get("id").asLong()

        val updateRequest = UpdateAdmissionRequest(
            triageCodeId = triageCodeId,
            roomId = roomId,
            treatingPhysicianId = doctorUser.id!!,
            inventory = "Updated inventory: Wallet, phone, keys",
        )

        mockMvc.perform(
            put("/api/v1/admissions/$admissionId")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.inventory").value("Updated inventory: Wallet, phone, keys"))
    }

    @Test
    fun `update admission should fail for discharged admission`() {
        val createRequest = createValidAdmissionRequest()
        val createResult = mockMvc.perform(
            post("/api/v1/admissions")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)),
        ).andReturn()

        val admissionId = objectMapper.readTree(createResult.response.contentAsString)
            .get("data").get("id").asLong()

        // Discharge the patient
        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/discharge")
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )

        val updateRequest = UpdateAdmissionRequest(
            triageCodeId = triageCodeId,
            roomId = roomId,
            treatingPhysicianId = doctorUser.id!!,
            inventory = "Should fail",
        )

        mockMvc.perform(
            put("/api/v1/admissions/$admissionId")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.message").value("Cannot update a discharged admission"))
    }

    // ============ DISCHARGE TESTS ============

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

        val patient2Id = createSecondPatient()
        val request2 = createValidAdmissionRequest().copy(patientId = patient2Id)
        mockMvc.perform(
            post("/api/v1/admissions")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)),
        ).andExpect(status().isCreated)

        // Room is now full, third admission should fail
        val patient3Id = createThirdPatient()
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

    // ============ DELETE ADMISSION TESTS ============

    @Test
    fun `delete admission should soft delete admission`() {
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
            delete("/api/v1/admissions/$admissionId")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)

        // Should not be found after deletion
        mockMvc.perform(
            get("/api/v1/admissions/$admissionId")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `delete admission should fail for administrative staff`() {
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
            delete("/api/v1/admissions/$admissionId")
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )
            .andExpect(status().isForbidden)
    }

    // ============ CONSENT DOCUMENT TESTS ============

    @Test
    fun `upload consent document should work for administrative staff`() {
        val request = createValidAdmissionRequest()
        val createResult = mockMvc.perform(
            post("/api/v1/admissions")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andReturn()

        val admissionId = objectMapper.readTree(createResult.response.contentAsString)
            .get("data").get("id").asLong()

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
        val request = createValidAdmissionRequest()
        val createResult = mockMvc.perform(
            post("/api/v1/admissions")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andReturn()

        val admissionId = objectMapper.readTree(createResult.response.contentAsString)
            .get("data").get("id").asLong()

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
        val request = createValidAdmissionRequest()
        val createResult = mockMvc.perform(
            post("/api/v1/admissions")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andReturn()

        val admissionId = objectMapper.readTree(createResult.response.contentAsString)
            .get("data").get("id").asLong()

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
            get("/api/v1/admissions/$admissionId/consent")
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )
            .andExpect(status().isNotFound)
    }

    // ============ SEARCH PATIENTS TESTS ============

    @Test
    fun `search patients should return matching patients`() {
        mockMvc.perform(
            get("/api/v1/admissions/patients/search")
                .param("q", "Juan")
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].firstName").value("Juan"))
    }

    @Test
    fun `search patients should return empty for no matches`() {
        mockMvc.perform(
            get("/api/v1/admissions/patients/search")
                .param("q", "NonExistent")
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").isEmpty)
    }

    // ============ LIST DOCTORS TESTS ============

    @Test
    fun `list doctors should return doctors only`() {
        mockMvc.perform(
            get("/api/v1/admissions/doctors")
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].firstName").value("Dr. Maria"))
            .andExpect(jsonPath("$.data[0].salutation").value("DR"))
    }

    // ============ CONSULTING PHYSICIANS TESTS ============

    @Test
    fun `list consulting physicians should return empty list for new admission`() {
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
            get("/api/v1/admissions/$admissionId/consulting-physicians")
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data").isEmpty)
    }

    @Test
    fun `add consulting physician should return 201`() {
        val admissionId = createAdmissionAndGetId()

        val consultingRequest = AddConsultingPhysicianRequest(
            physicianId = secondDoctorUser.id!!,
            reason = "Cardiology consultation",
            requestedDate = LocalDate.now(),
        )

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/consulting-physicians")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(consultingRequest)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.physician.firstName").value("Dr. Carlos"))
            .andExpect(jsonPath("$.data.reason").value("Cardiology consultation"))
    }

    @Test
    fun `add consulting physician should fail when adding treating physician`() {
        val admissionId = createAdmissionAndGetId()

        val consultingRequest = AddConsultingPhysicianRequest(
            physicianId = doctorUser.id!!,
            reason = "Should fail",
        )

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/consulting-physicians")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(consultingRequest)),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.message").value("Cannot add treating physician as a consulting physician"))
    }

    @Test
    fun `add consulting physician should fail when user is not a doctor`() {
        val admissionId = createAdmissionAndGetId()

        // Use admin user (not a doctor)
        val adminUser = userRepository.findByEmail("admin@example.com")!!

        val consultingRequest = AddConsultingPhysicianRequest(
            physicianId = adminUser.id!!,
            reason = "Should fail",
        )

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/consulting-physicians")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(consultingRequest)),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.message").value("Consulting physician must have the DOCTOR role"))
    }

    @Test
    fun `add consulting physician should fail when physician already assigned`() {
        val admissionId = createAdmissionAndGetId()

        val consultingRequest = AddConsultingPhysicianRequest(
            physicianId = secondDoctorUser.id!!,
            reason = "First assignment",
        )

        // First assignment should succeed
        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/consulting-physicians")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(consultingRequest)),
        )
            .andExpect(status().isCreated)

        // Second assignment should fail
        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/consulting-physicians")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(consultingRequest)),
        )
            .andExpect(status().isBadRequest)
            .andExpect(
                jsonPath("$.error.message")
                    .value("Physician is already assigned as a consultant for this admission"),
            )
    }

    @Test
    fun `remove consulting physician should return 204`() {
        val admissionId = createAdmissionAndGetId()

        val consultingRequest = AddConsultingPhysicianRequest(
            physicianId = secondDoctorUser.id!!,
            reason = "To be removed",
        )

        val addResult = mockMvc.perform(
            post("/api/v1/admissions/$admissionId/consulting-physicians")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(consultingRequest)),
        ).andReturn()

        val consultingPhysicianId = objectMapper.readTree(addResult.response.contentAsString)
            .get("data").get("id").asLong()

        mockMvc.perform(
            delete("/api/v1/admissions/$admissionId/consulting-physicians/$consultingPhysicianId")
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )
            .andExpect(status().isNoContent)

        // Verify it's removed
        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/consulting-physicians")
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").isEmpty)
    }

    @Test
    fun `remove consulting physician should return 404 for non-existent record`() {
        val admissionId = createAdmissionAndGetId()

        mockMvc.perform(
            delete("/api/v1/admissions/$admissionId/consulting-physicians/99999")
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `add consulting physician should fail for doctor role`() {
        val admissionId = createAdmissionAndGetId()

        val consultingRequest = AddConsultingPhysicianRequest(
            physicianId = secondDoctorUser.id!!,
            reason = "Should fail - no permission",
        )

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/consulting-physicians")
                .header("Authorization", "Bearer $doctorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(consultingRequest)),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `get admission should include consulting physicians`() {
        val admissionId = createAdmissionAndGetId()

        val consultingRequest = AddConsultingPhysicianRequest(
            physicianId = secondDoctorUser.id!!,
            reason = "Cardiology consultation",
        )

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/consulting-physicians")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(consultingRequest)),
        )

        mockMvc.perform(
            get("/api/v1/admissions/$admissionId")
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.consultingPhysicians").isArray)
            .andExpect(jsonPath("$.data.consultingPhysicians[0].physician.firstName").value("Dr. Carlos"))
            .andExpect(jsonPath("$.data.consultingPhysicians[0].reason").value("Cardiology consultation"))
    }

    private fun createAdmissionAndGetId(): Long {
        val request = createValidAdmissionRequest()
        val createResult = mockMvc.perform(
            post("/api/v1/admissions")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andReturn()

        return objectMapper.readTree(createResult.response.contentAsString)
            .get("data").get("id").asLong()
    }

    // ============ HELPER METHODS ============

    private fun createSecondPatient(): Long {
        val request = CreatePatientRequest(
            firstName = "Maria",
            lastName = "Lopez",
            age = 35,
            sex = Sex.FEMALE,
            gender = "Femenino",
            maritalStatus = MaritalStatus.SINGLE,
            religion = "Católica",
            educationLevel = EducationLevel.UNIVERSITY,
            occupation = "Doctora",
            address = "5a Avenida 10-20 Zona 2, Guatemala",
            email = "maria.lopez@email.com",
            emergencyContacts = listOf(
                EmergencyContactRequest(
                    name = "Pedro Lopez",
                    relationship = "Hermano",
                    phone = "+502 5555-5678",
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

    private fun createThirdPatient(): Long {
        val request = CreatePatientRequest(
            firstName = "Carlos",
            lastName = "Ramirez",
            age = 50,
            sex = Sex.MALE,
            gender = "Masculino",
            maritalStatus = MaritalStatus.MARRIED,
            religion = "Evangélica",
            educationLevel = EducationLevel.SECONDARY,
            occupation = "Comerciante",
            address = "6a Calle 15-30 Zona 3, Guatemala",
            email = "carlos.ramirez@email.com",
            emergencyContacts = listOf(
                EmergencyContactRequest(
                    name = "Ana Ramirez",
                    relationship = "Esposa",
                    phone = "+502 5555-9999",
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
}
