package com.insidehealthgt.hms.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.insidehealthgt.hms.TestcontainersConfiguration
import com.insidehealthgt.hms.dto.request.CreateAdmissionRequest
import com.insidehealthgt.hms.dto.request.CreatePatientRequest
import com.insidehealthgt.hms.dto.request.EmergencyContactRequest
import com.insidehealthgt.hms.dto.response.ApiResponse
import com.insidehealthgt.hms.dto.response.AuthResponse
import com.insidehealthgt.hms.entity.AdmissionType
import com.insidehealthgt.hms.entity.BankAccount
import com.insidehealthgt.hms.entity.BankAccountType
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
import com.insidehealthgt.hms.repository.BankAccountRepository
import com.insidehealthgt.hms.repository.ClinicalHistoryRepository
import com.insidehealthgt.hms.repository.DocumentTypeRepository
import com.insidehealthgt.hms.repository.EmergencyContactRepository
import com.insidehealthgt.hms.repository.ExpenseRepository
import com.insidehealthgt.hms.repository.IncomeRepository
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
import com.insidehealthgt.hms.repository.PayrollEntryRepository
import com.insidehealthgt.hms.repository.PermissionRepository
import com.insidehealthgt.hms.repository.ProgressNoteRepository
import com.insidehealthgt.hms.repository.PsychotherapyActivityRepository
import com.insidehealthgt.hms.repository.PsychotherapyCategoryRepository
import com.insidehealthgt.hms.repository.RefreshTokenRepository
import com.insidehealthgt.hms.repository.RoleRepository
import com.insidehealthgt.hms.repository.RoomRepository
import com.insidehealthgt.hms.repository.SalaryHistoryRepository
import com.insidehealthgt.hms.repository.TreasuryEmployeeRepository
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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

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
    protected lateinit var bankAccountRepository: BankAccountRepository

    @Autowired
    protected lateinit var clinicalHistoryRepository: ClinicalHistoryRepository

    @Autowired
    protected lateinit var documentTypeRepository: DocumentTypeRepository

    @Autowired
    protected lateinit var incomeRepository: IncomeRepository

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
    protected lateinit var expenseRepository: ExpenseRepository

    @Autowired
    protected lateinit var medicalOrderRepository: MedicalOrderRepository

    @Autowired
    protected lateinit var nursingNoteRepository: NursingNoteRepository

    @Autowired
    protected lateinit var passwordResetTokenRepository: PasswordResetTokenRepository

    @Autowired
    protected lateinit var patientChargeRepository: PatientChargeRepository

    @Autowired
    protected lateinit var payrollEntryRepository: PayrollEntryRepository

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
    protected lateinit var salaryHistoryRepository: SalaryHistoryRepository

    @Autowired
    protected lateinit var treasuryEmployeeRepository: TreasuryEmployeeRepository

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
        jdbcTemplate.execute("DELETE FROM doctor_fees")
        jdbcTemplate.execute("DELETE FROM payroll_entries")
        jdbcTemplate.execute("DELETE FROM salary_history")
        jdbcTemplate.execute("DELETE FROM income_records")
        jdbcTemplate.execute("DELETE FROM expense_payments")
        jdbcTemplate.execute("DELETE FROM expenses")
        jdbcTemplate.execute("DELETE FROM treasury_employees")
        jdbcTemplate.execute("DELETE FROM bank_accounts")
        // Warehouse charges reference patient_charges/admissions/items/lots — clear first.
        jdbcTemplate.execute("DELETE FROM warehouse_charges")
        jdbcTemplate.execute("DELETE FROM medication_administrations")
        jdbcTemplate.execute("DELETE FROM patient_charges")
        jdbcTemplate.execute("DELETE FROM invoices")
        jdbcTemplate.execute("DELETE FROM inventory_movements")
        // Warehouse stock + transfers reference items/lots/warehouses; movements
        // reference transfers, so they come after inventory_movements above and
        // before inventory_lots/inventory_items below. warehouses + role_default_warehouses
        // are migration-seeded reference data and are intentionally NOT deleted.
        jdbcTemplate.execute("DELETE FROM inventory_warehouse_stock")
        jdbcTemplate.execute("DELETE FROM inventory_transfers")
        // medical_orders.inventory_item_id references inventory_items, so orders must be
        // removed first. Lab line items reference medical_orders + lab_provider_tests, so
        // they come before both.
        jdbcTemplate.execute("DELETE FROM medical_order_lab_tests")
        jdbcTemplate.execute("DELETE FROM medical_order_documents")
        jdbcTemplate.execute("DELETE FROM medical_orders")
        jdbcTemplate.execute("DELETE FROM inventory_lots")
        jdbcTemplate.execute("DELETE FROM medication_details")
        jdbcTemplate.execute("DELETE FROM inventory_items")
        jdbcTemplate.execute("DELETE FROM inventory_categories WHERE created_by IS NOT NULL")
        jdbcTemplate.execute("DELETE FROM nursing_notes")
        jdbcTemplate.execute("DELETE FROM vital_signs")
        jdbcTemplate.execute("DELETE FROM psychotherapy_activities")
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
        jdbcTemplate.execute("DELETE FROM user_warehouses")
        jdbcTemplate.execute("DELETE FROM user_roles")
        jdbcTemplate.execute("DELETE FROM users")
        // Lab catalog: V126 seeds reference rows (created_by IS NULL); keep those, drop only
        // test-created rows. Children before parents.
        jdbcTemplate.execute("DELETE FROM lab_panel_items WHERE created_by IS NOT NULL")
        jdbcTemplate.execute("DELETE FROM lab_panels WHERE created_by IS NOT NULL")
        jdbcTemplate.execute("DELETE FROM lab_provider_tests WHERE created_by IS NOT NULL")
        jdbcTemplate.execute("DELETE FROM lab_tests WHERE created_by IS NOT NULL")
        jdbcTemplate.execute("DELETE FROM lab_providers WHERE created_by IS NOT NULL")
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
        extraRoleCodes: List<String> = emptyList(),
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
        extraRoleCodes.forEach { extra ->
            roleRepository.findByCode(extra)?.let { user.roles.add(it) }
        }
        val savedUser = userRepository.save(user)
        val token = loginAndGetToken(email, password)
        return Pair(savedUser, token)
    }

    // Admin does NOT carry RESIDENT_DOCTOR, mirroring production (V122): ADMIN is
    // a code-level exception in AdmissionService.resolveResident() and admits by
    // picking a resident (residentId), not by being one. The shared createAdmission
    // helper supplies that residentId automatically.
    protected fun createAdminUser(): Pair<User, String> = createUserWithRole(
        roleCode = "ADMINISTRADOR",
        username = "admin",
        email = "admin@example.com",
        password = "admin123",
        firstName = "Admin",
        lastName = "User",
    )

    protected fun createResidentUser(): Pair<User, String> = createUserWithRole(
        roleCode = "MEDICO_RESIDENTE",
        username = "resident",
        email = "resident@example.com",
        password = "password123",
        firstName = "Dr. Andrea",
        lastName = "Pineda",
        salutation = Salutation.DRA,
    )

    protected fun createDoctorUser(): Pair<User, String> = createUserWithRole(
        roleCode = "MEDICO",
        username = "doctor",
        email = "doctor@example.com",
        password = "password123",
        firstName = "Dr. Maria",
        lastName = "Garcia",
        salutation = Salutation.DR,
    )

    protected fun createNurseUser(): Pair<User, String> = createUserWithRole(
        roleCode = "ENFERMERO",
        username = "nurse",
        email = "nurse@example.com",
        password = "password123",
        firstName = "Nurse",
        lastName = "Johnson",
    )

    protected fun createChiefNurseUser(): Pair<User, String> = createUserWithRole(
        roleCode = "JEFE_ENFERMERIA",
        username = "chiefnurse",
        email = "chiefnurse@example.com",
        password = "password123",
        firstName = "Chief",
        lastName = "Flores",
    )

    // Administrative staff does NOT carry RESIDENT_DOCTOR, mirroring production:
    // only residents (and admins, who pick a resident) may register admissions.
    // Fixtures that need an admission register it through the admin or resident
    // token via the shared createAdmission helper.
    protected fun createAdminStaffUser(): Pair<User, String> = createUserWithRole(
        roleCode = "PERSONAL_ADMINISTRATIVO",
        username = "receptionist",
        email = "receptionist@example.com",
        password = "password123",
        firstName = "Reception",
        lastName = "Staff",
    )

    protected fun createPsychologistUser(): Pair<User, String> = createUserWithRole(
        roleCode = "PSICOLOGO",
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
            dateOfBirth = guatemalaToday().minusYears(45),
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

    // Ensures at least one RESIDENT_DOCTOR exists and returns its id. Used by the
    // shared createAdmission helper so admin-token callers can satisfy the new
    // "admins must pick a resident" rule without each suite seeding one.
    protected fun ensureFixtureResidentId(): Long {
        userRepository.findByRoleCode("MEDICO_RESIDENTE").firstOrNull()?.let { return it.id!! }
        val role = roleRepository.findByCode("MEDICO_RESIDENTE")!!
        val resident = User(
            username = "fixture_resident",
            email = "fixture.resident@example.com",
            passwordHash = passwordEncoder.encode("password123")!!,
            firstName = "Fixture",
            lastName = "Resident",
            mustChangePassword = false,
        )
        resident.roles.add(role)
        return userRepository.save(resident).id!!
    }

    protected fun createAdmission(
        token: String,
        patientId: Long,
        doctorId: Long,
        type: AdmissionType = AdmissionType.AMBULATORY,
        roomId: Long? = null,
        triageCodeId: Long? = null,
        admissionDate: LocalDateTime = LocalDateTime.now(),
        residentId: Long? = null,
    ): Long {
        val request = CreateAdmissionRequest(
            patientId = patientId,
            treatingPhysicianId = doctorId,
            admissionDate = admissionDate,
            type = type,
            roomId = roomId,
            triageCodeId = triageCodeId,
            // Honored only when the posting token is an admin; residents auto-bind
            // to themselves. Default to any seeded resident so admin-token fixtures
            // keep working.
            residentId = residentId ?: ensureFixtureResidentId(),
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
            dateOfBirth = guatemalaToday().minusYears(35),
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
            dateOfBirth = guatemalaToday().minusYears(50),
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
        // Discharge is restricted to ADMINISTRADOR / MEDICO_RESIDENTE and requires a note;
        // callers must pass a discharge-capable token (e.g. adminToken / residentToken).
        mockMvc.perform(
            post("/api/v1/admissions/$id/discharge")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"dischargeNote": "Discharged (test)"}"""),
        ).andExpect(status().isOk)
    }

    protected fun createBankAccount(name: String = "Test Account"): Long {
        val account = bankAccountRepository.save(
            BankAccount(
                name = name,
                accountType = BankAccountType.CHECKING,
                currency = "GTQ",
            ),
        )
        return account.id!!
    }

    protected fun guatemalaToday(): LocalDate = LocalDate.now(GUATEMALA_ZONE)

    companion object {
        private val GUATEMALA_ZONE: ZoneId = ZoneId.of("America/Guatemala")
    }
}
