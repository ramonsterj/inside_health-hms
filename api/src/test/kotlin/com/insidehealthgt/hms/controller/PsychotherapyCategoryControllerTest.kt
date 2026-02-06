package com.insidehealthgt.hms.controller

import com.insidehealthgt.hms.dto.request.CreatePsychotherapyCategoryRequest
import com.insidehealthgt.hms.dto.request.UpdatePsychotherapyCategoryRequest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class PsychotherapyCategoryControllerTest : AbstractIntegrationTest() {

    private lateinit var adminToken: String
    private lateinit var psychologistToken: String
    private lateinit var doctorToken: String
    private lateinit var nurseToken: String

    @BeforeEach
    fun setUp() {
        val (_, adminTkn) = createAdminUser()
        adminToken = adminTkn

        val (_, psychTkn) = createPsychologistUser()
        psychologistToken = psychTkn

        val (_, docTkn) = createDoctorUser()
        doctorToken = docTkn

        val (_, nurseTkn) = createNurseUser()
        nurseToken = nurseTkn
    }

    // ============ LIST CATEGORIES TESTS ============

    @Test
    fun `list active categories should return seeded categories`() {
        mockMvc.perform(
            get("/api/v1/psychotherapy-categories")
                .header("Authorization", "Bearer $psychologistToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data").isNotEmpty)
    }

    @Test
    fun `list active categories should be accessible by doctor`() {
        mockMvc.perform(
            get("/api/v1/psychotherapy-categories")
                .header("Authorization", "Bearer $doctorToken"),
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `list active categories should be accessible by nurse`() {
        mockMvc.perform(
            get("/api/v1/psychotherapy-categories")
                .header("Authorization", "Bearer $nurseToken"),
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `list all categories (admin) should include inactive`() {
        // First create an inactive category
        val request = CreatePsychotherapyCategoryRequest(
            name = "Inactive Category",
            description = "Test",
            displayOrder = 100,
            active = false,
        )
        mockMvc.perform(
            post("/api/v1/admin/psychotherapy-categories")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andExpect(status().isCreated)

        // List all categories
        mockMvc.perform(
            get("/api/v1/admin/psychotherapy-categories")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[?(@.name == 'Inactive Category')]").exists())
    }

    // ============ CREATE CATEGORY TESTS ============

    @Test
    fun `create category with valid data should return 201`() {
        val request = CreatePsychotherapyCategoryRequest(
            name = "Test Category",
            description = "Test description",
            displayOrder = 50,
            active = true,
        )

        mockMvc.perform(
            post("/api/v1/admin/psychotherapy-categories")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.name").value("Test Category"))
            .andExpect(jsonPath("$.data.description").value("Test description"))
            .andExpect(jsonPath("$.data.displayOrder").value(50))
            .andExpect(jsonPath("$.data.active").value(true))
    }

    @Test
    fun `create category should fail for psychologist`() {
        val request = CreatePsychotherapyCategoryRequest(
            name = "Test Category",
            description = "Test",
            displayOrder = 50,
            active = true,
        )

        mockMvc.perform(
            post("/api/v1/admin/psychotherapy-categories")
                .header("Authorization", "Bearer $psychologistToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `create category should fail with duplicate name`() {
        val request = CreatePsychotherapyCategoryRequest(
            name = "Taller", // Already seeded
            description = "Test",
            displayOrder = 50,
            active = true,
        )

        mockMvc.perform(
            post("/api/v1/admin/psychotherapy-categories")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `create category should fail with blank name`() {
        val request = mapOf(
            "name" to "",
            "description" to "Test",
            "displayOrder" to 50,
            "active" to true,
        )

        mockMvc.perform(
            post("/api/v1/admin/psychotherapy-categories")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)
    }

    // ============ GET CATEGORY TESTS ============

    @Test
    fun `get category should return category details`() {
        val category = psychotherapyCategoryRepository.findAll().first()

        mockMvc.perform(
            get("/api/v1/admin/psychotherapy-categories/${category.id}")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.name").exists())
    }

    @Test
    fun `get category should return 404 for non-existent`() {
        mockMvc.perform(
            get("/api/v1/admin/psychotherapy-categories/99999")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isNotFound)
    }

    // ============ UPDATE CATEGORY TESTS ============

    @Test
    fun `update category should update data`() {
        // Create a category first
        val createRequest = CreatePsychotherapyCategoryRequest(
            name = "Original Name",
            description = "Original description",
            displayOrder = 50,
            active = true,
        )
        val createResult = mockMvc.perform(
            post("/api/v1/admin/psychotherapy-categories")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)),
        ).andReturn()

        val categoryId = objectMapper.readTree(createResult.response.contentAsString)
            .get("data").get("id").asLong()

        val updateRequest = UpdatePsychotherapyCategoryRequest(
            name = "Updated Name",
            description = "Updated description",
            displayOrder = 60,
            active = false,
        )

        mockMvc.perform(
            put("/api/v1/admin/psychotherapy-categories/$categoryId")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.name").value("Updated Name"))
            .andExpect(jsonPath("$.data.description").value("Updated description"))
            .andExpect(jsonPath("$.data.displayOrder").value(60))
            .andExpect(jsonPath("$.data.active").value(false))
    }

    @Test
    fun `update category should fail for psychologist`() {
        val category = psychotherapyCategoryRepository.findAll().first()
        val updateRequest = UpdatePsychotherapyCategoryRequest(
            name = "Updated Name",
            description = "Updated",
            displayOrder = 60,
            active = true,
        )

        mockMvc.perform(
            put("/api/v1/admin/psychotherapy-categories/${category.id}")
                .header("Authorization", "Bearer $psychologistToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `update category should fail with duplicate name`() {
        // Create a category
        val createRequest = CreatePsychotherapyCategoryRequest(
            name = "Unique Category",
            description = "Test",
            displayOrder = 50,
            active = true,
        )
        val createResult = mockMvc.perform(
            post("/api/v1/admin/psychotherapy-categories")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)),
        ).andReturn()

        val categoryId = objectMapper.readTree(createResult.response.contentAsString)
            .get("data").get("id").asLong()

        // Try to update to a name that already exists (seeded data)
        val updateRequest = UpdatePsychotherapyCategoryRequest(
            name = "Taller",
            description = "Duplicate name",
            displayOrder = 50,
            active = true,
        )

        mockMvc.perform(
            put("/api/v1/admin/psychotherapy-categories/$categoryId")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.message").value("Psychotherapy category with name 'Taller' already exists"))
    }

    // ============ DELETE CATEGORY TESTS ============

    @Test
    fun `delete category should soft delete`() {
        // Create a category first
        val createRequest = CreatePsychotherapyCategoryRequest(
            name = "To Delete",
            description = "Will be deleted",
            displayOrder = 99,
            active = true,
        )
        val createResult = mockMvc.perform(
            post("/api/v1/admin/psychotherapy-categories")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)),
        ).andReturn()

        val categoryId = objectMapper.readTree(createResult.response.contentAsString)
            .get("data").get("id").asLong()

        mockMvc.perform(
            delete("/api/v1/admin/psychotherapy-categories/$categoryId")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isNoContent)

        // Should not be found after deletion
        mockMvc.perform(
            get("/api/v1/admin/psychotherapy-categories/$categoryId")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `delete category should fail for psychologist`() {
        val category = psychotherapyCategoryRepository.findAll().first()

        mockMvc.perform(
            delete("/api/v1/admin/psychotherapy-categories/${category.id}")
                .header("Authorization", "Bearer $psychologistToken"),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `delete category should fail without authentication`() {
        val category = psychotherapyCategoryRepository.findAll().first()

        mockMvc.perform(
            delete("/api/v1/admin/psychotherapy-categories/${category.id}"),
        )
            .andExpect(status().isUnauthorized)
    }
}
