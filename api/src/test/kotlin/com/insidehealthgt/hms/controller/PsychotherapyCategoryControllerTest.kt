package com.insidehealthgt.hms.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.insidehealthgt.hms.TestcontainersConfiguration
import com.insidehealthgt.hms.dto.request.CreatePsychotherapyCategoryRequest
import com.insidehealthgt.hms.dto.request.UpdatePsychotherapyCategoryRequest
import com.insidehealthgt.hms.dto.response.ApiResponse
import com.insidehealthgt.hms.dto.response.AuthResponse
import com.insidehealthgt.hms.entity.Salutation
import com.insidehealthgt.hms.entity.User
import com.insidehealthgt.hms.repository.PsychotherapyActivityRepository
import com.insidehealthgt.hms.repository.PsychotherapyCategoryRepository
import com.insidehealthgt.hms.repository.RoleRepository
import com.insidehealthgt.hms.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class PsychotherapyCategoryControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var roleRepository: RoleRepository

    @Autowired
    private lateinit var categoryRepository: PsychotherapyCategoryRepository

    @Autowired
    private lateinit var activityRepository: PsychotherapyActivityRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    private lateinit var adminToken: String
    private lateinit var psychologistToken: String
    private lateinit var doctorToken: String
    private lateinit var nurseToken: String

    @BeforeEach
    fun setUp() {
        activityRepository.deleteAllHard()
        userRepository.deleteAll()

        // Create admin user
        val adminRole = roleRepository.findByCode("ADMIN")!!
        val adminUser = User(
            username = "admin",
            email = "admin@example.com",
            passwordHash = passwordEncoder.encode("admin123")!!,
            firstName = "Admin",
            lastName = "User",
        )
        adminUser.roles.add(adminRole)
        userRepository.save(adminUser)
        adminToken = loginAndGetToken("admin@example.com", "admin123")

        // Create psychologist user
        val psychologistRole = roleRepository.findByCode("PSYCHOLOGIST")!!
        val psychologistUser = User(
            username = "psychologist",
            email = "psychologist@example.com",
            passwordHash = passwordEncoder.encode("password123")!!,
            firstName = "Sofia",
            lastName = "Martinez",
            salutation = Salutation.LICDA,
        )
        psychologistUser.roles.add(psychologistRole)
        userRepository.save(psychologistUser)
        psychologistToken = loginAndGetToken("psychologist@example.com", "password123")

        // Create doctor user
        val doctorRole = roleRepository.findByCode("DOCTOR")!!
        val doctorUser = User(
            username = "doctor",
            email = "doctor@example.com",
            passwordHash = passwordEncoder.encode("password123")!!,
            firstName = "Maria",
            lastName = "Garcia",
            salutation = Salutation.DR,
        )
        doctorUser.roles.add(doctorRole)
        userRepository.save(doctorUser)
        doctorToken = loginAndGetToken("doctor@example.com", "password123")

        // Create nurse user
        val nurseRole = roleRepository.findByCode("NURSE")!!
        val nurseUser = User(
            username = "nurse",
            email = "nurse@example.com",
            passwordHash = passwordEncoder.encode("password123")!!,
            firstName = "Ana",
            lastName = "Lopez",
        )
        nurseUser.roles.add(nurseRole)
        userRepository.save(nurseUser)
        nurseToken = loginAndGetToken("nurse@example.com", "password123")
    }

    private fun loginAndGetToken(email: String, password: String): String {
        val request = mapOf("identifier" to email, "password" to password)
        val result = mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andReturn()

        val responseType = objectMapper.typeFactory.constructParametricType(
            ApiResponse::class.java,
            AuthResponse::class.java,
        )
        val response: ApiResponse<AuthResponse> = objectMapper.readValue(
            result.response.contentAsString,
            responseType,
        )
        return response.data!!.accessToken
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
            .andExpect(jsonPath("$.data[0].name").value("Taller"))
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
        val category = categoryRepository.findAll().first()

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
        val category = categoryRepository.findAll().first()
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
        val category = categoryRepository.findAll().first()

        mockMvc.perform(
            delete("/api/v1/admin/psychotherapy-categories/${category.id}")
                .header("Authorization", "Bearer $psychologistToken"),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `delete category should fail without authentication`() {
        val category = categoryRepository.findAll().first()

        mockMvc.perform(
            delete("/api/v1/admin/psychotherapy-categories/${category.id}"),
        )
            .andExpect(status().isUnauthorized)
    }
}
