package com.insidehealthgt.hms.controller

import com.insidehealthgt.hms.dto.request.LoginRequest
import com.insidehealthgt.hms.dto.request.RefreshTokenRequest
import com.insidehealthgt.hms.entity.User
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class AuthControllerTest : AbstractIntegrationTest() {

    private fun createUser(
        username: String,
        email: String,
        password: String,
        firstName: String? = null,
        lastName: String? = null,
    ): User {
        val userRole = roleRepository.findByCode("USER")!!
        val user = User(
            username = username,
            email = email,
            passwordHash = passwordEncoder.encode(password)!!,
            firstName = firstName,
            lastName = lastName,
            mustChangePassword = false,
        )
        user.roles.add(userRole)
        return userRepository.save(user)
    }

    // ============ LOGIN TESTS ============

    @Test
    fun `login should return tokens for valid credentials`() {
        createUser(
            username = "loginuser",
            email = "login@example.com",
            password = "password123",
        )

        val loginRequest = LoginRequest(
            identifier = "login@example.com",
            password = "password123",
        )

        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.accessToken").exists())
            .andExpect(jsonPath("$.data.refreshToken").exists())
    }

    @Test
    fun `login should work with username as identifier`() {
        createUser(
            username = "testuser",
            email = "test@example.com",
            password = "password123",
        )

        val loginRequest = LoginRequest(
            identifier = "testuser",
            password = "password123",
        )

        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.accessToken").exists())
    }

    @Test
    fun `login should fail with invalid credentials`() {
        val loginRequest = LoginRequest(
            identifier = "nonexistent@example.com",
            password = "wrongpassword",
        )

        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.success").value(false))
    }

    @Test
    fun `login should fail with wrong password`() {
        createUser(
            username = "wrongpass",
            email = "wrongpass@example.com",
            password = "correctpassword",
        )

        val loginRequest = LoginRequest(
            identifier = "wrongpass@example.com",
            password = "wrongpassword",
        )

        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.success").value(false))
    }

    // ============ REFRESH TOKEN TESTS ============

    @Test
    fun `refresh should return new tokens for valid refresh token`() {
        createUser(
            username = "refreshuser",
            email = "refresh@example.com",
            password = "password123",
        )

        val authResponse = loginAndGetAuthResponse("refresh@example.com", "password123")

        val refreshRequest = RefreshTokenRequest(
            refreshToken = authResponse.refreshToken,
        )

        mockMvc.perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.accessToken").exists())
            .andExpect(jsonPath("$.data.refreshToken").exists())
    }

    @Test
    fun `refresh should fail with invalid refresh token`() {
        val refreshRequest = RefreshTokenRequest(
            refreshToken = "invalid-token-value",
        )

        mockMvc.perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest)),
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `refresh should fail with already-used refresh token`() {
        createUser(
            username = "revokeduser",
            email = "revoked@example.com",
            password = "password123",
        )

        val authResponse = loginAndGetAuthResponse("revoked@example.com", "password123")

        val refreshRequest = RefreshTokenRequest(
            refreshToken = authResponse.refreshToken,
        )

        // First refresh should succeed
        mockMvc.perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest)),
        )
            .andExpect(status().isOk)

        // Second refresh with the same token should fail (token was revoked after first use)
        mockMvc.perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest)),
        )
            .andExpect(status().isUnauthorized)
    }

    // ============ LOGOUT TESTS ============

    @Test
    fun `logout should succeed for authenticated user`() {
        createUser(
            username = "logoutuser",
            email = "logout@example.com",
            password = "password123",
        )

        val token = loginAndGetToken("logout@example.com", "password123")

        mockMvc.perform(
            post("/api/auth/logout")
                .header("Authorization", "Bearer $token"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
    }

    @Test
    fun `logout without authentication is a no-op`() {
        mockMvc.perform(
            post("/api/auth/logout"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
    }

    // ============ CHECK USERNAME TESTS ============

    @Test
    fun `check username should return available for non-existing username`() {
        mockMvc.perform(
            get("/api/auth/check-username")
                .param("username", "availableuser"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.available").value(true))
    }

    @Test
    fun `check username should return unavailable for existing username`() {
        createUser(
            username = "takenuser",
            email = "taken@example.com",
            password = "password123",
        )

        mockMvc.perform(
            get("/api/auth/check-username")
                .param("username", "takenuser"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.available").value(false))
    }

    @Test
    fun `check username should reject too short username`() {
        mockMvc.perform(
            get("/api/auth/check-username")
                .param("username", "ab"),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
    }
}
