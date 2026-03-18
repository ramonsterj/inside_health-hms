package com.insidehealthgt.hms.controller

import com.insidehealthgt.hms.entity.User
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class TreasuryEmployeeControllerTest : AbstractIntegrationTest() {

    private lateinit var adminToken: String
    private lateinit var adminUser: User
    private lateinit var doctorToken: String
    private var bankAccountId: Long = 0

    @BeforeEach
    fun setUp() {
        val (admin, adminTkn) = createAdminUser()
        adminUser = admin
        adminToken = adminTkn

        val (_, docTkn) = createDoctorUser()
        doctorToken = docTkn

        bankAccountId = createBankAccount()
    }

    // ============ EMPLOYEE CRUD TESTS ============

    @Test
    fun `create payroll employee should return 201`() {
        val request = mapOf(
            "fullName" to "Maria Lopez",
            "employeeType" to "PAYROLL",
            "baseSalary" to 5000.00,
            "position" to "Nurse",
            "hireDate" to "2025-01-15",
        )

        mockMvc.perform(
            post("/api/v1/treasury/employees")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.fullName").value("Maria Lopez"))
            .andExpect(jsonPath("$.data.employeeType").value("PAYROLL"))
            .andExpect(jsonPath("$.data.baseSalary").value(5000.00))
            .andExpect(jsonPath("$.data.active").value(true))
    }

    @Test
    fun `create contractor employee should return 201`() {
        val request = mapOf(
            "fullName" to "Carlos Mendez",
            "employeeType" to "CONTRACTOR",
            "contractedRate" to 3000.00,
            "position" to "IT Support",
        )

        mockMvc.perform(
            post("/api/v1/treasury/employees")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.fullName").value("Carlos Mendez"))
            .andExpect(jsonPath("$.data.employeeType").value("CONTRACTOR"))
            .andExpect(jsonPath("$.data.contractedRate").value(3000.00))
    }

    @Test
    fun `create employee with missing name should return 400`() {
        val request = mapOf(
            "employeeType" to "PAYROLL",
            "baseSalary" to 5000.00,
        )

        mockMvc.perform(
            post("/api/v1/treasury/employees")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `list employees should return results`() {
        createEmployee("PAYROLL", "Employee A")
        createEmployee("CONTRACTOR", "Employee B")

        mockMvc.perform(
            get("/api/v1/treasury/employees")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data.length()").value(2))
    }

    @Test
    fun `list employees with type filter should filter results`() {
        createEmployee("PAYROLL", "Payroll Employee")
        createEmployee("CONTRACTOR", "Contractor Employee")

        mockMvc.perform(
            get("/api/v1/treasury/employees")
                .param("type", "PAYROLL")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].employeeType").value("PAYROLL"))
    }

    @Test
    fun `list employees with activeOnly filter should exclude terminated`() {
        val empId = createEmployee("PAYROLL", "Active Employee")
        createEmployee("CONTRACTOR", "Other Employee")

        // Terminate the first employee
        terminateEmployee(empId)

        mockMvc.perform(
            get("/api/v1/treasury/employees")
                .param("activeOnly", "true")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].fullName").value("Other Employee"))
    }

    @Test
    fun `get employee by id should return 200`() {
        val empId = createEmployee("PAYROLL", "Test Employee")

        mockMvc.perform(
            get("/api/v1/treasury/employees/$empId")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.fullName").value("Test Employee"))
    }

    @Test
    fun `update employee should return 200`() {
        val empId = createEmployee("CONTRACTOR", "Original Name")

        val updateRequest = mapOf(
            "fullName" to "Updated Name",
            "position" to "Senior IT",
            "contractedRate" to 4000.00,
        )

        mockMvc.perform(
            put("/api/v1/treasury/employees/$empId")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.fullName").value("Updated Name"))
            .andExpect(jsonPath("$.data.position").value("Senior IT"))
    }

    // ============ SALARY TESTS ============

    @Test
    fun `update salary should return 200`() {
        val empId = createEmployee("PAYROLL", "Salary Employee")

        val request = mapOf(
            "newSalary" to 6000.00,
            "effectiveFrom" to "2026-04-01",
            "notes" to "Annual raise",
        )

        mockMvc.perform(
            put("/api/v1/treasury/employees/$empId/salary")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.baseSalary").value(6000.00))
    }

    @Test
    fun `get salary history should return list`() {
        val empId = createEmployee("PAYROLL", "Salary History Employee")

        // Update salary to create history
        val request = mapOf(
            "newSalary" to 6000.00,
            "effectiveFrom" to "2026-04-01",
        )
        mockMvc.perform(
            put("/api/v1/treasury/employees/$empId/salary")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andExpect(status().isOk)

        mockMvc.perform(
            get("/api/v1/treasury/employees/$empId/salary-history")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data.length()").isNotEmpty)
    }

    // ============ PAYROLL TESTS ============

    @Test
    fun `generate payroll schedule should return entries`() {
        val empId = createEmployee("PAYROLL", "Payroll Employee")

        val request = mapOf("year" to 2026)

        mockMvc.perform(
            post("/api/v1/treasury/employees/$empId/payroll/generate")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data.length()").value(24))
    }

    @Test
    fun `get payroll should return entries`() {
        val empId = createEmployee("PAYROLL", "Payroll Employee")

        // Generate payroll first
        val genRequest = mapOf("year" to 2026)
        mockMvc.perform(
            post("/api/v1/treasury/employees/$empId/payroll/generate")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(genRequest)),
        ).andExpect(status().isOk)

        mockMvc.perform(
            get("/api/v1/treasury/employees/$empId/payroll")
                .param("year", "2026")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data.length()").value(24))
    }

    @Test
    fun `pay payroll entry should return 200`() {
        val empId = createEmployee("PAYROLL", "Pay Employee")

        // Generate payroll
        val genRequest = mapOf("year" to 2026)
        val genResult = mockMvc.perform(
            post("/api/v1/treasury/employees/$empId/payroll/generate")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(genRequest)),
        ).andExpect(status().isOk)
            .andReturn()

        val entryId = objectMapper.readTree(genResult.response.contentAsString)
            .get("data").get(0).get("id").asLong()

        val payRequest = mapOf(
            "paymentDate" to "2026-01-15",
            "bankAccountId" to bankAccountId,
        )

        mockMvc.perform(
            post("/api/v1/treasury/employees/payroll/$entryId/pay")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payRequest)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.status").value("PAID"))
            .andExpect(jsonPath("$.data.paidDate").value("2026-01-15"))
    }

    // ============ CONTRACTOR PAYMENT TESTS ============

    @Test
    fun `record contractor payment should return 201`() {
        val empId = createEmployee("CONTRACTOR", "Contractor Payment")

        val request = mapOf(
            "amount" to 3000.00,
            "paymentDate" to "2026-03-01",
            "invoiceNumber" to "INV-001",
            "bankAccountId" to bankAccountId,
        )

        mockMvc.perform(
            post("/api/v1/treasury/employees/$empId/payments")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
    }

    // ============ TERMINATION TESTS ============

    @Test
    fun `terminate employee should return 200`() {
        val empId = createEmployee("PAYROLL", "To Terminate")

        val request = mapOf(
            "terminationDate" to "2026-03-15",
            "terminationReason" to "Resigned",
        )

        mockMvc.perform(
            post("/api/v1/treasury/employees/$empId/terminate")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.active").value(false))
            .andExpect(jsonPath("$.data.terminationDate").value("2026-03-15"))
    }

    @Test
    fun `terminate already terminated employee should return 400`() {
        val empId = createEmployee("PAYROLL", "Already Terminated")
        terminateEmployee(empId)

        val request = mapOf(
            "terminationDate" to "2026-04-01",
        )

        mockMvc.perform(
            post("/api/v1/treasury/employees/$empId/terminate")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)
    }

    // ============ INDEMNIZACION TESTS ============

    @Test
    fun `get indemnizacion should return calculation`() {
        val empId = createEmployee("PAYROLL", "Indemnizacion Employee")

        mockMvc.perform(
            get("/api/v1/treasury/employees/$empId/indemnizacion")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").exists())
    }

    // ============ PAYMENT HISTORY TESTS ============

    @Test
    fun `get payment history should return list`() {
        val empId = createEmployee("PAYROLL", "History Employee")

        mockMvc.perform(
            get("/api/v1/treasury/employees/$empId/payment-history")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray)
    }

    // ============ PERMISSION TESTS ============

    @Test
    fun `doctor cannot create employee`() {
        val request = mapOf(
            "fullName" to "Unauthorized",
            "employeeType" to "PAYROLL",
            "baseSalary" to 5000.00,
        )

        mockMvc.perform(
            post("/api/v1/treasury/employees")
                .header("Authorization", "Bearer $doctorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `doctor cannot list employees`() {
        mockMvc.perform(
            get("/api/v1/treasury/employees")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `doctor cannot terminate employee`() {
        val empId = createEmployee("PAYROLL", "Test Employee")

        val request = mapOf(
            "terminationDate" to "2026-03-15",
        )

        mockMvc.perform(
            post("/api/v1/treasury/employees/$empId/terminate")
                .header("Authorization", "Bearer $doctorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isForbidden)
    }

    // ============ HELPERS ============

    private fun createEmployee(type: String = "PAYROLL", name: String = "Test Employee"): Long {
        val request = mutableMapOf<String, Any>(
            "fullName" to name,
            "employeeType" to type,
        )
        if (type == "PAYROLL") request["baseSalary"] = 5000.00
        if (type == "CONTRACTOR") request["contractedRate"] = 3000.00

        val result = mockMvc.perform(
            post("/api/v1/treasury/employees")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andExpect(status().isCreated)
            .andReturn()

        return objectMapper.readTree(result.response.contentAsString)
            .get("data").get("id").asLong()
    }

    private fun terminateEmployee(empId: Long) {
        val request = mapOf(
            "terminationDate" to "2026-03-15",
            "terminationReason" to "Test termination",
        )

        mockMvc.perform(
            post("/api/v1/treasury/employees/$empId/terminate")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andExpect(status().isOk)
    }
}
