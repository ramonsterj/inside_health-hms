package com.insidehealthgt.hms.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.insidehealthgt.hms.TestcontainersConfiguration
import com.insidehealthgt.hms.dto.request.CreateAdmissionRequest
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
import com.insidehealthgt.hms.repository.AdmissionConsentDocumentRepository
import com.insidehealthgt.hms.repository.AdmissionConsultingPhysicianRepository
import com.insidehealthgt.hms.repository.AdmissionDocumentRepository
import com.insidehealthgt.hms.repository.AdmissionRepository
import com.insidehealthgt.hms.repository.AuditLogRepository
import com.insidehealthgt.hms.repository.ClinicalHistoryRepository
import com.insidehealthgt.hms.repository.DocumentTypeRepository
import com.insidehealthgt.hms.repository.EmergencyContactRepository
import com.insidehealthgt.hms.repository.InventoryCategoryRepository
import com.insidehealthgt.hms.repository.InventoryItemRepository
import com.insidehealthgt.hms.repository.InventoryMovementRepository
import com.insidehealthgt.hms.repository.InvoiceRepository
import com.insidehealthgt.hms.repository.MedicalOrderRepository
import com.insidehealthgt.hms.repository.NursingNoteRepository
import com.insidehealthgt.hms.repository.PasswordResetTokenRepository
import com.insidehealthgt.hms.repository.PatientChargeRepository
import com.insidehealthgt.hms.repository.PatientIdDocumentRepository
import com.insidehealthgt.hms.repository.PatientRepository
import com.insidehealthgt.hms.repository.PermissionRepository
import com.insidehealthgt.hms.repository.ProgressNoteRepository
import com.insidehealthgt.hms.repository.PsychotherapyActivityRepository
import com.insidehealthgt.hms.repository.PsychotherapyCategoryRepository
import com.insidehealthgt.hms.repository.RefreshTokenRepository
import com.insidehealthgt.hms.repository.RoleRepository
import com.insidehealthgt.hms.repository.RoomRepository
import com.insidehealthgt.hms.repository.TriageCodeRepository
import com.insidehealthgt.hms.repository.UserPhoneNumberRepository
import com.insidehealthgt.hms.repository.UserRepository
import com.insidehealthgt.hms.repository.VitalSignRepository
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class)
abstract class AbstractIntegrationTest {

    @Autowired
    protected lateinit var mockMvc: MockMvc

    @Autowired
    protected lateinit var objectMapper: ObjectMapper

    @Autowired
    protected lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    protected lateinit var jdbcTemplate: JdbcTemplate

    // ============ REPOSITORIES ============

    @Autowired
    protected lateinit var admissionConsentDocumentRepository: AdmissionConsentDocumentRepository

    @Autowired
    protected lateinit var admissionConsultingPhysicianRepository: AdmissionConsultingPhysicianRepository

    @Autowired
    protected lateinit var admissionDocumentRepository: AdmissionDocumentRepository

    @Autowired
    protected lateinit var admissionRepository: AdmissionRepository

    @Autowired
    protected lateinit var auditLogRepository: AuditLogRepository

    @Autowired
    protected lateinit var clinicalHistoryRepository: ClinicalHistoryRepository

    @Autowired
    protected lateinit var documentTypeRepository: DocumentTypeRepository

    @Autowired
    protected lateinit var inventoryCategoryRepository: InventoryCategoryRepository

    @Autowired
    protected lateinit var inventoryItemRepository: InventoryItemRepository

    @Autowired
    protected lateinit var inventoryMovementRepository: InventoryMovementRepository

    @Autowired
    protected lateinit var invoiceRepository: InvoiceRepository

    @Autowired
    protected lateinit var emergencyContactRepository: EmergencyContactRepository

    @Autowired
    protected lateinit var medicalOrderRepository: MedicalOrderRepository

    @Autowired
    protected lateinit var nursingNoteRepository: NursingNoteRepository

    @Autowired
    protected lateinit var passwordResetTokenRepository: PasswordResetTokenRepository

    @Autowired
    protected lateinit var patientChargeRepository: PatientChargeRepository

