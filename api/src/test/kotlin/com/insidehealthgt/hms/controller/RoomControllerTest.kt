package com.insidehealthgt.hms.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.insidehealthgt.hms.TestcontainersConfiguration
import com.insidehealthgt.hms.dto.request.CreateAdmissionRequest
import com.insidehealthgt.hms.dto.request.CreatePatientRequest
import com.insidehealthgt.hms.dto.request.CreateRoomRequest
import com.insidehealthgt.hms.dto.request.EmergencyContactRequest
import com.insidehealthgt.hms.dto.request.UpdateRoomRequest
import com.insidehealthgt.hms.dto.response.ApiResponse
import com.insidehealthgt.hms.dto.response.AuthResponse
import com.insidehealthgt.hms.entity.EducationLevel
import com.insidehealthgt.hms.entity.MaritalStatus
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
class RoomControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var roleRepository: RoleRepository

    @Autowired
    private lateinit var roomRepository: RoomRepository

    @Autowired
    private lateinit var admissionRepository: AdmissionRepository

    @Autowired
    private lateinit var patientRepository: PatientRepository

    @Autowired
    private lateinit var triageCodeRepository: TriageCodeRepository

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

    private fun createValidRoomRequest(): CreateRoomRequest = CreateRoomRequest(
        number = "101",
        type = RoomType.PRIVATE,
        capacity = 1,
    )

    // ============ CREATE ROOM TESTS ============

    @Test
    fun `create room with valid data should return 201`() {
        val request = createValidRoomRequest()

        mockMvc.perform(
            post("/api/v1/rooms")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.number").value("101"))
            .andExpect(jsonPath("$.data.type").value("PRIVATE"))
            .andExpect(jsonPath("$.data.capacity").value(1))
    }

    @Test
    fun `create room should fail with duplicate name`() {
        val request = createValidRoomRequest()

        // Create first room
        mockMvc.perform(
            post("/api/v1/rooms")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andExpect(status().isCreated)

        // Try to create duplicate
        mockMvc.perform(
            post("/api/v1/rooms")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `create room should fail for administrative staff`() {
        val request = createValidRoomRequest()

        mockMvc.perform(
            post("/api/v1/rooms")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `create room should fail with invalid capacity`() {
        val request = createValidRoomRequest().copy(capacity = 0)

        mockMvc.perform(
            post("/api/v1/rooms")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `create room should fail with blank number`() {
        val request = mapOf(
            "number" to "",
            "type" to "PRIVATE",
            "capacity" to 1,
        )

        mockMvc.perform(
            post("/api/v1/rooms")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `create room should fail without authentication`() {
        val request = createValidRoomRequest()

        mockMvc.perform(
            post("/api/v1/rooms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isUnauthorized)
    }

    // ============ LIST ROOMS TESTS ============

    @Test
    fun `list rooms should return all rooms`() {
        // Create rooms
        val room1 = createValidRoomRequest()
        mockMvc.perform(
            post("/api/v1/rooms")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(room1)),
        )

        val room2 = CreateRoomRequest(
            number = "201",
            type = RoomType.SHARED,
            capacity = 4,
        )
        mockMvc.perform(
            post("/api/v1/rooms")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(room2)),
        )

        mockMvc.perform(
            get("/api/v1/rooms")
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data.length()").value(2))
    }

    @Test
    fun `list rooms should be accessible by administrative staff`() {
        mockMvc.perform(
            get("/api/v1/rooms")
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `list rooms should be accessible by doctor`() {
        mockMvc.perform(
            get("/api/v1/rooms")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isOk)
    }

    // ============ GET AVAILABLE ROOMS TESTS ============

    @Test
    fun `list available rooms should return rooms with availability`() {
        val room = createValidRoomRequest().copy(capacity = 2)
        mockMvc.perform(
            post("/api/v1/rooms")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(room)),
        )

        mockMvc.perform(
            get("/api/v1/rooms/available")
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].number").value("101"))
            .andExpect(jsonPath("$.data[0].capacity").value(2))
            .andExpect(jsonPath("$.data[0].availableBeds").value(2))
    }

    @Test
    fun `available rooms should decrease after admission`() {
        // Create room with capacity 2
        val roomRequest = createValidRoomRequest().copy(capacity = 2)
        val roomResult = mockMvc.perform(
            post("/api/v1/rooms")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(roomRequest)),
        ).andReturn()

        val roomId = objectMapper.readTree(roomResult.response.contentAsString)
            .get("data").get("id").asLong()

        // Create patient and doctor for admission
        val patientId = createPatient()
        val doctorId = createDoctor()
        val triageCodeId = triageCodeRepository.findAll().first().id!!

        // Create admission
        val admissionRequest = CreateAdmissionRequest(
            patientId = patientId,
            triageCodeId = triageCodeId,
            roomId = roomId,
            treatingPhysicianId = doctorId,
            admissionDate = LocalDateTime.now(),
            inventory = null,
        )

        mockMvc.perform(
            post("/api/v1/admissions")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(admissionRequest)),
        ).andExpect(status().isCreated)

        // Check available beds decreased
        mockMvc.perform(
            get("/api/v1/rooms/available")
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].availableBeds").value(1))
    }

    @Test
    fun `full rooms should not appear in available list`() {
        // Create room with capacity 1
        val roomRequest = createValidRoomRequest().copy(capacity = 1)
        val roomResult = mockMvc.perform(
            post("/api/v1/rooms")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(roomRequest)),
        ).andReturn()

        val roomId = objectMapper.readTree(roomResult.response.contentAsString)
            .get("data").get("id").asLong()

        // Create patient and doctor for admission
        val patientId = createPatient()
        val doctorId = createDoctor()
        val triageCodeId = triageCodeRepository.findAll().first().id!!

        // Create admission to fill the room
        val admissionRequest = CreateAdmissionRequest(
            patientId = patientId,
            triageCodeId = triageCodeId,
            roomId = roomId,
            treatingPhysicianId = doctorId,
            admissionDate = LocalDateTime.now(),
            inventory = null,
        )

        mockMvc.perform(
            post("/api/v1/admissions")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(admissionRequest)),
        ).andExpect(status().isCreated)

        // Room should not appear in available list
        mockMvc.perform(
            get("/api/v1/rooms/available")
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").isEmpty)
    }

    // ============ GET ROOM TESTS ============

    @Test
    fun `get room should return room details`() {
        val request = createValidRoomRequest()
        val createResult = mockMvc.perform(
            post("/api/v1/rooms")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andReturn()

        val roomId = objectMapper.readTree(createResult.response.contentAsString)
            .get("data").get("id").asLong()

        mockMvc.perform(
            get("/api/v1/rooms/$roomId")
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.number").value("101"))
    }

    @Test
    fun `get room should return 404 for non-existent room`() {
        mockMvc.perform(
            get("/api/v1/rooms/99999")
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `get room availability should return room with availability info`() {
        val request = createValidRoomRequest().copy(capacity = 3)
        val createResult = mockMvc.perform(
            post("/api/v1/rooms")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andReturn()

        val roomId = objectMapper.readTree(createResult.response.contentAsString)
            .get("data").get("id").asLong()

        mockMvc.perform(
            get("/api/v1/rooms/$roomId/availability")
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.capacity").value(3))
            .andExpect(jsonPath("$.data.availableBeds").value(3))
    }

    // ============ UPDATE ROOM TESTS ============

    @Test
    fun `update room should update room data`() {
        val createRequest = createValidRoomRequest()
        val createResult = mockMvc.perform(
            post("/api/v1/rooms")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)),
        ).andReturn()

        val roomId = objectMapper.readTree(createResult.response.contentAsString)
            .get("data").get("id").asLong()

        val updateRequest = UpdateRoomRequest(
            number = "101-Updated",
            type = RoomType.SHARED,
            capacity = 4,
        )

        mockMvc.perform(
            put("/api/v1/rooms/$roomId")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.number").value("101-Updated"))
            .andExpect(jsonPath("$.data.type").value("SHARED"))
            .andExpect(jsonPath("$.data.capacity").value(4))
    }

    @Test
    fun `update room should fail for administrative staff`() {
        val createRequest = createValidRoomRequest()
        val createResult = mockMvc.perform(
            post("/api/v1/rooms")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)),
        ).andReturn()

        val roomId = objectMapper.readTree(createResult.response.contentAsString)
            .get("data").get("id").asLong()

        val updateRequest = UpdateRoomRequest(
            number = "101-Updated",
            type = RoomType.SHARED,
            capacity = 4,
        )

        mockMvc.perform(
            put("/api/v1/rooms/$roomId")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `update room should return 404 for non-existent room`() {
        val updateRequest = UpdateRoomRequest(
            number = "101-Updated",
            type = RoomType.SHARED,
            capacity = 4,
        )

        mockMvc.perform(
            put("/api/v1/rooms/99999")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)),
        )
            .andExpect(status().isNotFound)
    }

    // ============ DELETE ROOM TESTS ============

    @Test
    fun `delete room should soft delete room`() {
        val request = createValidRoomRequest()
        val createResult = mockMvc.perform(
            post("/api/v1/rooms")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andReturn()

        val roomId = objectMapper.readTree(createResult.response.contentAsString)
            .get("data").get("id").asLong()

        mockMvc.perform(
            delete("/api/v1/rooms/$roomId")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)

        // Should not be found after deletion
        mockMvc.perform(
            get("/api/v1/rooms/$roomId")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `delete room should fail when room has active admissions`() {
        // Create room
        val roomRequest = createValidRoomRequest()
        val roomResult = mockMvc.perform(
            post("/api/v1/rooms")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(roomRequest)),
        ).andReturn()

        val roomId = objectMapper.readTree(roomResult.response.contentAsString)
            .get("data").get("id").asLong()

        // Create patient and doctor for admission
        val patientId = createPatient()
        val doctorId = createDoctor()
        val triageCodeId = triageCodeRepository.findAll().first().id!!

        // Create admission
        val admissionRequest = CreateAdmissionRequest(
            patientId = patientId,
            triageCodeId = triageCodeId,
            roomId = roomId,
            treatingPhysicianId = doctorId,
            admissionDate = LocalDateTime.now(),
            inventory = null,
        )

        mockMvc.perform(
            post("/api/v1/admissions")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(admissionRequest)),
        ).andExpect(status().isCreated)

        // Try to delete room - should fail
        mockMvc.perform(
            delete("/api/v1/rooms/$roomId")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `delete room should fail for administrative staff`() {
        val request = createValidRoomRequest()
        val createResult = mockMvc.perform(
            post("/api/v1/rooms")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andReturn()

        val roomId = objectMapper.readTree(createResult.response.contentAsString)
            .get("data").get("id").asLong()

        mockMvc.perform(
            delete("/api/v1/rooms/$roomId")
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
