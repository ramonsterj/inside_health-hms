@file:Suppress("FunctionSignature", "FunctionExpressionBody")

package com.insidehealthgt.hms.controller

import com.insidehealthgt.hms.dto.request.CreateLabPanelRequest
import com.insidehealthgt.hms.dto.request.CreateLabProviderRequest
import com.insidehealthgt.hms.dto.request.CreateLabProviderTestRequest
import com.insidehealthgt.hms.dto.request.CreateLabTestRequest
import com.insidehealthgt.hms.repository.LabPanelRepository
import com.insidehealthgt.hms.repository.LabProviderRepository
import com.insidehealthgt.hms.repository.LabTestRepository
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.hasItem
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal

class LabCatalogControllerTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var labProviderRepository: LabProviderRepository

    @Autowired
    private lateinit var labTestRepository: LabTestRepository

    @Autowired
    private lateinit var labPanelRepository: LabPanelRepository

    private lateinit var adminToken: String
    private lateinit var doctorToken: String

    @BeforeEach
    fun setUp() {
        adminToken = createAdminUser().second
        doctorToken = createDoctorUser().second
    }

    private fun clonyId(): Long {
        return labProviderRepository.findAllByActiveTrueOrderByNameAsc().first { it.name == "CLONY" }.id!!
    }

    private fun herreraId(): Long {
        return labProviderRepository.findAllByActiveTrueOrderByNameAsc()
            .first { it.name == "HOSPITAL HERRERA LLERANDI" }.id!!
    }

    private fun ingresoPanelId(): Long {
        return labPanelRepository.findAllWithItems().first { it.name == "Laboratorios de ingreso" }.id!!
    }

    // ============ READ AUTHORIZATION (AC10 / AC13) ============

    @Test
    fun `doctor can read lab providers`() {
        mockMvc.perform(get("/api/v1/lab/providers").header("Authorization", "Bearer $doctorToken"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[*].name").value(hasItem("CLONY")))
    }

    @Test
    fun `resident can read lab providers`() {
        val residentToken = createResidentUser().second
        mockMvc.perform(get("/api/v1/lab/providers").header("Authorization", "Bearer $residentToken"))
            .andExpect(status().isOk)
    }

    @Test
    fun `psychologist cannot read lab catalog`() {
        val token = createPsychologistUser().second
        mockMvc.perform(get("/api/v1/lab/providers").header("Authorization", "Bearer $token"))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `nurse cannot read lab catalog`() {
        val token = createNurseUser().second
        mockMvc.perform(get("/api/v1/lab/tests").header("Authorization", "Bearer $token"))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `auxiliary nurse and chief nurse and admin staff cannot read lab catalog`() {
        val aux = createUserWithRole("AUXILIAR_ENFERMERIA", "aux", "aux@x.com", "password123").second
        val chief = createChiefNurseUser().second
        val staff = createAdminStaffUser().second
        for (token in listOf(aux, chief, staff)) {
            mockMvc.perform(get("/api/v1/lab/panels").header("Authorization", "Bearer $token"))
                .andExpect(status().isForbidden)
        }
    }

    // ============ MANAGE AUTHORIZATION ============

    @Test
    fun `doctor cannot manage lab catalog`() {
        val request = CreateLabProviderRequest(name = "New Provider")
        mockMvc.perform(
            post("/api/v1/lab/providers")
                .header("Authorization", "Bearer $doctorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andExpect(status().isForbidden)
    }

    // ============ CRUD + SOFT DELETE (AC5) ============

    @Test
    fun `admin can create update and soft-delete a provider`() {
        val createRes = mockMvc.perform(
            post("/api/v1/lab/providers")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(CreateLabProviderRequest(name = "Bio Lab", code = "BIO"))),
        ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.name").value("Bio Lab"))
            .andReturn()
        val id = objectMapper.readTree(createRes.response.contentAsString).get("data").get("id").asLong()

        mockMvc.perform(delete("/api/v1/lab/providers/$id").header("Authorization", "Bearer $adminToken"))
            .andExpect(status().isOk)

        // Soft-deleted: no longer returned.
        mockMvc.perform(get("/api/v1/lab/providers").header("Authorization", "Bearer $adminToken"))
            .andExpect(jsonPath("$.data[?(@.name == 'Bio Lab')]").isEmpty)
    }

    @Test
    fun `creating a provider with a duplicate name returns 400`() {
        mockMvc.perform(
            post("/api/v1/lab/providers")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(CreateLabProviderRequest(name = "clony"))),
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `deleting a referenced provider is blocked`() {
        mockMvc.perform(delete("/api/v1/lab/providers/${clonyId()}").header("Authorization", "Bearer $adminToken"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.message").value(containsString("provider tests")))
    }

    @Test
    fun `admin can add a provider test`() {
        // Create a fresh provider + canonical test, then offer the test.
        val provRes = mockMvc.perform(
            post("/api/v1/lab/providers")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(CreateLabProviderRequest(name = "Offer Lab"))),
        ).andReturn()
        val provId = objectMapper.readTree(provRes.response.contentAsString).get("data").get("id").asLong()

        val testRes = mockMvc.perform(
            post("/api/v1/lab/tests")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(CreateLabTestRequest(name = "Custom Marker"))),
        ).andReturn()
        val testId = objectMapper.readTree(testRes.response.contentAsString).get("data").get("id").asLong()

        mockMvc.perform(
            post("/api/v1/lab/providers/$provId/tests")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        CreateLabProviderTestRequest(
                            labTestId = testId,
                            displayName = "Custom Marker (panel)",
                            cost = BigDecimal("10.00"),
                            salesPrice = BigDecimal("25.00"),
                        ),
                    ),
                ),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.salesPrice").value(25.00))
            .andExpect(jsonPath("$.data.labTestName").value("Custom Marker"))
    }

    @Test
    fun `admin can create a panel`() {
        val testRes = mockMvc.perform(
            post("/api/v1/lab/tests")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(CreateLabTestRequest(name = "Panel Marker"))),
        ).andReturn()
        val testId = objectMapper.readTree(testRes.response.contentAsString).get("data").get("id").asLong()

        mockMvc.perform(
            post("/api/v1/lab/panels")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        CreateLabPanelRequest(name = "Custom Panel", labTestIds = listOf(testId)),
                    ),
                ),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.items.length()").value(1))
    }

    @Test
    fun `updating a panel soft-deletes replaced items`() {
        val firstTestId = createCanonicalTest("Panel Marker A")
        val secondTestId = createCanonicalTest("Panel Marker B")

        val panelRes = mockMvc.perform(
            post("/api/v1/lab/panels")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        CreateLabPanelRequest(name = "Soft Delete Panel", labTestIds = listOf(firstTestId)),
                    ),
                ),
        ).andExpect(status().isCreated).andReturn()
        val panelId = objectMapper.readTree(panelRes.response.contentAsString).get("data").get("id").asLong()

        mockMvc.perform(
            put("/api/v1/lab/panels/$panelId")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        CreateLabPanelRequest(name = "Soft Delete Panel", labTestIds = listOf(secondTestId)),
                    ),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.items.length()").value(1))
            .andExpect(jsonPath("$.data.items[0].labTestId").value(secondTestId.toInt()))

        val rowCounts = jdbcTemplate.queryForMap(
            """
            SELECT COUNT(*) AS total,
                   COUNT(*) FILTER (WHERE deleted_at IS NOT NULL) AS deleted
            FROM lab_panel_items
            WHERE panel_id = ?
            """.trimIndent(),
            panelId,
        )
        org.junit.jupiter.api.Assertions.assertEquals(2L, (rowCounts["total"] as Number).toLong())
        org.junit.jupiter.api.Assertions.assertEquals(1L, (rowCounts["deleted"] as Number).toLong())
    }

    // ============ PANEL RESOLUTION (AC2 / AC6) ============

    @Test
    fun `resolving ingreso panel against CLONY matches all panel tests`() {
        mockMvc.perform(
            get("/api/v1/lab/panels/${ingresoPanelId()}/resolve")
                .param("providerId", clonyId().toString())
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.unmatchedTests").isEmpty)
            .andExpect(jsonPath("$.data.matched[*].displayName").value(hasItem("Hemograma completo")))
    }

    @Test
    fun `resolving ingreso panel against Herrera flags the blood drug panel as unmatched`() {
        mockMvc.perform(
            get("/api/v1/lab/panels/${ingresoPanelId()}/resolve")
                .param("providerId", herreraId().toString())
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.unmatchedTests.length()").value(1))
            .andExpect(
                jsonPath("$.data.unmatchedTests[0].name")
                    .value(containsString("Panel de drogas en sangre")),
            )
    }

    private fun createCanonicalTest(name: String): Long {
        val response = mockMvc.perform(
            post("/api/v1/lab/tests")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(CreateLabTestRequest(name = name))),
        ).andExpect(status().isCreated).andReturn()
        return objectMapper.readTree(response.response.contentAsString).get("data").get("id").asLong()
    }
}