    @Autowired
    protected lateinit var patientIdDocumentRepository: PatientIdDocumentRepository

    @Autowired
    protected lateinit var patientRepository: PatientRepository

    @Autowired
    protected lateinit var permissionRepository: PermissionRepository

    @Autowired
    protected lateinit var progressNoteRepository: ProgressNoteRepository

    @Autowired
    protected lateinit var psychotherapyActivityRepository: PsychotherapyActivityRepository

    @Autowired
    protected lateinit var psychotherapyCategoryRepository: PsychotherapyCategoryRepository

    @Autowired
    protected lateinit var refreshTokenRepository: RefreshTokenRepository

    @Autowired
    protected lateinit var roleRepository: RoleRepository

    @Autowired
    protected lateinit var roomRepository: RoomRepository

    @Autowired
    protected lateinit var triageCodeRepository: TriageCodeRepository

    @Autowired
    protected lateinit var userPhoneNumberRepository: UserPhoneNumberRepository

    @Autowired
    protected lateinit var userRepository: UserRepository

    @Autowired
    protected lateinit var vitalSignRepository: VitalSignRepository

    // ============ DATABASE CLEANUP ============

    @BeforeEach
    fun cleanDatabase() {
        // Hard delete all test data in FK dependency order
        // Uses native SQL to bypass @SQLRestriction soft delete filters
        // Reference tables (triage_codes, psychotherapy_categories)
        // contain migration-seeded data. Only test-created rows are removed
        // (seeded rows have created_by IS NULL since they come from Flyway).
        jdbcTemplate.execute("DELETE FROM patient_charges")
        jdbcTemplate.execute("DELETE FROM invoices")
        jdbcTemplate.execute("DELETE FROM inventory_movements")
        jdbcTemplate.execute("DELETE FROM inventory_items")
        jdbcTemplate.execute("DELETE FROM inventory_categories WHERE created_by IS NOT NULL")
        jdbcTemplate.execute("DELETE FROM nursing_notes")
        jdbcTemplate.execute("DELETE FROM vital_signs")
        jdbcTemplate.execute("DELETE FROM psychotherapy_activities")
        jdbcTemplate.execute("DELETE FROM medical_orders")
        jdbcTemplate.execute("DELETE FROM progress_notes")
        jdbcTemplate.execute("DELETE FROM clinical_histories")
        jdbcTemplate.execute("DELETE FROM admission_consulting_physicians")
        jdbcTemplate.execute("DELETE FROM admission_consent_documents")
        jdbcTemplate.execute("DELETE FROM admission_documents")
        jdbcTemplate.execute("DELETE FROM admissions")
        jdbcTemplate.execute("DELETE FROM triage_codes WHERE created_by IS NOT NULL")
        jdbcTemplate.execute("DELETE FROM psychotherapy_categories WHERE created_by IS NOT NULL")
        jdbcTemplate.execute("DELETE FROM emergency_contacts")
        jdbcTemplate.execute("DELETE FROM patient_id_documents")
        jdbcTemplate.execute("DELETE FROM patients")
        jdbcTemplate.execute("DELETE FROM rooms")
        jdbcTemplate.execute("DELETE FROM password_reset_tokens")
        jdbcTemplate.execute("DELETE FROM refresh_tokens")
        jdbcTemplate.execute("DELETE FROM audit_logs")
        jdbcTemplate.execute("DELETE FROM user_phone_numbers")
        jdbcTemplate.execute("DELETE FROM user_roles")
        jdbcTemplate.execute("DELETE FROM users")
    }

    // ============ AUTH HELPERS ============

    protected fun loginAndGetToken(email: String, password: String): String =
        loginAndGetAuthResponse(email, password).accessToken

    protected fun loginAndGetAuthResponse(email: String, password: String): AuthResponse {
        val request = mapOf("identifier" to email, "password" to password)
        val result = mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andExpect(status().isOk)
            .andReturn()

        val responseType = objectMapper.typeFactory.constructParametricType(
            ApiResponse::class.java,
            AuthResponse::class.java,
        )
        val response: ApiResponse<AuthResponse> = objectMapper.readValue(
            result.response.contentAsString,
            responseType,
        )
        return response.data ?: error("Login failed for $email: ${result.response.contentAsString}")
    }

