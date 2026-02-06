package com.insidehealthgt.hms.controller

import com.insidehealthgt.hms.dto.request.CreatePatientRequest
import com.insidehealthgt.hms.dto.request.EmergencyContactRequest
import com.insidehealthgt.hms.dto.request.UpdatePatientRequest
import com.insidehealthgt.hms.entity.EducationLevel
import com.insidehealthgt.hms.entity.MaritalStatus
import com.insidehealthgt.hms.entity.Sex
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class PatientControllerTest : AbstractIntegrationTest() {

    private lateinit var adminToken: String
    private lateinit var administrativeStaffToken: String
    private lateinit var doctorToken: String

    @BeforeEach
    fun setUp() {
        val (_, adminTkn) = createAdminUser()
        adminToken = adminTkn

        val (_, staffTkn) = createAdminStaffUser()
        administrativeStaffToken = staffTkn

        val (_, doctorTkn) = createDoctorUser()
        doctorToken = doctorTkn
    }

    private fun createValidPatientRequest(): CreatePatientRequest = CreatePatientRequest(
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
        notes = "Paciente referido por Dr. López",
        emergencyContacts = listOf(
            EmergencyContactRequest(
                name = "María de Pérez",
                relationship = "Esposa",
                phone = "+502 5555-1234",
            ),
        ),
    )

    // ============ CREATE PATIENT TESTS ============

    @Test
    fun `create patient with valid data should return 201`() {
        val request = createValidPatientRequest()

        mockMvc.perform(
            post("/api/v1/patients")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.firstName").value("Juan"))
            .andExpect(jsonPath("$.data.lastName").value("Pérez García"))
            .andExpect(jsonPath("$.data.age").value(45))
            .andExpect(jsonPath("$.data.sex").value("MALE"))
            .andExpect(jsonPath("$.data.emergencyContacts").isArray)
            .andExpect(jsonPath("$.data.emergencyContacts[0].name").value("María de Pérez"))
            .andExpect(jsonPath("$.data.createdBy").exists())
    }

    @Test
    fun `create patient should fail without emergency contact`() {
        val request = CreatePatientRequest(
            firstName = "Juan",
            lastName = "Pérez",
            age = 45,
            sex = Sex.MALE,
            gender = "Masculino",
            maritalStatus = MaritalStatus.MARRIED,
            religion = "Católica",
            educationLevel = EducationLevel.UNIVERSITY,
            occupation = "Ingeniero",
            address = "4a Calle 5-67 Zona 1, Guatemala",
            email = "juan.perez@email.com",
            emergencyContacts = emptyList(),
        )

        mockMvc.perform(
            post("/api/v1/patients")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
    }

    @Test
    fun `create patient should fail with missing required fields`() {
        val request = mapOf(
            "firstName" to "Juan",
            "emergencyContacts" to listOf(
                mapOf("name" to "Contact", "relationship" to "Friend", "phone" to "12345"),
            ),
        )

        mockMvc.perform(
            post("/api/v1/patients")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `create patient should fail with invalid email`() {
        val request = createValidPatientRequest().copy(email = "invalid-email")

        mockMvc.perform(
            post("/api/v1/patients")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `create patient should fail with age out of range`() {
        val request = createValidPatientRequest().copy(age = 200)

        mockMvc.perform(
            post("/api/v1/patients")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `create patient should return 409 for potential duplicate`() {
        val request = createValidPatientRequest()

        // Create first patient
        mockMvc.perform(
            post("/api/v1/patients")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andExpect(status().isCreated)

        // Try to create duplicate (same name + age)
        val duplicateRequest = request.copy(email = "different@email.com")
        mockMvc.perform(
            post("/api/v1/patients")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicateRequest)),
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.data.potentialDuplicates").isArray)
            .andExpect(jsonPath("$.data.potentialDuplicates[0].firstName").value("Juan"))
    }

    @Test
    fun `create patient should fail for doctor (clinical staff)`() {
        val request = createValidPatientRequest()

        mockMvc.perform(
            post("/api/v1/patients")
                .header("Authorization", "Bearer $doctorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `create patient should fail without authentication`() {
        val request = createValidPatientRequest()

        mockMvc.perform(
            post("/api/v1/patients")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isUnauthorized)
    }

    // ============ LIST PATIENTS TESTS ============

    @Test
    fun `list patients should return paginated results`() {
        val request = createValidPatientRequest()
        mockMvc.perform(
            post("/api/v1/patients")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )

        mockMvc.perform(
            get("/api/v1/patients")
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content").isArray)
            .andExpect(jsonPath("$.data.content[0].firstName").value("Juan"))
    }

    @Test
    fun `list patients should work with search parameter`() {
        val request = createValidPatientRequest()
        mockMvc.perform(
            post("/api/v1/patients")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )

        mockMvc.perform(
            get("/api/v1/patients")
                .param("search", "Juan")
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content[0].firstName").value("Juan"))

        mockMvc.perform(
            get("/api/v1/patients")
                .param("search", "NonExistent")
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content").isEmpty)
    }

    @Test
    fun `list patients should be accessible by doctor`() {
        mockMvc.perform(
            get("/api/v1/patients")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isOk)
    }

    // ============ GET PATIENT TESTS ============

    @Test
    fun `get patient should return patient details`() {
        val request = createValidPatientRequest()
        val createResult = mockMvc.perform(
            post("/api/v1/patients")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andReturn()

        val patientId = objectMapper.readTree(createResult.response.contentAsString)
            .get("data").get("id").asLong()

        mockMvc.perform(
            get("/api/v1/patients/$patientId")
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.firstName").value("Juan"))
            .andExpect(jsonPath("$.data.emergencyContacts").isArray)
    }

    @Test
    fun `get patient should return 404 for non-existent patient`() {
        mockMvc.perform(
            get("/api/v1/patients/99999")
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `get patient should be accessible by doctor`() {
        val request = createValidPatientRequest()
        val createResult = mockMvc.perform(
            post("/api/v1/patients")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andReturn()

        val patientId = objectMapper.readTree(createResult.response.contentAsString)
            .get("data").get("id").asLong()

        mockMvc.perform(
            get("/api/v1/patients/$patientId")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isOk)
    }

    // ============ UPDATE PATIENT TESTS ============

    @Test
    fun `update patient should update patient data`() {
        val createRequest = createValidPatientRequest()
        val createResult = mockMvc.perform(
            post("/api/v1/patients")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)),
        ).andReturn()

        val patientId = objectMapper.readTree(createResult.response.contentAsString)
            .get("data").get("id").asLong()

        val updateRequest = UpdatePatientRequest(
            firstName = "Juan Carlos",
            lastName = "Pérez García",
            age = 46,
            sex = Sex.MALE,
            gender = "Masculino",
            maritalStatus = MaritalStatus.MARRIED,
            religion = "Católica",
            educationLevel = EducationLevel.UNIVERSITY,
            occupation = "Ingeniero Senior",
            address = "Nueva Dirección, Guatemala",
            email = "juan.perez@email.com",
            idDocumentNumber = "1234567890101",
            emergencyContacts = listOf(
                EmergencyContactRequest(
                    name = "María de Pérez",
                    relationship = "Esposa",
                    phone = "+502 5555-1234",
                ),
                EmergencyContactRequest(
                    name = "Carlos Pérez",
                    relationship = "Hijo",
                    phone = "+502 5555-5678",
                ),
            ),
        )

        mockMvc.perform(
            put("/api/v1/patients/$patientId")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.firstName").value("Juan Carlos"))
            .andExpect(jsonPath("$.data.age").value(46))
            .andExpect(jsonPath("$.data.occupation").value("Ingeniero Senior"))
            .andExpect(jsonPath("$.data.emergencyContacts.length()").value(2))
    }

    @Test
    fun `update patient should fail for doctor`() {
        val createRequest = createValidPatientRequest()
        val createResult = mockMvc.perform(
            post("/api/v1/patients")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)),
        ).andReturn()

        val patientId = objectMapper.readTree(createResult.response.contentAsString)
            .get("data").get("id").asLong()

        val updateRequest = UpdatePatientRequest(
            firstName = "Juan Carlos",
            lastName = "Pérez García",
            age = 46,
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

        mockMvc.perform(
            put("/api/v1/patients/$patientId")
                .header("Authorization", "Bearer $doctorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `update patient should return 404 for non-existent patient`() {
        val updateRequest = UpdatePatientRequest(
            firstName = "Test",
            lastName = "User",
            age = 30,
            sex = Sex.MALE,
            gender = "Male",
            maritalStatus = MaritalStatus.SINGLE,
            religion = "None",
            educationLevel = EducationLevel.UNIVERSITY,
            occupation = "Engineer",
            address = "Test Address",
            email = "test@example.com",
            emergencyContacts = listOf(
                EmergencyContactRequest(
                    name = "Contact",
                    relationship = "Friend",
                    phone = "12345",
                ),
            ),
        )

        mockMvc.perform(
            put("/api/v1/patients/99999")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)),
        )
            .andExpect(status().isNotFound)
    }

    // ============ ID DOCUMENT TESTS ============

    @Test
    fun `upload ID document should work for administrative staff`() {
        val createRequest = createValidPatientRequest()
        val createResult = mockMvc.perform(
            post("/api/v1/patients")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)),
        ).andReturn()

        val patientId = objectMapper.readTree(createResult.response.contentAsString)
            .get("data").get("id").asLong()

        val mockFile = MockMultipartFile(
            "file",
            "id-document.jpg",
            MediaType.IMAGE_JPEG_VALUE,
            "fake-image-content".toByteArray(),
        )

        mockMvc.perform(
            multipart("/api/v1/patients/$patientId/id-document")
                .file(mockFile)
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.hasIdDocument").value(true))
    }

    @Test
    fun `upload ID document should fail for doctor`() {
        val createRequest = createValidPatientRequest()
        val createResult = mockMvc.perform(
            post("/api/v1/patients")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)),
        ).andReturn()

        val patientId = objectMapper.readTree(createResult.response.contentAsString)
            .get("data").get("id").asLong()

        val mockFile = MockMultipartFile(
            "file",
            "id-document.jpg",
            MediaType.IMAGE_JPEG_VALUE,
            "fake-image-content".toByteArray(),
        )

        mockMvc.perform(
            multipart("/api/v1/patients/$patientId/id-document")
                .file(mockFile)
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `upload ID document should fail for invalid file type`() {
        val createRequest = createValidPatientRequest()
        val createResult = mockMvc.perform(
            post("/api/v1/patients")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)),
        ).andReturn()

        val patientId = objectMapper.readTree(createResult.response.contentAsString)
            .get("data").get("id").asLong()

        val mockFile = MockMultipartFile(
            "file",
            "document.txt",
            MediaType.TEXT_PLAIN_VALUE,
            "text content".toByteArray(),
        )

        mockMvc.perform(
            multipart("/api/v1/patients/$patientId/id-document")
                .file(mockFile)
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `download ID document should work for administrative staff`() {
        val createRequest = createValidPatientRequest()
        val createResult = mockMvc.perform(
            post("/api/v1/patients")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)),
        ).andReturn()

        val patientId = objectMapper.readTree(createResult.response.contentAsString)
            .get("data").get("id").asLong()

        val mockFile = MockMultipartFile(
            "file",
            "id-document.jpg",
            MediaType.IMAGE_JPEG_VALUE,
            "fake-image-content".toByteArray(),
        )

        mockMvc.perform(
            multipart("/api/v1/patients/$patientId/id-document")
                .file(mockFile)
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )

        mockMvc.perform(
            get("/api/v1/patients/$patientId/id-document")
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `download ID document should fail for doctor`() {
        val createRequest = createValidPatientRequest()
        val createResult = mockMvc.perform(
            post("/api/v1/patients")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)),
        ).andReturn()

        val patientId = objectMapper.readTree(createResult.response.contentAsString)
            .get("data").get("id").asLong()

        val mockFile = MockMultipartFile(
            "file",
            "id-document.jpg",
            MediaType.IMAGE_JPEG_VALUE,
            "fake-image-content".toByteArray(),
        )

        mockMvc.perform(
            multipart("/api/v1/patients/$patientId/id-document")
                .file(mockFile)
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )

        mockMvc.perform(
            get("/api/v1/patients/$patientId/id-document")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `delete ID document should work for administrative staff`() {
        val createRequest = createValidPatientRequest()
        val createResult = mockMvc.perform(
            post("/api/v1/patients")
                .header("Authorization", "Bearer $administrativeStaffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)),
        ).andReturn()

        val patientId = objectMapper.readTree(createResult.response.contentAsString)
            .get("data").get("id").asLong()

        val mockFile = MockMultipartFile(
            "file",
            "id-document.jpg",
            MediaType.IMAGE_JPEG_VALUE,
            "fake-image-content".toByteArray(),
        )

        mockMvc.perform(
            multipart("/api/v1/patients/$patientId/id-document")
                .file(mockFile)
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )

        mockMvc.perform(
            delete("/api/v1/patients/$patientId/id-document")
                .header("Authorization", "Bearer $administrativeStaffToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.hasIdDocument").value(false))
    }
}
