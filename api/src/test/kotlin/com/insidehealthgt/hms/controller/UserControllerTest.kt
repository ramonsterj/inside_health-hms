package com.insidehealthgt.hms.controller

import com.insidehealthgt.hms.dto.request.AssignRolesRequest
import com.insidehealthgt.hms.dto.request.ChangePasswordRequest
import com.insidehealthgt.hms.dto.request.CreateUserRequest
import com.insidehealthgt.hms.dto.request.PhoneNumberRequest
import com.insidehealthgt.hms.dto.request.UpdateUserRequest
import com.insidehealthgt.hms.entity.PhoneType
import com.insidehealthgt.hms.entity.User
import com.insidehealthgt.hms.entity.UserStatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

@Suppress("LargeClass")
class UserControllerTest : AbstractIntegrationTest() {

    private lateinit var userToken: String
    private lateinit var adminToken: String
    private lateinit var regularUser: User

    @BeforeEach
    fun setUp() {
        // Create regular user (with permissions loaded)
        val userRole = roleRepository.findByCodeWithPermissions("USER")!!
        val user = User(
            username = "user",
            email = "user@example.com",
            passwordHash = passwordEncoder.encode("password123")!!,
            firstName = "Regular",
            lastName = "User",
            mustChangePassword = false,
        )
        user.roles.add(userRole)
        regularUser = userRepository.save(user)
        userToken = loginAndGetToken("user@example.com", "password123")

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
        adminToken = loginAndGetToken("admin@example.com", "admin123")
    }

