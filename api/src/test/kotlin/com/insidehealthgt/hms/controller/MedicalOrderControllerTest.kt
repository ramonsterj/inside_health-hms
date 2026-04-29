package com.insidehealthgt.hms.controller

import com.insidehealthgt.hms.dto.request.CreateMedicalOrderRequest
import com.insidehealthgt.hms.dto.request.UpdateMedicalOrderRequest
import com.insidehealthgt.hms.entity.AdministrationRoute
import com.insidehealthgt.hms.entity.InventoryItem
import com.insidehealthgt.hms.entity.MedicalOrderCategory
import com.insidehealthgt.hms.entity.User
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.hasItem
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.LocalDate

@Suppress("LargeClass")
class MedicalOrderControllerTest : AbstractIntegrationTest() {

    private lateinit var adminToken: String
    private lateinit var doctorToken: String
    private lateinit var nurseToken: String
    private lateinit var doctorUser: User
    private var admissionId: Long = 0

    @BeforeEach
    fun setUp() {
        val (_, adminTkn) = createAdminUser()
        adminToken = adminTkn

        val (docUsr, docTkn) = createDoctorUser()
        doctorUser = docUsr
        doctorToken = docTkn

        val (_, nurseTkn) = createNurseUser()
        nurseToken = nurseTkn

        // Create admission for tests
        val patientId = createPatient(adminToken)
        admissionId = createAdmission(adminToken, patientId, doctorUser.id!!)
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
            .andExpect(jsonPath("$.data.status").value("SOLICITADO"))
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
        val orderId = createMedicalOrderAndGetId(MedicalOrderCategory.DIETA, null)

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/medical-orders/$orderId/discontinue")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("DESCONTINUADO"))
            .andExpect(jsonPath("$.data.discontinuedAt").exists())
            .andExpect(jsonPath("$.data.discontinuedBy.firstName").value("Dr. Maria"))
    }

