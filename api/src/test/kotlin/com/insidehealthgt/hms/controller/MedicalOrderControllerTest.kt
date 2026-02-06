package com.insidehealthgt.hms.controller

import com.insidehealthgt.hms.dto.request.CreateMedicalOrderRequest
import com.insidehealthgt.hms.dto.request.UpdateMedicalOrderRequest
import com.insidehealthgt.hms.entity.AdministrationRoute
import com.insidehealthgt.hms.entity.MedicalOrderCategory
import com.insidehealthgt.hms.entity.User
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate

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
        val orderId = createMedicalOrderAndGetId(MedicalOrderCategory.MEDICAMENTOS, "To discontinue")

        // Discontinue the order first
        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/medical-orders/$orderId/discontinue")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isOk)

        // Try to update the discontinued order
        val updateRequest = UpdateMedicalOrderRequest(
            category = MedicalOrderCategory.MEDICAMENTOS,
            startDate = LocalDate.now(),
            medication = "Should fail",
        )

        mockMvc.perform(
            put("/api/v1/admissions/$admissionId/medical-orders/$orderId")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.message").value("Cannot update a discontinued medical order"))
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