    // ============ GET CURRENT USER TESTS ============

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
            .andExpect(status().isUnauthorized)
    }

    // ============ UPDATE PROFILE TESTS ============

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

    // ============ CHANGE PASSWORD TESTS ============

    @Test
    fun `changePassword should succeed with correct current password`() {
        val request = ChangePasswordRequest(
            currentPassword = "password123",
            newPassword = "newpassword456",
        )

        mockMvc.perform(
            put("/api/users/me/password")
                .header("Authorization", "Bearer $userToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))

        // Verify login works with new password
        loginAndGetToken("user@example.com", "newpassword456")
    }

    @Test
    fun `changePassword should fail with wrong current password`() {
        val request = ChangePasswordRequest(
            currentPassword = "wrongpassword",
            newPassword = "newpassword456",
        )

        mockMvc.perform(
            put("/api/users/me/password")
                .header("Authorization", "Bearer $userToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.message").value("Current password is incorrect"))
    }

    @Test
    fun `changePassword should fail with too short new password`() {
        val request = ChangePasswordRequest(
            currentPassword = "password123",
            newPassword = "short",
        )

        mockMvc.perform(
            put("/api/users/me/password")
                .header("Authorization", "Bearer $userToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `changePassword should fail without authentication`() {
        val request = ChangePasswordRequest(
            currentPassword = "password123",
            newPassword = "newpassword456",
        )

        mockMvc.perform(
            put("/api/users/me/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isUnauthorized)
    }

    // ============ UPDATE LOCALE TESTS ============

    @Test
    fun `updateLocale should succeed with supported locale`() {
        mockMvc.perform(
            put("/api/users/me/locale")
                .param("locale", "es")
                .header("Authorization", "Bearer $userToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
    }

    @Test
    fun `updateLocale should fail with unsupported locale`() {
        mockMvc.perform(
            put("/api/users/me/locale")
                .param("locale", "fr")
                .header("Authorization", "Bearer $userToken"),
        )
            .andExpect(status().isBadRequest)
    }

    // ============ LIST USERS TESTS ============

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

    // ============ CREATE USER (ADMIN) TESTS ============

    @Test
    fun `createUser should succeed for admin`() {
        val request = CreateUserRequest(
            username = "newuser",
            email = "newuser@example.com",
            password = "password123",
            firstName = "New",
            lastName = "User",
            phoneNumbers = listOf(PhoneNumberRequest(phoneNumber = "+502 5555-1234", phoneType = PhoneType.MOBILE)),
        )

        mockMvc.perform(
            post("/api/users")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.username").value("newuser"))
            .andExpect(jsonPath("$.data.email").value("newuser@example.com"))
            .andExpect(jsonPath("$.data.firstName").value("New"))
    }

    @Test
    fun `createUser should fail with duplicate email`() {
        val request = CreateUserRequest(
            username = "differentuser",
            email = "user@example.com", // Already exists
            password = "password123",
            phoneNumbers = listOf(PhoneNumberRequest(phoneNumber = "+502 5555-1234", phoneType = PhoneType.MOBILE)),
        )

        mockMvc.perform(
            post("/api/users")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.error.message").value("Email 'user@example.com' is already registered"))
    }

    @Test
    fun `createUser should fail with duplicate username`() {
        val request = CreateUserRequest(
            username = "user", // Already exists
            email = "different@example.com",
            password = "password123",
            phoneNumbers = listOf(PhoneNumberRequest(phoneNumber = "+502 5555-1234", phoneType = PhoneType.MOBILE)),
        )

        mockMvc.perform(
            post("/api/users")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.error.message").value("Username 'user' is already taken"))
    }

    @Test
    fun `createUser should fail for regular user`() {
        val request = CreateUserRequest(
            username = "unauthorized",
            email = "unauthorized@example.com",
            password = "password123",
            phoneNumbers = listOf(PhoneNumberRequest(phoneNumber = "+502 5555-1234", phoneType = PhoneType.MOBILE)),
        )

        mockMvc.perform(
            post("/api/users")
                .header("Authorization", "Bearer $userToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isForbidden)
    }

    // ============ GET USER (ADMIN) TESTS ============

    @Test
    fun `getUser should return user details for admin`() {
        mockMvc.perform(
            get("/api/users/${regularUser.id}")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.email").value("user@example.com"))
    }

    @Test
    fun `getUser should return 404 for non-existent user`() {
        mockMvc.perform(
            get("/api/users/99999")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `getUser should fail for regular user`() {
        mockMvc.perform(
            get("/api/users/${regularUser.id}")
                .header("Authorization", "Bearer $userToken"),
        )
            .andExpect(status().isForbidden)
    }

    // ============ DELETE USER TESTS ============

    @Test
    fun `deleteUser should soft delete and exclude from queries`() {
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

    // ============ RESET PASSWORD (ADMIN) TESTS ============

    @Test
    fun `resetPassword should succeed for admin`() {
        mockMvc.perform(
            post("/api/users/${regularUser.id}/reset-password")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.temporaryPassword").exists())
    }

    @Test
    fun `resetPassword should fail for regular user`() {
        val users = userRepository.findAll()
        val adminUser = users.first { it.email == "admin@example.com" }

        mockMvc.perform(
            post("/api/users/${adminUser.id}/reset-password")
                .header("Authorization", "Bearer $userToken"),
        )
            .andExpect(status().isForbidden)
    }

    // ============ LIST DELETED USERS (ADMIN) TESTS ============

    @Test
    fun `listDeleted should return deleted users for admin`() {
        // Soft delete a user
        regularUser.status = UserStatus.DELETED
        regularUser.deletedAt = LocalDateTime.now()
        userRepository.save(regularUser)

        mockMvc.perform(
            get("/api/users/deleted")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content").isArray)
            .andExpect(jsonPath("$.data.content.length()").value(1))
    }

    @Test
    fun `listDeleted should fail for regular user`() {
        mockMvc.perform(
            get("/api/users/deleted")
                .header("Authorization", "Bearer $userToken"),
        )
            .andExpect(status().isForbidden)
    }

    // ============ RESTORE USER (ADMIN) TESTS ============

    @Test
    fun `restoreUser should succeed for admin`() {
        // Soft delete a user first
        regularUser.status = UserStatus.DELETED
        regularUser.deletedAt = LocalDateTime.now()
        userRepository.save(regularUser)

        mockMvc.perform(
            post("/api/users/${regularUser.id}/restore")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.email").value("user@example.com"))

        // User should be found again
        mockMvc.perform(
            get("/api/users/${regularUser.id}")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `restoreUser should return 404 for non-deleted user`() {
        mockMvc.perform(
            post("/api/users/99999/restore")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isNotFound)
    }

    // ============ ASSIGN ROLES (ADMIN) TESTS ============

    @Test
    fun `assignRoles should succeed for admin`() {
        val request = AssignRolesRequest(
            roleCodes = listOf("USER", "DOCTOR"),
        )

        mockMvc.perform(
            put("/api/users/${regularUser.id}/roles")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.roles.length()").value(2))
    }

    @Test
    fun `assignRoles should fail with invalid role codes`() {
        val request = AssignRolesRequest(
            roleCodes = listOf("NONEXISTENT_ROLE"),
        )

        mockMvc.perform(
            put("/api/users/${regularUser.id}/roles")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `assignRoles should fail for regular user`() {
        val request = AssignRolesRequest(
            roleCodes = listOf("ADMIN"),
        )

        mockMvc.perform(
            put("/api/users/${regularUser.id}/roles")
                .header("Authorization", "Bearer $userToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isForbidden)
    }
}