    @Test
    fun `cannot discontinue already discontinued order`() {
        val orderId = createMedicalOrderAndGetId(MedicalOrderCategory.DIETA, null)

        // First discontinue (DIETA is directive, starts in ACTIVA which is discontinuable)
        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/medical-orders/$orderId/discontinue")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.status").value("DESCONTINUADO"))

        // Second discontinue should fail
        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/medical-orders/$orderId/discontinue")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isBadRequest)
            .andExpect(
                jsonPath("$.error.message")
                    .value(containsString("DESCONTINUADO")),
            )
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

    // ============ UNAUTHENTICATED / NON-EXISTENT ADMISSION TESTS ============

    @Test
    fun `list medical orders fails without authentication`() {
        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/medical-orders"),
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `create medical order fails without authentication`() {
        val request = CreateMedicalOrderRequest(
            category = MedicalOrderCategory.MEDICAMENTOS,
            startDate = LocalDate.now(),
            medication = "Test",
        )

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/medical-orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `list medical orders for non-existent admission returns 404`() {
        mockMvc.perform(
            get("/api/v1/admissions/99999/medical-orders")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `create medical order for non-existent admission returns 404`() {
        val request = CreateMedicalOrderRequest(
            category = MedicalOrderCategory.MEDICAMENTOS,
            startDate = LocalDate.now(),
            medication = "Test",
        )

        mockMvc.perform(
            post("/api/v1/admissions/99999/medical-orders")
                .header("Authorization", "Bearer $doctorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isNotFound)
    }

    // ============ UPDATE DISCONTINUED ORDER TEST ============

    @Test
    fun `update discontinued medical order returns 400`() {
        val orderId = createMedicalOrderAndGetId(MedicalOrderCategory.DIETA, null)

        // Discontinue the order first (DIETA starts in ACTIVA, can be discontinued)
        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/medical-orders/$orderId/discontinue")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isOk)

        // Try to update the discontinued order
        val updateRequest = UpdateMedicalOrderRequest(
            category = MedicalOrderCategory.DIETA,
            startDate = LocalDate.now(),
            observations = "Should fail",
        )

        mockMvc.perform(
            put("/api/v1/admissions/$admissionId/medical-orders/$orderId")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)),
        )
            .andExpect(status().isBadRequest)
            .andExpect(
                jsonPath("$.error.message")
                    .value("Cannot update a medical order in a terminal state (DESCONTINUADO)"),
            )
    }

    // ============ AUDIT TESTS ============

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

    // ============ STATE TRANSITION TESTS ============

    @Test
    fun `newly created medical order is in SOLICITADO`() {
        val orderId = createMedicalOrderAndGetId(MedicalOrderCategory.LABORATORIOS, "Hemograma")

        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/medical-orders/$orderId")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.status").value("SOLICITADO"))
            .andExpect(jsonPath("$.data.authorizedAt").doesNotExist())
            .andExpect(jsonPath("$.data.authorizedBy").doesNotExist())
    }

    @Test
    fun `admin staff can authorize a SOLICITADO order`() {
        val (_, staffToken) = createAdminStaffUser()
        val orderId = createMedicalOrderAndGetId(MedicalOrderCategory.LABORATORIOS, "Glicemia")

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/medical-orders/$orderId/authorize")
                .header("Authorization", "Bearer $staffToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.status").value("AUTORIZADO"))
            .andExpect(jsonPath("$.data.authorizedAt").exists())
            .andExpect(jsonPath("$.data.authorizedBy.firstName").exists())
    }

    @Test
    fun `admin can authorize a SOLICITADO order`() {
        val orderId = createMedicalOrderAndGetId(MedicalOrderCategory.MEDICAMENTOS, "Lorazepam")

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/medical-orders/$orderId/authorize")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.status").value("AUTORIZADO"))
    }

    @Test
    fun `doctor cannot authorize an order`() {
        val orderId = createMedicalOrderAndGetId(MedicalOrderCategory.MEDICAMENTOS, "Lorazepam")

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/medical-orders/$orderId/authorize")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `nurse cannot authorize an order`() {
        val orderId = createMedicalOrderAndGetId(MedicalOrderCategory.MEDICAMENTOS, "Lorazepam")

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/medical-orders/$orderId/authorize")
                .header("Authorization", "Bearer $nurseToken"),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `cannot authorize an order that is not SOLICITADO`() {
        val (_, staffToken) = createAdminStaffUser()
        val orderId = createMedicalOrderAndGetId(MedicalOrderCategory.LABORATORIOS, "Glicemia")

        // First authorize - succeeds
        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/medical-orders/$orderId/authorize")
                .header("Authorization", "Bearer $staffToken"),
        )
            .andExpect(status().isOk)

        // Second authorize - blocked
        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/medical-orders/$orderId/authorize")
                .header("Authorization", "Bearer $staffToken"),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.message").value(containsString("SOLICITADO")))
    }

    @Test
    fun `admin staff can reject a SOLICITADO order with reason`() {
        val (staffUser, staffToken) = createAdminStaffUser()
        val orderId = createMedicalOrderAndGetId(MedicalOrderCategory.LABORATORIOS, "Test")

        val rejectBody = """{"reason":"Pendiente de cobertura del seguro"}"""

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/medical-orders/$orderId/reject")
                .header("Authorization", "Bearer $staffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(rejectBody),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.status").value("NO_AUTORIZADO"))
            .andExpect(jsonPath("$.data.rejectionReason").value("Pendiente de cobertura del seguro"))
            // Rejection populates dedicated audit fields (issue #56) — not authorized_*.
            .andExpect(jsonPath("$.data.rejectedAt").exists())
            .andExpect(jsonPath("$.data.rejectedBy.id").value(staffUser.id))
            .andExpect(jsonPath("$.data.authorizedAt").doesNotExist())
            .andExpect(jsonPath("$.data.authorizedBy").doesNotExist())
    }

    @Test
    fun `reject with no body is allowed`() {
        val (staffUser, staffToken) = createAdminStaffUser()
        val orderId = createMedicalOrderAndGetId(MedicalOrderCategory.LABORATORIOS, "Test")

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/medical-orders/$orderId/reject")
                .header("Authorization", "Bearer $staffToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.status").value("NO_AUTORIZADO"))
            .andExpect(jsonPath("$.data.rejectedAt").exists())
            .andExpect(jsonPath("$.data.rejectedBy.id").value(staffUser.id))
            .andExpect(jsonPath("$.data.authorizedAt").doesNotExist())
            .andExpect(jsonPath("$.data.authorizedBy").doesNotExist())
    }

    @Test
    fun `directive order is created in ACTIVA`() {
        val orderId = createMedicalOrderAndGetId(MedicalOrderCategory.DIETA, null)

        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/medical-orders/$orderId")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.status").value("ACTIVA"))
    }

    @Test
    fun `cannot authorize a directive order`() {
        val (_, staffToken) = createAdminStaffUser()
        val orderId = createMedicalOrderAndGetId(MedicalOrderCategory.DIETA, null)

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/medical-orders/$orderId/authorize")
                .header("Authorization", "Bearer $staffToken"),
        )
            .andExpect(status().isBadRequest)
            .andExpect(
                jsonPath("$.error.message")
                    .value(containsString("does not require authorization")),
            )
    }

    @Test
    fun `nurse can mark authorized lab order EN_PROCESO`() {
        val (_, staffToken) = createAdminStaffUser()
        val orderId = createMedicalOrderAndGetId(MedicalOrderCategory.LABORATORIOS, "Hemograma")

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/medical-orders/$orderId/authorize")
                .header("Authorization", "Bearer $staffToken"),
        ).andExpect(status().isOk)

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/medical-orders/$orderId/mark-in-progress")
                .header("Authorization", "Bearer $nurseToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.status").value("EN_PROCESO"))
            .andExpect(jsonPath("$.data.inProgressAt").exists())
            .andExpect(jsonPath("$.data.inProgressBy.firstName").exists())
    }

    @Test
    fun `cannot mark in progress for non-results category`() {
        val (_, staffToken) = createAdminStaffUser()
        val orderId = createMedicalOrderAndGetId(MedicalOrderCategory.MEDICAMENTOS, "Lorazepam")

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/medical-orders/$orderId/authorize")
                .header("Authorization", "Bearer $staffToken"),
        ).andExpect(status().isOk)

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/medical-orders/$orderId/mark-in-progress")
                .header("Authorization", "Bearer $nurseToken"),
        )
            .andExpect(status().isBadRequest)
            .andExpect(
                jsonPath("$.error.message")
                    .value(containsString("in-progress phase")),
            )
    }

    @Test
    fun `cannot mark in progress when order is not AUTORIZADO`() {
        val orderId = createMedicalOrderAndGetId(MedicalOrderCategory.LABORATORIOS, "Test")

        // Order is in SOLICITADO
        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/medical-orders/$orderId/mark-in-progress")
                .header("Authorization", "Bearer $nurseToken"),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.message").value(containsString("AUTORIZADO")))
    }

    @Test
    fun `discontinue blocked from EN_PROCESO`() {
        val (_, staffToken) = createAdminStaffUser()
        val orderId = createMedicalOrderAndGetId(MedicalOrderCategory.LABORATORIOS, "Test")

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/medical-orders/$orderId/authorize")
                .header("Authorization", "Bearer $staffToken"),
        ).andExpect(status().isOk)

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/medical-orders/$orderId/mark-in-progress")
                .header("Authorization", "Bearer $nurseToken"),
        ).andExpect(status().isOk)

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/medical-orders/$orderId/discontinue")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.message").value(containsString("EN_PROCESO")))
    }

    @Test
    fun `discontinue blocked from terminal NO_AUTORIZADO state`() {
        val (_, staffToken) = createAdminStaffUser()
        val orderId = createMedicalOrderAndGetId(MedicalOrderCategory.LABORATORIOS, "Test")

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/medical-orders/$orderId/reject")
                .header("Authorization", "Bearer $staffToken"),
        ).andExpect(status().isOk)

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/medical-orders/$orderId/discontinue")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.message").value(containsString("NO_AUTORIZADO")))
    }

    @Test
    fun `doctor can emergency-authorize a SOLICITADO order with reason`() {
        val orderId = createMedicalOrderAndGetId(MedicalOrderCategory.MEDICAMENTOS, "Stat sedative")

        val body = """{"reason":"PATIENT_IN_CRISIS"}"""

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/medical-orders/$orderId/emergency-authorize")
                .header("Authorization", "Bearer $doctorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.status").value("AUTORIZADO"))
            .andExpect(jsonPath("$.data.emergencyAuthorized").value(true))
            .andExpect(jsonPath("$.data.emergencyReason").value("PATIENT_IN_CRISIS"))
            .andExpect(jsonPath("$.data.emergencyAt").exists())
            .andExpect(jsonPath("$.data.emergencyBy.firstName").exists())
            .andExpect(jsonPath("$.data.authorizedAt").exists())
    }

    @Test
    fun `emergency-authorize with reason OTHER requires reasonNote`() {
        val orderId = createMedicalOrderAndGetId(MedicalOrderCategory.MEDICAMENTOS, "Stat med")

        val body = """{"reason":"OTHER"}"""

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/medical-orders/$orderId/emergency-authorize")
                .header("Authorization", "Bearer $doctorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.message").value(containsString("reasonNote")))
    }

    @Test
    fun `emergency-authorize requires reason field`() {
        val orderId = createMedicalOrderAndGetId(MedicalOrderCategory.MEDICAMENTOS, "Stat med")

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/medical-orders/$orderId/emergency-authorize")
                .header("Authorization", "Bearer $doctorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `nurse cannot emergency-authorize`() {
        val orderId = createMedicalOrderAndGetId(MedicalOrderCategory.MEDICAMENTOS, "Stat med")

        val body = """{"reason":"AFTER_HOURS_NO_ADMIN"}"""

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/medical-orders/$orderId/emergency-authorize")
                .header("Authorization", "Bearer $nurseToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `cannot emergency-authorize a directive order`() {
        val orderId = createMedicalOrderAndGetId(MedicalOrderCategory.DIETA, null)

        val body = """{"reason":"PATIENT_IN_CRISIS"}"""

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/medical-orders/$orderId/emergency-authorize")
                .header("Authorization", "Bearer $doctorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body),
        )
            .andExpect(status().isBadRequest)
    }

    // ============ CROSS-ADMISSION DASHBOARD TESTS ============

    @Test
    fun `cross-admission listing returns orders filtered by status`() {
        val (_, staffToken) = createAdminStaffUser()
        val solicitadoId = createMedicalOrderAndGetId(MedicalOrderCategory.LABORATORIOS, "Lab requested")
        val authorizedId = createMedicalOrderAndGetId(MedicalOrderCategory.LABORATORIOS, "Lab authorized")
        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/medical-orders/$authorizedId/authorize")
                .header("Authorization", "Bearer $staffToken"),
        ).andExpect(status().isOk)

        mockMvc.perform(
            get("/api/v1/medical-orders")
                .param("status", "SOLICITADO")
                .header("Authorization", "Bearer $staffToken"),
        )
            .andExpect(status().isOk)
            .andExpect(
                jsonPath(
                    "$.data.content[?(@.id == $solicitadoId)].status",
                    hasItem("SOLICITADO"),
                ),
            )
            .andExpect(
                jsonPath(
                    "$.data.content[?(@.id == $authorizedId)]",
                    hasSize<Any>(0),
                ),
            )
    }

    @Test
    fun `cross-admission listing requires medical-order read permission`() {
        val (_, psychTkn) = createPsychologistUser()

        mockMvc.perform(
            get("/api/v1/medical-orders")
                .header("Authorization", "Bearer $psychTkn"),
        )
            .andExpect(status().isForbidden)
    }

    // ============ AUTHORIZATION-TIME BILLING TESTS ============

    @Test
    fun `authorize on MEDICAMENTOS with inventory item does not create a charge`() {
        // MEDICAMENTOS bills per-administration via InventoryDispensedEvent. An
        // authorization-time charge would double-bill against that flow.
        val item = saveTestInventoryItem("Lorazepam 1mg")
        val orderId = createMedicalOrderWithItemAndGetId(MedicalOrderCategory.MEDICAMENTOS, item.id!!)

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/medical-orders/$orderId/authorize")
                .header("Authorization", "Bearer $adminToken"),
        ).andExpect(status().isOk)

        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/charges")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data.length()").value(0))
    }

    @Test
    fun `authorize on LABORATORIOS with inventory item creates a LAB charge`() {
        val item = saveTestInventoryItem("Hemograma")
        val orderId = createMedicalOrderWithItemAndGetId(MedicalOrderCategory.LABORATORIOS, item.id!!)

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/medical-orders/$orderId/authorize")
                .header("Authorization", "Bearer $adminToken"),
        ).andExpect(status().isOk)

        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/charges")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].chargeType").value("LAB"))
            .andExpect(jsonPath("$.data[0].description").value("Hemograma"))
    }

    private fun saveTestInventoryItem(name: String): InventoryItem {
        val category = inventoryCategoryRepository.findAll().first()
        return inventoryItemRepository.save(
            InventoryItem(
                category = category,
                name = name,
                price = BigDecimal("50.00"),
                cost = BigDecimal("20.00"),
                quantity = 100,
                restockLevel = 10,
            ),
        )
    }

    private fun createMedicalOrderWithItemAndGetId(category: MedicalOrderCategory, inventoryItemId: Long): Long {
        val request = CreateMedicalOrderRequest(
            category = category,
            startDate = LocalDate.now(),
            inventoryItemId = inventoryItemId,
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
        ).andExpect(status().isCreated)
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
