package com.insidehealthgt.hms.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.insidehealthgt.hms.TestcontainersConfiguration
import com.insidehealthgt.hms.dto.request.UpdateUserRequest
import com.insidehealthgt.hms.dto.response.ApiResponse
import com.insidehealthgt.hms.dto.response.AuthResponse
import com.insidehealthgt.hms.entity.User
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
class UserControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var roleRepository: RoleRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    private lateinit var userToken: String
    private lateinit var adminToken: String

    @BeforeEach
    fun setUp() {
        userRepository.deleteAll()

        // Create regular user (with permissions loaded)
        val userRole = roleRepository.findByCodeWithPermissions("USER")!!
        val regularUser = User(
            username = "user",
            email = "user@example.com",
            passwordHash = passwordEncoder.encode("password123")!!,
            firstName = "Regular",
            lastName = "User",
            mustChangePassword = false,
        )
        regularUser.roles.add(userRole)
        userRepository.save(regularUser)
        userToken = loginAndGetToken("user@example.com", "password123").accessToken

        // Create admin user (with permissions loaded)
        val adminRole = roleRepository.findByCodeWithPermissions("ADMIN")!!
        val adminUser = User(
            username = "admin",
            email = "admin@example.com",
            passwordHash = passwordEncoder.encode("admin123")!!,
            firstName = "Admin",
            lastName = "User",
            mustChangePassword = false,
        )
        adminUser.roles.add(adminRole)
        userRepository.save(adminUser)
        adminToken = loginAndGetToken("admin@example.com", "admin123").accessToken
    }

    private fun loginAndGetToken(email: String, password: String): AuthResponse {
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
        return response.data!!
    }

    @Test
    fun `getCurrentUser should return authenticated user`() {
        mockMvc.perform(
            get("/api/users/me")
                .header("Authorization", "Bearer $userToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.email").value("user@example.com"))
    }

    @Test
    fun `getCurrentUser should fail without authentication`() {
        mockMvc.perform(get("/api/users/me"))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `updateProfile should update user details`() {
        val request = UpdateUserRequest(
            firstName = "Updated",
            lastName = "Name",
        )

        mockMvc.perform(
            put("/api/users/me")
                .header("Authorization", "Bearer $userToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.firstName").value("Updated"))
            .andExpect(jsonPath("$.data.lastName").value("Name"))
    }

    @Test
    fun `listUsers should only be accessible by admin`() {
        // Regular user should be forbidden
        mockMvc.perform(
            get("/api/users")
                .header("Authorization", "Bearer $userToken"),
        )
            .andExpect(status().isForbidden)

        // Admin should succeed
        mockMvc.perform(
            get("/api/users")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content").isArray)
    }

    @Test
    fun `deleteUser should soft delete and exclude from queries`() {
        // Get user ID first
        val users = userRepository.findAll()
        val regularUser = users.first { it.email == "user@example.com" }

        // Admin deletes the user
        mockMvc.perform(
            delete("/api/users/${regularUser.id}")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)

        // User should no longer be found in normal queries
        mockMvc.perform(
            get("/api/users/${regularUser.id}")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `regular user cannot delete other users`() {
        val users = userRepository.findAll()
        val adminUser = users.first { it.email == "admin@example.com" }

        mockMvc.perform(
            delete("/api/users/${adminUser.id}")
                .header("Authorization", "Bearer $userToken"),
        )
            .andExpect(status().isForbidden)
    }
}
