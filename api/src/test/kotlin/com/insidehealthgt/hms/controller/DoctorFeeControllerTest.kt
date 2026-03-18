package com.insidehealthgt.hms.controller

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
import java.math.BigDecimal
import java.time.LocalDate

class DoctorFeeControllerTest : AbstractIntegrationTest() {

    private lateinit var adminToken: String
    private lateinit var doctorToken: String
    private var bankAccountId: Long = 0

    @BeforeEach
    fun setUp() {
        val (_, token) = createAdminUser()
        adminToken = token
        val (_, dToken) = createDoctorUser()
        doctorToken = dToken
        bankAccountId = createBankAccount("Test Bank")
    }

    // ============ CREATE FEE TESTS ============

    @Test
    fun `create returns 201 with correct fee data`() {
        val employeeId = createDoctorEmployee(commissionPct = BigDecimal("15.00"))

        mockMvc.perform(
            post("/api/v1/treasury/employees/$employeeId/doctor-fees")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "billingType" to "HOSPITAL_BILLED",
                            "grossAmount" to BigDecimal("1000.00"),
                            "feeDate" to LocalDate.now().toString(),
                        ),
                    ),
                ),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.grossAmount").value(1000.00))
            .andExpect(jsonPath("$.data.commissionPct").value(15.00))
            .andExpect(jsonPath("$.data.netAmount").value(850.00))
            .andExpect(jsonPath("$.data.status").value("PENDING"))
    }

    @Test
    fun `create uses employee default commission when not provided`() {
        val employeeId = createDoctorEmployee(commissionPct = BigDecimal("20.00"))

        val result = mockMvc.perform(
            post("/api/v1/treasury/employees/$employeeId/doctor-fees")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "billingType" to "HOSPITAL_BILLED",
                            "grossAmount" to BigDecimal("500.00"),
                            "feeDate" to LocalDate.now().toString(),
                        ),
                    ),
                ),
        ).andExpect(status().isCreated)
            .andReturn()

        val commissionPct = objectMapper.readTree(result.response.contentAsString)
            .get("data").get("commissionPct").decimalValue()
        assert(commissionPct.compareTo(BigDecimal("20.00")) == 0) {
            "Expected commissionPct 20.00 but was $commissionPct"
        }
    }

    @Test
    fun `create with custom commission overrides employee default`() {
        val employeeId = createDoctorEmployee(commissionPct = BigDecimal("15.00"))

        mockMvc.perform(
            post("/api/v1/treasury/employees/$employeeId/doctor-fees")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "billingType" to "HOSPITAL_BILLED",
                            "grossAmount" to BigDecimal("1000.00"),
                            "commissionPct" to BigDecimal("20.00"),
                            "feeDate" to LocalDate.now().toString(),
                        ),
                    ),
                ),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.commissionPct").value(20.00))
            .andExpect(jsonPath("$.data.netAmount").value(800.00))
    }

    @Test
    fun `create EXTERNAL billing type returns 201`() {
        val employeeId = createDoctorEmployee()

        mockMvc.perform(
            post("/api/v1/treasury/employees/$employeeId/doctor-fees")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "billingType" to "EXTERNAL",
                            "grossAmount" to BigDecimal("750.00"),
                            "feeDate" to LocalDate.now().toString(),
                        ),
                    ),
                ),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.billingType").value("EXTERNAL"))
    }

    @Test
    fun `create returns 400 for non-DOCTOR employee`() {
        val payrollEmployeeId = createPayrollEmployee()

        mockMvc.perform(
            post("/api/v1/treasury/employees/$payrollEmployeeId/doctor-fees")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "billingType" to "HOSPITAL_BILLED",
                            "grossAmount" to BigDecimal("1000.00"),
                            "feeDate" to LocalDate.now().toString(),
                        ),
                    ),
                ),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `create returns 403 for doctor role`() {
        val employeeId = createDoctorEmployee()

        mockMvc.perform(
            post("/api/v1/treasury/employees/$employeeId/doctor-fees")
                .header("Authorization", "Bearer $doctorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "billingType" to "HOSPITAL_BILLED",
                            "grossAmount" to BigDecimal("1000.00"),
                            "feeDate" to LocalDate.now().toString(),
                        ),
                    ),
                ),
        )
            .andExpect(status().isForbidden)
    }

    // ============ LIST FEES TESTS ============

    @Test
    fun `list returns all fees for employee`() {
        val employeeId = createDoctorEmployee()
        createDoctorFee(employeeId, BigDecimal("500.00"))
        createDoctorFee(employeeId, BigDecimal("800.00"))

        mockMvc.perform(
            get("/api/v1/treasury/employees/$employeeId/doctor-fees")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data.length()").value(2))
    }

    @Test
    fun `list filters by status`() {
        val employeeId = createDoctorEmployee()
        val feeId = createDoctorFee(employeeId, BigDecimal("1000.00"))
        createDoctorFee(employeeId, BigDecimal("500.00"))
        submitInvoice(employeeId, feeId)

        mockMvc.perform(
            get("/api/v1/treasury/employees/$employeeId/doctor-fees")
                .param("status", "PENDING")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].status").value("PENDING"))
    }

    @Test
    fun `list returns empty array for employee with no fees`() {
        val employeeId = createDoctorEmployee()

        mockMvc.perform(
            get("/api/v1/treasury/employees/$employeeId/doctor-fees")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data.length()").value(0))
    }

    // ============ GET FEE TESTS ============

    @Test
    fun `get returns fee by id`() {
        val employeeId = createDoctorEmployee()
        val feeId = createDoctorFee(employeeId, BigDecimal("1000.00"))

        mockMvc.perform(
            get("/api/v1/treasury/employees/$employeeId/doctor-fees/$feeId")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(feeId))
            .andExpect(jsonPath("$.data.grossAmount").value(1000.00))
    }

    @Test
    fun `get returns 404 for non-existent fee`() {
        val employeeId = createDoctorEmployee()

        mockMvc.perform(
            get("/api/v1/treasury/employees/$employeeId/doctor-fees/99999")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isNotFound)
    }

    // ============ UPDATE STATUS TESTS ============

    @Test
    fun `update status to INVOICED with invoice number`() {
        val employeeId = createDoctorEmployee()
        val feeId = createDoctorFee(employeeId)

        mockMvc.perform(
            put("/api/v1/treasury/employees/$employeeId/doctor-fees/$feeId/status")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf("status" to "INVOICED", "doctorInvoiceNumber" to "INV-TEST-001"),
                    ),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.status").value("INVOICED"))
            .andExpect(jsonPath("$.data.doctorInvoiceNumber").value("INV-TEST-001"))
    }

    @Test
    fun `update status returns 400 for PAID transition`() {
        val employeeId = createDoctorEmployee()
        val feeId = createDoctorFee(employeeId)

        mockMvc.perform(
            put("/api/v1/treasury/employees/$employeeId/doctor-fees/$feeId/status")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf("status" to "PAID", "doctorInvoiceNumber" to "INV-001"),
                    ),
                ),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `update status returns 400 when not in PENDING state`() {
        val employeeId = createDoctorEmployee()
        val feeId = createDoctorFee(employeeId)
        submitInvoice(employeeId, feeId, "INV-001")

        mockMvc.perform(
            put("/api/v1/treasury/employees/$employeeId/doctor-fees/$feeId/status")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf("status" to "INVOICED", "doctorInvoiceNumber" to "INV-002"),
                    ),
                ),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `update status returns 400 when invoice number is missing`() {
        val employeeId = createDoctorEmployee()
        val feeId = createDoctorFee(employeeId)

        mockMvc.perform(
            put("/api/v1/treasury/employees/$employeeId/doctor-fees/$feeId/status")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf("status" to "INVOICED"),
                    ),
                ),
        )
            .andExpect(status().isBadRequest)
    }

    // ============ UPLOAD DOCUMENT TESTS ============

    @Test
    fun `upload invoice document returns 200 with updated path`() {
        val employeeId = createDoctorEmployee()
        val feeId = createDoctorFee(employeeId)

        val file = MockMultipartFile("file", "invoice.pdf", "application/pdf", "pdf-content".toByteArray())

        mockMvc.perform(
            multipart("/api/v1/treasury/employees/$employeeId/doctor-fees/$feeId/invoice-document")
                .file(file)
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.invoiceDocumentPath").isNotEmpty)
    }

    // ============ SETTLE FEE TESTS ============

    @Test
    fun `settle transitions to PAID and creates expense`() {
        val employeeId = createDoctorEmployee()
        val feeId = createDoctorFee(employeeId)

        submitInvoice(employeeId, feeId, "INV-SETTLE-001")
        uploadInvoiceDocument(employeeId, feeId)

        mockMvc.perform(
            post("/api/v1/treasury/employees/$employeeId/doctor-fees/$feeId/settle")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "bankAccountId" to bankAccountId,
                            "paymentDate" to LocalDate.now().toString(),
                        ),
                    ),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.status").value("PAID"))
            .andExpect(jsonPath("$.data.expenseId").isNotEmpty)
    }

    @Test
    fun `settle returns 400 when fee is not INVOICED`() {
        val employeeId = createDoctorEmployee()
        val feeId = createDoctorFee(employeeId)

        mockMvc.perform(
            post("/api/v1/treasury/employees/$employeeId/doctor-fees/$feeId/settle")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "bankAccountId" to bankAccountId,
                            "paymentDate" to LocalDate.now().toString(),
                        ),
                    ),
                ),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `settle returns 400 when invoice document is missing`() {
        val employeeId = createDoctorEmployee()
        val feeId = createDoctorFee(employeeId)

        submitInvoice(employeeId, feeId, "INV-NO-DOC-001")
        // Intentionally skip uploadInvoiceDocument

        mockMvc.perform(
            post("/api/v1/treasury/employees/$employeeId/doctor-fees/$feeId/settle")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "bankAccountId" to bankAccountId,
                            "paymentDate" to LocalDate.now().toString(),
                        ),
                    ),
                ),
        )
            .andExpect(status().isBadRequest)
    }

    // ============ DELETE FEE TESTS ============

    @Test
    fun `delete returns 200 for PENDING fee`() {
        val employeeId = createDoctorEmployee()
        val feeId = createDoctorFee(employeeId)

        mockMvc.perform(
            delete("/api/v1/treasury/employees/$employeeId/doctor-fees/$feeId")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
    }

    @Test
    fun `delete returns 400 for INVOICED fee`() {
        val employeeId = createDoctorEmployee()
        val feeId = createDoctorFee(employeeId)
        submitInvoice(employeeId, feeId, "INV-DELETE-001")

        mockMvc.perform(
            delete("/api/v1/treasury/employees/$employeeId/doctor-fees/$feeId")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `delete returns 403 for doctor role`() {
        val employeeId = createDoctorEmployee()
        val feeId = createDoctorFee(employeeId)

        mockMvc.perform(
            delete("/api/v1/treasury/employees/$employeeId/doctor-fees/$feeId")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isForbidden)
    }

    // ============ SUMMARY TESTS ============

    @Test
    fun `summary returns correct aggregated data`() {
        val employeeId = createDoctorEmployee(commissionPct = BigDecimal("15.00"))
        createDoctorFee(employeeId, BigDecimal("1000.00"))
        createDoctorFee(employeeId, BigDecimal("500.00"))

        mockMvc.perform(
            get("/api/v1/treasury/employees/$employeeId/doctor-fees/summary")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.totalFees").value(2))
            .andExpect(jsonPath("$.data.pendingCount").value(2))
            .andExpect(jsonPath("$.data.employeeId").value(employeeId))
    }

    // ============ HELPERS ============

    private fun createDoctorEmployee(
        fullName: String = "Dr. Test",
        commissionPct: BigDecimal = BigDecimal("15.00"),
    ): Long {
        val request = mapOf(
            "fullName" to fullName,
            "employeeType" to "DOCTOR",
            "hospitalCommissionPct" to commissionPct,
            "doctorFeeArrangement" to "HOSPITAL_BILLED",
        )
        val result = mockMvc.perform(
            post("/api/v1/treasury/employees")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andExpect(status().isCreated)
            .andReturn()
        return objectMapper.readTree(result.response.contentAsString).get("data").get("id").asLong()
    }

    private fun createPayrollEmployee(fullName: String = "Payroll Employee"): Long {
        val request = mapOf(
            "fullName" to fullName,
            "employeeType" to "PAYROLL",
            "baseSalary" to 5000.00,
            "hireDate" to "2024-01-01",
        )
        val result = mockMvc.perform(
            post("/api/v1/treasury/employees")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andExpect(status().isCreated)
            .andReturn()
        return objectMapper.readTree(result.response.contentAsString).get("data").get("id").asLong()
    }

    private fun createDoctorFee(
        employeeId: Long,
        grossAmount: BigDecimal = BigDecimal("1000.00"),
        billingType: String = "HOSPITAL_BILLED",
        patientChargeId: Long? = null,
    ): Long {
        val request = mutableMapOf<String, Any>(
            "billingType" to billingType,
            "grossAmount" to grossAmount,
            "feeDate" to LocalDate.now().toString(),
        )
        patientChargeId?.let { request["patientChargeId"] = it }
        val result = mockMvc.perform(
            post("/api/v1/treasury/employees/$employeeId/doctor-fees")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andExpect(status().isCreated)
            .andReturn()
        return objectMapper.readTree(result.response.contentAsString).get("data").get("id").asLong()
    }

    private fun submitInvoice(employeeId: Long, feeId: Long, invoiceNumber: String = "INV-001") {
        val request = mapOf("status" to "INVOICED", "doctorInvoiceNumber" to invoiceNumber)
        mockMvc.perform(
            put("/api/v1/treasury/employees/$employeeId/doctor-fees/$feeId/status")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andExpect(status().isOk)
    }

    private fun uploadInvoiceDocument(employeeId: Long, feeId: Long) {
        val file = MockMultipartFile("file", "invoice.pdf", "application/pdf", "pdf-content".toByteArray())
        mockMvc.perform(
            multipart("/api/v1/treasury/employees/$employeeId/doctor-fees/$feeId/invoice-document")
                .file(file)
                .header("Authorization", "Bearer $adminToken"),
        ).andExpect(status().isOk)
    }
}