    // ============ USER CREATION HELPERS ============

    protected fun createUserWithRole(
        roleCode: String,
        username: String,
        email: String,
        password: String,
        firstName: String? = null,
        lastName: String? = null,
        salutation: Salutation? = null,
    ): Pair<User, String> {
        val role = roleRepository.findByCode(roleCode)!!
        val user = User(
            username = username,
            email = email,
            passwordHash = passwordEncoder.encode(password)!!,
            firstName = firstName,
            lastName = lastName,
            salutation = salutation,
            mustChangePassword = false,
        )
        user.roles.add(role)
        val savedUser = userRepository.save(user)
        val token = loginAndGetToken(email, password)
        return Pair(savedUser, token)
    }

    protected fun createAdminUser(): Pair<User, String> = createUserWithRole(
        roleCode = "ADMIN",
        username = "admin",
        email = "admin@example.com",
        password = "admin123",
        firstName = "Admin",
        lastName = "User",
    )

    protected fun createDoctorUser(): Pair<User, String> = createUserWithRole(
        roleCode = "DOCTOR",
        username = "doctor",
        email = "doctor@example.com",
        password = "password123",
        firstName = "Dr. Maria",
        lastName = "Garcia",
        salutation = Salutation.DR,
    )

    protected fun createNurseUser(): Pair<User, String> = createUserWithRole(
        roleCode = "NURSE",
        username = "nurse",
        email = "nurse@example.com",
        password = "password123",
        firstName = "Nurse",
        lastName = "Johnson",
    )

    protected fun createAdminStaffUser(): Pair<User, String> = createUserWithRole(
        roleCode = "ADMINISTRATIVE_STAFF",
        username = "receptionist",
        email = "receptionist@example.com",
        password = "password123",
        firstName = "Reception",
        lastName = "Staff",
    )

    protected fun createPsychologistUser(): Pair<User, String> = createUserWithRole(
        roleCode = "PSYCHOLOGIST",
        username = "psychologist",
        email = "psychologist@example.com",
        password = "password123",
        firstName = "Psych",
        lastName = "Expert",
    )

    // ============ ENTITY CREATION HELPERS ============

    protected fun createPatient(token: String): Long {
        val request = CreatePatientRequest(
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
                EmergencyContactRequest(
                    name = "Maria",
                    relationship = "Esposa",
                    phone = "555-1234",
                ),
            ),
        )

        val result = mockMvc.perform(
            post("/api/v1/patients")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andExpect(status().isCreated)
            .andReturn()

        return objectMapper.readTree(result.response.contentAsString)
            .get("data").get("id").asLong()
    }

    protected fun createAdmission(
        token: String,
        patientId: Long,
        doctorId: Long,
        type: AdmissionType = AdmissionType.AMBULATORY,
        roomId: Long? = null,
        triageCodeId: Long? = null,
        admissionDate: LocalDateTime = LocalDateTime.now(),
    ): Long {
        val request = CreateAdmissionRequest(
            patientId = patientId,
            treatingPhysicianId = doctorId,
            admissionDate = admissionDate,
            type = type,
            roomId = roomId,
            triageCodeId = triageCodeId,
        )

        val result = mockMvc.perform(
            post("/api/v1/admissions")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andExpect(status().isCreated)
            .andReturn()

        return objectMapper.readTree(result.response.contentAsString)
            .get("data").get("id").asLong()
    }

    protected fun createSecondPatient(token: String): Long {
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
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andExpect(status().isCreated)
            .andReturn()

        return objectMapper.readTree(result.response.contentAsString)
            .get("data").get("id").asLong()
    }

    protected fun createThirdPatient(token: String): Long {
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
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andExpect(status().isCreated)
            .andReturn()

        return objectMapper.readTree(result.response.contentAsString)
            .get("data").get("id").asLong()
    }

    protected fun dischargeAdmission(id: Long, token: String) {
        mockMvc.perform(
            post("/api/v1/admissions/$id/discharge")
                .header("Authorization", "Bearer $token"),
        ).andExpect(status().isOk)
    }
}
