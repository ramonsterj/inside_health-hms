@file:Suppress("FunctionSignature", "FunctionExpressionBody")

package com.insidehealthgt.hms.controller

import com.insidehealthgt.hms.dto.request.CreateLabProviderTestRequest
import com.insidehealthgt.hms.dto.request.CreateLabTestRequest
import com.insidehealthgt.hms.dto.request.CreateMedicalOrderRequest
import com.insidehealthgt.hms.entity.MedicalOrderCategory
import com.insidehealthgt.hms.entity.User
import com.insidehealthgt.hms.repository.LabProviderRepository
import com.insidehealthgt.hms.repository.LabProviderTestRepository
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Suppress("LargeClass")
class LabOrderControllerTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var labProviderRepository: LabProviderRepository

    @Autowired
    private lateinit var labProviderTestRepository: LabProviderTestRepository

    private lateinit var adminToken: String
    private lateinit var doctorToken: String
    private lateinit var nurseToken: String
    private lateinit var doctorUser: User
    private var admissionId: Long = 0

    @BeforeEach
    fun setUp() {
        adminToken = createAdminUser().second
        val (docUsr, docTkn) = createDoctorUser()
        doctorUser = docUsr
        doctorToken = docTkn
        nurseToken = createNurseUser().second

        val patientId = createPatient(adminToken)
        admissionId = createAdmission(adminToken, patientId, doctorUser.id!!)
    }

    private fun clony() =
        labProviderRepository.findAllByActiveTrueOrderByNameAsc().first { it.name == "CLONY" }

    private fun clonyTests() =
        labProviderTestRepository.findByProviderIdAndActiveTrueOrderByDisplayNameAsc(clony().id!!)

    private fun herrera() =
        labProviderRepository.findAllByActiveTrueOrderByNameAsc().first { it.name == "HOSPITAL HERRERA LLERANDI" }

    private fun postOrder(body: CreateMedicalOrderRequest, token: String = doctorToken) =
        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/medical-orders")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)),
        )

    // ============ AC1: create with line total ============

    @Test
    fun `create lab order with provider and tests persists snapshot lines and total`() {
        val tests = clonyTests().take(2)
        val expectedTotal = tests.sumOf { it.salesPrice }

        postOrder(
            CreateMedicalOrderRequest(
                category = MedicalOrderCategory.LABORATORIOS,
                startDate = LocalDate.now(),
                labProviderId = clony().id,
                labProviderTestIds = tests.map { it.id!! },
            ),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.status").value("SOLICITADO"))
            .andExpect(jsonPath("$.data.labProvider.name").value("CLONY"))
            .andExpect(jsonPath("$.data.labTests.length()").value(2))
            .andExpect(jsonPath("$.data.labTotal").value(expectedTotal.toDouble()))
            .andExpect(jsonPath("$.data.inventoryItemId").doesNotExist())
    }

    // ============ AC3: billing on authorize ============

    @Test
    fun `authorizing a lab order creates one LAB charge per test, summing to the line total`() {
        val tests = clonyTests().take(3)
        val total = tests.sumOf { it.salesPrice }
        val orderId = createLabOrder(clony().id!!, tests.map { it.id!! })

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/medical-orders/$orderId/authorize")
                .header("Authorization", "Bearer $adminToken"),
        ).andExpect(status().isOk).andExpect(jsonPath("$.data.status").value("AUTORIZADO"))

        val charges = mockMvc.perform(
            get("/api/v1/admissions/$admissionId/charges").header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(3))
            .andExpect(jsonPath("$.data[0].chargeType").value("LAB"))
            .andExpect(jsonPath("$.data[0].description").value(containsString("CLONY")))
            .andReturn()

        // Every test is itemized as its own LAB charge; the charges sum to the requisition total.
        val chargeTotal = objectMapper.readTree(charges.response.contentAsString).get("data")
            .sumOf { it.get("totalAmount").decimalValue() }
        org.junit.jupiter.api.Assertions.assertEquals(0, total.compareTo(chargeTotal))
    }

    @Test
    fun `rejecting a lab order creates no charge and leaves authorization fields null`() {
        val tests = clonyTests().take(3)
        val orderId = createLabOrder(clony().id!!, tests.map { it.id!! })

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/medical-orders/$orderId/reject")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"reason":"Sin cobertura"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.status").value("NO_AUTORIZADO"))
            .andExpect(jsonPath("$.data.rejectedAt").exists())
            .andExpect(jsonPath("$.data.authorizedAt").doesNotExist())
            .andExpect(jsonPath("$.data.authorizedBy").doesNotExist())

        // Billing fires only on authorization — a rejected lab order bills nothing (cross-ref AC3).
        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/charges").header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(0))
    }

    // ============ AC7: cross-provider line rejected ============

    @Test
    fun `lab order with a test from another provider returns 400`() {
        val clonyTest = clonyTests().first().id!!
        val herreraTest = labProviderTestRepository
            .findByProviderIdAndActiveTrueOrderByDisplayNameAsc(herrera().id!!).first().id!!

        postOrder(
            CreateMedicalOrderRequest(
                category = MedicalOrderCategory.LABORATORIOS,
                startDate = LocalDate.now(),
                labProviderId = clony().id,
                labProviderTestIds = listOf(clonyTest, herreraTest),
            ),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.message").value(containsString("provider")))
    }

    // ============ AC8: zero lines ============

    @Test
    fun `lab order with a provider but no tests returns 400`() {
        postOrder(
            CreateMedicalOrderRequest(
                category = MedicalOrderCategory.LABORATORIOS,
                startDate = LocalDate.now(),
                labProviderId = clony().id,
                labProviderTestIds = emptyList(),
            ),
        ).andExpect(status().isBadRequest)
    }

    // ============ AC11: inactive provider-test ============

    @Test
    fun `lab order referencing a soft-deleted provider-test returns 400`() {
        val targetId = createClonyProviderTest("Soft Deleted Provider Test")
        val target = labProviderTestRepository.findById(targetId).orElseThrow()
        target.deletedAt = LocalDateTime.now()
        labProviderTestRepository.save(target)

        postOrder(
            CreateMedicalOrderRequest(
                category = MedicalOrderCategory.LABORATORIOS,
                startDate = LocalDate.now(),
                labProviderId = clony().id,
                labProviderTestIds = listOf(targetId),
            ),
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `recorded lab order remains readable after its provider-test is soft-deleted`() {
        val targetId = createClonyProviderTest("Readable Snapshot Provider Test")
        val orderId = createLabOrder(clony().id!!, listOf(targetId))

        val target = labProviderTestRepository.findById(targetId).orElseThrow()
        target.deletedAt = LocalDateTime.now()
        labProviderTestRepository.save(target)

        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/medical-orders/$orderId")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.labTests[0].labProviderTestId").value(targetId.toInt()))
    }

    // ============ AC9: discharged admission ============

    @Test
    fun `creating a lab order on a discharged admission returns 400`() {
        dischargeAdmission(admissionId, adminToken)
        postOrder(
            CreateMedicalOrderRequest(
                category = MedicalOrderCategory.LABORATORIOS,
                startDate = LocalDate.now(),
                labProviderId = clony().id,
                labProviderTestIds = clonyTests().take(1).map { it.id!! },
            ),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.message").value(containsString("discharged")))
    }

    // ============ AC14: update lines only while SOLICITADO ============

    @Test
    fun `lab lines can be updated while SOLICITADO`() {
        val tests = clonyTests()
        val orderId = createLabOrder(clony().id!!, tests.take(1).map { it.id!! })

        mockMvc.perform(
            put("/api/v1/admissions/$admissionId/medical-orders/$orderId")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        CreateMedicalOrderRequest(
                            category = MedicalOrderCategory.LABORATORIOS,
                            startDate = LocalDate.now(),
                            labProviderId = clony().id,
                            labProviderTestIds = tests.take(3).map { it.id!! },
                        ),
                    ),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.labTests.length()").value(3))

        val rowCounts = jdbcTemplate.queryForMap(
            """
            SELECT COUNT(*) AS total,
                   COUNT(*) FILTER (WHERE deleted_at IS NOT NULL) AS deleted
            FROM medical_order_lab_tests
            WHERE medical_order_id = ?
            """.trimIndent(),
            orderId,
        )
        org.junit.jupiter.api.Assertions.assertEquals(4L, (rowCounts["total"] as Number).toLong())
        org.junit.jupiter.api.Assertions.assertEquals(1L, (rowCounts["deleted"] as Number).toLong())
    }

    @Test
    fun `lab lines cannot be updated after authorization`() {
        val tests = clonyTests()
        val orderId = createLabOrder(clony().id!!, tests.take(1).map { it.id!! })

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/medical-orders/$orderId/authorize")
                .header("Authorization", "Bearer $adminToken"),
        ).andExpect(status().isOk)

        mockMvc.perform(
            put("/api/v1/admissions/$admissionId/medical-orders/$orderId")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        CreateMedicalOrderRequest(
                            category = MedicalOrderCategory.LABORATORIOS,
                            startDate = LocalDate.now(),
                            labProviderId = clony().id,
                            labProviderTestIds = tests.take(2).map { it.id!! },
                        ),
                    ),
                ),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.message").value(containsString("SOLICITADO")))
    }

    // ============ AC12: catalog edits do not change recorded order / charge ============

    @Test
    fun `repricing a provider-test after authorization does not change the recorded line or charge`() {
        val tests = clonyTests().take(2)
        val originalTotal = tests.sumOf { it.salesPrice }
        val orderId = createLabOrder(clony().id!!, tests.map { it.id!! })

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/medical-orders/$orderId/authorize")
                .header("Authorization", "Bearer $adminToken"),
        ).andExpect(status().isOk)

        // Reprice the catalog row afterwards.
        val first = tests.first()
        first.salesPrice = first.salesPrice.add(BigDecimal("999.00"))
        labProviderTestRepository.save(first)

        // Recorded order total unchanged.
        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/medical-orders/$orderId")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(jsonPath("$.data.labTotal").value(originalTotal.toDouble()))

        // Charges unchanged: one per test, still summing to the snapshotted total.
        val charges = mockMvc.perform(
            get("/api/v1/admissions/$admissionId/charges").header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(jsonPath("$.data.length()").value(tests.size))
            .andReturn()
        val chargeTotal = objectMapper.readTree(charges.response.contentAsString).get("data")
            .sumOf { it.get("totalAmount").decimalValue() }
        org.junit.jupiter.api.Assertions.assertEquals(0, originalTotal.compareTo(chargeTotal))
    }

    // ============ No legacy support: a lab order must carry a provider + lines ============

    @Test
    fun `lab order without provider or tests returns 400`() {
        postOrder(
            CreateMedicalOrderRequest(
                category = MedicalOrderCategory.LABORATORIOS,
                startDate = LocalDate.now(),
                observations = "Free-text lab without catalog lines",
            ),
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `changing a lab order to another category on update is rejected and leaves provider and lines intact`() {
        val tests = clonyTests().take(2)
        val orderId = createLabOrder(clony().id!!, tests.map { it.id!! })

        mockMvc.perform(
            put("/api/v1/admissions/$admissionId/medical-orders/$orderId")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        CreateMedicalOrderRequest(
                            category = MedicalOrderCategory.ORDENES_MEDICAS,
                            startDate = LocalDate.now(),
                            observations = "Now a directive order",
                        ),
                    ),
                ),
        )
            .andExpect(status().isBadRequest)

        // The order is unchanged: still a LABORATORIOS order with its provider + lines.
        mockMvc.perform(
            get("/api/v1/admissions/$admissionId/medical-orders/$orderId")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.category").value("LABORATORIOS"))
            .andExpect(jsonPath("$.data.labProvider.name").value("CLONY"))
            .andExpect(jsonPath("$.data.labTests.length()").value(tests.size))
    }

    @Test
    fun `editing observations on an authorized lab order succeeds and leaves lines intact`() {
        val tests = clonyTests().take(2)
        val orderId = createLabOrder(clony().id!!, tests.map { it.id!! })

        mockMvc.perform(
            post("/api/v1/admissions/$admissionId/medical-orders/$orderId/authorize")
                .header("Authorization", "Bearer $adminToken"),
        ).andExpect(status().isOk)

        // Re-send the same provider + tests (as the disabled UI does) but change observations.
        mockMvc.perform(
            put("/api/v1/admissions/$admissionId/medical-orders/$orderId")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        CreateMedicalOrderRequest(
                            category = MedicalOrderCategory.LABORATORIOS,
                            startDate = LocalDate.now(),
                            labProviderId = clony().id,
                            labProviderTestIds = tests.map { it.id!! },
                            observations = "Updated clinical observations",
                        ),
                    ),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.status").value("AUTORIZADO"))
            .andExpect(jsonPath("$.data.observations").value("Updated clinical observations"))
            .andExpect(jsonPath("$.data.labTests.length()").value(tests.size))
    }

    private fun createLabOrder(providerId: Long, testIds: List<Long>): Long {
        val result = postOrder(
            CreateMedicalOrderRequest(
                category = MedicalOrderCategory.LABORATORIOS,
                startDate = LocalDate.now(),
                labProviderId = providerId,
                labProviderTestIds = testIds,
            ),
        ).andExpect(status().isCreated).andReturn()
        return objectMapper.readTree(result.response.contentAsString).get("data").get("id").asLong()
    }

    private fun createClonyProviderTest(testName: String): Long {
        val testResult = mockMvc.perform(
            post("/api/v1/lab/tests")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(CreateLabTestRequest(name = testName))),
        ).andExpect(status().isCreated).andReturn()
        val testId = objectMapper.readTree(testResult.response.contentAsString).get("data").get("id").asLong()

        val providerTestResult = mockMvc.perform(
            post("/api/v1/lab/providers/${clony().id}/tests")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        CreateLabProviderTestRequest(
                            labTestId = testId,
                            displayName = testName,
                            cost = BigDecimal("10.00"),
                            salesPrice = BigDecimal("25.00"),
                        ),
                    ),
                ),
        ).andExpect(status().isCreated).andReturn()
        return objectMapper.readTree(providerTestResult.response.contentAsString).get("data").get("id").asLong()
    }
}
