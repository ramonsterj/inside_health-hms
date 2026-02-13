package com.insidehealthgt.hms.controller

import com.insidehealthgt.hms.entity.AdmissionType
import com.insidehealthgt.hms.entity.InventoryItem
import com.insidehealthgt.hms.entity.Room
import com.insidehealthgt.hms.entity.RoomGender
import com.insidehealthgt.hms.entity.RoomType
import com.insidehealthgt.hms.entity.User
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal

class BillingControllerTest : AbstractIntegrationTest() {

    private lateinit var adminToken: String
    private lateinit var adminUser: User
    private lateinit var doctorUser: User
    private lateinit var doctorToken: String
    private lateinit var nurseToken: String
    private var admissionId: Long = 0
    private var roomId: Long = 0

    @BeforeEach
    fun setUp() {
        val (admin, adminTkn) = createAdminUser()
        adminUser = admin
        adminToken = adminTkn

        val (doctor, docTkn) = createDoctorUser()
        doctorUser = doctor
        doctorToken = docTkn

        val (_, nurseTkn) = createNurseUser()
        nurseToken = nurseTkn

        // Create room with price
        val room = roomRepository.save(
            Room(
                number = "101",
                type = RoomType.PRIVATE,
                gender = RoomGender.MALE,
                capacity = 2,
                price = BigDecimal("500.00"),
            ),
        )
        roomId = room.id!!

        // Create patient and admission with room and triage code
        val triageCodeId = triageCodeRepository.findAll().first().id!!
        val patientId = createPatient(adminToken)
        admissionId = createAdmission(
            token = adminToken,
            patientId = patientId,
            doctorId = doctorUser.id!!,
            type = AdmissionType.HOSPITALIZATION,
            roomId = roomId,
            triageCodeId = triageCodeId,
        )
    }

    // ============ MANUAL CHARGE TESTS ============

