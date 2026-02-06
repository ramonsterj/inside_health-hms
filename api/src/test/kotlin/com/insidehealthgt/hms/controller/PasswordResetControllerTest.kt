package com.insidehealthgt.hms.controller

import com.insidehealthgt.hms.dto.request.ForgotPasswordRequest
import com.insidehealthgt.hms.dto.request.LoginRequest
import com.insidehealthgt.hms.dto.request.ResetPasswordRequest
import com.insidehealthgt.hms.entity.User
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PasswordResetControllerTest : AbstractIntegrationTest() {

    private fun createUser(username: String, email: String, password: String): User {
        val userRole = roleRepository.findByCode("USER")!!
        val user = User(
            username = username,
            email = email,
            passwordHash = passwordEncoder.encode(password)!!,
            mustChangePassword = false,
        )
        user.roles.add(userRole)
        return userRepository.save(user)
    }

    @Test
    fun `forgot-password should return success even for non-existent email`() {
        val request = ForgotPasswordRequest(email = "nonexistent@example.com")

        mockMvc.perform(
            post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").exists())
    }

    @Test
    fun `forgot-password should create token for existing user`() {
        createUser("resetuser", "reset@example.com", "password123")

        val request = ForgotPasswordRequest(email = "reset@example.com")

        mockMvc.perform(
            post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))

        val tokens = passwordResetTokenRepository.findAll()
        assertEquals(1, tokens.size)
        assertNotNull(tokens[0].token)
        assertEquals(64, tokens[0].token.length)
    }

    @Test
    fun `forgot-password should fail validation for invalid email`() {
        val request = ForgotPasswordRequest(email = "invalid-email")

        mockMvc.perform(
            post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
    }

    @Test
    fun `forgot-password should fail validation for blank email`() {
        val request = ForgotPasswordRequest(email = "")

        mockMvc.perform(
            post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
    }

    @Test
    fun `forgot-password should invalidate previous tokens`() {
        createUser("resetuser2", "reset2@example.com", "password123")

        val request = ForgotPasswordRequest(email = "reset2@example.com")

        mockMvc.perform(
            post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )

        val firstToken = passwordResetTokenRepository.findAll().first().token

        mockMvc.perform(
            post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )

        val tokens = passwordResetTokenRepository.findAll()
        assertEquals(1, tokens.size)
        assertTrue(tokens[0].token != firstToken)
    }

    @Test
    fun `reset-password should update password with valid token`() {
        createUser("resetuser3", "reset3@example.com", "oldpassword")

        mockMvc.perform(
            post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ForgotPasswordRequest("reset3@example.com"))),
        )

        val token = passwordResetTokenRepository.findAll().first().token

        val request = ResetPasswordRequest(token = token, newPassword = "newpassword123")

        mockMvc.perform(
            post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))

        assertEquals(0, passwordResetTokenRepository.count())

        val loginRequest = LoginRequest(identifier = "reset3@example.com", password = "newpassword123")
        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
    }

    @Test
    fun `reset-password should fail with invalid token`() {
        val request = ResetPasswordRequest(token = "invalid-token", newPassword = "newpassword123")

        mockMvc.perform(
            post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.error.code").value("INVALID_TOKEN"))
    }

    @Test
    fun `reset-password should fail with short password`() {
        val request = ResetPasswordRequest(token = "some-token", newPassword = "short")

        mockMvc.perform(
            post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
    }

    @Test
    fun `reset-password should fail with blank token`() {
        val request = ResetPasswordRequest(token = "", newPassword = "newpassword123")

        mockMvc.perform(
            post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
    }

    @Test
    fun `reset-password should fail with already used token`() {
        createUser("resetuser4", "reset4@example.com", "oldpassword")

        mockMvc.perform(
            post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ForgotPasswordRequest("reset4@example.com"))),
        )

        val token = passwordResetTokenRepository.findAll().first().token

        mockMvc.perform(
            post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ResetPasswordRequest(token, "newpassword123"))),
        )
            .andExpect(status().isOk)

        mockMvc.perform(
            post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ResetPasswordRequest(token, "anotherpassword"))),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.error.code").value("INVALID_TOKEN"))
    }
}
