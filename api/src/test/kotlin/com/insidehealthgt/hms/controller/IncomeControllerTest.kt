package com.insidehealthgt.hms.controller

import com.insidehealthgt.hms.entity.User
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class IncomeControllerTest : AbstractIntegrationTest() {

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

    // ============ CREATE TESTS ============

    @Test
    fun `create income should return 201`() {
        val request = mapOf(
            "description" to "Patient payment",
            "category" to "PATIENT_PAYMENT",
            "amount" to 1500.00,
            "incomeDate" to "2026-03-01",
            "reference" to "REC-001",
            "bankAccountId" to bankAccountId,
        )

        mockMvc.perform(
            post("/api/v1/treasury/income")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.description").value("Patient payment"))
            .andExpect(jsonPath("$.data.category").value("PATIENT_PAYMENT"))
            .andExpect(jsonPath("$.data.amount").value(1500.00))
            .andExpect(jsonPath("$.data.bankAccountName").value("Test Account"))
    }

    @Test
    fun `create income with missing fields should return 400`() {
        val request = mapOf(
            "description" to "Incomplete",
        )

        mockMvc.perform(
            post("/api/v1/treasury/income")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `create income with invalid category should return 400`() {
        val request = mapOf(
            "description" to "Test",
            "category" to "INVALID_CATEGORY",
            "amount" to 100.00,
            "incomeDate" to "2026-03-01",
            "bankAccountId" to bankAccountId,
        )

        mockMvc.perform(
            post("/api/v1/treasury/income")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `create income with zero amount should return 400`() {
        val request = mapOf(
            "description" to "Zero amount",
            "category" to "DONATION",
            "amount" to 0,
            "incomeDate" to "2026-03-01",
            "bankAccountId" to bankAccountId,
        )

        mockMvc.perform(
            post("/api/v1/treasury/income")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)
    }

    // ============ LIST TESTS ============

    @Test
    fun `list income should return paginated results`() {
        createIncome("Payment A", "PATIENT_PAYMENT", 500.00)
        createIncome("Payment B", "DONATION", 200.00)

        mockMvc.perform(
            get("/api/v1/treasury/income")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content").isArray)
            .andExpect(jsonPath("$.data.content.length()").value(2))
            .andExpect(jsonPath("$.data.page.totalElements").value(2))
    }

    @Test
    fun `list income with category filter should filter results`() {
        createIncome("Patient payment", "PATIENT_PAYMENT", 500.00)
        createIncome("Donation", "DONATION", 200.00)

        mockMvc.perform(
            get("/api/v1/treasury/income")
                .param("category", "DONATION")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content.length()").value(1))
            .andExpect(jsonPath("$.data.content[0].category").value("DONATION"))
    }

    @Test
    fun `list income with search filter should match description`() {
        createIncome("Patient payment Juan", "PATIENT_PAYMENT", 500.00)
        createIncome("Donation Red Cross", "DONATION", 200.00)

        mockMvc.perform(
            get("/api/v1/treasury/income")
                .param("search", "Juan")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content.length()").value(1))
            .andExpect(jsonPath("$.data.content[0].description").value("Patient payment Juan"))
    }

    // ============ GET BY ID TESTS ============

    @Test
    fun `get income by id should return 200`() {
        val incomeId = createIncome("Test income", "PATIENT_PAYMENT", 750.00)

        mockMvc.perform(
            get("/api/v1/treasury/income/$incomeId")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.description").value("Test income"))
            .andExpect(jsonPath("$.data.amount").value(750.00))
    }

    @Test
    fun `get income for non-existent id should return 404`() {
        mockMvc.perform(
            get("/api/v1/treasury/income/99999")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isNotFound)
    }

    // ============ UPDATE TESTS ============

    @Test
    fun `update income should return 200`() {
        val incomeId = createIncome("Original description", "PATIENT_PAYMENT", 500.00)

        val updateRequest = mapOf(
            "description" to "Updated description",
            "category" to "INSURANCE_PAYMENT",
            "amount" to 750.00,
            "incomeDate" to "2026-03-15",
            "bankAccountId" to bankAccountId,
        )

        mockMvc.perform(
            put("/api/v1/treasury/income/$incomeId")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.description").value("Updated description"))
            .andExpect(jsonPath("$.data.category").value("INSURANCE_PAYMENT"))
            .andExpect(jsonPath("$.data.amount").value(750.00))
    }

    // ============ DELETE TESTS ============

    @Test
    fun `delete income should return 200`() {
        val incomeId = createIncome("To be deleted", "DONATION", 100.00)

        mockMvc.perform(
            delete("/api/v1/treasury/income/$incomeId")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)

        // Should be soft-deleted and not found
        mockMvc.perform(
            get("/api/v1/treasury/income/$incomeId")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `delete income twice should return 404`() {
        val incomeId = createIncome("To be deleted", "DONATION", 100.00)

        mockMvc.perform(
            delete("/api/v1/treasury/income/$incomeId")
                .header("Authorization", "Bearer $adminToken"),
        ).andExpect(status().isOk)

        mockMvc.perform(
            delete("/api/v1/treasury/income/$incomeId")
                .header("Authorization", "Bearer $adminToken"),
        ).andExpect(status().isNotFound)
    }

    // ============ INVOICE SEARCH TESTS ============

    @Test
    fun `search invoices should return results`() {
        mockMvc.perform(
            get("/api/v1/treasury/income/invoices")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray)
    }

    // ============ PERMISSION TESTS ============

    @Test
    fun `doctor cannot create income`() {
        val request = mapOf(
            "description" to "Unauthorized income",
            "category" to "DONATION",
            "amount" to 100.00,
            "incomeDate" to "2026-03-01",
            "bankAccountId" to bankAccountId,
        )

        mockMvc.perform(
            post("/api/v1/treasury/income")
                .header("Authorization", "Bearer $doctorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `doctor cannot list income`() {
        mockMvc.perform(
            get("/api/v1/treasury/income")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `doctor cannot delete income`() {
        val incomeId = createIncome("Test", "DONATION", 100.00)

        mockMvc.perform(
            delete("/api/v1/treasury/income/$incomeId")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isForbidden)
    }

    // ============ HELPERS ============

    private fun createIncome(description: String, category: String, amount: Double): Long {
        val request = mapOf(
            "description" to description,
            "category" to category,
            "amount" to amount,
            "incomeDate" to "2026-03-01",
            "bankAccountId" to bankAccountId,
        )

        val result = mockMvc.perform(
            post("/api/v1/treasury/income")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andExpect(status().isCreated)
            .andReturn()

        return objectMapper.readTree(result.response.contentAsString)
            .get("data").get("id").asLong()
    }
}