    @Test
    fun `create manual charge should return 201`() {
        val request = mapOf(
            "chargeType" to "SERVICE",
            "description" to "Physical therapy session",
            "quantity" to 1,
            "unitPrice" to 150.00,
        )

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/charges")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.chargeType").value("SERVICE"))
            .andExpect(jsonPath("$.data.totalAmount").value(150.00))
            .andExpect(jsonPath("$.data.invoiced").value(false))
    }

    @Test
    fun `create charge with missing fields should return 400`() {
        val request = mapOf(
            "chargeType" to "SERVICE",
        )

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/charges")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `create charge on discharged admission should return 400`() {
        dischargeAdmission(admissionId, adminToken)

        val request = mapOf(
            "chargeType" to "SERVICE",
            "description" to "Late charge",
            "quantity" to 1,
            "unitPrice" to 50.00,
        )

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/charges")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `create charge with ROOM type should return 400`() {
        val request = mapOf(
            "chargeType" to "ROOM",
            "description" to "Room charge",
            "quantity" to 1,
            "unitPrice" to 500.00,
        )

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/charges")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)
    }

    // ============ ADJUSTMENT TESTS ============

    @Test
    fun `create adjustment should return 201`() {
        val request = mapOf(
            "description" to "Billing correction",
            "amount" to -75.00,
            "reason" to "Duplicate charge for medication",
        )

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/adjustments")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.chargeType").value("ADJUSTMENT"))
            .andExpect(jsonPath("$.data.unitPrice").value(-75.00))
            .andExpect(jsonPath("$.data.totalAmount").value(-75.00))
    }

    @Test
    fun `create adjustment without reason should return 400`() {
        val request = mapOf(
            "description" to "Billing correction",
            "amount" to -75.00,
            "reason" to "",
        )

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/adjustments")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `create adjustment with positive amount should return 400`() {
        val request = mapOf(
            "description" to "Billing correction",
            "amount" to 75.00,
            "reason" to "Some reason",
        )

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/adjustments")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)
    }

    // ============ BALANCE TESTS ============

    @Test
    fun `get balance with no charges should return zero total`() {
        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/balance")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.totalBalance").value(0))
            .andExpect(jsonPath("$.data.dailyBreakdown").isEmpty)
    }

    @Test
    fun `get balance with charges should return correct grouping`() {
        // Create two charges
        createCharge("SERVICE", "Therapy session", 1, BigDecimal("200.00"))
        createCharge("MEDICATION", "Pain medication", 2, BigDecimal("25.00"))

        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/balance")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.totalBalance").value(250.00))
            .andExpect(jsonPath("$.data.dailyBreakdown").isNotEmpty)
    }

    @Test
    fun `get balance with adjustments should reflect deductions`() {
        createCharge("SERVICE", "Therapy session", 1, BigDecimal("200.00"))
        createAdjustment("Correction", BigDecimal("-50.00"), "Duplicate charge")

        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/balance")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.totalBalance").value(150.00))
    }

    // ============ INVOICE TESTS ============

    @Test
    fun `discharge should auto-generate invoice`() {
        createCharge("SERVICE", "Therapy", 1, BigDecimal("100.00"))
        dischargeAdmission(admissionId, adminToken)

        // Invoice is auto-generated on discharge via event listener
        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/invoice")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.invoiceNumber").exists())
    }

    @Test
    fun `generate invoice for active admission should return 400`() {
        createCharge("SERVICE", "Therapy", 1, BigDecimal("100.00"))

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/invoice")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `generate duplicate invoice after auto-invoice should return 409`() {
        createCharge("SERVICE", "Therapy", 1, BigDecimal("100.00"))
        dischargeAdmission(admissionId, adminToken)

        // Auto-invoice was already generated on discharge, manual attempt should be 409
        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/invoice")
                .header("Authorization", "Bearer $adminToken"),
        ).andExpect(status().isConflict)
    }

    @Test
    fun `generate invoice with no charges should auto-generate empty invoice on discharge`() {
        dischargeAdmission(admissionId, adminToken)

        // Auto-invoice was generated on discharge even with no charges
        // Manual attempt should return 409 Conflict
        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/invoice")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isConflict)
    }

    // ============ INVENTORY INTEGRATION TEST ============

    @Test
    fun `inventory EXIT with admissionId should auto-create charge`() {
        val category = inventoryCategoryRepository.findAll().first()
        val item = inventoryItemRepository.save(
            InventoryItem(
                category = category,
                name = "Amoxicillin 500mg",
                price = BigDecimal("25.00"),
                cost = BigDecimal("10.00"),
                quantity = 100,
                restockLevel = 10,
            ),
        )

        val movementRequest = mapOf(
            "type" to "EXIT",
            "quantity" to 3,
            "notes" to "Dispensed to patient",
            "admissionId" to admissionId,
        )

        mockMvc.perform(
            post("/api/v1/admin/inventory-items/${item.id}/movements")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(movementRequest)),
        ).andExpect(status().isCreated)

        // Verify charge was auto-created
        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/charges")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].chargeType").value("MEDICATION"))
            .andExpect(jsonPath("$.data[0].description").value("Amoxicillin 500mg"))
            .andExpect(jsonPath("$.data[0].quantity").value(3))
    }

    // ============ PERMISSION TESTS ============

    @Test
    fun `doctor can read charges`() {
        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/charges")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `nurse can read charges`() {
        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/charges")
                .header("Authorization", "Bearer $nurseToken"),
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `doctor cannot create charges`() {
        val request = mapOf(
            "chargeType" to "SERVICE",
            "description" to "Test",
            "quantity" to 1,
            "unitPrice" to 50.00,
        )

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/charges")
                .header("Authorization", "Bearer $doctorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `nurse cannot create adjustments`() {
        val request = mapOf(
            "description" to "Correction",
            "amount" to -50.00,
            "reason" to "Error",
        )

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/adjustments")
                .header("Authorization", "Bearer $nurseToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isForbidden)
    }

    // ============ HELPERS ============

    private fun createCharge(chargeType: String, description: String, quantity: Int, unitPrice: BigDecimal) {
        val request = mapOf(
            "chargeType" to chargeType,
            "description" to description,
            "quantity" to quantity,
            "unitPrice" to unitPrice,
        )

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/charges")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andExpect(status().isCreated)
    }

    private fun createAdjustment(description: String, amount: BigDecimal, reason: String) {
        val request = mapOf(
            "description" to description,
            "amount" to amount,
            "reason" to reason,
        )

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/adjustments")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andExpect(status().isCreated)
    }
}
